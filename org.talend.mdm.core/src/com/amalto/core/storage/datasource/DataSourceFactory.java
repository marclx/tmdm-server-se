/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.storage.datasource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amalto.core.server.DataSourceExtension;

public class DataSourceFactory {

    public static final String DB_DATASOURCES = "db.datasources"; //$NON-NLS-1$

    private static final DataSourceFactory INSTANCE = new DataSourceFactory();

    private static final Logger LOGGER = Logger.getLogger(DataSourceFactory.class);

    private static final String REVISION_PLACEHOLDER = "${revision}"; //$NON-NLS-1$

    private static final String CONTAINER_PLACEHOLDER = "${container}"; //$NON-NLS-1$

    private static final XPath xPath = XPathFactory.newInstance().newXPath();

    private static DocumentBuilderFactory factory;

    private DataSourceFactory() {
        factory = DocumentBuilderFactory.newInstance();
    }

    public static DataSourceFactory getInstance() {
        return INSTANCE;
    }

    private static void replacePlaceholder(DataSource dataSource, String placeholderName, String value) {
        if (dataSource instanceof RDBMSDataSource) {
            RDBMSDataSource rdbmsDataSource = (RDBMSDataSource) dataSource;
            // JDBC URL
            String connectionURL = rdbmsDataSource.getConnectionURL();
            String processedConnectionURL;
            RDBMSDataSource.DataSourceDialect dialect = ((RDBMSDataSource) dataSource).getDialectName();
            switch (dialect) {
                case POSTGRES:
                    // Postgres always creates lower case database name
                    processedConnectionURL = connectionURL.replace(placeholderName, value).toLowerCase();
                    break;
                case MYSQL:
                    // TMDM-6559: MySQL doesn't like '-' in database name
                    processedConnectionURL = connectionURL.replace(placeholderName, value);
                    if (processedConnectionURL.indexOf('-') > 0) {
                        // Uses URI-based parsing to prevent replace of '-' in host name.
                        URI uri = URI.create(processedConnectionURL.substring(5));
                        if (uri.getPath().indexOf('-') > 0) {
                            String previousURL = processedConnectionURL;
                            processedConnectionURL = processedConnectionURL.replace(uri.getPath(), uri.getPath().replace('-', '_'));
                            LOGGER.warn("JDBC URL '" + previousURL + "' contains character(s) not supported by MySQL (replaced with '" + processedConnectionURL + "' by MDM).");
                        }
                    }
                    break;
                case H2:
                case ORACLE_10G:
                case SQL_SERVER:
                default: // default for all databases
                    processedConnectionURL = connectionURL.replace(placeholderName, value);
                    break;
            }
            rdbmsDataSource.setConnectionURL(processedConnectionURL);
            // Database name
            String databaseName = rdbmsDataSource.getDatabaseName();
            String processedDatabaseName = databaseName.replace(placeholderName, value);
            switch (dialect) {
                case POSTGRES:
                    // Postgres always creates lower case database name
                    processedDatabaseName = processedDatabaseName.toLowerCase();
                    break;
                case MYSQL:
                    if (processedDatabaseName.indexOf('-') > 0) {
                        LOGGER.warn("Database name '" + processedDatabaseName + "' contains character(s) not supported by MySQL.");
                    }
                    processedDatabaseName = processedDatabaseName.replace('-', '_'); // TMDM-6559: MySQL doesn't like '-' in
                    // database name
                    break;
                case H2:
                case ORACLE_10G:
                case SQL_SERVER:
                case DB2:
                default:
                    // Nothing to do for other databases
                    break;
            }
            rdbmsDataSource.setDatabaseName(processedDatabaseName);
            // User name
            rdbmsDataSource.setUserName(rdbmsDataSource.getUserName().replace(placeholderName, value));
            // Advanced properties
            Map<String, String> advancedProperties = rdbmsDataSource.getAdvancedProperties();
            for (Map.Entry<String, String> entry : advancedProperties.entrySet()) {
                advancedProperties.put(entry.getKey(), entry.getValue().replace(placeholderName, value));
            }
        }
    }

