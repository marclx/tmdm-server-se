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

import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.datasource.RDBMSDataSource;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

@SuppressWarnings("UnusedDeclaration")
// Dynamically called! Do not remove!
public class DefaultStorageClassLoader extends StorageClassLoader {

    private static final Logger LOGGER = Logger.getLogger(DefaultStorageClassLoader.class);

    private static final XPath pathFactory = XPathFactory.newInstance().newXPath();

    public DefaultStorageClassLoader(ClassLoader parent,
                                     String storageName,
                                     StorageType type) {
        super(parent, storageName, type);
    }

    public InputStream generateEhCacheConfig() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            Document document = documentBuilder.parse(this.getClass().getResourceAsStream(EHCACHE_XML_CONFIG));
            // <diskStore path="java.io.tmpdir"/>
            XPathExpression compile = pathFactory.compile("ehcache/diskStore"); //$NON-NLS-1$
            Node node = (Node) compile.evaluate(document, XPathConstants.NODE);
            node.getAttributes().getNamedItem("path").setNodeValue(dataSource.getCacheDirectory() + '/' + dataSource.getName()); //$NON-NLS-1$
            OutputFormat format = new OutputFormat(document);
            StringWriter stringOut = new StringWriter();
            XMLSerializer serial = new XMLSerializer(stringOut, format);
            serial.serialize(document);
            return new ByteArrayInputStream(stringOut.toString().getBytes("UTF-8")); //$NON-NLS-1$
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream generateHibernateMapping() {
        if (resolver == null) {
            throw new IllegalStateException("Expected table resolver to be set before this method is called.");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            documentBuilder.setEntityResolver(HibernateStorage.ENTITY_RESOLVER);
            Document document = documentBuilder.parse(this.getClass().getResourceAsStream(HIBERNATE_MAPPING_TEMPLATE));
            MappingGenerator mappingGenerator = getMappingGenerator(document, resolver);
            for (Map.Entry<String, Class<? extends Wrapper>> classNameToClass : registeredClasses.entrySet()) {
                ComplexTypeMetadata typeMetadata = knownTypes.get(classNameToClass.getKey());
                if (typeMetadata != null && typeMetadata.getSuperTypes().isEmpty()) {
                    Element classElement = typeMetadata.accept(mappingGenerator);
                    if (classElement != null) { // Class element might be null if mapping is not applicable for this type
                        document.getDocumentElement().appendChild(classElement);
                    }
                }
            }
            OutputFormat format = new OutputFormat(document);
            StringWriter stringOut = new StringWriter();
            XMLSerializer serial = new XMLSerializer(stringOut, format);
            serial.serialize(document);
            return new ByteArrayInputStream(stringOut.toString().getBytes("UTF-8")); //$NON-NLS-1$
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MappingGenerator getMappingGenerator(Document document, TableResolver resolver) {
        switch (type) {
            case MASTER:
            case SYSTEM:
            case STAGING:
                return new MappingGenerator(document, resolver, dataSource);
            default:
                throw new NotImplementedException("No support for storage type '" + type + "'.");
        }
    }

    public InputStream generateHibernateConfig() {
        try {
            Document document = generateHibernateConfiguration(dataSource);
            OutputFormat format = new OutputFormat(document);
            StringWriter stringOut = new StringWriter();
            XMLSerializer serial = new XMLSerializer(stringOut, format);
            serial.serialize(document);
            return new ByteArrayInputStream(stringOut.toString().getBytes("UTF-8")); //$NON-NLS-1$
        } catch (Exception e) {
            throw new RuntimeException("Could not generate Hibernate configuration", e);
        }
    }

    public Document generateHibernateConfiguration(RDBMSDataSource rdbmsDataSource) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);

        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        documentBuilder.setEntityResolver(HibernateStorage.ENTITY_RESOLVER);
        Document document = documentBuilder.parse(DefaultStorageClassLoader.class.getResourceAsStream(HIBERNATE_CONFIG_TEMPLATE));
        // Override default configuration with values from configuration
        Map<String, String> advancedProperties = rdbmsDataSource.getAdvancedProperties();
        for (Map.Entry<String, String> currentAdvancedProperty : advancedProperties.entrySet()) {
            setPropertyValue(document, currentAdvancedProperty.getKey(), currentAdvancedProperty.getValue());
        }
        String connectionUrl = rdbmsDataSource.getConnectionURL();
        String userName = rdbmsDataSource.getUserName();
        String driverClass = rdbmsDataSource.getDriverClassName();
        RDBMSDataSource.DataSourceDialect dialectType = rdbmsDataSource.getDialectName();
        String dialect = getDialect(dialectType);
        String password = rdbmsDataSource.getPassword();
        String indexBase = rdbmsDataSource.getIndexDirectory();
        int connectionPoolMinSize = rdbmsDataSource.getConnectionPoolMinSize();
        int connectionPoolMaxSize = rdbmsDataSource.getConnectionPoolMaxSize();

        setPropertyValue(document, "hibernate.connection.url", connectionUrl); //$NON-NLS-1$
        setPropertyValue(document, "hibernate.connection.username", userName); //$NON-NLS-1$
        setPropertyValue(document, "hibernate.connection.driver_class", driverClass); //$NON-NLS-1$
        setPropertyValue(document, "hibernate.dialect", dialect); //$NON-NLS-1$
        setPropertyValue(document, "hibernate.connection.password", password); //$NON-NLS-1$
        
        if (connectionPoolMinSize > 0 && connectionPoolMaxSize > 0 && connectionPoolMaxSize >= connectionPoolMinSize) {
            setPropertyValue(document, "hibernate.c3p0.min_size", String.valueOf(connectionPoolMinSize)); //$NON-NLS-1$
            setPropertyValue(document, "hibernate.c3p0.max_size", String.valueOf(connectionPoolMaxSize)); //$NON-NLS-1$
        }
        if (connectionPoolMaxSize < connectionPoolMinSize) {
            LOGGER.warn("Connection max size pool for datasource '" + rdbmsDataSource.getName() + "' is incorrect (lower than min size). " + //$NON-NLS-1$//$NON-NLS-2$
                    "Connection pool is created by using the system default value (minSize=5, maxSize=50)."); //$NON-NLS-1$
        }

        Node sessionFactoryElement = document.getElementsByTagName("session-factory").item(0); //$NON-NLS-1$
        if (rdbmsDataSource.supportFullText()) {
            addProperty(document, sessionFactoryElement, "hibernate.search.default.directory_provider", "org.hibernate.search.store.FSDirectoryProvider"); //$NON-NLS-1$ //$NON-NLS-2$
            addProperty(document, sessionFactoryElement, "hibernate.search.default.indexBase", indexBase + '/' + storageName); //$NON-NLS-1$
            addProperty(document, sessionFactoryElement, "hibernate.search.default.sourceBase", indexBase + '/' + storageName); //$NON-NLS-1$
            addProperty(document, sessionFactoryElement, "hibernate.search.default.source", ""); //$NON-NLS-1$ //$NON-NLS-2$
            addProperty(document, sessionFactoryElement, "hibernate.search.default.exclusive_index_use", "false"); //$NON-NLS-1$ //$NON-NLS-2$
            addProperty(document, sessionFactoryElement, "hibernate.search.analyzer", "com.amalto.core.storage.hibernate.MDMStandardAnalyzer"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (dataSource.getCacheDirectory() != null && !dataSource.getCacheDirectory().isEmpty()) {
            /*
            <!-- Second level cache -->
            <property name="hibernate.cache.use_second_level_cache">true</property>
            <property name="hibernate.cache.provider_class">net.sf.ehcache.hibernate.EhCacheProvider</property>
            <property name="hibernate.cache.use_query_cache">true</property>
            <property name="net.sf.ehcache.configurationResourceName">ehcache.xml</property>
             */
            addProperty(document, sessionFactoryElement, "hibernate.cache.use_second_level_cache", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            addProperty(document, sessionFactoryElement, "hibernate.cache.provider_class", "net.sf.ehcache.hibernate.EhCacheProvider"); //$NON-NLS-1$ //$NON-NLS-2$
            addProperty(document, sessionFactoryElement, "hibernate.cache.use_query_cache", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            addProperty(document, sessionFactoryElement, "net.sf.ehcache.configurationResourceName", "ehcache.xml"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Hibernate configuration does not define second level cache extensions due to datasource configuration."); //$NON-NLS-1$
            }
            addProperty(document, sessionFactoryElement, "hibernate.cache.use_second_level_cache", "false"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Order of elements highly matters and mapping shall be declared after <property/> and before <event/>.
        Element mapping = document.createElement("mapping"); //$NON-NLS-1$
        Attr resource = document.createAttribute("resource"); //$NON-NLS-1$
        resource.setValue(HIBERNATE_MAPPING);
        mapping.getAttributes().setNamedItem(resource);
        sessionFactoryElement.appendChild(mapping);

        if (rdbmsDataSource.supportFullText()) {
            addEvent(document, sessionFactoryElement, "post-update", "org.hibernate.search.event.FullTextIndexEventListener"); //$NON-NLS-1$ //$NON-NLS-2$
            addEvent(document, sessionFactoryElement, "post-insert", "org.hibernate.search.event.FullTextIndexEventListener"); //$NON-NLS-1$ //$NON-NLS-2$
            addEvent(document, sessionFactoryElement, "post-delete", "org.hibernate.search.event.FullTextIndexEventListener"); //$NON-NLS-1$ //$NON-NLS-2$
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Hibernate configuration does not define full text extensions due to datasource configuration."); //$NON-NLS-1$
        }

        return document;
    }

    protected String getDialect(RDBMSDataSource.DataSourceDialect dialectType) {
        switch (dialectType) {
            case H2:
                // Default Hibernate configuration for Hibernate forgot some JDBC type mapping.
                return H2CustomDialect.class.getName();
            default:
                throw new IllegalArgumentException("Not supported database type '" + dialectType + "'");
        }
    }

    private static void addEvent(Document document, Node sessionFactoryElement, String eventType, String listenerClass) {
        Element event = document.createElement("event"); //$NON-NLS-1$
        Attr type = document.createAttribute("type"); //$NON-NLS-1$
        type.setValue(eventType);
        event.getAttributes().setNamedItem(type);
        {
            Element listener = document.createElement("listener"); //$NON-NLS-1$
            Attr clazz = document.createAttribute("class"); //$NON-NLS-1$
            clazz.setValue(listenerClass);
            listener.getAttributes().setNamedItem(clazz);
            event.appendChild(listener);
        }
        sessionFactoryElement.appendChild(event);
    }

    private static void addProperty(Document document, Node sessionFactoryElement, String propertyName, String propertyValue) {
        Element property = document.createElement("property"); //$NON-NLS-1$
        Attr name = document.createAttribute("name"); //$NON-NLS-1$
        name.setValue(propertyName);
        property.getAttributes().setNamedItem(name);
        property.appendChild(document.createTextNode(propertyValue));
        sessionFactoryElement.appendChild(property);
    }

    private static void setPropertyValue(Document document, String propertyName, String value) throws XPathExpressionException {
        XPathExpression compile = pathFactory.compile("hibernate-configuration/session-factory/property[@name='" + propertyName + "']"); //$NON-NLS-1$ //$NON-NLS-2$
        Node node = (Node) compile.evaluate(document, XPathConstants.NODE);
        if (node != null) {
            node.setTextContent(value);
        } else {
            XPathExpression parentNodeExpression = pathFactory.compile("hibernate-configuration/session-factory"); //$NON-NLS-1$
            Node parentNode = (Node) parentNodeExpression.evaluate(document, XPathConstants.NODE);
            // Create a new property element for this datasource-specified property (TMDM-4927).
            Element property = document.createElement("property"); //$NON-NLS-1$
            Attr propertyNameAttribute = document.createAttribute("name"); //$NON-NLS-1$
            property.setAttributeNode(propertyNameAttribute);
            propertyNameAttribute.setValue(propertyName);
            property.setTextContent(value);
            parentNode.appendChild(property);
        }
    }
}
