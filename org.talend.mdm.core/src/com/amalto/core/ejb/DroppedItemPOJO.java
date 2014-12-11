package com.amalto.core.ejb;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amalto.core.metadata.LongString;
import com.amalto.core.schema.manage.SchemaCoreAgent;
import com.amalto.core.util.Util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.talend.mdm.commmon.util.bean.ItemCacheKey;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import org.talend.mdm.commmon.util.datamodel.management.BusinessConcept;
import org.talend.mdm.commmon.util.datamodel.management.DataModelID;
import org.talend.mdm.commmon.util.webapp.XObjectType;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;
import org.xml.sax.InputSource;

import com.amalto.core.delegator.ILocalUser;
import com.amalto.core.ejb.local.XmlServerSLWrapperLocal;
import com.amalto.core.objects.datacluster.ejb.DataClusterPOJOPK;
import com.amalto.core.objects.universe.ejb.UniversePOJO;
import com.amalto.core.util.LocalUser;
import com.amalto.core.util.XtentisException;

public class DroppedItemPOJO implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(DroppedItemPOJO.class);

    private static final String MDM_ITEMS_TRASH = "MDMItemsTrash"; //$NON-NLS-1$

    private String revisionID;

    private DataClusterPOJOPK dataClusterPOJOPK;

    private String uniqueId;

    private String conceptName;//redundancy

    private String[] ids;//redundancy

    private String partPath;

    private String insertionUserName;

    private Long insertionTime;

    private String projection;

    public DroppedItemPOJO() {
    }

    public DroppedItemPOJO(String revisionID,
                           DataClusterPOJOPK dataClusterPOJOPK,
                           String uniqueId,
                           String conceptName,
                           String[] ids,
                           String partPath,
                           String projection,
                           String insertionUserName,
                           long insertionTime) {
        this.revisionID = revisionID;
        this.dataClusterPOJOPK = dataClusterPOJOPK;
        this.uniqueId = uniqueId;
        this.conceptName = conceptName;
        this.ids = ids;
        this.partPath = partPath;
        this.projection = projection;
        this.insertionUserName = insertionUserName;
        this.insertionTime = insertionTime;
    }

    public String getRevisionID() {
        return revisionID;
    }

    public void setRevisionID(String revisionID) {
        this.revisionID = revisionID;
    }

    public DataClusterPOJOPK getDataClusterPOJOPK() {
        return dataClusterPOJOPK;
    }

    public void setDataClusterPOJOPK(DataClusterPOJOPK dataClusterPOJOPK) {
        this.dataClusterPOJOPK = dataClusterPOJOPK;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public String[] getIds() {
        return ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }

    public String getPartPath() {
        return partPath;
    }

    public void setPartPath(String partPath) {
        this.partPath = partPath;
    }

    public String getInsertionUserName() {
        return insertionUserName;
    }

    public void setInsertionUserName(String insertionUserName) {
        this.insertionUserName = insertionUserName;
    }

    public Long getInsertionTime() {
        return insertionTime;
    }

    public void setInsertionTime(Long insertionTime) {
        this.insertionTime = insertionTime;
    }

    @LongString
    public String getProjection() {
        return projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }

    public DroppedItemPOJOPK obtainDroppedItemPK() {
        return new DroppedItemPOJOPK(
                revisionID,
                obtainRefItemPK(),
                partPath
        );
    }

    public ItemPOJOPK obtainRefItemPK() {
        return new ItemPOJOPK(
                dataClusterPOJOPK,
                conceptName,
                ids
        );
    }

    @Override
    public String toString() {
        // Marshal
        StringWriter sw = new StringWriter();
        try {
            Marshaller.marshal(this, sw);
        } catch (Exception e) {
            return "Could not marshal object due to " + e.getMessage();
        }
        return sw.toString();
    }

    /**
     * recover dropped item
     */
    public static ItemPOJOPK recover(DroppedItemPOJOPK droppedItemPOJOPK) throws XtentisException {
        // validate input
        if (droppedItemPOJOPK == null) {
            return null;
        }
        String partPath = droppedItemPOJOPK.getPartPath();
        if (partPath == null || partPath.length() == 0) {
            return null;
        }
        if (!"/".equals(partPath)) { //$NON-NLS-1$
            throw new IllegalArgumentException("Path '" + partPath + "' is not valid.");
        }
        ItemPOJOPK refItemPOJOPK = droppedItemPOJOPK.getRefItemPOJOPK();
        String actionName = "recover"; //$NON-NLS-1$
        // for recover we need to be admin, or have a role of admin, or role of write on instance
        rolesFilter(refItemPOJOPK, actionName, "w"); //$NON-NLS-1$
        // get the universe and revision ID
        universeFilter(refItemPOJOPK);
        String sourceItemRevision = droppedItemPOJOPK.getRevisionId();
        XmlServerSLWrapperLocal server = Util.getXmlServerCtrlLocal();
        try {
            // load dropped content
            String doc = server.getDocumentAsString(null,
                    MDM_ITEMS_TRASH,
                    droppedItemPOJOPK.getUniquePK(),
                    null);
            if (doc == null) {
                return null;
            }
            // recover source item
            DroppedItemPOJO droppedItemPOJO = (DroppedItemPOJO) Unmarshaller.unmarshal(DroppedItemPOJO.class,
                    new InputSource(new StringReader(doc)));
            String clusterName = refItemPOJOPK.getDataClusterPOJOPK().getUniqueId();
            server.start(clusterName);
            try {
                server.putDocumentFromString(droppedItemPOJO.getProjection(),
                        refItemPOJOPK.getUniqueID(),
                        clusterName,
                        sourceItemRevision);
                server.commit(clusterName);
            } catch (Exception e) {
                server.rollback(clusterName);
                throw e;
            }
            //delete dropped item
            try {
                server.deleteDocument(
                        null,
                        MDM_ITEMS_TRASH,
                        droppedItemPOJOPK.getUniquePK()
                );
            } catch (Exception e) {
                server.rollback(MDM_ITEMS_TRASH);
                throw e;
            }
            // It needs to be removed from cache, because it could still be loaded from cache.
            ItemPOJO.getCache().remove(
                    new ItemCacheKey(droppedItemPOJOPK.getRevisionId(),
                            droppedItemPOJOPK.getRefItemPOJOPK().getUniqueID(),
                            droppedItemPOJOPK.getRefItemPOJOPK().getDataClusterPOJOPK().getUniqueId()));
            return refItemPOJOPK;
        } catch (Exception e) {
            String err = "Unable to " + actionName + " the dropped item " + droppedItemPOJOPK.getUniquePK()
                    + ": " + e.getClass().getName() + ": " + e.getLocalizedMessage();
            LOGGER.error(err, e);
            throw new XtentisException(err, e);
        }
    }

    /**
     * find all pks of dropped items
     */
    public static List<DroppedItemPOJOPK> findAllPKs(String regex) throws XtentisException {
        universeFilter();
        // get XmlServerSLWrapperLocal
        XmlServerSLWrapperLocal server = Util.getXmlServerCtrlLocal();
        if ("".equals(regex) || "*".equals(regex) || ".*".equals(regex)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            regex = null;
        }
        try {
            //retrieve the item
            String[] ids = server.getAllDocumentsUniqueID(null, MDM_ITEMS_TRASH);
            if (ids == null) {
                return Collections.emptyList();
            }
            //build PKs collection
            List<DroppedItemPOJOPK> list = new ArrayList<DroppedItemPOJOPK>();
            Map<String, BusinessConcept> conceptMap = new HashMap<String, BusinessConcept>();
            for (String uid : ids) {
                String[] uidValues = uid.split("\\."); //$NON-NLS-1$
                ItemPOJOPK refItemPOJOPK;
                if (MDMConfiguration.isSqlDataBase()) {
                    if (uidValues.length < 3) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Could not read id '" + uid + "'. Skipping it.");
                        }
                        continue;
                    }
                    // check xsd key's length
                    String uidPrefix = uidValues[0] + "." + uidValues[1] + ".";  //$NON-NLS-1$//$NON-NLS-2$
                    String[] idArray = Arrays.copyOfRange(uidValues, 2, uidValues.length);
                    if (!conceptMap.containsKey(uidPrefix)) {
                        BusinessConcept businessConcept = SchemaCoreAgent.getInstance().getBusinessConcept(uidValues[1], new DataModelID(uidValues[0], null));
                        businessConcept.load();
                        conceptMap.put(uidPrefix, businessConcept);
                    }
                    if (conceptMap.get(uidPrefix) != null && conceptMap.get(uidPrefix).getKeyFieldPaths().size() == 1) {
                        idArray = new String[] {StringUtils.removeStart(uid, uidPrefix)};
                    }
                    
                    refItemPOJOPK = new ItemPOJOPK(new DataClusterPOJOPK(uidValues[0]), uidValues[1], idArray);
                } else {
                    // XML db format (deprecated)
                    if (uidValues.length < 4) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Could not read id '" + uid + "'. Skipping it.");
                        }
                        continue;
                    }
                    uidValues[uidValues.length - 1] = uidValues[uidValues.length - 1].replaceAll("\\-$", ""); //$NON-NLS-1$//$NON-NLS-2$
                    refItemPOJOPK = new ItemPOJOPK(new DataClusterPOJOPK(uidValues[1]), uidValues[2], Arrays.copyOfRange(
                            uidValues, 3, uidValues.length));
                }
                // set revision id as ""
                DroppedItemPOJOPK droppedItemPOJOPK = new DroppedItemPOJOPK("", refItemPOJOPK, "/"); //$NON-NLS-1$ //$NON-NLS-2$
                if (regex != null) {
                    if (uid.matches(regex)) {
                        list.add(droppedItemPOJOPK);
                    }
                } else {
                    list.add(droppedItemPOJOPK);
                }
            }
            return list;
        } catch (Exception e) {
            String err = "Unable to find all the identifiers for dropped items "
                    + ": " + e.getClass().getName() + ": " + e.getLocalizedMessage();
            LOGGER.error(err, e);
            throw new XtentisException(err, e);
        }
    }

    /**
     * load a dropped item
     */
    public static DroppedItemPOJO load(DroppedItemPOJOPK droppedItemPOJOPK) throws XtentisException {
        if (droppedItemPOJOPK == null) {
            return null;
        }
        ItemPOJOPK refItemPOJOPK = droppedItemPOJOPK.getRefItemPOJOPK();
        String actionName = "load"; //$NON-NLS-1$
        //for load we need to be admin, or have a role of admin , or role of write on instance or role of read on instance
        rolesFilter(refItemPOJOPK, actionName, "r"); //$NON-NLS-1$
        //get XmlServerSLWrapperLocal
        XmlServerSLWrapperLocal server = Util.getXmlServerCtrlLocal();
        //load the dropped item
        try {
            //retrieve the dropped item
            String droppedItemStr = server.getDocumentAsString(null,
                    MDM_ITEMS_TRASH,
                    droppedItemPOJOPK.getUniquePK());
            if (droppedItemStr == null) {
                return null;
            }
            return (DroppedItemPOJO) Unmarshaller.unmarshal(DroppedItemPOJO.class,
                    new InputSource(new StringReader(droppedItemStr)));
        } catch (Exception e) {
            String err = "Unable to load the dropped item  " + droppedItemPOJOPK.getUniquePK()
                    + ": " + e.getClass().getName() + ": " + e.getLocalizedMessage();
            LOGGER.error(err, e);
            throw new XtentisException(err, e);
        }
    }

    /**
     * remove a dropped item record
     */
    public static DroppedItemPOJOPK remove(DroppedItemPOJOPK droppedItemPOJOPK) throws XtentisException {
        if (droppedItemPOJOPK == null) {
            return null;
        }
        ItemPOJOPK refItemPOJOPK = droppedItemPOJOPK.getRefItemPOJOPK();
        String actionName = "remove"; //$NON-NLS-1$
        //for remove we need to be admin, or have a role of admin , or role of write on instance
        rolesFilter(refItemPOJOPK, actionName, "w");  //$NON-NLS-1$
        //get XmlServerSLWrapperLocal
        XmlServerSLWrapperLocal server = Util.getXmlServerCtrlLocal();
        try {
            //remove the record
            long res = server.deleteDocument(
                    null,
                    MDM_ITEMS_TRASH,
                    droppedItemPOJOPK.getUniquePK()
            );
            if (res == -1) {
                return null;
            }
            return droppedItemPOJOPK;
        } catch (Exception e) {
            String err = "Unable to " + actionName + " the dropped item " + droppedItemPOJOPK.getUniquePK()
                    + ": " + e.getClass().getName() + ": " + e.getLocalizedMessage();
            LOGGER.error(err, e);
            throw new XtentisException(err, e);
        }
    }

    private static String universeFilter(ItemPOJOPK refItemPOJOPK) throws XtentisException {
        UniversePOJO universe = universeFilter();
        return universe.getConceptRevisionID(refItemPOJOPK.getConceptName());
    }

    private static UniversePOJO universeFilter() throws XtentisException {
        UniversePOJO universe = LocalUser.getLocalUser().getUniverse();
        if (universe == null) {
            String err = "ERROR: no Universe set for user '" + LocalUser.getLocalUser().getUsername() + "'";
            LOGGER.error(err);
            throw new XtentisException(err);
        }
        return universe;
    }

    private static String rolesFilter(ItemPOJOPK refItemPOJOPK, String actionName, String authorizeMode) throws XtentisException {
        boolean authorized = false;
        ILocalUser user = LocalUser.getLocalUser();

        if (authorizeMode.equals("w")) {
            if (MDMConfiguration.getAdminUser().equals(user.getUsername())
                    || LocalUser.UNAUTHENTICATED_USER.equals(user.getUsername())) {
                authorized = true;
            } else if (XSystemObjects.isExist(XObjectType.DATA_CLUSTER, refItemPOJOPK.getDataClusterPOJOPK().getUniqueId())
                    || user.userItemCanWrite(ItemPOJO.adminLoad(refItemPOJOPK), refItemPOJOPK.getDataClusterPOJOPK().getUniqueId(), refItemPOJOPK.getConceptName())) {
                authorized = true;
            }
        } else if (authorizeMode.equals("r")) {
            if (MDMConfiguration.getAdminUser().equals(user.getUsername())
                    || LocalUser.UNAUTHENTICATED_USER.equals(user.getUsername())) {
                authorized = true;
            } else if (user.userItemCanRead(ItemPOJO.adminLoad(refItemPOJOPK))) {
                authorized = true;
            }
        }
        if (!authorized) {
            String err = "Unauthorized " + actionName + " access by " +
                    "user " + user.getUsername() + " on a dropped item of Item '" + refItemPOJOPK.getUniqueID() + "'";
            LOGGER.error(err);
            throw new XtentisException(err);
        }
        return user.getUsername();
    }
}
