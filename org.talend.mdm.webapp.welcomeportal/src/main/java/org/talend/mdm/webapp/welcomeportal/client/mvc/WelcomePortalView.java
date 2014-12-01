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
package org.talend.mdm.webapp.welcomeportal.client.mvc;

import java.util.HashMap;
import java.util.Map;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.welcomeportal.client.GenerateContainer;
import org.talend.mdm.webapp.welcomeportal.client.MainFramePanel;
import org.talend.mdm.webapp.welcomeportal.client.WelcomePortal;
import org.talend.mdm.webapp.welcomeportal.client.WelcomePortalEvents;
import org.talend.mdm.webapp.welcomeportal.client.WelcomePortalServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.mvc.AppEvent;
import com.extjs.gxt.ui.client.mvc.Controller;
import com.extjs.gxt.ui.client.mvc.View;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.custom.Portal;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;

/**
 * DOC Administrator class global comment. Detailled comment
 */

public class WelcomePortalView extends View {

    private static final String USING_DEFAULT_COLUMN_NUM = "defaultColNum"; //$NON-NLS-1$

    private static final String CHARTS_ENABLED = "chartsOn"; //$NON-NLS-1$

    private static final int DEFAULT_COLUMN_NUM = 3;

    private static final int ALTERNATIVE_COLUMN_NUM = 2;

    private int numColumns;

    private boolean chartsOn;

    private Portal portal;

    private boolean chartsSwitcherUpdated;

    private boolean isEnterprise;

    private static PortalProperties portalConfigCache;

    private WelcomePortalServiceAsync service = (WelcomePortalServiceAsync) Registry.get(WelcomePortal.WELCOMEPORTAL_SERVICE);

    public WelcomePortalView(Controller controller) {
        super(controller);
    }

    @Override
    protected void handleEvent(AppEvent event) {
        if (event.getType() == WelcomePortalEvents.InitFrame) {
            onInitFrame(event);
        } else if (event.getType() == WelcomePortalEvents.RefreshPortlet) {
            onRefreshPortlet();
        } else if (event.getType() == WelcomePortalEvents.RefreshPortal) {
            onRefreshPortal(event);
        } else if (event.getType() == WelcomePortalEvents.RevertRefreshPortal) {
            onRevertRefreshPortal(event);
        }
    }

    private void onRefreshPortal(AppEvent event) {
        String dataString = event.getData();

        Map<String, Boolean> parsedUserConfig = parseConfig(dataString);
        Boolean defaultColConfig = parsedUserConfig.get(USING_DEFAULT_COLUMN_NUM);
        ContentPanel container = GenerateContainer.getContentPanel();
        if (isEnterprise) {
            chartsSwitcherUpdated = !(chartsOn == parsedUserConfig.get(CHARTS_ENABLED));

            if ((!chartsSwitcherUpdated) && (!switchColumn(defaultColConfig))) {
                ((MainFramePanel) (container.getItems().get(0))).refresh(parsedUserConfig);
            } else {// for switching to diff column number or chartsSwitherUpdated
                updatePortal(parsedUserConfig);
            }
        } else {
            if (!switchColumn(defaultColConfig)) {
                ((MainFramePanel) (container.getItems().get(0))).refresh(parsedUserConfig);
            } else {
                updatePortal(parsedUserConfig);
            }
        }
    }

    private boolean switchColumn(Boolean defaultColConfig) {
        if (isEnterprise) {
            return (defaultColConfig && numColumns == ALTERNATIVE_COLUMN_NUM)
                    || (!defaultColConfig && numColumns == DEFAULT_COLUMN_NUM);
        } else {
            return (defaultColConfig && numColumns == DEFAULT_COLUMN_NUM)
                    || (!defaultColConfig && numColumns == ALTERNATIVE_COLUMN_NUM);
        }
    }

    private void onRevertRefreshPortal(AppEvent event) {

        if (Log.isInfoEnabled()) {
            Log.info("Revert RefreshPortal due to saving to db failed... ");//$NON-NLS-1$
        }

        removePortal();

        portalConfigCache = (PortalProperties) event.getData();

        numColumns = portalConfigCache.getColumnNum();

        portal = new MainFramePanel(numColumns, portalConfigCache, isEnterprise);
        ContentPanel container = GenerateContainer.getContentPanel();
        container.add(portal);
        container.layout(true);
    }

    private Map<String, Boolean> parseConfig(String dataString) {
        Map<String, Boolean> config = new HashMap<String, Boolean>();
        String temp = dataString.substring(1, dataString.length() - 1);
        String[] nameValues = temp.split(","); //$NON-NLS-1$
        String name;
        Boolean visible;
        String[] nameValuePair;
        for (String nameValue : nameValues) {
            nameValuePair = nameValue.split("="); //$NON-NLS-1$
            name = nameValuePair[0].trim();
            visible = Boolean.parseBoolean(nameValuePair[1]);
            config.put(name, visible);
        }
        return config;
    }

    private void updatePortal(final Map<String, Boolean> userConfig) {
        if (Log.isInfoEnabled()) {
            Log.info("Refresh with different column number... ");//$NON-NLS-1$
        }

        removePortal();

        numColumns = getColumNum(userConfig.get(USING_DEFAULT_COLUMN_NUM));

        if (isEnterprise) {
            chartsOn = userConfig.get(CHARTS_ENABLED);
        }

        portal = new MainFramePanel(numColumns, portalConfigCache, userConfig, isEnterprise);
        ContentPanel container = GenerateContainer.getContentPanel();
        container.add(portal);
        container.layout(true);
    }

    public void removePortal() {
        ((MainFramePanel) portal).stopAutoRefresh();
        ((MainFramePanel) portal).removeAllPortlets();
        GenerateContainer.getContentPanel().remove(portal);
    }

    private int getColumNum(boolean usingDefaultCol) {
        if (usingDefaultCol) {
            return isEnterprise ? DEFAULT_COLUMN_NUM : ALTERNATIVE_COLUMN_NUM;
        } else {
            return isEnterprise ? ALTERNATIVE_COLUMN_NUM : DEFAULT_COLUMN_NUM;
        }

    }

    private void onRefreshPortlet() {
        ContentPanel container = GenerateContainer.getContentPanel();
        ((MainFramePanel) (container.getItems().get(0))).refreshPortlets();
    }

    private void onInitFrame(AppEvent event) {
        if (Log.isInfoEnabled()) {
            Log.info("Init frame... ");//$NON-NLS-1$
        }
        portalConfigCache = (PortalProperties) event.getData();
        final ContentPanel container = GenerateContainer.getContentPanel();
        // container.setHeaderVisible(false);
        container.setLayout(new FitLayout());
        container.setStyleAttribute("height", "100%");//$NON-NLS-1$ //$NON-NLS-2$

        service.isEnterpriseVersion(new SessionAwareAsyncCallback<Boolean>() {

            @Override
            public void onSuccess(Boolean isEE) {
                isEnterprise = isEE;
                Integer numColumnsObj = portalConfigCache.getColumnNum();
                numColumns = (numColumnsObj == null) ? getDefaultColumNum() : numColumnsObj;

                if (isEnterprise) {
                    Boolean chartsOnObj = portalConfigCache.getChartsOn();
                    chartsOn = (chartsOnObj == null) ? true : chartsOnObj;
                }

                portal = new MainFramePanel(numColumns, portalConfigCache, isEnterprise);
                container.add(portal);
                container.layout(true);
            }
        });

    }

    private Integer getDefaultColumNum() {
        return isEnterprise ? DEFAULT_COLUMN_NUM : ALTERNATIVE_COLUMN_NUM;
    }
}
