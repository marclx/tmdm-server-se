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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

@SuppressWarnings("nls")
public class PortalPropertiesTest extends TestCase {

    private static String PORTLET_TO_LOC_STRING = "{portlet1=[0, 0], portlet2=[1, 0], portlet3=[0, 1], portlet4=[1, 1], portlet5=[0, 2], portlet6=[1, 2], portlet7=[0, 3], portlet8=[1, 3], portlet9=[0, 4]}";

    private String PORTLET_VISIBILITIES_STRING = "{portlet1=true, portlet2=false, portlet3=true, portlet4=false, portlet5=true, portlet6=false, portlet7=true, portlet8=false, portlet9=true}";

    private String CHART_SETTINGS_STRING = "{chart1=day, chart2=5, chart3=all, chart4=all}";

    private String CHARTSON_STRING = "false";

    private String COLUMN_NUM_STRING = "3";

    private String ALL_CHARTS_STRING = "[chart1, chart2, chart3, chart4]";

    private String AUTO_ONOFFS_STRING = "{portlet1=true, portlet2=false, portlet3=true, portlet4=false, portlet5=true, portlet6=false, portlet7=true, portlet8=false, portlet9=true}";

    Map<String, String> propsWithValues;

    Map<String, String> propsWithNulls;

    PortalProperties props;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        propsWithValues = new HashMap<String, String>();
        propsWithValues.put(PortalProperties.KEY_PORTLET_LOCATIONS, PORTLET_TO_LOC_STRING);
        propsWithValues.put(PortalProperties.KEY_CHARTS_ON, CHARTSON_STRING);
        propsWithValues.put(PortalProperties.KEY_COLUMN_NUM, COLUMN_NUM_STRING);
        propsWithValues.put(PortalProperties.KEY_PORTLET_VISIBILITIES, PORTLET_VISIBILITIES_STRING);
        propsWithValues.put(PortalProperties.KEY_ALL_CHARTS, ALL_CHARTS_STRING);
        propsWithValues.put(PortalProperties.KEY_AUTO_ONOFFS, AUTO_ONOFFS_STRING);
        propsWithValues.put(PortalProperties.KEY_CHART_SETTINGS, CHART_SETTINGS_STRING);

