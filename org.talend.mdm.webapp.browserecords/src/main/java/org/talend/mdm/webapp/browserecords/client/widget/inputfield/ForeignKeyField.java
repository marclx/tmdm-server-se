package org.talend.mdm.webapp.browserecords.client.widget.inputfield;

import java.util.List;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.client.model.ForeignKeyBean;
import org.talend.mdm.webapp.browserecords.client.BrowseRecords;
import org.talend.mdm.webapp.browserecords.client.BrowseRecordsEvents;
import org.talend.mdm.webapp.browserecords.client.ServiceFactory;
import org.talend.mdm.webapp.browserecords.client.handler.ItemTreeHandler;
import org.talend.mdm.webapp.browserecords.client.handler.ItemTreeHandlingStatus;
import org.talend.mdm.webapp.browserecords.client.i18n.MessagesFactory;
import org.talend.mdm.webapp.browserecords.client.model.ItemNodeModel;
import org.talend.mdm.webapp.browserecords.client.mvc.BrowseRecordsView;
import org.talend.mdm.webapp.browserecords.client.resources.icon.Icons;
import org.talend.mdm.webapp.browserecords.client.util.Locale;
import org.talend.mdm.webapp.browserecords.client.widget.ForeignKeyFieldList;
import org.talend.mdm.webapp.browserecords.client.widget.ItemPanel;
import org.talend.mdm.webapp.browserecords.client.widget.ItemsDetailPanel;
import org.talend.mdm.webapp.browserecords.client.widget.ForeignKey.FKField;
import org.talend.mdm.webapp.browserecords.client.widget.ForeignKey.ReturnCriteriaFK;
import org.talend.mdm.webapp.browserecords.client.widget.treedetail.ForeignKeyListWindow;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.mvc.AppEvent;
import com.extjs.gxt.ui.client.mvc.Dispatcher;
import com.extjs.gxt.ui.client.widget.ComponentHelper;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Image;

public class ForeignKeyField extends TextField<ForeignKeyBean> implements ReturnCriteriaFK, FKField {

    private SuggestComboBoxField suggestBox;

    private Image selectFKBtn = new Image(Icons.INSTANCE.link());

    private Image addFKBtn = new Image(Icons.INSTANCE.link_add());

    private Image cleanFKBtn = new Image(Icons.INSTANCE.link_delete());

    private Image relationFKBtn = new Image(Icons.INSTANCE.link_go());

    private String foreignKeyName;

    private String foreignKey;

    private List<String> foreignKeyInfo;

    private ForeignKeyListWindow fkWindow = new ForeignKeyListWindow();

    private boolean isFkList;

    private ForeignKeyFieldList fkFieldList;

    private ItemsDetailPanel itemsDetailPanel;

    private boolean validateFlag = true;

    private boolean staging;

    private String currentNodeXpath;

    private String fkFilter;

    public ForeignKeyField(String currentNodeXpath, String fkFilter, String foreignKey, List<String> foreignKeyInfo,
            ItemsDetailPanel itemsDetailPanel) {
        this.validateFlag = BrowseRecords.getSession().getAppHeader().isAutoValidate();
        this.currentNodeXpath = currentNodeXpath;
        this.fkFilter = fkFilter;
        this.itemsDetailPanel = itemsDetailPanel;
        this.foreignKey = foreignKey;
        this.foreignKeyInfo = foreignKeyInfo;
        this.foreignKeyName = foreignKey.split("/")[0]; //$NON-NLS-1$
        this.staging = itemsDetailPanel.isStaging();
        this.setFireChangeEventOnSetValue(true);
        this.setReturnCriteriaFK();
        fkWindow.setForeignKeyInfos(foreignKey, foreignKeyInfo);
        fkWindow.setCurrentXpath(currentNodeXpath);
        fkWindow.setForeignKeyFilter(fkFilter);
        fkWindow.setSize(470, 340);
        fkWindow.setResizable(false);
        fkWindow.setModal(true);
        fkWindow.setBlinkModal(true);
        fkWindow.setStaging(staging);

        suggestBox = new SuggestComboBoxField(this);
    }

    public ForeignKeyField(String foreignKey, List<String> foreignKeyInfo, ForeignKeyFieldList fkFieldList,
            ItemsDetailPanel itemsDetailPanel) {
        this(null, null, foreignKey, foreignKeyInfo, itemsDetailPanel);
        this.fkFieldList = fkFieldList;
        this.isFkList = true;
        this.staging = itemsDetailPanel.isStaging();
    }

    public void initForeignKeyListWindow() {

    }

