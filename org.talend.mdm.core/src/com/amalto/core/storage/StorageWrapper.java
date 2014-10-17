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

package com.amalto.core.storage;

import com.amalto.core.load.io.ResettableStringWriter;
import com.amalto.core.query.user.*;
import com.amalto.core.query.user.metadata.Timestamp;
import com.amalto.core.server.ServerContext;
import com.amalto.core.server.StorageAdmin;
import com.amalto.core.storage.record.*;
import com.amalto.xmlserver.interfaces.IWhereItem;
import com.amalto.xmlserver.interfaces.IXmlServerSLWrapper;
import com.amalto.xmlserver.interfaces.ItemPKCriteria;
import com.amalto.xmlserver.interfaces.XmlServerException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.metadata.*;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.*;
import java.util.*;

import static com.amalto.core.query.user.UserQueryBuilder.*;

public class StorageWrapper implements IXmlServerSLWrapper {

    private static final Logger LOGGER = Logger.getLogger(StorageWrapper.class);

    private final DataRecordReader<String> xmlStringReader = new XmlStringDataRecordReader();

    protected StorageAdmin storageAdmin;

    public StorageWrapper() {
    }

    private static Select getSelectTypeById(ComplexTypeMetadata type, String revisionId, String[] splitUniqueId, String uniqueID) {
        ComplexTypeMetadata typeForSelect = type;
        while (typeForSelect.getSuperTypes() != null && !typeForSelect.getSuperTypes().isEmpty() && typeForSelect.getSuperTypes().size() > 0) {
            typeForSelect = (ComplexTypeMetadata) typeForSelect.getSuperTypes().iterator().next();
        }
        UserQueryBuilder qb = UserQueryBuilder.from(typeForSelect);
        Collection<FieldMetadata> keyFields = type.getKeyFields();

        if (splitUniqueId.length < (2 + keyFields.size())) {
            StringBuilder builder = new StringBuilder();
            for (String currentId : splitUniqueId) {
                builder.append(currentId).append('.');
            }
            throw new IllegalArgumentException("Id '" + builder.toString() + "' does not contain all required values for key of type '" + type.getName() + "'.");
        } else if (keyFields.size() == 1) {
            // Split unique id > keyField: if # of key elements is 1, consider all remaining value as a single value (with '.' separators).
            String uniqueIdPrefix = splitUniqueId[0] + '.' + splitUniqueId[1] + '.';
            String key = StringUtils.removeStart(uniqueID, uniqueIdPrefix);
            qb.where(eq(keyFields.iterator().next(), key));
        } else {
            int currentIndex = 2;
            for (FieldMetadata keyField : keyFields) {
                qb.where(eq(keyField, splitUniqueId[currentIndex++]));
            }
        }
        qb.getSelect().setRevisionId(revisionId);
        return qb.getSelect();
    }

    protected static String getTypeName(String uniqueID) {
        if (uniqueID == null) {
            throw new IllegalArgumentException("Unique id can not be null.");
        }
        char[] chars = uniqueID.toCharArray();
        StringBuilder typeName = new StringBuilder();
        boolean isType = false;
        for (char currentChar : chars) {
            if ('.' == currentChar) {
                if (isType) {
                    break;
                } else {
                    isType = true;
                }
            } else {
                if (isType) {
                    typeName.append(currentChar);
                }
            }
        }
        return typeName.toString();
    }

    public boolean isUpAndRunning() {
        return getStorageAdmin() != null;
    }

    public String[] getAllClusters(String revisionID) throws XmlServerException {
        return getStorageAdmin().getAll(revisionID);
    }

    public long deleteCluster(String revisionID, String clusterName) throws XmlServerException {
        long start = System.currentTimeMillis();
        {
            // TMDM-4692 Delete both master and staging data containers.
            getStorageAdmin().delete(clusterName, StorageType.MASTER, revisionID, true);
            getStorageAdmin().delete(clusterName, StorageType.STAGING, revisionID, true);
        }
        return System.currentTimeMillis() - start;
    }

    public long deleteAllClusters(String revisionID) throws XmlServerException {
        long start = System.currentTimeMillis();
        {
            getStorageAdmin().deleteAll(revisionID, true);
        }
        return System.currentTimeMillis() - start;
    }

