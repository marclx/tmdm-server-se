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
package org.talend.mdm.webapp.welcomeportal.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.client.util.UrlUtil;
import org.talend.mdm.webapp.welcomeportal.client.mvc.PortalProperties;
import org.talend.mdm.webapp.welcomeportal.client.widget.AlertPortlet;
import org.talend.mdm.webapp.welcomeportal.client.widget.BasePortlet;
import org.talend.mdm.webapp.welcomeportal.client.widget.DataChart;
import org.talend.mdm.webapp.welcomeportal.client.widget.JournalChart;
import org.talend.mdm.webapp.welcomeportal.client.widget.MatchingChart;
import org.talend.mdm.webapp.welcomeportal.client.widget.ProcessPortlet;
import org.talend.mdm.webapp.welcomeportal.client.widget.RoutingChart;
import org.talend.mdm.webapp.welcomeportal.client.widget.SearchPortlet;
import org.talend.mdm.webapp.welcomeportal.client.widget.StartPortlet;
import org.talend.mdm.webapp.welcomeportal.client.widget.TaskPortlet;

import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.event.ContainerEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.PortalEvent;
import com.extjs.gxt.ui.client.mvc.AppEvent;
import com.extjs.gxt.ui.client.mvc.Dispatcher;
import com.extjs.gxt.ui.client.widget.custom.Portal;
import com.extjs.gxt.ui.client.widget.custom.Portlet;

/**
 * DOC Administrator class global comment. Detailled comment
 */
public class MainFramePanel extends Portal {

    private static final int DEFAULT_REFRESH_INTERVAL = 10000;

    private static final boolean DEFAULT_REFRESH_STARTASON = false;

    private static final String NAME_START = "start", NAME_PROCESS = "process", NAME_ALERT = "alert", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            NAME_SEARCH = "search", NAME_TASKS = "tasks", NAME_CHART_DATA = "chart_data", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            NAME_CHART_ROUTING_EVENT = "chart_routing_event", NAME_CHART_JOURNAL = "chart_journal", NAME_CHART_MATCHING = "chart_matching"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private static final String USING_DEFAULT_COLUMN_NUM = "defaultColNum"; //$NON-NLS-1$

    private static final int DEFAULT_COLUMN_NUM = 3;

    private static final int ALTERNATIVE_COLUMN_NUM = 2;

    private static final String CHARTS_ENABLED = "chartsOn"; //$NON-NLS-1$

    private static final Set<String> nonAutoedPortlets = new HashSet<String>(Arrays.asList(WelcomePortal.START,
            WelcomePortal.SEARCH));

    private Set<String> allCharts;

    private boolean chartsOn;

    private boolean startedAsOn;

    private boolean isEnterprise;

    private int interval;

    private boolean hiddenWorkFlowTask;

    private boolean hiddenDSCTask;

    private List<BasePortlet> portlets;

    private int pos;

    private int numColumns;

    private Map<String, List<Integer>> portletToLocations;

    private Map<String, Boolean> portletToVisibilities;

    private Map<String, Boolean> portletToAutoOnOffs;

    private Map<String, Boolean> configFromActionsPanel;

    private PortalProperties props;

    private List<String> default_index_ordering;

    private WelcomePortalServiceAsync service = (WelcomePortalServiceAsync) Registry.get(WelcomePortal.WELCOMEPORTAL_SERVICE);

    public MainFramePanel(int numColumns, PortalProperties portalConfig, boolean isEnterpriseVersion) {
        this(numColumns, portalConfig, null, isEnterpriseVersion);
    }

