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
package org.talend.mdm.webapp.browserecords.client;

import org.talend.mdm.webapp.browserecords.client.model.QueryModel;
import org.talend.mdm.webapp.browserecords.client.widget.DownloadFilePanel;
import org.talend.mdm.webapp.browserecords.client.widget.UploadFileFormPanel;
import org.talend.mdm.webapp.browserecords.shared.ViewBean;

import com.extjs.gxt.ui.client.widget.Window;

public abstract class WidgetFactory {

    private static WidgetFactory instance;

    public static void initialize(WidgetFactory factoryImpl) {
        WidgetFactory.instance = factoryImpl;
    }

    public static WidgetFactory getInstance() {
        if (instance == null) {
            instance = new DefaultWidgetFactoryImpl();
        }
        return instance;
    }

    public abstract UploadFileFormPanel createUploadFileFormPanel(ViewBean viewBean, Window window);

    public abstract DownloadFilePanel createDownloadFilePanel(ViewBean viewBean, QueryModel queryModel, Window window);
}
