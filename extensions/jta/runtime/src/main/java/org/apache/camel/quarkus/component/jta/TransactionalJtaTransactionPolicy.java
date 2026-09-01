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
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.jta.JtaTransactionPolicy;

/**
 * Helper methods for transaction handling
 */
public abstract class TransactionalJtaTransactionPolicy extends JtaTransactionPolicy {

    @Inject
    TransactionManager transactionManager;

    protected void runWithTransaction(final Runnable runnable, final boolean isNew) throws Throwable {
        if (isNew) {
            begin();
        }
        try {
            runnable.run();
        } catch (Throwable e) {
            rollbackSuppressing(e, isNew);
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
            rollbackSuppressing(e, true);
            throw e;
        }
    }

    /**
     * Rolls the transaction back, or marks it for rollback when it belongs to an outer policy.
     *
     * A failure here is raised rather than logged and discarded. Callers that already have an exception on its way
     * out attach this one to it through {@link #rollbackSuppressing(Throwable, boolean)}, so the original cause is
     * never replaced.
     */
    final protected void rollback(boolean isNew) throws Exception {
        try {
            if (isNew) {
                transactionManager.rollback();
            } else {
                transactionManager.setRollbackOnly();
            }
        } catch (Throwable e) {
            throw new CamelException(
                    isNew ? "Unable to rollback transaction" : "Unable to mark the transaction for rollback", e);
        }
    }

    /**
     * Rolls back while an exception is already on its way out, attaching a rollback failure to it as a suppressed
     * exception. Replacing the original would hide why the rollback was needed in the first place.
     */
    private void rollbackSuppressing(Throwable primary, boolean isNew) {
        try {
            rollback(isNew);
        } catch (Throwable rollbackFailure) {
            primary.addSuppressed(rollbackFailure);
        }
    }

    final protected Transaction suspendTransaction() throws Exception {
        return transactionManager.suspend();
    }

    /**
     * Resumes the suspended transaction, raising a failure rather than logging and discarding it, so that work after
     * this point does not silently continue outside the transaction the caller expects to be restored.
     */
    final protected void resumeTransaction(final Transaction suspendedTransaction) {
        try {
            resumeTransaction(suspendedTransaction, null);
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }
    }

    /**
     * Resumes the suspended transaction while {@code primary} may already be on its way out, which is the case in the
     * `finally` block of a policy that suspends. A resume failure is attached to {@code primary} when there is one and
     * raised on its own otherwise, so it is neither lost nor allowed to replace the original failure.
     */
    final protected void resumeTransaction(final Transaction suspendedTransaction, final Throwable primary)
            throws Exception {
        if (suspendedTransaction == null) {
            return;
        }

        try {
            transactionManager.resume(suspendedTransaction);
        } catch (Throwable e) {
            CamelException failure = new CamelException("Unable to resume transaction", e);
            if (primary != null) {
                primary.addSuppressed(failure);
            } else {
                throw failure;
            }
        }
    }

    final protected boolean hasActiveTransaction() throws Exception {
        return transactionManager.getStatus() != Status.STATUS_MARKED_ROLLBACK
                && transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION;
    }
}
