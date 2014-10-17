// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package com.amalto.webapp.core.bean;

import java.util.Date;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amalto.core.delegator.ILocalUser;
import com.amalto.core.util.LocalUser;
import com.amalto.core.webservice.WSBoolean;
import com.amalto.core.webservice.WSDataClusterPK;
import com.amalto.core.webservice.WSDataModelPK;
import com.amalto.core.webservice.WSExistsDataCluster;
import com.amalto.core.webservice.WSExistsDataModel;
import com.amalto.core.webservice.WSGetItem;
import com.amalto.core.webservice.WSItemPK;
import com.amalto.core.webservice.WSPutItem;
import com.amalto.webapp.core.dwr.CommonDWR;
import com.amalto.webapp.core.util.SessionListener;
import com.amalto.webapp.core.util.Util;

public class Configuration {

    private static final Logger LOG = Logger.getLogger(Configuration.class);

    private static ConfigurationContext defaultConfigurationContext = new DefaultConfigurationContext();

    private static ConfigurationContext gwtConfigurationContext;

    private String cluster;

    private String model;

    public interface ConfigurationContext {

        public HttpSession getSession();
    }

    private static class DefaultConfigurationContext implements ConfigurationContext {

        @Override
        public HttpSession getSession() {
            HttpSession session;
            WebContext ctx = WebContextFactory.get();
            if (ctx != null) {
                // DWR call ?
                session = ctx.getSession();
            } else if (gwtConfigurationContext != null) {
                // GWT call ?
                session = gwtConfigurationContext.getSession();
            } else {
                // Unknown context
                session = null;
            }

            if (LOG.isTraceEnabled()) {
                if (session == null) {
                    LOG.info("Called with null session"); //$NON-NLS-1$
                } else {
                    LOG.info("Session id: " + session.getId() + " ;creation: " + new Date(session.getCreationTime()) //$NON-NLS-1$ //$NON-NLS-2$
                            + " ;last access: " + new Date(session.getLastAccessedTime())); //$NON-NLS-1$
                }
            }
            return session;
        }
    }

    private Configuration() {
    }

    private Configuration(String cluster, String model) {
        this.cluster = cluster;
        this.model = model;
    }

    public static Configuration getInstance(boolean forceReload, ConfigurationContext configurationContext) throws Exception {
        Configuration instance;
        if (forceReload) {
            HttpSession session = configurationContext.getSession();
            instance = load(session);
        } else {
            instance = getInstance();
        }
        return instance;
    }

    public static Configuration getInstance(ConfigurationContext configurationContext) throws Exception {
        Configuration instance;

        HttpSession session = configurationContext.getSession();
        if (session == null) {
            instance = null;
        } else {
            instance = SessionListener.getRegisteredConfiguration(session.getId());
        }
        if (instance == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Configuration instance is null, loading ..."); //$NON-NLS-1$
            }
            instance = load(session);
        }

