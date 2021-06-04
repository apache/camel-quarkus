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
package org.apache.camel.quarkus.component.messaging.it;

import javax.inject.Named;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.camel.component.jms.MessageListenerContainerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

public class JmsProducers {

    @Named
    public MessageListenerContainerFactory customMessageListener() {
        return jmsEndpoint -> new DefaultMessageListenerContainer();
    }

    @Named
    public DestinationResolver customDestinationResolver() {
        return (session, destinationName, pubSubDomain) -> {
            if (destinationName.equals("ignored")) {
                // Ignore and override the original queue name
                return session.createQueue("destinationOverride");
            }

            if (pubSubDomain) {
                return session.createTopic(destinationName);
            } else {
                return session.createQueue(destinationName);
            }
        };
    }

    @Named
    public PlatformTransactionManager transactionManager(UserTransaction userTransaction,
            TransactionManager transactionManager) {
        return new JtaTransactionManager(userTransaction, transactionManager);
    }

    @Named
    public MessageConverter customMessageConverter() {
        return new MessageConverter() {
            @Override
            public Message toMessage(Object o, Session session) throws JMSException, MessageConversionException {
                if (o instanceof String) {
                    TextMessage message = session.createTextMessage("converter prefix " + o);
                    return message;
                }
                return null;
            }

            @Override
            public Object fromMessage(Message message) throws JMSException, MessageConversionException {
                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    return textMessage.getText() + " converter suffix";
                }
                return null;
            }
        };
    }
}
