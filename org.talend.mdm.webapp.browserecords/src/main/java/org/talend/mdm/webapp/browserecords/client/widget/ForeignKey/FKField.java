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
package org.talend.mdm.webapp.browserecords.client.widget.ForeignKey;

import java.util.List;

import org.talend.mdm.webapp.base.client.model.ForeignKeyBean;

public interface FKField {

    public boolean isStaging();

    public String getForeignKey();

    public List<String> getForeignKeyInfo();

    public void setValue(ForeignKeyBean bean);
    
    public void setSuperValue(ForeignKeyBean bean);
}
