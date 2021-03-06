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
package org.talend.mdm.webapp.base.client.widget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.talend.mdm.webapp.base.client.BaseRemoteService;
import org.talend.mdm.webapp.base.client.BaseRemoteServiceAsync;
import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.client.i18n.BaseMessagesFactory;
import org.talend.mdm.webapp.base.client.model.ItemBaseModel;
import org.talend.mdm.webapp.base.client.model.MultiLanguageModel;
import org.talend.mdm.webapp.base.client.resources.icon.Icons;
import org.talend.mdm.webapp.base.client.util.FormatUtil;
import org.talend.mdm.webapp.base.client.util.LanguageUtil;
import org.talend.mdm.webapp.base.client.util.MultilanguageMessageParser;
import org.talend.mdm.webapp.base.client.util.UrlUtil;
import org.talend.mdm.webapp.base.shared.OperatorValueConstants;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.core.XDOM;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ComponentHelper;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.grid.CellEditor;
import com.extjs.gxt.ui.client.widget.grid.CheckBoxSelectionModel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridView;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;


public class MultiLanguageField extends TextField<String> {

    private Image displayMultiLanguageBtn = new Image(Icons.INSTANCE.world_edit());

    private MultiLanguageModel multiLanguageModel;

    private String currentLanguage = UrlUtil.getUpperLanguage();

    private LinkedHashMap<String, ItemBaseModel> languageColumnMap = new LinkedHashMap<String, ItemBaseModel>();

    private boolean isFormInput;
    
    private boolean isAdding;

    private BaseRemoteServiceAsync service = GWT.create(BaseRemoteService.class);

    public MultiLanguageField() {
        super();
    }

    public MultiLanguageField(boolean isFormInput) {
        this.isFormInput = isFormInput;
    }

    public MultiLanguageField(MultiLanguageModel _multiLanguageModel) {
        this();
        this.multiLanguageModel = _multiLanguageModel;
    }

    public boolean isAdding() {
        return this.isAdding;
    }

    public void setAdding(boolean isAdding) {
        this.isAdding = isAdding;
    }

    protected void onRender(Element target, int index) {
        if (isFormInput) {
            El wrap = new El(DOM.createTable());
            Element tbody = DOM.createTBody();
            Element mlstr = DOM.createTR();
            tbody.appendChild(mlstr);
            Element tdInput = DOM.createTD();
            Element tdIcon = DOM.createTD();
            mlstr.appendChild(tdInput);
            mlstr.appendChild(tdIcon);

            wrap.appendChild(tbody);
            wrap.addStyleName("x-form-field-wrap"); //$NON-NLS-1$
            wrap.addStyleName("x-form-file-wrap"); //$NON-NLS-1$

            input = new El(DOM.createInputText());
            input.addStyleName(fieldStyle);
            input.setId(XDOM.getUniqueId());
            input.setEnabled(true);

            tdInput.appendChild(input.dom);
            Element buttonDiv = DOM.createTable();
            Element tr = DOM.createTR();
            Element body = DOM.createTBody();

            Element displayTD = DOM.createTD();

            buttonDiv.appendChild(body);
            body.appendChild(tr);
            tr.appendChild(displayTD);

            tdIcon.appendChild(buttonDiv);
            setElement(wrap.dom, target, index);

            displayTD.appendChild(displayMultiLanguageBtn.getElement());
            displayMultiLanguageBtn.getElement().getStyle().setCursor(Cursor.POINTER);
            updateCtrlButton();

            addListener();
            this.setAutoWidth(true);
            this.setStyleAttribute("margin-left", "-2px"); //$NON-NLS-1$ //$NON-NLS-2$

        }
        super.onRender(target, index);
    }

