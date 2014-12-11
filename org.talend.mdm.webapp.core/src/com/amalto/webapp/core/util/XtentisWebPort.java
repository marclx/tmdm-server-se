package com.amalto.webapp.core.util;

import java.rmi.RemoteException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.ObjectNotFoundException;

import com.amalto.core.integrity.FKIntegrityCheckResult;
import com.amalto.core.jobox.util.JobNotFoundException;
import com.amalto.core.storage.exception.ConstraintViolationException;
import com.amalto.core.storage.exception.FullTextQueryCompositeKeyException;
import com.amalto.core.util.EntityNotFoundException;
import com.amalto.core.util.*;
import com.amalto.core.util.RoutingException;import com.amalto.core.webservice.*;

public class XtentisWebPort implements XtentisPort {

    // validate not null field is null
    public static final String VALIDATE_EXCEPTION_MESSAGE = "save_validationrule_fail"; //$NON-NLS-1$

    // don't exist job was called
    public static final String JOB_NOT_FOUND_EXCEPTION_MESSAGE = "save_failure_job_notfound"; //$NON-NLS-1$

    // don't exist job was called
    public static final String JOBOX_EXCEPTION_MESSAGE = "save_failure_job_ox"; //$NON-NLS-1$

    // call by core util.defaultValidate,but it will be wrap as ValidateException
    public static final String CVC_EXCEPTION_MESSAGE = "save_fail_cvc_exception"; //$NON-NLS-1$

    // default save exception message
    public static final String SAVE_EXCEPTION_MESSAGE = "save_fail"; //$NON-NLS-1$

    // define before saving report in error level
    public static final String SAVE_PROCESS_BEFORE_SAVING_FAILURE_MESSAGE = "save_failure_beforesaving_validate_error"; //$NON-NLS-1$ 

    // define before saving report xml format error in studio
    public static final String BEFORE_SAVING_FORMAT_ERROR_MESSAGE = "save_failure_beforesaving_format_error"; //$NON-NLS-1$ 

    // define trigger to execute process error in studio
    public static final String ROUTING_ERROR_MESSAGE = "save_success_rounting_fail"; //$NON-NLS-1$

    // missing output report (before saving)
    public static final String OUTPUT_REPORT_MISSING_ERROR_MESSAGE = "output_report_missing"; //$NON-NLS-1$

    private static final String INTEGRITY_CONSTRAINT_CHECK_FAILED_MESSAGE = "delete_failure_constraint_violation"; //$NON-NLS-1$

    // full text query entity include composite key
    public static final String FULLTEXT_QUERY_COMPOSITE_KEY_EXCEPTION_MESSAGE = "fulltext_query_compositekey_fail"; //$NON-NLS-1$

    // default remote error
    public static final String DEFAULT_REMOTE_ERROR_MESSAGE = "default_remote_error_message"; //$NON-NLS-1$

    private final XtentisPort delegate;

    protected XtentisWebPort(XtentisPort delegate) {
        this.delegate = delegate;
    }

    public static XtentisPort wrap(XtentisPort port) {
        return new XtentisWebPort(port);
    }

    /**
     * Handle the <code>throwable</code> parameter and returns a web ui exception (with potentially less technical information
     * but better suited for end-user display).
     *
     * @param throwable The throwable to process.
     * @param errorMessage A default error message if exception does not have a message.
     * @return A {@link java.rmi.RemoteException exception} suited for Web UI display.
     */
    public static RemoteException handleException(Throwable throwable, String errorMessage) {
        WebCoreException webCoreException;
        if (WebCoreException.class.isInstance(throwable)) {
            webCoreException = (WebCoreException) throwable;
        } else if (ValidateException.class.isInstance(throwable)) {
            webCoreException = new WebCoreException(VALIDATE_EXCEPTION_MESSAGE, throwable);
        } else if (SchematronValidateException.class.isInstance(throwable)) {
            webCoreException = new WebCoreException(VALIDATE_EXCEPTION_MESSAGE, throwable.getMessage(), WebCoreException.INFO);
        } else if (CVCException.class.isInstance(throwable)) {
            webCoreException = new WebCoreException(CVC_EXCEPTION_MESSAGE, throwable);
        } else if (JobNotFoundException.class.isInstance(throwable)) {
            webCoreException = new WebCoreException(JOB_NOT_FOUND_EXCEPTION_MESSAGE, throwable);
        } else if (com.amalto.core.jobox.util.JoboxException.class.isInstance(throwable)) {
            webCoreException = new WebCoreException(JOBOX_EXCEPTION_MESSAGE, throwable);
        } else if (BeforeSavingErrorException.class.isInstance(throwable)) {
            webCoreException = new WebCoreException(SAVE_PROCESS_BEFORE_SAVING_FAILURE_MESSAGE, throwable);
        } else if (BeforeSavingFormatException.class.isInstance(throwable)) {
            webCoreException = new WebCoreException(BEFORE_SAVING_FORMAT_ERROR_MESSAGE, throwable);
            webCoreException.setClient(true);
        } else if (OutputReportMissingException.class.isInstance(throwable)) {
            webCoreException = new WebCoreException(OUTPUT_REPORT_MISSING_ERROR_MESSAGE, throwable);
        } else if (RoutingException.class.isInstance(throwable)) {
            webCoreException = new WebCoreException(ROUTING_ERROR_MESSAGE, throwable);
        } else if (FullTextQueryCompositeKeyException.class.isInstance(throwable)) {
            webCoreException = new WebCoreException(FULLTEXT_QUERY_COMPOSITE_KEY_EXCEPTION_MESSAGE, throwable);
        } else {
            if (throwable.getCause() != null) {
                return handleException(throwable.getCause(), errorMessage);
            } else {
                webCoreException = new WebCoreException(errorMessage, throwable);
            }
        }
        return new RemoteException(StringUtils.EMPTY, webCoreException);
    }

