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
package org.apache.camel.quarkus.component.debezium.common.it;

import javax.inject.Inject;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Parent for debezium based resources.
 * Provides methods receive and receiveEmptyMessages.
 * To change parameters in endpoint url, please override getEndpoinUrl method and change parameters there.
 */
public abstract class AbstractDebeziumResource {
    @ConfigProperty(name = "test.debezium.timeout", defaultValue = "10000")
    private long TIMEOUT;

    private final Type type;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    Config config;

    public AbstractDebeziumResource(Type type) {
        this.type = type;
    }

    String getEndpoinUrl(String hostname, String port, String username, String password, String databaseServerName,
            String offsetStorageFileName) {
        return type.getComponent() + ":localhost?"
                + "databaseHostname=" + hostname
                + "&databasePort=" + port
                + "&databaseUser=" + username
                + "&databasePassword=" + password
                + "&databaseServerName=" + databaseServerName
                + "&offsetStorageFileName=" + offsetStorageFileName;
    }

    public String receive() {
        return receiveAsRecord().getValue();
    }

    public Record receiveAsRecord() {
        Exchange exchange = receiveAsExchange();
        if (exchange == null) {
            return null;
        }
        return new Record(exchange.getIn().getHeader(DebeziumConstants.HEADER_OPERATION, String.class),
                exchange.getIn().getBody(String.class));
    }

    public String receiveEmptyMessages() {

        int i = 0;
        Exchange exchange;
        while (i++ < 10) {
            exchange = receiveAsExchange();
            //if exchange is null (timeout), all empty messages are received
            if (exchange == null) {
                return null;
            }
            //if exchange contains data, return value
            String value = exchange.getIn().getBody(String.class);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private Exchange receiveAsExchange() {
        String endpoint = getEndpoinUrl(
                config.getValue(type.getPropertyHostname(), String.class),
                config.getValue(type.getPropertyPort(), String.class),
                config.getValue(type.getPropertyUsername(), String.class),
                config.getValue(type.getPropertyPassword(), String.class),
                "qa",
                config.getValue(type.getPropertyOffsetFileName(), String.class));
        return consumerTemplate.receive(endpoint, TIMEOUT);
    }
}
