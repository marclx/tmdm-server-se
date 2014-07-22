/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.storage.hibernate;

import com.amalto.core.query.user.*;
import com.amalto.core.query.user.metadata.TaskId;
import com.amalto.core.query.user.metadata.Timestamp;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageResults;
import com.amalto.core.storage.datasource.DataSource;
import com.amalto.core.storage.datasource.RDBMSDataSource;
import com.amalto.core.storage.exception.FullTextQueryCompositeKeyException;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.record.metadata.UnsupportedDataRecordMetadata;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.*;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.talend.mdm.commmon.metadata.*;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.util.*;


class FullTextQueryHandler extends AbstractQueryHandler {

    private FullTextQuery query;

    private int pageSize;

    private final MappingRepository mappings;

    public FullTextQueryHandler(Storage storage,
                                MappingRepository mappings,
                                StorageClassLoader storageClassLoader,
                                Session session,
                                Select select,
                                List<TypedExpression> selectedFields,
                                Set<ResultsCallback> callbacks) {
        super(storage, storageClassLoader, session, select, selectedFields, callbacks);
        this.mappings = mappings;
    }

    @Override
    public StorageResults visit(Select select) {
        // TMDM-4654: Checks if entity has a composite PK.
        Set<ComplexTypeMetadata> compositeKeyTypes = new HashSet<ComplexTypeMetadata>();
        // TMDM-7496: Search should include references to reused types
        Collection<ComplexTypeMetadata> types = new HashSet<ComplexTypeMetadata>(select.getTypes());
        for (ComplexTypeMetadata type : select.getTypes()) {
            types.addAll(type.accept(new SearchTransitiveClosure()));
        }
        for (ComplexTypeMetadata type : types) {
            if (type.getKeyFields().size() > 1) {
                compositeKeyTypes.add(type);
            }
        }
        if (!compositeKeyTypes.isEmpty()) {
            StringBuilder message = new StringBuilder();
            Iterator it = compositeKeyTypes.iterator();
            while (it.hasNext()) {
                ComplexTypeMetadata compositeKeyType = (ComplexTypeMetadata) it.next();
                message.append(compositeKeyType.getName());
                if (it.hasNext()) {
                    message.append(',');
                }
            }
            throw new FullTextQueryCompositeKeyException(message.toString());
        }
        // Removes Joins and joined fields.
        List<Join> joins = select.getJoins();
        if (!joins.isEmpty()) {
            Set<ComplexTypeMetadata> joinedTypes = new HashSet<ComplexTypeMetadata>();
            for (Join join : joins) {
                joinedTypes.add(join.getRightField().getFieldMetadata().getContainingType());
            }
            for (ComplexTypeMetadata joinedType : joinedTypes) {
                types.remove(joinedType);
            }
            List<TypedExpression> filteredFields = new LinkedList<TypedExpression>();
            for (TypedExpression expression : select.getSelectedFields()) {
                if (expression instanceof Field) {
                    FieldMetadata fieldMetadata = ((Field) expression).getFieldMetadata();
                    if (joinedTypes.contains(fieldMetadata.getContainingType())) {
                        TypeMapping mapping = mappings.getMappingFromDatabase(fieldMetadata.getContainingType());
                        filteredFields.add(new Alias(new StringConstant(StringUtils.EMPTY), mapping.getUser(fieldMetadata).getName()));
                    } else {
                        filteredFields.add(expression);
                    }
                } else {
                    filteredFields.add(expression);
                }
            }
            selectedFields.clear();
            selectedFields.addAll(filteredFields);
        }
        // Handle condition
        Condition condition = select.getCondition();
        if (condition == null) {
            throw new IllegalArgumentException("Expected a condition in select clause but got 0.");
        }
        // Create Lucene query (concatenates all sub queries together).
        FullTextSession fullTextSession = Search.getFullTextSession(session);
        DataSource dataSource = storage.getDataSource();
        if (!(dataSource instanceof RDBMSDataSource)) {
            throw new IllegalArgumentException("Data source is not a RDBMS datasource.");
        }
        Query parsedQuery = select.getCondition().accept(new LuceneQueryGenerator(types, (RDBMSDataSource) dataSource));
        // Create Hibernate Search query
        Set<Class> classes = new HashSet<Class>();
        for (ComplexTypeMetadata type : types) {
            String className = ClassCreator.getClassName(type.getName());
            try {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                classes.add(contextClassLoader.loadClass(className));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not find class '" + className + "'.", e);
            }
        }
        FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(parsedQuery,
                classes.toArray(new Class<?>[classes.size()]));
        // Very important to leave this null (would disable ability to search across different types)
        fullTextQuery.setCriteriaQuery(null);
        fullTextQuery.setSort(Sort.RELEVANCE); // Default sort (if no order by specified).
        query = EntityFinder.wrap(fullTextQuery, (HibernateStorage) storage, session); // ensures only MDM entity objects are returned.
        // Order by
        for (OrderBy current : select.getOrderBy()) {
            current.accept(this);
        }
        // Paging
        Paging paging = select.getPaging();
        paging.accept(this);
        pageSize = paging.getLimit();
        boolean hasPaging = pageSize < Integer.MAX_VALUE;
        if (!hasPaging) {
            return createResults(query.scroll(ScrollMode.FORWARD_ONLY));
        } else {
            return createResults(query.list());
        }
    }