    @Override
    public WSVersion getComponentVersion(WSGetComponentVersion wsGetComponentVersion) throws RemoteException {
        return delegate.getComponentVersion(wsGetComponentVersion);
    }

    @Override
    public WSString ping(WSPing wsPing) throws RemoteException {
        return delegate.ping(wsPing);
    }

    @Override
    public WSString refreshCache(WSRefreshCache refreshCache) throws RemoteException {
        return delegate.refreshCache(refreshCache);
    }

    @Override
    public WSString logout(WSLogout wsLogout) throws RemoteException {
        return delegate.logout(wsLogout);
    }

    @Override
    public WSInt initMDM(WSInitData initData) throws RemoteException {
        return delegate.initMDM(initData);
    }

    @Override
    public WSDataModelPKArray getDataModelPKs(WSRegexDataModelPKs regexp) throws RemoteException {
        return delegate.getDataModelPKs(regexp);
    }

    @Override
    public WSDataModel getDataModel(WSGetDataModel wsDataModelget) throws RemoteException {
        return delegate.getDataModel(wsDataModelget);
    }

    @Override
    public WSBoolean existsDataModel(WSExistsDataModel wsDataModelExists) throws RemoteException {
        return delegate.existsDataModel(wsDataModelExists);
    }

    @Override
    public WSDataModelPK putDataModel(WSPutDataModel wsDataModel) throws RemoteException {
        return delegate.putDataModel(wsDataModel);
    }

    @Override
    public WSDataModelPK deleteDataModel(WSDeleteDataModel wsDeleteDataModel) throws RemoteException {
        return delegate.deleteDataModel(wsDeleteDataModel);
    }

    @Override
    public WSString checkSchema(WSCheckSchema wsSchema) throws RemoteException {
        return delegate.checkSchema(wsSchema);
    }

    @Override
    public WSString deleteBusinessConcept(WSDeleteBusinessConcept wsDeleteBusinessConcept) throws RemoteException {
        return delegate.deleteBusinessConcept(wsDeleteBusinessConcept);
    }

    @Override
    public WSStringArray getBusinessConcepts(WSGetBusinessConcepts wsGetBusinessConcepts) throws RemoteException {
        return delegate.getBusinessConcepts(wsGetBusinessConcepts);
    }

    @Override
    public WSString putBusinessConcept(WSPutBusinessConcept wsPutBusinessConcept) throws RemoteException {
        return delegate.putBusinessConcept(wsPutBusinessConcept);
    }

    @Override
    public WSString putBusinessConceptSchema(WSPutBusinessConceptSchema wsPutBusinessConceptSchema) throws RemoteException {
        return delegate.putBusinessConceptSchema(wsPutBusinessConceptSchema);
    }

    @Override
    public WSConceptKey getBusinessConceptKey(WSGetBusinessConceptKey wsGetBusinessConceptKey) throws RemoteException {
        return delegate.getBusinessConceptKey(wsGetBusinessConceptKey);
    }

    @Override
    public WSDataClusterPKArray getDataClusterPKs(WSRegexDataClusterPKs regexp) throws RemoteException {
        return delegate.getDataClusterPKs(regexp);
    }

    @Override
    public WSDataCluster getDataCluster(WSGetDataCluster wsDataClusterPK) throws RemoteException {
        return delegate.getDataCluster(wsDataClusterPK);
    }

    @Override
    public WSBoolean existsDataCluster(WSExistsDataCluster wsExistsDataCluster) throws RemoteException {
        return delegate.existsDataCluster(wsExistsDataCluster);
    }

