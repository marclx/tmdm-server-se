package org.talend.mdm.webapp.general.server.actions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.talend.mdm.commmon.util.webapp.XObjectType;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;
import org.talend.mdm.webapp.base.client.exception.ServiceException;
import org.talend.mdm.webapp.base.server.util.XmlUtil;
import org.talend.mdm.webapp.general.client.GeneralService;
import org.talend.mdm.webapp.general.gwt.GWTConfigurationContext;
import org.talend.mdm.webapp.general.gwt.GwtWebContextFactory;
import org.talend.mdm.webapp.general.model.ActionBean;
import org.talend.mdm.webapp.general.model.ComboBoxModel;
import org.talend.mdm.webapp.general.model.LanguageBean;
import org.talend.mdm.webapp.general.model.MenuBean;
import org.talend.mdm.webapp.general.model.MenuGroup;
import org.talend.mdm.webapp.general.model.ProductInfo;
import org.talend.mdm.webapp.general.model.UserBean;
import org.talend.mdm.webapp.general.server.util.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amalto.core.delegator.ILocalUser;
import com.amalto.core.util.LicenseUserNumberValidationException;
import com.amalto.core.util.LocalUser;
import com.amalto.core.util.Messages;
import com.amalto.core.util.MessagesFactory;
import com.amalto.core.webservice.WSDataCluster;
import com.amalto.core.webservice.WSDataClusterPK;
import com.amalto.core.webservice.WSDataModel;
import com.amalto.core.webservice.WSDataModelPK;
import com.amalto.core.webservice.WSGetDataCluster;
import com.amalto.core.webservice.WSGetDataModel;
import com.amalto.core.webservice.WSGetItem;
import com.amalto.core.webservice.WSItem;
import com.amalto.core.webservice.WSItemPK;
import com.amalto.core.webservice.WSLogout;
import com.amalto.core.webservice.WSRegexDataClusterPKs;
import com.amalto.core.webservice.WSRegexDataModelPKs;
import com.amalto.webapp.core.bean.Configuration;
import com.amalto.webapp.core.util.Menu;
import com.amalto.webapp.core.util.SessionListener;
import com.amalto.webapp.core.util.SystemLocale;
import com.amalto.webapp.core.util.SystemLocaleFactory;
import com.amalto.webapp.core.util.SystemLocaleInitializable;
import com.amalto.webapp.core.util.Util;
import com.amalto.webapp.core.util.Webapp;

public class GeneralAction implements GeneralService {

    private static final Logger LOG = Logger.getLogger(GeneralAction.class);

    private static final GWTConfigurationContext configurationContext = new GWTConfigurationContext();

    private static final Messages MESSAGES = MessagesFactory.getMessages(
            "org.talend.mdm.webapp.general.client.i18n.GeneralMessages", GeneralAction.class.getClassLoader()); //$NON-NLS-1$

    @Override
    public ProductInfo getProductInfo() throws ServiceException {
        if (com.amalto.core.util.Util.isEnterprise()) {
            ProductInfo info = new ProductInfo();
            Map<String, String> map = com.amalto.webapp.core.util.Webapp.INSTANCE.getProductInfo();
            if (map != null) {
                info.setProductKey(map.get("productKey")); //$NON-NLS-1$
                info.setProductName(map.get("productName")); //$NON-NLS-1$
                info.setProductEdition(map.get("productEdition")); //$NON-NLS-1$
                return info;
            }
        }
        return null;
    }

    @Override
    public MenuGroup getMenus(String language) throws ServiceException {
        MenuGroup result = new MenuGroup();
        try {

            List<MenuBean> menus = new ArrayList<MenuBean>();
            Utils.getSubMenus(Menu.getRootMenu(), language, menus, 1, 1);
            result.setMenuBean(menus);
            result.setGroupItem(Utils.getGroupItems(language));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ServiceException(e.getLocalizedMessage());
        }
        return result;
    }

