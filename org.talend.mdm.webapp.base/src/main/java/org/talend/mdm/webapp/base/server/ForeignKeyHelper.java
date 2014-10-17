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
package org.talend.mdm.webapp.base.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.util.core.EDBType;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import org.talend.mdm.commmon.util.datamodel.management.BusinessConcept;
import org.talend.mdm.commmon.util.datamodel.management.ReusableType;
import org.talend.mdm.webapp.base.client.model.BasePagingLoadConfigImpl;
import org.talend.mdm.webapp.base.client.model.DataTypeConstants;
import org.talend.mdm.webapp.base.client.model.ForeignKeyBean;
import org.talend.mdm.webapp.base.client.model.ItemBasePageLoadResult;
import org.talend.mdm.webapp.base.client.util.MultilanguageMessageParser;
import org.talend.mdm.webapp.base.server.util.CommonUtil;
import org.talend.mdm.webapp.base.server.util.Constants;
import org.talend.mdm.webapp.base.shared.EntityModel;
import org.talend.mdm.webapp.base.shared.TypeModel;
import org.talend.mdm.webapp.base.shared.XpathUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.amalto.core.ejb.ItemPOJO;
import com.amalto.core.ejb.ItemPOJOPK;
import com.amalto.core.objects.datacluster.ejb.DataClusterPOJOPK;
import com.amalto.core.webservice.WSDataClusterPK;
import com.amalto.core.webservice.WSGetItemsByCustomFKFilters;
import com.amalto.core.webservice.WSInt;
import com.amalto.core.webservice.WSStringArray;
import com.amalto.core.webservice.WSWhereAnd;
import com.amalto.core.webservice.WSWhereCondition;
import com.amalto.core.webservice.WSWhereItem;
import com.amalto.core.webservice.WSXPathsSearch;
import com.amalto.webapp.core.dmagent.SchemaAbstractWebAgent;
import com.amalto.webapp.core.dmagent.SchemaWebAgent;
import com.amalto.webapp.core.util.Util;
import com.amalto.webapp.core.util.XmlUtil;
import com.extjs.gxt.ui.client.Style.SortDir;

public class ForeignKeyHelper {

    private static final Logger LOG = Logger.getLogger(ForeignKeyHelper.class);

    private static final Pattern TOTAL_COUNT_PATTERN = Pattern.compile("<totalCount>(.*)</totalCount>"); //$NON-NLS-1$

    private static SchemaAbstractWebAgent schemaManager = SchemaWebAgent.getInstance();

    public static void overrideSchemaManager(SchemaAbstractWebAgent _schemaManager) {
        schemaManager = _schemaManager;
    }

    public static ForeignKeyBean getForeignKeyBean(TypeModel model, EntityModel entityModel, String dataClusterPK, String ids,
            String xml, String currentXpath, String language) throws Exception {
        ForeignKeyBean foreignKeyBean = null;
        boolean hasForeignKeyFilter = model.getFkFilter() != null && model.getFkFilter().trim().length() > 0 ? true : false;
        ForeignKeyHolder holder = getForeignKeyHolder(xml,
                currentXpath.split("/")[0], currentXpath, model, hasForeignKeyFilter, ids); //$NON-NLS-1$
        String[] results = null;
        if (holder != null) {
            String conceptName = holder.conceptName;
            List<String> xPaths = holder.xpaths;
            WSWhereItem whereItem = holder.whereItem;
            String fkFilter = holder.fkFilter;

            // Run the query
            if (!Util.isCustomFilter(fkFilter)) {
                results = CommonUtil
                        .getPort()
                        .xPathsSearch(
                                new WSXPathsSearch(new WSDataClusterPK(dataClusterPK), null, new WSStringArray(xPaths
                                        .toArray(new String[xPaths.size()])), whereItem, -1, 0, 20, null, null, true))
                        .getStrings();
            } else {
                String injectedXpath = Util.getInjectedXpath(fkFilter);
                results = CommonUtil
                        .getPort()
                        .getItemsByCustomFKFilters(
                                new WSGetItemsByCustomFKFilters(new WSDataClusterPK(dataClusterPK), conceptName,
                                        new WSStringArray(xPaths.toArray(new String[xPaths.size()])), injectedXpath, 0, 20, null,
                                        null, true, whereItem)).getStrings();
            }
        }
        if (results != null) {
            List<ForeignKeyBean> foreignKeyBeanList = convertForeignKeyBeanList(results, entityModel, model, dataClusterPK, 0,
                    language);
            if (foreignKeyBeanList.size() > 0) {
                foreignKeyBean = foreignKeyBeanList.get(0);
            }
        }
        return foreignKeyBean;
    }