    @Override
    public WSBoolean existsDBDataCluster(WSExistsDBDataCluster wsExistsDBDataCluster) throws RemoteException {
        return delegate.existsDBDataCluster(wsExistsDBDataCluster);
    }

    @Override
    public WSDataClusterPK putDataCluster(WSPutDataCluster wsDataCluster) throws RemoteException {
        return delegate.putDataCluster(wsDataCluster);
    }

    @Override
    public WSBoolean putDBDataCluster(WSPutDBDataCluster wsDataCluster) throws RemoteException {
        return delegate.putDBDataCluster(wsDataCluster);
    }

    @Override
    public WSDataClusterPK deleteDataCluster(WSDeleteDataCluster wsDeleteDataCluster) throws RemoteException {
        return delegate.deleteDataCluster(wsDeleteDataCluster);
    }

    @Override
    public WSStringArray getConceptsInDataCluster(WSGetConceptsInDataCluster wsGetConceptsInDataCluster) throws RemoteException {
        return delegate.getConceptsInDataCluster(wsGetConceptsInDataCluster);
    }

    @Override
    public WSConceptRevisionMap getConceptsInDataClusterWithRevisions(
            WSGetConceptsInDataClusterWithRevisions wsGetConceptsInDataClusterWithRevisions) throws RemoteException {
        return delegate.getConceptsInDataClusterWithRevisions(wsGetConceptsInDataClusterWithRevisions);
    }

    @Override
    public WSViewPKArray getViewPKs(WSGetViewPKs regexp) throws RemoteException {
        return delegate.getViewPKs(regexp);
    }

    @Override
    public WSView getView(WSGetView wsViewPK) throws RemoteException {
        return delegate.getView(wsViewPK);
    }

    @Override
    public WSBoolean existsView(WSExistsView wsViewPK) throws RemoteException {
        return delegate.existsView(wsViewPK);
    }

    @Override
    public WSViewPK putView(WSPutView wsView) throws RemoteException {
        return delegate.putView(wsView);
    }

    @Override
    public WSViewPK deleteView(WSDeleteView wsViewDel) throws RemoteException {
        return delegate.deleteView(wsViewDel);
    }

    @Override
    public WSString getBusinessConceptValue(WSGetBusinessConceptValue wsGetBusinessConceptValue) throws RemoteException {
        return delegate.getBusinessConceptValue(wsGetBusinessConceptValue);
    }

    @Override
    public WSStringArray getFullPathValues(WSGetFullPathValues wsGetFullPathValues) throws RemoteException {
        return delegate.getFullPathValues(wsGetFullPathValues);
    }

    @Override
    public WSItem getItem(WSGetItem wsGetItem) throws RemoteException {
        try {
            return delegate.getItem(wsGetItem);
        } catch (RemoteException e) {
            String entityNotFoundErrorMessage = "entity_not_found"; //$NON-NLS-1$
            if (com.amalto.webapp.core.util.Util.causeIs(e, com.amalto.core.util.EntityNotFoundException.class)) {
                EntityNotFoundException cause = Util.cause(e, EntityNotFoundException.class);
                throw new RemoteException(StringUtils.EMPTY, new WebCoreException(entityNotFoundErrorMessage, cause));
            } else if (com.amalto.webapp.core.util.Util.causeIs(e, org.hibernate.ObjectNotFoundException.class)) {
                ObjectNotFoundException cause = Util.cause(e, ObjectNotFoundException.class);
                throw new RemoteException(StringUtils.EMPTY, new WebCoreException(entityNotFoundErrorMessage, cause));
            }
            throw (new RemoteException(e.getLocalizedMessage(), e));
        }
    }

    @Override
    public WSBoolean existsItem(WSExistsItem wsExistsItem) throws RemoteException {
        return delegate.existsItem(wsExistsItem);
    }

    @Override
    public WSStringArray getItems(WSGetItems wsGetItems) throws RemoteException {
        return delegate.getItems(wsGetItems);
    }

    @Override
    public WSStringArray getItemsSort(WSGetItemsSort wsGetItemsSort) throws RemoteException {
        return delegate.getItemsSort(wsGetItemsSort);
    }

    @Override
    public WSItemPKsByCriteriaResponse getItemPKsByCriteria(WSGetItemPKsByCriteria wsGetItemPKsByCriteria) throws RemoteException {
        return delegate.getItemPKsByCriteria(wsGetItemPKsByCriteria);
    }

    @Override
    public WSItemPKsByCriteriaResponse getItemPKsByFullCriteria(WSGetItemPKsByFullCriteria wsGetItemPKsByFullCriteria)
            throws RemoteException {
        return delegate.getItemPKsByFullCriteria(wsGetItemPKsByFullCriteria);
    }