        propsWithNulls = new HashMap<String, String>();
        propsWithNulls.put(PortalProperties.KEY_PORTLET_LOCATIONS, null);
        propsWithNulls.put(PortalProperties.KEY_CHARTS_ON, null);
        propsWithNulls.put(PortalProperties.KEY_COLUMN_NUM, null);
        propsWithNulls.put(PortalProperties.KEY_PORTLET_VISIBILITIES, null);
        propsWithNulls.put(PortalProperties.KEY_ALL_CHARTS, null);
        propsWithNulls.put(PortalProperties.KEY_AUTO_ONOFFS, null);
        propsWithNulls.put(PortalProperties.KEY_CHART_SETTINGS, null);
    }

    public void testGetPortletToLocations() {
        props = new PortalProperties(propsWithValues);
        Map<String, List<Integer>> expected = getExpectedLocations();
        Map<String, List<Integer>> actual1 = props.getPortletToLocations();
        assertTrue(expected.equals(actual1));

        props = new PortalProperties(propsWithNulls);
        Map<String, List<Integer>> actual2 = props.getPortletToLocations();
        assertNull(actual2);
    }

    public void testGetPortletToVisibilities() {
        props = new PortalProperties(propsWithValues);
        Map<String, Boolean> expected = getExpectedPortletVisibles();
        Map<String, Boolean> actual1 = props.getPortletToVisibilities();
        assertTrue(expected.equals(actual1));

        props = new PortalProperties(propsWithNulls);
        Map<String, Boolean> actual2 = props.getPortletToVisibilities();
        assertNull(actual2);
    }

    public void testGetAllCharts() {
        props = new PortalProperties(propsWithValues);
        Set<String> expected = getExpectedAllCharts(ALL_CHARTS_STRING);
        Set<String> actual1 = props.getAllCharts();
        assertTrue(expected.equals(actual1));

        props = new PortalProperties(propsWithNulls);
        Set<String> actual2 = props.getAllCharts();
        assertNull(actual2);
    }

    public void testGetChartsOn() {
        props = new PortalProperties(propsWithValues);
        Boolean expected = Boolean.parseBoolean(CHARTSON_STRING);
        Boolean actual1 = props.getChartsOn();
        assertTrue(expected.equals(actual1));

        props = new PortalProperties(propsWithNulls);
        Boolean actual2 = props.getChartsOn();
        assertNull(actual2);
    }

    public void testGetColumnNum() {
        props = new PortalProperties(propsWithValues);
        Integer expected = Integer.parseInt(COLUMN_NUM_STRING);
        Integer actual1 = props.getColumnNum();
        assertTrue(expected.equals(actual1));

        props = new PortalProperties(propsWithNulls);
        Integer actual2 = props.getColumnNum();
        assertNull(actual2);
    }

    public void testGetAutoRefreshStatusMap() {
        props = new PortalProperties(propsWithValues);
        Map<String, Boolean> expected = getExpectedAutoRefreshStatus();
        Map<String, Boolean> actual1 = props.getAutoRefreshStatus();
        assertTrue(expected.equals(actual1));

        props = new PortalProperties(propsWithNulls);
        Map<String, Boolean> actual2 = props.getAutoRefreshStatus();
        assertNull(actual2);
    }

    public void testGetAutoRefreshStatus() {
        List<String> portlets = Arrays.asList("portlet1", "portlet2", "portlet3", "portlet4");
        props = new PortalProperties(propsWithValues);
        Boolean expected;
        Boolean actual1;
        for (String name : portlets) {
            expected = getExpectedAutoRefreshes(name.charAt(name.length() - 1));
            actual1 = props.getAutoRefreshStatus(name);
            assertTrue(expected.equals(actual1));
        }

        props = new PortalProperties(propsWithNulls);
        Boolean actual2 = props.getAutoRefreshStatus("portlet1");
        assertNull(actual2);

        propsWithNulls.put(PortalProperties.KEY_AUTO_ONOFFS, null);
        props = new PortalProperties(propsWithNulls);
        actual2 = props.getAutoRefreshStatus("portlet1");
        assertNull(actual2);
    }

    public void testGetChartSettings() {
        props = new PortalProperties(propsWithValues);
        Map<String, String> expected = getExpectedChartSettings();
        Map<String, String> actual1 = props.getChartSettings();
        assertTrue(expected.equals(actual1));

        props = new PortalProperties(propsWithNulls);
        Map<String, String> actual2 = props.getChartSettings();
        assertNull(actual2);
    }

    public void testDisableAutoRefresh() {
        Set<String> portletNames = new HashSet<String>(Arrays.asList("portlet1", "portlet3", "portlet5", "portlet7", "portlet9"));
        props = new PortalProperties(propsWithValues);

        props.disableAutoRefresh(portletNames);

        Boolean expected = false;
        Boolean actual;
        for (String name : portletNames) {
            actual = props.getAutoRefreshStatus(name);
            assertTrue(expected.equals(actual));
        }
    }

    private Boolean getExpectedAutoRefreshes(char index) {
        switch (index) {
        case '1':
            return true;
        case '2':
            return false;
        case '3':
            return true;
        case '4':
            return false;
        default:
            return null;

        }
    }

    private Map<String, List<Integer>> getExpectedLocations() {
        Map<String, List<Integer>> expects = new HashMap<String, List<Integer>>();
        expects.put("portlet1", setLocs(0, 0));
        expects.put("portlet2", setLocs(1, 0));
        expects.put("portlet3", setLocs(0, 1));
        expects.put("portlet4", setLocs(1, 1));
        expects.put("portlet5", setLocs(0, 2));
        expects.put("portlet6", setLocs(1, 2));
        expects.put("portlet7", setLocs(0, 3));
        expects.put("portlet8", setLocs(1, 3));
        expects.put("portlet9", setLocs(0, 4));
        return expects;
    }

    private List<Integer> setLocs(int i, int j) {
        List<Integer> loc = new ArrayList<Integer>(2);
        loc.add(i);
        loc.add(j);
        return loc;
    }

    private Map<String, Boolean> getExpectedPortletVisibles() {
        Map<String, Boolean> expects = new HashMap<String, Boolean>();
        expects.put("portlet1", true);
        expects.put("portlet2", false);
        expects.put("portlet3", true);
        expects.put("portlet4", false);
        expects.put("portlet5", true);
        expects.put("portlet6", false);
        expects.put("portlet7", true);
        expects.put("portlet8", false);
        expects.put("portlet9", true);
        return expects;
    }

    private Map<String, Boolean> getExpectedAutoRefreshStatus() {
        Map<String, Boolean> expects = new HashMap<String, Boolean>();
        expects.put("portlet1", true);
        expects.put("portlet2", false);
        expects.put("portlet3", true);
        expects.put("portlet4", false);
        expects.put("portlet5", true);
        expects.put("portlet6", false);
        expects.put("portlet7", true);
        expects.put("portlet8", false);
        expects.put("portlet9", true);
        return expects;
    }

    private Set<String> getExpectedAllCharts(String allCharts) {
        Set<String> allChartsNames = new HashSet<String>();
        allChartsNames.add("chart1");
        allChartsNames.add("chart2");
        allChartsNames.add("chart3");
        allChartsNames.add("chart4");
        return allChartsNames;
    }

    private Map<String, String> getExpectedChartSettings() {
        Map<String, String> expects = new HashMap<String, String>();
        expects.put("chart1", "day");
        expects.put("chart2", "5");
        expects.put("chart3", "all");
        expects.put("chart4", "all");

        return expects;
    }

}
