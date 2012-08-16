// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.webapp.stagingarea.client.view;

import org.talend.mdm.webapp.base.client.model.UserContextModel;
import org.talend.mdm.webapp.base.client.util.UserContextUtil;
import org.talend.mdm.webapp.stagingarea.client.controller.ControllerContainer;
import org.talend.mdm.webapp.stagingarea.client.model.StagingContainerModel;

import com.extjs.gxt.charts.client.Chart;
import com.extjs.gxt.charts.client.model.ChartModel;
import com.extjs.gxt.charts.client.model.Legend;
import com.extjs.gxt.charts.client.model.Legend.Position;
import com.extjs.gxt.charts.client.model.charts.PieChart;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.extjs.gxt.ui.client.widget.layout.TableLayout;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;



public class StagingContainerSummaryView extends AbstractView {

    public static final String Chart_Url = "/core/secure/gxt/resources/chart/open-flash-chart.swf"; //$NON-NLS-1$

    public static final String STAGING_AREA_TITLE = "staging_area_title"; //$NON-NLS-1$

    public static final String STAGING_AREA_WAITING = "staging_area_waiting"; //$NON-NLS-1$

    public static final String STAGING_AREA_INVALID = "staging_area_invalid"; //$NON-NLS-1$

    public static final String STAGING_AREA_VALID = "staging_area_valid"; //$NON-NLS-1$

    private StagingContainerModel stagingContainerModel;

    private Button startValidate;

    private Grid titleGrid;

    private Label titleLabel;

    private Label containerLabel;

    private Label containerName;

    private Label modelLabel;

    private Label dataModelName;

    private static Widget chart;

    private UserContextModel ucx;

    public static void setChart(Widget chart) {
        StagingContainerSummaryView.chart = chart;
    }

    private HTMLPanel detailPanel;

    @Override
    protected void initComponents() {
        ucx = UserContextUtil.getUserContext();
        titleLabel = new Label(messages.staging_area_title());
        titleLabel.setStyleAttribute("margin-right", "20px"); //$NON-NLS-1$//$NON-NLS-2$
        containerLabel = new Label(messages.data_container());

        containerName = new Label(ucx.getDataContainer());
        containerName.setStyleAttribute("font-weight", "bold"); //$NON-NLS-1$//$NON-NLS-2$

        modelLabel = new Label(messages.data_model());
        dataModelName = new Label(ucx.getDataModel());
        dataModelName.setStyleAttribute("font-weight", "bold"); //$NON-NLS-1$//$NON-NLS-2$
        titleGrid = new Grid(1, 5);

        StringBuffer buffer = new StringBuffer();
        buffer.append("<div style='margin-bottom:10px; font-size:16px;' id='" + STAGING_AREA_TITLE + "'></div>"); //$NON-NLS-1$ //$NON-NLS-2$
        buffer.append("<div style='margin-left:20px; color:#0000ff; margin-bottom:5px;' id='" + STAGING_AREA_WAITING + "'></div>"); //$NON-NLS-1$ //$NON-NLS-2$
        buffer.append("<div style='margin-left:20px; color:#ff0000; margin-bottom:5px;' id='" + STAGING_AREA_INVALID + "'></div>"); //$NON-NLS-1$//$NON-NLS-2$
        buffer.append("<div style='margin-left:20px; color:#00aa00; margin-bottom:5px;' id='" + STAGING_AREA_VALID + "'></div>"); //$NON-NLS-1$//$NON-NLS-2$

        detailPanel = new HTMLPanel(buffer.toString());

        detailPanel.setSize("400px", "80px"); //$NON-NLS-1$//$NON-NLS-2$

        startValidate = new Button(messages.start_validation());
        startValidate.setSize(200, 30);
        startValidate.setEnabled(false);

        mainPanel.setHeight(220);
        mainPanel.setBodyBorder(false);
    }

