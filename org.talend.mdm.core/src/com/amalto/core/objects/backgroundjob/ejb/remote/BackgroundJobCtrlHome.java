/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.objects.backgroundjob.ejb.remote;

/**
 * Home interface for BackgroundJobCtrl.
 * @xdoclet-generated at 3-09-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface BackgroundJobCtrlHome
   extends javax.ejb.EJBHome
{
   public static final String COMP_NAME="java:comp/env/ejb/BackgroundJobCtrl";
   public static final String JNDI_NAME="amalto/remote/core/backgroundjobctrl";

   public com.amalto.core.objects.backgroundjob.ejb.remote.BackgroundJobCtrl create()
      throws javax.ejb.CreateException,java.rmi.RemoteException;

}
