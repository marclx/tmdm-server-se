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

import static com.amalto.core.query.user.UserQueryBuilder.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.amalto.core.query.user.UserQueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONWriter;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.amalto.core.query.user.Expression;
import com.amalto.core.server.ServerContext;
import com.amalto.core.server.StorageAdmin;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageResults;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.record.DataRecord;

@Path("/system/stats/events") //$NON-NLS-1$
public class EventStatistics {

    private static final Logger LOGGER = Logger.getLogger(EventStatistics.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEventStatistics(@QueryParam("timeframe") Long timeFrame) { //$NON-NLS-1$
        StorageAdmin storageAdmin = ServerContext.INSTANCE.get().getStorageAdmin();
        Storage system = storageAdmin.get(StorageAdmin.SYSTEM_STORAGE, StorageType.SYSTEM, null);
        if (system == null) {
            LOGGER.debug("Could not find system storage. Statistics is not supported for XML database"); //$NON-NLS-1$
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        // Build statistics
        StringWriter stringWriter = new StringWriter();
        JSONWriter writer = new JSONWriter(stringWriter);
        MetadataRepository repository = system.getMetadataRepository();
        try {
            system.begin();
            // Get defined triggers
            ComplexTypeMetadata triggerDefinition = repository.getComplexType("routing-rule-pOJO"); //$NON-NLS-1$
            FieldMetadata triggerName = triggerDefinition.getField("name"); //$NON-NLS-1$
            FieldMetadata triggerParameters = triggerDefinition.getField("parameters"); //$NON-NLS-1$
            UserQueryBuilder qb = from(triggerDefinition).select(triggerName).select(triggerParameters);
            StorageResults results = system.fetch(qb.getSelect());
            Map<String, String> triggerNameToParameter;
            try {
                triggerNameToParameter = new HashMap<String, String>(results.getCount());
                for (DataRecord result : results) {
                    String trigger = String.valueOf(result.get(triggerName));
                    String parameters = String.valueOf(result.get(triggerParameters));
                    triggerNameToParameter.put(trigger, parameters);
                }
            } finally {
                results.close();
            }
            // Get event count from failed and completed queues
            writer.object().key("events"); //$NON-NLS-1$
            {
                writer.array();
                {
                    // Failed events
                    ComplexTypeMetadata failedRoutingOrder = repository.getComplexType("failed-routing-order-v2-pOJO"); //$NON-NLS-1$
                    writeTo(system, triggerNameToParameter, failedRoutingOrder, writer, timeFrame, "failed"); //$NON-NLS-1$
                    // Completed events
                    ComplexTypeMetadata completedRoutingOrder = repository.getComplexType("completed-routing-order-v2-pOJO"); //$NON-NLS-1$
                    writeTo(system, triggerNameToParameter, completedRoutingOrder, writer, timeFrame,  "completed"); //$NON-NLS-1$
                }
                writer.endArray();
            }
            writer.endObject();
            system.commit();
        } catch (JSONException e) {
            if (system.isClosed()) {
                // TMDM-7749: Ignore errors when storage is closed.
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Exception occurred due to closed storage.", e);
                }
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                system.rollback();
                throw new RuntimeException("Could not provide statistics.", e);
            }
        }
        return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(stringWriter.toString())
                .header("Access-Control-Allow-Origin", "*").build(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void writeTo(Storage system, Map<String, String> triggerNameToParameter, ComplexTypeMetadata routingOrderType,
            JSONWriter writer, Long timeFrame, String categoryName) throws JSONException {
        FieldMetadata parameters = routingOrderType.getField("service-parameters"); //$NON-NLS-1$
        writer.object().key(categoryName);
        {
            writer.array();
            {
                try {
                    for (Map.Entry<String, String> entry : triggerNameToParameter.entrySet()) {
                        String key = entry.getKey();
                        UserQueryBuilder qb = from(routingOrderType).select(count()).where(eq(parameters, entry.getValue()))
                                .limit(1).cache();
                        if (timeFrame != null && timeFrame > 0) {
                            qb.where(gte(routingOrderType.getField("time-last-run-completed"), //$NON-NLS-1$
                                    String.valueOf(System.currentTimeMillis() - (timeFrame * 1000))));
                        }
                        StorageResults results = system.fetch(qb.getSelect());
                        try {
                            writer.object().key(key).value(results.iterator().next().get("count")).endObject(); //$NON-NLS-1$
                        } finally {
                            results.close();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Could not build event statistics for '" + categoryName + "' events", e);
                }
            }
            writer.endArray();
        }
        writer.endObject();
    }
}
