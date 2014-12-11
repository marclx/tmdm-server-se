package org.talend.mdm.webapp.browserecords.client.widget;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.client.model.ForeignKeyBean;
import org.talend.mdm.webapp.base.client.model.ItemResult;
import org.talend.mdm.webapp.base.shared.TypeModel;
import org.talend.mdm.webapp.browserecords.client.ServiceFactory;
import org.talend.mdm.webapp.browserecords.client.i18n.MessagesFactory;
import org.talend.mdm.webapp.browserecords.client.model.ItemNodeModel;
import org.talend.mdm.webapp.browserecords.client.util.Locale;

import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.store.Record;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.RowEditor;

public class ForeignKeyRowEditor extends RowEditor<ItemNodeModel> {

    TypeModel fkTypeModel;

    boolean staging;

    public ForeignKeyRowEditor(TypeModel fkTypeModel, boolean staging) {
        this.fkTypeModel = fkTypeModel;
        this.staging = staging;
    }

    // cancel click Editor
    @Override
    protected void onRowClick(GridEvent<ItemNodeModel> e) {
    }

    @Override
    public void startEditing(int rowIndex, boolean doFocus) {
        super.startEditing(rowIndex, doFocus);
        grid.getSelectionModel().setLocked(true);

    }

    @Override
    public void stopEditing(boolean saveChanges) {
        super.stopEditing(saveChanges);
        grid.getSelectionModel().setLocked(false);
        if (saveChanges) {
            final ItemNodeModel node = grid.getSelectionModel().getSelectedItem();
            ForeignKeyBean fkBean = (ForeignKeyBean) node.getObjectValue();
            if (fkBean == null) {
                MessageBox.alert(MessagesFactory.getMessages().message_fail(), MessagesFactory.getMessages().fk_edit_failure(),
                        null);
                return;
            }
            String ids = fkBean.getId().replaceAll("\\[", "").replaceAll("\\]", ""); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
            ServiceFactory
                    .getInstance()
                    .getService(staging)
                    .updateItem(
                            fkTypeModel.getForeignkey().split("/")[0], ids, fkBean.getForeignKeyInfo(), null, null, Locale.getLanguage(), //$NON-NLS-1$
                            new SessionAwareAsyncCallback<ItemResult>() {

                                @Override
                                protected void doOnFailure(Throwable caught) {
                                    Record record;
                                    Store<ItemNodeModel> store = grid.getStore();
                                    if (store != null) {
                                        record = store.getRecord(node);
                                    } else {
                                        record = null;
                                    }

                                    if (record != null) {
                                        record.reject(false);
                                    }

                                    String err = caught.getLocalizedMessage();
                                    if (err != null) {
                                        err.replaceAll("\\[", "{").replaceAll("\\]", "}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                    }
                                    MessageBox.alert(MessagesFactory.getMessages().error_title(),
                                            Locale.getExceptionString(Locale.getLanguage(), err), null);

                                }

                                @Override
                                public void onSuccess(ItemResult result) {
                                    Record record;
                                    Store<ItemNodeModel> store = grid.getStore();
                                    if (store != null) {
                                        record = store.getRecord(node);
                                    } else {
                                        record = null;
                                    }

                                    if (record != null) {
                                        record.commit(false);
                                    }
                                    // TODO refreshForm(itemBean);
                                    MessageBox.alert(MessagesFactory.getMessages().info_title(),
                                            Locale.getExceptionMessageByLanguage(Locale.getLanguage(), result.getDescription()),
                                            null);
                                }
                            });
        }
    }

}
