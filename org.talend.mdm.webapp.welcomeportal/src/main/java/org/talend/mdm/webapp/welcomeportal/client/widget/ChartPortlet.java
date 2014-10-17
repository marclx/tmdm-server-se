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
package org.talend.mdm.webapp.welcomeportal.client.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.welcomeportal.client.mvc.ConfigModel;
import org.talend.mdm.webapp.welcomeportal.client.mvc.PortalProperties;
import org.talend.mdm.webapp.welcomeportal.client.resources.icon.Icons;
import org.talend.mdm.webapp.welcomeportal.client.widget.PortletConfigDialog.PortletConfigListener;

import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.custom.Portal;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.googlecode.gflot.client.SimplePlot;
import com.googlecode.gflot.client.options.GlobalSeriesOptions;
import com.googlecode.gflot.client.options.PlotOptions;

public abstract class ChartPortlet extends BasePortlet {

    protected SimplePlot plot;

    protected Map<String, Object> chartData;

    protected String dc;

    protected boolean dataContainerChanged;

    private int plotWidth;

    private int plotHeight;

    public ChartPortlet(String name, Portal portal) {
        super(name, portal);

    }

    @Override
    protected PortletConfigListener initConfigListener() {
        PortletConfigListener configListener = new PortletConfigListener() {

            @Override
            public void onConfigUpdate(final ConfigModel configModelFromUser) {
                boolean autoUpdated;
                final ConfigModel configModelOrig = configModel;
                if (!configModel.equals(configModelFromUser)) {
                    autoUpdated = !configModel.isAutoRefresh().equals(configModelFromUser.isAutoRefresh());
                    configSettingChanged = !configModel.getSetting().equals(configModelFromUser.getSetting());
                    configModel = configModelFromUser;
                    if (!autoUpdated) { // only setting changed
                        service.savePortalConfig(PortalProperties.KEY_CHART_SETTINGS, portletName, configModel.getSetting(),
                                new SessionAwareAsyncCallback<Void>() {

                                    @Override
                                    public void onSuccess(Void result) {
                                        portalConfigs.add(PortalProperties.KEY_CHART_SETTINGS, portletName,
                                                configModel.getSetting());
                                        if (!configModelFromUser.isAutoRefresh()) {
                                            refresh();
                                        } // else->auto=true, so no need to call redundant refresh()
                                        return;
                                    }

                                    @Override
                                    protected void doOnFailure(Throwable caught) {
                                        super.doOnFailure(caught);
                                        configModel = configModelOrig; // if db update fails, revert to original on UI
                                    }
                                });
                    } else {
                        if (!configSettingChanged) {
                            service.savePortalConfig(PortalProperties.KEY_AUTO_ONOFFS, portletName, configModel.isAutoRefresh()
                                    .toString(), new SessionAwareAsyncCallback<Void>() {

                                @Override
                                public void onSuccess(Void result) {
                                    portalConfigs.add(PortalProperties.KEY_AUTO_ONOFFS, portletName, configModel.isAutoRefresh()
                                            .toString());
                                    ChartPortlet.this.autoRefresh(configModel.isAutoRefresh());
                                }

                                @Override
                                protected void doOnFailure(Throwable caught) {
                                    super.doOnFailure(caught);
                                    // revert to original auto & setting on UI
                                    configModel = configModelOrig;
                                }
                            });

                        } else {
                            List<String> autoAndSetting = Arrays.asList(configModel.isAutoRefresh().toString(),
                                    configModel.getSetting());

                            service.savePortalConfigAutoAndSetting(portletName, autoAndSetting,
                                    new SessionAwareAsyncCallback<Void>() {

                                        @Override
                                        public void onSuccess(Void result1) {
                                            portalConfigs.add(PortalProperties.KEY_AUTO_ONOFFS, portletName, configModel
                                                    .isAutoRefresh().toString());
                                            portalConfigs.add(PortalProperties.KEY_CHART_SETTINGS, portletName,
                                                    configModel.getSetting());
                                            ChartPortlet.this.autoRefresh(configModel.isAutoRefresh());
                                            // user turn off autorefresh, call refresh() to reflect charts with new
                                            // setting
                                            if (!configModel.isAutoRefresh()) {
                                                refresh();
                                            }
                                        }

                                        @Override
                                        protected void doOnFailure(Throwable caught) {
                                            super.doOnFailure(caught);
                                            // revert to original auto & setting on UI
                                            configModel = configModelOrig;
                                        }
                                    });

                        }
                    }
                } else {
                    configSettingChanged = false;
                    return;
                }
            }
        };

        return configListener;
    }

    protected void initPlot() {
        PlotOptions plotOptions = PlotOptions.create();
        plotOptions.setGlobalSeriesOptions(GlobalSeriesOptions.create());
        plot = new SimplePlot(plotOptions);
        plotWidth = this.getWidth() - 50;
        plotHeight = this.getWidth() - 60;
        plot.setWidth(plotWidth);
        plot.setHeight(plotHeight);
    }

    protected void resizePlot() {
        plot.setWidth(plotWidth);
        plot.setHeight(plotHeight);
        plot.redraw();
        set.layout(true);
    }

    protected void refreshPlot() {
        updatePlot();
        plot.redraw();
        set.layout(true);
    }

    protected void initAndShow() {
        initPlot();
        set.removeAll();
        set.add(plot);
        set.layout(true);
        this.autoRefresh(configModel.isAutoRefresh());

        this.addListener(Events.Resize, new Listener<BaseEvent>() {

            @Override
            public void handleEvent(BaseEvent be) {
                plotWidth = ChartPortlet.this.getWidth() - 50;
                plotHeight = plotWidth - 10;
                resizePlot();
            }
        });
    }

    protected void doRefreshWith(Map<String, Object> newData) {
        if (dataContainerChanged || configSettingChanged || isDifferentFrom(newData)) {
            chartData = newData;
            refreshPlot();
            if (configSettingChanged) {
                configSettingChanged = !configSettingChanged;
            }
        }
    }

    protected List<String> sort(Set<String> names) {
        List<String> appnamesSorted = new ArrayList<String>(names);
        Collections.sort(appnamesSorted);

        return appnamesSorted;
    }

    @Override
    protected void setIcon() {
        this.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.chart()));
    }

    abstract Map<String, Object> parseJSONData(JSONArray jsonArray);

    abstract protected void updatePlot();

    abstract protected boolean isDifferentFrom(Map<String, Object> newData);
}
