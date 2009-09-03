/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.objects.synchronization.ejb.remote;

/**
 * Remote interface for SynchronizationItemCtrl.
 * @xdoclet-generated at 3-09-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface SynchronizationItemCtrl
   extends javax.ejb.EJBObject
{
   /**
    * Creates or updates a SynchronizationItem
    * @throws XtentisException
    */
   public com.amalto.core.objects.synchronization.ejb.SynchronizationItemPOJOPK putSynchronizationItem( com.amalto.core.objects.synchronization.ejb.SynchronizationItemPOJO synchronizationItem )
      throws com.amalto.core.util.XtentisException, java.rmi.RemoteException;

   /**
    * Get SynchronizationItem
    * @throws XtentisException
    */
   public com.amalto.core.objects.synchronization.ejb.SynchronizationItemPOJO getSynchronizationItem( com.amalto.core.objects.synchronization.ejb.SynchronizationItemPOJOPK pk )
      throws com.amalto.core.util.XtentisException, java.rmi.RemoteException;

   /**
    * Get a SynchronizationItem - no exception is thrown: returns null if not found
    * @throws XtentisException
    */
   public com.amalto.core.objects.synchronization.ejb.SynchronizationItemPOJO existsSynchronizationItem( com.amalto.core.objects.synchronization.ejb.SynchronizationItemPOJOPK pk )
      throws com.amalto.core.util.XtentisException, java.rmi.RemoteException;

   /**
    * Remove an item
    * @throws XtentisException
    */
   public com.amalto.core.objects.synchronization.ejb.SynchronizationItemPOJOPK removeSynchronizationItem( com.amalto.core.objects.synchronization.ejb.SynchronizationItemPOJOPK pk )
      throws com.amalto.core.util.XtentisException, java.rmi.RemoteException;

   /**
    * Retrieve all SynchronizationItem PKS
    * @throws XtentisException
    */
   public java.util.Collection getSynchronizationItemPKs( java.lang.String regex )
      throws com.amalto.core.util.XtentisException, java.rmi.RemoteException;

   /**
    * Get a SynchronizationItem - no exception is thrown: returns null if not found
    * @throws XtentisException
    */
   public com.amalto.core.objects.synchronization.ejb.SynchronizationItemPOJO resolveSynchronization( com.amalto.core.objects.synchronization.ejb.SynchronizationItemPOJOPK pk,java.lang.String resolvedProjection )
      throws com.amalto.core.util.XtentisException, java.rmi.RemoteException;

}
