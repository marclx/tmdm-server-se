/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.service.smtp.ejb.local;

/**
 * Local home interface for Smtp.
 * @xdoclet-generated at 29-09-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface SmtpLocalHome
   extends javax.ejb.EJBLocalHome
{
   public static final String COMP_NAME="java:comp/env/ejb/SmtpLocal";
   public static final String JNDI_NAME="amalto/local/service/smtp";

   public com.amalto.service.smtp.ejb.local.SmtpLocal create()
      throws javax.ejb.CreateException;

}
