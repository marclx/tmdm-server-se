/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.objects.routing.v2.ejb.local;

/**
 * Local home interface for RoutingOrderV2Ctrl.
 * @xdoclet-generated at 3-09-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface RoutingOrderV2CtrlLocalHome
   extends javax.ejb.EJBLocalHome
{
   public static final String COMP_NAME="java:comp/env/ejb/RoutingOrderV2CtrlLocal";
   public static final String JNDI_NAME="amalto/local/core/RoutingOrderCtrlV2";

   public com.amalto.core.objects.routing.v2.ejb.local.RoutingOrderV2CtrlLocal create()
      throws javax.ejb.CreateException;

}
