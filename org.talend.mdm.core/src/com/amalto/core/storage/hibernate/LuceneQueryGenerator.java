/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.storage.hibernate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.NotImplementedException;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.ContainedComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.DefaultMetadataVisitor;
import org.talend.mdm.commmon.metadata.EnumerationFieldMetadata;
import org.talend.mdm.commmon.metadata.ReferenceFieldMetadata;
import org.talend.mdm.commmon.metadata.SimpleTypeFieldMetadata;

import com.amalto.core.query.user.Alias;
import com.amalto.core.query.user.BigDecimalConstant;
import com.amalto.core.query.user.BinaryLogicOperator;
import com.amalto.core.query.user.BooleanConstant;
import com.amalto.core.query.user.ByteConstant;
import com.amalto.core.query.user.Compare;
import com.amalto.core.query.user.DateConstant;
import com.amalto.core.query.user.DateTimeConstant;
import com.amalto.core.query.user.DoubleConstant;
import com.amalto.core.query.user.Expression;
import com.amalto.core.query.user.Field;
import com.amalto.core.query.user.FieldFullText;
import com.amalto.core.query.user.FloatConstant;
import com.amalto.core.query.user.FullText;
import com.amalto.core.query.user.IntegerConstant;
import com.amalto.core.query.user.LongConstant;
import com.amalto.core.query.user.Predicate;
import com.amalto.core.query.user.Range;
import com.amalto.core.query.user.ShortConstant;
import com.amalto.core.query.user.StringConstant;
import com.amalto.core.query.user.TimeConstant;
import com.amalto.core.query.user.UnaryLogicOperator;
import com.amalto.core.query.user.VisitorAdapter;
import com.amalto.core.query.user.metadata.MetadataField;
import com.amalto.core.query.user.metadata.StagingBlockKey;
import com.amalto.core.query.user.metadata.StagingError;
import com.amalto.core.query.user.metadata.StagingSource;
import com.amalto.core.query.user.metadata.StagingStatus;
import com.amalto.core.query.user.metadata.TaskId;
import com.amalto.core.query.user.metadata.Timestamp;
import com.amalto.core.storage.Storage;

class LuceneQueryGenerator extends VisitorAdapter<Query> {

    private final Collection<ComplexTypeMetadata> types;

    private String currentFieldName;

    private Object currentValue;

    private boolean isBuildingNot;

    LuceneQueryGenerator(Collection<ComplexTypeMetadata> types) {
        this.types = types;
    }

