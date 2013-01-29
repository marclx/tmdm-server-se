// ============================================================================
//
// Copyright (C) 2006-2013 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.webapp.journal.server.util;

import java.util.ArrayList;
import java.util.List;

import org.talend.mdm.commmon.util.webapp.XSystemObjects;
import org.talend.mdm.webapp.journal.shared.JournalSearchCriteria;

import com.amalto.webapp.util.webservices.WSDataClusterPK;
import com.amalto.webapp.util.webservices.WSGetItems;
import com.amalto.webapp.util.webservices.WSStringPredicate;
import com.amalto.webapp.util.webservices.WSWhereAnd;
import com.amalto.webapp.util.webservices.WSWhereCondition;
import com.amalto.webapp.util.webservices.WSWhereItem;
import com.amalto.webapp.util.webservices.WSWhereOperator;


/**
 * created by talend2 on 2013-1-29
 * Detailled comment
 *
 */
public class Util {
    
    public static final int TIME_OF_ONE_DAY = 24 * 3600 * 1000;
    
    public static List<WSWhereItem> buildWhereItems(JournalSearchCriteria criteria,boolean isBrowseRecord) {         
        
        List<WSWhereItem> whereItemList = new ArrayList<WSWhereItem>();
        if (criteria.getEntity() != null) {
            WSWhereCondition wc = new WSWhereCondition(
                    "Concept", isBrowseRecord ? WSWhereOperator.EQUALS : WSWhereOperator.CONTAINS, criteria.getEntity().trim(), WSStringPredicate.NONE, false); //$NON-NLS-1$
            WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
            whereItemList.add(wsWhereItem);
        }

        if (criteria.getKey() != null) {
            WSWhereCondition wc = new WSWhereCondition(
                    "Key", isBrowseRecord ? WSWhereOperator.EQUALS : WSWhereOperator.CONTAINS, criteria.getKey().trim(), WSStringPredicate.NONE, false); //$NON-NLS-1$
            WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
            whereItemList.add(wsWhereItem);
        }

        if (criteria.getSource() != null) {
            WSWhereCondition wc = new WSWhereCondition(
                    "Source", WSWhereOperator.EQUALS, criteria.getSource().trim(), WSStringPredicate.NONE, false); //$NON-NLS-1$
            WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
            whereItemList.add(wsWhereItem);
        }

        if (criteria.getOperationType() != null) {
            WSWhereCondition wc = new WSWhereCondition(
                    "OperationType", WSWhereOperator.EQUALS, criteria.getOperationType().trim(), WSStringPredicate.NONE, false); //$NON-NLS-1$
            WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
            whereItemList.add(wsWhereItem);
        }

        if (criteria.getStartDate() != null) {
            WSWhereCondition wc = new WSWhereCondition(
                    "TimeInMillis", WSWhereOperator.GREATER_THAN_OR_EQUAL, criteria.getStartDate().getTime() + "", WSStringPredicate.NONE, false); //$NON-NLS-1$ //$NON-NLS-2$
            WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
            whereItemList.add(wsWhereItem);
        }

        if (criteria.getEndDate() != null) {
            WSWhereCondition wc = new WSWhereCondition(
                    "TimeInMillis", WSWhereOperator.LOWER_THAN, (criteria.getEndDate().getTime() + TIME_OF_ONE_DAY) + "", WSStringPredicate.NONE, false); //$NON-NLS-1$ //$NON-NLS-2$
            WSWhereItem wsWhereItem = new WSWhereItem(wc, null, null);
            whereItemList.add(wsWhereItem);
        }
        return whereItemList;
    }
    
    public static WSGetItems buildGetItem(List<WSWhereItem> conditions,int start, int limit) {
        
        WSWhereItem wi;
        if (conditions.size() == 0) {
            wi = null;
        } else if (conditions.size() == 1) {
            wi = conditions.get(0);
        } else {
            WSWhereAnd and = new WSWhereAnd(conditions.toArray(new WSWhereItem[conditions.size()]));
            wi = new WSWhereItem(null, and, null);
        }

        WSGetItems item = new WSGetItems();
        item.setConceptName("Update"); //$NON-NLS-1$
        item.setWhereItem(wi);
        item.setTotalCountOnFirstResult(true);
        item.setSkip(start);
        item.setMaxItems(limit);
        item.setWsDataClusterPK(new WSDataClusterPK(XSystemObjects.DC_UPDATE_PREPORT.getName()));
        return item;
    }
}
