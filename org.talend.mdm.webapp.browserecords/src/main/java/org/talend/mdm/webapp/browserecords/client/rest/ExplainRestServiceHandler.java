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
package org.talend.mdm.webapp.browserecords.client.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.restlet.client.Request;
import org.restlet.client.Response;
import org.restlet.client.data.MediaType;
import org.restlet.client.data.Method;
import org.restlet.client.ext.json.JsonRepresentation;
import org.restlet.client.representation.StringRepresentation;
import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.client.rest.ClientResourceWrapper;
import org.talend.mdm.webapp.base.client.rest.ResourceSessionAwareCallbackHandler;
import org.talend.mdm.webapp.base.client.rest.RestServiceHelper;
import org.talend.mdm.webapp.browserecords.client.util.StagingConstant;

import com.extjs.gxt.ui.client.data.BaseTreeModel;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

public class ExplainRestServiceHandler {

    private String restServiceUrl = RestServiceHelper.BASE_URL + "datamanager/services/tasks/matching/explain"; //$NON-NLS-1$

    private ClientResourceWrapper client;

    private static ExplainRestServiceHandler handler;

    private ExplainRestServiceHandler() {
        client = new ClientResourceWrapper();
    }

    public static ExplainRestServiceHandler get() {
        if (handler == null) {
            handler = new ExplainRestServiceHandler();
        }
        return handler;
    }

    public void setClient(ClientResourceWrapper client) {
        this.client = client;
    }

    public void explainGroupResult(String dataCluster, String concept, String groupId,
            final SessionAwareAsyncCallback<BaseTreeModel> callback) {
        if (dataCluster == null || dataCluster.isEmpty() || concept == null || concept.isEmpty() || groupId == null
                || groupId.isEmpty()) {
            throw new IllegalArgumentException();
        }
        StringBuilder uri = new StringBuilder();
        uri.append(restServiceUrl).append(RestServiceHelper.SEPARATOR).append(dataCluster).append(RestServiceHelper.SEPARATOR)
                .append("groups").append(RestServiceHelper.SEPARATOR); //$NON-NLS-1$
        Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("type", concept); //$NON-NLS-1$
        parameterMap.put("group", groupId); //$NON-NLS-1$
        client.init(Method.GET, uri.toString(), parameterMap);
        client.setCallback(new ResourceSessionAwareCallbackHandler() {

            @Override
            public void doProcess(Request request, Response response) throws Exception {
                BaseTreeModel result = null;
                JsonRepresentation jsonRepresentation = RestServiceHelper.getJsonRepresentationFromResponse(response);
                if (jsonRepresentation != null) {
                    result = buildGroupResultFromJsonRepresentation(jsonRepresentation);
                }
                callback.onSuccess(result);
            }
        });
        client.request();
    }

    public void simulateMatch(String dataCluster, String concept, String ids,
            final SessionAwareAsyncCallback<BaseTreeModel> callback) {
        if (dataCluster == null || dataCluster.isEmpty() || concept == null || concept.isEmpty()) {
            throw new IllegalArgumentException();
        }
        StringBuilder uri = new StringBuilder();
        uri.append(restServiceUrl).append(RestServiceHelper.SEPARATOR).append(dataCluster).append(RestServiceHelper.SEPARATOR)
                .append("records").append(RestServiceHelper.SEPARATOR); //$NON-NLS-1$
        Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("type", concept); //$NON-NLS-1$

        client.init(Method.POST, uri.toString(), parameterMap);
        client.setPostEntity(new StringRepresentation(ids, MediaType.TEXT_PLAIN));
        client.setCallback(new ResourceSessionAwareCallbackHandler() {

            @Override
            public void doProcess(Request request, Response response) throws Exception {
                BaseTreeModel result = null;
                JsonRepresentation jsonRepresentation = RestServiceHelper.getJsonRepresentationFromResponse(response);
                if (jsonRepresentation != null) {
                    result = buildGroupResultFromJsonRepresentation(jsonRepresentation);
                }
                callback.onSuccess(result);
            }
        });
        client.request(MediaType.TEXT_PLAIN);
    }

