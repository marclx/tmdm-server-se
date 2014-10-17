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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.welcomeportal.client.WelcomePortal;
import org.talend.mdm.webapp.welcomeportal.client.i18n.MessagesFactory;
import org.talend.mdm.webapp.welcomeportal.client.mvc.TimeframeConfigModel;
import org.talend.mdm.webapp.welcomeportal.client.rest.StatisticsRestServiceHandler;

import com.extjs.gxt.ui.client.widget.custom.Portal;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.googlecode.gflot.client.DataPoint;
import com.googlecode.gflot.client.PlotModel;
import com.googlecode.gflot.client.Series;
import com.googlecode.gflot.client.SeriesHandler;
import com.googlecode.gflot.client.options.AxesOptions;
import com.googlecode.gflot.client.options.AxisOptions;
import com.googlecode.gflot.client.options.BarSeriesOptions;
import com.googlecode.gflot.client.options.BarSeriesOptions.BarAlignment;
import com.googlecode.gflot.client.options.CategoriesAxisOptions;
import com.googlecode.gflot.client.options.GlobalSeriesOptions;
import com.googlecode.gflot.client.options.GridOptions;
import com.googlecode.gflot.client.options.LegendOptions;
import com.googlecode.gflot.client.options.LineSeriesOptions;
import com.googlecode.gflot.client.options.PlotOptions;

public class RoutingChart extends ChartPortlet {

    private static String ROUTING_STATUS_FAILED = "failed"; //$NON-NLS-1$

    private static String ROUTING_STATUS_COMPLETED = "completed"; //$NON-NLS-1$

    public RoutingChart(Portal portal) {
        super(WelcomePortal.CHART_ROUTING_EVENT, portal);

        String setting = portalConfigs.getChartSetting(portletName);
        if (setting != null) {
            configModel = new TimeframeConfigModel(startedAsOn, setting);
        } else {
            configModel = new TimeframeConfigModel(startedAsOn);
        }

        initConfigSettings();

        initChart();
    }

    @Override
    public void refresh() {
        StatisticsRestServiceHandler.getInstance().getRoutingEventStats(configModel, new SessionAwareAsyncCallback<JSONArray>() {

            @Override
            public void onSuccess(JSONArray jsonArray) {
                Map<String, Object> newData = parseJSONData(jsonArray);
                doRefreshWith(newData);
            }
        });

    }

    private void initChart() {
        StatisticsRestServiceHandler.getInstance().getRoutingEventStats(configModel, new SessionAwareAsyncCallback<JSONArray>() {

            @Override
            public void onSuccess(JSONArray jsonArray) {
                chartData = parseJSONData(jsonArray);
                initAndShow();
            }
        });
    }

    @Override
    protected void initPlot() {
        super.initPlot();
        PlotModel model = plot.getModel();
        PlotOptions plotOptions = plot.getOptions();
        Set<String> appNames = chartData.keySet();
        List<String> appnamesSorted = sort(appNames);

        plotOptions
                .setGlobalSeriesOptions(
                        GlobalSeriesOptions
                                .create()
                                .setLineSeriesOptions(LineSeriesOptions.create().setShow(false).setFill(true))
                                .setBarsSeriesOptions(
                                        BarSeriesOptions.create().setShow(true).setBarWidth(0.6)
                                                .setAlignment(BarAlignment.CENTER)).setStack(true))
                .setYAxesOptions(AxesOptions.create().addAxisOptions(AxisOptions.create().setTickDecimals(0).setMinimum(0)))
                .setXAxesOptions(
                        AxesOptions.create().addAxisOptions(
                                CategoriesAxisOptions.create().setCategories(
                                        appnamesSorted.toArray(new String[appnamesSorted.size()]))));

        plotOptions.setLegendOptions(LegendOptions.create().setShow(true));
        plotOptions.setGridOptions(GridOptions.create().setHoverable(true));
        // create series
        SeriesHandler seriesCompleted = model.addSeries(Series.of(MessagesFactory.getMessages().chart_routing_event_completed()));
        SeriesHandler seriesFailed = model.addSeries(Series.of(MessagesFactory.getMessages().chart_routing_event_failed()));

        // add data
        for (String appName : appnamesSorted) {
            seriesCompleted.add(DataPoint.of(appName,
                    ((Map<String, Integer>) chartData.get(appName)).get(ROUTING_STATUS_COMPLETED)));
            seriesFailed.add(DataPoint.of(appName, ((Map<String, Integer>) chartData.get(appName)).get(ROUTING_STATUS_FAILED)));
        }
    }