    public ForeignKeyListWindow getFkWindow() {
        return fkWindow;
    }

    @Override
    protected void onRender(Element target, int index) {
        El wrap = new El(DOM.createTable());
        Element tbody = DOM.createTBody();
        Element fktr = DOM.createTR();
        tbody.appendChild(fktr);
        Element tdInput = DOM.createTD();
        Element tdIcon = DOM.createTD();
        fktr.appendChild(tdInput);
        fktr.appendChild(tdIcon);

        wrap.appendChild(tbody);
        wrap.setElementAttribute("cellSpacing", "0"); //$NON-NLS-1$ //$NON-NLS-2$
        wrap.addStyleName("x-form-field-wrap"); //$NON-NLS-1$
        wrap.addStyleName("x-form-file-wrap"); //$NON-NLS-1$

        input = new El(DOM.createInputText());
        suggestBox.render(tdInput);

        Element foreignDiv = DOM.createTable();
        Element tr = DOM.createTR();
        Element body = DOM.createTBody();

        Element selectTD = DOM.createTD();
        Element addTD = DOM.createTD();
        Element cleanTD = DOM.createTD();
        Element relationTD = DOM.createTD();

        foreignDiv.appendChild(body);
        body.appendChild(tr);
        tr.appendChild(selectTD);
        tr.appendChild(addTD);
        tr.appendChild(cleanTD);
        tr.appendChild(relationTD);

        tdIcon.setAttribute("align", "right"); //$NON-NLS-1$//$NON-NLS-2$
        tdIcon.appendChild(foreignDiv);
        setElement(wrap.dom, target, index);

        selectTD.appendChild(selectFKBtn.getElement());
        addTD.appendChild(addFKBtn.getElement());
        cleanTD.appendChild(cleanFKBtn.getElement());
        relationTD.appendChild(relationFKBtn.getElement());

        updateCtrlButton();

        addListener();
        this.setAutoWidth(true);
        super.onRender(target, index);
    }

    @Override
    protected void afterRender() {
        super.afterRender();
        if (GXT.isIE && suggestBox != null) {
            suggestBox.setWidth(135);
        }
    }

    @Override
    protected void onFocus(ComponentEvent be) {
        if (suggestBox != null) {
            suggestBox.focus();
        }
    }

