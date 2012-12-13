// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.webapp.stagingareacontrol.client;

import org.talend.mdm.webapp.stagingareacontrol.client.i18n.MessagesFactory;

import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;


public class GenerateContainer {

    private static ContentPanel instance;

    public static void generateContentPanel() {
        if (instance != null) {
            instance.removeFromParent();
        }

        instance = new ContentPanel();
        instance.setLayout(new FitLayout());
        instance.setId(StagingareaControl.STAGINGAREA_ID);
        instance.setHeading(MessagesFactory.getMessages().stagingarea_title());
        instance.setHeaderVisible(false);
        instance.setBodyBorder(false);
    }

    public static ContentPanel getContentPanel() {
        return instance;
    }
}