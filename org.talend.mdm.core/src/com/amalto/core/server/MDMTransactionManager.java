/*
 * Copyright (C) 2006-2013 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.server;

import com.amalto.core.storage.transaction.Transaction;
import com.amalto.core.storage.transaction.TransactionManager;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

class MDMTransactionManager implements TransactionManager {

    private static final Logger LOGGER = Logger.getLogger(MDMTransactionManager.class);

    private static final Map<Thread, Transaction> currentTransactions = new HashMap<Thread, Transaction>();

    private static final Map<String, Transaction> activeTransactions = new HashMap<String, Transaction>();

    @Override
    public Transaction create(Transaction.Lifetime lifetime) {
        MDMTransaction transaction = new MDMTransaction(lifetime);
        synchronized (activeTransactions) {
            activeTransactions.put(transaction.getId(), transaction);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("New transaction: " + transaction.toString());
        }
        return transaction;
    }

    @Override
    public Transaction get(String transactionId) {
        synchronized (activeTransactions) {
            return activeTransactions.get(transactionId);
        }
    }

    @Override
    public void remove(Transaction transaction) {
        synchronized (currentTransactions) {
            currentTransactions.remove(Thread.currentThread());
        }
        synchronized (activeTransactions) {
            activeTransactions.remove(transaction.getId());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Transaction removed: " + transaction.getId());
        }
    }

    @Override
    public void init() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(20000);
                        // TODO Log
                        System.out.println("Active transaction(s) (" + activeTransactions.size() + ")");
                        for (Map.Entry<String, Transaction> currentTransaction : activeTransactions.entrySet()) {
                            System.out.println(currentTransaction.getKey());
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();
    }

    @Override
    public void close() {
        synchronized (activeTransactions) {
            for (Transaction transaction : activeTransactions.values()) {
                transaction.rollback();
            }
        }
    }

    @Override
    public Transaction currentTransaction() {
        synchronized (currentTransactions) {
            Transaction transaction = currentTransactions.get(Thread.currentThread());
            if (transaction == null) {
                return associate(create(Transaction.Lifetime.AD_HOC));
            }
            return transaction;
        }
    }

    @Override
    public Transaction associate(Transaction transaction) {
        synchronized (currentTransactions) {
            currentTransactions.put(Thread.currentThread(), transaction);
        }
        return transaction;
    }

    @Override
    public void dissociate(Transaction transaction) {
        synchronized (currentTransactions) {
            if (transaction == currentTransactions.get(Thread.currentThread())) {
                currentTransactions.remove(Thread.currentThread());
            }
        }
    }

    @Override
    public boolean hasTransaction() {
        return currentTransactions.get(Thread.currentThread()) != null;
    }
}