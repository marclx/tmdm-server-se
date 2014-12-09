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

package com.amalto.core.save.context;

import com.amalto.core.ejb.ItemPOJOPK;
import com.amalto.core.history.MutableDocument;
import com.amalto.core.save.generator.AutoIncrementGenerator;
import org.talend.mdm.commmon.metadata.MetadataUtils;
import com.amalto.core.save.DOMDocument;
import com.amalto.core.schema.validation.SkipAttributeDocumentBuilder;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;
import com.amalto.core.objects.datacluster.ejb.DataClusterPOJOPK;
import com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK;
import com.amalto.core.objects.routing.v2.ejb.local.RoutingEngineV2CtrlLocal;
import com.amalto.core.save.DocumentSaverContext;
import com.amalto.core.schema.validation.XmlSchemaValidator;
import com.amalto.core.server.DataModel;
import com.amalto.core.server.MetadataRepositoryAdmin;
import com.amalto.core.server.ServerContext;
import com.amalto.core.server.XmlServer;
import com.amalto.core.servlet.LoadServlet;
import com.amalto.core.util.*;
import org.talend.mdm.commmon.metadata.TypeMetadata;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultSaverSource implements SaverSource {

    private final XmlServer database;

    private final DataModel dataModel;

    private final Map<String, String> schemasAsString = new HashMap<String, String>();

    private final String userName;

    public DefaultSaverSource() {
        this(null);
    }

    public DefaultSaverSource(String userName) {
        try {
            database = Util.getXmlServerCtrlLocal();
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }

        try {
            dataModel = Util.getDataModelCtrlLocal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.userName = userName;
    }

    public static SaverSource getDefault() {
        if (isFullSQL()) {
            return new StorageSaverSource();
        } else {
            return new DefaultSaverSource();
        }
    }

    private static boolean isFullSQL() {
        return MDMConfiguration.isSqlDataBase()
                && "com.amalto.core.storage.SQLWrapper".equals(MDMConfiguration.getConfiguration().get("xmlserver.class"));  //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static SaverSource getDefault(String userName) {
         if (isFullSQL()) {
            return new StorageSaverSource(userName);
        } else {
            return new DefaultSaverSource(userName);
        }
    }

    public MutableDocument get(String dataClusterName, String dataModelName, String typeName, String revisionId, String[] key) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append(dataClusterName).append('.').append(typeName).append('.');
            for (int i = 0; i < key.length; i++) {
                builder.append(key[i]);
                if (i < key.length - 1) {
                    builder.append('.');
                }
            }
            String uniqueId = builder.toString();
            String documentAsString = database.getDocumentAsString(revisionId, dataClusterName, uniqueId, "UTF-8"); //$NON-NLS-1$
            if (documentAsString == null) {
                return null;
            }
            DocumentBuilder documentBuilder;
            documentBuilder = new SkipAttributeDocumentBuilder(SaverContextFactory.DOCUMENT_BUILDER, false);
            ComplexTypeMetadata type = ServerContext.INSTANCE.get().getMetadataRepositoryAdmin().get(dataModelName).getComplexType(typeName);
            Document databaseDomDocument = documentBuilder.parse(new ByteArrayInputStream(documentAsString.getBytes("UTF-8")));
            Element userXmlElement = getUserXmlElement(databaseDomDocument);
            return new DOMDocument(userXmlElement, type, revisionId, dataClusterName, dataModelName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Element getUserXmlElement(Document databaseDomDocument) {
        NodeList userXmlPayloadElement = databaseDomDocument.getElementsByTagName("p"); //$NON-NLS-1$
        if (userXmlPayloadElement.getLength() > 1) {
            throw new IllegalStateException("Document has multiple payload elements.");
        }
        Node current = userXmlPayloadElement.item(0).getFirstChild();
        while (current != null) {
            if (current instanceof Element) {
                Element newRoot = (Element) current;
                NamedNodeMap attrs = databaseDomDocument.getDocumentElement().getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attrNode = attrs.item(i);
                    newRoot.setAttributeNS(attrNode.getNamespaceURI(), attrNode.getNodeName(), attrNode.getNodeValue());
                }
                return (Element) current;
            }
            current = current.getNextSibling();
        }
        throw new IllegalStateException("Element 'p' is expected to have an XML element as child.");
    }

    public boolean exist(String dataCluster, String dataModelName, String typeName, String revisionId, String[] key) {
        return get(dataCluster, dataModelName, typeName, revisionId, key) != null;
    }

    public MetadataRepository getMetadataRepository(String dataModelName) {
        try {
            MetadataRepositoryAdmin admin = ServerContext.INSTANCE.get().getMetadataRepositoryAdmin();
            return admin.get(dataModelName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getSchema(String dataModelName) {
        try {
            synchronized (schemasAsString) {
                if (schemasAsString.get(dataModelName) == null) {
                    String schemaAsString = dataModel.getDataModel(new DataModelPOJOPK(dataModelName)).getSchema();
                    schemasAsString.put(dataModelName, schemaAsString);
                }
            }
            return new ByteArrayInputStream(schemasAsString.get(dataModelName).getBytes("UTF-8")); //$NON-NLS-1$
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUniverse() {
        try {
            return LocalUser.getLocalUser().getUniverse().getName();
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public OutputReport invokeBeforeSaving(DocumentSaverContext context, MutableDocument updateReportDocument) {
        try {
            return Util.beforeSaving(context.getUserDocument().getType().getName(),
                    context.getDatabaseDocument().exportToString(),
                    updateReportDocument.exportToString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getCurrentUserRoles() {
        try {
            // get user roles from current user.
            return LocalUser.getLocalUser().getRoles();
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserName() {
        try {
            return LocalUser.getLocalUser().getUsername();
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean existCluster(String revisionID, String dataClusterName) {
        try {
            return database.existCluster(revisionID, dataClusterName);
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public String getConceptRevisionID(String typeName) {
        try {
            return LocalUser.getLocalUser().getUniverse().getConceptRevisionID(typeName);
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetLocalUsers() {
        try {
            LocalUser.resetLocalUsers();
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public void initAutoIncrement() {
        AutoIncrementGenerator.get().init();
    }

    public void routeItem(String dataCluster, String typeName, String[] id) {
        try {
            RoutingEngineV2CtrlLocal ctrl = Util.getRoutingEngineV2CtrlLocal();
            DataClusterPOJOPK dataClusterPOJOPK = new DataClusterPOJOPK(dataCluster);
            ctrl.route(new ItemPOJOPK(dataClusterPOJOPK, typeName, id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void invalidateTypeCache(String dataModelName) {
        XmlSchemaValidator.invalidateCache(dataModelName);
        synchronized (schemasAsString) {
            schemasAsString.remove(dataModelName);
        }
        synchronized (LoadServlet.typeNameToKeyDef) {
            LoadServlet.typeNameToKeyDef.clear();
        }
    }

    public void saveAutoIncrement() {
        try {
            AutoIncrementGenerator.get().saveState(Util.getXmlServerCtrlLocal());
        } catch (XtentisException e) {
            throw new RuntimeException("Unable to save auto increment value.", e);
        }
    }

    public String nextAutoIncrementId(String universe, String dataCluster, String dataModelName, String conceptName) {
        String autoIncrementId = null;
        String concept;
        String field;
        if (conceptName.contains(".")) { //$NON-NLS-1$
            String[] conceptArray = conceptName.split("\\."); //$NON-NLS-1$
            concept = conceptArray[0];
            field = conceptArray[1];
        } else {
            concept = conceptName;
            field = null;
        }
        MetadataRepository metadataRepository = getMetadataRepository(dataModelName);
        if (metadataRepository != null) {
            ComplexTypeMetadata complexType = metadataRepository.getComplexType(concept);
            if (complexType != null) {
                TypeMetadata superType = MetadataUtils.getSuperConcreteType(complexType);
                if (superType != null) {
                    concept = superType.getName();
                }
                String autoIncrementFieldName = field != null ? concept + "." + field : concept; //$NON-NLS-1$
                autoIncrementId = AutoIncrementGenerator.get().generateId(dataCluster, conceptName, autoIncrementFieldName);
            } 
        }
        return autoIncrementId;
    }

    public String getLegitimateUser() {
        // web service caller
        if (userName != null) {
            return userName;
        }
        return getUserName();
    }

}