    public long createCluster(String revisionID, String clusterName) throws XmlServerException {
        long start = System.currentTimeMillis();
        {
            StorageAdmin admin = getStorageAdmin();
            String dataSourceName = admin.getDatasource(clusterName);
            admin.create(clusterName, clusterName, admin.getType(clusterName), dataSourceName, null);
        }
        return System.currentTimeMillis() - start;
    }

    public boolean existCluster(String revision, String cluster) throws XmlServerException {
        StorageType storageType = cluster.endsWith(StorageAdmin.STAGING_SUFFIX) ? StorageType.STAGING : StorageType.MASTER;
        return getStorageAdmin().exist(cluster, storageType, revision);
    }

    public long putDocumentFromFile(String fileName, String uniqueID, String clusterName, String revisionID) throws XmlServerException {
        return putDocumentFromFile(fileName, uniqueID, clusterName, revisionID, IXmlServerSLWrapper.TYPE_DOCUMENT);
    }

    public long putDocumentFromFile(String fileName, String uniqueID, String clusterName, String revisionID, String documentType) throws XmlServerException {
        long startTime = System.currentTimeMillis();
        File file = new File(fileName);
        if (!file.canRead()) {
            throw new XmlServerException("Can not read file '" + fileName + "'."); //$NON-NLS-1$
        }
        String content;
        try {
            if ("BINARY".equals(documentType)) { //$NON-NLS-1$
                content = "<filename>" + file.getName() + "</filename>"; //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                content = FileUtils.readFileToString(file);
            }
            putDocumentFromString(content, uniqueID, clusterName, revisionID);
        } catch (Exception e) {
            throw new XmlServerException("Can not save document file.", e); //$NON-NLS-1$
        }
        return System.currentTimeMillis() - startTime;
    }

    public long putDocumentFromString(String xmlString, String uniqueID, String clusterName, String revisionID) throws XmlServerException {
        return putDocumentFromString(xmlString, uniqueID, clusterName, revisionID, null);
    }

    public long putDocumentFromString(String xmlString, String uniqueID, String clusterName, String revisionID, String documentType) throws XmlServerException {
        String typeName = getTypeName(uniqueID);
        long start = System.currentTimeMillis();
        {
            Storage storage = getStorage(clusterName, revisionID);
            MetadataRepository repository = storage.getMetadataRepository();
            DataRecord record = xmlStringReader.read(revisionID, repository, repository.getComplexType(typeName), xmlString);
            try {
                storage.update(record);
            } catch (Exception e) {
                throw new XmlServerException(e);
            }
        }
        return System.currentTimeMillis() - start;
    }

    public long putDocumentFromDOM(Element root, String uniqueID, String clusterName, String revisionID) throws XmlServerException {
        String typeName = getTypeName(uniqueID);
        long start = System.currentTimeMillis();
        {
            DataRecordReader<Element> reader = new XmlDOMDataRecordReader();
            Storage storage = getStorage(clusterName, revisionID);
            MetadataRepository repository = storage.getMetadataRepository();
            DataRecord record = reader.read(revisionID, repository, repository.getComplexType(typeName), root);
            storage.update(record);
        }
        return System.currentTimeMillis() - start;
    }

    public long putDocumentFromSAX(String dataClusterName, XMLReader docReader, InputSource input, String revisionId) throws XmlServerException {
        String typeName = getTypeName(input.getPublicId());
        long start = System.currentTimeMillis();
        {
            Storage storage = getStorage(dataClusterName, revisionId);
            if (storage == null) {
                throw new XmlServerException("Data cluster '" + dataClusterName + "' does not exist.");
            }
            DataRecordReader<XmlSAXDataRecordReader.Input> reader = new XmlSAXDataRecordReader();
            XmlSAXDataRecordReader.Input readerInput = new XmlSAXDataRecordReader.Input(docReader, input);
            MetadataRepository repository = storage.getMetadataRepository();
            DataRecord record = reader.read(revisionId, repository, repository.getComplexType(typeName), readerInput);
            storage.update(record);
        }
        return System.currentTimeMillis() - start;
    }

    public String getDocumentAsString(String revisionID, String clusterName, String uniqueID) throws XmlServerException {
        // TODO Web UI sends incomplete ids (in case of save of instance with auto increment). Fix caller code in IXtentisRMIPort.
        if (uniqueID.split("\\.").length < 3) { //$NON-NLS-1$
            return null;
        }
        return getDocumentAsString(revisionID, clusterName, uniqueID, "UTF-8"); //$NON-NLS-1$
    }

