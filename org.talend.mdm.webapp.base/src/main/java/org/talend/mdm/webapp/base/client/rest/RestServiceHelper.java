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
package org.talend.mdm.webapp.base.client.rest;

import org.restlet.client.Response;
import org.restlet.client.ext.json.JsonRepresentation;
import org.restlet.client.ext.xml.DomRepresentation;
import org.restlet.client.representation.EmptyRepresentation;

import com.google.gwt.core.client.GWT;

public class RestServiceHelper {

    public static final String BASE_URL = GWT.getHostPageBaseURL().replaceAll("/general/secure", ""); //$NON-NLS-1$ //$NON-NLS-2$

    private static boolean validResponse(Response response) {
        return response != null && response.getEntity() != null && !(response.getEntity() instanceof EmptyRepresentation);
    }

    public static DomRepresentation getDomRepresentationFromResponse(Response response) {
        if (validResponse(response)) {
            return new DomRepresentation(response.getEntity());
        } else {
            return null;
        }
    }

    public static JsonRepresentation getJsonRepresentationFromResponse(Response response) {
        if (validResponse(response)) {
            return new JsonRepresentation(response.getEntity());
        } else {
            return null;
        }
    }
}