/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.service.jdbc.ejb.local;

/**
 * Local home interface for Jdbc.
 * @xdoclet-generated at 4-08-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface JdbcLocalHome
   extends javax.ejb.EJBLocalHome
{
   public static final String COMP_NAME="java:comp/env/ejb/JdbcLocal";
   public static final String JNDI_NAME="amalto/local/service/jdbc";

   public com.amalto.service.jdbc.ejb.local.JdbcLocal create()
      throws javax.ejb.CreateException;

}