    @Override
    public WSString countItemsByCustomFKFilters(WSCountItemsByCustomFKFilters wsCountItemsByCustomFKFilters)
            throws RemoteException {
        return delegate.countItemsByCustomFKFilters(wsCountItemsByCustomFKFilters);
    }

    @Override
    public WSStringArray getItemsByCustomFKFilters(WSGetItemsByCustomFKFilters wsGetItemsByCustomFKFilters)
            throws RemoteException {
        return delegate.getItemsByCustomFKFilters(wsGetItemsByCustomFKFilters);
    }

    @Override
    public WSStringArray viewSearch(WSViewSearch wsViewSearch) throws RemoteException {
        try {
            return delegate.viewSearch(wsViewSearch);
        } catch (RemoteException e) {
            throw handleException(e, DEFAULT_REMOTE_ERROR_MESSAGE);
        }
    }

    @Override
    public WSStringArray xPathsSearch(WSXPathsSearch wsXPathsSearch) throws RemoteException {
        return delegate.xPathsSearch(wsXPathsSearch);
    }

    @Override
    public WSStringArray getItemsPivotIndex(WSGetItemsPivotIndex wsGetItemsPivotIndex) throws RemoteException {
        return delegate.getItemsPivotIndex(wsGetItemsPivotIndex);
    }

    @Override
    public WSStringArray getChildrenItems(WSGetChildrenItems wsGetChildrenItems) throws RemoteException {
        return delegate.getChildrenItems(wsGetChildrenItems);
    }

    @Override
    public WSString count(WSCount wsCount) throws RemoteException {
        return delegate.count(wsCount);
    }

    @Override
    public WSStringArray quickSearch(WSQuickSearch wsQuickSearch) throws RemoteException {
        return delegate.quickSearch(wsQuickSearch);
    }

    @Override
    public WSItemPK putItem(WSPutItem wsPutItem) throws RemoteException {
        return delegate.putItem(wsPutItem);
    }

    @Override
    public WSItemPK updateItemMetadata(WSUpdateMetadataItem wsUpdateMetadataItem) throws RemoteException {
        return delegate.updateItemMetadata(wsUpdateMetadataItem);
    }

    @Override
    public WSItemPK partialPutItem(WSPartialPutItem wsPartialPutItem) throws RemoteException {
        return delegate.partialPutItem(wsPartialPutItem);
    }

    @Override
    public WSItemPKArray putItemArray(WSPutItemArray wsPutItemArray) throws RemoteException {
        return delegate.putItemArray(wsPutItemArray);
    }

    @Override
    public WSItemPKArray putItemWithReportArray(WSPutItemWithReportArray wsPutItemWithReportArray) throws RemoteException {
        try {
            return delegate.putItemWithReportArray(wsPutItemWithReportArray);
        } catch (RemoteException e) {
            throw handleException(e, SAVE_EXCEPTION_MESSAGE);
        }
    }

    @Override
    public WSItemPK putItemWithReport(WSPutItemWithReport wsPutItemWithReport) throws RemoteException {
        try {
            return delegate.putItemWithReport(wsPutItemWithReport);
        } catch (RemoteException e) {
            throw handleException(e, SAVE_EXCEPTION_MESSAGE);
        }
    }

    @Override
    public WSItemPK putItemWithCustomReport(WSPutItemWithCustomReport wsPutItemWithCustomReport) throws RemoteException {
        return delegate.putItemWithCustomReport(wsPutItemWithCustomReport);
    }

    @Override
    public WSBoolean isItemModifiedByOther(WSIsItemModifiedByOther wsItem) throws RemoteException {
        return delegate.isItemModifiedByOther(wsItem);
    }

    @Override
    public WSPipeline extractUsingTransformer(WSExtractUsingTransformer wsExtractUsingTransformer) throws RemoteException {
        return delegate.extractUsingTransformer(wsExtractUsingTransformer);
    }

    @Override
    public WSPipeline extractUsingTransformerThruView(WSExtractUsingTransformerThruView wsExtractUsingTransformerThruView)
            throws RemoteException {
        return delegate.extractUsingTransformerThruView(wsExtractUsingTransformerThruView);
    }

