package org.talend.mdm.webapp.general.client.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.client.util.UserContextUtil;
import org.talend.mdm.webapp.general.client.General;
import org.talend.mdm.webapp.general.client.GeneralServiceAsync;
import org.talend.mdm.webapp.general.client.i18n.MessageFactory;
import org.talend.mdm.webapp.general.client.mvc.GeneralEvent;
import org.talend.mdm.webapp.general.model.ActionBean;
import org.talend.mdm.webapp.general.model.ComboBoxModel;

import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.mvc.Dispatcher;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.form.CheckBoxGroup;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.Radio;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;

public class ActionsPanel extends FormPanel {

    private static ActionsPanel instance;

    private static final String NAME_START = "start", NAME_PROCESS = "process", NAME_ALERT = "alert", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            NAME_SEARCH = "search", NAME_TASKS = "tasks", NAME_CHART_DATA = "chart_data", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            NAME_CHART_ROUTING_EVENT = "chart_routing_event", NAME_CHART_JOURNAL = "chart_journal", NAME_CHART_MATCHING = "chart_matching"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private static final List<String> DEFAULT_NONCHART_NAMES = Arrays.asList(NAME_START, NAME_PROCESS, NAME_ALERT, NAME_SEARCH,
            NAME_TASKS);

    private static final Set<String> DEFAULT_CHART_NAMES = new HashSet<String>(Arrays.asList(NAME_CHART_DATA,
            NAME_CHART_ROUTING_EVENT, NAME_CHART_JOURNAL, NAME_CHART_MATCHING));

    private static final String DEFAULT_COLUMN_NUM = "defaultColNum"; //$NON-NLS-1$

    private GeneralServiceAsync service = (GeneralServiceAsync) Registry.get(General.OVERALL_SERVICE);

    private ListStore<ComboBoxModel> containerStore = new ListStore<ComboBoxModel>();

    private ComboBox<ComboBoxModel> dataContainerBox = new ComboBox<ComboBoxModel>();

    private ListStore<ComboBoxModel> dataStore = new ListStore<ComboBoxModel>();

    private ComboBox<ComboBoxModel> dataModelBox = new ComboBox<ComboBoxModel>();

    private Map<String, CheckBox> portletCKBoxes;

    private CheckBox chartsCheck;

    private Radio col2Radio;

    private Radio col3Radio;

    private FormData formData;

    private Button saveBtn = new Button(MessageFactory.getMessages().save());

    private Button saveConfigBtn = new Button(MessageFactory.getMessages().save());

    private List<String> allPortlets;

    private List<String> portletsToCheck;

    private int colNum = 3;

    private boolean chartsOn = false;

    private ComboBoxModel emptyModelValue = new ComboBoxModel();

    private static Boolean modelSelectFlag = true;

    private static Boolean containerSelectFlag = true;

    private static String WELCOMEPORTAL_CONTEXT = "welcomeportal"; //$NON-NLS-1$

    private boolean isEnterprise;

    private static String WELCOMEPORTAL_APP = "WelcomePortal"; //$NON-NLS-1$

    private ActionsPanel() {
        super();
        this.setHeading(MessageFactory.getMessages().actions());
        this.setStyleAttribute("background", "#fff"); //$NON-NLS-1$ //$NON-NLS-2$
        FieldSet domainConfig = new FieldSet();
        FormLayout formLayout = new FormLayout(LabelAlign.TOP);
        domainConfig.setLayout(formLayout);
        domainConfig.setHeading(MessageFactory.getMessages().domain_configuration());

        dataContainerBox.setFieldLabel(MessageFactory.getMessages().data_container());
        dataContainerBox.setDisplayField("value"); //$NON-NLS-1$
        dataContainerBox.setValueField("value"); //$NON-NLS-1$
        dataContainerBox.setAllowBlank(false);
        dataContainerBox.setWidth(windowResizeDelay);
        dataContainerBox.setStore(containerStore);
        dataContainerBox.setTypeAhead(true);
        dataContainerBox.setTriggerAction(TriggerAction.ALL);
        dataContainerBox.setEditable(disabled);
        dataModelBox.setFieldLabel(MessageFactory.getMessages().data_model());
        dataModelBox.setDisplayField("value"); //$NON-NLS-1$
        dataModelBox.setValueField("value"); //$NON-NLS-1$
        dataModelBox.setAllowBlank(false);
        dataModelBox.setWidth(windowResizeDelay);
        dataModelBox.setStore(dataStore);
        dataModelBox.setTypeAhead(true);
        dataModelBox.setTriggerAction(TriggerAction.ALL);
        dataModelBox.setEditable(disabled);
        saveBtn.disable();
        formData = new FormData();
        formData.setMargins(new Margins(3, 0, 3, 0));
        domainConfig.add(dataContainerBox, formData);
        domainConfig.add(dataModelBox, formData);
        domainConfig.add(saveBtn, formData);

        this.add(domainConfig);

        this.addDefaultPortletConfig();
        this.setScrollMode(Scroll.AUTO);
        initEvent();
    }