    public int getWidth() {
        // when isChrome, it need to add buttonDiv's width
        return GXT.isChrome ? getOffsetWidth() + 75 : getOffsetWidth();
    }

    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        updateCtrlButton();
    }

    private void updateCtrlButton() {
        displayMultiLanguageBtn.setVisible(!readOnly);
    }

    private void addListener() {
        displayMultiLanguageBtn.setTitle(BaseMessagesFactory.getMessages().open_mls_title());
        displayMultiLanguageBtn.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent arg0) {
                if (LanguageUtil.getInstance().getLanguages().isEmpty()) {
                    service.getLanguageModels(new SessionAwareAsyncCallback<List<ItemBaseModel>>() {

                        public void onSuccess(List<ItemBaseModel> result) {
                            LinkedHashMap<String, ItemBaseModel> temp_languageColumnMap = new LinkedHashMap<String, ItemBaseModel>();
                            for (ItemBaseModel model : result) {
                                temp_languageColumnMap.put(model.get("value").toString(), model); //$NON-NLS-1$
                            }
                            LanguageUtil.getInstance().setLanguageColumnMap(temp_languageColumnMap);
                            LanguageUtil.getInstance().setLanguags(result);
                            displayMultLanguageWindow();
                        }

                        @Override
                        protected void doOnFailure(Throwable caught) {
                            super.doOnFailure(caught);
                        }
                    });
                } else {
                    displayMultLanguageWindow();
                }

            }
            
        });
    }

    protected void doAttachChildren() {
        super.doAttachChildren();
        ComponentHelper.doAttach(displayMultiLanguageBtn);
    }

    protected void doDetachChildren() {
        super.doDetachChildren();
        ComponentHelper.doDetach(displayMultiLanguageBtn);
    }

    public void setValue(String value) {
        if (multiLanguageModel != null)
            multiLanguageModel.setValueByLanguage(currentLanguage, value);
        super.setValue(value);
    }

    public void clear() {
        if (this.multiLanguageModel != null)
            this.multiLanguageModel.clear();
        super.clear();
    }

    public void setMultiLanguageStringValue(String multiLanguageString) {
        this.multiLanguageModel = new MultiLanguageModel(multiLanguageString);
        this.value = multiLanguageModel.getValueByLanguage(currentLanguage);
    }

    public String getMultiLanguageStringValue() {
        multiLanguageModel.setValueByLanguage(currentLanguage, value);
        return multiLanguageModel.toString();
    }

    public MultiLanguageModel getMultiLanguageModel() {
        return multiLanguageModel;
    }

    public void setMultiLanguageModel(MultiLanguageModel _multiLanguageModel) {
        this.multiLanguageModel = _multiLanguageModel;
    }

    public String getValueWithLanguage(String operator) {
        String v = FormatUtil.languageValueEncode(value);
        if("*".equals(v)){ //$NON-NLS-1$
            return v;
        }        
        if (OperatorValueConstants.CONTAINS.equals(operator)) {
            return "*[" + this.currentLanguage + ":*" + v + "*]*"; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
        } else if (OperatorValueConstants.STARTSWITH.equals(operator)) {
            return "*[" + this.currentLanguage + ":" + v + "*]*"; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
        } else if (OperatorValueConstants.STRICTCONTAINS.equals(operator)) {
            return "*[" + this.currentLanguage + ":*" + v + "*]*"; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        }
        return v;
    }
    
    public String getInputValue(String operator, String value) {
        String formatValue = MultilanguageMessageParser.getValueByLanguage(value, this.currentLanguage);
        if (OperatorValueConstants.CONTAINS.equals(operator)) {
            return formatValue.substring(1, formatValue.length() - 1);
        } else if (OperatorValueConstants.STARTSWITH.equals(operator)) {
            return formatValue.substring(0, formatValue.length() - 1);
        } else if (OperatorValueConstants.STRICTCONTAINS.equals(operator)) {
            return formatValue.substring(1, formatValue.length() - 1);
        }
        return value;
    }

    private void displayMultLanguageWindow() {
        final Window window = new Window();
        window.setPlain(true);
        window.setModal(true);
        window.setBlinkModal(true);
        window.setHeading(BaseMessagesFactory.getMessages().open_mls_title());
        window.setLayout(new FitLayout());
        window.setSize(600, 350);
        GridView view = new GridView();
        List<ColumnConfig> columns = new ArrayList<ColumnConfig>();
        final ListStore<ItemBaseModel> languageList = new ListStore<ItemBaseModel>();
        final ListStore<ItemBaseModel> store = new ListStore<ItemBaseModel>();
        final MultiLanguageRowEditor re = new MultiLanguageRowEditor(MultiLanguageField.this, window);
        final ComboBox<ItemBaseModel> combo = new ComboBox<ItemBaseModel>();
        combo.setDisplayField("language"); //$NON-NLS-1$
        combo.setValueField("value"); //$NON-NLS-1$
        combo.setStore(languageList);
        combo.setTriggerAction(TriggerAction.ALL);
        languageList.add(LanguageUtil.getInstance().getLanguages());
        languageColumnMap = LanguageUtil.getInstance().getLanguageColumnMap();

        final CellEditor editor = new CellEditor(combo) {

            @Override
            public Object preProcessValue(Object value) {
                if (value == null) {
                    return value;
                }
                if (Boolean.parseBoolean(store.getAt(re.getSelectedRowIndex()).get("isNewNode").toString())) //$NON-NLS-1$
                    this.getField().setEnabled(true);
                else
                    this.getField().setEnabled(false);
                return languageColumnMap.get(value);
            }

            @Override
            public Object postProcessValue(Object value) {
                if (value == null) {
                    return value;
                }
                return ((ItemBaseModel) value).get("value"); //$NON-NLS-1$
            }
        };
        ColumnConfig languageColumn = new ColumnConfig("language", BaseMessagesFactory.getMessages().language_title(), 200); //$NON-NLS-1$
        languageColumn.setRenderer(new GridCellRenderer<ItemBaseModel>() {

            public Object render(ItemBaseModel model, String property, ColumnData config, int rowIndex, int colIndex,
                    ListStore<ItemBaseModel> store, Grid<ItemBaseModel> grid) {
                return languageColumnMap.get(model.get("language")).get("language"); //$NON-NLS-1$ //$NON-NLS-2$
            }

        });
        languageColumn.setEditor(editor);
        TextField<String> text = new TextField<String>();  
        ColumnConfig valueColumn = new ColumnConfig("value", BaseMessagesFactory.getMessages().value_title(), 200); //$NON-NLS-1$
        valueColumn.setRenderer(new GridCellRenderer<ItemBaseModel>() {

            public Object render(ItemBaseModel model, String property, ColumnData config, int rowIndex, int colIndex,
                    ListStore<ItemBaseModel> store, Grid<ItemBaseModel> grid) {
                return model.get("value"); //$NON-NLS-1$
            }

        });
        valueColumn.setEditor(new CellEditor(text));  

        final CheckBoxSelectionModel<ItemBaseModel> selectionModel = new CheckBoxSelectionModel<ItemBaseModel>();
        columns.add(selectionModel.getColumn());
        columns.add(languageColumn);
        columns.add(valueColumn);
        ColumnModel cm = new ColumnModel(columns);

        LinkedHashMap<String, String> languageValueMap = this.multiLanguageModel.getLanguageValueMap();
        for (String language : languageValueMap.keySet()) {
            ItemBaseModel model = new ItemBaseModel();
            model.set("language", language); //$NON-NLS-1$
            model.set("value", languageValueMap.get(language)); //$NON-NLS-1$
            model.set("isNewNode", false); //$NON-NLS-1$
            store.add(model);
        }

        Grid<ItemBaseModel> grid = new Grid<ItemBaseModel>(store, cm);
        grid.setTrackMouseOver(false);
        grid.setLoadMask(true);
        grid.setBorders(false);
        grid.setSelectionModel(selectionModel);
        grid.addPlugin(selectionModel);
        grid.addPlugin(re);
        grid.setView(view);
        grid.getView().setForceFit(true);
        hookContextMenu(re, grid);
        ToolBar toolBar = new ToolBar();
        Button addButton = new Button(BaseMessagesFactory.getMessages().add_btn(), AbstractImagePrototype.create(Icons.INSTANCE
                .Create()));
        addButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
           @Override
            public void componentSelected(ButtonEvent ce) {
                setAdding(true);
                ItemBaseModel model = new ItemBaseModel();
                model.set("language", "EN"); //$NON-NLS-1$//$NON-NLS-2$
                model.set("value", ""); //$NON-NLS-1$//$NON-NLS-2$
                model.set("isNewNode", true); //$NON-NLS-1$
                
                if (re.isEditing()) {
                    re.stopEditing(false);
                }
                
                store.add(model);
                re.startEditing(store.indexOf(model), true);
            } 
        });
        Button removeButton = new Button(BaseMessagesFactory.getMessages().remove_btn(),
                AbstractImagePrototype.create(Icons.INSTANCE.Delete()));
        removeButton.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                List<ItemBaseModel> selectedModelList = selectionModel.getSelectedItems();
                if (selectedModelList != null && selectedModelList.size() > 0) {
                    boolean allSelected = (store.getCount() == selectedModelList.size());
                    int endIndex = allSelected ? 1 : 0;
                    for (int i = selectedModelList.size() - 1; i >= endIndex; i--) {
                        ItemBaseModel model = selectedModelList.get(i);
                        multiLanguageModel.setValueByLanguage(model.get("language").toString(), null); //$NON-NLS-1$
                        store.remove(model);
                    }
                    MultiLanguageField.this.fireEvent(Events.Change);
                }
            }
        });
        toolBar.add(addButton);
        toolBar.add(new SeparatorToolItem());
        toolBar.add(removeButton);
        toolBar.add(new SeparatorToolItem());
        window.setTopComponent(toolBar);
        window.add(grid);
        window.setScrollMode(Scroll.AUTO);

        Button closeBTN = new Button(GXT.MESSAGES.messageBox_close());
        closeBTN.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                window.hide();
            }
        });
        window.addButton(closeBTN);
        window.show();
    }

    private void hookContextMenu(final MultiLanguageRowEditor re, final Grid<ItemBaseModel> grid) {
        Menu contextMenu = new Menu();
        MenuItem editRow = new MenuItem();
        editRow.setText(BaseMessagesFactory.getMessages().edititem());
        editRow.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.Edit()));
        editRow.addSelectionListener(new SelectionListener<MenuEvent>() {

            @Override
            public void componentSelected(MenuEvent ce) {
                ItemBaseModel m = grid.getSelectionModel().getSelectedItem();
                int rowIndex = grid.getStore().indexOf(m);
                re.startEditing(rowIndex, true);
            }
        });

        contextMenu.add(editRow);
        grid.setContextMenu(contextMenu);

    }

}