    @Override
    public WSItemPK deleteItem(WSDeleteItem wsDeleteItem) throws RemoteException {
        try {
            return delegate.deleteItem(wsDeleteItem);
        } catch (RemoteException e) {
            if (Util.causeIs(e, com.amalto.core.storage.exception.ConstraintViolationException.class)) {
                ConstraintViolationException cause = Util.cause(e, ConstraintViolationException.class);
                throw new RemoteException(StringUtils.EMPTY, new WebCoreException(INTEGRITY_CONSTRAINT_CHECK_FAILED_MESSAGE,
                        cause));
            }
            throw new RemoteException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public WSString deleteItemWithReport(WSDeleteItemWithReport wsDeleteItem) throws RemoteException {
        return delegate.deleteItemWithReport(wsDeleteItem);
    }

    @Override
    public WSInt deleteItems(WSDeleteItems wsDeleteItems) throws RemoteException {
        try {
            return delegate.deleteItems(wsDeleteItems);
        } catch (RemoteException e) {
            if (Util.causeIs(e, com.amalto.core.storage.exception.ConstraintViolationException.class)) {
                throw new RemoteException(StringUtils.EMPTY, new WebCoreException(INTEGRITY_CONSTRAINT_CHECK_FAILED_MESSAGE,
                        e.getCause()));
            }
            throw new RemoteException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public WSDroppedItemPK dropItem(WSDropItem wsDropItem) throws RemoteException {
        try {
            return delegate.dropItem(wsDropItem);
        } catch (RemoteException e) {
            if (Util.causeIs(e, com.amalto.core.storage.exception.ConstraintViolationException.class)) {
                ConstraintViolationException cause = Util.cause(e, ConstraintViolationException.class);
                throw new RemoteException(StringUtils.EMPTY, new WebCoreException(INTEGRITY_CONSTRAINT_CHECK_FAILED_MESSAGE,
                        cause));
            }
            throw new RemoteException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public WSStringArray runQuery(WSRunQuery wsRunQuery) throws RemoteException {
        return delegate.runQuery(wsRunQuery);
    }

    @Override
    public WSConnectorInteractionResponse connectorInteraction(WSConnectorInteraction wsConnectorInteraction)
            throws RemoteException {
        return delegate.connectorInteraction(wsConnectorInteraction);
    }

    @Override
    public WSString serviceAction(WSServiceAction wsServiceAction) throws RemoteException {
        return delegate.serviceAction(wsServiceAction);
    }

    @Override
    public WSString getServiceConfiguration(WSServiceGetConfiguration wsGetConfiguration) throws RemoteException {
        return delegate.getServiceConfiguration(wsGetConfiguration);
    }

    @Override
    public WSString putServiceConfiguration(WSServicePutConfiguration wsPutConfiguration) throws RemoteException {
        return delegate.putServiceConfiguration(wsPutConfiguration);
    }

    @Override
    public WSServicesList getServicesList(WSGetServicesList wsGetServicesList) throws RemoteException {
        return delegate.getServicesList(wsGetServicesList);
    }

    @Override
    public WSServiceGetDocument getServiceDocument(WSString serviceName) throws RemoteException {
        return delegate.getServiceDocument(serviceName);
    }

    @Override
    public WSStoredProcedure getStoredProcedure(WSGetStoredProcedure wsGetStoredProcedure) throws RemoteException {
        return delegate.getStoredProcedure(wsGetStoredProcedure);
    }

    @Override
    public WSBoolean existsStoredProcedure(WSExistsStoredProcedure wsExistsStoredProcedure) throws RemoteException {
        return delegate.existsStoredProcedure(wsExistsStoredProcedure);
    }

    @Override
    public WSStoredProcedurePKArray getStoredProcedurePKs(WSRegexStoredProcedure regex) throws RemoteException {
        return delegate.getStoredProcedurePKs(regex);
    }

    @Override
    public WSStoredProcedurePK putStoredProcedure(WSPutStoredProcedure wsStoredProcedure) throws RemoteException {
        return delegate.putStoredProcedure(wsStoredProcedure);
    }

    @Override
    public WSStoredProcedurePK deleteStoredProcedure(WSDeleteStoredProcedure wsStoredProcedureDelete) throws RemoteException {
        return delegate.deleteStoredProcedure(wsStoredProcedureDelete);
    }

    @Override
    public WSStringArray executeStoredProcedure(WSExecuteStoredProcedure wsExecuteStoredProcedure) throws RemoteException {
        return delegate.executeStoredProcedure(wsExecuteStoredProcedure);
    }

    @Override
    public WSTransformer getTransformer(WSGetTransformer wsGetTransformer) throws RemoteException {
        return delegate.getTransformer(wsGetTransformer);
    }

    @Override
    public WSBoolean existsTransformer(WSExistsTransformer wsExistsTransformer) throws RemoteException {
        return delegate.existsTransformer(wsExistsTransformer);
    }

    @Override
    public WSTransformerPKArray getTransformerPKs(WSGetTransformerPKs regex) throws RemoteException {
        return delegate.getTransformerPKs(regex);
    }

    @Override
    public WSTransformerPK putTransformer(WSPutTransformer wsTransformer) throws RemoteException {
        return delegate.putTransformer(wsTransformer);
    }

    @Override
    public WSTransformerPK deleteTransformer(WSDeleteTransformer wsTransformerDelete) throws RemoteException {
        return delegate.deleteTransformer(wsTransformerDelete);
    }

    @Override
    public WSPipeline processBytesUsingTransformer(WSProcessBytesUsingTransformer wsProcessBytesUsingTransformer)
            throws RemoteException {
        return delegate.processBytesUsingTransformer(wsProcessBytesUsingTransformer);
    }

    @Override
    public WSPipeline processFileUsingTransformer(WSProcessFileUsingTransformer wsProcessFileUsingTransformer)
            throws RemoteException {
        return delegate.processFileUsingTransformer(wsProcessFileUsingTransformer);
    }

    @Override
    public WSBackgroundJobPK processBytesUsingTransformerAsBackgroundJob(
            WSProcessBytesUsingTransformerAsBackgroundJob wsProcessBytesUsingTransformerAsBackgroundJob) throws RemoteException {
        return delegate.processBytesUsingTransformerAsBackgroundJob(wsProcessBytesUsingTransformerAsBackgroundJob);
    }

    @Override
    public WSBackgroundJobPK processFileUsingTransformerAsBackgroundJob(
            WSProcessFileUsingTransformerAsBackgroundJob wsProcessFileUsingTransformerAsBackgroundJob) throws RemoteException {
        return delegate.processFileUsingTransformerAsBackgroundJob(wsProcessFileUsingTransformerAsBackgroundJob);
    }

    @Override
    public WSMenu getMenu(WSGetMenu wsGetMenu) throws RemoteException {
        return delegate.getMenu(wsGetMenu);
    }

    @Override
    public WSBoolean existsMenu(WSExistsMenu wsExistsMenu) throws RemoteException {
        return delegate.existsMenu(wsExistsMenu);
    }

    @Override
    public WSMenuPKArray getMenuPKs(WSGetMenuPKs regex) throws RemoteException {
        return delegate.getMenuPKs(regex);
    }

    @Override
    public WSMenuPK putMenu(WSPutMenu wsMenu) throws RemoteException {
        return delegate.putMenu(wsMenu);
    }

    @Override
    public WSMenuPK deleteMenu(WSDeleteMenu wsMenuDelete) throws RemoteException {
        return delegate.deleteMenu(wsMenuDelete);
    }

    @Override
    public WSBackgroundJobPKArray findBackgroundJobPKs(WSFindBackgroundJobPKs status) throws RemoteException {
        return delegate.findBackgroundJobPKs(status);
    }

    @Override
    public WSBackgroundJob getBackgroundJob(WSGetBackgroundJob wsGetBackgroundJob) throws RemoteException {
        return delegate.getBackgroundJob(wsGetBackgroundJob);
    }

    @Override
    public WSBackgroundJobPK putBackgroundJob(WSPutBackgroundJob wsPutBackgroundJob) throws RemoteException {
        return delegate.putBackgroundJob(wsPutBackgroundJob);
    }

    @Override
    public WSUniverse getCurrentUniverse(WSGetCurrentUniverse wsGetCurrentUniverse) throws RemoteException {
        return delegate.getCurrentUniverse(wsGetCurrentUniverse);
    }

    @Override
    public WSItemPK recoverDroppedItem(WSRecoverDroppedItem wsRecoverDroppedItem) throws RemoteException {
        return delegate.recoverDroppedItem(wsRecoverDroppedItem);
    }

    @Override
    public WSDroppedItemPKArray findAllDroppedItemsPKs(WSFindAllDroppedItemsPKs regex) throws RemoteException {
        return delegate.findAllDroppedItemsPKs(regex);
    }

    @Override
    public WSDroppedItem loadDroppedItem(WSLoadDroppedItem wsLoadDroppedItem) throws RemoteException {
        return delegate.loadDroppedItem(wsLoadDroppedItem);
    }

    @Override
    public WSDroppedItemPK removeDroppedItem(WSRemoveDroppedItem wsRemoveDroppedItem) throws RemoteException {
        return delegate.removeDroppedItem(wsRemoveDroppedItem);
    }

    @Override
    public WSMDMConfig getMDMConfiguration() throws RemoteException {
        return delegate.getMDMConfiguration();
    }

    @Override
    public WSCheckServiceConfigResponse checkServiceConfiguration(WSCheckServiceConfigRequest serviceName) throws RemoteException {
        return delegate.checkServiceConfiguration(serviceName);
    }

    @Override
    public WSRoutingRulePKArray getRoutingRulePKs(WSGetRoutingRulePKs regexp) throws RemoteException {
        return delegate.getRoutingRulePKs(regexp);
    }

    @Override
    public WSRoutingRule getRoutingRule(WSGetRoutingRule wsRoutingRulePK) throws RemoteException {
        return delegate.getRoutingRule(wsRoutingRulePK);
    }

    @Override
    public WSBoolean existsRoutingRule(WSExistsRoutingRule wsExistsRoutingRule) throws RemoteException {
        return delegate.existsRoutingRule(wsExistsRoutingRule);
    }

    @Override
    public WSRoutingRulePK putRoutingRule(WSPutRoutingRule wsRoutingRule) throws RemoteException {
        return delegate.putRoutingRule(wsRoutingRule);
    }

    @Override
    public WSRoutingRulePK deleteRoutingRule(WSDeleteRoutingRule wsRoutingRuleDel) throws RemoteException {
        return delegate.deleteRoutingRule(wsRoutingRuleDel);
    }

    @Override
    public WSTransformerV2 getTransformerV2(WSGetTransformerV2 wsGetTransformerV2) throws RemoteException {
        return delegate.getTransformerV2(wsGetTransformerV2);
    }

    @Override
    public WSBoolean existsTransformerV2(WSExistsTransformerV2 wsExistsTransformerV2) throws RemoteException {
        return delegate.existsTransformerV2(wsExistsTransformerV2);
    }

    @Override
    public WSTransformerV2PKArray getTransformerV2PKs(WSGetTransformerV2PKs regex) throws RemoteException {
        return delegate.getTransformerV2PKs(regex);
    }

    @Override
    public WSTransformerV2PK putTransformerV2(WSPutTransformerV2 wsTransformerV2) throws RemoteException {
        return delegate.putTransformerV2(wsTransformerV2);
    }

    @Override
    public WSTransformerV2PK deleteTransformerV2(WSDeleteTransformerV2 wsDeleteTransformerV2) throws RemoteException {
        return delegate.deleteTransformerV2(wsDeleteTransformerV2);
    }

    @Override
    public WSTransformerContext executeTransformerV2(WSExecuteTransformerV2 wsExecuteTransformerV2) throws RemoteException {
        return delegate.executeTransformerV2(wsExecuteTransformerV2);
    }

    @Override
    public WSBackgroundJobPK executeTransformerV2AsJob(WSExecuteTransformerV2AsJob wsExecuteTransformerV2AsJob)
            throws RemoteException {
        return delegate.executeTransformerV2AsJob(wsExecuteTransformerV2AsJob);
    }

    @Override
    public WSTransformerContext extractThroughTransformerV2(WSExtractThroughTransformerV2 wsExtractThroughTransformerV2)
            throws RemoteException {
        return delegate.extractThroughTransformerV2(wsExtractThroughTransformerV2);
    }

    @Override
    public WSBoolean existsTransformerPluginV2(WSExistsTransformerPluginV2 wsExistsTransformerPluginV2) throws RemoteException {
        return delegate.existsTransformerPluginV2(wsExistsTransformerPluginV2);
    }

    @Override
    public WSString getTransformerPluginV2Configuration(WSTransformerPluginV2GetConfiguration wsGetConfiguration)
            throws RemoteException {
        return delegate.getTransformerPluginV2Configuration(wsGetConfiguration);
    }

    @Override
    public WSString putTransformerPluginV2Configuration(WSTransformerPluginV2PutConfiguration wsPutConfiguration)
            throws RemoteException {
        return delegate.putTransformerPluginV2Configuration(wsPutConfiguration);
    }

    @Override
    public WSTransformerPluginV2Details getTransformerPluginV2Details(
            WSGetTransformerPluginV2Details wsGetTransformerPluginV2Details) throws RemoteException {
        return delegate.getTransformerPluginV2Details(wsGetTransformerPluginV2Details);
    }

    @Override
    public WSTransformerPluginV2SList getTransformerPluginV2SList(WSGetTransformerPluginV2SList wsGetTransformerPluginV2SList)
            throws RemoteException {
        return delegate.getTransformerPluginV2SList(wsGetTransformerPluginV2SList);
    }

    @Override
    public WSRoutingOrderV2 getRoutingOrderV2(WSGetRoutingOrderV2 wsGetRoutingOrderV2) throws RemoteException {
        return delegate.getRoutingOrderV2(wsGetRoutingOrderV2);
    }

    @Override
    public WSRoutingOrderV2 existsRoutingOrderV2(WSExistsRoutingOrderV2 wsExistsRoutingOrder) throws RemoteException {
        return delegate.existsRoutingOrderV2(wsExistsRoutingOrder);
    }

    @Override
    public WSRoutingOrderV2PK deleteRoutingOrderV2(WSDeleteRoutingOrderV2 wsDeleteRoutingOrder) throws RemoteException {
        return delegate.deleteRoutingOrderV2(wsDeleteRoutingOrder);
    }

    @Override
    public WSRoutingOrderV2PK executeRoutingOrderV2Asynchronously(
            WSExecuteRoutingOrderV2Asynchronously wsExecuteRoutingOrderAsynchronously) throws RemoteException {
        return delegate.executeRoutingOrderV2Asynchronously(wsExecuteRoutingOrderAsynchronously);
    }

    @Override
    public WSString executeRoutingOrderV2Synchronously(WSExecuteRoutingOrderV2Synchronously wsExecuteRoutingOrderSynchronously)
            throws RemoteException {
        return delegate.executeRoutingOrderV2Synchronously(wsExecuteRoutingOrderSynchronously);
    }

    @Override
    public WSRoutingOrderV2PKArray getRoutingOrderV2PKsByCriteria(
            WSGetRoutingOrderV2PKsByCriteria wsGetRoutingOrderV2PKsByCriteria) throws RemoteException {
        return delegate.getRoutingOrderV2PKsByCriteria(wsGetRoutingOrderV2PKsByCriteria);
    }

    @Override
    public WSRoutingOrderV2Array getRoutingOrderV2SByCriteria(WSGetRoutingOrderV2SByCriteria wsGetRoutingOrderV2SByCriteria)
            throws RemoteException {
        return delegate.getRoutingOrderV2SByCriteria(wsGetRoutingOrderV2SByCriteria);
    }

    @Override
    public WSRoutingOrderV2Array getRoutingOrderV2ByCriteriaWithPaging(
            WSGetRoutingOrderV2ByCriteriaWithPaging wsGetRoutingOrderV2ByCriteriaWithPaging) throws RemoteException {
        return delegate.getRoutingOrderV2ByCriteriaWithPaging(wsGetRoutingOrderV2ByCriteriaWithPaging);
    }

    @Override
    public WSRoutingRulePKArray routeItemV2(WSRouteItemV2 wsRouteItem) throws RemoteException {
        return delegate.routeItemV2(wsRouteItem);
    }

    @Override
    public WSRoutingEngineV2Status routingEngineV2Action(WSRoutingEngineV2Action wsRoutingEngineAction) throws RemoteException {
        return delegate.routingEngineV2Action(wsRoutingEngineAction);
    }

    @Override
    public WSMDMJobArray getMDMJob(WSMDMNULL mdmJobRequest) throws RemoteException {
        return delegate.getMDMJob(mdmJobRequest);
    }

    @Override
    public WSBoolean putMDMJob(WSPUTMDMJob putMDMJobRequest) throws RemoteException {
        return delegate.putMDMJob(putMDMJobRequest);
    }

    @Override
    public WSBoolean deleteMDMJob(WSDELMDMJob deleteMDMJobRequest) throws RemoteException {
        return delegate.deleteMDMJob(deleteMDMJobRequest);
    }

    @Override
    public WSCategoryData getMDMCategory(WSCategoryData wsCategoryDataRequest) throws RemoteException {
        return delegate.getMDMCategory(wsCategoryDataRequest);
    }

    @Override
    public WSAutoIncrement getAutoIncrement(WSAutoIncrement wsAutoIncrementRequest) throws RemoteException {
        return delegate.getAutoIncrement(wsAutoIncrementRequest);
    }

    @Override
    public WSBoolean isXmlDB() throws RemoteException {
        return delegate.isXmlDB();
    }

    @Override
    public WSDigest getDigest(WSDigestKey wsDigestKey) throws RemoteException {
        return delegate.getDigest(wsDigestKey);
    }

    @Override
    public WSLong updateDigest(WSDigest wsDigest) throws RemoteException {
        return delegate.updateDigest(wsDigest);
    }

    @Override
    public WSRole getRole(WSGetRole wsGetRole) throws RemoteException {
        return delegate.getRole(wsGetRole);
    }

    @Override
    public WSBoolean isPagingAccurate(WSInt wsInt) throws RemoteException {
        return delegate.isPagingAccurate(wsInt);
    }

    @Override
    public WSBoolean supportStaging(WSDataClusterPK dataClusterPK) throws RemoteException {
        return delegate.supportStaging(dataClusterPK);
    }

    @Override
    public WSUniversePKArray getUniversePKs(WSGetUniversePKs wsGetUniversePKs) throws RemoteException {
        return delegate.getUniversePKs(wsGetUniversePKs);
    }

    @Override
    public FKIntegrityCheckResult checkFKIntegrity(WSDeleteItem deleteItem) throws RemoteException {
        return delegate.checkFKIntegrity(deleteItem);
    }
}
