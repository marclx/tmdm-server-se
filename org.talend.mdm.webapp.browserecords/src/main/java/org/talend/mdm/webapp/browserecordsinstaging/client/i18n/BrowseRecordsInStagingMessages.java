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
package org.talend.mdm.webapp.browserecordsinstaging.client.i18n;

import com.google.gwt.i18n.client.Messages;

public interface BrowseRecordsInStagingMessages extends Messages {

    String staging_browse_record_title();

    String source();

    String status();

    String error();

    String match_group();

    String mark_as_deleted();

    String select_mark_item_record();

    String mark_deleted_confirm();

    String status_000();

    String status_201(String dataCluser);

    String status_202(String dataContainer);

    String status_203(String dataCluser);

    String status_204(String dataCluser);

    String status_205();

    String status_206();

    String status_401(String dataCluser);

    String status_402(String dataCluser);

    String status_403();

    String status_404();

    String status_405();

    String status_format();
}
