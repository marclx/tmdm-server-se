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
package org.talend.mdm.webapp.stagingareacontrol.client.i18n;

import com.google.gwt.core.client.GWT;

public class MessagesFactory {

    private static StagingareaMessages MESSAGES;

    private MessagesFactory() {
    }

    public static StagingareaMessages getMessages() {
        if (GWT.isClient()) {
            if (MESSAGES == null)
                MESSAGES = GWT.create(StagingareaMessages.class);
            return MESSAGES;
        }
        // Can't be called from server-side
        throw new IllegalStateException();
    }
}