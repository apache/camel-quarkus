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
package org.apache.camel.quarkus.component.jms.ibmmq.support;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;

/**
 * Before using the destinations in IBM MQ, it is needed to create them.
 */
public class IBMMQDestinations {
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "passw0rd";
    private static final String ADMIN_CHANNEL = "DEV.ADMIN.SVRCONN";

    private final String host;
    private final int port;
    private final String queueManagerName;

    private final PCFMessageAgent agent;
    // The destination can be created only once, otherwise the request will fail
    private final Set<String> createdQueues = new HashSet<>();
    private final Set<String> createdTopics = new HashSet<>();

    public IBMMQDestinations(String host, int port, String queueManagerName) {
        this.host = host;
        this.port = port;
        this.queueManagerName = queueManagerName;

        // Disable creating log files for client
        System.setProperty("com.ibm.msg.client.commonservices.log.status", "OFF");

        agent = createPCFAgent();
    }

    private MQQueueManager createQueueManager() {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(MQConstants.HOST_NAME_PROPERTY, host);
        properties.put(MQConstants.PORT_PROPERTY, port);
        properties.put(MQConstants.CHANNEL_PROPERTY, ADMIN_CHANNEL);
        properties.put(MQConstants.USE_MQCSP_AUTHENTICATION_PROPERTY, true);
        properties.put(MQConstants.USER_ID_PROPERTY, ADMIN_USER);
        properties.put(MQConstants.PASSWORD_PROPERTY, ADMIN_PASSWORD);
        try {
            return new MQQueueManager(queueManagerName, properties);
        } catch (MQException e) {
            throw new RuntimeException("Unable to create MQQueueManager:", e);
        }
    }

    private PCFMessageAgent createPCFAgent() {
        try {
            return new PCFMessageAgent(createQueueManager());
        } catch (MQDataException e) {
            throw new RuntimeException("Unable to create PCFMessageAgent:", e);
        }
    }

    private void sendRequest(PCFMessage request) {
        try {
            agent.send(request);
        } catch (Exception e) {
            throw new RuntimeException("Unable to send PCFMessage:", e);
        }
    }

    public void createQueue(String queueName) {
        if (!createdQueues.contains(queueName)) {
            PCFMessage request = new PCFMessage(MQConstants.MQCMD_CREATE_Q);
            request.addParameter(MQConstants.MQCA_Q_NAME, queueName);
            request.addParameter(MQConstants.MQIA_Q_TYPE, MQConstants.MQQT_LOCAL);
            sendRequest(request);
            createdQueues.add(queueName);
        }
    }

    public void createTopic(String topicName) {
        if (!createdTopics.contains(topicName)) {
            PCFMessage request = new PCFMessage(MQConstants.MQCMD_CREATE_TOPIC);
            request.addParameter(MQConstants.MQCA_TOPIC_NAME, topicName);
            request.addParameter(MQConstants.MQCA_TOPIC_STRING, topicName);
            request.addParameter(MQConstants.MQIA_TOPIC_TYPE, MQConstants.MQTOPT_LOCAL);
            sendRequest(request);
            createdTopics.add(topicName);
        }
    }
}
