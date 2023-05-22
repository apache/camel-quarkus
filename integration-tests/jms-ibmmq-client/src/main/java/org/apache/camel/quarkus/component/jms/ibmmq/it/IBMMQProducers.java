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
package org.apache.camel.quarkus.component.jms.ibmmq.it;

import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.enterprise.inject.Produces;
import jakarta.jms.ConnectionFactory;
import org.eclipse.microprofile.config.ConfigProvider;

public class IBMMQProducers {
    @Produces
    ConnectionFactory createConnectionFactory() {
        MQConnectionFactory connectionFactory = new MQConnectionFactory();
        connectionFactory.setHostName(ConfigProvider.getConfig().getValue("ibm.mq.host", String.class));
        try {
            connectionFactory.setPort(ConfigProvider.getConfig().getValue("ibm.mq.port", Integer.class));
            connectionFactory.setChannel(ConfigProvider.getConfig().getValue("ibm.mq.channel", String.class));
            connectionFactory.setQueueManager(ConfigProvider.getConfig().getValue("ibm.mq.queueManagerName", String.class));
            connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            connectionFactory.setStringProperty(WMQConstants.USERID,
                    ConfigProvider.getConfig().getValue("ibm.mq.user", String.class));
            connectionFactory.setStringProperty(WMQConstants.PASSWORD,
                    ConfigProvider.getConfig().getValue("ibm.mq.password", String.class));
        } catch (Exception e) {
            throw new RuntimeException("Unable to create new IBM MQ connection factory", e);
        }
        return connectionFactory;
    }
}
