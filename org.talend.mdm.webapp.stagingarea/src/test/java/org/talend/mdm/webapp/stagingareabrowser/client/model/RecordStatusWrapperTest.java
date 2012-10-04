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
package org.talend.mdm.webapp.stagingareabrowser.client.model;

import com.google.gwt.junit.client.GWTTestCase;

@SuppressWarnings("nls")
public class RecordStatusWrapperTest extends GWTTestCase {

    public void testCheckStatus() {

        RecordStatusWrapper wrapper = new RecordStatusWrapper(RecordStatus.Model_Validation_Fail);
        assertTrue(!wrapper.isValid());
        assertEquals("statusInvalid", wrapper.getIcon().getName());
        assertEquals("red", wrapper.getColor());

        wrapper = new RecordStatusWrapper(RecordStatus.Validation_Success);
        assertTrue(wrapper.isValid());
        assertEquals("statusValid", wrapper.getIcon().getName());
        assertEquals("green", wrapper.getColor());

    }

    public String getModuleName() {
        return "org.talend.mdm.webapp.stagingareabrowser.StagingareaBrowse"; //$NON-NLS-1$
    }

}