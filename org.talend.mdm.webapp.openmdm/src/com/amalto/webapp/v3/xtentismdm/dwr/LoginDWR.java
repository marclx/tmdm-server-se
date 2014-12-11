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
package com.amalto.webapp.v3.xtentismdm.dwr;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.amalto.webapp.core.util.Util;
import com.amalto.webapp.core.util.XtentisWebappException;
import com.amalto.core.webservice.WSGetUniversePKs;
import com.amalto.core.webservice.WSUniversePK;
import com.amalto.core.webservice.WSUniversePKArray;
import com.amalto.core.webservice.XtentisPort;

public class LoginDWR {

    private static final String SESSION_TIME_OUT = "sessionTimeOut"; //$NON-NLS-1$

    private static final String HEAD = "HEAD"; //$NON-NLS-1$

    private static final String REGEX = ".*"; //$NON-NLS-1$

    public boolean isTimeOut() {
        boolean timeout = false;
        WebContext ctx = WebContextFactory.get();
        return ctx.getSession(false) == null || timeout;
    }

    public String[] getUniverseNames() throws XtentisWebappException {
        Set<String> universeNames = new HashSet<String>();
        universeNames.add(HEAD);
        try {
            if (com.amalto.core.util.Util.isEnterprise()) {
                XtentisPort port = Util.getPort(null, null);
                WSUniversePKArray pks = port.getUniversePKs(new WSGetUniversePKs(REGEX));
                if (pks != null) {
                    WSUniversePK[] wsUniversePKs = pks.getWsUniversePK();
                    if (wsUniversePKs != null && wsUniversePKs.length > 0) {
                        for (WSUniversePK wsUniversePK : wsUniversePKs) {
                            universeNames.add(wsUniversePK.getPk());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new XtentisWebappException(e);
        }
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession(false);
        if (session != null) {
            session.setAttribute(SESSION_TIME_OUT, session.getMaxInactiveInterval());
            session.setMaxInactiveInterval(-1);
        }
        return universeNames.toArray(new String[universeNames.size()]);
    }
}
