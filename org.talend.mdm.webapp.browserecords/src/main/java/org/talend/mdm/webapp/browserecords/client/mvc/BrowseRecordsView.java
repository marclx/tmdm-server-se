// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.webapp.browserecords.client.mvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.talend.mdm.webapp.base.shared.SimpleTypeModel;
import org.talend.mdm.webapp.base.shared.TypeModel;
import org.talend.mdm.webapp.browserecords.client.BrowseRecords;
import org.talend.mdm.webapp.browserecords.client.BrowseRecordsEvents;
import org.talend.mdm.webapp.browserecords.client.creator.CellEditorCreator;
import org.talend.mdm.webapp.browserecords.client.creator.CellRendererCreator;
import org.talend.mdm.webapp.browserecords.client.model.ForeignKeyModel;
import org.talend.mdm.webapp.browserecords.client.model.ItemBean;
import org.talend.mdm.webapp.browserecords.client.util.Locale;
import org.talend.mdm.webapp.browserecords.client.util.UserSession;
import org.talend.mdm.webapp.browserecords.client.util.ViewUtil;
import org.talend.mdm.webapp.browserecords.client.widget.BreadCrumb;
import org.talend.mdm.webapp.browserecords.client.widget.GenerateContainer;
import org.talend.mdm.webapp.browserecords.client.widget.ItemDetailToolBar;
import org.talend.mdm.webapp.browserecords.client.widget.ItemPanel;
import org.talend.mdm.webapp.browserecords.client.widget.ItemsDetailPanel;
import org.talend.mdm.webapp.browserecords.client.widget.ItemsListPanel;
import org.talend.mdm.webapp.browserecords.client.widget.ItemsSearchContainer;
import org.talend.mdm.webapp.browserecords.client.widget.ItemsToolBar;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.creator.FieldCreator;
import org.talend.mdm.webapp.browserecords.client.widget.treedetail.ForeignKeyListWindow;
import org.talend.mdm.webapp.browserecords.client.widget.treedetail.ForeignKeyTreeDetail;
import org.talend.mdm.webapp.browserecords.shared.ComplexTypeModel;
import org.talend.mdm.webapp.browserecords.shared.EntityModel;
import org.talend.mdm.webapp.browserecords.shared.ViewBean;
import org.talend.mdm.webapp.browserecords.shared.VisibleRuleResult;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.mvc.AppEvent;
import com.extjs.gxt.ui.client.mvc.Controller;
import com.extjs.gxt.ui.client.mvc.Dispatcher;
import com.extjs.gxt.ui.client.mvc.View;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.grid.CellEditor;
import com.extjs.gxt.ui.client.widget.grid.CheckBoxSelectionModel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;

/**
 * DOC Administrator class global comment. Detailled comment
 */
public class BrowseRecordsView extends View {

    public BrowseRecordsView(Controller controller) {
        super(controller);
    }

    @Override
    protected void handleEvent(AppEvent event) {
        if (event.getType() == BrowseRecordsEvents.InitFrame) {
            onInitFrame(event);
        } else if (event.getType() == BrowseRecordsEvents.InitSearchContainer) {
            onInitSearchContainer(event);
        } else if (event.getType() == BrowseRecordsEvents.GetView) {
            onGetView(event);
        } else if (event.getType() == BrowseRecordsEvents.SearchView) {
            onSearchView(event);
        } else if (event.getType() == BrowseRecordsEvents.CreateForeignKeyView) {
            onCreateForeignKeyView(event);
        } else if (event.getType() == BrowseRecordsEvents.SelectForeignKeyView) {
            onSelectForeignKeyView(event);
        } else if (event.getType() == BrowseRecordsEvents.ViewItem) {
            onViewItem(event);
        } else if (event.getType() == BrowseRecordsEvents.ViewForeignKey) {
            onViewForeignKey(event);
        } else if (event.getType() == BrowseRecordsEvents.UpdatePolymorphism) {
            onUpdatePolymorphism(event);
        } else if (event.getType() == BrowseRecordsEvents.ExecuteVisibleRule) {
            onExecuteVisibleRule(event);
        }
    }

    private void onExecuteVisibleRule(AppEvent event) {
        ItemPanel itemPanel = ItemsDetailPanel.getInstance().getCurrentItemPanel();
        if (itemPanel != null) {
            itemPanel.onExecuteVisibleRule((List<VisibleRuleResult>) event.getData());
        }
    }

    private void onUpdatePolymorphism(AppEvent event) {
        ItemPanel itemPanel = ItemsDetailPanel.getInstance().getCurrentItemPanel();
        if (itemPanel != null) {
            itemPanel.onUpdatePolymorphism((ComplexTypeModel) event.getData());
        }
    }

    private void onViewForeignKey(AppEvent event) {
        ForeignKeyModel model = event.getData();
        ForeignKeyTreeDetail tree = new ForeignKeyTreeDetail(model, false);
        ItemsDetailPanel.getInstance().addTabItem(model.getViewBean().getBindingEntityModel().getConceptName(), tree,
                ItemsDetailPanel.MULTIPLE, model.getViewBean().getDescription());
    }

