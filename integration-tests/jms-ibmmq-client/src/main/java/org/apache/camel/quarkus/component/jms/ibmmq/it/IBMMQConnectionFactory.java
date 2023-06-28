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
import jakarta.enterprise.context.Dependent;
import org.eclipse.microprofile.config.ConfigProvider;

@Dependent
public class IBMMQConnectionFactory extends MQConnectionFactory {

    public IBMMQConnectionFactory() {
        setHostName(ConfigProvider.getConfig().getValue("ibm.mq.host", String.class));
        try {
            setPort(ConfigProvider.getConfig().getValue("ibm.mq.port", Integer.class));
            setChannel(ConfigProvider.getConfig().getValue("ibm.mq.channel", String.class));
            setQueueManager(ConfigProvider.getConfig().getValue("ibm.mq.queueManagerName", String.class));
            setTransportType(WMQConstants.WMQ_CM_CLIENT);
            setStringProperty(WMQConstants.USERID,
                    ConfigProvider.getConfig().getValue("ibm.mq.user", String.class));
            setStringProperty(WMQConstants.PASSWORD,
                    ConfigProvider.getConfig().getValue("ibm.mq.password", String.class));
        } catch (Exception e) {
            throw new RuntimeException("Unable to create new IBM MQ connection factory", e);
        }
    }
}
