/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.servlet;

import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.amalto.core.ejb.local.XmlServerSLWrapperLocal;
import com.amalto.core.jobox.properties.ThreadIsolatedSystemProperties;
import com.amalto.core.objects.routing.v2.ejb.RoutingEngineV2POJO;
import com.amalto.core.save.generator.AutoIdGenerator;
import com.amalto.core.save.generator.AutoIncrementGenerator;
import com.amalto.core.server.ConfigurationInfo;
import com.amalto.core.server.XmlServer;
import com.amalto.core.util.Util;
import com.amalto.core.util.XtentisException;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;

/**
 *
 */
public class ContextListener implements ServletContextListener {

    private static final Logger log = Logger.getLogger(ContextListener.class);

    private Properties previousSystemProperties;

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        if (log.isDebugEnabled()) {
            log.debug("contextInitialized()"); //$NON-NLS-1$
        }
        // TMDM-2933: ThreadIsolatedSystemProperties allows threads to get different system properties when needed.
        previousSystemProperties = System.getProperties();
        System.setProperties(ThreadIsolatedSystemProperties.getInstance());
        if (log.isDebugEnabled()) {
            log.debug("Enabled system properties isolation for threads."); //$NON-NLS-1$
        }
        // AutoUpgrade
        try {
            ConfigurationInfo configurationInfo = Util.getConfigurationInfoCtrlLocal();
            configurationInfo.autoUpgradeInBackground();
        } catch (Throwable e) {
            log.error("Unable to perform Auto Upgrade", e);  //$NON-NLS-1$
        }
    }

    /**
     * <p>
     * Called by J2EE container when web application is being un-deployed. This is usually a good place to
     * clean up the resources such as database.
     * </p>
     *
     * @param servletContextEvent Parameter to access servlet specific info (provided by J2EE container)
     */
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // Save auto increment state
        try {
            XmlServerSLWrapperLocal server = Util.getXmlServerCtrlLocal();
            server.start(XSystemObjects.DC_CONF.getName());
            AutoIncrementGenerator.get().saveState(server);
            server.commit(XSystemObjects.DC_CONF.getName());
        } catch (XtentisException e) {
            // Don't re-throw exception in case this breaks normal J2EE container operation.
            log.error("Unable to save auto increment counter state.", e);   //$NON-NLS-1$
        }

        // Close database
        log.info("Closing database");  //$NON-NLS-1$
        try {
            XmlServerSLWrapperLocal server = Util.getXmlServerCtrlLocal();
            server.close();
        } catch (Throwable e) {
            // Don't re-throw exception in case this breaks normal J2EE container operation.
            log.error("Error during database shutdown", e);   //$NON-NLS-1$
        }

        log.info("Shutdown detected. Stopping the Routing Engine");
        try {
            RoutingEngineV2POJO.getInstance().setStatus(RoutingEngineV2POJO.STOPPED);
        } catch (Throwable e) {
            // Don't re-throw exception in case this breaks normal J2EE container operation.
            log.error("Error during routing engine shutdown", e); //$NON-NLS-1$
        }

        // Server is shutting down, replace the system properties with the ones backed up at start up.
        System.setProperties(previousSystemProperties);


    }
}