    @Override
    protected void updatePlot() {
        PlotModel model = plot.getModel();
        PlotOptions plotOptions = plot.getOptions();
        Set<String> appNames = chartData.keySet();
        List<String> appnamesSorted = sort(appNames);

        plotOptions.setXAxesOptions(AxesOptions.create().addAxisOptions(
                CategoriesAxisOptions.create().setCategories(appnamesSorted.toArray(new String[appnamesSorted.size()]))));

        List<? extends SeriesHandler> series = model.getHandlers();
        assert series.size() == 2;
        SeriesHandler seriesCompleted = series.get(0);
        SeriesHandler seriesFailed = series.get(1);

        seriesCompleted.clear();
        seriesFailed.clear();
        for (String appName : appnamesSorted) {
            seriesCompleted.add(DataPoint.of(appName,
                    ((Map<String, Integer>) chartData.get(appName)).get(ROUTING_STATUS_COMPLETED)));
            seriesFailed.add(DataPoint.of(appName, ((Map<String, Integer>) chartData.get(appName)).get(ROUTING_STATUS_FAILED)));
        }
    }

    @Override
    protected Map<String, Object> parseJSONData(JSONArray jsonArray) {
        assert (jsonArray.size() == 2);

        Map<String, Object> routingDataNew = new HashMap<String, Object>();

        JSONObject failedJSONObj;
        JSONObject completedJSONObj;
        JSONArray failures;
        JSONArray completes;
        failedJSONObj = (JSONObject) jsonArray.get(0);
        completedJSONObj = (JSONObject) jsonArray.get(1);
        String currApp;
        Set<String> appNames = new HashSet<String>();
        failures = failedJSONObj.get(ROUTING_STATUS_FAILED).isArray();
        Map<String, Integer> completesMap = new HashMap<String, Integer>();

        JSONObject curFailure;
        Map<String, Integer> failureMap = new HashMap<String, Integer>();
        int numOfFailed = 0;
        for (int i = 0; i < failures.size(); i++) {
            curFailure = failures.get(i).isObject();
            currApp = curFailure.keySet().iterator().next();
            numOfFailed = (int) curFailure.get(currApp).isNumber().getValue();
            failureMap.put(currApp, numOfFailed);
            appNames.add(currApp);
        }

        completes = completedJSONObj.get(ROUTING_STATUS_COMPLETED).isArray();
        JSONObject curComplete;
        int numOfCompleted = 0;
        for (int i = 0; i < completes.size(); i++) {
            curComplete = completes.get(i).isObject();
            currApp = curComplete.keySet().iterator().next();
            numOfCompleted = (int) curComplete.get(currApp).isNumber().getValue();
            completesMap.put(currApp, numOfCompleted);
            appNames.add(currApp);
        }

        Map<String, Integer> status;
        for (String appName : appNames) {
            status = new HashMap<String, Integer>(2);
            status.put(ROUTING_STATUS_FAILED, !failureMap.containsKey(appName) ? 0 : failureMap.get(appName));
            status.put(ROUTING_STATUS_COMPLETED, !completesMap.containsKey(appName) ? 0 : completesMap.get(appName));
            routingDataNew.put(appName, status);
        }

        return routingDataNew;
    }

    @Override
    protected boolean isDifferentFrom(Map<String, Object> newData) {
        if (chartData.size() != newData.size()) {
            return true;
        } else {
            Map<String, Integer> status;
            Map<String, Integer> statusNew;
            for (String appName : chartData.keySet()) {
                status = (Map<String, Integer>) chartData.get(appName);
                statusNew = (Map<String, Integer>) newData.get(appName);

                if (!status.get(ROUTING_STATUS_FAILED).equals(statusNew.get(ROUTING_STATUS_FAILED))
                        || !status.get(ROUTING_STATUS_COMPLETED).equals(statusNew.get(ROUTING_STATUS_COMPLETED))) {
                    return true;
                }

            }
        }
        return false;
    }

}
