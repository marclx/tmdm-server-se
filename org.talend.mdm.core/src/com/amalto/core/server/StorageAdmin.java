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

package com.amalto.core.server;

import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageType;

import java.util.Collection;

/**
 * API for the {@link Storage} administration. There's usually a 1-to-1 mapping between a Storage and a MDM data cluster.
 * However this might not be true for System objects (stored in the amaltoOBJECTS* data clusters), where all system objects
 * are stored in the same Storage. But this is be transparent to the API user (calling code should just not expect a 1-1
 * mapping between Storage instance and data cluster).
 */
public interface StorageAdmin {

    String STAGING_SUFFIX = "#STAGING";

    /**
     * Internal/System storage name: this is the name for the Storage instance that stores all MDM internal
     * configuration.
     */
    String SYSTEM_STORAGE = "__SYSTEM";

    /**
     * @param revisionID A revision id (or null for HEAD revision).
     * @return An array that contains all storage names in this revision. Returns an empty array if there's no storage
     * for this revision.
     */
    String[] getAll(String revisionID);

    /**
     * Removes a storage from MDM: this is what happens when a user deletes a container. This method does not
     * always removes container's data (this depends on the value of <code>dropExistingData</code>).
     *
     * @param storageName      A storage name.
     * @param type             The storage {@link com.amalto.core.storage.StorageType type} (Staging, Master...).
     * @param revisionID       A revision id (or null for HEAD revision).
     * @param dropExistingData <code>true</code> removes also storage data during operation.
     */
    void delete(String storageName, StorageType type, String revisionID, boolean dropExistingData);

    /**
     * Deletes all storages for a given revision. This code is equivalent to:
     * <code>
     * String[] names = storageAdmin.getAll(revisionId);
     * for(String name : names) {
     * storageAdmin.delete(revisionId, dropExistingData);
     * }
     * </code>
     *
     * @param revisionID       A revision id (or null for HEAD revision).
     * @param dropExistingData <code>true</code> removes also storage data during operation.
     */
    void deleteAll(String revisionID, boolean dropExistingData);

    /**
     * Creates a new storage based on parameter information. If a storage already exists, this return the previously created
     * instance.
     *
     * @param dataModelName  A data model name (one managed by {@link MetadataRepositoryAdmin#get(String)}).
     * @param storageName    A non-null, non-empty identifier for the new storage instance.
     * @param type           The storage {@link com.amalto.core.storage.StorageType type} (Staging, Master...).
     * @param dataSourceName A datasource name (a data source that must exists in datasources configuration file).
     * @param revisionId     A revision id (<code>null</code> is the same as "HEAD" for the HEAD revision).   @return A previously created instance or a new instance ready for immediate usage.
     */
    Storage create(String dataModelName, String storageName, StorageType type, String dataSourceName, String revisionId);

    /**
     * @param storageName A non-null, non-empty identifier for the storage instance.
     * @param storageType Indicates whether caller checks for {@link com.amalto.core.storage.StorageType#MASTER} or {@link com.amalto.core.storage.StorageType#STAGING}.  @return Returns <code>true</code> if storage already exist for this name, revision and type.
     * @param revision    A revision id (<code>null</code> is the same as "HEAD" for the HEAD revision).
     */
    boolean exist(String storageName, StorageType storageType, String revision);

    /**
     * To be closed on MDM server shutdown: this method ensures all managed storages are correctly closed.
     */
    void close();

    /**
     * @param storageName A {@link com.amalto.core.storage.Storage} name.
     * @return The datasource name that should be used with storage <code>storageName</code>.
     */
    String getDatasource(String storageName);

    /**
     * @param storageName A {@link com.amalto.core.storage.Storage} name.
     * @param type        The storage {@link com.amalto.core.storage.StorageType type} (Staging, Master...).
     * @param revisionId  A revision id.
     * @return A previously created {@link Storage} or <code>null</code> if no storage was previously created.
     * @see #create(String, String, com.amalto.core.storage.StorageType, String, String)
     */
    Storage get(String storageName, StorageType type, String revisionId);

    /**
     * @param storageName A {@link com.amalto.core.storage.Storage} name.
     * @param type        The storage {@link com.amalto.core.storage.StorageType type} (Staging, Master...).
     * @return A list of previously created {@link Storage} for all revisions or empty list if no storage was previously
     * created.
     * @see #create(String, String, com.amalto.core.storage.StorageType, String, String)
     */
    Collection<Storage> get(String storageName, StorageType type);

    /**
     * Returns if <code>storageName</code> is allowed to get a staging area (for instance Update Report and Crossreferencing
     * are not allowed to).
     * @param storageName A storage name.
     * @return <code>true</code> if <code>storageName</code> can have a staging area, <code>false</code> otherwise.
     */
    boolean supportStaging(String storageName);

    /**
     * @param name A data model or a data container name.
     * @return The {@link com.amalto.core.storage.StorageType type} of storage to be associated with the name.
     */
    StorageType getType(String name);
}