    public void compareRecords(String dataModel, String concept, String recordXml,
            final SessionAwareAsyncCallback<BaseTreeModel> callback) {
        if (dataModel == null || dataModel.isEmpty() || concept == null || concept.isEmpty()) {
            throw new IllegalArgumentException();
        }
        StringBuilder uri = new StringBuilder();
        uri.append(restServiceUrl).append(RestServiceHelper.SEPARATOR);
        Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("model", dataModel); //$NON-NLS-1$
        parameterMap.put("type", concept); //$NON-NLS-1$
        client.init(Method.POST, uri.toString(), parameterMap);
        client.setPostEntity(new StringRepresentation(recordXml, MediaType.APPLICATION_XML));
        client.setCallback(new ResourceSessionAwareCallbackHandler() {

            @Override
            public void doProcess(Request request, Response response) throws Exception {
                BaseTreeModel result = null;
                JsonRepresentation jsonRepresentation = RestServiceHelper.getJsonRepresentationFromResponse(response);
                if (jsonRepresentation != null) {
                    JSONObject jsonObject = jsonRepresentation.getJsonObject();
                    JSONValue jsonValue = jsonObject.get(StagingConstant.MATCH_ROOT_NAME);
                    result = buildTreeModelFromJsonRepresentation(jsonValue);
                }
                callback.onSuccess(result);
            }
        });
        client.request(MediaType.APPLICATION_XML);
    }

    private BaseTreeModel buildGroupResultFromJsonRepresentation(JsonRepresentation representation) throws IOException {
        BaseTreeModel rootModel = new BaseTreeModel();
        List<String> matchFieldList = new ArrayList<String>();
        rootModel.set(StagingConstant.MATCH_FIELD_LIST, matchFieldList);
        JSONObject jsonObject = representation.getJsonObject();
        if (jsonObject != null) {
            JSONValue rootValue = jsonObject.get(StagingConstant.MATCH_ROOT_NAME);
            if (rootValue != null && rootValue.isArray() != null) {
                JSONArray array = rootValue.isArray();
                for (int i = 0; i < array.size(); i++) {
                    JSONValue groupValue = array.get(i);
                    buildGroupNode(groupValue, rootModel, i);
                }
            }
        }
        return rootModel;
    }

