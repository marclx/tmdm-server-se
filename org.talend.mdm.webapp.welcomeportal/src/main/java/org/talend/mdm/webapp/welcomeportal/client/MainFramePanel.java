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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private static final String USING_DEFAULT_COLUMN_NUM = "defaultColNum"; //$NON-NLS-1$

    private static final int DEFAULT_COLUMN_NUM = 3;

    private static final int ALTERNATIVE_COLUMN_NUM = 2;

    private static final Set<String> nonAutoedPortlets = new HashSet<String>(Arrays.asList(WelcomePortal.START,
            WelcomePortal.SEARCH));

    private Set<String> allPortlets;

    private boolean startedAsOn;

    private boolean isEnterprise;

    private int interval;

    private boolean hiddenWorkFlowTask;

    private boolean hiddenDSCTask;

    private List<BasePortlet> portlets;

    private int pos;

    private int numColumns;

    private Map<String, List<Integer>> portletToLocations;

    private Map<String, Boolean> portletToAutoOnOffs;

    // config updates from ActionPanel
    private List<String> userConfigs;

    // db config cache
    private PortalProperties props;

    private List<String> default_index_ordering;

    private WelcomePortalServiceAsync service = (WelcomePortalServiceAsync) Registry.get(WelcomePortal.WELCOMEPORTAL_SERVICE);

    public MainFramePanel(int colNum, PortalProperties portalConfig, boolean isEnterpriseVersion) {
        this(colNum, portalConfig, null, isEnterpriseVersion);
    }

    public MainFramePanel(int colNum, PortalProperties portalConfig, List<String> userConfig, boolean isEnterpriseVersion) {
        super(colNum);

        numColumns = colNum;
        isEnterprise = isEnterpriseVersion;
        props = portalConfig;
        userConfigs = userConfig;

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
                    markPortalConfigsOnUI(getConfigsForUser());
                }
            }

        });

        this.addListener(Events.Drop, new Listener<PortalEvent>() {

            @Override
            public void handleEvent(PortalEvent pe) {

                updateLocations();
                removeAllPortlets();
                sortPortletsAndLocations();
                refresh();
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

        service.getWelcomePortletConfig(new SessionAwareAsyncCallback<Map<Boolean, Integer>>() {

            @Override
            public void onSuccess(Map<Boolean, Integer> config) {
                boolean startedAsOnCp = startedAsOn;
                if (!config.containsKey(startedAsOnCp)) {
                    startedAsOnCp = !startedAsOnCp;
                }

                interval = config.get(startedAsOnCp);
                default_index_ordering = getDefaultPortletOrdering(isEnterprise);

                initializePortlets();
            }

            @Override
            public void doOnFailure(Throwable e) {
                startedAsOn = DEFAULT_REFRESH_STARTASON;
                interval = DEFAULT_REFRESH_INTERVAL;
                default_index_ordering = getDefaultPortletOrdering(isEnterprise);

                initializePortlets();
            }

        });
    }

    public List<BasePortlet> getPortlets() {
        return this.portlets;
    }

    public int getColumnConfig() {
        return this.numColumns;
    }

    private String getConfigsForUser() {
        return allPortlets.toString() + "; " + portletToLocations.keySet().toString() + "; " //$NON-NLS-1$ //$NON-NLS-2$
                + ((Integer) MainFramePanel.this.numColumns).toString();
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
        } else if (userConfigs == null) {// login: init with configs in db
            portletToLocations = props.getPortletToLocations();
            allPortlets = props.getAllPortlets();
            initializePortlets(portletToLocations);
            markPortalConfigsOnUI(getConfigsForUser());
        } else {
            // switch column config
            final PortalProperties portalPropertiesCp = new PortalProperties(props);
            portletToLocations = props.getPortletToLocations();
            Set<String> portletsExist = portletToLocations.keySet();
            portletsExist.retainAll(userConfigs);
            Map<String, List<Integer>> portletLocsExist = new LinkedHashMap<String, List<Integer>>(userConfigs.size());
            for (String nameExists : portletsExist) {
                portletLocsExist.put(nameExists, portletToLocations.get(nameExists));
            }

            SortedMap<List<Integer>, String> locationToPortlets = new TreeMap<List<Integer>, String>(getLocationComparator());
            for (String portletName : portletLocsExist.keySet()) {
                locationToPortlets.put(portletLocsExist.get(portletName), portletName);
            }

            List<String> orderedPortletNames = new ArrayList<String>(portletToLocations.size());
            // previous ordering based on locs
            orderedPortletNames.addAll(locationToPortlets.values());

            // add the new portlets
            List<String> namesToCreate = new ArrayList<String>(userConfigs);
            namesToCreate.removeAll(orderedPortletNames);
            orderedPortletNames.addAll(namesToCreate);

            List<List<Integer>> defaultLocations = getDefaultLocations(numColumns);
            int index = 0;
            List<Integer> loc;
            for (String name : orderedPortletNames) {
                loc = defaultLocations.get(index++);
                initializePortlet(name, loc);
            }

            updateLocations();

            props.add(PortalProperties.KEY_PORTLET_LOCATIONS, portletToLocations.toString());
            props.add(PortalProperties.KEY_COLUMN_NUM, ((Integer) numColumns).toString());

            service.savePortalConfig(props, new SessionAwareAsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    markPortalConfigsOnUI(getConfigsForUser());
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
    private void initializePortlets(Map<String, List<Integer>> locs) {

        SortedMap<List<Integer>, String> locationToPortlets = new TreeMap<List<Integer>, String>(getLocationComparator());
        for (String portletName : locs.keySet()) {
            locationToPortlets.put(locs.get(portletName), portletName);
        }
        String portletName;
        for (List<Integer> loc : locationToPortlets.keySet()) {
            portletName = locationToPortlets.get(loc);
            initializePortlet(portletName, loc);
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

    private void initializePortletOnly(String portletName) {
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

        portlets.add(portlet);
    }

    private void initializePortlet(String portletName, List<Integer> loc) {
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
            add(portlet, loc);
        }
    }

    // called when db saving fails for 'Drop' event
    private void revertPortletsToLocations(Map<String, List<Integer>> locs) {

        SortedMap<List<Integer>, String> locationToPortlets = new TreeMap<List<Integer>, String>(getLocationComparator());
        for (String portletName : locs.keySet()) {
            locationToPortlets.put(locs.get(portletName), portletName);
        }

        for (BasePortlet portlet : portlets) {
            remove(portlet);

        }

        for (List<Integer> loc : locationToPortlets.keySet()) {
            Portlet portlet = getPortlet(locationToPortlets.get(loc));
            assert (portlet != null);
            add(portlet, loc);
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

    public void updateLocations() {
        portletToLocations = new HashMap<String, List<Integer>>(portlets.size());

        int column;
        int row;
        for (BasePortlet portlet : portlets) {
            column = this.getPortletColumn(portlet);
            row = this.getPortletIndex(portlet);

            portletToLocations.put(portlet.getPortletName(), Arrays.asList(column, row));
        }
    }

    public Map<String, List<Integer>> getUpdatedLocations() {
        updateLocations();
        return portletToLocations;
    }

    public void refreshPortlets() {
        for (BasePortlet portlet : portlets) {
            portlet.refresh();
        }
    }

    public void stopAutoRefresh() {
        for (BasePortlet portlet : portlets) {
            if (!nonAutoedPortlets.contains(portlet.getPortletName()) && portlet.isAutoOn()) {
                portlet.autoRefresh(false);
            }
        }
    }

    public Set<String> getAllPortletNames() {
        return portletToLocations.keySet();
    }

    public void removeAllPortlets() {
        for (BasePortlet portlet : portlets) {
            remove(portlet);
        }
    }

    public void remove(BasePortlet portlet) {
        int col = portletToLocations.get(portlet.getPortletName()).get(0);
        this.remove(portlet, col);
    }

    private List<String> parseString(String names) {
        String temp = names.substring(1, names.length() - 1);
        String[] namesParsed = temp.split(", "); //$NON-NLS-1$

        return Arrays.asList(namesParsed);
    }

    // add all portlets into Portal
    private void addPortlets() {
        List<List<Integer>> locs = getDefaultLocations(numColumns);
        int index = 0;
        for (Portlet portlet : portlets) {
            add(portlet, locs.get(index++));
        }
    }

    private void sortPortletsAndLocations() {
        assert (portletToLocations.keySet().equals(new HashSet<String>(getPortletNames())));

        List<BasePortlet> sorted = new ArrayList<BasePortlet>(portlets.size());

        SortedMap<List<Integer>, String> locationToPortlets = new TreeMap<List<Integer>, String>(getLocationComparator());
        for (String portletName : portletToLocations.keySet()) {
            locationToPortlets.put(portletToLocations.get(portletName), portletName);
        }

        portletToLocations.clear();
        for (Entry<List<Integer>, String> entry : locationToPortlets.entrySet()) {
            sorted.add((BasePortlet) getPortlet(entry.getValue()));
            portletToLocations.put(entry.getValue(), entry.getKey());
        }

        portlets.clear();
        portlets = sorted;
    }

    // For basic config change (checked/unchecked update, no column number change)
    // triggered from Action Panel
    public void refresh(final List<String> userConfigUpdates) {

        removeAllPortlets();

        final Set<BasePortlet> toDestroy = updatePortlets(new HashSet<String>(userConfigUpdates));

        refresh();

        service.savePortalConfig(PortalProperties.KEY_PORTLET_LOCATIONS, portletToLocations.toString(),
                new SessionAwareAsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        props.add(PortalProperties.KEY_PORTLET_LOCATIONS, portletToLocations.toString());
                        MainFramePanel.this.layout(true);

                        for (BasePortlet portlet : toDestroy) {
                            portlet.autoRefresh(false);
                            portlet.removeAllListeners();
                            portlet.removeAll();
                        }
                    }

                    @Override
                    protected void doOnFailure(Throwable caught) {
                        // revert to previous portlets and marks on Action Panel
                        super.doOnFailure(caught);
                        removeAllPortlets();
                        portletToLocations = props.getPortletToLocations();

                        // newly created if success, now need to revert them
                        Set<String> namesToDestroy = new HashSet<String>(userConfigUpdates);
                        namesToDestroy.removeAll(portletToLocations.keySet());
                        removeFromPortlets(namesToDestroy);

                        // destoryed if success, now need to add them back
                        addToPortlets(toDestroy);

                        sortPortletsAndLocations();

                        refresh();

                        MainFramePanel.this.layout(true);

                        markPortalConfigsOnUI(getConfigsForUser());
                    }
                });
    }

    public void refresh() {
        addPortlets();
        updateLocations();
    }

    public void render() {
        layout(true);
    }

    private void addToPortlets(Set<BasePortlet> portletsToAdd) {
        portlets.addAll(portletsToAdd);
    }

    private void removeFromPortlets(Set<String> namesToDestroy) {
        Iterator<BasePortlet> iterator = portlets.iterator();
        BasePortlet portlet;
        while (iterator.hasNext()) {
            portlet = iterator.next();

            if (namesToDestroy.contains(portlet.getPortletName())) {
                iterator.remove();
                portlet.autoRefresh(false);
                portlet.removeAllListeners();
                portlet.removeAll();
            }
        }
    }

    private Set<BasePortlet> updatePortlets(Set<String> userConfigUpdates) {
        List<String> namePortletsToDestroy = new ArrayList<String>(portletToLocations.keySet());
        namePortletsToDestroy.removeAll(userConfigUpdates);

        List<String> portletAlreadyExist = new ArrayList<String>(portletToLocations.keySet());
        List<String> portletsToCreate = new ArrayList<String>(userConfigUpdates);
        portletsToCreate.removeAll(portletAlreadyExist);

        Iterator<BasePortlet> iterator = portlets.iterator();
        BasePortlet portlet;
        Set<BasePortlet> toDestroy = new HashSet<BasePortlet>(namePortletsToDestroy.size());
        while (iterator.hasNext()) {
            portlet = iterator.next();

            if (namePortletsToDestroy.contains(portlet.getPortletName())) {
                iterator.remove();
                toDestroy.add(portlet);
            }
        }
        for (String name : portletsToCreate) {
            initializePortletOnly(name);
        }

        return toDestroy;
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

    public Map<String, List<Integer>> getPortletToLocations() {
        return this.portletToLocations;
    }

    public void setPortletToLocations(Map<String, List<Integer>> portletToLocations) {
        this.portletToLocations = portletToLocations;
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
                                markPortalConfigsOnUI(getConfigsForUser());
                            }
                        }

                    }
                });
            }
        });

    }

    // Store portal config values after all portlet initialized
    private void initDatabaseWithPortalSettings() {
        portletToAutoOnOffs = new HashMap<String, Boolean>();
        for (String name : portletToLocations.keySet()) {
            if (!name.equals(WelcomePortal.START) && !name.equals(WelcomePortal.SEARCH)) {
                portletToAutoOnOffs.put(name, false);
            }
        }

        allPortlets = new HashSet<String>(portlets.size());
        for (BasePortlet portlet : portlets) {
            allPortlets.add(portlet.getPortletName());
        }

        props.add(PortalProperties.KEY_PORTLET_LOCATIONS, portletToLocations.toString());
        props.add(PortalProperties.KEY_COLUMN_NUM, ((Integer) MainFramePanel.this.numColumns).toString());
        props.add(PortalProperties.KEY_ALL_PORTLETS, allPortlets.toString());
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
    private native void markPortalConfigsOnUI(String configs)/*-{
		$wnd.amalto.core.markPortlets(configs);
    }-*/;

}