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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.welcomeportal.client.WelcomePortal;
import org.talend.mdm.webapp.welcomeportal.client.i18n.MessagesFactory;
import org.talend.mdm.webapp.welcomeportal.client.mvc.EntityConfigModel;
import org.talend.mdm.webapp.welcomeportal.client.rest.StatisticsRestServiceHandler;

import com.extjs.gxt.ui.client.widget.custom.Portal;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.HTML;
import com.googlecode.gflot.client.DataPoint;
import com.googlecode.gflot.client.PlotModel;
import com.googlecode.gflot.client.Series;
import com.googlecode.gflot.client.SeriesHandler;
import com.googlecode.gflot.client.options.GlobalSeriesOptions;
import com.googlecode.gflot.client.options.GridOptions;
import com.googlecode.gflot.client.options.LegendOptions;
import com.googlecode.gflot.client.options.PieSeriesOptions;
import com.googlecode.gflot.client.options.PieSeriesOptions.Label.Background;
import com.googlecode.gflot.client.options.PieSeriesOptions.Label.Formatter;
import com.googlecode.gflot.client.options.PlotOptions;

public class DataChart extends ChartPortlet {

    public DataChart(Portal portal) {
        super(WelcomePortal.CHART_DATA, portal);

        String setting = portalConfigs.getChartSetting(portletName);
        if (setting != null) {
            configModel = new EntityConfigModel(startedAsOn, setting);
        } else {
            configModel = new EntityConfigModel(startedAsOn);
        }
        initConfigSettings();

        initChart();
    }

    private void initChart() {
        String noDCAlertPrefix = "<span id=\"licenseAlert\" style=\"padding-right:8px;cursor: pointer;\" class=\"labelStyle\" title=\"" //$NON-NLS-1$
                + MessagesFactory.getMessages().alerts_title() + "\">"; //$NON-NLS-1$
        final String alertIcon = "<IMG SRC=\"/talendmdm/secure/img/genericUI/alert-icon.png\"/>&nbsp;"; //$NON-NLS-1$
        final HTML alertHtml = new HTML();
        final StringBuilder noDCAlertMsg = new StringBuilder(noDCAlertPrefix);

        service.getCurrentDataContainer(new SessionAwareAsyncCallback<String>() {

            @Override
            public void onSuccess(String dataContainer) {

                if (dataContainer != null) {
                    dc = dataContainer;
                    dataContainerChanged = false;

                    StatisticsRestServiceHandler.getInstance().getContainerDataStats(dataContainer, configModel,
                            new SessionAwareAsyncCallback<JSONArray>() {

                                @Override
                                public void onSuccess(JSONArray jsonArray) {
                                    chartData = parseJSONData(jsonArray);
                                    initAndShow();
                                }
                            });
                } else {
                    noDCAlertMsg.append(alertIcon);
                    noDCAlertMsg.append(MessagesFactory.getMessages().no_container());
                    noDCAlertMsg.append("</span>"); //$NON-NLS-1$
                    alertHtml.setHTML(noDCAlertMsg.toString());
                    set.add(alertHtml);
                    set.layout(true);
                }
            }

        });
    }

    @Override
    public void refresh() {

        service.getCurrentDataContainer(new SessionAwareAsyncCallback<String>() {

            @Override
            public void onSuccess(String dataContainer) {

                dataContainerChanged = !dc.equals(dataContainer);
                dc = dataContainer;

                StatisticsRestServiceHandler.getInstance().getContainerDataStats(dataContainer, configModel,
                        new SessionAwareAsyncCallback<JSONArray>() {

                            @Override
                            public void onSuccess(JSONArray jsonArray) {
                                Map<String, Object> newData = parseJSONData(jsonArray);
                                if (plot == null) {
                                    chartData = newData;
                                    initAndShow();
                                } else {
                                    doRefreshWith(newData);
                                }
                            }
                        });
            }
        });

    }

