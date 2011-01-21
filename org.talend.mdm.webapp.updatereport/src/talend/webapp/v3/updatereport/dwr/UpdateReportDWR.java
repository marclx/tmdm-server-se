package talend.webapp.v3.updatereport.dwr;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;
import org.w3c.dom.Document;

import talend.webapp.v3.updatereport.bean.DataChangeLog;

import com.amalto.webapp.core.bean.Configuration;
import com.amalto.webapp.core.bean.ListRange;
import com.amalto.webapp.core.json.JSONObject;
import com.amalto.webapp.core.util.Util;
import com.amalto.webapp.util.webservices.WSCount;
import com.amalto.webapp.util.webservices.WSDataClusterPK;
import com.amalto.webapp.util.webservices.WSString;
import com.amalto.webapp.util.webservices.WSStringArray;
import com.amalto.webapp.util.webservices.WSStringPredicate;
import com.amalto.webapp.util.webservices.WSWhereAnd;
import com.amalto.webapp.util.webservices.WSWhereCondition;
import com.amalto.webapp.util.webservices.WSWhereItem;
import com.amalto.webapp.util.webservices.WSWhereOperator;
import com.amalto.webapp.util.webservices.WSXPathsSearch;

public class UpdateReportDWR {

    private Logger logger = org.apache.log4j.Logger.getLogger(this.getClass());

    public UpdateReportDWR() {

    }

