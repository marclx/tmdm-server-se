/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.objects.datamodel.ejb.local;

/**
 * Local home interface for DataModelCtrl.
 * @xdoclet-generated
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface DataModelCtrlLocalHome
   extends javax.ejb.EJBLocalHome
{
   public static final String COMP_NAME="java:comp/env/ejb/DataModelCtrlLocal";
   public static final String JNDI_NAME="amalto/local/core/datamodelctrl";

   public com.amalto.core.objects.datamodel.ejb.local.DataModelCtrlLocal create()
      throws javax.ejb.CreateException;

}