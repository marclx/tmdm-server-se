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
package org.talend.mdm.webapp.general.client.layout;

import java.util.Iterator;
import java.util.List;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.client.exception.LicenseUserNumberValidationException;
import org.talend.mdm.webapp.base.client.util.UrlUtil;
import org.talend.mdm.webapp.general.client.General;
import org.talend.mdm.webapp.general.client.GeneralServiceAsync;
import org.talend.mdm.webapp.general.client.i18n.MessageFactory;
import org.talend.mdm.webapp.general.client.resources.icon.Icons;
import org.talend.mdm.webapp.general.model.GroupItem;
import org.talend.mdm.webapp.general.model.MenuBean;
import org.talend.mdm.webapp.general.model.MenuGroup;

import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.WidgetComponent;
import com.extjs.gxt.ui.client.widget.layout.AccordionLayout;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HTML;

public class AccordionMenus extends ContentPanel {

    private static final String CHECKMENUID_WELCOME_PORTAL = "welcomeportal.WelcomePortal"; //$NON-NLS-1$

    private static AccordionMenus instance;

    private HTMLMenuItem activeItem;

    private HTMLMenuItem welcomeportalItem;

    private GeneralServiceAsync service = (GeneralServiceAsync) Registry.get(General.OVERALL_SERVICE);

    private AccordionMenus() {
        super();
        this.setHeading(MessageFactory.getMessages().menus());
        this.addStyleName("menus-list"); //$NON-NLS-1$
        this.setLayout(new AccordionLayout());
        // this.setScrollMode(Scroll.AUTO);// add it for vertical ScrollBar
        // registerOpenPages();
    }

    public static AccordionMenus getInstance() {
        if (instance == null) {
            instance = new AccordionMenus();
        }
        return instance;
    }