    public ListRange getUpdateReportList(int start, int limit, String sort, String dir, String regex) throws Exception {

        ListRange listRange = new ListRange();

        WSDataClusterPK wsDataClusterPK = new WSDataClusterPK(XSystemObjects.DC_UPDATE_PREPORT.getName());
        String conceptName = "Update";// Hard Code

        // Where condition
        ArrayList<WSWhereItem> conditions = new ArrayList<WSWhereItem>();
        if (regex != null && regex.length() > 0) {
            JSONObject criteria = new JSONObject(regex);
            boolean itemsBrowser = !criteria.isNull("itemsBrowser") && criteria.get("itemsBrowser").equals("true");

            if (itemsBrowser) {
                Configuration configuration = Configuration.getInstance();
                String dataCluster = configuration.getCluster();
                String dataModel = configuration.getModel();

                WSWhereCondition clusterwc = new WSWhereCondition("/Update/DataCluster", WSWhereOperator.EQUALS,
                        dataCluster.trim(), WSStringPredicate.NONE, false);

                WSWhereCondition modelwc = new WSWhereCondition("/Update/DataModel", WSWhereOperator.EQUALS, dataModel.trim(),
                        WSStringPredicate.NONE, false);

                WSWhereItem wsWhereDataCluster = new WSWhereItem(clusterwc, null, null);
                WSWhereItem wsWhereDataModel = new WSWhereItem(modelwc, null, null);
                conditions.add(wsWhereDataCluster);
                conditions.add(wsWhereDataModel);
            }

            if (!criteria.isNull("concept")) {
                String concept = (String) criteria.get("concept");
                WSWhereCondition wc = new WSWhereCondition("/Update/Concept", itemsBrowser ? WSWhereOperator.EQUALS
                        : WSWhereOperator.CONTAINS, concept.trim(), WSStringPredicate.NONE, false);
                WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
                conditions.add(wsWhereItem);
            }

            if (!criteria.isNull("key")) {
                String key = (String) criteria.get("key");
                WSWhereCondition wc = new WSWhereCondition("/Update/Key", itemsBrowser ? WSWhereOperator.EQUALS
                        : WSWhereOperator.CONTAINS, key.trim(), WSStringPredicate.NONE, false);
                WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
                conditions.add(wsWhereItem);
            }

            if (!criteria.isNull("source")) {
                String source = (String) criteria.get("source");
                WSWhereCondition wc = new WSWhereCondition("/Update/Source", WSWhereOperator.EQUALS, source.trim(),
                        WSStringPredicate.NONE, false);
                WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
                conditions.add(wsWhereItem);
            }

            if (!criteria.isNull("operationType")) {
                String operationType = (String) criteria.get("operationType");
                WSWhereCondition wc = new WSWhereCondition("/Update/OperationType", WSWhereOperator.EQUALS, operationType.trim(),
                        WSStringPredicate.NONE, false);
                WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
                conditions.add(wsWhereItem);
            }

            if (!criteria.isNull("startDate")) {
                String startDate = (String) criteria.get("startDate");
                SimpleDateFormat dataFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = dataFmt.parse(startDate);
                WSWhereCondition wc = new WSWhereCondition("/Update/TimeInMillis", WSWhereOperator.GREATER_THAN_OR_EQUAL,
                        date.getTime() + "", WSStringPredicate.NONE, false);
                WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
                conditions.add(wsWhereItem);
            }

            if (!criteria.isNull("endDate")) {
                String endDate = (String) criteria.get("endDate");
                SimpleDateFormat dataFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = dataFmt.parse(endDate);
                WSWhereCondition wc = new WSWhereCondition("/Update/TimeInMillis", WSWhereOperator.LOWER_THAN_OR_EQUAL,
                        date.getTime() + "", WSStringPredicate.NONE, false);
                WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
                conditions.add(wsWhereItem);
            }

        }

        WSWhereItem wi = null;
        if (conditions.size() == 0)
            wi = null;
        else {
            WSWhereAnd and = new WSWhereAnd(conditions.toArray(new WSWhereItem[conditions.size()]));
            wi = new WSWhereItem(null, and, null);
        }

        // count each time
        WSString totalString = Util.getPort().count(new WSCount(wsDataClusterPK, conceptName, wi, -1));
        int totalSize = 0;
        if (totalString != null && totalString.getValue() != null && totalString.getValue().length() > 0)
            totalSize = Integer.parseInt(totalString.getValue());

        String[] results = Util
                .getPort()
                .xPathsSearch(
                        new WSXPathsSearch(wsDataClusterPK, null, new WSStringArray(new String[] { conceptName }), wi, -1, start,
                                limit, "/Update/TimeInMillis", "descending")).getStrings();

        logger.debug("Total:" + totalSize + ";Start:" + start + ";Limit:" + limit + ";Length:"
                + (results == null ? 0 : results.length));
        // sub result
        // start=start<results.length?start:results.length-1;
        // if(start<0)start=0;
        // int end=results.length<(start+limit)?results.length-1:(start+limit-1);
        //
        // String[] subResults=end+1-start<limit?new String[end+1-start]:new String[limit];
        // for (int i = start,j=0; i < end+1; i++,j++) {
        // subResults[j]=results[i];
        // }
        String[] subResults = results;
        // parse data
        DataChangeLog[] data = new DataChangeLog[subResults.length];
        for (int i = 0; i < data.length; i++) {
            DataChangeLog item = new DataChangeLog();

            String result = subResults[i];
            if (result != null) {
                // Not very OO
                Document doc = Util.parse(result);

                String userName = Util.getFirstTextNode(doc, "/Update/UserName");
                String source = Util.getFirstTextNode(doc, "/Update/Source");
                String timeInMillis = Util.getFirstTextNode(doc, "/Update/TimeInMillis");
                String operationType = Util.getFirstTextNode(doc, "/Update/OperationType");
                String revisionID = Util.getFirstTextNode(doc, "/Update/RevisionID");
                String dataCluster = Util.getFirstTextNode(doc, "/Update/DataCluster");
                String dataModel = Util.getFirstTextNode(doc, "/Update/DataModel");
                String concept = Util.getFirstTextNode(doc, "/Update/Concept");
                String key = Util.getFirstTextNode(doc, "/Update/Key");

                item.setUserName(userName);
                item.setSource(source);
                item.setTimeInMillis(timeInMillis);
                item.setOperationType(operationType);
                item.setRevisionID(revisionID);
                item.setDataCluster(dataCluster);
                item.setDataModel(dataModel);
                item.setConcept(concept);
                item.setKey(key);
                // item.setXmlSource(result);
                item.setIds(Util.joinStrings(new String[] { source, timeInMillis }, "."));

            }

            data[i] = item;
        }

        if (data.length <= 0) {
            listRange.setData(new DataChangeLog[0]);
            listRange.setTotalSize(0);
            return listRange;
        }

        listRange.setData(data);
        listRange.setTotalSize(totalSize);
        return listRange;
    }

    public boolean checkDCAndDM(String dataCluster, String dataModel) {
        return Util.checkDCAndDM(dataCluster, dataModel);
    }
}