    private void buildGroupNode(JSONValue value, final BaseTreeModel parent, int index) {
        JSONArray groupArray = getJSONArray(value, StagingConstant.MATCH_GROUP_NAME);
        if (groupArray != null) {
            final BaseTreeModel group = new BaseTreeModel();
            group.set(StagingConstant.MATCH_DATA, buildTreeModelFromJsonRepresentation(value).getChild(0));
            group.setParent(parent);
            JSONValue resultValue = groupArray.get(0);
            JSONArray resultArray = getJSONArray(resultValue, "result"); //$NON-NLS-1$
            if (resultArray != null) {
                group.set(StagingConstant.MATCH_IS_GROUP, true);
                group.set(StagingConstant.MATCH_GROUP_NAME, StagingConstant.MATCH_GROUP_NAME + (index + 1));
                JSONValue relatedIdArrayValue = getJSONValue(resultArray, StagingConstant.MATCH_RELATED_IDS);
                if (relatedIdArrayValue != null && relatedIdArrayValue.isArray() != null) {
                    JSONArray relatedIdArray = relatedIdArrayValue.isArray();
                    List<String> idList = new ArrayList<String>();
                    for (int i = 0; i < relatedIdArray.size(); i++) {
                        idList.add(getStringValue(relatedIdArray.get(i)));
                    }
                    group.set(StagingConstant.MATCH_RELATED_IDS, idList);
                    String groupId = getStringValue(getJSONValue(resultArray, StagingConstant.MATCH_GROUP_ID));
                    group.set(StagingConstant.MATCH_GROUP_ID, groupId);
                    group.set(StagingConstant.MATCH_GROUP_CONFIDENCE,
                            getStringValue(getJSONValue(resultArray, StagingConstant.MATCH_GROUP_CONFIDENCE)));
                    group.set(StagingConstant.MATCH_GROUP_GID, groupId);
                    group.set(StagingConstant.MATCH_GROUP_SZIE, relatedIdArray.size());
                    JSONValue valueArrayValue = getJSONValue(resultArray, "values"); //$NON-NLS-1$
                    if (valueArrayValue != null && valueArrayValue.isArray() != null) {
                        JSONArray valueArray = valueArrayValue.isArray();
                        List<String> matchFieldList = parent.get(StagingConstant.MATCH_FIELD_LIST);
                        for (int i = 0; i < valueArray.size(); i++) {
                            JSONArray childArray = getJSONArray(valueArray.get(i), StagingConstant.MATCH_VALUE);
                            if (childArray != null) {
                                String fieldName = getStringValue(getJSONValue(childArray, StagingConstant.MATCH_FIELD));
                                if (!matchFieldList.contains(fieldName)) {
                                    matchFieldList.add(fieldName);
                                }
                                String fieldValue = getStringValue(getJSONValue(childArray, StagingConstant.MATCH_VALUE));
                                group.set(fieldName, fieldValue);
                            }
                        }
                    }
                }
            }
            JSONValue detailsValue = getJSONValue(groupArray, StagingConstant.MATCH_DETAILS);
            if (detailsValue != null && detailsValue.isArray() != null) {
                JSONArray detailsArray = detailsValue.isArray();
                if (detailsArray != null) {
                    for (int i = 0; i < detailsArray.size(); i++) {
                        JSONValue detailValue = detailsArray.get(i);
                        buildDetailNode(detailValue, group);
                    }
                }
            }
            parent.add(group);
        }
    }

    private void buildDetailNode(JSONValue value, BaseTreeModel parent) {
        final JSONArray detailArray = getJSONArray(value, "detail"); //$NON-NLS-1$
        if (detailArray != null) {
            List<String> relatedIdList = parent.get(StagingConstant.MATCH_RELATED_IDS);
            String id = getStringValue(getJSONValue(detailArray, StagingConstant.MATCH_GROUP_ID));
            if (relatedIdList.contains(id)) {
                final BaseTreeModel detail = new BaseTreeModel();
                detail.setParent(parent);
                detail.set(StagingConstant.MATCH_IS_GROUP, false);
                detail.set(StagingConstant.MATCH_GROUP_NAME, ""); //$NON-NLS-1$
                detail.set(StagingConstant.MATCH_GROUP_ID, id);
                JSONValue valuesValue = getJSONValue(detailArray, "values"); //$NON-NLS-1$
                if (valuesValue != null && valuesValue.isArray() != null) {
                    JSONArray valuesArray = valuesValue.isArray();
                    for (int i = 0; i < valuesArray.size(); i++) {
                        JSONArray valueArray = getJSONArray(valuesArray.get(i), "value"); //$NON-NLS-1$
                        if (valueArray != null) {
                            String fieldName = getStringValue(getJSONValue(valueArray, StagingConstant.MATCH_FIELD));
                            String fieldValue = getStringValue(getJSONValue(valueArray, "value")); //$NON-NLS-1$
                            detail.set(fieldName, fieldValue);
                        }
                    }
                }

                for (int i = 0; i < detailArray.size(); i++) {
                    JSONValue detailValue = detailArray.get(i);
                    JSONArray matchArray = getJSONArray(detailValue, "match"); //$NON-NLS-1$
                    if (matchArray != null) {
                        if ("true".equals(getStringValue(getJSONValue(matchArray, "is_match")))) { //$NON-NLS-1$ //$NON-NLS-2$
                            JSONValue scoresValue = getJSONValue(matchArray, "scores"); //$NON-NLS-1$
                            if (scoresValue != null && scoresValue.isArray() != null) {
                                JSONArray scoresArray = scoresValue.isArray();
                                StringBuilder attributebBuilder = new StringBuilder();
                                for (int j = 0; j < scoresArray.size(); j++) {
                                    JSONArray scoreArray = getJSONArray(scoresArray.get(j), StagingConstant.MATCH_SCORE);
                                    if (scoreArray != null) {
                                        attributebBuilder.append(getStringValue(getJSONValue(scoreArray,
                                                StagingConstant.MATCH_FIELD)));
                                        attributebBuilder.append(StagingConstant.VALUE_SEPARATOR);
                                        attributebBuilder.append(getStringValue(getJSONValue(scoreArray,
                                                StagingConstant.MATCH_VALUE)));
                                        attributebBuilder.append(","); //$NON-NLS-1$
                                    }
                                }
                                String scoreValue = attributebBuilder.length() > 0 ? attributebBuilder.toString().substring(0,
                                        attributebBuilder.toString().length() - 1) : ""; //$NON-NLS-1$
                                detail.set(StagingConstant.MATCH_SCORE, scoreValue);
                            }
                        }
                    }
                }
                parent.add(detail);
            }
        }
    }

