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
package org.talend.mdm.webapp.welcomeportal.client.widget;

import java.util.HashMap;
import java.util.Map;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.client.util.UrlUtil;
import org.talend.mdm.webapp.welcomeportal.client.MainFramePanel;
import org.talend.mdm.webapp.welcomeportal.client.WelcomePortal;
import org.talend.mdm.webapp.welcomeportal.client.i18n.MessagesFactory;

import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;

public class ProcessPortlet extends BasePortlet {

    private Map<String, String> processMap = new HashMap<String, String>();

    public ProcessPortlet(MainFramePanel portal) {
        super(WelcomePortal.PROCESS, portal);
        initConfigSettings();
        label.setText(MessagesFactory.getMessages().process_desc());
        updateProcesses();
        autoRefresh(configModel.isAutoRefresh());
    }

    @Override
    public void refresh() {
        updateProcesses();
    }

    private void buildProcesses() {
        if (processMap.isEmpty()) {
            label.setText(MessagesFactory.getMessages().no_standalone_process());
            fieldSet.setVisible(false);
        } else {
            for (final String key : processMap.keySet()) {
                String description = processMap.get(key);
                HTML processHtml = new HTML();
                StringBuilder sb = new StringBuilder();
                sb.append("<span id=\"processes"); //$NON-NLS-1$
                sb.append(key);
                sb.append("\" style=\"padding-right:8px;cursor: pointer;\" class=\"labelStyle\">"); //$NON-NLS-1$
                sb.append("<IMG SRC=\"/talendmdm/secure/img/genericUI/runnable_bullet.png\"/>&nbsp;"); //$NON-NLS-1$
                sb.append(description != null ? description.replace("Runnable#", "") : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                sb.append("</span>"); //$NON-NLS-1$
                processHtml.setHTML(sb.toString());
                processHtml.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent arg0) {
                        service.isExpired(UrlUtil.getLanguage(), new SessionAwareAsyncCallback<Boolean>() {

                            @Override
                            public void onSuccess(Boolean result1) {
                                final MessageBox box = MessageBox.wait(null, MessagesFactory.getMessages().waiting_msg(),
                                        MessagesFactory.getMessages().waiting_desc());
                                Timer t = new Timer() {

                                    @Override
                                    public void run() {
                                        box.close();
                                    }
                                };
                                t.schedule(600000);
                                service.runProcess(key, new SessionAwareAsyncCallback<String>() {

                                    @Override
                                    protected void doOnFailure(Throwable caught) {
                                        box.close();
                                        MessageBox.alert(MessagesFactory.getMessages().run_status(), MessagesFactory
                                                .getMessages().run_fail(), null);

                                    }

                                    @Override
                                    public void onSuccess(final String result2) {
                                        box.close();
                                        MessageBox.alert(MessagesFactory.getMessages().run_status(), MessagesFactory
                                                .getMessages().run_done(), new Listener<MessageBoxEvent>() {

                                            @Override
                                            public void handleEvent(MessageBoxEvent be) {
                                                if (result2.length() > 0) {
                                                    portal.openWindow(result2);
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
                fieldSet.add(processHtml);
                fieldSet.layout(true);

            }
        }
    }

    private void updateProcesses() {
        service.getStandaloneProcess(UrlUtil.getLanguage(), new SessionAwareAsyncCallback<Map<String, String>>() {

            @Override
            public void onSuccess(Map<String, String> newProcessMap) {
                if (compareProcesses(newProcessMap)) {
                    processMap = newProcessMap;
                    fieldSet.removeAll();
                    buildProcesses();
                }
            }
        });

    }

    protected boolean compareProcesses(Map<String, String> newProcessMap) {
        boolean flag = true;
        if (processMap.size() == newProcessMap.size()) {
            for (String key : newProcessMap.keySet()) {
                if (!processMap.containsKey(key)) {
                    flag = false;
                    break;
                }
            }
        } else {
            flag = false;
        }
        return flag;
    }
}