        return instance;
    }

    public static void initialize(String cluster, String model, ConfigurationContext configurationContext) throws Exception {
        if (cluster == null || cluster.trim().length() == 0) {
            throw new Exception("Data Container can't be empty!");
        }
        if (model == null || model.trim().length() == 0) {
            throw new Exception("Data Model can't be empty!");
        }

        store(cluster, model);

        HttpSession session = configurationContext.getSession();
        if (session != null) {
            SessionListener.registerConfiguration(session.getId(), new Configuration(cluster, model));
        }
    }

    public static Configuration getInstance(boolean forceReload) throws Exception {
        return getInstance(forceReload, defaultConfigurationContext);
    }

    public static Configuration getInstance() throws Exception {
        return getInstance(defaultConfigurationContext);
    }

    public static Configuration getConfiguration() throws Exception {
        return getInstance();
    }

    public static void initialize(String cluster, String model) throws Exception {
        initialize(cluster, model, defaultConfigurationContext);
    }

    private static synchronized void store(String cluster, String model) throws Exception {
        if (cluster == null || cluster.trim().length() == 0) {
            throw new Exception("nocontainer"); //$NON-NLS-1$
        } else if (model == null || model.trim().length() == 0) {
            throw new Exception("nomodel"); //$NON-NLS-1$
        }

        String xml = Util
                .getPort()
                .getItem(
                        new WSGetItem(new WSItemPK(
                                new WSDataClusterPK("PROVISIONING"), "User", new String[] { Util.getLoginUserName() }))) //$NON-NLS-1$//$NON-NLS-2$
                .getContent();
        Document d = Util.parse(xml);
        NodeList nodeList = Util.getNodeList(d, "//property"); //$NON-NLS-1$
        // TMDM-7434: portal config propert already exist
        if (Util.getNodeList(d, "//property[name='cluster']").getLength() == 0) { //$NON-NLS-1$
            if (Util.getNodeList(d, "//properties").item(0) == null) { //$NON-NLS-1$
                d.getDocumentElement().appendChild(d.createElement("properties")); //$NON-NLS-1$
            }
            Node node = Util.getNodeList(d, "//properties").item(0).appendChild(d.createElement("property")); //$NON-NLS-1$ //$NON-NLS-2$
            node.appendChild(d.createElement("name")).appendChild(d.createTextNode("cluster")); //$NON-NLS-1$ //$NON-NLS-2$
            node.appendChild(d.createElement("value")).appendChild(d.createTextNode(cluster)); //$NON-NLS-1$
            Node node2 = Util.getNodeList(d, "//properties").item(0).appendChild(d.createElement("property")); //$NON-NLS-1$ //$NON-NLS-2$
            node2.appendChild(d.createElement("name")).appendChild(d.createTextNode("model")); //$NON-NLS-1$ //$NON-NLS-2$
            node2.appendChild(d.createElement("value")).appendChild(d.createTextNode(model)); //$NON-NLS-1$
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("cluster".equals(Util.getFirstTextNode(node, "name"))) { //$NON-NLS-1$ //$NON-NLS-2$
                if (Util.getFirstTextNode(node, "value") == null) { //$NON-NLS-1$
                    Util.getNodeList(node, "value").item(0).appendChild(d.createTextNode(cluster)); //$NON-NLS-1$
                } else {
                    Util.getNodeList(node, "value").item(0).getFirstChild().setNodeValue(cluster); //$NON-NLS-1$
                }
            }
        }
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("model".equals(Util.getFirstTextNode(node, "name"))) { //$NON-NLS-1$ //$NON-NLS-2$
                if (Util.getFirstTextNode(node, "value") == null) { //$NON-NLS-1$
                    Util.getNodeList(node, "value").item(0).appendChild(d.createTextNode(model)); //$NON-NLS-1$
                } else {
                    Util.getNodeList(node, "value").item(0).getFirstChild().setNodeValue(model); //$NON-NLS-1$
                }
            }
        }

        String updatedUser = CommonDWR.getXMLStringFromDocument(d).replaceAll("<\\?xml.*?\\?>", ""); //$NON-NLS-1$ //$NON-NLS-2$
        ILocalUser user = LocalUser.getLocalUser();
        if (com.amalto.core.util.Util.isEnterprise()) {
            Util.getPort().putItem(
                    new WSPutItem(new WSDataClusterPK("PROVISIONING"), updatedUser, new WSDataModelPK("PROVISIONING"), false)); //$NON-NLS-1$ //$NON-NLS-2$ 

        } else {
            Util.storeProvisioning(Util.getLoginUserName(), updatedUser);
        }
        user.setUserXML(updatedUser);// syn cache
    }

    private static Configuration load(HttpSession session) throws Exception {
        Configuration configuration = loadConfigurationFromDB();
        if (session != null) {
            SessionListener.registerConfiguration(session.getId(), configuration);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("MDM set up with " + configuration.getCluster() + " and " + configuration.getModel()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return configuration;
    }

    public static Configuration loadConfigurationFromDBDirectly() throws Exception {
        return loadConfigurationFromDB();
    }

    private static synchronized Configuration loadConfigurationFromDB() throws Exception {
        Configuration configuration = new Configuration();

        Element user = null;
        try {
            user = Util.getLoginProvisioningFromDB();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        if (user == null) {
            String userxml = Util.getAjaxSubject().getXml();
            user = (Element) Util.getNodeList(Util.parse(userxml), "//" + "User").item(0); //$NON-NLS-1$ //$NON-NLS-2$
        }

        NodeList nodeList = Util.getNodeList(user, "//property"); //$NON-NLS-1$
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("cluster".equals(Util.getFirstTextNode(node, "name"))) { //$NON-NLS-1$ //$NON-NLS-2$
                Node fchild = Util.getNodeList(node, "value").item(0).getFirstChild(); //$NON-NLS-1$
                if (fchild != null) {
                    WSExistsDataCluster wsExistsDataCluster = new WSExistsDataCluster();
                    wsExistsDataCluster.setWsDataClusterPK(new WSDataClusterPK(fchild.getNodeValue()));
                    WSBoolean wsBoolean = Util.getPort().existsDataCluster(wsExistsDataCluster);
                    if (wsBoolean.is_true()) {
                        configuration.setCluster(fchild.getNodeValue());
                    }

                }

            }
        }
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("model".equals(Util.getFirstTextNode(node, "name"))) { //$NON-NLS-1$ //$NON-NLS-2$
                Node fchild = Util.getNodeList(node, "value").item(0).getFirstChild(); //$NON-NLS-1$
                if (fchild != null) {
                    WSExistsDataModel wsExistsDataModel = new WSExistsDataModel();
                    wsExistsDataModel.setWsDataModelPK(new WSDataModelPK(fchild.getNodeValue()));
                    WSBoolean wsBoolean = Util.getPort().existsDataModel(wsExistsDataModel);
                    if (wsBoolean.is_true()) {
                        configuration.setModel(fchild.getNodeValue());
                    }
                }

            }
        }
        boolean attemptToStoreAgain = false;
        if (configuration.getCluster() == null && CommonDWR.getClusters().length > 0) {
            configuration.setCluster(CommonDWR.getClusters()[0]);
            attemptToStoreAgain = true;
        }
        if (configuration.getModel() == null && CommonDWR.getModels().length > 0) {
            configuration.setModel(CommonDWR.getModels()[0]);
            attemptToStoreAgain = true;
        }

        if (attemptToStoreAgain) {
            try {
                store(configuration.getCluster(), configuration.getModel());
            } catch (Exception e) {
                LOG.error("Unable to store updated configuration", e); //$NON-NLS-1$
            }
        }
        Document d = user.getOwnerDocument();
        String updatedUser = CommonDWR.getXMLStringFromDocument(d).replaceAll("<\\?xml.*?\\?>", ""); //$NON-NLS-1$ //$NON-NLS-2$
        ILocalUser iUser = LocalUser.getLocalUser();
        iUser.setUserXML(updatedUser);// syn cache
        return configuration;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public static void setGwtConfigurationContext(ConfigurationContext _gwtConfigurationContext) {
        gwtConfigurationContext = _gwtConfigurationContext;
    }

    @Override
    public String toString() {
        return "cluster:" + cluster + "," + "model:" + model; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