    public static ItemBasePageLoadResult<ForeignKeyBean> getForeignKeyList(BasePagingLoadConfigImpl config, TypeModel model,
            EntityModel entityModel, String dataClusterPK, boolean ifFKFilter, String value) throws Exception {

        ForeignKeyHolder holder = getForeignKeyHolder((String) config.get("xml"), (String) config.get("dataObject"), //$NON-NLS-1$ //$NON-NLS-2$
                (String) config.get("currentXpath"), model, ifFKFilter, value); //$NON-NLS-1$
        String[] results = null;
        if (holder != null) {
            String conceptName = holder.conceptName;
            List<String> xPaths = holder.xpaths;
            WSWhereItem whereItem = holder.whereItem;
            String fkFilter = holder.fkFilter;

            String sortDir;
            String xpath;

            if (SortDir.ASC.equals(SortDir.findDir(config.getSortDir()))) {
                sortDir = Constants.SEARCH_DIRECTION_ASC;
            } else if (SortDir.DESC.equals(SortDir.findDir(config.getSortDir()))) {
                sortDir = Constants.SEARCH_DIRECTION_DESC;
            } else {
                sortDir = null;
            }

            if (sortDir != null) {
                if (model.getForeignKeyInfo() != null && model.getForeignKeyInfo().size() > 0) {
                    // TMDM-5276: use substringBeforeLast in case sort field is a contained field
                    // (Entity/field1/.../fieldN)
                    xpath = StringUtils.substringBeforeLast(xPaths.get(0), "/") + "/" + config.getSortField(); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    xpath = StringUtils.substringBefore(xPaths.get(0), "/") + "/../../i"; //$NON-NLS-1$ //$NON-NLS-2$
                }
            } else {
                xpath = null;
            }

            // Run the query
            if (!Util.isCustomFilter(fkFilter)) {
                results = CommonUtil
                        .getPort()
                        .xPathsSearch(
                                new WSXPathsSearch(new WSDataClusterPK(dataClusterPK), null, new WSStringArray(xPaths
                                        .toArray(new String[xPaths.size()])), whereItem, -1, config.getOffset(), config
                                        .getLimit(), xpath, sortDir, true)).getStrings();
            } else {
                String injectedXpath = Util.getInjectedXpath(fkFilter);
                results = CommonUtil
                        .getPort()
                        .getItemsByCustomFKFilters(
                                new WSGetItemsByCustomFKFilters(new WSDataClusterPK(dataClusterPK), conceptName,
                                        new WSStringArray(xPaths.toArray(new String[xPaths.size()])), injectedXpath, config
                                                .getOffset(), config.getLimit(), xpath, sortDir, true, whereItem)).getStrings();
            }
        }
        if (results != null) {

            Matcher matcher = TOTAL_COUNT_PATTERN.matcher(results[0]);
            String count;
            if (matcher.matches()) {
                count = matcher.group(1);
            } else {
                throw new IllegalArgumentException("Total count '" + results[0] + "' does not match expected format"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            boolean isPagingAccurate = CommonUtil.getPort().isPagingAccurate(new WSInt(Integer.valueOf(count))).is_true();
            return new ItemBasePageLoadResult<ForeignKeyBean>(convertForeignKeyBeanList(results, entityModel, model,
                    dataClusterPK, config.getOffset(), (String) config.get("language")), config.getOffset(), //$NON-NLS-1$
                    Integer.valueOf(count), isPagingAccurate);
        } else {
            return new ItemBasePageLoadResult<ForeignKeyBean>(new ArrayList<ForeignKeyBean>(), config.getOffset(), 0);
        }
    }

    private static List<ForeignKeyBean> convertForeignKeyBeanList(String[] results, EntityModel entityModel, TypeModel model,
            String dataClusterPK, int offset, String language) throws Exception {
        List<ForeignKeyBean> fkBeans = new ArrayList<ForeignKeyBean>();
        if (LOG.isDebugEnabled()) {
            for (String result : results) {
                LOG.debug(result);
            }
        }
        String fk = model.getForeignkey().split("/")[0]; //$NON-NLS-1$
        BusinessConcept businessConcept = schemaManager.getBusinessConcept(fk);
        // init foreignKey info type
        if (model.getForeignKeyInfo() != null && model.getForeignKeyInfo().size() > 0 && businessConcept != null) {
            businessConcept.load();
        }
        // Polymorphism FK
        boolean isPolymorphismFK = false;
        if (businessConcept != null) {
            String fkReusableType = businessConcept.getCorrespondTypeName();
            if (fkReusableType != null) {
                List<ReusableType> subTypes = SchemaWebAgent.getInstance().getMySubtypes(fkReusableType, true);
                List<ReusableType> parentTypes = SchemaWebAgent.getInstance().getMyParents(fkReusableType);
                isPolymorphismFK = subTypes.size() > 0 || parentTypes.size() > 0;
            }
        }

        for (int currentResult = 1; currentResult < results.length; currentResult++) { // TMDM-2834: first
                                                                                       // result is count
            if (results[currentResult] == null) {
                // Ignore null results.
                continue;
            }
            Element resultAsDOM = Util.parse(results[currentResult]).getDocumentElement();

            ForeignKeyBean bean = new ForeignKeyBean();
            if (businessConcept != null && isPolymorphismFK) {
                bean.setTypeName(businessConcept.getCorrespondTypeName());
                bean.setConceptName(fk);
            }
            String id = ""; //$NON-NLS-1$
            NodeList nodes = Util.getNodeList(resultAsDOM, "//i"); //$NON-NLS-1$
            if (nodes != null) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    if (nodes.item(i) instanceof Element) {
                        id += "[" + (nodes.item(i).getTextContent() == null ? "" : nodes.item(i).getTextContent()) + "]"; //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
                    }
                }
            }

            if (resultAsDOM.getNodeName().equals("result")) { //$NON-NLS-1$
                initFKBean(dataClusterPK, entityModel, resultAsDOM, bean, fk, model.getForeignKeyInfo(),
                        businessConcept != null ? businessConcept.getXpathDerivedSimpleTypeMap() : null, language);
                convertFKInfo2DisplayInfo(bean, model.getForeignKeyInfo());
            } else {
                bean.set(resultAsDOM.getNodeName(), resultAsDOM.getTextContent().trim());
            }
            bean.setId(id);
            fkBeans.add(bean);
        }
        return fkBeans;
    }

