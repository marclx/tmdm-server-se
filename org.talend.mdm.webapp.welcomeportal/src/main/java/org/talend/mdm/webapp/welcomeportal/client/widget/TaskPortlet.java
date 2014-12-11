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

import java.util.Map;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.welcomeportal.client.MainFramePanel;
import org.talend.mdm.webapp.welcomeportal.client.WelcomePortal;
import org.talend.mdm.webapp.welcomeportal.client.i18n.MessagesFactory;

import com.extjs.gxt.ui.client.widget.custom.Portal;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;

public class TaskPortlet extends BasePortlet {

    private static String DSCTASKTYPE_NEW = "new"; //$NON-NLS-1$

    private static String DSCTASKTYPE_PENDING = "pending"; //$NON-NLS-1$

    private static String WORKFLOWTASKS_PREFIX = "<span id=\"workflowtasks\" style=\"padding-right:8px;cursor: pointer;\" class=\"labelStyle\" title=\"" + MessagesFactory.getMessages().tasks_title() + "\">"; //$NON-NLS-1$ //$NON-NLS-2$

    private static String DSCTASKS_PREFIX = "<span id=\"dsctasks\" style=\"padding-right:8px;cursor: pointer;\" class=\"labelStyle\" title=\"" + MessagesFactory.getMessages().tasks_title() + "\">"; //$NON-NLS-1$ //$NON-NLS-2$

    private boolean isHiddenWorkFlowTask = true;

    private boolean isHiddenDSCTask = true;

    private ClickHandler workflowClikcHanlder;

    private ClickHandler dscClikcHanlder;

    private int numOfWorkflowTask;

    private Map<String, Integer> numOfDSCTasks;

    private StringBuilder sbForWorkflowtasks;

    private StringBuilder sbForDsctasks;

    private HTML taskHtml_workflow;

    private HTML taskHtml_dsc;

