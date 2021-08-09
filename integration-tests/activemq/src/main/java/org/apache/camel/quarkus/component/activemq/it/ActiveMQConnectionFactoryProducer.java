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
package org.apache.camel.quarkus.component.activemq.it;

import java.util.Arrays;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.quarkus.component.messaging.it.Person;
import org.apache.camel.support.DefaultExchangeHolder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ActiveMQConnectionFactoryProducer {

    @ConfigProperty(name = "camel.component.activemq.broker-url")
    String brokerUrl;

    @Singleton
    @javax.enterprise.inject.Produces
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        factory.setTrustedPackages(Arrays.asList(
                Collections.class.getPackageName(),
                DefaultExchangeHolder.class.getPackageName(),
                IllegalStateException.class.getPackageName(),
                Person.class.getPackageName()));
        return factory;
    }
}
