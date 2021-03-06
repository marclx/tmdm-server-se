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
package org.talend.mdm.webapp.browserecords.server.util;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;

import junit.framework.TestCase;

@SuppressWarnings("nls")
public class UploadUtilTest  extends TestCase {
    
    HashMap<String, Boolean> visibleMap = null;
    String mandatoryField = "Id@Name@Age";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        visibleMap = new HashMap<String, Boolean>();
        visibleMap.put("Id", true);
        visibleMap.put("Name", false);
        visibleMap.put("Age", true);        
    }

    public void testChechMandatoryField(){
        Set<String>  mandatorySet = UploadUtil.chechMandatoryField(mandatoryField, visibleMap.keySet());
        assertEquals(mandatorySet.size(), 0);
    }
    
    public void testGetFieldName(){
        assertEquals(UploadUtil.getFieldName("Name=true"), "Name");
    }
  
    public void testGetFieldVisible(){
        assertTrue(UploadUtil.getFieldVisible("Name=true"));
    }    
    
    public void testGetDefaultHeader(String header){
        UploadUtil.getDefaultHeader(header);
        String[] headerArray = UploadUtil.getDefaultHeader(header);        
        assertEquals(headerArray.length, 3);
        assertEquals(headerArray[0], "Id");
        assertEquals(headerArray[1], "Name");
        assertEquals(headerArray[2], "Age");
    }
    
    public void testGetRootCause(){
        RemoteException romoteException = new RemoteException("RemoteException Cause");
        ServletException servletException = new ServletException("ServletException Cause",romoteException);
        Exception exception = new Exception("Exception Cause",servletException);
        assertEquals(UploadUtil.getRootCause(exception),"RemoteException Cause");
    }
        
    public void testIsViewableXpathValid(){
        Set<String> viewableXpathsSet = new HashSet<String>();
        viewableXpathsSet.add("Product/Id");
        viewableXpathsSet.add("Product/Name");
        viewableXpathsSet.add("Product/Description");
        viewableXpathsSet.add("Product/Price");
        viewableXpathsSet.add("Product/Family");
        viewableXpathsSet.add("ProductFamily/Name");
        String concept = "Product";
        assertFalse(UploadUtil.isViewableXpathValid(viewableXpathsSet, concept));
        
        concept = "ProductFamily";
        assertFalse(UploadUtil.isViewableXpathValid(viewableXpathsSet, concept));
        
        viewableXpathsSet = new HashSet<String>();
        viewableXpathsSet.add("Product/Id");
        viewableXpathsSet.add("Product/Name");
        viewableXpathsSet.add("Product/Description");
        viewableXpathsSet.add("Product/Price");
        viewableXpathsSet.add("Product/Family");
        concept = "Product";
        assertTrue(UploadUtil.isViewableXpathValid(viewableXpathsSet, concept));
    }
}