    @Override
    protected void initPlot() {
        super.initPlot();
        PlotModel model = plot.getModel();
        PlotOptions plotOptions = plot.getOptions();
        final NumberFormat formatter = NumberFormat.getFormat("0.#"); //$NON-NLS-1$
        Set<String> entityNames = chartData.keySet();
        List<String> entityNamesSorted = sort(entityNames);

        plotOptions.setGlobalSeriesOptions(GlobalSeriesOptions.create().setPieSeriesOptions(
                PieSeriesOptions
                        .create()
                        .setShow(true)
                        .setRadius(1)
                        .setInnerRadius(0.2)
                        .setLabel(
                                com.googlecode.gflot.client.options.PieSeriesOptions.Label.create().setShow(true)
                                        .setRadius(3d / 4d).setBackground(Background.create().setOpacity(0.8)).setThreshold(0.05)
                                        .setFormatter(new Formatter() {

                                            @Override
                                            public String format(String label, Series series) {
                                                return "<div style=\"font-size:8pt;text-align:center;padding:2px;color:white;\">" //$NON-NLS-1$
                                                        + label + "<br/>" + formatter.format(series.getData().getY(0)) + " / " //$NON-NLS-1$//$NON-NLS-2$
                                                        + formatter.format(series.getPercent()) + "%</div>"; //$NON-NLS-1$
                                            }
                                        }))));
        plotOptions.setLegendOptions(LegendOptions.create().setShow(false));
        plotOptions.setGridOptions(GridOptions.create().setHoverable(true));

        // create series and add data
        for (String entityName : entityNamesSorted) {
            SeriesHandler seriesEntity = model.addSeries(Series.of(entityName));
            seriesEntity.add(DataPoint.of(entityName, (Integer) chartData.get(entityName)));
        }

    }

    @Override
    protected void updatePlot() {
        PlotModel model = plot.getModel();
        Set<String> entityNames = chartData.keySet();
        List<String> entityNamesSorted = sort(entityNames);

        if (!dataContainerChanged && !configSettingChanged) {
            // keep SeriesHandler, just clean their data
            model.clear();
            List<? extends SeriesHandler> series = model.getHandlers();

            // new entities maybe added to DM and needs to be reflected on the new chart
            Map<String, SeriesHandler> seriesMap = new HashMap<String, SeriesHandler>(entityNamesSorted.size());
            int count = 0;
            int prevTotal = series.size();
            for (String entityName : entityNamesSorted) {
                if (count < prevTotal) {
                    seriesMap.put(entityName, series.get(count++));
                } else {// for newly added enities in the same dm
                    seriesMap.put(entityName, model.addSeries(Series.of(entityName)));
                }
            }

            for (String entityName : entityNamesSorted) {
                seriesMap.get(entityName).add(DataPoint.of(entityName, (Integer) chartData.get(entityName)));
            }
        } else {
            // switched to diff dm/updated config, them dump and rebuild all Series
            model.removeAllSeries();
            for (String entityName : entityNamesSorted) {
                SeriesHandler seriesEntity = model.addSeries(Series.of(entityName));
                seriesEntity.add(DataPoint.of(entityName, (Integer) chartData.get(entityName)));
            }
        }
    }

    @Override
    protected Map<String, Object> parseJSONData(JSONArray jsonArray) {
        Map<String, Object> entityDataNew = new HashMap<String, Object>(jsonArray.size());
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.get(i).isObject();
            String name = jsonObject.keySet().iterator().next();
            int value = new Double(jsonObject.get(name).isNumber().doubleValue()).intValue();
            entityDataNew.put(name, value);
        }

        return entityDataNew;
    }

    @Override
    protected boolean isDifferentFrom(Map<String, Object> newData) {
        if (chartData.size() != newData.size()) {
            return true;
        } else {
            for (String entityName : chartData.keySet()) {
                if (!chartData.get(entityName).equals(newData.get(entityName))) {
                    return true;
                }
            }
        }
        return false;
    }

}
