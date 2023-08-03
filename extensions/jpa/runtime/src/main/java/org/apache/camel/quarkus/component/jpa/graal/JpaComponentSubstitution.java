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

package org.apache.camel.quarkus.component.jpa.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import jakarta.persistence.EntityManagerFactory;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.component.jpa.TransactionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TargetClass(JpaComponent.class)
final public class JpaComponentSubstitution {
    @Alias
    private TransactionStrategy transactionStrategy;

    @Alias
    private EntityManagerFactory entityManagerFactory;

    @Alias
    private void initEntityManagerFactory() {
    }

    @Substitute
    protected void doInit() throws Exception {
        initEntityManagerFactory();

        Logger LOG = LoggerFactory.getLogger(JpaComponent.class);
        if (entityManagerFactory == null) {
            LOG.warn(
                    "No EntityManagerFactory has been configured on this JpaComponent. Each JpaEndpoint will auto create their own EntityManagerFactory.");
        }

        if (transactionStrategy != null) {
            LOG.info("Using TransactionStrategy configured: {}", transactionStrategy);
        }
    }
}