    @Override
    protected void registerEvent() {
        startValidate.addSelectionListener(new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
                ControllerContainer.get().getSummaryController().startValidation();
            }
        });
    }

    @Override
    protected void initLayout() {
        mainPanel.setLayout(new TableLayout(2));
        TableData titleData = new TableData();
        titleData.setColspan(2);
        titleData.setRowspan(1);

        titleGrid.setWidget(0, 0, titleLabel);
        titleGrid.setWidget(0, 1, containerLabel);
        titleGrid.setWidget(0, 2, containerName);
        containerName.setStyleAttribute("margin-right", "10px"); //$NON-NLS-1$//$NON-NLS-2$
        titleGrid.setWidget(0, 3, modelLabel);
        titleGrid.setWidget(0, 4, dataModelName);

        mainPanel.add(titleGrid, titleData);
        TableData chartData = new TableData();
        chartData.setColspan(1);
        chartData.setRowspan(2);
        mainPanel.add(chart, chartData);
        TableData detailData = new TableData();
        detailData.setColspan(1);
        detailData.setRowspan(1);
        mainPanel.add(detailPanel, detailData);
        TableData startData = new TableData();
        startData.setHorizontalAlign(HorizontalAlignment.CENTER);
        mainPanel.add(startValidate, startData);
    }

    private ChartModel getPieChartData() {
        ChartModel cm = new ChartModel();
        cm.setBackgroundColour("#FFFFFF"); //$NON-NLS-1$
        Legend lg = new Legend(Position.RIGHT, true);
        lg.setPadding(1);
        cm.setLegend(lg);

        if (stagingContainerModel != null) {
            int waiting = stagingContainerModel.getWaitingValidationRecords();
            int valid = stagingContainerModel.getValidRecords();
            int invalid = stagingContainerModel.getInvalidRecords();

            PieChart pie = new PieChart();
            pie.setAlpha(0.5f);
            pie.setNoLabels(false);
            pie.setTooltip("#label# record #val#<br>#percent#"); //$NON-NLS-1$
            pie.setColours("#0000ff", "#ff0000", "#00aa00"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            pie.addSlices(new PieChart.Slice(waiting, messages.waiting()));
            pie.addSlices(new PieChart.Slice(invalid, messages.invalid()));
            pie.addSlices(new PieChart.Slice(valid, messages.valid()));
            cm.addChartConfig(pie);
        }
        return cm;
    }
    
    public void initDetailPanel() {
        if (stagingContainerModel != null) {
            int waiting = stagingContainerModel.getWaitingValidationRecords();
            int valid = stagingContainerModel.getValidRecords();
            int invalid = stagingContainerModel.getInvalidRecords();

            Element titleEl = detailPanel.getElementById(STAGING_AREA_TITLE);
            titleEl.setInnerHTML(messages.total_desc("<b>" + stagingContainerModel.getTotalRecords() + "</b>")); //$NON-NLS-1$ //$NON-NLS-2$

            Element waitingEl = detailPanel.getElementById(STAGING_AREA_WAITING);
            waitingEl.setInnerHTML(messages.waiting_desc("<b>" + waiting + "</b>")); //$NON-NLS-1$ //$NON-NLS-2$

            Element invalidEl = detailPanel.getElementById(STAGING_AREA_INVALID);
            invalidEl.setInnerHTML(messages.invalid_desc("<b>" + invalid + "</b>") + " <b><span id=\"open_invalid_record\" style=\"color:red; text-decoration:underline; cursor:pointer;\">" + messages.open_invalid_record() + "</span><b>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

            Element openEl = detailPanel.getElementById("open_invalid_record"); //$NON-NLS-1$
            addClickForOpenInvalidRecord(ucx.getDataContainer(), ucx.getDataModel(), openEl);
            
            Element validEl = detailPanel.getElementById(STAGING_AREA_VALID);
            validEl.setInnerHTML(messages.valid_desc("<b>" + valid + "</b>")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
    
    private native void addClickForOpenInvalidRecord(String container, String dataModel, Element el)/*-{
        el.onclick = function(){
            if ($wnd.amalto.browserecords && $wnd.amalto.browserecords.BrowseRecords){
                var stagingArea = {
                    dataContainer: "#STAGING",
                    dataModel: dataModel,
                    criteria: "Product/Id EQUALS *",
                    from: " from Staging area"
                };
                $wnd.amalto.browserecords.BrowseRecords.init(stagingArea);
            }
        };
    }-*/;

    public void refresh(StagingContainerModel stagingContainerModel) {
        this.stagingContainerModel = stagingContainerModel;
        initDetailPanel();
        if (chart instanceof Chart) {
            ((Chart) chart).setChartModel(getPieChartData());
        }
    }

    public StagingContainerModel getStagingContainerModel() {
        return stagingContainerModel;
    }

    public void disabledStartValidation() {
        startValidate.setEnabled(false);
    }

    public void enabledStartValidation() {
        startValidate.setEnabled(true);
    }

    public Button getStartValidateButton() {
        return startValidate;
    }

    protected void onAttach() {
        super.onAttach();
        ControllerContainer.get().getSummaryController().refreshView();
    }

}
