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
package com.amalto.core.delegator;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;
import org.talend.mdm.commmon.util.core.ICoreConstants;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.amalto.core.ejb.DroppedItemPOJO;
import com.amalto.core.ejb.DroppedItemPOJOPK;
import com.amalto.core.ejb.ItemPOJO;
import com.amalto.core.ejb.ItemPOJOPK;
import com.amalto.core.ejb.ObjectPOJO;
import com.amalto.core.ejb.TransformerCtrlBean;
import com.amalto.core.ejb.TransformerPOJO;
import com.amalto.core.ejb.TransformerPOJOPK;
import com.amalto.core.ejb.UpdateReportItemPOJO;
import com.amalto.core.ejb.UpdateReportPOJO;
import com.amalto.core.ejb.local.DroppedItemCtrlLocal;
import com.amalto.core.ejb.local.ItemCtrl2Local;
import com.amalto.core.ejb.local.TransformerCtrlLocal;
import com.amalto.core.ejb.local.XmlServerSLWrapperLocal;
import com.amalto.core.integrity.FKIntegrityCheckResult;
import com.amalto.core.metadata.ClassRepository;
import com.amalto.core.migration.MigrationRepository;
import com.amalto.core.objects.backgroundjob.ejb.BackgroundJobPOJOPK;
import com.amalto.core.objects.datacluster.ejb.DataClusterPOJO;
import com.amalto.core.objects.datacluster.ejb.DataClusterPOJOPK;
import com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK;
import com.amalto.core.objects.menu.ejb.MenuPOJO;
import com.amalto.core.objects.menu.ejb.MenuPOJOPK;
import com.amalto.core.objects.menu.ejb.local.MenuCtrlLocal;
import com.amalto.core.objects.role.ejb.RolePOJO;
import com.amalto.core.objects.role.ejb.RolePOJOPK;
import com.amalto.core.objects.role.ejb.local.RoleCtrlLocal;
import com.amalto.core.objects.routing.v2.ejb.AbstractRoutingOrderV2POJO;
import com.amalto.core.objects.routing.v2.ejb.AbstractRoutingOrderV2POJOPK;
import com.amalto.core.objects.routing.v2.ejb.ActiveRoutingOrderV2POJO;
import com.amalto.core.objects.routing.v2.ejb.CompletedRoutingOrderV2POJO;
import com.amalto.core.objects.routing.v2.ejb.FailedRoutingOrderV2POJO;
import com.amalto.core.objects.routing.v2.ejb.RoutingEngineV2POJO;
import com.amalto.core.objects.routing.v2.ejb.RoutingRulePOJO;
import com.amalto.core.objects.routing.v2.ejb.RoutingRulePOJOPK;
import com.amalto.core.objects.routing.v2.ejb.local.RoutingEngineV2CtrlLocal;
import com.amalto.core.objects.routing.v2.ejb.local.RoutingOrderV2CtrlLocal;
import com.amalto.core.objects.routing.v2.ejb.local.RoutingRuleCtrlLocal;
import com.amalto.core.objects.storedprocedure.ejb.StoredProcedurePOJO;
import com.amalto.core.objects.storedprocedure.ejb.StoredProcedurePOJOPK;
import com.amalto.core.objects.storedprocedure.ejb.local.StoredProcedureCtrlLocal;
import com.amalto.core.objects.transformers.v2.ejb.TransformerV2POJO;
import com.amalto.core.objects.transformers.v2.ejb.TransformerV2POJOPK;
import com.amalto.core.objects.transformers.v2.ejb.local.TransformerV2CtrlLocal;
import com.amalto.core.objects.transformers.v2.util.TransformerCallBack;
import com.amalto.core.objects.transformers.v2.util.TransformerContext;
import com.amalto.core.objects.transformers.v2.util.TransformerPluginVariableDescriptor;
import com.amalto.core.objects.transformers.v2.util.TypedContent;
import com.amalto.core.objects.universe.ejb.UniversePOJO;
import com.amalto.core.objects.view.ejb.ViewPOJOPK;
import com.amalto.core.query.user.UserQueryBuilder;
import com.amalto.core.save.SaveException;
import com.amalto.core.save.SaverHelper;
import com.amalto.core.save.SaverSession;
import com.amalto.core.save.context.DocumentSaver;
import com.amalto.core.server.MetadataRepositoryAdmin;
import com.amalto.core.server.ServerContext;
import com.amalto.core.server.StorageAdmin;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageMetadataUtils;
import com.amalto.core.storage.StorageResults;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.util.BeforeDeletingErrorException;
import com.amalto.core.util.DigestHelper;
import com.amalto.core.util.EntityNotFoundException;
import com.amalto.core.util.LocalUser;
import com.amalto.core.util.RemoteExceptionFactory;
import com.amalto.core.util.TransformerPluginContext;
import com.amalto.core.util.Util;
import com.amalto.core.util.ValidateException;
import com.amalto.core.util.Version;
import com.amalto.core.util.WhereConditionForcePivotFilter;
import com.amalto.core.util.XConverter;
import com.amalto.core.util.XtentisException;
import com.amalto.core.webservice.*;
import com.amalto.xmlserver.interfaces.ItemPKCriteria;

public abstract class IXtentisWSDelegator implements IBeanDelegator {

    public static final String MDM_TIS_JOB = "MDMTISJOB";//$NON-NLS-1$

    public static final String JOB = "JOB";//$NON-NLS-1$

    private static Logger LOGGER = Logger.getLogger(IXtentisWSDelegator.class);

    private transient ConnectionFactory cxFactory = null;