    protected static class ForeignKeyHolder {

        String conceptName;

        List<String> xpaths;

        String orderbyPath;

        WSWhereItem whereItem;

        String fkFilter;
    }

    protected static ForeignKeyHolder getForeignKeyHolder(String xml, String dataObject, String currentXpath, TypeModel model,
            boolean ifFKFilter, String value) throws Exception {

        String xpathForeignKey = model.getForeignkey();
        if (xpathForeignKey == null) {
            return null;
        }

        // to verify
        String xpathInfoForeignKey;
        if (model.getForeignKeyInfo() != null && model.getForeignKeyInfo().size() > 0) {
            xpathInfoForeignKey = Util.joinStrings(model.getForeignKeyInfo()
                    .toArray(new String[model.getForeignKeyInfo().size()]), ",");//$NON-NLS-1$
        } else {
            xpathInfoForeignKey = "";//$NON-NLS-1$
        }
        // in search panel, the fkFilter is empty
        String fkFilter;
        if (ifFKFilter) {
            fkFilter = model.getFkFilter().replaceAll("&quot;", "\""); //$NON-NLS-1$ //$NON-NLS-2$
            fkFilter = parseForeignKeyFilter(xml, dataObject, fkFilter, currentXpath);
        } else {
            fkFilter = ""; //$NON-NLS-1$
        }

        String initxpathForeignKey = Util.getForeignPathFromPath(xpathForeignKey);

        WSWhereCondition whereCondition = Util.getConditionFromPath(xpathForeignKey);
        WSWhereItem whereItem = null;
        if (whereCondition != null) {
            whereItem = new WSWhereItem(whereCondition, null, null);
        }

        if (!Util.isCustomFilter(fkFilter)) {
            // get FK filter
            WSWhereItem fkFilterWi = Util.getConditionFromFKFilter(xpathForeignKey, xpathInfoForeignKey, fkFilter, false);
            if (fkFilterWi != null) {
                whereItem = fkFilterWi;
            }
        }

        initxpathForeignKey = initxpathForeignKey.split("/")[0]; //$NON-NLS-1$

        if (xpathInfoForeignKey == null) {
            xpathInfoForeignKey = ""; //$NON-NLS-1$
        }

        // foreign key set by business concept
        if (initxpathForeignKey.split("/").length == 1) { //$NON-NLS-1$
            String conceptName = initxpathForeignKey;
            // determine if we have xPath Infos: e.g. labels to display
            String[] xpathInfos = new String[1];
            if (xpathInfoForeignKey.trim().length() != 0) {
                xpathInfos = xpathInfoForeignKey.split(","); //$NON-NLS-1$
            } else {
                xpathInfos[0] = initxpathForeignKey;
            }
            value = value == null ? "" : value; //$NON-NLS-1$

            // build query - add a content condition on the pivot if we search for a particular value
            if (value != null && !"".equals(value.trim()) && !".*".equals(value.trim()) && !"'*'".equals(value.trim())) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                List<WSWhereItem> condition = new ArrayList<WSWhereItem>();
                if (whereItem != null) {
                    condition.add(whereItem);
                }

                String fkWhere = initxpathForeignKey + "/../* CONTAINS " + value; //$NON-NLS-1$
                if (xpathInfoForeignKey.trim().length() > 0) {
                    StringBuffer ids = new StringBuffer();
                    String realXpathForeignKey = null; // In studio, ForeignKey = ConceptName, but not ConceptName/Id
                    if (xpathForeignKey.indexOf("/") == -1) { //$NON-NLS-1$
                        String[] fks = Util.getBusinessConceptKeys(conceptName);
                        if (fks != null && fks.length > 0) {
                            realXpathForeignKey = fks[0];
                            for (int i = 0; i < fks.length; i++) {
                                ids.append(fks[i] + " CONTAINS " + value); //$NON-NLS-1$
                                if (i != fks.length - 1) {
                                    ids.append(" OR "); //$NON-NLS-1$
                                }
                            }
                        }
                    }
                    StringBuffer sb = new StringBuffer();
                    for (String fkInfo : xpathInfos) {
                        sb.append((fkInfo.startsWith(".") ? XpathUtil.convertAbsolutePath( //$NON-NLS-1$
                                (realXpathForeignKey != null && realXpathForeignKey.trim().length() > 0) ? realXpathForeignKey
                                        : xpathForeignKey, fkInfo) : fkInfo)
                                + " CONTAINS " + value); //$NON-NLS-1$
                        sb.append(" OR "); //$NON-NLS-1$
                    }
                    if (realXpathForeignKey != null) {
                        sb.append(ids.toString());
                    } else {
                        sb.append(xpathForeignKey + " CONTAINS " + value); //$NON-NLS-1$
                    }
                    fkWhere = sb.toString();
                }

                WSWhereItem wc = Util.buildWhereItems(fkWhere);
                condition.add(wc);
                WSWhereAnd and = new WSWhereAnd(condition.toArray(new WSWhereItem[condition.size()]));
                WSWhereItem whand = new WSWhereItem(null, and, null);
                if (whand != null) {
                    whereItem = whand;
                }
            }

            // add the xPath Infos Path
            ArrayList<String> xPaths = new ArrayList<String>();
            if (model.isRetrieveFKinfos()) {
                // add the xPath Infos Path
                for (String xpathInfo : xpathInfos) {
                    xPaths.add(Util.getFormatedFKInfo(xpathInfo.replaceFirst(initxpathForeignKey, initxpathForeignKey),
                            initxpathForeignKey));
                }
            }
            // add the key paths last, since there may be multiple keys
            xPaths.add(initxpathForeignKey + "/../../i"); //$NON-NLS-1$
            // order by
            String orderbyPath = null;
            if (!MDMConfiguration.getDBType().getName().equals(EDBType.QIZX.getName())) {
                if (xpathInfoForeignKey.length() != 0) {
                    orderbyPath = Util.getFormatedFKInfo(xpathInfos[0].replaceFirst(initxpathForeignKey, initxpathForeignKey),
                            initxpathForeignKey);
                }
            }
            ForeignKeyHolder holder = new ForeignKeyHolder();
            holder.xpaths = xPaths;
            holder.orderbyPath = orderbyPath;
            holder.conceptName = conceptName;
            holder.whereItem = whereItem;
            holder.fkFilter = fkFilter;
            return holder;
        }
        return null;
    }

    protected static void initFKBean(String dataClusterPK, EntityModel entityModel, Element ele, ForeignKeyBean bean, String fk,
            List<String> getForeignKeyInfos, Map<String, String> xpathTypeMap, String language) throws Exception {
        int positionIndex = 0;
        for (int i = 0; i < ele.getChildNodes().getLength(); i++) {
            if (ele.getChildNodes().item(i) instanceof Element) {
                positionIndex++;
                Element curEle = (Element) ele.getChildNodes().item(i);
                String value = curEle.getTextContent().trim();
                String nodeName = curEle.getNodeName();
                String fkInfo;
                int keyInfoIndex = positionIndex - 1;

                if (positionIndex <= getForeignKeyInfos.size()) {
                    fkInfo = getForeignKeyInfos.get(keyInfoIndex);
                    while (!fkInfo.endsWith(nodeName) && keyInfoIndex < getForeignKeyInfos.size() - 1) {
                        fkInfo = getForeignKeyInfos.get(++keyInfoIndex);
                    }
                    if (!fkInfo.endsWith(nodeName)) {
                        fkInfo = fk + "/" + curEle.getNodeName(); //$NON-NLS-1$ 
                    }
                } else {
                    fkInfo = fk + "/" + curEle.getNodeName(); //$NON-NLS-1$
                }

                if (getForeignKeyInfos != null && getForeignKeyInfos.contains(fkInfo)) {
                    if (entityModel != null) {
                        value = getDisplayValue(curEle.getTextContent().trim(), fkInfo, dataClusterPK, entityModel, language);
                    }
                    if (xpathTypeMap != null && xpathTypeMap.containsKey(fkInfo)
                            && xpathTypeMap.get(fkInfo).equals("xsd:MULTI_LINGUAL")) { //$NON-NLS-1$
                        bean.getForeignKeyInfo().put(fkInfo, MultilanguageMessageParser.getValueByLanguage(value, language));
                    } else {
                        bean.getForeignKeyInfo().put(fkInfo, value);
                    }
                }
                bean.set(curEle.getNodeName(), value);
                initFKBean(dataClusterPK, entityModel, curEle, bean, fk, getForeignKeyInfos, xpathTypeMap, language);
            }
        }
    }

    public static String getDisplayValue(String value, String path, String dataClusterPK, EntityModel entityModel, String language)
            throws Exception {
        List<String> subFKInfoList = null;
        String subFKInfo = ""; //$NON-NLS-1$
        TypeModel subModel = entityModel.getTypeModel(path);
        if (subModel != null && subModel.getForeignKeyInfo() != null && subModel.getForeignKeyInfo().size() > 0
                && !"".equals(value)) { //$NON-NLS-1$
            subFKInfoList = subModel.getForeignKeyInfo();
        }

        if (subFKInfoList != null) {
            ItemPOJOPK pk = new ItemPOJOPK();
            String[] ids = CommonUtil.extractFKRefValue(value, language);
            for (String id : ids) {
                if (id == null || StringUtils.EMPTY.equals(id.trim())) {
                    return StringUtils.EMPTY;
                }
            }
            pk.setIds(ids);
            String conceptName = subModel.getForeignkey().split("/")[0]; //$NON-NLS-1$
            pk.setConceptName(conceptName);
            pk.setDataClusterPOJOPK(new DataClusterPOJOPK(dataClusterPK));
            ItemPOJO item = com.amalto.core.util.Util.getItemCtrl2Local().getItem(pk);
            if (item != null) {
                org.w3c.dom.Document document = item.getProjection().getOwnerDocument();
                List<String> foreignKeyInfo = subModel.getForeignKeyInfo();
                for (String foreignKeyPath : foreignKeyInfo) {
                    NodeList nodes = com.amalto.core.util.Util.getNodeList(document,
                            StringUtils.substringAfter(foreignKeyPath, "/")); //$NON-NLS-1$
                    if (nodes.getLength() == 1) {
                        String foreignKeyValue = nodes.item(0).getTextContent();

                        if (subModel.getType().equals(DataTypeConstants.MLS)) {
                            foreignKeyValue = MultilanguageMessageParser.getValueByLanguage(foreignKeyValue, language);
                        }

                        if (subFKInfo.equals("")) { //$NON-NLS-1$
                            subFKInfo += foreignKeyValue;
                        } else {
                            subFKInfo += "-" + foreignKeyValue; //$NON-NLS-1$
                        }
                    }
                }
            }
            return subFKInfo;
        } else {
            return value;
        }
    }

    public static void convertFKInfo2DisplayInfo(ForeignKeyBean bean, List<String> foreignKeyInfos) {
        if (foreignKeyInfos.size() != 0) {
            StringBuilder displayInfo = new StringBuilder();
            Map<String, String> foreignKeyInfoMap = bean.getForeignKeyInfo();
            for (String info : foreignKeyInfos) {
                if (!info.isEmpty() && foreignKeyInfoMap.get(info) != null) {
                    if (displayInfo.length() == 0) {
                        displayInfo.append(foreignKeyInfoMap.get(info));
                    } else {
                        displayInfo.append("-"); //$NON-NLS-1$
                        displayInfo.append(foreignKeyInfoMap.get(info));
                    }
                }
            }
            bean.setDisplayInfo(displayInfo.toString());
        }
    }

    private static String parseForeignKeyFilter(String xml, String dataObject, String fkFilter, String currentXpath)
            throws Exception {
        String parsedFkfilter = fkFilter;
        if (fkFilter != null) {
            if (Util.isCustomFilter(fkFilter)) {
                fkFilter = StringEscapeUtils.unescapeXml(fkFilter);
                parsedFkfilter = parseRightExpression(xml, dataObject, fkFilter, currentXpath);
                return parsedFkfilter;
            }
            // parse
            String[] criterias = fkFilter.split("#");//$NON-NLS-1$
            List<Map<String, String>> conditions = new ArrayList<Map<String, String>>();
            for (String cria : criterias) {
                Map<String, String> conditionMap = new HashMap<String, String>();
                String[] values = cria.split("\\$\\$");//$NON-NLS-1$
                for (int i = 0; i < values.length; i++) {

                    switch (i) {
                    case 0:
                        conditionMap.put("Xpath", values[0]);//$NON-NLS-1$
                        break;
                    case 1:
                        conditionMap.put("Operator", values[1]);//$NON-NLS-1$
                        break;
                    case 2:
                        String rightValueOrPath = values[2];
                        rightValueOrPath = StringEscapeUtils.unescapeXml(rightValueOrPath);
                        rightValueOrPath = parseRightValueOrPath(xml, dataObject, rightValueOrPath, currentXpath);
                        if (isFkPath(values[0])) {
                            rightValueOrPath = wrapFkValue(rightValueOrPath);
                        } else {
                            rightValueOrPath = unwrapFkValue(rightValueOrPath);
                        }
                        conditionMap.put("Value", rightValueOrPath);//$NON-NLS-1$
                        break;
                    case 3:
                        conditionMap.put("Predicate", values[3]);//$NON-NLS-1$
                        break;
                    default:
                        break;
                    }
                }
                conditions.add(conditionMap);
            }
            // build
            if (conditions.size() > 0) {
                StringBuffer sb = new StringBuffer();
                for (Map<String, String> map : conditions) {
                    Map<String, String> conditionMap = map;
                    if (conditionMap.size() > 0) {
                        String xpath = conditionMap.get("Xpath") == null ? "" : conditionMap.get("Xpath");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                        String operator = conditionMap.get("Operator") == null ? "" : conditionMap.get("Operator");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                        String value = conditionMap.get("Value") == null ? "" : conditionMap.get("Value");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                        String predicate = conditionMap.get("Predicate") == null ? "" : conditionMap.get("Predicate");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                        sb.append(xpath + "$$" + operator + "$$" + value + "$$" + predicate + "#");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
                    }
                }
                if (sb.length() > 0) {
                    parsedFkfilter = sb.toString();
                }
            }
        }
        return parsedFkfilter;
    }

    public static String wrapFkValue(String value) {
        if (value.startsWith("[") && value.endsWith("]")) { //$NON-NLS-1$//$NON-NLS-2$
            return value;
        }
        return "[" + value + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static String unwrapFkValue(String value) {
        if (value.startsWith("[") && value.endsWith("]")) { //$NON-NLS-1$ //$NON-NLS-2$
            if (value.contains("][")) { //$NON-NLS-1$
                return value;
            } else {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    public static boolean isFkPath(String fkPath) {
        String concept = fkPath.split("/")[0]; //$NON-NLS-1$
        try {
            BusinessConcept businessConcept = schemaManager.getBusinessConcept(concept);
            Map<String, String> fkMap = businessConcept.getForeignKeyMap();
            if (fkMap != null && fkMap.containsKey("/" + fkPath)) { //$NON-NLS-1$
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static String parseRightExpression(String xml, String dataObject, String rightExpression, String currentXpath)
            throws Exception {
        // Use legacy logic for custom FK filter
        String patternString = dataObject + "(/[A-Za-z0-9_]*)+";//$NON-NLS-1$
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(rightExpression);
        while (matcher.find()) {
            for (int j = 0; j < matcher.groupCount(); j++) {
                String gettedXpath = matcher.group(j);
                if (gettedXpath != null) {
                    String replaceValue = parseRightValueOrPath(xml, dataObject, gettedXpath, currentXpath);
                    if (replaceValue != null) {
                        rightExpression = rightExpression.replaceFirst(patternString, "\"" + replaceValue + "\""); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            }
        }
        return rightExpression;

    }

    private static String parseRightValueOrPath(String xml, String dataObject, String rightValueOrPath, String currentXpath)
            throws Exception {
        if (rightValueOrPath == null || currentXpath == null) {
            throw new IllegalArgumentException();
        }

        boolean isValue = false;
        boolean isRelativePath = false;

        rightValueOrPath = rightValueOrPath.trim();// space(s) ignore

        // switch cases
        if (rightValueOrPath.startsWith("\"") && rightValueOrPath.endsWith("\"") || //$NON-NLS-1$//$NON-NLS-2$
                rightValueOrPath.startsWith("'") && rightValueOrPath.endsWith("'")) {
            isValue = true;
        } else if (rightValueOrPath.startsWith(".") || rightValueOrPath.startsWith("..")) {
            isRelativePath = true;
        }

        // cases handle
        String result = rightValueOrPath;// by default result equals input value/path
        if (isValue) {
            result = rightValueOrPath.substring(1, rightValueOrPath.length() - 1);
        } else {
            if (xml != null) {
                // get context for expression
                org.dom4j.Document doc = XmlUtil.parseDocument(Util.parse(xml));
                org.dom4j.Node currentNode = doc.selectSingleNode(currentXpath);
                org.dom4j.Node targetNode = null;
                if (isRelativePath) {
                    targetNode = currentNode.selectSingleNode(rightValueOrPath);
                } else {
                    String xpath = rightValueOrPath.startsWith("/") ? rightValueOrPath : "/" + rightValueOrPath; //$NON-NLS-1$//$NON-NLS-2$
                    targetNode = currentNode.selectSingleNode(xpath);
                }
                if (targetNode != null && targetNode.getText() != null) {
                    result = targetNode.getText();
                }
            }
        }

        return result;
    }
}