    private void addDefaultPortletConfig() {
        portletCKBoxes = new HashMap<String, CheckBox>(9);
        final FieldSet portalConfig = new FieldSet();
        FormLayout formLayout = new FormLayout();
        formLayout.setLabelAlign(LabelAlign.TOP);
        portalConfig.setLayout(formLayout);
        portalConfig.setHeading(MessageFactory.getMessages().portal_configuration());
        final CheckBoxGroup checkGroup = new CheckBoxGroup();
        checkGroup.setName("portlets"); //$NON-NLS-1$
        checkGroup.setFieldLabel(MessageFactory.getMessages().portal_portlets());
        checkGroup.setOrientation(Orientation.VERTICAL);
        CheckBox check;

        for (String portletName : DEFAULT_NONCHART_NAMES) {
            check = new CheckBox();
            check.setName(portletName);
            check.setBoxLabel(getPortletLabel(portletName));
            check.setValue(false);
            check.setVisible(false);
            checkGroup.add(check);
            portletCKBoxes.put(portletName, check);
        }

        service.isEnterpriseVersion(new SessionAwareAsyncCallback<Boolean>() {

            @Override
            public void onSuccess(Boolean isEE) {
                isEnterprise = isEE;
                if (isEnterprise) {
                    chartsCheck = new CheckBox() {

                        @Override
                        protected void onClick(ComponentEvent be) {
                            if (!this.getValue()) {
                                chartsOn = false;
                            } else {
                                chartsOn = true;
                            }
                            updateChartsConfig(chartsOn);
                        }
                    };
                    chartsCheck.setName("charts"); //$NON-NLS-1$
                    chartsCheck.setBoxLabel(MessageFactory.getMessages().portal_chart_portlets());
                    chartsCheck.setValue(true);
                    chartsCheck.setVisible(true);
                    checkGroup.add(chartsCheck);

                    final CheckBoxGroup chartsGroup = new CheckBoxGroup();
                    chartsGroup.setHideLabel(true);
                    chartsGroup.setOrientation(Orientation.VERTICAL);

                    CheckBox checkChart;
                    for (String portletName : DEFAULT_CHART_NAMES) {
                        checkChart = new CheckBox() {

                            @Override
                            protected void onClick(ComponentEvent be) {
                                if (this.getValue()) {
                                    chartsCheck.setValue(true);
                                    chartsOn = true;
                                } else {
                                    if (allChartsUnchecked()) {
                                        chartsCheck.setValue(false);
                                    }
                                }
                            }

                            private boolean allChartsUnchecked() {
                                List<Field<?>> chartsChks = chartsGroup.getAll();
                                for (Field<?> check : chartsChks) {
                                    if (((CheckBox) check).getValue()) {
                                        return false;
                                    }
                                }
                                return true;
                            }
                        };
                        checkChart.setName(portletName);
                        checkChart.setBoxLabel(getPortletLabel(portletName));
                        checkChart.setValue(false);
                        checkChart.setVisible(false);
                        chartsGroup.add(checkChart);
                        portletCKBoxes.put(portletName, checkChart);
                    }

                    formData = new FormData();
                    formData.setMargins(new Margins(2, -20, 2, 20));
                    portalConfig.add(checkGroup, formData);

                    FormData formDataCharts = new FormData();
                    formDataCharts.setMargins(new Margins(-2, -40, 2, 40));
                    portalConfig.add(chartsGroup, formDataCharts);
                } else {
                    formData = new FormData();
                    formData.setMargins(new Margins(2, -20, 2, 20));
                    portalConfig.add(checkGroup, formData);
                }

                RadioGroup colRadioGroup = new RadioGroup();
                colRadioGroup.setFieldLabel(MessageFactory.getMessages().portal_columns());
                colRadioGroup.setOrientation(Orientation.VERTICAL);

                col2Radio = new Radio();
                col2Radio.setBoxLabel(MessageFactory.getMessages().portal_columns_two());

                col3Radio = new Radio();
                col3Radio.setBoxLabel(MessageFactory.getMessages().portal_columns_three());

                colRadioGroup.add(col2Radio);
                colRadioGroup.add(col3Radio);

                formData = new FormData();
                formData.setMargins(new Margins(2, -20, 2, 20));
                portalConfig.add(colRadioGroup, formData);

                formData = new FormData();
                formData.setMargins(new Margins(2, 0, 2, 0));
                portalConfig.add(saveConfigBtn, formData);
                saveConfigBtn.disable();
                ActionsPanel.this.add(portalConfig);
                ActionsPanel.this.layout(true);
            }

        });
    }