    public WSVersion getComponentVersion(WSGetComponentVersion wsGetComponentVersion) throws RemoteException {
        try {
            if (WSComponent.DataManager.equals(wsGetComponentVersion.getComponent())) {
                Version version = Version.getVersion(this.getClass());
                return new WSVersion(version.getMajor(), version.getMinor(), version.getRevision(), version.getBuild(),
                        version.getDescription(), version.getDate());
            }
            throw new RemoteException("Version information is not available yet for "
                    + wsGetComponentVersion.getComponent().getValue() + " components");
        } catch (RemoteException e) {
            throw (e);
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()));
        }
    }

    public WSString ping(WSPing wsPing) throws RemoteException {
        if ("Studio".equals(wsPing.getEcho())) {// check view user can't use studio //$NON-NLS-1$
            try {
                if (LocalUser.getLocalUser().getRoles().contains(XSystemObjects.ROLE_DEFAULT_VIEWER.getName())) {
                    throw new RemoteException("Viewer user can't use MDM Studio!");
                }
            } catch (Exception e) {
                throw new RemoteException(e.getLocalizedMessage(), e);
            }
        }
        return new WSString(wsPing.getEcho());
    }

    public WSString logout(WSLogout logout) throws RemoteException {
        String msg = "OK";
        try {
            ILocalUser user = LocalUser.getLocalUser();
            user.logout();
        } catch (Exception e) {
            msg = e.getMessage();
        }
        return new WSString(msg);
    }

    public WSInt initMDM(WSInitData initData) throws RemoteException {
        // run migration tasks
        MigrationRepository.getInstance().execute(true);
        return new WSInt(0);
    }

    public WSMDMConfig getMDMConfiguration() throws RemoteException {
        WSMDMConfig mdmConfig = new WSMDMConfig();
        Properties property = MDMConfiguration.getConfiguration();
        try {
            mdmConfig.setServerName(property.getProperty("xmldb.server.name")); //$NON-NLS-1$
            mdmConfig.setServerPort(property.getProperty("xmldb.server.port")); //$NON-NLS-1$
            mdmConfig.setUserName(property.getProperty("admin.user")); //$NON-NLS-1$
            mdmConfig.setPassword(property.getProperty("admin.password")); //$NON-NLS-1$
            mdmConfig.setXdbDriver(property.getProperty("xmldb.driver")); //$NON-NLS-1$
            mdmConfig.setXdbID(property.getProperty("xmldb.dbid")); //$NON-NLS-1$
            mdmConfig.setXdbUrl(property.getProperty("xmldb.dburl")); //$NON-NLS-1$
            mdmConfig.setIsupurl(property.getProperty("xmldb.isupurl")); //$NON-NLS-1$
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
        return mdmConfig;
    }

    public WSDataModel getDataModel(WSGetDataModel wsGetDataModel) throws RemoteException {
        try {
            return XConverter.VO2WS(Util.getDataModelCtrlLocal()
                    .getDataModel(new DataModelPOJOPK(wsGetDataModel.getWsDataModelPK().getPk())));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSBoolean existsDataModel(WSExistsDataModel wsExistsDataModel) throws RemoteException {
        try {
            return new WSBoolean((Util.getDataModelCtrlLocal().existsDataModel(
                    new DataModelPOJOPK(wsExistsDataModel.getWsDataModelPK().getPk())) != null));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSDataModelPKArray getDataModelPKs(WSRegexDataModelPKs regexp) throws RemoteException {
        try {
            WSDataModelPKArray array = new WSDataModelPKArray();
            Collection<DataModelPOJOPK> list = Util.getDataModelCtrlLocal().getDataModelPKs(regexp.getRegex());
            ArrayList<WSDataModelPK> wsList = new ArrayList<WSDataModelPK>();
            for (DataModelPOJOPK pk : list) {
                WSDataModelPK wsPK = new WSDataModelPK(pk.getUniqueId());
                wsList.add(wsPK);
            }
            array.setWsDataModelPKs(wsList.toArray(new WSDataModelPK[list.size()]));
            return array;
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSDataModelPK deleteDataModel(WSDeleteDataModel wsDeleteDataModel) throws RemoteException {
        try {
            return new WSDataModelPK(Util.getDataModelCtrlLocal()
                    .removeDataModel(new DataModelPOJOPK(wsDeleteDataModel.getWsDataModelPK().getPk())).getUniqueId());
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSDataModelPK putDataModel(WSPutDataModel wsDataModel) throws RemoteException {
        try {
            WSDataModelPK wsDataModelPK = new WSDataModelPK(Util.getDataModelCtrlLocal()
                    .putDataModel(XConverter.WS2VO(wsDataModel.getWsDataModel())).getUniqueId());
            SaverSession session = SaverSession.newSession();
            session.invalidateTypeCache(wsDataModelPK.getPk());
            session.end();
            return wsDataModelPK;
        } catch (Exception e) {
            throw RemoteExceptionFactory.aggregateCauses(e, true);
        }
    }

    public WSString checkSchema(WSCheckSchema wsSchema) throws RemoteException {
        try {
            return new WSString(Util.getDataModelCtrlLocal().checkSchema(wsSchema.getSchema()));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    @SuppressWarnings("nls")
    public WSString putBusinessConcept(WSPutBusinessConcept wsPutBusinessConcept) throws RemoteException {
        WSBusinessConcept bc = wsPutBusinessConcept.getBusinessConcept();
        try {
            String s = "<xsd:element name=\"" + bc.getName() + "\" type=\"" + bc.getBusinessTemplate() + "\">"
                    + "	<xsd:annotation>";
            WSI18NString[] labels = bc.getWsLabel();
            for (WSI18NString label : labels) {
                s += "<xsd:appinfo source=\"" + label.getLanguage().getValue() + "\">" + label.getLabel() + "</xsd:appinfo>";
            }
            WSI18NString[] docs = bc.getWsDescription();
            for (WSI18NString doc : docs) {
                s += "<xsd:documentation xml:lang=\"" + doc.getLanguage().getValue() + "\">" + doc.getLabel()
                        + "</xsd:documentation>";
            }
            s += "	</xsd:annotation>" + "	<xsd:unique name=\"" + bc.getName() + "\">" + "		<xsd:selector xpath=\""
                    + bc.getWsUniqueKey().getSelectorpath() + "\"/>";
            for (int i = 0; i < bc.getWsUniqueKey().getFieldpath().length; i++) {
                s += "<xsd:field xpath=\"" + bc.getWsUniqueKey().getFieldpath()[i] + "\"/>";
            }
            s += "	</xsd:unique>" + "</xsd:element>";
            return new WSString(Util.getDataModelCtrlLocal().putBusinessConceptSchema(
                    new DataModelPOJOPK(wsPutBusinessConcept.getWsDataModelPK().getPk()), s));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSString putBusinessConceptSchema(WSPutBusinessConceptSchema wsPutBusinessConceptSchema) throws RemoteException {
        try {
            return new WSString(Util.getDataModelCtrlLocal().putBusinessConceptSchema(
                    new DataModelPOJOPK(wsPutBusinessConceptSchema.getWsDataModelPK().getPk()),
                    wsPutBusinessConceptSchema.getBusinessConceptSchema()));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSString deleteBusinessConcept(WSDeleteBusinessConcept wsDeleteBusinessConcept) throws RemoteException {
        try {
            return new WSString(Util.getDataModelCtrlLocal().deleteBusinessConcept(
                    new DataModelPOJOPK(wsDeleteBusinessConcept.getWsDataModelPK().getPk()),
                    wsDeleteBusinessConcept.getBusinessConceptName()));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray getBusinessConcepts(WSGetBusinessConcepts wsGetBusinessConcepts) throws RemoteException {
        try {
            return new WSStringArray(Util.getDataModelCtrlLocal().getAllBusinessConceptsNames(
                    new DataModelPOJOPK(wsGetBusinessConcepts.getWsDataModelPK().getPk())));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSConceptKey getBusinessConceptKey(WSGetBusinessConceptKey wsGetBusinessConceptKey) throws RemoteException {
        try {
            MetadataRepositoryAdmin metadataRepositoryAdmin = ServerContext.INSTANCE.get().getMetadataRepositoryAdmin();
            MetadataRepository repository = metadataRepositoryAdmin.get(wsGetBusinessConceptKey.getWsDataModelPK().getPk());
            ComplexTypeMetadata type = repository.getComplexType(wsGetBusinessConceptKey.getConcept());
            Collection<FieldMetadata> keyFields = type.getKeyFields();
            String[] fields = new String[keyFields.size()];
            int i = 0;
            for (FieldMetadata keyField : keyFields) {
                fields[i++] = keyField.getName();
            }
            return new WSConceptKey(".", fields); //$NON-NLS-1$
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSDataCluster getDataCluster(WSGetDataCluster wsDataClusterGet) throws RemoteException {
        try {
            return XConverter.VO2WS(Util.getDataClusterCtrlLocal().getDataCluster(
                    new DataClusterPOJOPK(wsDataClusterGet.getWsDataClusterPK().getPk())));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSBoolean existsDataCluster(WSExistsDataCluster wsExistsDataCluster) throws RemoteException {
        try {
            return new WSBoolean(Util.getDataClusterCtrlLocal().existsDataCluster(
                    new DataClusterPOJOPK(wsExistsDataCluster.getWsDataClusterPK().getPk())) != null);
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSBoolean existsDBDataCluster(WSExistsDBDataCluster wsExistsDataCluster) throws RemoteException {
        try {
            String revisionId = wsExistsDataCluster.getRevisionID();
            String clusterName = wsExistsDataCluster.getName();
            boolean exist = Util.getXmlServerCtrlLocal().existCluster(revisionId, clusterName);
            return new WSBoolean(exist);
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSDataClusterPKArray getDataClusterPKs(WSRegexDataClusterPKs regexp) throws RemoteException {
        try {
            WSDataClusterPKArray array = new WSDataClusterPKArray();
            ArrayList<WSDataClusterPK> l = new ArrayList<WSDataClusterPK>();
            Collection<DataClusterPOJOPK> vos = Util.getDataClusterCtrlLocal().getDataClusterPKs(regexp.getRegex());
            for (DataClusterPOJOPK pk : vos) {
                l.add(new WSDataClusterPK(pk.getUniqueId()));
            }
            array.setWsDataClusterPKs(l.toArray(new WSDataClusterPK[l.size()]));
            return array;
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSDataClusterPK deleteDataCluster(WSDeleteDataCluster wsDeleteDataCluster) throws RemoteException {
        try {
            return new WSDataClusterPK(Util.getDataClusterCtrlLocal()
                    .removeDataCluster(new DataClusterPOJOPK(wsDeleteDataCluster.getWsDataClusterPK().getPk())).getUniqueId());
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSDataClusterPK putDataCluster(WSPutDataCluster wsDataCluster) throws RemoteException {
        try {
            return new WSDataClusterPK(Util.getDataClusterCtrlLocal().putDataCluster(XConverter.WS2VO(wsDataCluster.getWsDataCluster()))
                    .getUniqueId());
        } catch (Exception e) {
            throw RemoteExceptionFactory.aggregateCauses(e, true);
        }
    }

    public WSBoolean putDBDataCluster(WSPutDBDataCluster wsDataCluster) throws RemoteException {
        try {
            Util.getXmlServerCtrlLocal().createCluster(wsDataCluster.getRevisionID(), wsDataCluster.getName());
            DataClusterPOJO pojo = new DataClusterPOJO(wsDataCluster.getName(), "", ""); //$NON-NLS-1$ //$NON-NLS-2$
            pojo.store(wsDataCluster.getRevisionID());
            return new WSBoolean(true);
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray getConceptsInDataCluster(WSGetConceptsInDataCluster wsGetConceptsInDataCluster) throws RemoteException {
        try {
            Set<String> concepts = Util.getItemCtrl2Local()
                    .getConceptsInDataCluster(new DataClusterPOJOPK(wsGetConceptsInDataCluster.getWsDataClusterPK().getPk()))
                    .keySet();
            return new WSStringArray(concepts.toArray(new String[concepts.size()]));
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage()));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSConceptRevisionMap getConceptsInDataClusterWithRevisions(
            WSGetConceptsInDataClusterWithRevisions wsGetConceptsInDataClusterWithRevisions) throws RemoteException {
        try {
            UniversePOJO pojo = null;
            if (wsGetConceptsInDataClusterWithRevisions == null
                    || wsGetConceptsInDataClusterWithRevisions.getUniversePK() == null
                    || wsGetConceptsInDataClusterWithRevisions.getUniversePK().getPk() == null
                    || "".equals(wsGetConceptsInDataClusterWithRevisions.getUniversePK().getPk())) { //$NON-NLS-1$
                pojo = new UniversePOJO();// default head revision
            }
            // get conceptRevisions
            DataClusterPOJOPK dataClusterPOJOPK = new DataClusterPOJOPK(wsGetConceptsInDataClusterWithRevisions
                    .getDataClusterPOJOPK().getPk());
            Map concepts = Util.getItemCtrl2Local().getConceptsInDataCluster(dataClusterPOJOPK, pojo);
            if (concepts == null) {
                return null;
            }
            // convert
            WSConceptRevisionMapMapEntry[] mapEntry = new WSConceptRevisionMapMapEntry[concepts.size()];
            int i = 0;
            for (Iterator iterator = concepts.keySet().iterator(); iterator.hasNext(); i++) {
                String concept = (String) iterator.next();
                String revisionId = (String) concepts.get(concept);
                if (revisionId == null) {
                    revisionId = ""; //$NON-NLS-1$
                }
                WSConceptRevisionMapMapEntry entry = new WSConceptRevisionMapMapEntry(concept, revisionId);
                mapEntry[i] = entry;
            }
            return new WSConceptRevisionMap(mapEntry);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage()));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSView getView(WSGetView wsViewGet) throws RemoteException {
        try {
            return XConverter.VO2WS(Util.getViewCtrlLocal().getView(new ViewPOJOPK(wsViewGet.getWsViewPK().getPk())));
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSBoolean existsView(WSExistsView wsExistsView) throws RemoteException {
        try {
            return new WSBoolean(Util.getViewCtrlLocal().existsView(new ViewPOJOPK(wsExistsView.getWsViewPK().getPk())) != null);
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSViewPKArray getViewPKs(WSGetViewPKs regexp) throws RemoteException {
        try {
            WSViewPKArray array = new WSViewPKArray();
            String regex = ((regexp.getRegex() == null) || ("".equals(regexp.getRegex())) || ("*".equals(regexp.getRegex())) ? ".*" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    : regexp.getRegex());
            Collection<ViewPOJOPK> pks = Util.getViewCtrlLocal().getViewPKs(regex);
            List<WSViewPK> list = new ArrayList<WSViewPK>();
            for (ViewPOJOPK pk : pks) {
                list.add(new WSViewPK(pk.getIds()[0]));
            }
            array.setWsViewPK(list.toArray(new WSViewPK[list.size()]));
            return array;
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSViewPK deleteView(WSDeleteView wsDeleteView) throws RemoteException {
        try {
            return new WSViewPK(
                    Util.getViewCtrlLocal().removeView(new ViewPOJOPK(wsDeleteView.getWsViewPK().getPk())).getIds()[0]);
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSViewPK putView(WSPutView wsView) throws RemoteException {
        try {
            return new WSViewPK(Util.getViewCtrlLocal().putView(XConverter.WS2VO(wsView.getWsView())).getIds()[0]);
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray viewSearch(WSViewSearch wsViewSearch) throws RemoteException {
        WSWhereItem whereItem = wsViewSearch.getWhereItem();
        if (whereItem != null && whereItem.getWhereAnd() == null && whereItem.getWhereOr() == null
                && whereItem.getWhereCondition() == null) {
            whereItem = null;
        }
        try {
            Collection res = Util.getItemCtrl2Local().viewSearch(
                    new DataClusterPOJOPK(wsViewSearch.getWsDataClusterPK().getPk()),
                    new ViewPOJOPK(wsViewSearch.getWsViewPK().getPk()), XConverter.WS2VO(whereItem), wsViewSearch.getSpellTreshold(),
                    wsViewSearch.getOrderBy(), wsViewSearch.getDirection(), wsViewSearch.getSkip(), wsViewSearch.getMaxItems());
            return new WSStringArray((String[]) res.toArray(new String[res.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray xPathsSearch(WSXPathsSearch wsXPathsSearch) throws RemoteException {
        try {
            if (wsXPathsSearch.getReturnCount() == null) {
                wsXPathsSearch.setReturnCount(Boolean.FALSE);
            }
            Collection res = Util.getItemCtrl2Local().xPathsSearch(
                    new DataClusterPOJOPK(wsXPathsSearch.getWsDataClusterPK().getPk()), wsXPathsSearch.getPivotPath(),
                    new ArrayList<String>(Arrays.asList(wsXPathsSearch.getViewablePaths().getStrings())),
                    XConverter.WS2VO(wsXPathsSearch.getWhereItem()), wsXPathsSearch.getSpellTreshold(), wsXPathsSearch.getOrderBy(),
                    wsXPathsSearch.getDirection(), wsXPathsSearch.getSkip(), wsXPathsSearch.getMaxItems(),
                    wsXPathsSearch.getReturnCount());
            return new WSStringArray((String[]) res.toArray(new String[res.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray getItemsPivotIndex(WSGetItemsPivotIndex wsGetItemsPivotIndex) throws RemoteException {
        try {
            Collection res = Util.getItemCtrl2Local().getItemsPivotIndex(
                    wsGetItemsPivotIndex.getClusterName(),
                    wsGetItemsPivotIndex.getMainPivotName(),
                    XConverter.WS2VO(wsGetItemsPivotIndex.getPivotWithKeys()),
                    wsGetItemsPivotIndex.getIndexPaths().getStrings(),
                    XConverter.WS2VO(wsGetItemsPivotIndex.getWhereItem()),
                    wsGetItemsPivotIndex.getPivotDirections() == null ? null : wsGetItemsPivotIndex.getPivotDirections()
                            .getStrings(),
                    wsGetItemsPivotIndex.getIndexDirections() == null ? null : wsGetItemsPivotIndex.getIndexDirections()
                            .getStrings(), wsGetItemsPivotIndex.getStart(), wsGetItemsPivotIndex.getLimit());
            return new WSStringArray((String[]) res.toArray(new String[res.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray getChildrenItems(WSGetChildrenItems wsGetChildrenItems) throws RemoteException {
        try {
            Collection res = Util.getItemCtrl2Local().getChildrenItems(wsGetChildrenItems.getClusterName(),
                    wsGetChildrenItems.getConceptName(), wsGetChildrenItems.getPKXpaths().getStrings(),
                    wsGetChildrenItems.getFKXpath(), wsGetChildrenItems.getLabelXpath(), wsGetChildrenItems.getFatherPK(),
                    XConverter.WS2VO(wsGetChildrenItems.getWhereItem()), wsGetChildrenItems.getStart(), wsGetChildrenItems.getLimit());
            return new WSStringArray((String[]) res.toArray(new String[res.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSString count(WSCount wsCount) throws RemoteException {
        try {
            String countPath = wsCount.getCountPath();
            Map wcfContext = new HashMap();
            wcfContext.put(WhereConditionForcePivotFilter.FORCE_PIVOT, countPath);
            long count = Util.getItemCtrl2Local().count(new DataClusterPOJOPK(wsCount.getWsDataClusterPK().getPk()),
                    wsCount.getCountPath(), XConverter.WS2VO(wsCount.getWhereItem(), new WhereConditionForcePivotFilter(wcfContext)),
                    wsCount.getSpellTreshold());
            return new WSString(count + ""); //$NON-NLS-1$
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray getItems(WSGetItems wsGetItems) throws RemoteException {
        try {
            Map wcfContext = new HashMap();
            wcfContext.put(WhereConditionForcePivotFilter.FORCE_PIVOT, wsGetItems.getConceptName());
            Collection res = Util.getItemCtrl2Local().getItems(new DataClusterPOJOPK(wsGetItems.getWsDataClusterPK().getPk()),
                    wsGetItems.getConceptName(),
                    XConverter.WS2VO(wsGetItems.getWhereItem(), new WhereConditionForcePivotFilter(wcfContext)),
                    wsGetItems.getSpellTreshold(), wsGetItems.getSkip(), wsGetItems.getMaxItems(),
                    wsGetItems.getTotalCountOnFirstResult() == null ? false : wsGetItems.getTotalCountOnFirstResult());
            return new WSStringArray((String[]) res.toArray(new String[res.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray getItemsSort(WSGetItemsSort wsGetItemsSort) throws RemoteException {
        try {
            Map wcfContext = new HashMap();
            wcfContext.put(WhereConditionForcePivotFilter.FORCE_PIVOT, wsGetItemsSort.getConceptName());
            Collection res = Util.getItemCtrl2Local().getItems(
                    new DataClusterPOJOPK(wsGetItemsSort.getWsDataClusterPK().getPk()), wsGetItemsSort.getConceptName(),
                    XConverter.WS2VO(wsGetItemsSort.getWhereItem(), new WhereConditionForcePivotFilter(wcfContext)),
                    wsGetItemsSort.getSpellTreshold(), wsGetItemsSort.getSort(), wsGetItemsSort.getDir(),
                    wsGetItemsSort.getSkip(), wsGetItemsSort.getMaxItems(),
                    wsGetItemsSort.getTotalCountOnFirstResult() == null ? false : wsGetItemsSort.getTotalCountOnFirstResult());
            return new WSStringArray((String[]) res.toArray(new String[res.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSItemPKsByCriteriaResponse getItemPKsByCriteria(WSGetItemPKsByCriteria wsGetItemPKsByCriteria) throws RemoteException {
        return doGetItemPKsByCriteria(wsGetItemPKsByCriteria, false);
    }

    public WSItemPKsByCriteriaResponse getItemPKsByFullCriteria(WSGetItemPKsByFullCriteria wsGetItemPKsByFullCriteria)
            throws RemoteException {
        return doGetItemPKsByCriteria(wsGetItemPKsByFullCriteria.getWsGetItemPKsByCriteria(),
                wsGetItemPKsByFullCriteria.isUseFTSearch());
    }

    private WSItemPKsByCriteriaResponse doGetItemPKsByCriteria(WSGetItemPKsByCriteria wsGetItemPKsByCriteria, boolean useFTSearch)
            throws RemoteException {
        // With Universe, this method must be reviewed since various concepts
        // may be store in various revisions
        try {
            String dataClusterName = wsGetItemPKsByCriteria.getWsDataClusterPK().getPk();
            // Check if user is allowed to read the cluster
            ILocalUser user = LocalUser.getLocalUser();
            boolean authorized = false;
            if (MDMConfiguration.getAdminUser().equals(user.getUsername())
                    || LocalUser.UNAUTHENTICATED_USER.equals(user.getUsername())) {
                authorized = true;
            } else if (user.userCanRead(DataClusterPOJO.class, dataClusterName)) {
                authorized = true;
            }
            if (!authorized) {
                throw new RemoteException("Unauthorized read access on data cluster '" + dataClusterName + "' by user '"
                        + user.getUsername() + "'");
            }
            // If not all concepts are store in the same revision,
            // force the concept to be specified by the user.
            // It would be too demanding to get all the concepts in all revisions (?)
            // The meat of this method should be ported to ItemCtrlBean
            String revisionID;
            String conceptName = wsGetItemPKsByCriteria.getConceptName();
            if (conceptName == null) {
                if (user.getUniverse().getItemsRevisionIDs().size() > 0) {
                    throw new RemoteException("User " + user.getUsername() + " is using items coming from multiple revisions."
                            + " In that particular case, the concept must be specified");
                } else {
                    revisionID = user.getUniverse().getDefaultItemRevisionID();
                }
            } else {
                revisionID = user.getUniverse().getConceptRevisionID(conceptName);
            }
            ItemPKCriteria criteria = new ItemPKCriteria();
            criteria.setRevisionId(revisionID);
            criteria.setClusterName(dataClusterName);
            criteria.setConceptName(conceptName);
            criteria.setContentKeywords(wsGetItemPKsByCriteria.getContentKeywords());
            criteria.setKeysKeywords(wsGetItemPKsByCriteria.getKeysKeywords());
            criteria.setCompoundKeyKeywords(false);
            criteria.setFromDate(wsGetItemPKsByCriteria.getFromDate());
            criteria.setToDate(wsGetItemPKsByCriteria.getToDate());
            criteria.setMaxItems(wsGetItemPKsByCriteria.getMaxItems());
            criteria.setSkip(wsGetItemPKsByCriteria.getSkip());
            criteria.setUseFTSearch(useFTSearch);
            List<String> results = com.amalto.core.util.Util.getItemCtrl2Local().getItemPKsByCriteria(criteria);
            XPath xpath = XPathFactory.newInstance().newXPath();
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            WSItemPKsByCriteriaResponseResults[] res = new WSItemPKsByCriteriaResponseResults[results.size()];
            int i = 0;
            for (String result : results) {
                if (i == 0) {
                    res[i++] = new WSItemPKsByCriteriaResponseResults(System.currentTimeMillis(), new WSItemPK(
                            wsGetItemPKsByCriteria.getWsDataClusterPK(), result, null), ""); //$NON-NLS-1$
                    continue;
                }
                Element r = documentBuilder.parse(new InputSource(new StringReader(result))).getDocumentElement();
                long t = new Long(xpath.evaluate("t", r)); //$NON-NLS-1$
                String cn = xpath.evaluate("n", r); //$NON-NLS-1$
                String taskId = xpath.evaluate("taskId", r); //$NON-NLS-1$

                NodeList idsList = (NodeList) xpath.evaluate("./ids/i", r, XPathConstants.NODESET); //$NON-NLS-1$
                String[] ids = new String[idsList.getLength()];
                for (int j = 0; j < idsList.getLength(); j++) {
                    ids[j] = (idsList.item(j).getFirstChild() == null ? "" : idsList.item(j).getFirstChild().getNodeValue()); //$NON-NLS-1$
                }
                res[i++] = new WSItemPKsByCriteriaResponseResults(t, new WSItemPK(wsGetItemPKsByCriteria.getWsDataClusterPK(),
                        cn, ids), taskId);
            }
            return new WSItemPKsByCriteriaResponse(res);

        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSItem getItem(WSGetItem wsGetItem) throws RemoteException {
        try {
            if (wsGetItem.getWsItemPK().getIds() == null) {
                throw (new RemoteException("input ids is null!"));
            }
            ItemPOJOPK pk = new ItemPOJOPK(new DataClusterPOJOPK(wsGetItem.getWsItemPK().getWsDataClusterPK().getPk()), wsGetItem
                    .getWsItemPK().getConceptName(), wsGetItem.getWsItemPK().getIds());
            ItemPOJO pojo = Util.getItemCtrl2Local().getItem(pk);
            return new WSItem(wsGetItem.getWsItemPK().getWsDataClusterPK(), pojo.getDataModelName(), pojo.getDataModelRevision(),
                    wsGetItem.getWsItemPK().getConceptName(), wsGetItem.getWsItemPK().getIds(), pojo.getInsertionTime(),
                    pojo.getTaskId(), pojo.getProjectionAsString());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSBoolean existsItem(WSExistsItem wsExistsItem) throws RemoteException {
        try {
            return new WSBoolean((Util.getItemCtrl2Local().existsItem(
                    new ItemPOJOPK(new DataClusterPOJOPK(wsExistsItem.getWsItemPK().getWsDataClusterPK().getPk()), wsExistsItem
                            .getWsItemPK().getConceptName(), wsExistsItem.getWsItemPK().getIds())) != null));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    /**
     * @ejb.interface-method view-type = "service-endpoint"
     * @ejb.permission role-name = "authenticated" view-type = "service-endpoint"
     */
    public WSStringArray quickSearch(WSQuickSearch wsQuickSearch) throws RemoteException {
        try {
            Collection c = Util.getItemCtrl2Local().quickSearch(
                    new DataClusterPOJOPK(wsQuickSearch.getWsDataClusterPK().getPk()),
                    new ViewPOJOPK(wsQuickSearch.getWsViewPK().getPk()), wsQuickSearch.getSearchedValue(),
                    wsQuickSearch.isMatchAllWords(), wsQuickSearch.getSpellTreshold(), wsQuickSearch.getOrderBy(),
                    wsQuickSearch.getDirection(), wsQuickSearch.getSkip(), wsQuickSearch.getMaxItems());
            if (c == null) {
                return null;
            }
            return new WSStringArray((String[]) c.toArray(new String[c.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    protected WSItemPK POJO2WS(ItemPOJOPK itemPK) throws Exception {
        return new WSItemPK(new WSDataClusterPK(itemPK.getDataClusterPOJOPK().getUniqueId()), itemPK.getConceptName(),
                itemPK.getIds());
    }

    public WSString getBusinessConceptValue(WSGetBusinessConceptValue wsGetBusinessConceptValue) throws RemoteException {
        try {
            ItemPOJO iv = Util.getItemCtrl2Local().getItem(
                    new ItemPOJOPK(new DataClusterPOJOPK(wsGetBusinessConceptValue.getWsDataClusterPK().getPk()),
                            wsGetBusinessConceptValue.getWsBusinessConceptPK().getConceptName(), wsGetBusinessConceptValue
                            .getWsBusinessConceptPK().getIds()));
            return new WSString(itemAsString(iv));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray getFullPathValues(WSGetFullPathValues wsGetFullPathValues) throws RemoteException {
        try {
            Collection res = Util.getItemCtrl2Local().getFullPathValues(
                    new DataClusterPOJOPK(wsGetFullPathValues.getWsDataClusterPK().getPk()), wsGetFullPathValues.getFullPath(),
                    XConverter.WS2VO(wsGetFullPathValues.getWhereItem()), wsGetFullPathValues.getSpellThreshold(),
                    wsGetFullPathValues.getOrderBy(), wsGetFullPathValues.getDirection());
            if (res == null) {
                return null;
            }
            return new WSStringArray((String[]) res.toArray(new String[res.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    /**
     * Serializes the object to an xml string
     * @return the xml string
     */
    protected String itemAsString(ItemPOJO iv) throws Exception {
        StringBuilder item = new StringBuilder();
        item.append("<businessconcept><cluster>").append(iv.getDataClusterPOJOPK().getUniqueId()).append("</cluster>"); //$NON-NLS-1$ //$NON-NLS-2$
        String[] ids = iv.getItemIds();
        for (String id : ids) {
            item.append("<id>").append(id).append("</id>"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        item.append("<lastmodifiedtime>").append(iv.getInsertionTime()).append("</lastmodifiedtime>"); //$NON-NLS-1$ //$NON-NLS-2$
        item.append("<projection>").append(iv.getProjection()).append("</projection>"); //$NON-NLS-1$ //$NON-NLS-2$
        item.append("</businessconcept>"); //$NON-NLS-1$
        return item.toString();
    }

    public WSItemPK partialPutItem(WSPartialPutItem partialPutItem) throws RemoteException {
        try {
            SaverSession session = SaverSession.newSession();
            DocumentSaver saver = SaverHelper.saveItem(partialPutItem, session);
            // Cause items being saved to be committed to database.
            session.end();
            String[] savedId = saver.getSavedId();
            String savedConceptName = saver.getSavedConceptName();
            return new WSItemPK(new WSDataClusterPK(partialPutItem.getDatacluster()), savedConceptName, savedId);
        } catch (Exception e) {
            LOGGER.error("Could not do partial update.", e);
            throw new RemoteException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @param wsPutItem The record to be added/updated in MDM.
     * @return The PK of the record created/updated.
     * @throws java.rmi.RemoteException In case of server exception.
     */
    public WSItemPK putItem(WSPutItem wsPutItem) throws RemoteException {
        try {
            WSDataClusterPK dataClusterPK = wsPutItem.getWsDataClusterPK();
            WSDataModelPK dataModelPK = wsPutItem.getWsDataModelPK();
            String dataClusterName = dataClusterPK.getPk();
            String dataModelName = dataModelPK.getPk();
            SaverSession session = SaverSession.newSession();
            DocumentSaver saver;
            try {
                saver = SaverHelper.saveItem(wsPutItem, session, dataClusterName, dataModelName);
            } catch (Exception e) {
                try {
                    session.abort();
                } catch (Exception e1) {
                    LOGGER.error("Could not abort save session.", e1);
                }
                throw new RuntimeException(e);
            }
            // Cause items being saved to be committed to database.
            session.end();
            String[] savedId = saver.getSavedId();
            String savedConceptName = saver.getSavedConceptName();
            return new WSItemPK(dataClusterPK, savedConceptName, savedId);
        } catch (Exception e) {
            LOGGER.error("Error during save.", e);
            // TMDM-5594: Original cause was somehow lost during serialization,implementing a workaround here
            Throwable cause = e.getCause();
            if (cause == null) {
                throw new RemoteException(e.getLocalizedMessage(), e);
            } else {
                throw new RemoteException((cause.getCause() == null ? cause.getLocalizedMessage() : cause.getCause()
                        .getLocalizedMessage()), e);
            }
        }
    }

    /**
     * @param wsPutItemArray Items to save
     * @return An array of ids saved to database.
     * @throws java.rmi.RemoteException In case of server error
     */
    public WSItemPKArray putItemArray(WSPutItemArray wsPutItemArray) throws RemoteException {
        WSPutItem[] items = wsPutItemArray.getWsPutItem();
        try {
            List<WSItemPK> pks = new LinkedList<WSItemPK>();
            SaverSession session = SaverSession.newSession();
            for (WSPutItem item : items) {
                String dataClusterName = item.getWsDataClusterPK().getPk();
                String dataModelName = item.getWsDataModelPK().getPk();

                DocumentSaver saver;
                try {
                    saver = SaverHelper.saveItem(item, session, dataClusterName, dataModelName);
                } catch (Exception e) {
                    try {
                        session.abort();
                    } catch (Exception e1) {
                        LOGGER.error("Could not abort save session.", e1);
                    }
                    throw new RuntimeException(e);
                }
                pks.add(new WSItemPK(new WSDataClusterPK(), saver.getSavedConceptName(), saver.getSavedId()));
            }
            // Cause items being saved to be committed to database.
            session.end();
            return new WSItemPKArray(pks.toArray(new WSItemPK[pks.size()]));
        } catch (Exception e) {
            LOGGER.error("Error during save.", e);
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    /**
     * @param wsPutItemWithReportArray Records to be added to MDM.
     * @return An array of PKs for the records created/updated.
     * @throws java.rmi.RemoteException In case of server error.
     */
    public WSItemPKArray putItemWithReportArray(com.amalto.core.webservice.WSPutItemWithReportArray wsPutItemWithReportArray)
            throws RemoteException {
        try {
            WSPutItemWithReport[] items = wsPutItemWithReportArray.getWsPutItem();
            List<WSItemPK> pks = new LinkedList<WSItemPK>();
            SaverSession session = SaverSession.newSession();
            for (WSPutItemWithReport item : items) {
                WSPutItem wsPutItem = item.getWsPutItem();
                String source = item.getSource();
                String dataClusterName = wsPutItem.getWsDataClusterPK().getPk();
                String dataModelName = wsPutItem.getWsDataModelPK().getPk();
                DocumentSaver saver;
                try {
                    saver = SaverHelper.saveItemWithReport(wsPutItem, session, dataClusterName, dataModelName, source,
                            item.getInvokeBeforeSaving());
                    // Expected (legacy) behavior: set the before saving message as source.
                    item.setSource(saver.getBeforeSavingMessage());
                } catch (SaveException e) {
                    try {
                        session.abort();
                    } catch (Exception e1) {
                        LOGGER.error("Could not abort save session.", e1);
                    }
                    // Expected (legacy) behavior: set the before saving message as source.
                    item.setSource(e.getBeforeSavingMessage());
                    throw new RemoteException("Could not save record.", e);
                }
                pks.add(new WSItemPK(new WSDataClusterPK(), saver.getSavedConceptName(), saver.getSavedId()));
            }
            // Cause items being saved to be committed to database.
            session.end();
            return new WSItemPKArray(pks.toArray(new WSItemPK[pks.size()]));
        } catch (Exception e) {
            LOGGER.error("Error during save.", e);
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    /**
     * @param wsPutItemWithReport Object that describe the record to be added/updated.
     * @return The PK of the newly inserted document.
     * @throws java.rmi.RemoteException In case of server exception.
     */
    public WSItemPK putItemWithReport(com.amalto.core.webservice.WSPutItemWithReport wsPutItemWithReport) throws RemoteException {
        try {
            WSPutItem wsPutItem = wsPutItemWithReport.getWsPutItem();
            WSDataClusterPK dataClusterPK = wsPutItem.getWsDataClusterPK();
            WSDataModelPK dataModelPK = wsPutItem.getWsDataModelPK();
            String dataClusterName = dataClusterPK.getPk();
            String dataModelName = dataModelPK.getPk();
            SaverSession session = SaverSession.newSession();
            DocumentSaver saver;
            try {
                saver = SaverHelper.saveItemWithReport(wsPutItem, session, dataClusterName, dataModelName,
                        wsPutItemWithReport.getSource(), wsPutItemWithReport.getInvokeBeforeSaving());
                // Expected (legacy) behavior: set the before saving message as source.
                wsPutItemWithReport.setSource(saver.getBeforeSavingMessage());
            } catch (SaveException e) {
                try {
                    session.abort();
                } catch (Exception e1) {
                    LOGGER.error("Could not abort save session.", e1);
                }
                ValidateException ve = Util.getException(e, ValidateException.class);
                if (ve != null) {
                    throw e;
                }
                // Expected (legacy) behavior: set the before saving message as source.
                wsPutItemWithReport.setSource(e.getBeforeSavingMessage());
                throw new RemoteException("Could not save record.", e);
            }
            // Cause items being saved to be committed to database.
            session.end();

            String[] savedId = saver.getSavedId();
            String conceptName = saver.getSavedConceptName();
            return new WSItemPK(dataClusterPK, conceptName, savedId);
        } catch (Exception e) {
            LOGGER.error("Error during save.", e);
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    /**
     * @param wsPutItemWithCustomReport Information about a put item with report that includes a special user name.
     * @ejb.interface-method view-type = "service-endpoint"
     * @ejb.permission role-name = "authenticated" view-type = "service-endpoint"
     * @return The PK of the newly created record.
     * @throws java.rmi.RemoteException In case of server exception.
     */
    public WSItemPK putItemWithCustomReport(com.amalto.core.webservice.WSPutItemWithCustomReport wsPutItemWithCustomReport)
            throws RemoteException {
        try {
            WSPutItemWithReport wsPutItemWithReport = wsPutItemWithCustomReport.getWsPutItemWithReport();
            WSPutItem wsPutItem = wsPutItemWithReport.getWsPutItem();
            WSDataClusterPK dataClusterPK = wsPutItem.getWsDataClusterPK();
            WSDataModelPK dataModelPK = wsPutItem.getWsDataModelPK();
            String dataClusterName = dataClusterPK.getPk();
            String dataModelName = dataModelPK.getPk();
            // This method uses a special user
            SaverSession session = SaverSession.newUserSession(wsPutItemWithCustomReport.getUser());
            DocumentSaver saver;
            try {
                saver = SaverHelper.saveItemWithReport(wsPutItem, session, dataClusterName, dataModelName,
                        wsPutItemWithReport.getSource(), wsPutItemWithReport.getInvokeBeforeSaving());
                // Expected (legacy) behavior: set the before saving message as source.
                wsPutItemWithReport.setSource(saver.getBeforeSavingMessage());
                // Cause items being saved to be committed to database.
                session.end();
            } catch (SaveException e) {
                try {
                    session.abort();
                } catch (Exception e1) {
                    LOGGER.error("Could not abort save session.", e1);
                }
                // Expected (legacy) behavior: set the before saving message as source.
                wsPutItemWithReport.setSource(e.getBeforeSavingMessage());
                throw new RemoteException("Could not save record.", e);
            }
            String[] savedId = saver.getSavedId();
            String conceptName = saver.getSavedConceptName();
            return new WSItemPK(dataClusterPK, conceptName, savedId);
        } catch (Exception e) {
            LOGGER.error("Error during save.", e);
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSPipeline extractUsingTransformer(WSExtractUsingTransformer wsExtractUsingTransformer) throws RemoteException {
        throw new RemoteException("Not supported.");
    }

    public WSPipeline extractUsingTransformerThruView(WSExtractUsingTransformerThruView wsExtractUsingTransformerThruView)
            throws RemoteException {
        try {
            TransformerContext context = Util.getItemCtrl2Local().extractUsingTransformerThroughView(
                    new DataClusterPOJOPK(wsExtractUsingTransformerThruView.getWsDataClusterPK().getPk()),
                    new TransformerV2POJOPK(wsExtractUsingTransformerThruView.getWsTransformerPK().getPk()),
                    new ViewPOJOPK(wsExtractUsingTransformerThruView.getWsViewPK().getPk()),
                    XConverter.WS2VO(wsExtractUsingTransformerThruView.getWhereItem()),
                    wsExtractUsingTransformerThruView.getSpellTreshold(), wsExtractUsingTransformerThruView.getOrderBy(),
                    wsExtractUsingTransformerThruView.getDirection(), wsExtractUsingTransformerThruView.getSkip(),
                    wsExtractUsingTransformerThruView.getMaxItems());
            HashMap<String, TypedContent> pipeline = context.getPipelineClone();
            return XConverter.POJO2WS(pipeline);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSItemPK deleteItem(WSDeleteItem wsDeleteItem) throws RemoteException {
        try {
            WSItemPK itemPK = wsDeleteItem.getWsItemPK();
            deleteItemWithReport(new WSDeleteItemWithReport(itemPK,
                    wsDeleteItem.getSource(),
                    UpdateReportPOJO.OPERATION_TYPE_PHYSICAL_DELETE,
                    "/", //$NON-NLS-1$
                    LocalUser.getLocalUser().getUsername(),
                    wsDeleteItem.getInvokeBeforeDeleting(),
                    wsDeleteItem.getWithReport(),
                    wsDeleteItem.getOverride()));
            return itemPK;
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    private void pushToUpdateReport(String dataClusterPK, String dataModelPK, String concept, String[] ids, boolean trigger,
            String source, String operationType, String deleteUser) throws Exception {
        ILocalUser user = LocalUser.getLocalUser();
        Map<String, UpdateReportItemPOJO> updateReportItemsMap = new HashMap<String, UpdateReportItemPOJO>();
        String userName;
        if (deleteUser != null && deleteUser.length() > 0) {
            userName = deleteUser;
        } else {
            userName = user.getUsername();
        }
        String revisionID = ""; //$NON-NLS-1$
        UniversePOJO universe = user.getUniverse();
        if (universe != null) {
            revisionID = universe.getConceptRevisionID(concept);
        }
        UpdateReportPOJO updateReportPOJO = new UpdateReportPOJO(concept, Util.joinStrings(ids, "."), operationType, //$NON-NLS-1$
                source, System.currentTimeMillis(), dataClusterPK, dataModelPK, userName, revisionID, updateReportItemsMap);
        WSItemPK itemPK = putItem(new WSPutItem(new WSDataClusterPK(UpdateReportPOJO.DATA_CLUSTER), updateReportPOJO.serialize(),
                new WSDataModelPK(UpdateReportPOJO.DATA_MODEL), false));
        if (trigger) {
            routeItemV2(new WSRouteItemV2(itemPK));
        }
    }

    public WSString deleteItemWithReport(WSDeleteItemWithReport wsDeleteItem) throws RemoteException {
        try {
            String dataClusterPK = wsDeleteItem.getWsItemPK().getWsDataClusterPK().getPk();
            String concept = wsDeleteItem.getWsItemPK().getConceptName();
            String[] ids = wsDeleteItem.getWsItemPK().getIds();
            ItemPOJOPK pk = new ItemPOJOPK(new DataClusterPOJOPK(dataClusterPK), concept, ids);
            ItemPOJO pojo = Util.getItemCtrl2Local().getItem(pk);
            if (pojo == null) {
                throw new EntityNotFoundException(pk);
            }
            String dataModelPK = pojo.getDataModelName();
            if (UpdateReportPOJO.OPERATION_TYPE_LOGICAL_DELETE.equals(wsDeleteItem.getOperateType())) {
                Util.getItemCtrl2Local().dropItem(pk, "/", wsDeleteItem.getOverride()); //$NON-NLS-1$
                if (!UpdateReportPOJO.DATA_CLUSTER.equals(dataClusterPK) && wsDeleteItem.getPushToUpdateReport()) {
                    pushToUpdateReport(dataClusterPK, dataModelPK, concept, ids, wsDeleteItem.getInvokeBeforeSaving(),
                            wsDeleteItem.getSource(), wsDeleteItem.getOperateType(), wsDeleteItem.getUser());
                }
                return new WSString("logical delete item successful!"); //$NON-NLS-1$
            } else { // Physical delete
                String message = "physical delete item successful!"; //$NON-NLS-1$
                if (wsDeleteItem.getInvokeBeforeSaving()) {
                    Util.BeforeDeleteResult result = Util.beforeDeleting(dataClusterPK, concept, ids,
                            wsDeleteItem.getOperateType());
                    if (result != null) { // There was a before delete process to execute
                        if (!"error".equals(result.type)) { //$NON-NLS-1$
                            message = result.message;
                        } else {
                            if (result.message == null) {
                                return new WSString(
                                        "Could not retrieve the validation process result. An error might have occurred. The record was not deleted."); //$NON-NLS-1$
                            } else {
                                throw new BeforeDeletingErrorException(result.message);
                            }
                        }
                    }
                }
                // Now before delete process (if any configured) was called, perform delete.
                ItemPOJOPK deleteItem = Util.getItemCtrl2Local().deleteItem(pk, wsDeleteItem.getOverride());
                if (deleteItem != null) {
                    if (!UpdateReportPOJO.DATA_CLUSTER.equals(dataClusterPK) && wsDeleteItem.getPushToUpdateReport()) {
                        pushToUpdateReport(dataClusterPK, dataModelPK, concept, ids, wsDeleteItem.getInvokeBeforeSaving(),
                                wsDeleteItem.getSource(), wsDeleteItem.getOperateType(), wsDeleteItem.getUser());
                    }
                } else {
                    message = "ERROR - Unable to delete item"; //$NON-NLS-1$
                }
                return new WSString(message);
            }
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSInt deleteItems(WSDeleteItems wsDeleteItems) throws RemoteException {
        try {
            int numItems = Util.getItemCtrl2Local().deleteItems(
                    new DataClusterPOJOPK(wsDeleteItems.getWsDataClusterPK().getPk()), wsDeleteItems.getConceptName(),
                    XConverter.WS2VO(wsDeleteItems.getWsWhereItem()), wsDeleteItems.getSpellTreshold(), wsDeleteItems.getOverride());
            return new WSInt(numItems);
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    /**
     * @ejb.interface-method view-type = "service-endpoint"
     * @ejb.permission role-name = "authenticated" view-type = "service-endpoint"
     */
    public WSDroppedItemPK dropItem(WSDropItem wsDropItem) throws RemoteException {
        try {
            WSItemPK wsItemPK = wsDropItem.getWsItemPK();
            deleteItemWithReport(new WSDeleteItemWithReport(wsItemPK,
                    wsDropItem.getSource(),
                    UpdateReportPOJO.OPERATION_TYPE_LOGICAL_DELETE,
                    wsDropItem.getPartPath(),
                    LocalUser.getLocalUser().getUsername(),
                    wsDropItem.getInvokeBeforeDeleting(),
                    wsDropItem.getWithReport(),
                    wsDropItem.getOverride()));
            return new WSDroppedItemPK(wsItemPK, wsDropItem.getPartPath(), null); // TODO Revision
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray runQuery(WSRunQuery wsRunQuery) throws RemoteException {
        try {
            DataClusterPOJOPK dcpk = (wsRunQuery.getWsDataClusterPK() == null) ? null : new DataClusterPOJOPK(wsRunQuery
                    .getWsDataClusterPK().getPk());
            Collection<String> result = Util.getItemCtrl2Local().runQuery(wsRunQuery.getRevisionID(), dcpk,
                    wsRunQuery.getQuery(), wsRunQuery.getParameters());
            // stored procedure may modify the db, so we need to clear the cache
            Util.getXmlServerCtrlLocal().clearCache();
            return new WSStringArray(result.toArray(new String[result.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSServiceGetDocument getServiceDocument(WSString serviceName) throws RemoteException {
        try {
            Object service = Util.retrieveComponent(null, "amalto/local/service/" + serviceName.getValue()); //$NON-NLS-1$
            String desc = ""; //$NON-NLS-1$
            Object descObject = Util.getMethod(service, "getDescription").invoke(service, ""); //$NON-NLS-1$ //$NON-NLS-2$
            if (descObject != null) {
                desc = (String) descObject;
            }
            String configuration = ""; //$NON-NLS-1$
            Object configurationObject = Util.getMethod(service, "getConfiguration").invoke(service, ""); //$NON-NLS-1$ //$NON-NLS-2$
            if (configurationObject != null) {
                configuration = (String) configurationObject;
            }
            String doc = ""; //$NON-NLS-1$
            String schema = ""; //$NON-NLS-1$
            String defaultConf = ""; //$NON-NLS-1$
            try {
                Method getDocumentationMethod = Util.getMethod(service, "getDocumentation"); //$NON-NLS-1$
                if (getDocumentationMethod != null) {
                    Object docObject = getDocumentationMethod.invoke(service, ""); //$NON-NLS-1$
                    if (docObject != null) {
                        doc = (String) docObject;
                    }
                }
                Method getDefaultConfigurationMethod = Util.getMethod(service, "getDefaultConfiguration"); //$NON-NLS-1$
                if (getDefaultConfigurationMethod != null) {
                    Object defaultConfObject = getDefaultConfigurationMethod.invoke(service);
                    if (defaultConfObject != null) {
                        defaultConf = (String) defaultConfObject;
                    }
                }

                Method getConfigurationSchemaMethod = Util.getMethod(service, "getConfigurationSchema"); //$NON-NLS-1$
                if (getConfigurationSchemaMethod != null) {
                    Object schemaObject = getConfigurationSchemaMethod.invoke(service);
                    if (schemaObject != null) {
                        schema = (String) schemaObject;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("IXtentisWSDelegator.getServiceDocument error.", e);
            }
            return new WSServiceGetDocument(desc, configuration, doc, schema, defaultConf);
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSString getServiceConfiguration(WSServiceGetConfiguration wsGetConfiguration) throws RemoteException {
        try {
            Object service = Util.retrieveComponent(null, wsGetConfiguration.getJndiName());

            String configuration = (String) Util.getMethod(service, "getConfiguration").invoke(service, //$NON-NLS-1$
                    wsGetConfiguration.getOptionalParameter());
            return new WSString(configuration);
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSCheckServiceConfigResponse checkServiceConfiguration(WSCheckServiceConfigRequest serviceName) throws RemoteException {
        try {
            Object service = Util.retrieveComponent(null, "amalto/local/service/" + serviceName.getJndiName()); //$NON-NLS-1$

            Boolean result = (Boolean) Util.getMethod(service, "checkConfigure").invoke(service, //$NON-NLS-1$
                    serviceName.getConf());
            return new WSCheckServiceConfigResponse(result);
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSString putServiceConfiguration(WSServicePutConfiguration wsPutConfiguration) throws RemoteException {
        try {
            Object service = Util.retrieveComponent(null, wsPutConfiguration.getJndiName());
            Util.getMethod(service, "putConfiguration").invoke(service, wsPutConfiguration.getConfiguration()); //$NON-NLS-1$
            return new WSString(wsPutConfiguration.getConfiguration());
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSString serviceAction(WSServiceAction wsServiceAction) throws RemoteException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("serviceAction() " + wsServiceAction.getJndiName());
        }
        try {
            Object service = com.amalto.core.util.Util.retrieveComponent(null, wsServiceAction.getJndiName());
            String result;
            if (WSServiceActionCode.EXECUTE.equals(wsServiceAction.getWsAction())) {
                Method method = com.amalto.core.util.Util.getMethod(service, wsServiceAction.getMethodName());
                result = (String) method.invoke(service, wsServiceAction.getMethodParameters());
            } else {
                if (WSServiceActionCode.START.equals(wsServiceAction.getWsAction())) {
                    com.amalto.core.util.Util.getMethod(service, "start").invoke(service); //$NON-NLS-1$
                } else if (WSServiceActionCode.STOP.equals(wsServiceAction.getWsAction())) {
                    com.amalto.core.util.Util.getMethod(service, "stop").invoke(service); //$NON-NLS-1$
                }
                result = (String) com.amalto.core.util.Util.getMethod(service, "getStatus").invoke(service); //$NON-NLS-1$
            }
            return new WSString(result);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSServicesList getServicesList(WSGetServicesList wsGetServicesList) throws RemoteException {
        try {
            ArrayList<WSServicesListItem> wsList = new ArrayList<WSServicesListItem>();
            List<String> jndiList = Util.getRuntimeServiceJndiList();
            String serviceJndiPrefix = "amalto/local/service/"; //$NON-NLS-1$
            for (String jndi : jndiList) {
                WSServicesListItem item = new WSServicesListItem();
                item.setJndiName(jndi.replaceAll(serviceJndiPrefix, "")); //$NON-NLS-1$
                wsList.add(item);
            }
            return new WSServicesList(wsList.toArray(new WSServicesListItem[wsList.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSString ping() throws RemoteException {
        try {
            return new WSString("OK");
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    protected Connection getConnection(String JNDIName) throws RemoteException {
        try {
            if (cxFactory == null) {
                cxFactory = (ConnectionFactory) (new InitialContext()).lookup(JNDIName);
            }
            return cxFactory.getConnection();
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    public WSStoredProcedurePK deleteStoredProcedure(WSDeleteStoredProcedure wsStoredProcedureDelete) throws RemoteException {
        try {
            StoredProcedureCtrlLocal ctrl = Util.getStoredProcedureCtrlLocal();
            StoredProcedurePOJOPK pk = ctrl.removeStoredProcedure(new StoredProcedurePOJOPK(wsStoredProcedureDelete
                    .getWsStoredProcedurePK().getPk()));
            return new WSStoredProcedurePK(pk.getIds()[0]);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    public WSStringArray executeStoredProcedure(WSExecuteStoredProcedure wsExecuteStoredProcedure) throws RemoteException {
        try {
            StoredProcedureCtrlLocal ctrl = Util.getStoredProcedureCtrlLocal();
            DataClusterPOJOPK dcpk = null;
            if (wsExecuteStoredProcedure.getWsDataClusterPK() != null) {
                dcpk = new DataClusterPOJOPK(wsExecuteStoredProcedure.getWsDataClusterPK().getPk());
            }
            Collection collection = ctrl.execute(new StoredProcedurePOJOPK(wsExecuteStoredProcedure.getWsStoredProcedurePK()
                    .getPk()), wsExecuteStoredProcedure.getRevisionID(), dcpk, wsExecuteStoredProcedure.getParameters());
            if (collection == null) {
                return null;
            }
            String[] documents = new String[collection.size()];
            int i = 0;
            for (Object o : collection) {
                documents[i++] = (String) o;
            }
            return new WSStringArray(documents);
        } catch (Exception e) {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }
    }

    public WSStoredProcedure getStoredProcedure(WSGetStoredProcedure wsGetStoredProcedure) throws RemoteException {
        try {
            StoredProcedureCtrlLocal ctrl = Util.getStoredProcedureCtrlLocal();
            StoredProcedurePOJO pojo = ctrl.getStoredProcedure(new StoredProcedurePOJOPK(wsGetStoredProcedure
                    .getWsStoredProcedurePK().getPk()));
            return XConverter.POJO2WS(pojo);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSBoolean existsStoredProcedure(WSExistsStoredProcedure wsExistsStoredProcedure) throws RemoteException {
        try {
            StoredProcedureCtrlLocal ctrl = Util.getStoredProcedureCtrlLocal();
            StoredProcedurePOJO pojo = ctrl.existsStoredProcedure(new StoredProcedurePOJOPK(wsExistsStoredProcedure
                    .getWsStoredProcedurePK().getPk()));
            return new WSBoolean(pojo != null);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSStoredProcedurePKArray getStoredProcedurePKs(WSRegexStoredProcedure regex) throws RemoteException {
        try {
            StoredProcedureCtrlLocal ctrl = Util.getStoredProcedureCtrlLocal();
            Collection collection = ctrl.getStoredProcedurePKs(regex.getRegex());
            if (collection == null) {
                return null;
            }
            WSStoredProcedurePK[] pks = new WSStoredProcedurePK[collection.size()];
            int i = 0;
            for (Object o : collection) {
                pks[i++] = new WSStoredProcedurePK(((StoredProcedurePOJOPK) o).getIds()[0]);
            }
            return new WSStoredProcedurePKArray(pks);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSStoredProcedurePK putStoredProcedure(WSPutStoredProcedure wsStoredProcedure) throws RemoteException {
        try {
            StoredProcedureCtrlLocal ctrl = Util.getStoredProcedureCtrlLocal();
            StoredProcedurePOJOPK pk = ctrl.putStoredProcedure(XConverter.WS2POJO(wsStoredProcedure.getWsStoredProcedure()));
            return new WSStoredProcedurePK(pk.getIds()[0]);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSMenuPK deleteMenu(WSDeleteMenu wsMenuDelete) throws RemoteException {
        try {
            MenuCtrlLocal ctrl = Util.getMenuCtrlLocal();
            return new WSMenuPK(ctrl.removeMenu(new MenuPOJOPK(wsMenuDelete.getWsMenuPK().getPk())).getUniqueId());
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSMenu getMenu(WSGetMenu wsGetMenu) throws RemoteException {
        try {
            MenuCtrlLocal ctrl = Util.getMenuCtrlLocal();
            MenuPOJO pojo = ctrl.getMenu(new MenuPOJOPK(wsGetMenu.getWsMenuPK().getPk()));
            return XConverter.POJO2WS(pojo);
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSBoolean existsMenu(WSExistsMenu wsExistsMenu) throws RemoteException {
        try {
            MenuCtrlLocal ctrl = Util.getMenuCtrlLocal();
            MenuPOJO pojo = ctrl.existsMenu(new MenuPOJOPK(wsExistsMenu.getWsMenuPK().getPk()));
            return new WSBoolean(pojo != null);
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSMenuPKArray getMenuPKs(WSGetMenuPKs regex) throws RemoteException {
        try {
            MenuCtrlLocal ctrl = Util.getMenuCtrlLocal();
            Collection collection = ctrl.getMenuPKs(regex.getRegex());
            if (collection == null) {
                return null;
            }
            WSMenuPK[] pks = new WSMenuPK[collection.size()];
            int i = 0;
            for (Object o : collection) {
                pks[i++] = new WSMenuPK(((MenuPOJOPK) o).getUniqueId());
            }
            return new WSMenuPKArray(pks);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSMenuPK putMenu(WSPutMenu wsMenu) throws RemoteException {
        try {
            MenuCtrlLocal ctrl = Util.getMenuCtrlLocal();
            MenuPOJOPK pk = ctrl.putMenu(XConverter.WS2POJO(wsMenu.getWsMenu()));
            return new WSMenuPK(pk.getUniqueId());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    public WSBackgroundJob getBackgroundJob(WSGetBackgroundJob wsBackgroundJobGet) throws RemoteException {
        try {
            return XConverter.POJO2WS(Util.getBackgroundJobCtrlLocal().getBackgroundJob(new BackgroundJobPOJOPK(wsBackgroundJobGet.getPk())));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSBackgroundJobPKArray findBackgroundJobPKs(WSFindBackgroundJobPKs wsFindBackgroundJobPKs) throws RemoteException {
        try {
            throw new RemoteException("WSBackgroundJobPKArray is not implemented in this version of the core");
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSBackgroundJobPK putBackgroundJob(WSPutBackgroundJob wsPutJob) throws RemoteException {
        try {
            return new WSBackgroundJobPK(Util.getBackgroundJobCtrlLocal()
                    .putBackgroundJob(XConverter.WS2POJO(wsPutJob.getWsBackgroundJob())).getUniqueId());
        } catch (Exception e) {
            throw new EJBException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()));
        }
    }

    public WSUniverse getCurrentUniverse(WSGetCurrentUniverse wsGetCurrentUniverse) throws RemoteException {
        try {
            // Fetch the user
            ILocalUser user = LocalUser.getLocalUser();
            return XConverter.POJO2WS(user.getUniverse());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    public WSTransformerPK deleteTransformer(WSDeleteTransformer wsTransformerDelete) throws RemoteException {
        try {
            TransformerCtrlLocal ctrl = Util.getTransformerCtrlLocal();
            return new WSTransformerPK(ctrl.removeTransformer(
                    new TransformerPOJOPK(wsTransformerDelete.getWsTransformerPK().getPk())).getUniqueId());

        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    public WSTransformer getTransformer(WSGetTransformer wsGetTransformer) throws RemoteException {
        try {
            TransformerCtrlLocal ctrl = Util.getTransformerCtrlLocal();
            TransformerPOJO pojo = ctrl.getTransformer(new TransformerPOJOPK(wsGetTransformer.getWsTransformerPK().getPk()));
            return XConverter.POJO2WS(pojo);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    public WSBoolean existsTransformer(WSExistsTransformer wsExistsTransformer) throws RemoteException {
        try {
            TransformerCtrlLocal ctrl = Util.getTransformerCtrlLocal();
            TransformerPOJO pojo = ctrl
                    .existsTransformer(new TransformerPOJOPK(wsExistsTransformer.getWsTransformerPK().getPk()));
            return new WSBoolean(pojo != null);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    public WSTransformerPKArray getTransformerPKs(WSGetTransformerPKs regex) throws RemoteException {
        try {
            TransformerCtrlLocal ctrl = Util.getTransformerCtrlLocal();
            Collection collection = ctrl.getTransformerPKs(regex.getRegex());
            if (collection == null) {
                return null;
            }
            WSTransformerPK[] pks = new WSTransformerPK[collection.size()];
            int i = 0;
            for (Object o : collection) {
                pks[i++] = new WSTransformerPK(((TransformerPOJOPK) o).getUniqueId());
            }
            return new WSTransformerPKArray(pks);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    public WSTransformerPK putTransformer(WSPutTransformer wsTransformer) throws RemoteException {
        try {
            TransformerCtrlLocal ctrl = Util.getTransformerCtrlLocal();
            TransformerPOJOPK pk = ctrl.putTransformer(XConverter.WS2POJO(wsTransformer.getWsTransformer()));
            return new WSTransformerPK(pk.getUniqueId());
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    public WSPipeline processBytesUsingTransformer(WSProcessBytesUsingTransformer wsProjectBytes) throws RemoteException {
        try {
            TransformerPluginContext context = Util.getTransformerCtrlLocal().process(
                    new com.amalto.core.util.TypedContent(null, wsProjectBytes.getWsBytes().getBytes(),
                            wsProjectBytes.getContentType()), new TransformerPOJOPK(wsProjectBytes.getWsTransformerPK().getPk()),
                    XConverter.WS2POJO(wsProjectBytes.getWsOutputDecisionTable()));
            HashMap<String, com.amalto.core.util.TypedContent> pipeline = (HashMap<String, com.amalto.core.util.TypedContent>) context
                    .get(TransformerCtrlBean.CTX_PIPELINE);
            // Add the Item PKs to the pipeline as comma separated lines
            String pksAsLine = "";//$NON-NLS-1$
            Collection<ItemPOJOPK> pks = (Collection<ItemPOJOPK>) context.get(TransformerCtrlBean.CTX_PKS);
            for (ItemPOJOPK pk : pks) {
                if (!"".equals(pksAsLine)) { //$NON-NLS-1$
                    pksAsLine += "\n";//$NON-NLS-1$
                }
                pksAsLine += pk.getConceptName() + "," + Util.joinStrings(pk.getIds(), ",");//$NON-NLS-1$ //$NON-NLS-2$
            }
            pipeline.put(TransformerCtrlBean.CTX_PKS, new com.amalto.core.util.TypedContent(null, pksAsLine.getBytes("UTF-8"), //$NON-NLS-1$
                    "text/plain; charset=\"utf-8\""));//$NON-NLS-1$
            // return the pipeline
            return XConverter.POJO2WSOLD(pipeline);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSPipeline processFileUsingTransformer(WSProcessFileUsingTransformer wsProcessFile) throws RemoteException {
        try {
            // read the entire file into bytes
            TransformerPluginContext context = Util.getTransformerCtrlLocal().process(
                    new com.amalto.core.util.TypedContent(new FileInputStream(new File(wsProcessFile.getFileName())), null,
                            wsProcessFile.getContentType()), new TransformerPOJOPK(wsProcessFile.getWsTransformerPK().getPk()),
                    XConverter.WS2POJO(wsProcessFile.getWsOutputDecisionTable()));
            HashMap<String, com.amalto.core.util.TypedContent> pipeline = (HashMap<String, com.amalto.core.util.TypedContent>) context
                    .get(TransformerCtrlBean.CTX_PIPELINE);
            // Add the Item PKs to the pipeline as comma separated lines
            String pksAsLine = "";//$NON-NLS-1$
            Collection<ItemPOJOPK> pks = (Collection<ItemPOJOPK>) context.get(TransformerCtrlBean.CTX_PKS);
            for (ItemPOJOPK pk : pks) {
                if (!"".equals(pksAsLine)) { //$NON-NLS-1$
                    pksAsLine += "\n";//$NON-NLS-1$
                }
                pksAsLine += pk.getConceptName() + "," + Util.joinStrings(pk.getIds(), ",");//$NON-NLS-1$ //$NON-NLS-2$
            }
            pipeline.put(TransformerCtrlBean.CTX_PKS, new com.amalto.core.util.TypedContent(null, pksAsLine.getBytes("UTF-8"),//$NON-NLS-1$
                    "text/plain; charset=\"utf-8\""));//$NON-NLS-1$
            // return the pipeline
            return XConverter.POJO2WSOLD(pipeline);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSBackgroundJobPK processBytesUsingTransformerAsBackgroundJob(
            WSProcessBytesUsingTransformerAsBackgroundJob wsProcessBytesUsingTransformerAsBackgroundJob) throws RemoteException {
        try {
            return new WSBackgroundJobPK(Util
                    .getTransformerCtrlLocal()
                    .processBytesAsBackgroundJob(wsProcessBytesUsingTransformerAsBackgroundJob.getWsBytes().getBytes(),
                            wsProcessBytesUsingTransformerAsBackgroundJob.getContentType(),
                            new TransformerPOJOPK(wsProcessBytesUsingTransformerAsBackgroundJob.getWsTransformerPK().getPk()),
                            XConverter.WS2POJO(wsProcessBytesUsingTransformerAsBackgroundJob.getWsOutputDecisionTable())).getUniqueId());
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSBackgroundJobPK processFileUsingTransformerAsBackgroundJob(
            WSProcessFileUsingTransformerAsBackgroundJob wsProcessFileUsingTransformerAsBackgroundJob) throws RemoteException {
        try {
            return new WSBackgroundJobPK(Util
                    .getTransformerCtrlLocal()
                    .processFileAsBackgroundJob(wsProcessFileUsingTransformerAsBackgroundJob.getFileName(),
                            wsProcessFileUsingTransformerAsBackgroundJob.getContentType(),
                            new TransformerPOJOPK(wsProcessFileUsingTransformerAsBackgroundJob.getWsTransformerPK().getPk()),
                            XConverter.WS2POJO(wsProcessFileUsingTransformerAsBackgroundJob.getWsOutputDecisionTable())).getUniqueId());
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSDroppedItemPKArray findAllDroppedItemsPKs(WSFindAllDroppedItemsPKs regex) throws RemoteException {
        try {
            List droppedItemPOJOPKs = Util.getDroppedItemCtrlLocal().findAllDroppedItemsPKs(regex.getRegex());
            WSDroppedItemPK[] wsDroppedItemPKs = new WSDroppedItemPK[droppedItemPOJOPKs.size()];
            for (int i = 0; i < droppedItemPOJOPKs.size(); i++) {
                DroppedItemPOJOPK droppedItemPOJOPK = (DroppedItemPOJOPK) droppedItemPOJOPKs.get(i);
                wsDroppedItemPKs[i] = XConverter.POJO2WS(droppedItemPOJOPK);
            }
            return new WSDroppedItemPKArray(wsDroppedItemPKs);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSDroppedItem loadDroppedItem(WSLoadDroppedItem wsLoadDroppedItem) throws RemoteException {
        try {
            DroppedItemPOJO droppedItemPOJO = Util.getDroppedItemCtrlLocal().loadDroppedItem(
                    XConverter.WS2POJO(wsLoadDroppedItem.getWsDroppedItemPK()));
            return XConverter.POJO2WS(droppedItemPOJO);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSItemPK recoverDroppedItem(WSRecoverDroppedItem wsRecoverDroppedItem) throws RemoteException {
        try {
            // Restore record
            DroppedItemPOJOPK droppedItemPOJOPK = XConverter.WS2POJO(wsRecoverDroppedItem.getWsDroppedItemPK());
            ItemPOJOPK itemPOJOPK = Util.getDroppedItemCtrlLocal().recoverDroppedItem(droppedItemPOJOPK);
            // Generate journal event (after restore operation's completed).
            WSItemPK itemPK = wsRecoverDroppedItem.getWsDroppedItemPK().getWsItemPK();
            String operationType = UpdateReportPOJO.OPERATION_TYPE_RESTORED;
            String clusterName = itemPK.getWsDataClusterPK().getPk();
            String dataModelName = clusterName; // TODO Missing data model name
            String conceptName = itemPK.getConceptName();
            String[] ids = itemPK.getIds();
            pushToUpdateReport(clusterName, dataModelName, conceptName, ids, true, "genericUI", operationType, null); //$NON-NLS-1$
            return XConverter.POJO2WS(itemPOJOPK);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSDroppedItemPK removeDroppedItem(WSRemoveDroppedItem wsRemoveDroppedItem) throws RemoteException {
        try {
            WSItemPK itemPK = wsRemoveDroppedItem.getWsDroppedItemPK().getWsItemPK();
            String clusterName = itemPK.getWsDataClusterPK().getPk();
            String dataModelName = clusterName; // TODO Missing data model name
            String conceptName = itemPK.getConceptName();
            String[] ids = itemPK.getIds();
            String operationType = UpdateReportPOJO.OPERATION_TYPE_PHYSICAL_DELETE;
            // Call beforeDelete process (if any).
            Util.BeforeDeleteResult result = Util.beforeDeleting(clusterName, conceptName, ids, operationType);
            if (result != null && "error".equals(result.type)) { //$NON-NLS-1$
                throw new RemoteException(result.message);
            }
            // Generate physical delete event in journal
            WSDroppedItemPK droppedItemPK = wsRemoveDroppedItem.getWsDroppedItemPK();
            pushToUpdateReport(clusterName, dataModelName, conceptName, ids, true,"genericUI", operationType, null); //$NON-NLS-1$ 
            // Removes item from recycle bin
            DroppedItemCtrlLocal droppedItemCtrlLocal = Util.getDroppedItemCtrlLocal();
            DroppedItemPOJOPK droppedItemPOJOPK = droppedItemCtrlLocal.removeDroppedItem(XConverter.WS2POJO(droppedItemPK));
            return XConverter.POJO2WS(droppedItemPOJOPK);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSRoutingRule getRoutingRule(WSGetRoutingRule wsRoutingRuleGet) throws RemoteException {
        try {
            RoutingRuleCtrlLocal routingRuleCtrlLocal = Util.getRoutingRuleCtrlLocal();
            RoutingRulePOJOPK pk = new RoutingRulePOJOPK(wsRoutingRuleGet.getWsRoutingRulePK().getPk());
            if (routingRuleCtrlLocal.existsRoutingRule(pk) == null) {
                return null;
            }
            return XConverter.VO2WS(routingRuleCtrlLocal.getRoutingRule(pk));
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSBoolean existsRoutingRule(WSExistsRoutingRule wsExistsRoutingRule) throws RemoteException {
        try {
            RoutingRuleCtrlLocal routingRuleCtrlLocal = Util.getRoutingRuleCtrlLocal();
            RoutingRulePOJOPK pk = new RoutingRulePOJOPK(wsExistsRoutingRule.getWsRoutingRulePK().getPk());
            return new WSBoolean(routingRuleCtrlLocal.existsRoutingRule(pk) != null);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSRoutingRulePK deleteRoutingRule(WSDeleteRoutingRule wsDeleteRoutingRule) throws RemoteException {
        try {
            RoutingRuleCtrlLocal routingRuleCtrlLocal = Util.getRoutingRuleCtrlLocal();
            RoutingRulePOJOPK pk = new RoutingRulePOJOPK(wsDeleteRoutingRule.getWsRoutingRulePK().getPk());
            return new WSRoutingRulePK(routingRuleCtrlLocal.removeRoutingRule(pk).getUniqueId());
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSRoutingRulePK putRoutingRule(WSPutRoutingRule wsRoutingRule) throws RemoteException {
        try {
            RoutingRuleCtrlLocal routingRuleCtrlLocal = Util.getRoutingRuleCtrlLocal();
            RoutingRulePOJO routingRule = XConverter.WS2VO(wsRoutingRule.getWsRoutingRule());
            return new WSRoutingRulePK(routingRuleCtrlLocal.putRoutingRule(routingRule).getUniqueId());
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSRoutingRulePKArray getRoutingRulePKs(WSGetRoutingRulePKs regex) throws RemoteException {
        try {
            RoutingRuleCtrlLocal ctrl = Util.getRoutingRuleCtrlLocal();
            Collection collection = ctrl.getRoutingRulePKs(regex.getRegex());
            if (collection == null) {
                return null;
            }
            WSRoutingRulePK[] pks = new WSRoutingRulePK[collection.size()];
            int i = 0;
            for (Object o : collection) {
                pks[i++] = new WSRoutingRulePK(((RoutingRulePOJOPK) o).getUniqueId());
            }
            return new WSRoutingRulePKArray(pks);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSTransformerV2PK deleteTransformerV2(WSDeleteTransformerV2 wsTransformerV2Delete) throws RemoteException {
        try {
            TransformerV2CtrlLocal ctrl = Util.getTransformerV2CtrlLocal();
            return new WSTransformerV2PK(ctrl.removeTransformer(
                    new TransformerV2POJOPK(wsTransformerV2Delete.getWsTransformerV2PK().getPk())).getUniqueId());
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSTransformerV2 getTransformerV2(WSGetTransformerV2 wsGetTransformerV2) throws RemoteException {
        try {
            TransformerV2CtrlLocal ctrl = Util.getTransformerV2CtrlLocal();
            String pk = wsGetTransformerV2.getWsTransformerV2PK().getPk();
            TransformerV2POJO pojo = ctrl.getTransformer(new TransformerV2POJOPK(pk));
            return XConverter.POJO2WS(pojo);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSBoolean existsTransformerV2(WSExistsTransformerV2 wsExistsTransformerV2) throws RemoteException {
        try {
            TransformerV2CtrlLocal ctrl = Util.getTransformerV2CtrlLocal();
            String pk = wsExistsTransformerV2.getWsTransformerV2PK().getPk();
            TransformerV2POJO pojo = ctrl.existsTransformer(new TransformerV2POJOPK(pk));
            return new WSBoolean(pojo != null);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSTransformerV2PKArray getTransformerV2PKs(WSGetTransformerV2PKs regex) throws RemoteException {
        try {
            TransformerV2CtrlLocal ctrl = Util.getTransformerV2CtrlLocal();
            Collection collection = ctrl.getTransformerPKs(regex.getRegex());
            if (collection == null) {
                return null;
            }
            WSTransformerV2PK[] pks = new WSTransformerV2PK[collection.size()];
            int i = 0;
            for (Object o : collection) {
                pks[i++] = new WSTransformerV2PK(((TransformerV2POJOPK) o).getUniqueId());
            }
            return new WSTransformerV2PKArray(pks);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSTransformerV2PK putTransformerV2(WSPutTransformerV2 wsTransformerV2) throws RemoteException {
        try {
            TransformerV2CtrlLocal ctrl = Util.getTransformerV2CtrlLocal();
            TransformerV2POJOPK pk = ctrl.putTransformer(XConverter.WS2POJO(wsTransformerV2.getWsTransformerV2()));
            return new WSTransformerV2PK(pk.getUniqueId());
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSTransformerContext executeTransformerV2(WSExecuteTransformerV2 wsExecuteTransformerV2) throws RemoteException {
        try {
            final String RUNNING = "XtentisWSBean.executeTransformerV2.running";
            TransformerContext context = XConverter.WS2POJO(wsExecuteTransformerV2.getWsTransformerContext());
            context.put(RUNNING, Boolean.TRUE);
            TransformerV2CtrlLocal ctrl = Util.getTransformerV2CtrlLocal();
            ctrl.execute(context, XConverter.WS2POJO(wsExecuteTransformerV2.getWsTypedContent()), new TransformerCallBack() {

                @Override
                public void contentIsReady(TransformerContext context) throws XtentisException {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("XtentisWSBean.executeTransformerV2.contentIsReady() ");
                    }
                }

                @Override
                public void done(TransformerContext context) throws XtentisException {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("XtentisWSBean.executeTransformerV2.done() ");
                    }
                    context.put(RUNNING, Boolean.FALSE);
                }
            });
            while ((Boolean) context.get(RUNNING)) {
                Thread.sleep(100);
            }
            return XConverter.POJO2WS(context);
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSBackgroundJobPK executeTransformerV2AsJob(WSExecuteTransformerV2AsJob wsExecuteTransformerV2AsJob)
            throws RemoteException {
        try {
            TransformerV2CtrlLocal ctrl = Util.getTransformerV2CtrlLocal();
            BackgroundJobPOJOPK bgPK = ctrl.executeAsJob(XConverter.WS2POJO(wsExecuteTransformerV2AsJob.getWsTransformerContext()),
                    new TransformerCallBack() {

                        @Override
                        public void contentIsReady(TransformerContext context) throws XtentisException {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("XtentisWSBean.executeTransformerV2AsJob.contentIsReady() ");
                            }
                        }

                        @Override
                        public void done(TransformerContext context) throws XtentisException {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("XtentisWSBean.executeTransformerV2AsJob.done() ");
                            }
                        }
                    });
            return new WSBackgroundJobPK(bgPK.getUniqueId());
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSTransformerContext extractThroughTransformerV2(WSExtractThroughTransformerV2 wsExtractThroughTransformerV2)
            throws RemoteException {
        try {
            TransformerV2CtrlLocal ctrl = Util.getTransformerV2CtrlLocal();
            return XConverter.POJO2WS(ctrl.extractThroughTransformer(new TransformerV2POJOPK(wsExtractThroughTransformerV2
                    .getWsTransformerV2PK().getPk()), XConverter.WS2POJO(wsExtractThroughTransformerV2.getWsItemPK())));
        } catch (Exception e) {
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSBoolean existsTransformerPluginV2(WSExistsTransformerPluginV2 wsExistsTransformerPlugin) throws RemoteException {
        try {
            return new WSBoolean(Util.existsComponent(null, wsExistsTransformerPlugin.getJndiName()));
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSString getTransformerPluginV2Configuration(WSTransformerPluginV2GetConfiguration wsGetConfiguration)
            throws RemoteException {
        try {
            Object service = Util.retrieveComponent(null, wsGetConfiguration.getJndiName());
            String configuration = (String) Util.getMethod(service, "getConfiguration").invoke(service, //$NON-NLS-1$
                    wsGetConfiguration.getOptionalParameter());
            return new WSString(configuration);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSString putTransformerPluginV2Configuration(WSTransformerPluginV2PutConfiguration wsPutConfiguration)
            throws RemoteException {
        try {
            Object service = Util.retrieveComponent(null, wsPutConfiguration.getJndiName());
            Util.getMethod(service, "putConfiguration").invoke(service, wsPutConfiguration.getConfiguration());//$NON-NLS-1$
            return new WSString(wsPutConfiguration.getConfiguration());
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSTransformerPluginV2Details getTransformerPluginV2Details(
            WSGetTransformerPluginV2Details wsGetTransformerPluginDetails) throws RemoteException {
        try {
            Object service = Util.retrieveComponent(null, wsGetTransformerPluginDetails.getJndiName());
            String description = (String) Util.getMethod(service, "getDescription").invoke(//$NON-NLS-1$
                    service, wsGetTransformerPluginDetails.getLanguage() == null ? "" : wsGetTransformerPluginDetails//$NON-NLS-1$
                            .getLanguage());
            String documentation = (String) Util.getMethod(service, "getDocumentation").invoke(//$NON-NLS-1$
                    service,
                    wsGetTransformerPluginDetails.getLanguage() == null ? "" : wsGetTransformerPluginDetails.getLanguage());
            String parametersSchema = (String) Util.getMethod(service, "getParametersSchema").invoke(service);//$NON-NLS-1$

            ArrayList<TransformerPluginVariableDescriptor> inputVariableDescriptors = (ArrayList<TransformerPluginVariableDescriptor>) Util
                    .getMethod(service, "getInputVariableDescriptors").invoke(//$NON-NLS-1$
                            service, wsGetTransformerPluginDetails.getLanguage() == null ? ""//$NON-NLS-1$
                                    : wsGetTransformerPluginDetails.getLanguage());
            ArrayList<WSTransformerPluginV2VariableDescriptor> wsInputVariableDescriptors = new ArrayList<WSTransformerPluginV2VariableDescriptor>();
            if (inputVariableDescriptors != null) {
                for (TransformerPluginVariableDescriptor descriptor : inputVariableDescriptors) {
                    wsInputVariableDescriptors.add(XConverter.POJO2WS(descriptor));
                }
            }
            ArrayList<TransformerPluginVariableDescriptor> outputVariableDescriptors = (ArrayList<TransformerPluginVariableDescriptor>) Util
                    .getMethod(service, "getOutputVariableDescriptors").invoke(//$NON-NLS-1$
                            service, wsGetTransformerPluginDetails.getLanguage() == null ? ""//$NON-NLS-1$
                                    : wsGetTransformerPluginDetails.getLanguage());
            ArrayList<WSTransformerPluginV2VariableDescriptor> wsOutputVariableDescriptors = new ArrayList<WSTransformerPluginV2VariableDescriptor>();
            if (outputVariableDescriptors != null) {
                for (TransformerPluginVariableDescriptor descriptor : outputVariableDescriptors) {
                    wsOutputVariableDescriptors.add(XConverter.POJO2WS(descriptor));
                }
            }
            return new WSTransformerPluginV2Details(
                    wsInputVariableDescriptors.toArray(new WSTransformerPluginV2VariableDescriptor[wsInputVariableDescriptors
                            .size()]),
                    wsOutputVariableDescriptors.toArray(new WSTransformerPluginV2VariableDescriptor[wsOutputVariableDescriptors
                            .size()]), description, documentation, parametersSchema);
        } catch (XtentisException e) {
            throw (new RemoteException(e.getLocalizedMessage(), e));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSTransformerPluginV2SList getTransformerPluginV2SList(WSGetTransformerPluginV2SList wsGetTransformerPluginsList)
            throws RemoteException {
        try {
            ArrayList<WSTransformerPluginV2SListItem> wsList = new ArrayList<WSTransformerPluginV2SListItem>();
            InitialContext ctx = new InitialContext();
            NamingEnumeration<NameClassPair> list = ctx.list("amalto/local/transformer/plugin");//$NON-NLS-1$
            while (list.hasMore()) {
                NameClassPair nc = list.next();
                WSTransformerPluginV2SListItem item = new WSTransformerPluginV2SListItem();
                item.setJndiName(nc.getName());
                Object service = Util.retrieveComponent(null, "amalto/local/transformer/plugin/" + nc.getName());//$NON-NLS-1$
                String description = (String) Util.getMethod(service, "getDescription").invoke(//$NON-NLS-1$
                        service, wsGetTransformerPluginsList.getLanguage() == null ? "" : wsGetTransformerPluginsList//$NON-NLS-1$
                                .getLanguage());
                item.setDescription(description);
                wsList.add(item);
            }
            return new WSTransformerPluginV2SList(wsList.toArray(new WSTransformerPluginV2SListItem[wsList.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSRoutingOrderV2 getRoutingOrderV2(WSGetRoutingOrderV2 wsGetRoutingOrder) throws RemoteException {
        try {
            RoutingOrderV2CtrlLocal ctrl = Util.getRoutingOrderV2CtrlLocal();
            return XConverter.POJO2WS(ctrl.getRoutingOrder(XConverter.WS2POJO(wsGetRoutingOrder.getWsRoutingOrderPK())));
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSRoutingOrderV2 existsRoutingOrderV2(WSExistsRoutingOrderV2 wsExistsRoutingOrder) throws RemoteException {
        try {
            RoutingOrderV2CtrlLocal ctrl = Util.getRoutingOrderV2CtrlLocal();
            return XConverter.POJO2WS(ctrl.existsRoutingOrder(XConverter.WS2POJO(wsExistsRoutingOrder.getWsRoutingOrderPK())));
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSRoutingOrderV2PK deleteRoutingOrderV2(WSDeleteRoutingOrderV2 wsDeleteRoutingOrder) throws RemoteException {
        try {
            RoutingOrderV2CtrlLocal ctrl = Util.getRoutingOrderV2CtrlLocal();
            return XConverter.POJO2WS(ctrl.removeRoutingOrder(XConverter.WS2POJO(wsDeleteRoutingOrder.getWsRoutingOrderPK())));
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSRoutingOrderV2PK executeRoutingOrderV2Asynchronously(
            WSExecuteRoutingOrderV2Asynchronously wsExecuteRoutingOrderAsynchronously) throws RemoteException {
        try {
            RoutingOrderV2CtrlLocal ctrl = Util.getRoutingOrderV2CtrlLocal();
            AbstractRoutingOrderV2POJO ro = ctrl.getRoutingOrder(XConverter.WS2POJO(wsExecuteRoutingOrderAsynchronously
                    .getRoutingOrderV2PK()));
            ctrl.executeAsynchronously(ro);
            return XConverter.POJO2WS(ro.getAbstractRoutingOrderPOJOPK());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSString executeRoutingOrderV2Synchronously(WSExecuteRoutingOrderV2Synchronously wsExecuteRoutingOrderSynchronously)
            throws RemoteException {
        try {
            RoutingOrderV2CtrlLocal ctrl = Util.getRoutingOrderV2CtrlLocal();
            AbstractRoutingOrderV2POJO ro = ctrl
                    .getRoutingOrder(XConverter.WS2POJO(wsExecuteRoutingOrderSynchronously.getRoutingOrderV2PK()));
            return new WSString(ctrl.executeSynchronously(ro));
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    protected Collection<AbstractRoutingOrderV2POJOPK> getRoutingOrdersByCriteria(WSRoutingOrderV2SearchCriteria criteria)
            throws Exception {
        try {
            RoutingOrderV2CtrlLocal ctrl = Util.getRoutingOrderV2CtrlLocal();
            Class<? extends AbstractRoutingOrderV2POJO> clazz = null;
            if (criteria.getStatus().equals(WSRoutingOrderV2Status.ACTIVE)) {
                clazz = ActiveRoutingOrderV2POJO.class;
            } else if (criteria.getStatus().equals(WSRoutingOrderV2Status.COMPLETED)) {
                clazz = CompletedRoutingOrderV2POJO.class;
            } else if (criteria.getStatus().equals(WSRoutingOrderV2Status.FAILED)) {
                clazz = FailedRoutingOrderV2POJO.class;
            }
            return ctrl.getRoutingOrderPKsByCriteria(clazz, criteria.getAnyFieldContains(), criteria.getNameContains(),
                    criteria.getTimeCreatedMin(), criteria.getTimeCreatedMax(), criteria.getTimeScheduledMin(),
                    criteria.getTimeScheduledMax(), criteria.getTimeLastRunStartedMin(), criteria.getTimeLastRunStartedMax(),
                    criteria.getTimeLastRunCompletedMin(), criteria.getTimeLastRunCompletedMax(),
                    criteria.getItemPKConceptContains(), criteria.getItemPKIDFieldsContain(), criteria.getServiceJNDIContains(),
                    criteria.getServiceParametersContain(), criteria.getMessageContain());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    protected Collection<AbstractRoutingOrderV2POJOPK> getRoutingOrdersByCriteriaWithPaging(
            WSRoutingOrderV2SearchCriteriaWithPaging criteria) throws Exception {
        try {
            RoutingOrderV2CtrlLocal ctrl = Util.getRoutingOrderV2CtrlLocal();
            Class<? extends AbstractRoutingOrderV2POJO> clazz = null;
            if (criteria.getStatus().equals(WSRoutingOrderV2Status.ACTIVE)) {
                clazz = ActiveRoutingOrderV2POJO.class;
            } else if (criteria.getStatus().equals(WSRoutingOrderV2Status.COMPLETED)) {
                clazz = CompletedRoutingOrderV2POJO.class;
            } else if (criteria.getStatus().equals(WSRoutingOrderV2Status.FAILED)) {
                clazz = FailedRoutingOrderV2POJO.class;
            }

            return ctrl.getRoutingOrderPKsByCriteriaWithPaging(clazz, criteria.getAnyFieldContains(), criteria.getNameContains(),
                    criteria.getTimeCreatedMin(), criteria.getTimeCreatedMax(), criteria.getTimeScheduledMin(),
                    criteria.getTimeScheduledMax(), criteria.getTimeLastRunStartedMin(), criteria.getTimeLastRunStartedMax(),
                    criteria.getTimeLastRunCompletedMin(), criteria.getTimeLastRunCompletedMax(),
                    criteria.getItemPKConceptContains(), criteria.getItemPKIDFieldsContain(), criteria.getServiceJNDIContains(),
                    criteria.getServiceParametersContain(), criteria.getMessageContain(), criteria.getSkip(),
                    criteria.getMaxItems(), criteria.getTotalCountOnFirstResult());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSRoutingOrderV2PKArray getRoutingOrderV2PKsByCriteria(
            WSGetRoutingOrderV2PKsByCriteria wsGetRoutingOrderV2PKsByCriteria) throws RemoteException {
        try {
            WSRoutingOrderV2PKArray wsPKArray = new WSRoutingOrderV2PKArray();
            ArrayList<WSRoutingOrderV2PK> list = new ArrayList<WSRoutingOrderV2PK>();
            // fetch results
            Collection<AbstractRoutingOrderV2POJOPK> pks = getRoutingOrdersByCriteria(wsGetRoutingOrderV2PKsByCriteria
                    .getWsSearchCriteria());
            for (AbstractRoutingOrderV2POJOPK pk : pks) {
                list.add(XConverter.POJO2WS(pk));
            }
            wsPKArray.setWsRoutingOrder(list.toArray(new WSRoutingOrderV2PK[list.size()]));
            return wsPKArray;
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSRoutingOrderV2Array getRoutingOrderV2SByCriteria(WSGetRoutingOrderV2SByCriteria wsGetRoutingOrderV2SByCriteria)
            throws RemoteException {
        try {
            RoutingOrderV2CtrlLocal ctrl = Util.getRoutingOrderV2CtrlLocal();
            WSRoutingOrderV2Array wsPKArray = new WSRoutingOrderV2Array();
            ArrayList<WSRoutingOrderV2> list = new ArrayList<WSRoutingOrderV2>();
            // fetch results
            Collection<AbstractRoutingOrderV2POJOPK> pks = getRoutingOrdersByCriteria(wsGetRoutingOrderV2SByCriteria
                    .getWsSearchCriteria());
            for (AbstractRoutingOrderV2POJOPK pk : pks) {
                list.add(XConverter.POJO2WS(ctrl.getRoutingOrder(pk)));
            }
            wsPKArray.setWsRoutingOrder(list.toArray(new WSRoutingOrderV2[list.size()]));
            return wsPKArray;
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSRoutingOrderV2Array getRoutingOrderV2ByCriteriaWithPaging(
            WSGetRoutingOrderV2ByCriteriaWithPaging wsGetRoutingOrderV2ByCriteriaWithPaging) throws RemoteException {
        try {
            RoutingOrderV2CtrlLocal ctrl = Util.getRoutingOrderV2CtrlLocal();
            WSRoutingOrderV2Array wsPKArray = new WSRoutingOrderV2Array();
            ArrayList<WSRoutingOrderV2> list = new ArrayList<WSRoutingOrderV2>();
            // fetch results
            Collection<AbstractRoutingOrderV2POJOPK> pks = getRoutingOrdersByCriteriaWithPaging(wsGetRoutingOrderV2ByCriteriaWithPaging
                    .getWsSearchCriteriaWithPaging());
            boolean withTotalCount = wsGetRoutingOrderV2ByCriteriaWithPaging.getWsSearchCriteriaWithPaging()
                    .getTotalCountOnFirstResult();
            boolean firstRecord = true;
            for (AbstractRoutingOrderV2POJOPK abstractRoutingOrderV2POJOPK : pks) {
                if (withTotalCount && firstRecord) {
                    firstRecord = false;
                    WSRoutingOrderV2 wsRoutingOrderV2 = new WSRoutingOrderV2();
                    // record totalCount and wsRoutingOrderV2 need to initialize attribute value
                    wsRoutingOrderV2.setName(abstractRoutingOrderV2POJOPK.getName());
                    wsRoutingOrderV2.setBindingUniverseName("");
                    wsRoutingOrderV2.setBindingUserToken("");
                    wsRoutingOrderV2.setMessage("");
                    wsRoutingOrderV2.setServiceJNDI("");
                    wsRoutingOrderV2.setServiceParameters("");
                    wsRoutingOrderV2.setStatus(WSRoutingOrderV2Status.COMPLETED);
                    wsRoutingOrderV2.setWsItemPK(new WSItemPK(new WSDataClusterPK(""), "", new String[0]));
                    list.add(wsRoutingOrderV2);
                    continue;
                }
                list.add(XConverter.POJO2WS(ctrl.getRoutingOrder(abstractRoutingOrderV2POJOPK)));
            }
            wsPKArray.setWsRoutingOrder(list.toArray(new WSRoutingOrderV2[list.size()]));
            return wsPKArray;
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSRoutingRulePKArray routeItemV2(WSRouteItemV2 wsRouteItem) throws RemoteException {
        try {
            RoutingEngineV2CtrlLocal ctrl = Util.getRoutingEngineV2CtrlLocal();
            RoutingRulePOJOPK[] rules = ctrl.route(XConverter.WS2POJO(wsRouteItem.getWsItemPK()));
            ArrayList<WSRoutingRulePK> list = new ArrayList<WSRoutingRulePK>();
            for (RoutingRulePOJOPK rule : rules) {
                list.add(new WSRoutingRulePK(rule.getUniqueId()));
            }
            return new WSRoutingRulePKArray(list.toArray(new WSRoutingRulePK[list.size()]));
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }

    public WSRoutingEngineV2Status routingEngineV2Action(WSRoutingEngineV2Action wsRoutingEngineAction) throws RemoteException {
        try {
            RoutingEngineV2CtrlLocal ctrl = Util.getRoutingEngineV2CtrlLocal();
            if (wsRoutingEngineAction.getWsAction().equals(WSRoutingEngineV2ActionCode.START)) {
                ctrl.start();
            } else if (wsRoutingEngineAction.getWsAction().equals(WSRoutingEngineV2ActionCode.STOP)) {
                ctrl.stop();
            } else if (wsRoutingEngineAction.getWsAction().equals(WSRoutingEngineV2ActionCode.SUSPEND)) {
                ctrl.suspend(true);
            } else if (wsRoutingEngineAction.getWsAction().equals(WSRoutingEngineV2ActionCode.RESUME)) {
                ctrl.suspend(false);
            } else if (wsRoutingEngineAction.getWsAction().equals(WSRoutingEngineV2ActionCode.STATUS)) {
                // done below;
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
        // get status
        try {
            RoutingEngineV2CtrlLocal ctrl = Util.getRoutingEngineV2CtrlLocal();
            int status = ctrl.getStatus();
            switch (status) {
            case RoutingEngineV2POJO.RUNNING:
                return WSRoutingEngineV2Status.RUNNING;
            case RoutingEngineV2POJO.STOPPED:
                return WSRoutingEngineV2Status.STOPPED;
            case RoutingEngineV2POJO.SUSPENDED:
                return WSRoutingEngineV2Status.SUSPENDED;
            default:
                return WSRoutingEngineV2Status.DEAD;
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                String err = "ERROR SYSTRACE: " + e.getMessage();
                LOGGER.debug(err, e);
            }
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }

    }

    public WSAutoIncrement getAutoIncrement(WSAutoIncrement request) throws RemoteException {
        try {
            XmlServerSLWrapperLocal xmlServerCtrlLocal = Util.getXmlServerCtrlLocal();
            if (request == null) {
                String xml = xmlServerCtrlLocal.getDocumentAsString(null, XSystemObjects.DC_CONF.getName(), "Auto_Increment");//$NON-NLS-1$
                if (xml != null) {
                    return new WSAutoIncrement(xml);
                }
            } else {
                xmlServerCtrlLocal.start(XSystemObjects.DC_CONF.getName());
                xmlServerCtrlLocal.putDocumentFromString(request.getAutoincrement(), "Auto_Increment",//$NON-NLS-1$
                        XSystemObjects.DC_CONF.getName(), null);
                xmlServerCtrlLocal.commit(XSystemObjects.DC_CONF.getName());
                return request;
            }
        } catch (XtentisException e) {
            LOGGER.error("IXtentisWSDelegator.getAutoIncrement error.", e);
        }
        return null;
    }

    public WSCategoryData getMDMCategory(WSCategoryData request) throws RemoteException {
        try {
            XmlServerSLWrapperLocal xmlServerCtrlLocal = Util.getXmlServerCtrlLocal();
            if (request == null) {
                // create and retrieve an empty treeObject Category from xdb in the case of request being null
                String category = xmlServerCtrlLocal.getDocumentAsString(null, "CONF", "CONF.TREEOBJECT.CATEGORY");//$NON-NLS-1$ //$NON-NLS-2$
                if (category == null) {
                    String empty = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";//$NON-NLS-1$
                    empty += "<" + ICoreConstants.DEFAULT_CATEGORY_ROOT + "/>";//$NON-NLS-1$ //$NON-NLS-2$
                    xmlServerCtrlLocal.start("CONF");
                    xmlServerCtrlLocal.putDocumentFromString(empty, "CONF.TREEOBJECT.CATEGORY", "CONF", "");//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    xmlServerCtrlLocal.commit("CONF");
                    category = empty;
                }
                return new WSCategoryData(category);
            } else {
                xmlServerCtrlLocal.start("CONF"); //$NON-NLS-1$
                xmlServerCtrlLocal.putDocumentFromString(request.getCategorySchema(), "CONF.TREEOBJECT.CATEGORY",//$NON-NLS-1$
                        "CONF", null);//$NON-NLS-1$
                xmlServerCtrlLocal.commit("CONF"); //$NON-NLS-1$
                return request;
            }
        } catch (XtentisException e) {
            LOGGER.error("IXtentisWSDelegator.getMDMCategory error.", e);
            return null;
        }
    }

    public WSRole getRole(WSGetRole wsGetRole) throws RemoteException {
        try {
            RoleCtrlLocal ctrl = Util.getRoleCtrlLocal();
            RolePOJO pojo = ctrl.getRole(new RolePOJOPK(wsGetRole.getWsRolePK().getPk()));
            return XConverter.POJO2WS(pojo);
        } catch (Exception e) {
            String err = "ERROR SYSTRACE: " + e.getMessage();
            LOGGER.debug(err, e);
            throw new RemoteException(e.getClass().getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    public WSBoolean putMDMJob(WSPUTMDMJob job) throws RemoteException {
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc;
            Element jobElem, newOne;
            String xmlData = null;
            XmlServerSLWrapperLocal xmlServerCtrlLocal = Util.getXmlServerCtrlLocal();
            try {
                xmlData = xmlServerCtrlLocal.getDocumentAsString(null, MDM_TIS_JOB, JOB);
            } catch (Exception e) {
                LOGGER.error("IXtentisWSDelegator.putMDMJob error.", e);
            }
            if (xmlData == null || xmlData.equals("")) {//$NON-NLS-1$
                doc = documentBuilder.newDocument();
                jobElem = doc.createElement("jobs");//$NON-NLS-1$
                doc.appendChild(jobElem);
            } else {
                doc = Util.parse(xmlData);
                jobElem = doc.getDocumentElement();
            }

            newOne = doc.createElement("job");//$NON-NLS-1$
            newOne.setAttribute("name", job.getJobName());//$NON-NLS-1$
            newOne.setAttribute("version", job.getJobVersion());//$NON-NLS-1$
            jobElem.appendChild(newOne);

            xmlServerCtrlLocal.start(MDM_TIS_JOB);
            xmlServerCtrlLocal.putDocumentFromString(Util.nodeToString(doc.getDocumentElement()), JOB, MDM_TIS_JOB, null);
            xmlServerCtrlLocal.commit(MDM_TIS_JOB);
            return new WSBoolean(true);
        } catch (Exception e) {
            LOGGER.error("IXtentisWSDelegator.putMDMJob error.", e);
        }
        return new WSBoolean(false);
    }

    public WSBoolean deleteMDMJob(WSDELMDMJob job) throws RemoteException {
        Document doc;
        try {
            String xmlData = null;
            XmlServerSLWrapperLocal xmlServerCtrlLocal = Util.getXmlServerCtrlLocal();
            try {
                xmlData = xmlServerCtrlLocal.getDocumentAsString(null, MDM_TIS_JOB, JOB);
            } catch (Exception e) {
                LOGGER.error("IXtentisWSDelegator.deleteMDMJob error.", e);
            }
            if (xmlData == null) {
                return new WSBoolean(false);
            }
            doc = Util.parse(xmlData);
            NodeList list = Util.getNodeList(doc, "/jobs/job[@name='" + job.getJobName() + "']");//$NON-NLS-1$ //$NON-NLS-2$
            if (list.getLength() > 0) {
                doc.getDocumentElement().removeChild(list.item(0));
                xmlData = Util.nodeToString(doc);
                xmlServerCtrlLocal.start(MDM_TIS_JOB);
                xmlServerCtrlLocal.putDocumentFromString(xmlData, JOB, MDM_TIS_JOB, null);
                xmlServerCtrlLocal.commit(MDM_TIS_JOB);
                return new WSBoolean(true);
            }
        } catch (Exception e) {
            LOGGER.error("IXtentisWSDelegator.deleteMDMJob error.", e);
        }
        return new WSBoolean(false);
    }

    /**
     * get job info from jboss deploy dir
     */
    public WSMDMJobArray getMDMJob(WSMDMNULL job) {
        WSMDMJobArray jobSet = new WSMDMJobArray();
        WSMDMJob[] jobs = Util.getMDMJobs();
        jobSet.setWsMDMJob(jobs);
        return jobSet;
    }

    public WSBoolean isItemModifiedByOther(WSItem item) throws RemoteException {
        try {
            boolean ret = Util.getItemCtrl2Local()
                    .isItemModifiedByOther(
                            new ItemPOJOPK(new DataClusterPOJOPK(item.getWsDataClusterPK().getPk()), item.getConceptName(),
                                    item.getIds()), item.getInsertionTime());
            return new WSBoolean(ret);
        } catch (Exception e) {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }
    }

    public WSString countItemsByCustomFKFilters(WSCountItemsByCustomFKFilters wsCountItemsByCustomFKFilters)
            throws RemoteException {
        try {
            long count = Util.getItemCtrl2Local().countItemsByCustomFKFilters(
                    new DataClusterPOJOPK(wsCountItemsByCustomFKFilters.getWsDataClusterPK().getPk()),
                    wsCountItemsByCustomFKFilters.getConceptName(), wsCountItemsByCustomFKFilters.getInjectedXpath());
            return new WSString(String.valueOf(count));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSStringArray getItemsByCustomFKFilters(WSGetItemsByCustomFKFilters wsGetItemsByCustomFKFilters)
            throws RemoteException {
        try {
            Map context = Collections.emptyMap();
            ArrayList res = Util.getItemCtrl2Local().getItemsByCustomFKFilters(
                    new DataClusterPOJOPK(wsGetItemsByCustomFKFilters.getWsDataClusterPK().getPk()),
                    new ArrayList<String>(Arrays.asList(wsGetItemsByCustomFKFilters.getViewablePaths().getStrings())),
                    wsGetItemsByCustomFKFilters.getInjectedXpath(),
                    XConverter.WS2VO(wsGetItemsByCustomFKFilters.getWhereItem(), new WhereConditionForcePivotFilter(context)),
                    wsGetItemsByCustomFKFilters.getSkip(), wsGetItemsByCustomFKFilters.getMaxItems(),
                    wsGetItemsByCustomFKFilters.getOrderBy(), wsGetItemsByCustomFKFilters.getDirection(),
                    wsGetItemsByCustomFKFilters.getReturnCount());
            return new WSStringArray((String[]) res.toArray(new String[res.size()]));
        } catch (Exception e) {
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }

    public WSString refreshCache(WSRefreshCache refreshCache) {
        ItemPOJO.clearCache();
        ObjectPOJO.clearCache();
        return new WSString("Refresh the item and object cache successfully!");
    }

    public WSBoolean isXmlDB() {
        StorageAdmin storageAdmin = ServerContext.INSTANCE.get().getStorageAdmin();
        // Retrieves SYSTEM storage
        Storage systemStorage = storageAdmin.get(StorageAdmin.SYSTEM_STORAGE, StorageType.SYSTEM, null);
        return new WSBoolean(systemStorage == null);
    }

    public WSDigest getDigest(WSDigestKey wsDigestKey) {
        StorageAdmin storageAdmin = ServerContext.INSTANCE.get().getStorageAdmin();
        // Retrieves SYSTEM storage
        Storage systemStorage = storageAdmin.get(StorageAdmin.SYSTEM_STORAGE, StorageType.SYSTEM, null);
        if (systemStorage != null) {
            // This repository holds all system object types
            MetadataRepository repository = systemStorage.getMetadataRepository();
            String type = wsDigestKey.getType();
            String name = wsDigestKey.getObjectName();
            systemStorage.begin(); // Storage needs an active transaction (even for read operations).
            try {
                String typeName = DigestHelper.getInstance().getTypeName(type);
                if (typeName != null) {
                    // Get the type definition for query
                    ComplexTypeMetadata storageType = repository.getComplexType(ClassRepository.format(typeName));
                    // Select instance of type where unique-id equals provided name
                    UserQueryBuilder qb = UserQueryBuilder.from(storageType).where(
                            UserQueryBuilder.eq(storageType.getField("unique-id"), name)); //$NON-NLS-1$
                    StorageResults results = systemStorage.fetch(qb.getSelect());

                    Iterator<DataRecord> iterator = results.iterator();
                    if (iterator.hasNext()) {
                        DataRecord result = iterator.next();
                        return new WSDigest(wsDigestKey,
                                (String) result.get("digest"), result.getRecordMetadata().getLastModificationTime()); //$NON-NLS-1$
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } finally {
                systemStorage.commit();
            }
        } else {
            return null;
        }
    }

    public WSLong updateDigest(WSDigest wsDigest) {
        StorageAdmin storageAdmin = ServerContext.INSTANCE.get().getStorageAdmin();
        // Retrieves SYSTEM storage
        Storage systemStorage = storageAdmin.get(StorageAdmin.SYSTEM_STORAGE, StorageType.SYSTEM, null);
        if (systemStorage != null) {
            // This repository holds all system object types
            MetadataRepository repository = systemStorage.getMetadataRepository();
            String type = wsDigest.getWsDigestKey().getType();
            String name = wsDigest.getWsDigestKey().getObjectName();
            systemStorage.begin(); // Storage needs an active transaction (even for read operations).
            try {
                String typeName = DigestHelper.getInstance().getTypeName(type);
                if (typeName != null) {
                    ComplexTypeMetadata storageType = repository.getComplexType(ClassRepository.format(typeName));
                    UserQueryBuilder qb = UserQueryBuilder.from(storageType)
                            .where(UserQueryBuilder.eq(storageType.getField("unique-id"), name)) //$NON-NLS-1$
                            .forUpdate(); // <- Important line here!
                    StorageResults results = systemStorage.fetch(qb.getSelect());
                    Iterator<DataRecord> iterator = results.iterator();
                    if (iterator.hasNext()) {
                        DataRecord result = iterator.next();
                        FieldMetadata digestField = storageType.getField("digest"); //$NON-NLS-1$
                        // Using convert ensure type is  correct
                        result.set(digestField, StorageMetadataUtils.convert(wsDigest.getDigestValue(), digestField));
                        systemStorage.update(result); // No need to set timestamp (update will update it).
                        systemStorage.commit();
                        return new WSLong(result.getRecordMetadata().getLastModificationTime());
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (Exception e) {
                systemStorage.rollback();
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }

    }

    public WSMatchRulePK putMatchRule(WSPutMatchRule wsPutMatchRule) throws RemoteException {
        return null; // Supported only in EE.
    }

    public WSMatchRulePK deleteMatchRule(WSDeleteMatchRule wsDeleteMatchRule) throws RemoteException {
        return null; // Supported only in EE.
    }

    public WSBoolean isPagingAccurate(WSInt currentTotalSize) {
        List<String> noSupportAccurateDbs = Arrays.asList("qizx");//$NON-NLS-1$
        Properties props = MDMConfiguration.getConfiguration();
        String dbName = props.getProperty("xmldb.type");//$NON-NLS-1$
        WSBoolean result = new WSBoolean(true);
        if (noSupportAccurateDbs.contains(dbName)) {
            String countSampleSize = props.getProperty("xmldb.qizx.ecountsamplesize"); //$NON-NLS-1$
            if (countSampleSize != null && countSampleSize.trim().length() > 0) {
                int size = Integer.parseInt(countSampleSize);
                if (currentTotalSize.getValue() > size) {
                    result.set_true(false);
                }
            }
        }
        return result;
    }

    public FKIntegrityCheckResult checkFKIntegrity(WSDeleteItem deleteItem) {
        try {
            WSItemPK wsItemPK = deleteItem.getWsItemPK();
            String dataClusterName = wsItemPK.getWsDataClusterPK().getPk();
            String conceptName = wsItemPK.getConceptName();
            String[] ids = wsItemPK.getIds();
            return Util.getItemCtrl2Local().checkFKIntegrity(dataClusterName, conceptName, ids);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> globalSearch(String dataCluster, String keyword, int start, int end) {
        try {
            return Util.getXmlServerCtrlLocal().globalSearch(dataCluster, keyword, start, end);
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public WSBoolean supportStaging(WSDataClusterPK dataClusterPK) {
        try {
            boolean supportStaging = Util.getXmlServerCtrlLocal().supportStaging(dataClusterPK.getPk());
            return new WSBoolean(supportStaging);
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public WSItemPK updateItemMetadata(WSUpdateMetadataItem wsUpdateMetadataItem) throws RemoteException {
        try {
            WSItemPK itemPK = wsUpdateMetadataItem.getWsItemPK();
            ItemPOJOPK itemPk = new ItemPOJOPK(new DataClusterPOJOPK(itemPK.getWsDataClusterPK().getPk()),
                    itemPK.getConceptName(), itemPK.getIds());
            ItemCtrl2Local itemCtrl2Local = Util.getItemCtrl2Local();
            ItemPOJO item = itemCtrl2Local.getItem(itemPk);
            item.setTaskId(wsUpdateMetadataItem.getTaskId());
            ItemPOJOPK itemPOJOPK = itemCtrl2Local.updateItemMetadata(item);
            return new WSItemPK(new WSDataClusterPK(itemPOJOPK.getDataClusterPOJOPK().getUniqueId()),
                    itemPOJOPK.getConceptName(), itemPOJOPK.getIds());
        } catch (XtentisException e) {
            String err = "ERROR SYSTRACE: " + e.getMessage();
            LOGGER.debug(err, e);
            throw new RemoteException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            String err = "ERROR SYSTRACE: " + e.getMessage();
            LOGGER.debug(err, e);
            throw new RemoteException((e.getCause() == null ? e.getLocalizedMessage() : e.getCause().getLocalizedMessage()), e);
        }
    }
}
