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
package org.apache.camel.quarkus.component.jta.it;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;

import io.quarkus.artemis.core.runtime.ArtemisRuntimeConfig;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.jboss.narayana.jta.jms.TransactionHelperImpl;

@Dependent
public class XAConnectionFactoryConfiguration {

    // This class should be remove if https://github.com/quarkusio/quarkus/issues/14871 resolved
    // And the ConnectionFactory could be integrated with TransactionManager
    @Produces
    @Named("xaConnectionFactory")
    public ConnectionFactory getXAConnectionFactory(TransactionManager tm, ArtemisRuntimeConfig config) {
        XAConnectionFactory cf = new ActiveMQXAConnectionFactory(
                config.url, config.username.orElse(null), config.password.orElse(null));
        return new ConnectionFactoryProxy(cf, new TransactionHelperImpl(tm));

    }
}
