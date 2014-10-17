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

package org.talend.mdm.webapp.browserecords.client.widget.ForeignKey;

import java.util.List;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.client.model.ForeignKeyBean;
import org.talend.mdm.webapp.base.shared.EntityModel;
import org.talend.mdm.webapp.browserecords.client.BrowseRecords;
import org.talend.mdm.webapp.browserecords.client.BrowseRecordsServiceAsync;
import org.talend.mdm.webapp.browserecords.client.i18n.MessagesFactory;
import org.talend.mdm.webapp.browserecords.client.resources.icon.Icons;
import org.talend.mdm.webapp.browserecords.client.util.Locale;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.SuggestComboBoxField;

import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.ComponentHelper;
import com.extjs.gxt.ui.client.widget.button.SplitButton;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class FKSearchField extends TextField<ForeignKeyBean> implements ReturnCriteriaFK, FKField {

    private SuggestComboBoxField suggestBox;

    private SplitButton foreignBtn = new SplitButton();

    private FKRelRecordWindow relWindow;

    private boolean retrieveFKinfos = false;

    private String foreignKey;

    private String foreignKeyField;

    private ReturnCriteriaFK returnCriteriaFK;

    private boolean staging;

    private BrowseRecordsServiceAsync service = (BrowseRecordsServiceAsync) Registry.get(BrowseRecords.BROWSERECORDS_SERVICE);;

    private String concept;

    private EntityModel currentEntityModel = BrowseRecords.getSession().getCurrentEntityModel();

    private List<String> foreignKeyInfo;

    private String usageField;

    public boolean isRetrieveFKinfos() {
        return retrieveFKinfos;
    }

    public void setRetrieveFKinfos(boolean retrieveFKinfos) {
        this.retrieveFKinfos = retrieveFKinfos;
    }

    public FKSearchField() {
        this.setFireChangeEventOnSetValue(true);
    }

    public FKSearchField(String foreignKey, List<String> foreignKeyInfo) {
        this.foreignKey = foreignKey;
        this.foreignKeyInfo = foreignKeyInfo;
        this.setFireChangeEventOnSetValue(true);
        suggestBox = new SuggestComboBoxField(this);
    }

    @Override
    protected void onRender(Element target, int index) {
        El wrap = new El(DOM.createDiv());
        wrap.addStyleName("x-form-field-wrap"); //$NON-NLS-1$
        wrap.addStyleName("x-form-file-wrap"); //$NON-NLS-1$

        input = new El(DOM.createInputText());

        foreignBtn.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.link()));
        foreignBtn.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {                
                if (relWindow != null) {
                    if(relWindow.getTypeComboBox() != null){
                        relWindow.getTypeComboBox().setRawValue(concept);
                    }
                    relWindow.show();
                } else {
                    if (foreignKeyField != null && currentEntityModel != null) {
                        concept = currentEntityModel.getTypeModel(foreignKeyField).getForeignkey().split("/")[0]; //$NON-NLS-1$
                        service.getEntityModel(concept, Locale.getLanguage(), new SessionAwareAsyncCallback<EntityModel>() {

                            @Override
                            public void onSuccess(EntityModel entityModel) {
                                showWindow(entityModel);
                            }

                            @Override
                            protected void doOnFailure(Throwable caught) {
                                super.doOnFailure(caught);
                            }
                        });
                    }
                }
            }
        });

        El foreignTable = new El(DOM.createTable());
        Element tbody = DOM.createTBody();
        Element fktr = DOM.createTR();
        tbody.appendChild(fktr);

        foreignTable.dom.setAttribute("cellspacing", "0"); //$NON-NLS-1$ //$NON-NLS-2$
        foreignTable.appendChild(tbody);

        Element tdInput = DOM.createTD();
        Element tdIcon = DOM.createTD();

        fktr.appendChild(tdInput);
        if (!"ForeignKeyTablePanel".equals(usageField)) { //$NON-NLS-1$
            fktr.appendChild(tdIcon);
        }
        wrap.appendChild(foreignTable.dom);

        final SelectionListener<ButtonEvent> closer = new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                if (relWindow != null) {
                    relWindow.hide();
                }
            }

        };

        setElement(wrap.dom, target, index);
        suggestBox.render(tdInput);
        foreignBtn.render(tdIcon);
        super.onRender(target, index);
    }

    private void showWindow(EntityModel entityModel) {
        relWindow = new FKRelRecordWindow();
        relWindow.setEntityModel(entityModel);
        relWindow.setSize(470, 340);
        relWindow.setResizable(false);
        relWindow.setModal(true);
        relWindow.setBlinkModal(true);
        relWindow.setFkKey(foreignKeyField);
        relWindow.setReturnCriteriaFK(returnCriteriaFK);
        relWindow.setHeading(MessagesFactory.getMessages().fk_RelatedRecord());
        relWindow.setStaging(staging);
        relWindow.show();
    }

    @Override
    protected void onResize(int width, int height) {
        super.onResize(width, height);
        if ("SearchFieldCreator".equals(usageField)) { //$NON-NLS-1$
            suggestBox.setWidth(width - foreignBtn.getWidth() - 20);
        } else {
            suggestBox.setWidth(width - foreignBtn.getWidth());
        }
    }

    @Override
    protected void doAttachChildren() {
        super.doAttachChildren();
        ComponentHelper.doAttach(foreignBtn);
        ComponentHelper.doAttach(suggestBox);
    }

    @Override
    protected void doDetachChildren() {
        super.doDetachChildren();
        ComponentHelper.doDetach(foreignBtn);
        ComponentHelper.doDetach(suggestBox);
    }

    public void Update(String foreignKeyField, ReturnCriteriaFK returnCriteriaFK) {
        this.foreignKeyField = foreignKeyField;
        this.returnCriteriaFK = returnCriteriaFK;
    }

    @Override
    public void setCriteriaFK(final ForeignKeyBean fk) {
        if (retrieveFKinfos) {
            fk.setShowInfo(true);
        }
        setValue(fk);
    }

    public void setReturnCriteriaFK(ReturnCriteriaFK returnCriteriaFK) {
        this.returnCriteriaFK = returnCriteriaFK;
    }

    @Override
    protected void onFocus(ComponentEvent be) {
        if (suggestBox != null) {
            suggestBox.focus();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        foreignBtn.setEnabled(enabled);
    }

    @Override
    public void setValue(ForeignKeyBean fk) {
        if (suggestBox != null) {
            suggestBox.setValue(fk);
        }
        super.setValue(fk);
    }
    
    @Override
    public void setSuperValue(ForeignKeyBean fk) {
        super.setValue(fk);
    }

    @Override
    public ForeignKeyBean getValue() {
        if (suggestBox.getValue() != null) {
            return suggestBox.getValue();
        }
        return value;
    }

    @Override
    public String getForeignKey() {
        return this.foreignKey;
    }

    @Override
    public List<String> getForeignKeyInfo() {
        return this.foreignKeyInfo;
    }

    public void setStaging(boolean staging) {
        this.staging = staging;
    }

    @Override
    public boolean isStaging() {
        return this.staging;
    }

    public void setUsageField(String usageField) {
        this.usageField = usageField;
    }

}
