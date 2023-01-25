/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.jta;

import jakarta.inject.Inject;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.apache.camel.CamelException;
import org.apache.camel.jta.JtaTransactionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods for transaction handling
 */
public abstract class TransactionalJtaTransactionPolicy extends JtaTransactionPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionalJtaTransactionPolicy.class);

    @Inject
    TransactionManager transactionManager;

    protected void runWithTransaction(final Runnable runnable, final boolean isNew) throws Throwable {
        if (isNew) {
            begin();
        }
        try {
            runnable.run();
        } catch (Throwable e) {
            rollback(isNew);
            throw e;
        }
        if (isNew) {
            commit();
        }
    }

    private void begin() throws Exception {
        transactionManager.begin();
    }

    private void commit() throws Exception {
        try {
            transactionManager.commit();
        } catch (HeuristicMixedException | HeuristicRollbackException | RollbackException | SystemException e) {
            throw new CamelException("Unable to commit transaction", e);
        } catch (Exception | Error e) {
            rollback(true);
            throw e;
        }
    }

    final protected void rollback(boolean isNew) throws Exception {
        try {
            if (isNew) {
                transactionManager.rollback();
            } else {
                transactionManager.setRollbackOnly();
            }
        } catch (Throwable e) {
            LOG.warn("Could not rollback transaction!", e);
        }
    }

    final protected Transaction suspendTransaction() throws Exception {
        return transactionManager.suspend();
    }

    final protected void resumeTransaction(final Transaction suspendedTransaction) {
        if (suspendedTransaction == null) {
            return;
        }

        try {
            transactionManager.resume(suspendedTransaction);
        } catch (Throwable e) {
            LOG.warn("Could not resume transaction!", e);
        }
    }

    final protected boolean hasActiveTransaction() throws Exception {
        return transactionManager.getStatus() != Status.STATUS_MARKED_ROLLBACK
                && transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION;
    }
}
