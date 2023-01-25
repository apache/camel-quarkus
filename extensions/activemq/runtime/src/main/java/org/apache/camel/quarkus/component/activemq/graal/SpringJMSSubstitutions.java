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
package org.apache.camel.quarkus.component.activemq.graal;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.Topic;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

/**
 * ActiveMQ is tied to JMS 1.1, so we need to disable any code paths in Spring JMS that try to leverage JMS 2.x APIs
 */
final class SpringJMSSubstitutions {
}

@TargetClass(AbstractMessageListenerContainer.class)
final class SubstituteAbstractMessageListenerContainer {

    @Alias
    private String subscriptionName;
    @Alias
    private volatile String messageSelector;
    @Alias
    private boolean pubSubDomain;
    @Alias
    private boolean subscriptionDurable;
    @Alias
    private boolean pubSubNoLocal;

    @Substitute
    protected MessageConsumer createConsumer(Session session, Destination destination) throws JMSException {
        // Removes references to JMS 2.0 shared subscriptions
        if (pubSubDomain && destination instanceof Topic) {
            if (subscriptionDurable) {
                return session.createDurableSubscriber(
                        (Topic) destination, subscriptionName, messageSelector, pubSubNoLocal);
            } else {
                return session.createConsumer(destination, messageSelector, pubSubNoLocal);
            }
        } else {
            return session.createConsumer(destination, messageSelector);
        }
    }
}

@TargetClass(JmsTemplate.class)
final class SubstituteJmsTemplate {

    @Alias
    private boolean explicitQosEnabled;
    @Alias
    private int deliveryMode;
    @Alias
    private int priority;
    @Alias
    private long timeToLive;

    @Substitute
    protected void doSend(MessageProducer producer, Message message) throws JMSException {
        // Removes references to JMS 2.0 delivery delay lookup
        if (explicitQosEnabled) {
            producer.send(message, deliveryMode, priority, timeToLive);
        } else {
            producer.send(message);
        }
    }
}