    private BaseTreeModel buildTreeModelFromJsonRepresentation(JSONValue value) {
        BaseTreeModel rootModel = new BaseTreeModel();
        rootModel.set(StagingConstant.DISPLAY_NAME, StagingConstant.MATCH_GROUP_NAME);
        retriveNode(value, rootModel);
        return rootModel;
    }

    public void retriveNode(JSONValue jsonValue, BaseTreeModel parent) {
        if (jsonValue.isArray() != null) {
            JSONArray array = jsonValue.isArray();
            for (int i = 0; i < array.size(); i++) {
                JSONValue value = array.get(i);
                String content = getStringValue(value);
                if (content != null) {
                    addChildNode(content, parent);
                }
                retriveNode(value, parent);
            }
        } else if (jsonValue.isObject() != null) {
            JSONObject object = jsonValue.isObject();
            Set<String> keySet = object.keySet();
            for (String key : keySet) {
                JSONValue value = object.get(key);
                String content = getStringValue(value);
                if (content != null) {
                    addChildNode(key + StagingConstant.VALUE_SEPARATOR + content, parent);
                } else {
                    BaseTreeModel treeModel = addChildNode(key, parent);
                    retriveNode(value, treeModel);
                }
            }
        }
    }

    private BaseTreeModel addChildNode(String content, BaseTreeModel parent) {
        BaseTreeModel treeModel = new BaseTreeModel();
        treeModel.set(StagingConstant.DISPLAY_NAME, content);
        parent.add(treeModel);
        return treeModel;
    }

    public JSONArray getJSONArray(JSONValue value, String key) {
        JSONArray childArray = null;
        if (value != null && value.isObject() != null) {
            JSONObject object = value.isObject();
            JSONValue childValue = object.get(key);
            if (childValue != null && childValue.isArray() != null) {
                childArray = childValue.isArray();
            }
        }
        return childArray;
    }

    private JSONValue getJSONValue(JSONArray array, String key) {
        JSONValue value = null;
        for (int i = 0; i < array.size(); i++) {
            JSONValue jsonValue = array.get(i);
            if (jsonValue.isObject() != null) {
                JSONObject object = jsonValue.isObject();
                if (object.get(key) != null) {
                    value = object.get(key);
                    break;
                }
            }
        }
        return value;
    }

    private String getStringValue(JSONValue jsonValue) {
        String value = null;
        if (jsonValue != null) {
            if (jsonValue.isString() != null) {
                value = jsonValue.isString().toString().replaceAll("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
            } else if (jsonValue.isBoolean() != null) {
                value = jsonValue.isBoolean().toString();
            } else if (jsonValue.isNumber() != null) {
                value = jsonValue.isNumber().toString();
            }
        }
        return value;
    }
}