    @Override
    public StorageResults visit(Paging paging) {
        query.setFirstResult(paging.getStart());
        query.setFetchSize(AbstractQueryHandler.JDBC_FETCH_SIZE);
        query.setMaxResults(paging.getLimit());
        return null;
    }

    private StorageResults createResults(final List list) {
        CloseableIterator<DataRecord> iterator;
        if (selectedFields.isEmpty()) {
            iterator = new ListIterator(mappings, storageClassLoader, list.iterator(), callbacks);
        } else {
            iterator = new ListIterator(mappings, storageClassLoader, list.iterator(), callbacks) {
                @Override
                public DataRecord next() {
                    final DataRecord next = super.next();
                    final ComplexTypeMetadata explicitProjectionType = new ComplexTypeMetadataImpl(StringUtils.EMPTY, Storage.PROJECTION_TYPE, false);
                    final DataRecord nextRecord = new DataRecord(explicitProjectionType, UnsupportedDataRecordMetadata.INSTANCE);
                    VisitorAdapter<Void> visitor = new VisitorAdapter<Void>() {
                        private String aliasName;

                        private String typeName;

                        @Override
                        public Void visit(Field field) {
                            FieldMetadata fieldMetadata = field.getFieldMetadata();
                            TypeMapping mapping = mappings.getMappingFromDatabase(fieldMetadata.getContainingType());
                            if (mapping != null && mapping.getUser(fieldMetadata) != null) {
                                fieldMetadata = mapping.getUser(fieldMetadata);
                            }
                            Object value;
                            if (fieldMetadata instanceof ReferenceFieldMetadata) {
                                value = getReferencedId(next, (ReferenceFieldMetadata) fieldMetadata);
                            } else {
                                value = next.get(fieldMetadata);
                            }
                            if (aliasName != null) {
                                SimpleTypeMetadata fieldType = new SimpleTypeMetadata(XMLConstants.W3C_XML_SCHEMA_NS_URI, typeName == null ? fieldMetadata.getType().getName() : typeName);
                                fieldMetadata = new SimpleTypeFieldMetadata(explicitProjectionType, false, false, false, aliasName, fieldType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
                                explicitProjectionType.addField(fieldMetadata);
                            } else {
                                explicitProjectionType.addField(fieldMetadata);
                            }
                            nextRecord.set(fieldMetadata, value);
                            return null;
                        }

                        @Override
                        public Void visit(StringConstant constant) {
                            if (aliasName != null) {
                                SimpleTypeMetadata fieldType = new SimpleTypeMetadata(XMLConstants.W3C_XML_SCHEMA_NS_URI, typeName);
                                FieldMetadata fieldMetadata = new SimpleTypeFieldMetadata(explicitProjectionType, false, false, false, aliasName, fieldType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
                                explicitProjectionType.addField(fieldMetadata);
                                nextRecord.set(fieldMetadata, constant.getValue());
                            } else {
                                throw new IllegalStateException("Expected an alias for a constant expression.");
                            }
                            return null;
                        }

                        @Override
                        public Void visit(Count count) {
                            if (aliasName != null) {
                                SimpleTypeMetadata fieldType = new SimpleTypeMetadata(XMLConstants.W3C_XML_SCHEMA_NS_URI, typeName);
                                FieldMetadata fieldMetadata = new SimpleTypeFieldMetadata(explicitProjectionType, false, false, false, aliasName, fieldType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
                                explicitProjectionType.addField(fieldMetadata);
                                nextRecord.set(fieldMetadata, list.size());
                            }
                            return null;
                        }

                        @Override
                        public Void visit(Alias alias) {
                            aliasName = alias.getAliasName();
                            typeName = alias.getTypeName();
                            {
                                alias.getTypedExpression().accept(this);
                            }
                            aliasName = null;
                            typeName = null;
                            return null;
                        }

                        @Override
                        public Void visit(Timestamp timestamp) {
                            SimpleTypeMetadata fieldType = new SimpleTypeMetadata(XMLConstants.W3C_XML_SCHEMA_NS_URI, typeName);
                            SimpleTypeFieldMetadata aliasField = new SimpleTypeFieldMetadata(explicitProjectionType, false, false, false, aliasName, fieldType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
                            explicitProjectionType.addField(aliasField);
                            nextRecord.set(aliasField, next.getRecordMetadata().getLastModificationTime());
                            return null;
                        }

                        @Override
                        public Void visit(TaskId taskId) {
                            SimpleTypeMetadata fieldType = new SimpleTypeMetadata(XMLConstants.W3C_XML_SCHEMA_NS_URI, typeName);
                            SimpleTypeFieldMetadata aliasField = new SimpleTypeFieldMetadata(explicitProjectionType, false, false, false, aliasName, fieldType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
                            explicitProjectionType.addField(aliasField);
                            nextRecord.set(aliasField, next.getRecordMetadata().getTaskId());
                            return null;
                        }

                        @Override
                        public Void visit(Type type) {
                            FieldMetadata fieldMetadata = type.getField().getFieldMetadata();
                            SimpleTypeMetadata fieldType = new SimpleTypeMetadata(XMLConstants.W3C_XML_SCHEMA_NS_URI, typeName);
                            SimpleTypeFieldMetadata aliasField = new SimpleTypeFieldMetadata(explicitProjectionType, false, false, false, aliasName, fieldType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
                            explicitProjectionType.addField(aliasField);
                            DataRecord dataRecord = (DataRecord) next.get(fieldMetadata.getName());
                            if (dataRecord != null) {
                                nextRecord.set(aliasField, dataRecord.getType().getName());
                            } else {
                                nextRecord.set(aliasField, StringUtils.EMPTY);
                            }
                            return null;
                        }
                    };
                    for (TypedExpression selectedField : selectedFields) {
                        selectedField.accept(visitor);
                    }
                    return nextRecord;
                }
            };
        }
        return new FullTextStorageResults(pageSize, query.getResultSize(), iterator);
    }

    private StorageResults createResults(ScrollableResults scrollableResults) {
        CloseableIterator<DataRecord> iterator;
        if (selectedFields.isEmpty()) {
            iterator = new ScrollableIterator(mappings,
                    storageClassLoader,
                    scrollableResults,
                    callbacks);
        } else {
            iterator = new ScrollableIterator(mappings,
                    storageClassLoader,
                    scrollableResults,
                    callbacks) {
                @Override
                public DataRecord next() {
                    DataRecord next = super.next();
                    ComplexTypeMetadata explicitProjectionType = new ComplexTypeMetadataImpl(StringUtils.EMPTY,
                            Storage.PROJECTION_TYPE,
                            false);
                    DataRecord nextRecord = new DataRecord(explicitProjectionType, UnsupportedDataRecordMetadata.INSTANCE);
                    for (TypedExpression selectedField : selectedFields) {
                        if (selectedField instanceof Field) {
                            FieldMetadata field = ((Field) selectedField).getFieldMetadata();
                            TypeMapping mapping = mappings.getMappingFromDatabase(field.getContainingType());
                            if (mapping != null && mapping.getUser(field) != null) {
                                field = mapping.getUser(field);
                            }
                            explicitProjectionType.addField(field);
                            if (field instanceof ReferenceFieldMetadata) {
                                nextRecord.set(field, getReferencedId(next, (ReferenceFieldMetadata) field));
                            } else {
                                nextRecord.set(field, next.get(field));
                            }
                        } else if (selectedField instanceof Alias) {
                            SimpleTypeMetadata fieldType = new SimpleTypeMetadata(XMLConstants.W3C_XML_SCHEMA_NS_URI, selectedField.getTypeName());
                            Alias alias = (Alias) selectedField;
                            SimpleTypeFieldMetadata newField = new SimpleTypeFieldMetadata(explicitProjectionType,
                                    false,
                                    false,
                                    false,
                                    alias.getAliasName(),
                                    fieldType,
                                    Collections.<String>emptyList(),
                                    Collections.<String>emptyList(),
                                    Collections.<String>emptyList());
                            explicitProjectionType.addField(newField);
                            TypedExpression typedExpression = alias.getTypedExpression();
                            if (typedExpression instanceof StringConstant) {
                                nextRecord.set(newField, ((StringConstant) typedExpression).getValue());
                            } else if(typedExpression instanceof Field) {
                                nextRecord.set(newField, next.get(((Field) typedExpression).getFieldMetadata()));
                            } else if(typedExpression instanceof Type) {
                                FieldMetadata fieldMetadata = ((Type) typedExpression).getField().getFieldMetadata();
                                nextRecord.set(newField, ((DataRecord) next.get(fieldMetadata.getName())).getType().getName());
                            } else {
                                throw new IllegalArgumentException("Aliased expression '" + typedExpression + "' is not supported.");
                            }
                        }
                    }
                    explicitProjectionType.freeze();
                    return nextRecord;
                }
            };
        }

        return new FullTextStorageResults(pageSize, query.getResultSize(), iterator);
    }

    private static Object getReferencedId(DataRecord next, ReferenceFieldMetadata field) {
        DataRecord record = (DataRecord) next.get(field);
        if (record != null) {
            Collection<FieldMetadata> keyFields = record.getType().getKeyFields();
            if (keyFields.size() == 1) {
                return record.get(keyFields.iterator().next());
            } else {
                List<Object> compositeKeyValues = new ArrayList<Object>(keyFields.size());
                for (FieldMetadata keyField : keyFields) {
                    compositeKeyValues.add(record.get(keyField));
                }
                return compositeKeyValues;
            }
        } else {
            return StringUtils.EMPTY;
        }
    }

    @Override
    public StorageResults visit(OrderBy orderBy) {
        TypedExpression field = orderBy.getField();
        if (field instanceof Field) {
            FieldMetadata fieldMetadata = ((Field) field).getFieldMetadata();
            SortField sortField = new SortField(fieldMetadata.getName(),
                    getSortType(fieldMetadata),
                    orderBy.getDirection() == OrderBy.Direction.DESC);
            query.setSort(new Sort(sortField));
            return null;
        } else {
            throw new NotImplementedException("No support for order by for full text search on non-field.");
        }
    }

    private static int getSortType(FieldMetadata fieldMetadata) {
        TypeMetadata fieldType = fieldMetadata.getType();
        String type = MetadataUtils.getSuperConcreteType(fieldType).getName();
        if (Types.STRING.equals(type)
                || Types.ANY_URI.equals(type)
                || Types.BOOLEAN.equals(type)
                || Types.BASE64_BINARY.equals(type)
                || Types.QNAME.equals(type)
                || Types.HEX_BINARY.equals(type)) {
            return SortField.STRING_VAL; // STRING does not work well for 'long' strings.
        } else if (Types.INT.equals(type)
                || Types.INTEGER.equals(type)
                || Types.POSITIVE_INTEGER.equals(type)
                || Types.NON_POSITIVE_INTEGER.equals(type)
                || Types.NON_NEGATIVE_INTEGER.equals(type)
                || Types.NEGATIVE_INTEGER.equals(type)
                || Types.UNSIGNED_INT.equals(type)) {
            return SortField.INT;
        } else if (Types.DECIMAL.equals(type) || Types.DOUBLE.equals(type)) {
            return SortField.DOUBLE;
        } else if (Types.DATE.equals(type)
                || Types.DATETIME.equals(type)
                || Types.TIME.equals(type)
                || Types.DURATION.equals(type)) {
            return SortField.STRING;
        } else if (Types.UNSIGNED_SHORT.equals(type) || Types.SHORT.equals(type)) {
            return SortField.SHORT;
        } else if (Types.UNSIGNED_LONG.equals(type) || Types.LONG.equals(type)) {
            return SortField.LONG;
        } else if (Types.FLOAT.equals(type)) {
            return SortField.FLOAT;
        } else if (Types.BYTE.equals(type) || Types.UNSIGNED_BYTE.equals(type)) {
            return SortField.BYTE;
        } else {
            throw new UnsupportedOperationException("No support for field typed as '" + type + "'");
        }
    }

    private static class FullTextStorageResults implements StorageResults {

        private final int size;

        private final int count;

        private final CloseableIterator<DataRecord> iterator;

        public FullTextStorageResults(int size, int count, CloseableIterator<DataRecord> iterator) {
            this.size = size;
            this.count = count;
            this.iterator = iterator;
        }

        public int getSize() {
            if (size == Integer.MAX_VALUE) {
                return getCount();
            }
            return size;
        }

        public int getCount() {
            return count;
        }

        public void close() {
            try {
                iterator.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public Iterator<DataRecord> iterator() {
            return iterator;
        }
    }

    private static class SearchTransitiveClosure extends DefaultMetadataVisitor<Collection<? extends ComplexTypeMetadata>> {

        private final Set<ComplexTypeMetadata> closure = new HashSet<ComplexTypeMetadata>();

        @Override
        public Collection<? extends ComplexTypeMetadata> visit(ComplexTypeMetadata complexType) {
            super.visit(complexType);
            return closure;
        }

        @Override
        public Collection<? extends ComplexTypeMetadata> visit(ContainedComplexTypeMetadata containedType) {
            super.visit(containedType);
            return closure;
        }

        @Override
        public Collection<? extends ComplexTypeMetadata> visit(ReferenceFieldMetadata referenceField) {
            ComplexTypeMetadata referencedType = referenceField.getReferencedType();
            if (!referencedType.isInstantiable()) {
                closure.add(referencedType);
            }
            return closure;
        }
    }

}