    private void onViewItem(AppEvent event) {
        ItemBean item = (ItemBean) event.getData();
        ItemsDetailPanel.getInstance().clearContent();
        ItemsDetailPanel.getInstance().initBanner(item.getPkInfoList());
        String operation = ItemDetailToolBar.VIEW_OPERATION;
        if (item.getSmartViewMode().equals(ItemBean.PERSOMODE))
            operation = ItemDetailToolBar.PERSONALEVIEW_OPERATION;
        else if (item.getSmartViewMode().equals(ItemBean.SMARTMODE))
            operation = ItemDetailToolBar.SMARTVIEW_OPERATION;
        ViewBean viewBean = (ViewBean) BrowseRecords.getSession().get(UserSession.CURRENT_VIEW);
        ItemPanel itemPanel = new ItemPanel(viewBean, item, operation);
        ItemsDetailPanel.getInstance().addTabItem(item.getConcept(), itemPanel, ItemsDetailPanel.SINGLETON, "itemView"); //$NON-NLS-1$

        // show breadcrumb

        ItemsDetailPanel.getInstance().clearBreadCrumb();
        if (item != null) {
            // List<String> breads = new ArrayList<String>();
            // breads.add(BreadCrumb.DEFAULTNAME);
            // breads.add(item.getConcept());
            // breads.add(item.getIds());
            Map<String, String> breads = new LinkedHashMap<String, String>();
            breads.put(BreadCrumb.DEFAULTNAME, null);
            breads.put(item.getConcept(), null);
            breads.put(item.getIds(), item.getConcept());
            ItemsDetailPanel.getInstance().initBreadCrumb(new BreadCrumb(breads));
        }
    }

    private void onSelectForeignKeyView(AppEvent event) {
        ViewBean viewBean = event.getData();
        ForeignKeyListWindow fkListWindow = (ForeignKeyListWindow) event.getSource();
        fkListWindow.show(viewBean);
    }

    private void onCreateForeignKeyView(AppEvent event) {
        ViewBean viewBean = event.getData();
        ForeignKeyTreeDetail tree = new ForeignKeyTreeDetail(viewBean, true);
        ItemsDetailPanel.getInstance().addTabItem(viewBean.getBindingEntityModel().getConceptName(), tree,
                ItemsDetailPanel.MULTIPLE, viewBean.getDescription());

    }

    private void onInitFrame(AppEvent event) {
        if (Log.isInfoEnabled())
            Log.info("Init frame... ");//$NON-NLS-1$
        GenerateContainer.getContentPanel().setLayout(new FitLayout());
        GenerateContainer.getContentPanel().setStyleAttribute("height", "100%");//$NON-NLS-1$ //$NON-NLS-2$       
        Dispatcher.forwardEvent(BrowseRecordsEvents.InitSearchContainer);
    }

    protected void onInitSearchContainer(AppEvent ae) {
        // create search panel
        if (Log.isInfoEnabled())
            Log.info("Init items-search-container... ");//$NON-NLS-1$
        GenerateContainer.getContentPanel().add(ItemsSearchContainer.getInstance());
        GenerateContainer.getContentPanel().layout(true);
    }

    protected void onGetView(final AppEvent event) {
        ViewBean viewBean = event.getData();
        ItemsToolBar.getInstance().updateToolBar(viewBean);
        // itemsSearchContainer.getItemsListPanel().resetShowItemTimes();
    }

    protected void onSearchView(final AppEvent event) {
        ViewBean viewBean = event.getData();
        EntityModel entityModel = viewBean.getBindingEntityModel();

        // TODO update columns
        List<ColumnConfig> ccList = new ArrayList<ColumnConfig>();
        CheckBoxSelectionModel<ItemBean> sm = new CheckBoxSelectionModel<ItemBean>();
        sm.setSelectionMode(SelectionMode.MULTI);
        ccList.add(sm.getColumn());
        List<String> viewableXpaths = viewBean.getViewableXpaths();
        Map<String, TypeModel> dataTypes = entityModel.getMetaDataTypes();
        List<String> keys = Arrays.asList(entityModel.getKeys());
        for (String xpath : viewableXpaths) {
            TypeModel typeModel = dataTypes.get(xpath);

            ColumnConfig cc = new ColumnConfig(xpath, typeModel == null ? xpath : ViewUtil.getViewableLabel(Locale.getLanguage(),
                    typeModel), 200);
            if (typeModel instanceof SimpleTypeModel && !keys.contains(xpath)) {
                Field<?> field = FieldCreator.createField((SimpleTypeModel) typeModel, null, false, Locale.getLanguage());

                CellEditor cellEditor = CellEditorCreator.createCellEditor(field);
                if (cellEditor != null) {
                    cc.setEditor(cellEditor);
                }
            }

            if (typeModel != null) {
                GridCellRenderer<ModelData> renderer = CellRendererCreator.createRenderer(typeModel, xpath);
                if (renderer != null) {
                    cc.setRenderer(renderer);
                }
            }

            ccList.add(cc);
        }

        ItemsListPanel.getInstance().updateGrid(sm, ccList);
        // itemsSearchContainer.getItemsListPanel().resetShowItemTimes();
        // TODO in the view of ViewItemForm binding

    }

}
