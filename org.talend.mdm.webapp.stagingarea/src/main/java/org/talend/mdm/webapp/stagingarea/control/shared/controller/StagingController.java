package org.talend.mdm.webapp.stagingarea.control.shared.controller;

import com.google.gwt.user.client.Timer;
import org.talend.mdm.webapp.stagingarea.control.client.StagingAreaControl;
import org.talend.mdm.webapp.stagingarea.control.shared.model.StagingAreaConfiguration;

public class StagingController {

    private final Timer timer;

    private boolean     autoRefresh;

    public StagingController() {
        timer = new Timer() {

            @Override
            public void run() {
                Controllers.get().getSummaryController().refresh();
                Controllers.get().getValidationController().refresh();
            }
        };
    }

    public void autoRefresh(boolean auto) {
        if (this.autoRefresh != auto) {
            this.autoRefresh = auto;
            if (auto) {
                StagingAreaConfiguration stagingAreaConfig = StagingAreaControl.getStagingAreaConfig();
                timer.scheduleRepeating(stagingAreaConfig.getRefreshIntervals());
            } else {
                timer.cancel();
            }
        }
    }
}