    private List<ComboBoxModel> getClusters() throws Exception {
        List<ComboBoxModel> clusters = new ArrayList<ComboBoxModel>();

        WSDataClusterPK[] wsDataClustersPKs = Util.getPort().getDataClusterPKs(new WSRegexDataClusterPKs("*") //$NON-NLS-1$
                ).getWsDataClusterPKs();
        Map<String, XSystemObjects> xDataClustersMap = XSystemObjects.getXSystemObjects(XObjectType.DATA_CLUSTER);
        for (int i = 0; i < wsDataClustersPKs.length; i++) {
            if (!XSystemObjects.isXSystemObject(xDataClustersMap, wsDataClustersPKs[i].getPk())) {
                WSDataCluster wsGetDataCluster = Util.getPort().getDataCluster(new WSGetDataCluster(wsDataClustersPKs[i]));
                clusters.add(new ComboBoxModel(wsGetDataCluster.getDescription(), wsDataClustersPKs[i].getPk()));
            }
        }

        return clusters;
    }

    private List<ComboBoxModel> getModels() throws Exception {
        List<ComboBoxModel> models = new ArrayList<ComboBoxModel>();
        WSDataModelPK[] wsDataModelsPKs = Util.getPort().getDataModelPKs(new WSRegexDataModelPKs("*") //$NON-NLS-1$
                ).getWsDataModelPKs();

        Map<String, XSystemObjects> xDataModelsMap = XSystemObjects.getXSystemObjects(XObjectType.DATA_MODEL);
        for (int i = 0; i < wsDataModelsPKs.length; i++) {
            if (!XSystemObjects.isXSystemObject(xDataModelsMap, wsDataModelsPKs[i].getPk())) {
                WSDataModel wsDataModel = Util.getPort().getDataModel(new WSGetDataModel(wsDataModelsPKs[i]));
                models.add(new ComboBoxModel(wsDataModel.getDescription(), wsDataModelsPKs[i].getPk()));
            }
        }

        return models;
    }

    @Override
    public ActionBean getAction() throws ServiceException {
        try {
            ActionBean action = new ActionBean();
            Configuration.setGwtConfigurationContext(configurationContext);
            action.setClusters(getClusters());
            action.setModels(getModels());
            Configuration configuration = Configuration.getInstance(configurationContext);
            action.setCurrentCluster(configuration.getCluster());
            action.setCurrentModel(configuration.getModel());
            return action;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            String err = e.getLocalizedMessage();
            if (e.getMessage().equals("nocontainer")) {
                err = MESSAGES.getMessage("nocontainer"); //$NON-NLS-1$
            } else if (e.getMessage().equals("nomodel")) {
                err = MESSAGES.getMessage("nomodel"); //$NON-NLS-1$
            }
            throw new ServiceException(err);
        }

    }

    @Override
    public void setClusterAndModel(String cluster, String model) throws ServiceException {
        try {
            Configuration.initialize(cluster, model, configurationContext);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ServiceException(e.getLocalizedMessage());
        }
    }

    @Override
    public UserBean getUsernameAndUniverse() throws ServiceException {
        try {
            UserBean userBean = new UserBean();
            userBean.setEnterprise(com.amalto.core.util.Util.isEnterprise());
            if (!com.amalto.core.util.Util.isEnterprise()) {
                // TMDM-7629 init locaUser cache
                String userName = Util.getLoginUserName();
                userBean.setName(userName);
                userBean.setUniverse("UNKNOWN"); //$NON-NLS-1$
                WSItem item = Util.getPort().getItem(
                        new WSGetItem(new WSItemPK(new WSDataClusterPK("PROVISIONING"), "User", new String[] { userName }))); //$NON-NLS-1$ //$NON-NLS-2$
                ILocalUser iUser = LocalUser.getLocalUser();
                iUser.setUserXML(item.getContent());
                return userBean;
            }
            String givenname = null;
            String familyname = null;
            String xml = Util.getAjaxSubject().getXml();
            if (xml != null) {
                Document d = Util.parse(xml);
                givenname = Util.getFirstTextNode(d, "//givenname"); //$NON-NLS-1$
                familyname = Util.getFirstTextNode(d, "//familyname"); //$NON-NLS-1$
            }

            String universe = Util.getLoginUniverse();
            if (familyname != null && givenname != null) {
                userBean.setName(givenname + " " + familyname); //$NON-NLS-1$
            } else {
                userBean.setName(Util.getAjaxSubject().getUsername());
            }
            userBean.setUniverse(universe);
            return userBean;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ServiceException(e.getLocalizedMessage());
        }
    }