    public static ActionsPanel getInstance() {
        if (instance == null) {
            instance = new ActionsPanel();
        }
        return instance;
    }

    private String getPortletLabel(String portletName) {
        if (portletName.equals(NAME_START)) {
            return MessageFactory.getMessages().portlet_start();
        } else if (portletName.equals(NAME_PROCESS)) {
            return MessageFactory.getMessages().portlet_process();
        } else if (portletName.equals(NAME_ALERT)) {
            return MessageFactory.getMessages().portlet_alert();
        } else if (portletName.equals(NAME_SEARCH)) {
            return MessageFactory.getMessages().portlet_search();
        } else if (portletName.equals(NAME_TASKS)) {
            return MessageFactory.getMessages().portlet_tasks();
        } else if (portletName.equals(NAME_CHART_DATA)) {
            return MessageFactory.getMessages().portlet_data();
        } else if (portletName.equals(NAME_CHART_ROUTING_EVENT)) {
            return MessageFactory.getMessages().portlet_routing();
        } else if (portletName.equals(NAME_CHART_JOURNAL)) {
            return MessageFactory.getMessages().portlet_journal();
        } else {
            return MessageFactory.getMessages().portlet_matching();
        }
    }

    private void initEvent() {
        saveBtn.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                Dispatcher dispatcher = Dispatcher.get();
                dispatcher.dispatch(GeneralEvent.SwitchClusterAndModel);
                if (!saveConfigBtn.isEnabled()) {
                    saveConfigBtn.enable();
                }
            }
        });

        saveConfigBtn.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                List<String> configUpdates = getPortalConfigUpdate();
                switchToWelcomeportal();
                refreshPortal(configUpdates.toString());

            }
        });

        dataModelBox.addSelectionChangedListener(new SelectionChangedListener<ComboBoxModel>() {

            @Override
            public void selectionChanged(SelectionChangedEvent<ComboBoxModel> se) {
                if (se.getSelectedItem() == null) {
                    saveBtn.disable();
                    return;
                }
                String selectedValue = se.getSelectedItem().get("value"); //$NON-NLS-1$
                if (selectedValue != null && !"".equals(selectedValue.trim())) { //$NON-NLS-1$
                    saveBtn.enable();
                    saveConfigBtn.enable();
                    if (!modelSelectFlag) {
                        modelSelectFlag = true;
                        return;
                    }
                    // look for data container
                    for (ComboBoxModel dataModel : dataContainerBox.getStore().getModels()) {
                        if (selectedValue.equals(dataModel.getValue())) {
                            dataContainerBox.setValue(dataModel);
                            containerSelectFlag = true;
                            saveBtn.enable();
                            saveConfigBtn.enable();
                            return;
                        }
                    }
                    containerSelectFlag = false;
                    dataContainerBox.setValue(emptyModelValue);
                    saveBtn.disable();
                    saveConfigBtn.disable();
                }
            }
        });

        dataContainerBox.addSelectionChangedListener(new SelectionChangedListener<ComboBoxModel>() {

            @Override
            public void selectionChanged(SelectionChangedEvent<ComboBoxModel> se) {
                if (se.getSelectedItem() == null) {
                    saveBtn.disable();
                    return;
                }
                String selectedValue = se.getSelectedItem().get("value"); //$NON-NLS-1$
                if (selectedValue != null && !"".equals(selectedValue.trim())) { //$NON-NLS-1$
                    saveBtn.enable();
                    saveConfigBtn.enable();
                    if (!containerSelectFlag) {
                        containerSelectFlag = true;
                        return;
                    }
                    // look for data model
                    for (ComboBoxModel dataModel : dataModelBox.getStore().getModels()) {
                        if (selectedValue.equals(dataModel.getValue())) {
                            dataModelBox.setValue(dataModel);
                            modelSelectFlag = true;
                            return;
                        }
                    }
                    modelSelectFlag = false;
                    dataModelBox.setValue(emptyModelValue);
                    saveBtn.disable();
                    saveConfigBtn.disable();
                }
            }
        });
    }

    protected void switchToWelcomeportal() {
        display(WELCOMEPORTAL_CONTEXT, WELCOMEPORTAL_APP);
        AccordionMenus.getInstance().selectedItem(AccordionMenus.getInstance().getWelcomeportalItem());
    }

    public void loadAction(ActionBean action) {
        containerStore.removeAll();
        dataStore.removeAll();

        containerStore.add(action.getClusters());
        dataStore.add(action.getModels());

        ComboBoxModel cluster = containerStore.findModel("value", action.getCurrentCluster()); //$NON-NLS-1$
        if (cluster != null) {
            dataContainerBox.setValue(cluster);
        }
        ComboBoxModel model = dataStore.findModel("value", action.getCurrentModel()); //$NON-NLS-1$
        if (model != null) {
            dataModelBox.setValue(model);
        }

        UserContextUtil.setDataContainer(action.getCurrentCluster());
        UserContextUtil.setDataModel(action.getCurrentModel());
    }

    public String getDataCluster() {
        return dataContainerBox.getValue().getValue();
    }

    public String getDataModel() {
        return dataModelBox.getValue().getValue();
    }

    public void updatePortletConfig(String configs) {
        populateConfigs(configs);

        for (CheckBox check : portletCKBoxes.values()) {
            String name = check.getName();
            check.setVisible(allPortlets.contains(name));
            check.setValue(portletsToCheck.contains(name));
        }

        if (isEnterprise) {
            chartsCheck.setValue(chartsOn);
        }

        col3Radio.setValue(((colNum == 3) ? true : false));
        col2Radio.setValue(((colNum == 2) ? true : false));

        if (saveBtn.isEnabled()) {
            saveConfigBtn.enable();
        }
        this.layout(true);
    }

    private void populateConfigs(String dataString) {
        String[] temp = dataString.split("; "); //$NON-NLS-1$

        String all = temp[0].substring(1, temp[0].length() - 1);
        allPortlets = Arrays.asList(all.split(", ")); //$NON-NLS-1$

        String checks = temp[1].substring(1, temp[1].length() - 1);
        chartsOn = (checks.contains("chart_")); //$NON-NLS-1$
        portletsToCheck = Arrays.asList(checks.split(", ")); //$NON-NLS-1$

        colNum = Integer.parseInt(temp[2]);
    }

    public void uncheckPortlet(String portletName) {
        portletCKBoxes.get(portletName).setValue(false);
        this.layout(true);
    }

    private void updateChartsConfig(boolean isChartsOn) {
        for (CheckBox check : portletCKBoxes.values()) {
            if (DEFAULT_CHART_NAMES.contains(check.getName())) {
                if (isChartsOn) {
                    check.setVisible(true);
                    check.setValue(true);
                } else {
                    check.setValue(false);
                }
            }
        }

        this.layout(true);
    }

    private List<String> getPortalConfigUpdate() {
        List<String> updates = new ArrayList<String>();
        CheckBox check;
        for (String name : getDefaultPortletNames()) {
            check = portletCKBoxes.get(name);
            if (check.getValue()) {
                updates.add(name);
            }
        }

        updates.add((col3Radio.getValue() ? "3" : "2")); //$NON-NLS-1$ //$NON-NLS-2$

        return updates;
    }

    private List<String> getDefaultPortletNames() {
        if (isEnterprise) {
            return Arrays.asList(NAME_START, NAME_PROCESS, NAME_ALERT, NAME_SEARCH, NAME_TASKS, NAME_CHART_DATA,
                    NAME_CHART_ROUTING_EVENT, NAME_CHART_JOURNAL, NAME_CHART_MATCHING);
        } else {
            return Arrays.asList(NAME_START, NAME_PROCESS, NAME_ALERT, NAME_SEARCH, NAME_TASKS);
        }
    }

    // call refresh in WelcomePortal
    private native void refreshPortal(String portalConfig)/*-{
		$wnd.amalto.core.refreshPortal(portalConfig);
    }-*/;

    private native void display(String context, String application)/*-{
		if ($wnd.amalto[context]) {
			if ($wnd.amalto[context][application]) {
				$wnd.amalto[context][application].init();
			}
		}
    }-*/;
}
