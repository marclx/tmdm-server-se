/*
 * Copyright (C) 2006-2012 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.storage.task;

import com.amalto.core.storage.record.DataRecord;
import org.apache.log4j.Logger;
import org.quartz.SchedulerConfigException;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.ThreadPool;

import javax.resource.spi.work.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class ThreadDispatcher implements Closure {

    private static final Logger LOGGER = Logger.getLogger(ThreadDispatcher.class);

    private final BlockingQueue<DataRecord> queue = new LinkedBlockingQueue<DataRecord>();

    private final Set<ConsumerRunnable> childClosures = new HashSet<ConsumerRunnable>();

    private long startTime;

    private int count = 0;

    ThreadDispatcher(int threadNumber, Closure closure) {
        for (int i = 0; i < threadNumber; i++) {
            childClosures.add(new ConsumerRunnable(queue, closure.copy()));
        }
    }

    private WorkManager getManager() {
        return new SimpleWorkManager();
    }

    public void begin() {
        startTime = System.currentTimeMillis();
        WorkManager workManager = getManager();
        try {
             for (ConsumerRunnable childThread : childClosures) {
                workManager.doWork(childThread);
            }
        } catch (WorkException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(DataRecord record) {
        try {
            if (!queue.offer(record)) {
                LOGGER.warn("Not enough consumers for records!");
                queue.put(record); // Wait for free room in queue.
            }
            count++;

            if (LOGGER.isDebugEnabled()) {
                if (count % 1000 == 0) {
                    LOGGER.debug("doc/s -> " + count / ((System.currentTimeMillis() - startTime) / 1000f) + " / queue size: " + queue.size());
                }
            }
        } catch (Exception e) {
            // TODO Logger
        }
    }

    public void end() {
        for (int i = 0; i < childClosures.size(); i++) {
            queue.offer(new EndDataRecord());
        }
        for (ConsumerRunnable childClosure : childClosures) {
            try {
                childClosure.waitForEnd();
            } catch (InterruptedException e) {
                throw new RuntimeException("Child executions did not complete normally.", e);
            }
        }
    }

    public Closure copy() {
        return this;
    }

    public static class EndDataRecord extends DataRecord {
        public EndDataRecord() {
            super(null, null);
        }
    }

    private static class SimpleWorkManager implements WorkManager {
        
        private final ThreadPool pool = new SimpleThreadPool(Runtime.getRuntime().availableProcessors() * 2, Thread.MAX_PRIORITY - 1);

        private SimpleWorkManager() {
            try {
                pool.initialize();
            } catch (SchedulerConfigException e) {
                throw new RuntimeException(e);
            }
        }

        public void doWork(Work work) throws WorkException {
            pool.runInThread(work);
        }

        public void doWork(Work work, long l, ExecutionContext executionContext, WorkListener workListener) throws WorkException {
            pool.runInThread(work);
        }

        public long startWork(Work work) throws WorkException {
            pool.runInThread(work);
            return 0;
        }

        public long startWork(Work work, long l, ExecutionContext executionContext, WorkListener workListener) throws WorkException {
            pool.runInThread(work);
            return 0;
        }

        public void scheduleWork(Work work) throws WorkException {
            pool.runInThread(work);
        }

        public void scheduleWork(Work work, long l, ExecutionContext executionContext, WorkListener workListener) throws WorkException {
            pool.runInThread(work);
        }
    }
}