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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.jsoup.helper.Validate.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class MandatoryJtaTransactionPolicyTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MockTransactionManagerProducer.class));

    @Inject
    TransactionManager transactionManager;

    @Inject
    @Named("PROPAGATION_MANDATORY")
    MandatoryJtaTransactionPolicy transactionPolicy;

    @AfterEach
    public void afterEach() {
        reset(transactionManager);
    }

    @Test
    public void runTransactionPolicyNoTransaction() throws Exception {
        when(transactionManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);
        try {
            transactionPolicy.run(() -> fail("Transaction policy should not run"));
        } catch (Throwable throwable) {
            assertTrue(throwable instanceof IllegalStateException);
        }
    }

    @Test
    public void runTransactionPolicyMarkedRollback() throws Exception {
        when(transactionManager.getStatus()).thenReturn(Status.STATUS_MARKED_ROLLBACK);
        try {
            transactionPolicy.run(() -> fail("Transaction policy should not run"));
        } catch (Throwable throwable) {
            assertTrue(throwable instanceof IllegalStateException);
        }
    }

    @Test
    public void runTransactionPolicyActiveTransaction() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        when(transactionManager.getStatus()).thenReturn(Status.STATUS_ACTIVE);
        try {
            transactionPolicy.run(() -> latch.countDown());
        } catch (Throwable throwable) {
            fail("Expected transaction policy to run successfully");
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