    public String getDocumentAsString(String revisionID, String clusterName, String uniqueID, String encoding) throws XmlServerException {
        if (encoding == null) {
            encoding = "UTF-8"; //$NON-NLS-1$
        }
        String[] splitUniqueId = uniqueID.split("\\."); //$NON-NLS-1$
        Storage storage = getStorage(clusterName, revisionID);
        MetadataRepository repository = storage.getMetadataRepository();
        String typeName = splitUniqueId[1];
        ComplexTypeMetadata type = repository.getComplexType(typeName);
        Select select = getSelectTypeById(type, revisionID, splitUniqueId, uniqueID);
        StorageResults records = null;
        try {
            storage.begin();
            records = storage.fetch(select);
            ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
            Iterator<DataRecord> iterator = records.iterator();
            // Enforce root element name in case query returned instance of a subtype.
            DataRecordXmlWriter dataRecordXmlWriter = new DataRecordXmlWriter(type);
            String xmlString = null;
            if (iterator.hasNext()) {
                DataRecord result = iterator.next();
                long timestamp = result.getRecordMetadata().getLastModificationTime();
                String taskId = result.getRecordMetadata().getTaskId();
                String modelName = StringUtils.substringBeforeLast(clusterName, StorageAdmin.STAGING_SUFFIX);
                byte[] start = ("<ii><c>" + clusterName + "</c><dmn>" + modelName + "</dmn><dmr/><sp/><t>" + timestamp + "</t><taskId>" + taskId + "</taskId><i>" + StringEscapeUtils.escapeXml(splitUniqueId[2]) + "</i><p>").getBytes(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                output.write(start);
                dataRecordXmlWriter.write(result, output);
                if (iterator.hasNext()) {
                    throw new IllegalStateException("Expected only 1 result.");
                }
                byte[] end = ("</p></ii>").getBytes(); //$NON-NLS-1$
                output.write(end);
                output.flush();
                xmlString = new String(output.toByteArray(), encoding);
            }
            storage.commit();
            return xmlString;
        } catch (IOException e) {
            storage.rollback();
            throw new XmlServerException(e);
        } finally {
            if (records != null) {
                records.close();
            }
        }
    }

    public byte[] getDocumentBytes(String revisionID, String clusterName, String uniqueID, String documentType) throws XmlServerException {
        try {
            return getDocumentAsString(revisionID, clusterName, uniqueID).getBytes("UTF-8"); //$NON-NLS-1$
        } catch (UnsupportedEncodingException e) {
            throw new XmlServerException(e);
        }
    }

    public String[] getAllDocumentsUniqueID(String revisionID, String clusterName) throws XmlServerException {
        List<String> uniqueIds = new LinkedList<String>();
        Storage storage = getStorage(clusterName, revisionID);
        MetadataRepository repository = storage.getMetadataRepository();
        Collection<ComplexTypeMetadata> typeToQuery;
        if(clusterName.contains("/")) { //$NON-NLS-1$
            String typeName = StringUtils.substringAfter(clusterName, "/"); //$NON-NLS-1$
            ComplexTypeMetadata complexType = repository.getComplexType(typeName);
            if (complexType == null) {
                throw new IllegalArgumentException("Type '" + typeName + "' does not exist in container '" + storage.getName() + "'");
            }
            typeToQuery = Collections.singletonList(complexType);
        } else {
            typeToQuery = getClusterTypes(clusterName, revisionID);
        }
        try {
            storage.begin();
            for (ComplexTypeMetadata currentType : typeToQuery) {
                UserQueryBuilder qb = from(currentType).selectId(currentType);
                StorageResults results = storage.fetch(qb.getSelect());
                for (DataRecord result : results) {
                    Iterator<FieldMetadata> setFields = result.getSetFields().iterator();
                    StringBuilder builder = new StringBuilder();
                    builder.append(clusterName).append('.').append(currentType.getName()).append('.');
                    while (setFields.hasNext()) {
                        builder.append(String.valueOf(result.get(setFields.next())));
                        if (setFields.hasNext()) {
                            builder.append('.');
                        }
                    }
                    uniqueIds.add(builder.toString());
                }
            }
            storage.commit();
            return uniqueIds.toArray(new String[uniqueIds.size()]);
        } catch (Exception e) {
            storage.rollback();
            throw new XmlServerException(e);
        }
    }

    public long deleteDocument(String revisionID, String clusterName, final String uniqueID, String documentType) throws XmlServerException {
        long start = System.currentTimeMillis();
        {
            String typeName = getTypeName(uniqueID);
            String[] splitUniqueID = uniqueID.split("\\."); //$NON-NLS-1$
            Storage storage = getStorage(clusterName, revisionID);
            ComplexTypeMetadata type = storage.getMetadataRepository().getComplexType(typeName);
            Select select = getSelectTypeById(type, revisionID, splitUniqueID, uniqueID);
            try {
                storage.begin();
                storage.delete(select);
                storage.commit();
            } catch (Exception e) {
                storage.rollback();
                throw new XmlServerException(e);
            } finally {
                storage.end();
            }
        }
        return System.currentTimeMillis() - start;
    }

    public int deleteXtentisObjects(HashMap<String, String> objectRootElementNameToRevisionID, HashMap<String, String> objectRootElementNameToClusterName, String objectRootElementName, IWhereItem whereItem) throws XmlServerException {
        // Storage does not handle MDM internal data, thus supporting this method seems useless.
        throw new UnsupportedOperationException();
    }

    public int deleteItems(LinkedHashMap<String, String> conceptPatternsToRevisionID, LinkedHashMap<String, String> conceptPatternsToClusterName, String conceptName, IWhereItem whereItem) throws XmlServerException {
        if (conceptName == null) {
            throw new IllegalArgumentException("Concept name can not be null.");
        }
        String clusterName = null;
        for (Map.Entry<String, String> entry : conceptPatternsToClusterName.entrySet()) {
            if (conceptName.matches(entry.getKey())) {
                clusterName = entry.getValue();
                break;
            }
        }
        if (clusterName == null) {
            throw new IllegalArgumentException("Could not find cluster name for concept '" + conceptName + "'.");
        }
        String revisionId = null;
        for (Map.Entry<String, String> entry : conceptPatternsToRevisionID.entrySet()) {
            if (conceptName.matches(entry.getKey())) {
                revisionId = entry.getValue();
                break;
            }
        }
        Storage storage = getStorage(clusterName, revisionId);
        MetadataRepository repository = storage.getMetadataRepository();
        ComplexTypeMetadata type = repository.getComplexType(conceptName);
        if (type == null) {
            throw new IllegalArgumentException("Type '" + conceptName + "' does not exist in cluster '" + storage.getName() + "'.");
        }
        UserQueryBuilder qb = from(type);
        qb.where(UserQueryHelper.buildCondition(qb, whereItem, repository));
        try {
            storage.begin();
            StorageResults records = storage.fetch(qb.getSelect());
            int count;
            try {
                count = records.getCount();
            } finally {
                records.close();
            }
            storage.delete(qb.getSelect());
            storage.commit();
            return count;
        } catch (Exception e) {
            storage.rollback();
            throw new XmlServerException(e);
        } finally {
            storage.end();
        }
    }

    public long moveDocumentById(String sourceRevisionID, String sourceClusterName, String uniqueID, String targetRevisionID, String targetClusterName) throws XmlServerException {
        throw new NotImplementedException();
    }

    public long countItems(Map<String, String> conceptPatternsToRevisionID, Map<String, String> conceptPatternsToClusterName, String conceptName, IWhereItem whereItem) throws XmlServerException {
        if (conceptName == null) {
            throw new IllegalArgumentException("Concept name can not be null.");
        }
        String clusterName = null;
        for (Map.Entry<String, String> entry : conceptPatternsToClusterName.entrySet()) {
            if (conceptName.matches(entry.getKey())) {
                clusterName = entry.getValue();
                break;
            }
        }
        if (clusterName == null) {
            throw new IllegalArgumentException("Could not find cluster name for concept '" + conceptName + "'.");
        }
        String revisionId = null;
        for (Map.Entry<String, String> entry : conceptPatternsToRevisionID.entrySet()) {
            if (conceptName.matches(entry.getKey())) {
                revisionId = entry.getValue();
                break;
            }
        }
        Storage storage = getStorage(clusterName, revisionId);
        MetadataRepository repository = storage.getMetadataRepository();
        Collection<ComplexTypeMetadata> types;
        if ("*".equals(conceptName)) { //$NON-NLS-1$
            types = repository.getUserComplexTypes();
        } else {
            ComplexTypeMetadata type = repository.getComplexType(conceptName);
            if (type == null) {
                throw new IllegalArgumentException("Type '" + conceptName + "' does not exist.");
            }
            types = Collections.singletonList(type);
        }
        int count = 0;
        try {
            storage.begin();
            for (ComplexTypeMetadata type : types) {
                UserQueryBuilder qb = from(type);
                qb.where(UserQueryHelper.buildCondition(qb, whereItem, repository));

                StorageResults results = storage.fetch(qb.getSelect());
                try {
                    count += results.getCount();
                } finally {
                    results.close();
                }

            }
            storage.commit();
        } catch (Exception e) {
            storage.rollback();
            throw new XmlServerException(e);
        }
        return count;
    }

    public long countXtentisObjects(HashMap<String, String> objectRootElementNameToRevisionID, HashMap<String, String> objectRootElementNameToClusterName, String mainObjectRootElementName, IWhereItem whereItem) throws XmlServerException {
        // Storage does not handle MDM internal data, thus supporting this method seems useless.
        throw new UnsupportedOperationException();
    }

    public String getItemsQuery(Map<String, String> conceptPatternsToRevisionID, Map<String, String> conceptPatternsToClusterName, String forceMainPivot, ArrayList<String> viewableFullPaths, IWhereItem whereItem, String orderBy, String direction, int start, int limit) throws XmlServerException {
        // Don't support query text stuff
        throw new UnsupportedOperationException();
    }

    public String getItemsQuery(Map<String, String> conceptPatternsToRevisionID, Map<String, String> conceptPatternsToClusterName, String forceMainPivot, ArrayList<String> viewableFullPaths, IWhereItem whereItem, String orderBy, String direction, int start, long limit, boolean totalCountOnFirstRow, Map<String, ArrayList<String>> metaDataTypes) throws XmlServerException {
        // Don't support query text stuff
        throw new UnsupportedOperationException();
    }

    public String getXtentisObjectsQuery(HashMap<String, String> objectRootElementNameToRevisionID, HashMap<String, String> objectRootElementNameToClusterName, String mainObjectRootElementName, ArrayList<String> viewableFullPaths, IWhereItem whereItem, String orderBy, String direction, int start, int limit) throws XmlServerException {
        // Don't support query text stuff
        throw new UnsupportedOperationException();
    }

    public String getXtentisObjectsQuery(LinkedHashMap<String, String> objectRootElementNameToRevisionID, LinkedHashMap<String, String> objectRootElementNameToClusterName, String mainObjectRootElementName, ArrayList<String> viewableFullPaths, IWhereItem whereItem, String orderBy, String direction, int start, long limit, boolean totalCountOnFirstRow) throws XmlServerException {
        // Don't support query text stuff
        throw new UnsupportedOperationException();
    }

    public String getPivotIndexQuery(String clusterName, String mainPivotName, LinkedHashMap<String, String[]> pivotWithKeys, LinkedHashMap<String, String> itemsRevisionIDs, String defaultRevisionID, String[] indexPaths, IWhereItem whereItem, String[] pivotDirections, String[] indexDirections, int start, int limit) throws XmlServerException {
        // Don't support query text stuff
        throw new UnsupportedOperationException();
    }

    public String getChildrenItemsQuery(String clusterName, String conceptName, String[] PKXPaths, String FKXpath, String labelXpath, String fatherPK, LinkedHashMap<String, String> itemsRevisionIDs, String defaultRevisionID, IWhereItem whereItem, int start, int limit) throws XmlServerException {
        // Don't support query text stuff
        throw new UnsupportedOperationException();
    }

    public ArrayList<String> runQuery(String revisionID, String clusterName, String query, String[] parameters) throws XmlServerException {
        return runQuery(revisionID, clusterName, query, parameters, 0, 0, false);
    }

    public ArrayList<String> runQuery(String revisionID, String clusterName, String query, String[] parameters, int start, int limit, boolean withTotalCount) throws XmlServerException {
        Storage storage = getStorage(clusterName, revisionID);
         // replace parameters in the procedure
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                String param = parameters[i];
                query = query.replaceAll("([^\\\\])%" + i + "([^\\d]*)", "$1" + param + "$2"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            }
        }
        UserQueryBuilder qb = from(query);
        storage.begin();
        ArrayList<String> resultsAsString;
        {
            StorageResults results = storage.fetch(qb.getExpression());
            resultsAsString = new ArrayList<String>(results.getSize() + 1);
            ResettableStringWriter writer = new ResettableStringWriter();
            DataRecordWriter xmlWriter = new DataRecordXmlWriter("result"); //$NON-NLS-1$
            try {
                for (DataRecord result : results) {
                    xmlWriter.write(result, writer);
                    resultsAsString.add(writer.toString());
                    writer.reset();
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not create query results", e);
            }
        }
        storage.commit();
        return resultsAsString;
    }

    public List<String> getItemPKsByCriteria(ItemPKCriteria criteria) throws XmlServerException {
        String clusterName = criteria.getClusterName();
        Storage storage = getStorage(clusterName, criteria.getRevisionId());
        MetadataRepository repository = storage.getMetadataRepository();

        int totalCount = 0;
        List<String> itemPKResults = new LinkedList<String>();
        String typeName = criteria.getConceptName();

        try {
            storage.begin();
            if (typeName != null && !typeName.isEmpty()) {
                totalCount = getTypeItemCount(criteria, repository.getComplexType(typeName), storage);
                itemPKResults.addAll(getTypeItems(criteria, repository.getComplexType(typeName), storage, repository.getComplexType(typeName).getName()));
            } else {
                // TMDM-4651: Returns type in correct dependency order.
                Collection<ComplexTypeMetadata> types = getClusterTypes(clusterName, criteria.getRevisionId());
                int maxCount = criteria.getMaxItems();
                if(criteria.getSkip() < 0) { // MDM Studio may send negative values
                    criteria.setSkip(0);
                }
                List<String> currentInstanceResults;
                for (ComplexTypeMetadata type : types) {
                    int count = getTypeItemCount(criteria, type, storage);
                    totalCount += count;
                    if(itemPKResults.size() < maxCount) {
                        if(count > criteria.getSkip()) {
                            currentInstanceResults = getTypeItems(criteria, type, storage, type.getName());
                            int n = maxCount - itemPKResults.size();
                            if (n <= currentInstanceResults.size()){
                                itemPKResults.addAll(currentInstanceResults.subList(0, n));
                            } else {
                                itemPKResults.addAll(currentInstanceResults);
                            }
                            criteria.setMaxItems(criteria.getMaxItems() - currentInstanceResults.size());
                            criteria.setSkip(0);
                        } else {
                            criteria.setSkip(criteria.getSkip() - count);
                        }
                    }
                }
            }
            itemPKResults.add(0, "<totalCount>" + totalCount + "</totalCount>");  //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            storage.commit();
        }
        return itemPKResults;
    }

    protected Collection<ComplexTypeMetadata> getClusterTypes(String clusterName, String revisionID) {
        Storage storage = getStorage(clusterName, revisionID);
        MetadataRepository repository = storage.getMetadataRepository();
        return MetadataUtils.sortTypes(repository, MetadataUtils.SortType.LENIENT);
    }

    protected int getTypeItemCount(ItemPKCriteria criteria, ComplexTypeMetadata type, Storage storage) {
        StorageResults results = storage.fetch(buildQueryBuilder(from(type).selectId(type), criteria, type).getSelect());
        try {
            return results.getCount();
        } finally {
            results.close();
        }
    }
    
    protected List<String> getTypeItems(ItemPKCriteria criteria, ComplexTypeMetadata type, Storage storage, String resultElementName) throws XmlServerException {
        // Build base query
        UserQueryBuilder qb = from(type)
                .select(alias(timestamp(), "timestamp")) //$NON-NLS-1$
                .select(alias(taskId(), "taskid")) //$NON-NLS-1$
                .selectId(type)
                .limit(criteria.getMaxItems())
                .start(criteria.getSkip());

        List<String> list = new LinkedList<String>();
        StorageResults results = storage.fetch(buildQueryBuilder(qb, criteria, type).getSelect());
        DataRecordWriter writer = new ItemPKCriteriaResultsWriter(resultElementName, type);
        ResettableStringWriter stringWriter = new ResettableStringWriter();
        for (DataRecord result : results) {
            try {
                writer.write(result, stringWriter);
            } catch (IOException e) {
                throw new XmlServerException(e);
            }
            list.add(stringWriter.toString());
            stringWriter.reset();
        }
        return list;
    }

    private UserQueryBuilder buildQueryBuilder(UserQueryBuilder qb, ItemPKCriteria criteria, ComplexTypeMetadata type) {
        // Filter by keys: expected format here: $EntityTypeName/Path/To/Field$[id_for_lookup]
        String keysKeywords = criteria.getKeysKeywords();
        if (keysKeywords != null && !keysKeywords.isEmpty()) {
            if (keysKeywords.charAt(0) == '$') {
                char[] chars = keysKeywords.toCharArray();
                List<String> paths = new LinkedList<String>();
                StringBuilder currentPath = new StringBuilder();
                StringBuilder id = new StringBuilder();
                StringBuilder idForLookup = new StringBuilder();
                int dollarCount = 0;
                int slashCount = 0;
                for (char current : chars) {
                    switch (current) {
                        case '$':
                            dollarCount++;
                            break;
                        case ',':
                            paths.add(currentPath.toString());
                            currentPath = new StringBuilder();
                            slashCount = 0;
                            break;
                        case '/':
                            slashCount++;
                        default:
                            if (dollarCount == 0) {
                                id.append(current);
                            } else if (dollarCount >= 2) {
                                idForLookup.append(current);
                            } else if (slashCount > 0) {
                                if (slashCount != 1 || '/' != current) {
                                    currentPath.append(current);
                                }
                            }
                    }
                }
                paths.add(currentPath.toString());
                if (currentPath.toString().isEmpty()) {
                    // TODO Implement compound key support
                    qb.where(eq(type.getKeyFields().iterator().next(), id.toString()));
                } else {
                    Condition globalCondition = UserQueryHelper.FALSE;
                    for (String path : paths) {
                        Condition pathCondition = UserQueryHelper.FALSE;
                        Set<FieldMetadata> candidateFields = Collections.singleton(type.getField(path));
                        for (FieldMetadata candidateField : candidateFields) {
                            if (candidateField instanceof ReferenceFieldMetadata
                                    && ((ReferenceFieldMetadata) candidateField).getReferencedField() instanceof CompoundFieldMetadata) {
                                // composite key: fkValue of the form '[value1.value2.value3...]'
                                pathCondition = or(eq(candidateField, StringUtils.replace(idForLookup.toString(), ".", "][")), pathCondition); //$NON-NLS-1$ //$NON-NLS-2$
                            } else {
                                pathCondition = or(eq(candidateField, idForLookup.toString()), pathCondition);
                            }
                        }
                        globalCondition = or(globalCondition, pathCondition);
                    }
                    qb.where(globalCondition);
                }
            } else {
                Collection<FieldMetadata> keyFields = type.getKeyFields();
                if (criteria.getClusterName().equals(XSystemObjects.DC_UPDATE_PREPORT.getName()) && type.getName().equals("Update")) { //$NON-NLS-1$
                    // UpdateReport: Source.TimeInMillis is the key
                    String[] keys = keysKeywords.split("\\."); //$NON-NLS-1$
                    if (keys.length == 1 || keys.length > 2) {
                        throw new IllegalArgumentException("The key format is 'Source.TimeInMillis' for type " + type.getName()); //$NON-NLS-1$
                    } else if (keys.length == 2) {
                        if (keys[1] == null || keys[1].trim().isEmpty() || !StorageMetadataUtils.isValueAssignable(keys[1], Timestamp.INSTANCE.getTypeName())) {
                            throw new IllegalArgumentException("The key format is 'Source.TimeInMillis' for type '" + type.getName() + "'" +  //$NON-NLS-1$//$NON-NLS-2$
                            		" and the TimeInMillis key value must be a long type."); //$NON-NLS-1$
                        }
                        int i = 0;
                        for (FieldMetadata keyField : keyFields) {
                            qb.where(eq(keyField, keys[i]));
                            i++;
                        }
                    }
                } else {
                    if (keyFields.size() > 1) {
                        throw new IllegalArgumentException("Expected type '" + type.getName() + "' to contain only 1 key field."); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    String uniqueKeyFieldName = keyFields.iterator().next().getName();
                    qb.where(eq(type.getField(uniqueKeyFieldName), keysKeywords));    
                }
                
            }
        }
        // Filter by timestamp
        if (criteria.getFromDate() > 0) {
            qb.where(gte(timestamp(), String.valueOf(criteria.getFromDate())));
        }
        if (criteria.getToDate() > 0) {
            qb.where(lte(timestamp(), String.valueOf(criteria.getToDate())));
        }
        // Content keywords
        String contentKeywords = criteria.getContentKeywords();
        if (contentKeywords != null && !contentKeywords.isEmpty()) {
            if (criteria.isUseFTSearch()) {
                qb.where(fullText(contentKeywords));
            } else {
                Condition condition = null;
                for (FieldMetadata field : type.getFields()) {
                    if (StorageMetadataUtils.isValueAssignable(contentKeywords, field.getType().getName())) {
                        if (!(field instanceof ContainedTypeFieldMetadata)) {
                            if (condition == null) {
                                condition = contains(field, contentKeywords);
                            } else {
                                condition = or(condition, contains(field, contentKeywords));
                            }
                        }
                    }
                }
                if(condition != null) {
                  qb.where(condition);
                }
            }
        }
        
        return qb;
    }
    
    public void clearCache() {
    }

    public boolean supportTransaction() {
        return true;
    }

    private StorageAdmin getStorageAdmin() {
        if (storageAdmin == null) {
            storageAdmin = ServerContext.INSTANCE.get().getStorageAdmin();
        }
        return storageAdmin;
    }

    protected Storage getStorage(String dataClusterName) {
        return getStorage(dataClusterName, null);
    }

    protected Storage getStorage(String dataClusterName, String revisionId) {
        StorageAdmin admin = getStorageAdmin();
        if (!admin.exist(dataClusterName, admin.getType(dataClusterName), revisionId)) {
            throw new IllegalStateException("Data container '" + dataClusterName + "' (revision: '" + revisionId + "') does not exist.");
        }
        return admin.get(dataClusterName, admin.getType(dataClusterName), revisionId);
    }

    public void start(String dataClusterName) throws XmlServerException {
        Storage storage = getStorage(dataClusterName);
        storage.begin();
    }

    public void commit(String dataClusterName) throws XmlServerException {
        Storage storage = getStorage(dataClusterName);
        storage.commit();
    }

    public void rollback(String dataClusterName) throws XmlServerException {
        Storage storage = getStorage(dataClusterName);
        storage.rollback();
    }

    public void end(String dataClusterName) throws XmlServerException {
        Storage storage = getStorage(dataClusterName);
        storage.end();
    }

    public void close() throws XmlServerException {
        getStorageAdmin().close();
    }

    public List<String> globalSearch(String dataCluster, String keyword, int start, int end) throws XmlServerException {
        Storage storage = getStorage(dataCluster, null);
        MetadataRepository repository = storage.getMetadataRepository();
        Iterator<ComplexTypeMetadata> types = repository.getUserComplexTypes().iterator();
        if (types.hasNext()) {
            ComplexTypeMetadata mainType = types.next();
            while (types.hasNext() && mainType.getKeyFields().size() > 1) {
                ComplexTypeMetadata next = types.next();
                if (next.getKeyFields().size() > 1) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Ignoring type '" + next.getName() + "' (compound key).");
                    }
                }
                mainType = next;
            }
            UserQueryBuilder qb = from(mainType);
            while (types.hasNext()) {
                ComplexTypeMetadata additionalType = types.next();
                if (additionalType.getKeyFields().size() == 1) {
                    qb.and(additionalType);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Ignoring type '" + additionalType.getName() + "' (compound key).");
                    }
                }
            }
            qb.where(fullText(keyword));
            qb.start(start);
            qb.limit(end);
            List<String> resultsAsXmlStrings = new LinkedList<String>();
            storage.begin();
            StorageResults results = null;
            try {
                results = Split.fetchAndMerge(storage, qb.getSelect()); // TMDM-7290: Split main query into smaller queries.
                DataRecordWriter writer = new FullTextResultsWriter(keyword);
                resultsAsXmlStrings.add(String.valueOf(results.getCount()));
                for (DataRecord result : results) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    writer.write(result, output);
                    output.flush();
                    resultsAsXmlStrings.add(output.toString());
                }
                storage.commit();
                return resultsAsXmlStrings;
            } catch (IOException e) {
                storage.rollback();
                throw new XmlServerException(e);
            } finally {
                if (results != null) {
                    results.close();
                }
            }
        } else {
            return Collections.emptyList();
        }
    }

    public void exportDocuments(String revisionId, String clusterName, int start, int end, boolean includeMetadata, OutputStream outputStream) throws XmlServerException {
        // No support for bulk export when using SQL storages (this could be in HibernateStorage but would require to define new API).
        throw new NotImplementedException("No support for bulk export.");
    }
}