    private void buildMenuGroup(ContentPanel menuPanel, MenuBean mb) {
        String toCheckMenuID = mb.getContext() + "." + mb.getApplication(); //$NON-NLS-1$
        String icon = makeImageIconPart(mb, toCheckMenuID);
        StringBuffer str = new StringBuffer();
        str.append("<span class='body'>"); //$NON-NLS-1$
        if ("workflowtasks.BonitaWorkflowTasks".equals(toCheckMenuID)) { //$NON-NLS-1$
            str.append("<img style='width: 16px;height: 16px;' src='" + icon + "'/>&nbsp;&nbsp;"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            str.append("<img src='" + icon + "'/>&nbsp;&nbsp;"); //$NON-NLS-1$ //$NON-NLS-2$    
        }
        str.append("<span class='desc'>" + mb.getName() + "</span></span>"); //$NON-NLS-1$ //$NON-NLS-2$
        HTMLMenuItem tempItem = new HTMLMenuItem(mb, str.toString());
        if (CHECKMENUID_WELCOME_PORTAL.equals(toCheckMenuID)) {
            welcomeportalItem = tempItem;
        }
        HTML html = tempItem;
        html.addClickHandler(clickHander);
        menuPanel.add(html);
    }

    private void setCollapsable(ContentPanel menuPanel) {
        menuPanel.setAnimCollapse(false);
        menuPanel.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.accordion()));
        menuPanel.setScrollMode(Scroll.AUTO);
        this.add(menuPanel);
    }

    public void initMenus(MenuGroup menuGroup) {
        List<MenuBean> menus = menuGroup.getMenuBean();
        for (GroupItem gi : menuGroup.getGroupItem()) {
            if ((null == gi.getMenuItems()) || (gi.getMenuItems().size() == 0)) {
                continue;
            }
            ContentPanel menuPanel = new ContentPanel();
            boolean hasMenuItem = false;
            for (String gi2 : gi.getMenuItems()) {
                MenuBean mb = getMenuBean(gi2, menus);
                if (null != mb) {
                    buildMenuGroup(menuPanel, mb);
                    hasMenuItem = true;
                }
            }
            if (hasMenuItem) {
                menuPanel.setHeading(gi.getGroupHeader());
                setCollapsable(menuPanel);
            }
        }

        ContentPanel otherPanel = new ContentPanel();
        otherPanel.setHeading(MessageFactory.getMessages().othermenu());
        boolean hasMiscMenus = false;
        for (MenuBean mb : menus) {
            if (!menuGroup.hasSpecifiedMenu(mb)) {
                buildMenuGroup(otherPanel, mb);
                hasMiscMenus = true;
            }
        }
        if (hasMiscMenus) {
            setCollapsable(otherPanel);
        }
        this.layout();
    }

    private MenuBean getMenuBean(String menuName, List<MenuBean> menus) {
        for (MenuBean mb : menus) {
            if ((mb.getContext() + "." + mb.getApplication()).equals(menuName)) {
                return mb;
            }
        }
        return null;
    }

    private String makeImageIconPart(MenuBean item, String toCheckMenuID) {
        String icon = null;
        if (item.getIcon() != null && item.getIcon().trim().length() != 0) {
            icon = "/imageserver/" + item.getIcon() + "?width=16&height=16"; //$NON-NLS-1$ //$NON-NLS-2$
        } else if ("itemsbrowser.ItemsBrowser".equals(toCheckMenuID) || "viewbrowser.ViewBrowser".equals(toCheckMenuID) || "itemsbrowser2.ItemsBrowser2".equals(toCheckMenuID) || "browserecords.BrowseRecords".equals(toCheckMenuID)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            icon = "/talendmdm/secure/img/menu/browse.png"; //$NON-NLS-1$
        } else if ("browserecords.BrowseRecordsInStaging".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/browsestaging.png"; //$NON-NLS-1$
        } else if ("crossreferencing.CrossReferencing".equals(toCheckMenuID) || "crossreference.CrossReference".equals(toCheckMenuID)) { //$NON-NLS-1$ //$NON-NLS-2$
            icon = "/talendmdm/secure/img/menu/crossref.png"; //$NON-NLS-1$
        } else if ("hierarchical.GroupingHierarchy".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/grouping_hier.png"; //$NON-NLS-1$
        } else if ("ehierarchical.DerivedHierarchy".equals(toCheckMenuID) || "hierarchy.Hierarchy".equals(toCheckMenuID)) { //$NON-NLS-1$ //$NON-NLS-2$
            icon = "/talendmdm/secure/img/menu/derived_hier.png"; //$NON-NLS-1$
        } else if ("usersandroles.Users".equals(toCheckMenuID) || "usermanager.UserManager".equals(toCheckMenuID)) { //$NON-NLS-1$ //$NON-NLS-2$
            icon = "/talendmdm/secure/img/menu/manage_users.png"; //$NON-NLS-1$
        } else if ("reporting.Reporting".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/reporting.png"; //$NON-NLS-1$
        } else if ("SynchronizationItem.SynchronizationItem".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/synchro_item.png"; //$NON-NLS-1$
        } else if ("SynchronizationAction.SynchronizationAction".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/synchronize.png"; //$NON-NLS-1$
        } else if ("ItemsTrash.ItemsTrash".equals(toCheckMenuID) || "recyclebin.RecycleBin".equals(toCheckMenuID)) { //$NON-NLS-1$ //$NON-NLS-2$
            icon = "/talendmdm/secure/img/menu/trash.png"; //$NON-NLS-1$
        } else if ("journal.Journal".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/updatereport.png"; //$NON-NLS-1$
        } else if ("workflowtasks.WorkflowTasks".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/workflowtasks.png"; //$NON-NLS-1$
        } else if ("workflowtasks.BonitaWorkflowTasks".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/bonita_user_xp.png"; //$NON-NLS-1$
        } else if ("license.License".equals(toCheckMenuID) || "licenseManager.LicenseManager".equals(toCheckMenuID)) { //$NON-NLS-1$ //$NON-NLS-2$
            icon = "/talendmdm/secure/img/menu/license.png"; //$NON-NLS-1$
        } else if ("datastewardship.Datastewardship".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/stewardship.png"; //$NON-NLS-1$
        } else if ("search.Search".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/find.png"; //$NON-NLS-1$
        } else if ("stagingarea.Stagingarea".equals(toCheckMenuID)) { //$NON-NLS-1$
            icon = "/talendmdm/secure/img/menu/stagingarea.png"; //$NON-NLS-1$
        } else {
            // default menus icon
            icon = "/talendmdm/secure/img/menu/default.gif"; //$NON-NLS-1$
        }
        return icon;
    }

    public void selectedItem(HTMLMenuItem item) {
        if (activeItem == item) {
            return;
        }
        if (activeItem != null) {
            activeItem.removeStyleName("selected"); //$NON-NLS-1$
        }
        item.addStyleName("selected"); //$NON-NLS-1$
        activeItem = item;
    }

    public void disabledMenuItem(String context, String application, boolean disabled) {
        Iterator<Component> groupIter = this.iterator();
        while (groupIter.hasNext()) {
            ContentPanel panel = (ContentPanel) groupIter.next();
            Iterator<Component> itemIter = panel.iterator();
            while (itemIter.hasNext()) {
                Component comp = itemIter.next();
                if (comp instanceof WidgetComponent) {
                    WidgetComponent widgetComp = (WidgetComponent) comp;
                    if (widgetComp.getWidget() instanceof HTMLMenuItem) {
                        HTMLMenuItem item = (HTMLMenuItem) widgetComp.getWidget();
                        MenuBean menuBean = item.getMenuBean();
                        if (context.equals(menuBean.getContext()) && application.equals(menuBean.getApplication())) {
                            item.setDisabled(disabled);
                        }
                    }
                }
            }
        }
    }

    ClickHandler clickHander = new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
            final HTMLMenuItem item = (HTMLMenuItem) event.getSource();
            final MenuBean menuBean = item.getMenuBean();
            if (menuBean.isDisabled()) {
                MessageBox.alert(null, menuBean.getDisabledDesc(), null);
                return;
            }
            if (!menuBean.getContext().toLowerCase().equals("licensemanager")) {
                service.isExpired(UrlUtil.getLanguage(), new SessionAwareAsyncCallback<Boolean>() {

                    @Override
                    public void onSuccess(Boolean result) {
                        if (!result) {
                            clickMenu(menuBean, item);
                        }
                    }

                    @Override
                    protected void doOnFailure(final Throwable caught) {
                        if (menuBean.getContext().toLowerCase().equals("usermanager") && //$NON-NLS-1$
                                caught != null && caught instanceof LicenseUserNumberValidationException) {
                            clickMenu(menuBean, item);
                        } else {
                            super.doOnFailure(caught);
                        }
                    }

                });
            } else {
                clickMenu(menuBean, item);
            }
        }
    };

    private void clickMenu(MenuBean menuBean, HTMLMenuItem item) {
        boolean succeeful = initUI(menuBean.getContext(), menuBean.getApplication(), MessageFactory.getMessages()
                .application_undefined(menuBean.getContext() + "." + menuBean.getApplication())); //$NON-NLS-1$
        if (succeeful) {
            selectedItem(item);
        }
    }

    private native boolean initUI(String context, String application, String errorMsg)/*-{
		if ($wnd.amalto[context]) {
			if ($wnd.amalto[context][application]) {
				$wnd.amalto[context][application].init();
				return true;
			} else {
				$wnd.alert(errorMsg);
			}
		} else {
			$wnd.alert(errorMsg);
		}
		return false;
    }-*/;

    class HTMLMenuItem extends HTML {

        MenuBean menuBean;

        public HTMLMenuItem(MenuBean menuBean, String html) {
            super(html);
            this.setWordWrap(false);
            this.setStyleName("menu-item"); //$NON-NLS-1$
            this.menuBean = menuBean;
            setDisabled(menuBean.isDisabled());
            this.getElement().setAttribute("id", "menu-" + menuBean.getContext()); //$NON-NLS-1$//$NON-NLS-2$
        }

        public void setDisabled(boolean disabled) {
            menuBean.setDisabled(disabled);
            if (disabled) {
                this.addStyleName("x-item-disabled"); //$NON-NLS-1$
                this.setTitle(menuBean.getDisabledDesc());
            } else {
                this.removeStyleName("x-item-disabled"); //$NON-NLS-1$
                this.setTitle(null);
            }
        }

        public MenuBean getMenuBean() {
            return menuBean;
        }
    }

    interface GetUrl {

        void getUrl(String url);
    }

    public HTMLMenuItem getWelcomeportalItem() {
        return this.welcomeportalItem;
    }
}