    public TaskPortlet(final Portal portal) {
        super(WelcomePortal.TASKS, portal);

        isHiddenWorkFlowTask = ((MainFramePanel) portal).isHiddenWorkFlowTask();
        isHiddenDSCTask = ((MainFramePanel) portal).isHiddenDSCTask();

        initConfigSettings();

        label.setText(MessagesFactory.getMessages().loading_task_msg());

        workflowClikcHanlder = new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                ((MainFramePanel) portal).itemClick(WelcomePortal.WORKFLOW_TASKCONTEXT, WelcomePortal.WORKFLOW_TASKAPP);
            }

        };

        dscClikcHanlder = new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                ((MainFramePanel) portal).itemClick(WelcomePortal.DSC_TASKCONTEXT, WelcomePortal.DSC_TASKAPP);
            }

        };
        initLinks();
    }

    @Override
    public void refresh() {
        updateLinks();
    }

    private void initLinks() {

        if (!isHiddenWorkFlowTask && isHiddenDSCTask) {
            service.getWorkflowTaskMsg(new SessionAwareAsyncCallback<Integer>() {

                @Override
                public void onSuccess(Integer num) {
                    numOfWorkflowTask = num;

                    buildAndShowWorkflowTasksOnly();
                    autoRefresh(configModel.isAutoRefresh());
                }

            });
        }

        if (!isHiddenDSCTask && isHiddenWorkFlowTask) {
            service.getDSCTaskMsg(new SessionAwareAsyncCallback<Map<String, Integer>>() {

                @Override
                public void onSuccess(Map<String, Integer> dscTasks) {
                    numOfDSCTasks = dscTasks;

                    buildAndShowDSCTasksOnly();
                    autoRefresh(configModel.isAutoRefresh());
                }
            });
        }

        if (!isHiddenWorkFlowTask && !isHiddenDSCTask) {
            service.getWorkflowTaskMsg(new SessionAwareAsyncCallback<Integer>() {

                @Override
                public void onSuccess(final Integer workflowNum) {

                    service.getDSCTaskMsg(new SessionAwareAsyncCallback<Map<String, Integer>>() {

                        @Override
                        public void onSuccess(Map<String, Integer> dscTasks) {
                            numOfWorkflowTask = workflowNum;
                            numOfDSCTasks = dscTasks;
                            buildAndShowWorkflowTasksAndDSCTasks();
                            autoRefresh(configModel.isAutoRefresh());
                        }
                    });
                }
            });
        }
    }

    private void updateLinks() {

        if (!isHiddenWorkFlowTask && isHiddenDSCTask) {
            service.getWorkflowTaskMsg(new SessionAwareAsyncCallback<Integer>() {

                @Override
                public void onSuccess(Integer num) {
                    if (numOfWorkflowTask != num) {
                        numOfWorkflowTask = num;
                        set.removeAll();

                        buildAndShowWorkflowTasksOnly();
                    }

                }
            });
        }

        if (!isHiddenDSCTask && isHiddenWorkFlowTask) {
            service.getDSCTaskMsg(new SessionAwareAsyncCallback<Map<String, Integer>>() {

                @Override
                public void onSuccess(Map<String, Integer> dscTasks) {
                    if (numOfDSCTasks.get(DSCTASKTYPE_NEW).equals(dscTasks.get(DSCTASKTYPE_NEW))
                            && numOfDSCTasks.get(DSCTASKTYPE_PENDING).equals(dscTasks.get(DSCTASKTYPE_PENDING))) {
                        return;
                    }

                    numOfDSCTasks = dscTasks;
                    set.removeAll();

                    buildAndShowDSCTasksOnly();
                }
            });
        }

        if (!isHiddenWorkFlowTask && !isHiddenDSCTask) {
            service.getWorkflowTaskMsg(new SessionAwareAsyncCallback<Integer>() {

                @Override
                public void onSuccess(final Integer workflowNum) {

                    service.getDSCTaskMsg(new SessionAwareAsyncCallback<Map<String, Integer>>() {

                        @Override
                        public void onSuccess(Map<String, Integer> dscTasks) {
                            if (numOfWorkflowTask == workflowNum
                                    && numOfDSCTasks.get(DSCTASKTYPE_NEW).equals(dscTasks.get(DSCTASKTYPE_NEW))
                                    && numOfDSCTasks.get(DSCTASKTYPE_PENDING).equals(dscTasks.get(DSCTASKTYPE_PENDING))) {
                                return;
                            }
                            numOfWorkflowTask = workflowNum;
                            numOfDSCTasks = dscTasks;
                            set.removeAll();
                            buildAndShowWorkflowTasksAndDSCTasks();
                        }
                    });
                }
            });
        }
    }

    private void buildAndShowWorkflowTasksOnly() {
        sbForWorkflowtasks = new StringBuilder(WORKFLOWTASKS_PREFIX);

        if (numOfWorkflowTask == 0) {
            label.setText(MessagesFactory.getMessages().no_tasks());
            set.setVisible(false);
        } else {
            String sbNum = buildMessageForWorkflowTasks(numOfWorkflowTask);
            sbForWorkflowtasks.append(sbNum);
            label.setText(MessagesFactory.getMessages().tasks_desc());
            set.setVisible(true);
        }
        sbForWorkflowtasks.append("</span>"); //$NON-NLS-1$

        taskHtml_workflow = new HTML();
        taskHtml_workflow.setHTML(sbForWorkflowtasks.toString());

        taskHtml_workflow.addClickHandler(workflowClikcHanlder);
        set.add(taskHtml_workflow);
        set.layout(true);
    }

    private void buildAndShowDSCTasksOnly() {
        sbForDsctasks = new StringBuilder(DSCTASKS_PREFIX);

        int total = numOfDSCTasks.get(DSCTASKTYPE_NEW) + numOfDSCTasks.get(DSCTASKTYPE_PENDING);
        if (total <= 0) {
            label.setText(MessagesFactory.getMessages().no_tasks());
            set.setVisible(false);
        } else {
            String sbNum = buildMessageForDSCTasks(numOfDSCTasks);
            sbForDsctasks.append(sbNum);
            label.setText(MessagesFactory.getMessages().tasks_desc());
            set.setVisible(true);
        }
        sbForDsctasks.append("</span>"); //$NON-NLS-1$

        taskHtml_dsc = new HTML();
        taskHtml_dsc.setHTML(sbForDsctasks.toString());

        taskHtml_dsc.addClickHandler(dscClikcHanlder);

        set.add(taskHtml_dsc);
        set.layout(true);
    }

    private void buildAndShowWorkflowTasksAndDSCTasks() {
        sbForWorkflowtasks = new StringBuilder(WORKFLOWTASKS_PREFIX);
        sbForDsctasks = new StringBuilder(DSCTASKS_PREFIX);

        taskHtml_workflow = new HTML();
        taskHtml_dsc = new HTML();

        int dscNum = numOfDSCTasks.get(DSCTASKTYPE_NEW) + numOfDSCTasks.get(DSCTASKTYPE_PENDING);
        if (numOfWorkflowTask + dscNum <= 0) {
            label.setText(MessagesFactory.getMessages().no_tasks());
            set.setVisible(false);
        } else {
            label.setText(MessagesFactory.getMessages().tasks_desc());
            set.setVisible(true);
            if (numOfWorkflowTask > 0 && dscNum == 0) {
                String sbNum = buildMessageForWorkflowTasks(numOfWorkflowTask);
                sbForWorkflowtasks.append(sbNum);
                sbForWorkflowtasks.append("</span>"); //$NON-NLS-1$

                taskHtml_workflow.setHTML(sbForWorkflowtasks.toString());

                taskHtml_workflow.addClickHandler(workflowClikcHanlder);

                set.add(taskHtml_workflow);
            } else if (numOfWorkflowTask == 0 && dscNum > 0) {
                String sbNum = buildMessageForDSCTasks(numOfDSCTasks);
                sbForDsctasks.append(sbNum);
                sbForDsctasks.append("</span>"); //$NON-NLS-1$

                taskHtml_dsc.setHTML(sbForDsctasks.toString());

                taskHtml_dsc.addClickHandler(dscClikcHanlder);

                set.add(taskHtml_dsc);
            } else {
                String sbNumWFT = buildMessageForWorkflowTasks(numOfWorkflowTask);
                sbForWorkflowtasks.append(sbNumWFT);
                sbForWorkflowtasks.append("</span>"); //$NON-NLS-1$

                String sbNumDSC = buildMessageForDSCTasks(numOfDSCTasks);
                sbForDsctasks.append(sbNumDSC);
                sbForDsctasks.append("</span>"); //$NON-NLS-1$

                taskHtml_workflow.setHTML(sbForWorkflowtasks.toString());
                taskHtml_dsc.setHTML(sbForDsctasks.toString());

                taskHtml_workflow.addClickHandler(workflowClikcHanlder);

                taskHtml_dsc.addClickHandler(dscClikcHanlder);

                set.add(taskHtml_workflow);
                set.add(taskHtml_dsc);
            }
        }
        set.layout(true);
    }

    private String buildMessageForWorkflowTasks(int num) {
        StringBuilder message = new StringBuilder("<IMG SRC=\"/talendmdm/secure/img/genericUI/task-list-icon.png\"/>&nbsp;"); //$NON-NLS-1$                                
        message.append(MessagesFactory.getMessages().waiting_task_prefix());
        message.append("&nbsp;<b style=\"color: red;\">"); //$NON-NLS-1$
        message.append(num);
        message.append("</b>&nbsp;"); //$NON-NLS-1$
        message.append(MessagesFactory.getMessages().waiting_workflowtask_suffix());

        return message.toString();
    }

    private String buildMessageForDSCTasks(Map<String, Integer> dscTasks) {
        StringBuilder message = new StringBuilder("<IMG SRC=\"/talendmdm/secure/img/genericUI/task-list-icon.png\"/>&nbsp;"); //$NON-NLS-1$                                
        message.append(MessagesFactory.getMessages().waiting_task_prefix());
        message.append("&nbsp;<b style=\"color: red;\">"); //$NON-NLS-1$
        message.append(MessagesFactory.getMessages().waiting_dsctask(dscTasks.get(DSCTASKTYPE_NEW), dscTasks.get(DSCTASKTYPE_PENDING)));
        message.append("</b>&nbsp;"); //$NON-NLS-1$
        message.append(MessagesFactory.getMessages().waiting_dsctask_suffix());

        return message.toString();
    }
}