    @Override
    public List<LanguageBean> getLanguages(String language) throws ServiceException {

        try {
            // Reload system locale
            SystemLocaleFactory.getInstance().load(new SystemLocaleInitializable() {

                @Override
                public void doInit() throws Exception {

                    InputStream is = XmlUtil.getXmlStream("languages.xml"); //$NON-NLS-1$
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(is);
                    Element root = doc.getDocumentElement();

                    NodeList nodes = root.getChildNodes();
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Node node = nodes.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            if (node.getNodeName().equals("language")) { //$NON-NLS-1$ 

                                Node isoNode = node.getAttributes().getNamedItem("value");//$NON-NLS-1$
                                String iso = isoNode.getNodeValue();
                                if (iso != null) {

                                    SystemLocale locale = new SystemLocale(iso, node.getTextContent());
                                    super.supportedLocales.put(iso, locale);

                                    Node dateTimeNode = node.getAttributes().getNamedItem("dateTime");//$NON-NLS-1$

                                    if (dateTimeNode != null && dateTimeNode.getNodeValue() != null) {
                                        locale.setDateTimeFormat(dateTimeNode.getNodeValue());
                                    }

                                }

                            }
                        }
                    }// end for

                }

            });
            String lang = language;
            String storeLang = Utils.getDefaultLanguage();
            if (storeLang != null && !"".equals(storeLang)) { //$NON-NLS-1$
                lang = storeLang;
            } else {
                // language is not set try to store it (if license is set)
                setDefaultLanguage(language, true);
            }
            return Utils.getLanguages(lang);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ServiceException(e.getLocalizedMessage());
        }
    }

    @Override
    public void logout() throws ServiceException {
        try {
            String username = com.amalto.webapp.core.util.Util.getAjaxSubject().getUsername();
            SessionListener.unregisterUser(username);
            Util.getPort().logout(new WSLogout("")).getValue(); //$NON-NLS-1$
            GwtWebContextFactory.get().getSession().invalidate();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ServiceException(e.getLocalizedMessage());
        }
    }

    @Override
    public boolean isExpired(String language) throws ServiceException {
        try {
            return Webapp.INSTANCE.isExpired(language);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            if (e instanceof LicenseUserNumberValidationException) {
                throw new org.talend.mdm.webapp.base.client.exception.LicenseUserNumberValidationException(
                        e.getLocalizedMessage());
            } else {
                throw new ServiceException(e.getLocalizedMessage());
            }

        }
    }

    @Override
    public boolean supportStaging(String dataCluster) throws ServiceException {
        try {
            WSDataClusterPK wsDataClusterPK = new WSDataClusterPK(Configuration.getConfiguration().getCluster());
            return Util.getPort().supportStaging(wsDataClusterPK).is_true();
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public void setDefaultLanguage(String language) throws ServiceException {
        setDefaultLanguage(language, false);
    }

    @Override
    public boolean isEnterpriseVersion() throws ServiceException {
        return Webapp.INSTANCE.isEnterpriseVersion();
    }

    private void setDefaultLanguage(String language, boolean failQuietly) throws ServiceException {
        try {
            Utils.setDefaultLanguage(language);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            if (!failQuietly) {
                throw new ServiceException(e.getLocalizedMessage());
            }
        }
    }

}