    @Override
    public Query visit(Compare condition) {
        condition.getLeft().accept(this);
        Expression right = condition.getRight();
        right.accept(this);
        if (condition.getPredicate() == Predicate.EQUALS || condition.getPredicate() == Predicate.CONTAINS
                || condition.getPredicate() == Predicate.STARTS_WITH) {
            StringTokenizer tokenizer = new StringTokenizer(String.valueOf(currentValue));
            BooleanQuery termQuery = new BooleanQuery();
            while (tokenizer.hasMoreTokens()) {
                TermQuery newTermQuery = new TermQuery(new Term(currentFieldName, tokenizer.nextToken().toLowerCase()));
                termQuery.add(newTermQuery, isBuildingNot ? BooleanClause.Occur.MUST_NOT : BooleanClause.Occur.MUST);
                if (condition.getPredicate() == Predicate.STARTS_WITH) {
                    break;
                }
            }
            return termQuery;
        } else if (condition.getPredicate() == Predicate.GREATER_THAN
                || condition.getPredicate() == Predicate.GREATER_THAN_OR_EQUALS
                || condition.getPredicate() == Predicate.LOWER_THAN || condition.getPredicate() == Predicate.LOWER_THAN_OR_EQUALS) {
            throw new RuntimeException("Greater than, less than are not supported in full text searches."); //$NON-NLS-1$
        } else {
            throw new NotImplementedException("No support for predicate '" + condition.getPredicate() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public Query visit(BinaryLogicOperator condition) {
        Query left = condition.getLeft().accept(this);
        Query right = condition.getRight().accept(this);
        BooleanQuery query = new BooleanQuery();
        if (condition.getPredicate() == Predicate.OR) {
            query.add(left, BooleanClause.Occur.SHOULD);
            query.add(right, BooleanClause.Occur.SHOULD);
        } else if (condition.getPredicate() == Predicate.AND) {
            query.add(left, isNotQuery(left) ? BooleanClause.Occur.SHOULD : BooleanClause.Occur.MUST);
            query.add(right, isNotQuery(right) ? BooleanClause.Occur.SHOULD : BooleanClause.Occur.MUST);
        } else {
            throw new NotImplementedException("No support for '" + condition.getPredicate() + "'."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return query;
    }

    private static boolean isNotQuery(Query left) {
        if (left instanceof BooleanQuery) {
            for (BooleanClause booleanClause : ((BooleanQuery) left).getClauses()) {
                if (booleanClause.getOccur() == BooleanClause.Occur.MUST_NOT) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Query visit(UnaryLogicOperator condition) {
        if (condition.getPredicate() == Predicate.NOT) {
            isBuildingNot = true;
            Query query = condition.getCondition().accept(this);
            isBuildingNot = false;
            return query;
        } else {
            throw new NotImplementedException("No support for predicate '" + condition.getPredicate() + "'."); //$NON-NLS-1$//$NON-NLS-2$
        }
    }

    @Override
    public Query visit(Range range) {
        if (range.getExpression() instanceof MetadataField) {
            if (range.getExpression() instanceof Timestamp) {
                Timestamp field = (Timestamp) range.getExpression();
                field.accept(this);
            } else {
                MetadataField field = (MetadataField) range.getExpression();
                field.getProjectionExpression().accept(this);
            }
        }
        range.getStart().accept(this);
        Long currentRangeStart = ((Long) currentValue) == Long.MIN_VALUE ? null : (Long) currentValue;
        range.getEnd().accept(this);
        Long currentRangeEnd = ((Long) currentValue) == Long.MAX_VALUE ? null : (Long) currentValue;
        return new TermRangeQuery(currentFieldName, String.valueOf(currentRangeStart), String.valueOf(currentRangeEnd), true, true);
        // Need Hibernate Search > 3.2 (+ @NumericField annotation in class ClassCreator)
        /*
        return NumericRangeQuery.newLongRange(currentFieldName, currentRangeStart, currentRangeEnd, true, true);
        */
    }

    @Override
    public Query visit(Timestamp timestamp) {
        currentFieldName = Storage.METADATA_TIMESTAMP;
        return null;
    }

    @Override
    public Query visit(StagingStatus stagingStatus) {
        currentFieldName = Storage.METADATA_STAGING_STATUS;
        return null;
    }

    @Override
    public Query visit(TaskId taskId) {
        currentFieldName = Storage.METADATA_TASK_ID;
        return null;
    }

    @Override
    public Query visit(StagingError stagingError) {
        currentFieldName = Storage.METADATA_STAGING_ERROR;
        return null;
    }

    @Override
    public Query visit(StagingSource stagingSource) {
        currentFieldName = Storage.METADATA_STAGING_SOURCE;
        return null;
    }

    @Override
    public Query visit(StagingBlockKey stagingBlockKey) {
        currentFieldName = Storage.METADATA_STAGING_BLOCK_KEY;
        return null;
    }

    @Override
    public Query visit(Field field) {
        currentFieldName = field.getFieldMetadata().getName();
        return null;
    }

    @Override
    public Query visit(Alias alias) {
        currentFieldName = alias.getAliasName();
        return null;
    }

    @Override
    public Query visit(StringConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(IntegerConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(DateConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(DateTimeConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(BooleanConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(BigDecimalConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(TimeConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(ShortConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(ByteConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(LongConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(DoubleConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(FloatConstant constant) {
        currentValue = constant.getValue();
        return null;
    }

    @Override
    public Query visit(FullText fullText) {
        // TODO Test me on conditions where many types share same field names.
        final Set<String> fields = new HashSet<String>();
        for (final ComplexTypeMetadata type : types) {
            type.accept(new DefaultMetadataVisitor<Void>() {

                @Override
                public Void visit(ContainedComplexTypeMetadata containedType) {
                    super.visit(containedType);
                    for (ComplexTypeMetadata subType : containedType.getSubTypes()) {
                        subType.accept(this);
                    }
                    return null;
                }

                @Override
                public Void visit(ReferenceFieldMetadata referenceField) {
                    ComplexTypeMetadata referencedType = referenceField.getReferencedType();
                    if (!referencedType.isInstantiable()) {
                        referencedType.accept(this);
                    }
                    return null;
                }

                @Override
                public Void visit(SimpleTypeFieldMetadata simpleField) {
                    if (!Storage.METADATA_TIMESTAMP.equals(simpleField.getName())
                            && !Storage.METADATA_TASK_ID.equals(simpleField.getName())) {
                        fields.add(simpleField.getName());
                    }
                    return null;
                }

                @Override
                public Void visit(EnumerationFieldMetadata enumField) {
                    fields.add(enumField.getName());
                    return null;
                }
            });
        }

        String[] fieldsAsArray = fields.toArray(new String[fields.size()]);
        StringBuilder queryBuffer = new StringBuilder();
        Iterator<String> fieldsIterator = fields.iterator();
        String fullTextValue = getValue(fullText);
        while (fieldsIterator.hasNext()) {
            String next = fieldsIterator.next();
            queryBuffer.append(next).append(':').append(fullTextValue);
            if (fieldsIterator.hasNext()) {
                queryBuffer.append(" OR "); //$NON-NLS-1$
            }
        }
        String fullTextQuery = queryBuffer.toString();
        return parseQuery(fieldsAsArray, fullTextQuery);
    }

    @Override
    public Query visit(FieldFullText fieldFullText) {
        String fieldName = fieldFullText.getField().getFieldMetadata().getName();
        String[] fieldsAsArray = new String[] { fieldName };
        String fullTextQuery = fieldName + ':' + getValue(fieldFullText);
        return parseQuery(fieldsAsArray, fullTextQuery);
    }

    private Query parseQuery(String[] fieldsAsArray, String fullTextQuery) {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_29, fieldsAsArray, new KeywordAnalyzer());
        // Very important! Lucene does an implicit lower case for "expanded terms" (which is something used).
        parser.setLowercaseExpandedTerms(true);
        try {
            return parser.parse(fullTextQuery);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid generated Lucene query", e); //$NON-NLS-1$
        }
    }

    private static String getValue(FullText fullText) {
        String value = fullText.getValue().toLowerCase();
        int index = 0;
        while (value.charAt(index) == '*') { // Skip '*' characters at beginning.
            index++;
        }
        if (index > 0) {
            value = value.substring(index);
        }
        char[] removes = new char[] { '[', ']', '+', '!', '(', ')', '^', '\"', '~', ':', '\\' }; // Removes reserved
                                                                                                 // characters
        for (char remove : removes) {
            value = value.replace(remove, ' ');
        }
        if (!value.endsWith("*")) { //$NON-NLS-1$
            value += '*';
        }
        return value;
    }
}