    public MainFramePanel(int numColumns, PortalProperties portalConfig, Map<String, Boolean> userConfig,
            boolean isEnterpriseVersion) {
        super(numColumns);

        this.numColumns = numColumns;

        setBorders(true);
        setStyleAttribute("backgroundColor", "white"); //$NON-NLS-1$ //$NON-NLS-2$
        if (numColumns == DEFAULT_COLUMN_NUM) {
            setColumnWidth(0, .33);
            setColumnWidth(1, .33);
            setColumnWidth(2, .33);
        } else {
            setColumnWidth(0, .50);
            setColumnWidth(1, .50);
        }

        this.addListener(Events.Add, new Listener<ContainerEvent>() {

            @Override
            public void handleEvent(ContainerEvent be) {

                BasePortlet portlet = (BasePortlet) be.getItem();
                String portletName = portlet.getPortletName();
                int column = MainFramePanel.this.getPortletColumn(portlet);
                int row = MainFramePanel.this.getPortletIndex(portlet);

                portletToLocations.put(portletName, Arrays.asList(column, row));
                int index = default_index_ordering.indexOf(portletName);
                if (index < default_index_ordering.size() - 1) {
                    initializePortlet(default_index_ordering.get(index + 1));
                } else {
                    initDatabaseWithPortalSettings();
                }
            }
        });

        this.addListener(Events.Drop, new Listener<PortalEvent>() {

            @Override
            public void handleEvent(PortalEvent pe) {

                updateLocations();
                service.savePortalConfig(PortalProperties.KEY_PORTLET_LOCATIONS, portletToLocations.toString(),
                        new SessionAwareAsyncCallback<Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                props.add(PortalProperties.KEY_PORTLET_LOCATIONS, portletToLocations.toString());
                                return;
                            }

                            @Override
                            protected void doOnFailure(Throwable caught) {
                                super.doOnFailure(caught);
                                portletToLocations = props.getPortletToLocations();
                                revertPortletsToLocations(portletToLocations);
                            }
                        });
            }
        });

        isEnterprise = isEnterpriseVersion;
        props = portalConfig;
        configFromActionsPanel = userConfig;

        if (portalConfig.getPortletToLocations() == null) { // no portal configs in db yet, get autrefresh config
                                                            // settings
            service.getWelcomePortletConfig(new SessionAwareAsyncCallback<Map<Boolean, Integer>>() {

                @Override
                public void onSuccess(Map<Boolean, Integer> config) {
                    if (!config.containsKey(startedAsOn)) {
                        startedAsOn = !startedAsOn;
                    }

                    interval = config.get(startedAsOn);
                    default_index_ordering = getDefaultPortletOrdering(isEnterprise);
                    if (isEnterprise) {
                        chartsOn = true;
                    }
                    initializePortlets();
                }

                @Override
                public void doOnFailure(Throwable e) {
                    startedAsOn = DEFAULT_REFRESH_STARTASON;
                    interval = DEFAULT_REFRESH_INTERVAL;
                    default_index_ordering = getDefaultPortletOrdering(isEnterprise);
                    if (isEnterprise) {
                        chartsOn = true;
                    }
                    initializePortlets();
                }

            });
        } else {
            initializePortlets();
        }
    }

    private List<String> getDefaultPortletOrdering(boolean isEE) {
        if (isEE) {
            return Arrays.asList(WelcomePortal.START, WelcomePortal.PROCESS, WelcomePortal.ALERT, WelcomePortal.SEARCH,
                    WelcomePortal.TASKS, WelcomePortal.CHART_DATA, WelcomePortal.CHART_ROUTING_EVENT,
                    WelcomePortal.CHART_JOURNAL, WelcomePortal.CHART_MATCHING);
        } else {
            return Arrays.asList(WelcomePortal.START, WelcomePortal.PROCESS, WelcomePortal.ALERT, WelcomePortal.TASKS);
        }
    }

    private void initializePortlets() {
        portlets = new ArrayList<BasePortlet>();
        Map<String, List<Integer>> locations = props.getPortletToLocations();

        if (locations == null) {// login: init from scratch - no data in db
            portletToLocations = new LinkedHashMap<String, List<Integer>>();
            BasePortlet portlet;
            // start portlets initialization, see ContainerEvent listener
            portlet = new StartPortlet(this);
            portlets.add(portlet);
            MainFramePanel.this.add(portlet);
        } else if (configFromActionsPanel == null) {// login: init with configs in db
            portletToLocations = props.getPortletToLocations();
            portletToVisibilities = props.getPortletToVisibilities();

            if (isEnterprise) {
                allCharts = props.getAllCharts();
                chartsOn = props.getChartsOn();
            }

            initializePortlets(portletToLocations, portletToVisibilities);
            recordPortalSettings();
        } else {
            // switch column config/chartsSwitcher status updated, get current ordering from database, other configs
            // from Actions Panel
            final PortalProperties portalPropertiesCp = new PortalProperties(props);
            portletToLocations = props.getPortletToLocations();
            SortedMap<List<Integer>, String> locationToPortlets = new TreeMap<List<Integer>, String>(getLocationComparator());
            for (String portletName : portletToLocations.keySet()) {
                locationToPortlets.put(portletToLocations.get(portletName), portletName);
            }

            List<String> orderedPortletNames = new ArrayList<String>(portletToLocations.size());
            orderedPortletNames.addAll(locationToPortlets.values());

            if (isEnterprise) {
                allCharts = props.getAllCharts();
                chartsOn = configFromActionsPanel.get(CHARTS_ENABLED);
                if (chartsOn) {
                    if (!orderedPortletNames.containsAll(allCharts)) {
                        orderedPortletNames.addAll(allCharts);
                    }
                } else {
                    orderedPortletNames.removeAll(allCharts);
                    props.disableAutoRefresh(allCharts);
                }
            }

            List<List<Integer>> defaultLocations = getDefaultLocations(numColumns);
            int index = 0;
            List<Integer> loc;
            portletToVisibilities = new HashMap<String, Boolean>();
            Boolean visible;
            for (String name : orderedPortletNames) {
                loc = defaultLocations.get(index);
                visible = configFromActionsPanel.get(name);
                if (!visible) {
                    props.add(PortalProperties.KEY_AUTO_ONOFFS, name, ((Boolean) false).toString());
                }
                initializePortlet(name, loc, visible);
                portletToVisibilities.put(name, visible);
                index++;
            }

            updateLocations();

            props.add(PortalProperties.KEY_PORTLET_LOCATIONS, portletToLocations.toString());
            props.add(PortalProperties.KEY_PORTLET_VISIBILITIES, portletToVisibilities.toString());
            props.add(PortalProperties.KEY_COLUMN_NUM, ((Integer) MainFramePanel.this.numColumns).toString());

            if (isEnterprise) {
                props.add(PortalProperties.KEY_CHARTS_ON, ((Boolean) chartsOn).toString());
            }

            service.savePortalConfig(props, new SessionAwareAsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    recordPortalSettings();
                    return;
                }

                @Override
                protected void doOnFailure(Throwable caught) {
                    super.doOnFailure(caught);
                    AppEvent appEvent = new AppEvent(WelcomePortalEvents.RevertRefreshPortal, portalPropertiesCp);
                    Dispatcher.forwardEvent(appEvent);
                }
            });

        }
    }

    private void recordPortalSettings() {
        Map<String, Boolean> portletConfigs = new HashMap<String, Boolean>(portletToLocations.size());
        for (String name : portletToLocations.keySet()) {
            portletConfigs.put(name, portletToVisibilities.get(name));
        }

        portletConfigs.put(USING_DEFAULT_COLUMN_NUM, numColumns == getDefaultColumNum());

        String portletConfigsStr;
        if (isEnterprise) {
            portletConfigs.put(CHARTS_ENABLED, chartsOn);
            portletConfigsStr = portletConfigs.toString() + allCharts.toString();
        } else {
            portletConfigsStr = portletConfigs.toString();
        }
        recordPortalConfigs(portletConfigsStr);
    }

    private Integer getDefaultColumNum() {
        return isEnterprise ? DEFAULT_COLUMN_NUM : ALTERNATIVE_COLUMN_NUM;
    }

    private Comparator<List<Integer>> getLocationComparator() {
        return new Comparator<List<Integer>>() {

            @Override
            public int compare(List<Integer> loc1, List<Integer> loc2) {
                assert loc1.size() == 2;
                assert loc2.size() == 2;

                int row1 = loc1.get(1);
                int row2 = loc2.get(1);

                int diff = row1 - row2;
                if (diff != 0) {
                    return diff;
                } else {
                    return loc1.get(0) - loc2.get(0);
                }
            }

        };
    }

    private List<List<Integer>> getDefaultLocations(int colNum) {

        List<List<Integer>> locs = new ArrayList<List<Integer>>(9);
        if (colNum == ALTERNATIVE_COLUMN_NUM) {
            locs.add(Arrays.asList(0, 0));
            locs.add(Arrays.asList(1, 0));
            locs.add(Arrays.asList(0, 1));
            locs.add(Arrays.asList(1, 1));
            locs.add(Arrays.asList(0, 2));
            locs.add(Arrays.asList(1, 2));
            locs.add(Arrays.asList(0, 3));
            locs.add(Arrays.asList(1, 3));
            locs.add(Arrays.asList(0, 4));
        } else {
            locs.add(Arrays.asList(0, 0));
            locs.add(Arrays.asList(1, 0));
            locs.add(Arrays.asList(2, 0));
            locs.add(Arrays.asList(0, 1));
            locs.add(Arrays.asList(1, 1));
            locs.add(Arrays.asList(2, 1));
            locs.add(Arrays.asList(0, 2));
            locs.add(Arrays.asList(1, 2));
            locs.add(Arrays.asList(2, 2));
        }

        return locs;
    }

    // called when portlet locations are restored from db -- back in synchronous mode
    private void initializePortlets(Map<String, List<Integer>> locs, Map<String, Boolean> visibles) {

        SortedMap<List<Integer>, String> locationToPortlets = new TreeMap<List<Integer>, String>(getLocationComparator());
        for (String portletName : locs.keySet()) {
            locationToPortlets.put(locs.get(portletName), portletName);
        }
        String portletName;
        for (List<Integer> loc : locationToPortlets.keySet()) {
            portletName = locationToPortlets.get(loc);
            initializePortlet(portletName, loc, visibles.get(portletName));
        }
    }

    private void initializePortlet(String portletName) {
        BasePortlet portlet = null;

        if (WelcomePortal.PROCESS.equals(portletName)) {
            portlet = new ProcessPortlet(this);
        } else if (WelcomePortal.ALERT.equals(portletName)) {
            initAlertPortlet();
        } else if (WelcomePortal.SEARCH.equals(portletName)) {
            portlet = new SearchPortlet(MainFramePanel.this);
        } else if (WelcomePortal.TASKS.equals(portletName)) {
            initTaskPortlet();
        } else if (WelcomePortal.CHART_DATA.equals(portletName)) {
            portlet = new DataChart(this);
        } else if (WelcomePortal.CHART_ROUTING_EVENT.equals(portletName)) {
            portlet = new RoutingChart(this);
        } else if (WelcomePortal.CHART_JOURNAL.equals(portletName)) {
            portlet = new JournalChart(this);
        } else if (WelcomePortal.CHART_MATCHING.equals(portletName)) {
            portlet = new MatchingChart(MainFramePanel.this);
        }

        if (portlet != null) {
            portlets.add(portlet);
            this.add(portlet);
        }
    }

    private void initializePortlet(String portletName, List<Integer> loc, boolean visible) {
        BasePortlet portlet = null;
        if (WelcomePortal.START.equals(portletName)) {
            portlet = new StartPortlet(this);
        } else if (WelcomePortal.PROCESS.equals(portletName)) {
            portlet = new ProcessPortlet(this);
        } else if (WelcomePortal.ALERT.equals(portletName)) {
            portlet = new AlertPortlet(this);
        } else if (WelcomePortal.SEARCH.equals(portletName)) {
            portlet = new SearchPortlet(this);
        } else if (WelcomePortal.TASKS.equals(portletName)) {
            portlet = new TaskPortlet(this);
        } else if (WelcomePortal.CHART_DATA.equals(portletName)) {
            portlet = new DataChart(this);
        } else if (WelcomePortal.CHART_ROUTING_EVENT.equals(portletName)) {
            portlet = new RoutingChart(this);
        } else if (WelcomePortal.CHART_JOURNAL.equals(portletName)) {
            portlet = new JournalChart(this);
        } else if (WelcomePortal.CHART_MATCHING.equals(portletName)) {
            portlet = new MatchingChart(this);
        } else {
            assert false;
        }

        if (portlet != null) {
            portlets.add(portlet);
            if (!visible) {
                portlet.hide();
            }
            this.add(portlet, loc);
        }
    }

    // called when db saving fails for 'Drop' event
    private void revertPortletsToLocations(Map<String, List<Integer>> locs) {

        SortedMap<List<Integer>, String> locationToPortlets = new TreeMap<List<Integer>, String>(getLocationComparator());
        for (String portletName : locs.keySet()) {
            locationToPortlets.put(locs.get(portletName), portletName);
        }

        for (BasePortlet portlet : portlets) {
            this.remove(portlet);

        }

        for (List<Integer> loc : locationToPortlets.keySet()) {
            Portlet portlet = getPortlet(locationToPortlets.get(loc));
            assert (portlet != null);
            this.add(portlet, loc);
        }
    }

    private Portlet getPortlet(String name) {
        Portlet result = null;
        for (BasePortlet portlet : portlets) {
            if (name.equals(portlet.getPortletName())) {
                result = portlet;
            }
        }
        return result;
    }

    private void add(Portlet portlet) {
        this.add(portlet, pos % numColumns);
        pos++;
        fireEvent(Events.Add, new ContainerEvent<Portal, Portlet>(this, portlet));
    }

    private void add(Portlet portlet, List<Integer> loc) {
        insert(portlet, loc.get(1), loc.get(0));
    }

    private List<String> getPortletNames() {
        List<String> names = new ArrayList<String>(portlets.size());
        for (BasePortlet portlet : portlets) {
            names.add(portlet.getPortletName());
        }
        return names;
    }

    private void updateLocations() {
        int column;
        int row;
        for (BasePortlet portlet : portlets) {
            column = this.getPortletColumn(portlet);
            row = this.getPortletIndex(portlet);

            portletToLocations.put(portlet.getPortletName(), Arrays.asList(column, row));
        }

        if (isEnterprise) {
            if ((!chartsOn) && (portlets.size() < portletToLocations.size())) {
                for (String name : allCharts) {
                    portletToLocations.remove(name);
                }
            }
        }
    }

    public void updateVisibilities() {
        for (BasePortlet portlet : portlets) {
            portletToVisibilities.put(portlet.getPortletName(), portlet.isVisible());
        }
    }

    public void refreshPortlets() {
        for (BasePortlet portlet : portlets) {
            portlet.refresh();
        }
    }

    public void stopAutoRefresh() {
        for (BasePortlet portlet : portlets) {
            if (!nonAutoedPortlets.contains(portlet.getPortletName()) && portlet.isAutoOn()) {
                portlet.resetAutofresh(false);
            }
        }
    }

    public Set<String> getAllPortletNames() {
        return portletToLocations.keySet();
    }

    public void removeAllPortlets() {
        this.removeAll();
    }

    // called for basic config change (portlets visibilities changes, except 'charts'/column number updates) triggered
    // from Action Panel
    public void refresh(Map<String, Boolean> config) {
        final Map<String, Boolean> portletToAutoOnOffsCp = props.getAutoRefreshStatus();
        final Map<String, Boolean> portletToVisibilitiesCp = props.getPortletToVisibilities();
        for (BasePortlet portlet : portlets) {

            if (config.get(portlet.getPortletName())) {
                portlet.show();
            } else {
                portlet.hide();
                portlet.resetAutofresh(false);
                props.add(PortalProperties.KEY_AUTO_ONOFFS, portlet.getPortletName(), ((Boolean) false).toString());
            }
        }
        updateVisibilities();

        List<String> configUpdates = Arrays.asList(portletToVisibilities.toString(), props.getAutoRefreshStatus().toString());

        service.savePortalConfig(configUpdates, new SessionAwareAsyncCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                // props already synched for autoOnOffs in the above
                props.add(PortalProperties.KEY_PORTLET_VISIBILITIES, portletToVisibilities.toString());
                MainFramePanel.this.layout(true);
                return;
            }

            @Override
            protected void doOnFailure(Throwable caught) {
                // revert to previous auto state on portlets and visible marks on Action Panel
                super.doOnFailure(caught);
                portletToVisibilities = portletToVisibilitiesCp;
                portletToAutoOnOffs = portletToAutoOnOffsCp;
                props.add(PortalProperties.KEY_PORTLET_VISIBILITIES, portletToVisibilitiesCp.toString());
                props.add(PortalProperties.KEY_AUTO_ONOFFS, portletToAutoOnOffsCp.toString());
                String portletName;

                for (BasePortlet portlet : portlets) {
                    portletName = portlet.getPortletName();
                    if (portletToVisibilities.get(portletName)) {
                        portlet.show();
                        if (!nonAutoedPortlets.contains(portletName) && portletToAutoOnOffs.get(portletName)) {
                            portlet.resetAutofresh(true);
                        }
                    } else {
                        portlet.hide();
                    }
                }

                MainFramePanel.this.layout(true);
                recordPortalSettings();
            }
        });
    }

    public PortalProperties getProps() {

        return props;

    }

    public void itemClick(final String context, final String application) {
        service.isExpired(UrlUtil.getLanguage(), new SessionAwareAsyncCallback<Boolean>() {

            @Override
            public void onSuccess(Boolean result) {
                initUI(context, application);
            }
        });
    }

    public boolean isHiddenWorkFlowTask() {
        return this.hiddenWorkFlowTask;
    }

    public void setHiddenWorkFlowTask(boolean hiddenWorkFlowTask) {
        this.hiddenWorkFlowTask = hiddenWorkFlowTask;
    }

    public boolean isHiddenDSCTask() {
        return this.hiddenDSCTask;
    }

    public void setHiddenDSCTask(boolean hiddenDSCTask) {
        this.hiddenDSCTask = hiddenDSCTask;
    }

    public boolean getStartedAsOn() {
        return this.startedAsOn;
    }

    public int getInterval() {
        return this.interval;
    }

    private void initAlertPortlet() {

        service.isHiddenLicense(new SessionAwareAsyncCallback<Boolean>() {

            @Override
            public void onSuccess(Boolean hideMe) {
                if (!hideMe) {
                    BasePortlet portlet = new AlertPortlet(MainFramePanel.this);
                    portlets.add(portlet);
                    MainFramePanel.this.add(portlet);
                } else {
                    int index = default_index_ordering.indexOf(WelcomePortal.ALERT);
                    initializePortlet(default_index_ordering.get(index + 1));
                }
            }

        });

    }

    private void initTaskPortlet() {

        service.isHiddenWorkFlowTask(new SessionAwareAsyncCallback<Boolean>() {

            @Override
            public void onSuccess(Boolean hideMe) {
                setHiddenWorkFlowTask(hideMe);

                service.isHiddenDSCTask(new SessionAwareAsyncCallback<Boolean>() {

                    @Override
                    public void onSuccess(Boolean hideMeToo) {
                        setHiddenDSCTask(hideMeToo);

                        if (!isHiddenWorkFlowTask() || !isHiddenDSCTask()) {
                            BasePortlet portlet = new TaskPortlet(MainFramePanel.this);
                            portlets.add(portlet);
                            MainFramePanel.this.add(portlet);
                        } else {
                            if (isEnterprise) {
                                int index = default_index_ordering.indexOf(WelcomePortal.TASKS);
                                initializePortlet(default_index_ordering.get(index + 1));
                            } else {
                                initDatabaseWithPortalSettings();
                            }
                        }

                    }
                });
            }
        });

    }

    // Store portal config values after all portlet initialized
    private void initDatabaseWithPortalSettings() {
        portletToVisibilities = new HashMap<String, Boolean>();
        portletToAutoOnOffs = new HashMap<String, Boolean>();
        for (String name : portletToLocations.keySet()) {
            portletToVisibilities.put(name, true);
            if (!name.equals(WelcomePortal.START) && !name.equals(WelcomePortal.SEARCH)) {
                portletToAutoOnOffs.put(name, false);
            }
        }
        // now we have all available portlets, need to record them in Actions panel in General project
        Map<String, Boolean> portletConfigs = new HashMap<String, Boolean>(portletToLocations.size());
        for (String name : portletToLocations.keySet()) {
            portletConfigs.put(name, portletToVisibilities.get(name));
        }

        props.add(PortalProperties.KEY_PORTLET_LOCATIONS, portletToLocations.toString());
        props.add(PortalProperties.KEY_PORTLET_VISIBILITIES, portletToVisibilities.toString());
        props.add(PortalProperties.KEY_COLUMN_NUM, ((Integer) MainFramePanel.this.numColumns).toString());
        if (isEnterprise) {
            allCharts = new HashSet<String>(portletToLocations.keySet());
            allCharts.retainAll(getDefaultChartNames());
            props.add(PortalProperties.KEY_ALL_CHARTS, allCharts.toString());
            props.add(PortalProperties.KEY_CHARTS_ON, ((Boolean) chartsOn).toString());
        }
        props.add(PortalProperties.KEY_AUTO_ONOFFS, portletToAutoOnOffs.toString());

        service.savePortalConfig(props, new SessionAwareAsyncCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                return;
            }

            @Override
            protected void doOnFailure(Throwable caught) {
                super.doOnFailure(caught);
                return;
            }
        });

        String portletConfigsStr;
        portletConfigs.put(USING_DEFAULT_COLUMN_NUM, MainFramePanel.this.numColumns == getDefaultColumNum());
        if (isEnterprise) {
            portletConfigs.put(CHARTS_ENABLED, chartsOn);
            portletConfigsStr = portletConfigs.toString() + allCharts.toString();
        } else {
            portletConfigsStr = portletConfigs.toString();
        }
        recordPortalConfigs(portletConfigsStr);
    }

    private Set<String> getDefaultChartNames() {
        return new HashSet<String>(Arrays.asList(NAME_CHART_DATA, NAME_CHART_ROUTING_EVENT, NAME_CHART_JOURNAL,
                NAME_CHART_MATCHING));
    }

    public native void openWindow(String url)/*-{
		window.open(url);
    }-*/;

    public native void initUI(String context, String application)/*-{
		$wnd.setTimeout(function() {
			$wnd.amalto[context][application].init();
		}, 50);
    }-*/;

    // record available portlets in Actions panel
    private native void recordPortalConfigs(String configs)/*-{
		$wnd.amalto.core.markPortlets(configs);
    }-*/;
}