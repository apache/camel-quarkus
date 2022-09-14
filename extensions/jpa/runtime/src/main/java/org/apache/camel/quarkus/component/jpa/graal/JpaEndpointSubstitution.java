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

import javax.persistence.EntityManagerFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.component.jpa.JpaEndpoint;
import org.apache.camel.component.jpa.TransactionStrategy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@TargetClass(JpaEndpoint.class)
final public class JpaEndpointSubstitution {
    @Alias
    private TransactionStrategy transactionStrategy;

    @Substitute
    protected EntityManagerFactory createEntityManagerFactory() {
        throw new UnsupportedOperationException("createEntityManagerFactory is not supported");
    }

    @Substitute
    protected TransactionTemplate createTransactionTemplate() {
        throw new UnsupportedOperationException("createTransactionTemplate is not supported");
    }

    @Substitute
    public PlatformTransactionManager getTransactionManager() {
        throw new UnsupportedOperationException("getTransactionManager is not supported");
    }

    @Substitute
    public TransactionStrategy getTransactionStrategy() {
        return transactionStrategy;
    }

}
