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
package org.talend.mdm.webapp.browserecords.client.widget.treedetail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.shared.TypeModel;
import org.talend.mdm.webapp.browserecords.client.BrowseRecords;
import org.talend.mdm.webapp.browserecords.client.BrowseRecordsServiceAsync;
import org.talend.mdm.webapp.browserecords.client.i18n.MessagesFactory;
import org.talend.mdm.webapp.browserecords.client.model.ColumnElement;
import org.talend.mdm.webapp.browserecords.client.model.ColumnTreeLayoutModel;
import org.talend.mdm.webapp.browserecords.client.model.ColumnTreeModel;
import org.talend.mdm.webapp.browserecords.client.model.ItemBean;
import org.talend.mdm.webapp.browserecords.client.model.ItemNodeModel;
import org.talend.mdm.webapp.browserecords.client.util.CommonUtil;
import org.talend.mdm.webapp.browserecords.client.util.Locale;
import org.talend.mdm.webapp.browserecords.shared.ComplexTypeModel;
import org.talend.mdm.webapp.browserecords.shared.ViewBean;
import org.talend.mdm.webapp.browserecords.shared.VisibleRuleResult;

import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class TreeDetail extends ContentPanel {

    private ViewBean viewBean;

    private Tree tree = new Tree();

    private TreeItem root;

    private HashMap<CountMapItem, Integer> occurMap = new HashMap<CountMapItem, Integer>();

    private ForeignKeyRender fkRender;

    private ClickHandler handler = new ClickHandler() {

        public void onClick(ClickEvent arg0) {
            final DynamicTreeItem selected = (DynamicTreeItem) tree.getSelectedItem();
            final DynamicTreeItem parentItem = (DynamicTreeItem) selected.getParentItem();

            final String xpath = selected.getItemNodeModel().getBindingPath();
            final CountMapItem countMapItem = new CountMapItem(xpath, parentItem);
            final int count = occurMap.containsKey(countMapItem) ? occurMap.get(countMapItem) : 0;

            if ("Add".equals(arg0.getRelativeElement().getId()) || "Clone".equals(arg0.getRelativeElement().getId())) { //$NON-NLS-1$ //$NON-NLS-2$               
                if (viewBean.getBindingEntityModel().getMetaDataTypes().get(xpath).getMaxOccurs() < 0
                        || count < viewBean.getBindingEntityModel().getMetaDataTypes().get(xpath).getMaxOccurs()) {
                    // clone a new item
                    ItemNodeModel model = selected.getItemNodeModel().clone(
                            "Clone".equals(arg0.getRelativeElement().getId()) ? true : false); //$NON-NLS-1$
                    // if it has default value
                    if (viewBean.getBindingEntityModel().getMetaDataTypes().get(xpath).getDefaultValue() != null)
                        model.setObjectValue(viewBean.getBindingEntityModel().getMetaDataTypes().get(xpath).getDefaultValue());
                    parentItem.insertItem(buildGWTTree(model, null, true), parentItem.getChildIndex(selected) + 1);
                    occurMap.put(countMapItem, count + 1);
                } else
                    MessageBox.alert(MessagesFactory.getMessages().status(), MessagesFactory.getMessages()
                            .multiOccurrence_maximize(count), null);
            } else {
                MessageBox.confirm(MessagesFactory.getMessages().confirm_title(), MessagesFactory.getMessages().delete_confirm(),
                        new Listener<MessageBoxEvent>() {

                            public void handleEvent(MessageBoxEvent be) {
                                if (be.getButtonClicked().getItemId().equals(Dialog.YES)) {
                                    if (count > 1
                                            && count > viewBean.getBindingEntityModel().getMetaDataTypes().get(xpath)
                                                    .getMinOccurs()) {
                                        parentItem.removeItem(selected);
                                        occurMap.put(countMapItem, count - 1);
                                    } else
                                        MessageBox.alert(MessagesFactory.getMessages().status(), MessagesFactory.getMessages()
                                                .multiOccurrence_minimize(count), null);
                                }
                            }
                        });
            }
        }
    };

    public TreeDetail() {
        this.setHeaderVisible(false);
        this.setHeight(1000);
        this.setScrollMode(Scroll.AUTO);
    }

    public void initTree(ViewBean viewBean, ItemBean itemBean) {
        this.viewBean = viewBean;
        if (itemBean == null) {
            buildPanel();
        } else {
            getItemService().getItemNodeModel(itemBean, viewBean.getBindingEntityModel(), Locale.getLanguage(),
                    new SessionAwareAsyncCallback<ItemNodeModel>() {

                        public void onSuccess(ItemNodeModel node) {
                            renderTree(node);
                            if (node.isHasVisiblueRule()) {
                                getItemService().executeVisibleRule(CommonUtil.toXML(node, TreeDetail.this.viewBean),
                                        new SessionAwareAsyncCallback<List<VisibleRuleResult>>() {

                                            public void onSuccess(List<VisibleRuleResult> arg0) {
                                                for (VisibleRuleResult visibleRuleResult : arg0) {
                                                    recrusiveSetItems(visibleRuleResult, (DynamicTreeItem) root);
                                                }
                                            }
                                        });
                            }
                        }

                    });
        }
    }

    private void buildPanel() {

        List<ItemNodeModel> models = CommonUtil.getDefaultTreeModel(
                viewBean.getBindingEntityModel().getMetaDataTypes().get(viewBean.getBindingEntityModel().getConceptName()),
                Locale.getLanguage());
        renderTree(models.get(0));
        if (hasVisibleRule(viewBean.getBindingEntityModel().getMetaDataTypes()
                .get(viewBean.getBindingEntityModel().getConceptName()))) {
            getItemService().executeVisibleRule(CommonUtil.toXML(models.get(0), viewBean),
                    new SessionAwareAsyncCallback<List<VisibleRuleResult>>() {

                        public void onSuccess(List<VisibleRuleResult> arg0) {
                            for (VisibleRuleResult visibleRuleResult : arg0) {
                                recrusiveSetItems(visibleRuleResult, (DynamicTreeItem) root);
                            }
                        }
                    });
        }
    }

    private DynamicTreeItem buildGWTTree(ItemNodeModel itemNode, DynamicTreeItem item, boolean withDefaultValue) {
        if (item == null) {
            item = new DynamicTreeItem();
            item.setItemNodeModel(itemNode);
            if (itemNode.getRealType() != null && itemNode.getRealType().trim().length() > 0) {
                item.setState(true);
            }
            item.setWidget(TreeDetailUtil.createWidget(itemNode, viewBean, handler));
        }
        if (itemNode.getChildren() != null && itemNode.getChildren().size() > 0) {
            Map<TypeModel, List<ItemNodeModel>> fkMap = new HashMap<TypeModel, List<ItemNodeModel>>();
            for (ModelData model : itemNode.getChildren()) {
                ItemNodeModel node = (ItemNodeModel) model;
                TypeModel typeModel = viewBean.getBindingEntityModel().getMetaDataTypes().get(node.getBindingPath());
                if (withDefaultValue && typeModel.getDefaultValue() != null
                        && (node.getObjectValue() == null || node.getObjectValue().equals(""))) //$NON-NLS-1$
                    node.setObjectValue(typeModel.getDefaultValue());
                if (typeModel.getForeignkey() != null && fkRender != null) {
                    if (!fkMap.containsKey(typeModel))
                        fkMap.put(typeModel, new ArrayList<ItemNodeModel>());
                    fkMap.get(typeModel).add(node);
                } else if (typeModel.getForeignkey() == null) {
                    TreeItem childItem = buildGWTTree(node, null, withDefaultValue);
                    item.addItem(childItem);
                    int count = 0;
                    CountMapItem countMapItem = new CountMapItem(node.getBindingPath(), item);
                    if (occurMap.containsKey(countMapItem))
                        count = occurMap.get(countMapItem);
                    occurMap.put(countMapItem, count + 1);
                }
            }
            for (TypeModel model : fkMap.keySet()) {
                fkRender.RenderForeignKey(fkMap.get(model), model);
            }

            item.getElement().getStyle().setPaddingLeft(3.0, Unit.PX);
        }

        item.setUserObject(itemNode);
        item.setState(viewBean.getBindingEntityModel().getMetaDataTypes().get(itemNode.getBindingPath()).isAutoExpand());
        
        return item;
    }

    public void onUpdatePolymorphism(ComplexTypeModel typeModel) {
        DynamicTreeItem item = (DynamicTreeItem) tree.getSelectedItem();
        if (item == null) {
            return;
        }
        item.setState(true);
        ItemNodeModel treeNode = item.getItemNodeModel();
        treeNode.setRealType(typeModel.getName());
        item.removeItems();

        List<ItemNodeModel> items = CommonUtil.getDefaultTreeModel(typeModel, Locale.getLanguage());
        if (items != null && items.size() > 0) {
            List<ItemNodeModel> childrenItems = new ArrayList<ItemNodeModel>();
            for (ModelData modelData : items.get(0).getChildren()) {
                childrenItems.add((ItemNodeModel) modelData);
            }
            treeNode.setChildNodes(childrenItems);
        }
        buildGWTTree(treeNode, item, true);

    }

    public void onExecuteVisibleRule(List<VisibleRuleResult> visibleResults) {
        DynamicTreeItem rootItem = (DynamicTreeItem) tree.getItem(0);
        for (VisibleRuleResult visibleResult : visibleResults) {
            recrusiveSetItems(visibleResult, rootItem);
        }
    }

    private Tree displayGWTTree(ColumnTreeModel columnLayoutModel) {
        Tree tree = new Tree();
        if (root != null && root.getChildCount() > 0) {

            for (ColumnElement ce : columnLayoutModel.getColumnElements()) {
                for (int i = 0; i < root.getChildCount(); i++) {
                    TreeItem child = root.getChild(i);
                    ItemNodeModel node = (ItemNodeModel) child.getUserObject();
                    if (("/" + node.getBindingPath()).equals(ce.getxPath())) { //$NON-NLS-1$
                        tree.addItem(child);
                        break;
                    }
                }
            }
        }

        return tree;
    }

    private void renderTree(ItemNodeModel rootModel) {
        root = buildGWTTree(rootModel, null, false);
        root.setState(true);
        tree = new Tree();
        tree.addItem(root);

        ColumnTreeLayoutModel columnLayoutModel = viewBean.getColumnLayoutModel();
        if (columnLayoutModel != null) {// TODO if create a new PrimaryKey, tree UI should not render according to the
                                        // layout template
            HorizontalPanel hp = new HorizontalPanel();

            for (ColumnTreeModel ctm : columnLayoutModel.getColumnTreeModels()) {
                Tree tree = displayGWTTree(ctm);
                hp.add(tree);
            }
            hp.setHeight("570px"); //$NON-NLS-1$
            HorizontalPanel spacehp = new HorizontalPanel();
            spacehp.setHeight("10px"); //$NON-NLS-1$
            add(spacehp);
            add(hp);

        } else {
            add(tree);
        }
        this.layout();
    }

    private void recrusiveSetItems(VisibleRuleResult visibleResult, DynamicTreeItem rootItem) {
        if (rootItem.getItemNodeModel().getBindingPath().equals(visibleResult.getXpath())) {
            rootItem.setVisible(visibleResult.isVisible());
        }

        if (rootItem.getChildCount() == 0) {
            return;
        }

        for (int i = 0; i < rootItem.getChildCount(); i++) {
            DynamicTreeItem item = (DynamicTreeItem) rootItem.getChild(i);
            recrusiveSetItems(visibleResult, item);
        }
    }

    public static class DynamicTreeItem extends TreeItem {

        private ItemNodeModel itemNode;

        public DynamicTreeItem() {
            super();
        }

        private List<TreeItem> items = new ArrayList<TreeItem>();

        public void insertItem(DynamicTreeItem item, int beforeIndex) {
            int count = this.getChildCount();

            for (int i = 0; i < count; i++) {
                items.add(this.getChild(i));
            }

            items.add(beforeIndex, item);
            itemNode.getChildren().add(beforeIndex, item.getItemNodeModel());
            this.removeItems();

            for (int j = 0; j < items.size(); j++) {
                this.addItem(items.get(j));
            }
            items.clear();
        }

        public void removeItem(DynamicTreeItem item) {
            super.removeItem(item);
            itemNode.getChildren().remove(item.getItemNodeModel());
        }

        public void setItemNodeModel(ItemNodeModel treeNode) {
            itemNode = treeNode;
        }

        public ItemNodeModel getItemNodeModel() {
            return itemNode;
        }
    }

    private static BrowseRecordsServiceAsync getItemService() {

        BrowseRecordsServiceAsync service = (BrowseRecordsServiceAsync) Registry.get(BrowseRecords.BROWSERECORDS_SERVICE);
        return service;

    }

    public Tree getTree() {
        return tree;
    }

    public void refreshTree(final ItemBean item) {
        item.set("isRefresh", true); //$NON-NLS-1$
        getItemService().getItemNodeModel(item, viewBean.getBindingEntityModel(), Locale.getLanguage(),
                new SessionAwareAsyncCallback<ItemNodeModel>() {

                    public void onSuccess(ItemNodeModel node) {
                        TreeDetail.this.removeAll();
                        item.set("time", node.get("time")); //$NON-NLS-1$ //$NON-NLS-2$
                        renderTree(node);
                    }

                    @Override
                    protected void doOnFailure(Throwable caught) {
                        MessageBox.alert(MessagesFactory.getMessages().error_title(), MessagesFactory.getMessages().refresh_tip()
                                + " " + MessagesFactory.getMessages().message_fail(), null); //$NON-NLS-1$
                    }
                });
    }

    private class CountMapItem {

        private String xpath;

        private TreeItem parentItem;

        public CountMapItem(String xpath, TreeItem parentItem) {
            this.xpath = xpath;
            this.parentItem = parentItem;
        }

        public String getXpath() {
            return this.xpath;
        }

        public TreeItem getParentItem() {
            return this.parentItem;
        }

        @Override
        public int hashCode() {
            return xpath.length();
        }

        @Override
        public boolean equals(Object o) {
            CountMapItem item = (CountMapItem) o;
            return item.getXpath().equals(xpath) && item.getParentItem().equals(parentItem);
        }
    }

    public boolean validateTree() {
        boolean flag = true;
        ItemNodeModel rootNode = (ItemNodeModel) tree.getItem(0).getUserObject();
        if (rootNode != null) {
            flag = validateNode(rootNode, flag);
        }
        return flag;
    }

    public boolean validateNode(ItemNodeModel rootNode, boolean flag) {

        if (rootNode.getChildren() != null && rootNode.getChildren().size() > 0) {
            for (ModelData model : rootNode.getChildren()) {

                ItemNodeModel node = (ItemNodeModel) model;
                if (!node.isValid() && node.getChildCount() == 0) {
                    com.google.gwt.user.client.Window.alert(node.getName() + "'Value validate failure"); //$NON-NLS-1$
                    flag = false;
                }

                if (node.getChildren() != null && node.getChildren().size() > 0) {
                    flag = validateNode(node, flag);
                }

                if (!flag) {
                    break;
                }
            }
        }
        return flag;
    }

    private boolean hasVisibleRule(TypeModel typeModel) {
        if (typeModel.isHasVisibleRule()) {
            return true;
        }

        if (!typeModel.isSimpleType()) {
            ComplexTypeModel complexModel = (ComplexTypeModel) typeModel;
            List<TypeModel> children = complexModel.getSubTypes();

            for (TypeModel model : children) {
                boolean childVisibleRule = hasVisibleRule(model);

                if (childVisibleRule) {
                    return true;
                }
            }
        }

        return false;
    }

    public ForeignKeyRender getFkRender() {
        return fkRender;
    }

    public void setFkRender(ForeignKeyRender fkRender) {
        this.fkRender = fkRender;
    }

}