    private static synchronized InputStream readDataSourcesConfiguration() {
        Properties configuration = MDMConfiguration.getConfiguration();
        String dataSourcesFileName = (String) configuration.get(DB_DATASOURCES);
        if (dataSourcesFileName == null) { // DB_DATASOURCES property is mandatory to continue.
            throw new IllegalStateException(DB_DATASOURCES + " is not defined in MDM configuration.");
        }
        InputStream configurationAsStream = null;
        // 1- Try from file (direct lookup)
        File file = new File(dataSourcesFileName);
        if (file.exists()) {
            LOGGER.info("Reading from datasource file at '" + file.getAbsolutePath() + "'."); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                configurationAsStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Unexpected state (file exists but can't create a stream from it).", e);
            }
        }
        // 1- Try from file (from the JBoss configuration directory)
        if (configurationAsStream == null) {
            String jbossServerDir = System.getProperty("jboss.server.home.dir"); //$NON-NLS-1$
            if (jbossServerDir != null) {
                file = new File(jbossServerDir + File.separator + "conf", dataSourcesFileName); //$NON-NLS-1$
                LOGGER.info("Reading from datasource file at '" + file.getAbsolutePath() + "'."); //$NON-NLS-1$ //$NON-NLS-2$
                if (file.exists()) {
                    try {
                        configurationAsStream = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        throw new IllegalStateException("Unexpected state (file exists but can't create a stream from it).", e);
                    }
                }
            }
        }
        // 2- From class path
        if (configurationAsStream == null) {
            List<String> filePaths = Arrays.asList(dataSourcesFileName);
            Iterator<String> iterator = filePaths.iterator();

            String currentFilePath = StringUtils.EMPTY;
            while (configurationAsStream == null && iterator.hasNext()) {
                currentFilePath = iterator.next();
                configurationAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(currentFilePath);
            }
            if (configurationAsStream != null) {
                LOGGER.info("Reading from datasource file at '" + currentFilePath + "'."); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        // 3- error: configuration was not found
        if (configurationAsStream == null) {
            throw new IllegalStateException("Could not find datasources configuration file '" + dataSourcesFileName + "'.");
        }
        return configurationAsStream;
    }

    private static Map<String, DataSourceDefinition> readDocument(InputStream configurationAsStream) {
        Document document;
        try {
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            document = documentBuilder.parse(configurationAsStream);
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred during data sources XML configuration parsing", e);
        }
        try {
            NodeList datasources = (NodeList) evaluate(document, "/datasources/datasource", XPathConstants.NODESET);
            Map<String, DataSourceDefinition> nameToDataSources = new HashMap<String, DataSourceDefinition>();
            for (int i = 0; i < datasources.getLength(); i++) {
                Node currentDataSourceElement = datasources.item(i);
                String name = (String) evaluate(currentDataSourceElement, "@name", XPathConstants.STRING); //$NON-NLS-1$
                DataSource master = getDataSourceConfiguration(currentDataSourceElement, name, "master"); //$NON-NLS-1$
                if (master == null) {
                    throw new IllegalArgumentException("Data source '" + name + "'does not declare a master data section");
                }
                DataSource staging = getDataSourceConfiguration(currentDataSourceElement, name, "staging"); //$NON-NLS-1$
                DataSource system = getDataSourceConfiguration(currentDataSourceElement, name, "system"); //$NON-NLS-1$
                nameToDataSources.put(name, new DataSourceDefinition(master, staging, system));
            }
            return nameToDataSources;
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Invalid data sources configuration.", e);
        }
    }

    private static DataSource getDataSourceConfiguration(Node document, String name, String path) throws XPathExpressionException {
        Node dataSource = (Node) evaluate(document, path, XPathConstants.NODE);
        if (dataSource == null) {
            return null;
        }
        String type = (String) evaluate(dataSource, "type", XPathConstants.STRING); //$NON-NLS-1$
        if ("RDBMS".equals(type)) { //$NON-NLS-1$
            String dialectName = (String) evaluate(dataSource, "rdbms-configuration/dialect", XPathConstants.STRING); //$NON-NLS-1$
            String driverClassName = (String) evaluate(dataSource,
                    "rdbms-configuration/connection-driver-class", XPathConstants.STRING); //$NON-NLS-1$
            String connectionURL = (String) evaluate(dataSource, "rdbms-configuration/connection-url", XPathConstants.STRING); //$NON-NLS-1$
            String userName = (String) evaluate(dataSource, "rdbms-configuration/connection-username", XPathConstants.STRING); //$NON-NLS-1$
            String password = (String) evaluate(dataSource, "rdbms-configuration/connection-password", XPathConstants.STRING); //$NON-NLS-1$
            int connectionPoolMinSize = ((Double) evaluate(dataSource,
                    "rdbms-configuration/connection-pool-minsize", XPathConstants.NUMBER)).intValue(); //$NON-NLS-1$
            int connectionPoolMaxSize = ((Double) evaluate(dataSource,
                    "rdbms-configuration/connection-pool-maxsize", XPathConstants.NUMBER)).intValue(); //$NON-NLS-1$
            String indexDirectory = (String) evaluate(dataSource,
                    "rdbms-configuration/fulltext-index-directory", XPathConstants.STRING); //$NON-NLS-1$
            String cacheDirectory = (String) evaluate(dataSource, "rdbms-configuration/cache-directory", XPathConstants.STRING); //$NON-NLS-1$
            String caseSensitivity = (String) evaluate(dataSource,
                    "rdbms-configuration/case-sensitive-search", XPathConstants.STRING); //$NON-NLS-1$
            Boolean caseSensitiveSearch = caseSensitivity == null || caseSensitivity.isEmpty()
                    || Boolean.parseBoolean(caseSensitivity);
            String schemaGeneration = (String) evaluate(dataSource,
                    "rdbms-configuration/schema-generation", XPathConstants.STRING); //$NON-NLS-1$
            if (schemaGeneration == null || schemaGeneration.isEmpty()) {
                // Default value is "update".
                schemaGeneration = "update"; //$NON-NLS-1$
            }
            String generateTechnicalFKAsString = (String) evaluate(dataSource,
                    "rdbms-configuration/schema-technical-fk", XPathConstants.STRING); //$NON-NLS-1$
            Boolean generateTechnicalFK;
            if (generateTechnicalFKAsString == null || generateTechnicalFKAsString.isEmpty()) {
                // Default value is "true" (enforce FK for technical FKs).
                generateTechnicalFK = Boolean.TRUE;
            } else {
                generateTechnicalFK = Boolean.parseBoolean(generateTechnicalFKAsString);
            }
            Map<String, String> advancedProperties = new HashMap<String, String>();
            NodeList properties = (NodeList) evaluate(dataSource,
                    "rdbms-configuration/properties/property", XPathConstants.NODESET); //$NON-NLS-1$
            for (int i = 0; i < properties.getLength(); i++) {
                Node item = properties.item(i);
                String propertyName = item.getAttributes().getNamedItem("name").getNodeValue(); //$NON-NLS-1$
                String propertyValue = item.getTextContent();
                advancedProperties.put(propertyName, propertyValue);
            }
            // Contains optimization
            String containsOptimizationAsString = (String) evaluate(dataSource,
                    "rdbms-configuration/contains-optimization", XPathConstants.STRING); //$NON-NLS-1$
            RDBMSDataSource.ContainsOptimization containsOptimization = RDBMSDataSource.ContainsOptimization.FULL_TEXT;
            if ("fulltext".equals(containsOptimizationAsString)) { //$NON-NLS-1$
                containsOptimization = RDBMSDataSource.ContainsOptimization.FULL_TEXT;
            } else if ("disabled".equals(containsOptimizationAsString)) { //$NON-NLS-1$
                containsOptimization = RDBMSDataSource.ContainsOptimization.DISABLED;
            } else if ("like".equals(containsOptimizationAsString)) { //$NON-NLS-1$
                containsOptimization = RDBMSDataSource.ContainsOptimization.LIKE;
            }
            String initConnectionURL = (String) evaluate(dataSource,
                    "rdbms-configuration/init/connection-url", XPathConstants.STRING); //$NON-NLS-1$
            String initUserName = (String) evaluate(dataSource,
                    "rdbms-configuration/init/connection-username", XPathConstants.STRING); //$NON-NLS-1$
            String initPassword = (String) evaluate(dataSource,
                    "rdbms-configuration/init/connection-password", XPathConstants.STRING); //$NON-NLS-1$
            String databaseName = (String) evaluate(dataSource, "rdbms-configuration/init/database-name", XPathConstants.STRING); //$NON-NLS-1$
            return new RDBMSDataSource(name, dialectName, driverClassName, userName, password, connectionPoolMinSize,
                    connectionPoolMaxSize, indexDirectory, cacheDirectory, caseSensitiveSearch, schemaGeneration,
                    generateTechnicalFK, advancedProperties, connectionURL, databaseName, containsOptimization, initPassword,
                    initUserName, initConnectionURL, true);
        } else {
            // Invoke extensions for datasource extensions
            ServiceLoader<DataSourceExtension> extensions = ServiceLoader.load(DataSourceExtension.class);
            if (LOGGER.isDebugEnabled()) {
                StringBuilder extensionsAsString = new StringBuilder();
                int i = 0;
                for (DataSourceExtension extension : extensions) {
                    extensionsAsString.append(extension.getClass().getName()).append(' ');
                    i++;
                }
                if (i == 0) {
                    LOGGER.debug("No datasource extension found");
                } else {
                    LOGGER.debug("Found datasource extensions (" + i + " found): " + extensionsAsString);
                }
            }
            for (DataSourceExtension extension : extensions) {
                if (extension.accept(type)) {
                    return extension.create(dataSource);
                } else {
                    LOGGER.debug("Extension '" + extension + "' is not eligible for datasource type '" + type + "'.");
                }
            }
            throw new NotImplementedException("No support for type '" + type + "'.");
        }
    }

    private static Object evaluate(Node node, String expression, QName returnType) throws XPathExpressionException {
        XPathExpression result;
        synchronized (xPath) {
            result = xPath.compile(expression);
        }
        return result.evaluate(node, returnType);
    }

    public boolean hasDataSource(String dataSourceName) {
        return hasDataSource(readDataSourcesConfiguration(), dataSourceName);
    }

    public boolean hasDataSource(InputStream configurationStream, String dataSourceName) {
        if (dataSourceName == null) {
            throw new IllegalArgumentException("Data source name can not be null.");
        }
        Map<String, DataSourceDefinition> dataSourceMap = readDocument(configurationStream);
        return dataSourceMap.get(dataSourceName) != null;
    }

    public DataSourceDefinition getDataSource(String dataSourceName, String container, String revisionId) {
        return getDataSource(readDataSourcesConfiguration(), dataSourceName, container, revisionId);
    }

    public DataSourceDefinition getDataSource(InputStream configurationStream, String dataSourceName, String container,
            String revisionId) {
        if (dataSourceName == null) {
            throw new IllegalArgumentException("Data source name can not be null.");
        }
        if (container == null) {
            throw new IllegalArgumentException("Container name can not be null.");
        }
        Map<String, DataSourceDefinition> dataSourceMap = readDocument(configurationStream);
        DataSourceDefinition dataSource = dataSourceMap.get(dataSourceName);
        if (dataSource == null) {
            throw new IllegalArgumentException("Data source '" + dataSourceName + "' can not be found in configuration.");
        }
        // Additional post parsing (replace potential ${container} with container parameter value).
        replacePlaceholder(dataSource.getMaster(), CONTAINER_PLACEHOLDER, container);
        // TMDM-6527: Call this for lower case processing.
        replacePlaceholder(dataSource.getSystem(), CONTAINER_PLACEHOLDER, StringUtils.EMPTY);
        if (dataSource.hasStaging()) {
            replacePlaceholder(dataSource.getStaging(), CONTAINER_PLACEHOLDER, container);
        }
        if (revisionId != null && !"HEAD".equals(revisionId)) { //$NON-NLS-1$
            // Additional post parsing (replace potential ${revision} with revision id parameter value).
            replacePlaceholder(dataSource.getMaster(), REVISION_PLACEHOLDER, revisionId);
            if (dataSource.hasStaging()) {
                replacePlaceholder(dataSource.getStaging(), REVISION_PLACEHOLDER, revisionId);
            }
        } else {
            // Additional post parsing (replace potential ${revision} with revision id parameter value).
            replacePlaceholder(dataSource.getMaster(), REVISION_PLACEHOLDER, StringUtils.EMPTY);
            if (dataSource.hasStaging()) {
                replacePlaceholder(dataSource.getStaging(), REVISION_PLACEHOLDER, StringUtils.EMPTY);
            }
        }
        return dataSource;
    }

}
