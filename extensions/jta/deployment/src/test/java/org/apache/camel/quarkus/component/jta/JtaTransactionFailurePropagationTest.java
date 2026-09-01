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

import java.util.Arrays;

import io.quarkus.test.QuarkusExtensionTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * A transaction whose rollback, rollback marking or resumption fails must not let that failure disappear, and must not
 * let it replace the exception that made the rollback necessary.
 */
public class JtaTransactionFailurePropagationTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MockTransactionManagerProducer.class, MockTransaction.class));

    @Inject
    TransactionManager transactionManager;

    @Inject
    @Named("PROPAGATION_REQUIRED")
    RequiredJtaTransactionPolicy requiredPolicy;

    @Inject
    @Named("PROPAGATION_REQUIRES_NEW")
    RequiresNewJtaTransactionPolicy requiresNewPolicy;

    @AfterEach
    public void afterEach() {
        reset(transactionManager);
    }

    @Test
    public void failedRollbackMarkingIsReportedAndKeepsTheOriginalCause() throws Exception {
        // Participating in an outer transaction, so the policy marks it rather than rolling it back
        when(transactionManager.getStatus()).thenReturn(Status.STATUS_ACTIVE);
        doThrow(new SystemException("mark failed")).when(transactionManager).setRollbackOnly();

        Exception routeFailure = new Exception("route failed");
        Throwable thrown = assertThrows(Throwable.class, () -> requiredPolicy.run(() -> {
            throw routeFailure;
        }));

        // The original failure still surfaces
        assertEquals(routeFailure, thrown);
        // and the marking failure rides along rather than vanishing into a log line
        assertTrue(Arrays.stream(thrown.getSuppressed())
                .anyMatch(s -> s.getMessage().contains("Unable to mark the transaction for rollback")),
                "expected the failed setRollbackOnly to be attached, got "
                        + Arrays.toString(thrown.getSuppressed()));
    }

    @Test
    public void failedRollbackIsReportedAndKeepsTheOriginalCause() throws Exception {
        when(transactionManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);
        doThrow(new SystemException("rollback failed")).when(transactionManager).rollback();

        Exception routeFailure = new Exception("route failed");
        Throwable thrown = assertThrows(Throwable.class, () -> requiredPolicy.run(() -> {
            throw routeFailure;
        }));

        assertEquals(routeFailure, thrown);
        assertTrue(Arrays.stream(thrown.getSuppressed())
                .anyMatch(s -> s.getMessage().contains("Unable to rollback transaction")),
                "expected the failed rollback to be attached, got " + Arrays.toString(thrown.getSuppressed()));
    }

    @Test
    public void failedResumeIsReportedWhenTheBodySucceeded() throws Exception {
        Transaction suspended = new MockTransaction();
        when(transactionManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);
        when(transactionManager.suspend()).thenReturn(suspended);
        doThrow(new SystemException("resume failed")).when(transactionManager).resume(suspended);

        // Nothing else is in flight, so the resume failure is the failure
        Throwable thrown = assertThrows(Throwable.class, () -> requiresNewPolicy.run(() -> {
        }));

        assertTrue(thrown.getMessage().contains("Unable to resume transaction"),
                "expected the resume failure to surface, got " + thrown);
    }

    @Test
    public void failedResumeDoesNotReplaceTheBodyFailure() throws Exception {
        Transaction suspended = new MockTransaction();
        when(transactionManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);
        when(transactionManager.suspend()).thenReturn(suspended);
        doThrow(new SystemException("resume failed")).when(transactionManager).resume(suspended);

        Exception routeFailure = new Exception("route failed");
        Throwable thrown = assertThrows(Throwable.class, () -> requiresNewPolicy.run(() -> {
            throw routeFailure;
        }));

        // The body's failure wins; the resume failure is attached to it
        assertEquals(routeFailure, thrown);
        assertTrue(Arrays.stream(thrown.getSuppressed())
                .anyMatch(s -> s.getMessage().contains("Unable to resume transaction")),
                "expected the failed resume to be attached, got " + Arrays.toString(thrown.getSuppressed()));
    }
}
