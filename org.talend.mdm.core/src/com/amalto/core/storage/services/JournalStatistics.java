/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.storage.services;

import com.amalto.core.query.user.*;
import com.amalto.core.server.ServerContext;
import com.amalto.core.server.StorageAdmin;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageResults;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.record.DataRecord;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONWriter;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import static com.amalto.core.query.user.UserQueryBuilder.*;

@Path("/system/stats/journal") //$NON-NLS-1$
public class JournalStatistics {

    private static final Logger LOGGER = Logger.getLogger(JournalStatistics.class);

    private static final int DEFAULT_SLICE_NUMBER = 5;

    private static void writeStatsTo(Storage storage, UserQueryBuilder query, String statName, JSONWriter writer)
            throws JSONException {
        Expression expression = query.getExpression();
        Field field = new Field(query.getSelect().getTypes().get(0).getField("TimeInMillis")); //$NON-NLS-1$
        Iterator<TimeSlicer.Slice> slices = TimeSlicer.slice(expression, storage, DEFAULT_SLICE_NUMBER, field);
        writer.array();
        {
            while (slices.hasNext()) {
                TimeSlicer.Slice slice = slices.next();
                StorageResults results = storage.fetch(slice.getExpression()); // Expects a live transaction here.
                try {
                    Long count = (Long) results.iterator().next().get("count"); //$NON-NLS-1$
                    if (count > 0) {
                        writer.object();
                        {
                            writer.key(statName).value(count);
                            writer.key("from").value(slice.getLowerBound()); //$NON-NLS-1$
                            writer.key("to").value(slice.getUpperBound()); //$NON-NLS-1$
                        }
                        writer.endObject();
                    }
                } finally {
                    results.close();
                }
            }
        }
        writer.endArray();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{container}") //$NON-NLS-1$
    public Response getJournalStatistics(@PathParam("container") //$NON-NLS-1$
    String containerName, @QueryParam("lang") //$NON-NLS-1$
    String language, @QueryParam("timeframe") Long timeFrame, @QueryParam("top") Integer top) { //$NON-NLS-1$ //$NON-NLS-2$
        StorageAdmin storageAdmin = ServerContext.INSTANCE.get().getStorageAdmin();
        Storage dataStorage = storageAdmin.get(containerName, StorageType.MASTER, null);
        if (dataStorage == null) {
            Storage systemStorage = storageAdmin.get(StorageAdmin.SYSTEM_STORAGE, StorageType.SYSTEM, null);
            if (systemStorage == null) { // is xmldb, not supported/implemented
                LOGGER.debug("Could not find system storage. Statistics is not supported for XMLDB"); //$NON-NLS-1$
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            throw new IllegalArgumentException("Container '" + containerName + "' does not exist.");
        }
        Storage updateReportStorage = storageAdmin.get(XSystemObjects.DC_UPDATE_PREPORT.getName(), StorageType.MASTER, null);
        ComplexTypeMetadata updateType = updateReportStorage.getMetadataRepository().getComplexType("Update");//$NON-NLS-1$
        // Get the top N types
        MetadataRepository repository = dataStorage.getMetadataRepository();
        Collection<ComplexTypeMetadata> types;
        if(top == null || top <= 0) {
            types = repository.getUserComplexTypes(); // No top information, handle all types
        } else {
            types = new ArrayList<ComplexTypeMetadata>(top); // Get the top N types present in update report
            try {
                // Query uses a quite expensive operation (orderBy count of field value), hopefully Concept is indexed
                // See com.amalto.core.server.MetadataRepositoryAdminImpl.getIndexedExpressions()
                UserQueryBuilder topTypeQuery = from(updateType)
                        .select(updateType.getField("Concept")) //$NON-NLS-1$
                        .where(eq(updateType.getField("DataModel"), containerName)) //$NON-NLS-1$
                        .orderBy(count(updateType.getField("Concept")), OrderBy.Direction.DESC) //$NON-NLS-1$
                        .limit(top)
                        .cache(); // The top N types should change much, so cache result
                updateReportStorage.begin();
                StorageResults topTypes = updateReportStorage.fetch(topTypeQuery.getSelect());
                try {
                    for (DataRecord topType : topTypes) {
                        ComplexTypeMetadata type = repository.getComplexType(String.valueOf(topType.get("Concept"))); //$NON-NLS-1$
                        if (type != null) {
                            types.add(type);
                        }
                    }
                } finally {
                    topTypes.close();
                }
                updateReportStorage.commit();
            } catch (Exception e) {
                try {
                    updateReportStorage.rollback();
                } catch (Exception rollbackException) {
                    LOGGER.debug("Unable to rollback transaction.", e);
                }
                LOGGER.warn("Could get the top " + top + " types.");
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Unable to compute top " + top  + " types due to storage exception.", e);
                }
                return Response.status(Response.Status.NO_CONTENT).build();
            }
        }
        // Build statistics
        try {
            StringWriter stringWriter = new StringWriter();
            JSONWriter writer = new JSONWriter(stringWriter);
            updateReportStorage.begin();
            writer.object().key("journal"); //$NON-NLS-1$
            {
                writer.array();
                {
                    for (ComplexTypeMetadata type : types) {
                        writer.object();
                        {
                            // Starts stats for type
                            String name;
                            if (language != null) {
                                name = type.getName(new Locale(language));
                            } else {
                                name = type.getName();
                            }
                            writer.key(name);
                            writer.array();
                            {
                                // Write create stats
                                writer.object().key("creations"); //$NON-NLS-1$
                                {
                                    UserQueryBuilder createQuery = from(updateType).select(count())
                                            .where(and(eq(updateType.getField("Concept"), type.getName()), //$NON-NLS-1$
                                                    eq(updateType.getField("OperationType"), "CREATE") //$NON-NLS-1$ //$NON-NLS-2$
                                            )).limit(1).cache();
                                    if (timeFrame != null && timeFrame > 0) {
                                        createQuery.where(gte(updateType.getField("TimeInMillis"), //$NON-NLS-1$
                                                String.valueOf(System.currentTimeMillis() - (timeFrame * 1000))));
                                    }
                                    writeStatsTo(updateReportStorage, createQuery, "create", writer); //$NON-NLS-1$
                                }
                                writer.endObject();
                                // Write update stats
                                writer.object().key("updates"); //$NON-NLS-1$
                                {
                                    UserQueryBuilder updateQuery = from(updateType).select(alias(count(), "count")) //$NON-NLS-1$
                                            .where(and(eq(updateType.getField("Concept"), type.getName()), //$NON-NLS-1$
                                                    eq(updateType.getField("OperationType"), "UPDATE") //$NON-NLS-1$ //$NON-NLS-2$
                                            )).limit(1).cache();
                                    if (timeFrame != null && timeFrame > 0) {
                                        updateQuery.where(gte(updateType.getField("TimeInMillis"), //$NON-NLS-1$
                                                String.valueOf(System.currentTimeMillis() - (timeFrame * 1000))));
                                    }
                                    writeStatsTo(updateReportStorage, updateQuery, "update", writer); //$NON-NLS-1$
                                }
                                writer.endObject();
                            }
                            writer.endArray();
                        }
                        writer.endObject();
                    }
                }
                writer.endArray();
            }
            writer.endObject();
            updateReportStorage.commit();
            return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(stringWriter.toString())
                    .header("Access-Control-Allow-Origin", "*").build(); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (Exception e) {
            try {
                updateReportStorage.rollback();
            } catch (Exception rollbackException) {
                LOGGER.debug("Unable to rollback transaction.", e);
            }
            if (updateReportStorage.isClosed()) {
                // TMDM-7749: Ignore errors when storage is closed.
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Exception occurred due to closed storage.", e);
                }
            } else {
                // TMDM-7970: Ignore all storage related errors.
                LOGGER.warn("Unable to compute statistics.");
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Unable to compute statistics due to storage exception.", e);
                }
            }
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }
}