    @Override
    public int getWidth() {
        if (GXT.isChrome) {
            return getOffsetWidth() + 75;
        } else if (GXT.isIE) {
            return getOffsetWidth() + 35;
        } else {
            return getOffsetWidth();
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        updateCtrlButton();
    }

    private void updateCtrlButton() {
        selectFKBtn.setVisible(!readOnly);
        addFKBtn.setVisible(!readOnly);
        cleanFKBtn.setVisible(!readOnly);
        relationFKBtn.setVisible(true);
    }

    private void addListener() {
        addFKBtn.setTitle(MessagesFactory.getMessages().fk_add_title());
        addFKBtn.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent arg0) {
                Dispatcher dispatch = Dispatcher.get();
                AppEvent event = new AppEvent(BrowseRecordsEvents.CreateForeignKeyView, foreignKeyName);
                event.setData(BrowseRecordsView.FK_SOURCE_WIDGET, ForeignKeyField.this);
                event.setData(BrowseRecordsView.ITEMS_DETAIL_PANEL, itemsDetailPanel);
                event.setData(BrowseRecordsView.IS_STAGING, itemsDetailPanel.isStaging());
                dispatch.dispatch(event);
            }
        });
        selectFKBtn.setTitle(MessagesFactory.getMessages().fk_select_title());
        selectFKBtn.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent arg0) {
                Dispatcher dispatch = Dispatcher.get();
                AppEvent event = new AppEvent(BrowseRecordsEvents.SelectForeignKeyView, ForeignKeyField.this.foreignKey
                        .split("/")[0]); //$NON-NLS-1$
                event.setData("detailPanel", itemsDetailPanel); //$NON-NLS-1$
                event.setSource(ForeignKeyField.this.getFkWindow());
                dispatch.dispatch(event);
            }
        });
        cleanFKBtn.setTitle(MessagesFactory.getMessages().fk_del_title());
        cleanFKBtn.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent arg0) {
                if (!isFkList) {
                    clear();
                } else {
                    fkFieldList.removeForeignKeyWidget(ForeignKeyField.this.getValue());
                }
            }
        });
        relationFKBtn.setTitle(MessagesFactory.getMessages().fk_open_title());
        relationFKBtn.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent arg0) {
                ForeignKeyBean fkBean = ForeignKeyField.this.getValue();
                if (fkBean == null || fkBean.getId() == null || "".equals(fkBean.getId())) {
                    return;
                }
                Dispatcher dispatch = Dispatcher.get();
                AppEvent event = new AppEvent(BrowseRecordsEvents.ViewForeignKey);
                event.setData("ids", ForeignKeyField.this.getValue().getId().replaceAll("^\\[|\\]$", "").replace("][", ".")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                event.setData("concept", ForeignKeyField.this.foreignKeyName); //$NON-NLS-1$
                event.setData("isStaging", itemsDetailPanel.isStaging()); //$NON-NLS-1$
                event.setData(BrowseRecordsView.ITEMS_DETAIL_PANEL, itemsDetailPanel);
                dispatch.dispatch(event);
            }
        });
    }

    @Override
    protected void doAttachChildren() {
        super.doAttachChildren();
        ComponentHelper.doAttach(addFKBtn);
        ComponentHelper.doAttach(selectFKBtn);
        ComponentHelper.doAttach(cleanFKBtn);
        ComponentHelper.doAttach(relationFKBtn);
        ComponentHelper.doAttach(suggestBox);
    }

    @Override
    protected void doDetachChildren() {
        super.doDetachChildren();
        ComponentHelper.doDetach(addFKBtn);
        ComponentHelper.doDetach(selectFKBtn);
        ComponentHelper.doDetach(cleanFKBtn);
        ComponentHelper.doDetach(relationFKBtn);
        ComponentHelper.doDetach(suggestBox);
    }

    public void setCriteriaFK(final String foreignKeyIds) {
        ItemPanel itemPanel = (ItemPanel) itemsDetailPanel.getFirstTabWidget();
        ItemNodeModel root = itemPanel.getTree().getRootModel();
        String xml = (new ItemTreeHandler(root, itemPanel.getViewBean(), ItemTreeHandlingStatus.BeforeLoad)).serializeItem();
        ServiceFactory.getInstance().getService(staging)
                .getForeignKeyBean(foreignKey.split("/")[0], foreignKeyIds, xml, currentNodeXpath, foreignKey, foreignKeyInfo, //$NON-NLS-1$
                        fkFilter, staging, Locale.getLanguage(), new SessionAwareAsyncCallback<ForeignKeyBean>() {

                            @Override
                            public void onSuccess(ForeignKeyBean foreignKeyBean) {
                                if (foreignKeyBean != null) {
                                    setCriteriaFK(foreignKeyBean);
                                } else {
                                    MessageBox.alert(MessagesFactory.getMessages().warning_title(), MessagesFactory.getMessages()
                                            .foreignkeybean_filter_warnging(), null);
                                }
                            }
                        });
    }

    @Override
    public void setCriteriaFK(final ForeignKeyBean fk) {
        if (fk != null && fk.getConceptName() != null && fk.getConceptName().trim().length() > 0) {
            this.foreignKeyName = fk.getConceptName();
        }
        setValue(fk);
    }

    @Override
    public void setValue(ForeignKeyBean fk) {
        if (fk != null) {
            if (suggestBox != null) {
                suggestBox.setValue(fk);
            }
            super.setValue(fk);
        }
    }
    
    @Override
    public void setSuperValue(ForeignKeyBean fk) {
        super.setValue(fk);
    }

    @Override
    public void clear() {
        super.clear();
        this.validate();

        ForeignKeyBean bean = new ForeignKeyBean();
        bean.setId(""); //$NON-NLS-1$
        setValue(bean);

        if (suggestBox != null) {
            suggestBox.clear();
        }
    }

    @Override
    public ForeignKeyBean getValue() {
        if (suggestBox.getValue() != null) {
            return suggestBox.getValue();
        }
        return value;
    }

    public void setReturnCriteriaFK() {
        fkWindow.setReturnCriteriaFK(this);
        fkWindow.setHeading(MessagesFactory.getMessages().fk_RelatedRecord());
    }

    @Override
    public boolean validateValue(String value) {
        if (!validateFlag) {
            return true;
        }
        return super.validateValue(value);
    }

    public void setValidateFlag(boolean validateFlag) {
        this.validateFlag = validateFlag;
    }

    public SuggestComboBoxField getSuggestBox() {
        return this.suggestBox;
    }

    @Override
    public String getForeignKey() {
        return this.foreignKey;
    }

    @Override
    public List<String> getForeignKeyInfo() {
        return this.foreignKeyInfo;
    }

    public ItemsDetailPanel getItemsDetailPanel() {
        return this.itemsDetailPanel;
    }

    @Override
    public boolean isStaging() {
        return this.staging;
    }
}