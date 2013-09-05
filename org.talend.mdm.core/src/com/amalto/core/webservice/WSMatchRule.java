/*
 * Copyright (C) 2006-2013 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation （1.1.2_01，编译版 R40）
// Generated source version: 1.1.2

package com.amalto.core.webservice;


public class WSMatchRule {

    protected String configurationXmlContent;

    private WSMatchRulePK PK;

    public WSMatchRule() {
    }

    public WSMatchRule(String name, String configurationXmlContent) {
        this.PK = new WSMatchRulePK(name);
        this.configurationXmlContent = configurationXmlContent;
    }

    public String getConfigurationXmlContent() {
        return configurationXmlContent;
    }

    public void setConfigurationXmlContent(String configurationXmlContent) {
        this.configurationXmlContent = configurationXmlContent;
    }

    public WSMatchRulePK getPK() {
        return PK;
    }
}
