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
package org.apache.camel.quarkus.component.nsq.it;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.nsq.NsqConstants;
import org.jboss.logging.Logger;

import static org.apache.camel.quarkus.component.nsq.it.NsqLogger.log;

@ApplicationScoped
public class NsqRoute extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(NsqRoute.class);

    public static final String CONSUMER_HOST_CFG_KEY = "quarkus.camel.nsq.test.consumer-host";
    public static final String CONSUMER_PORT_CFG_KEY = "quarkus.camel.nsq.test.consumer-port";

    public static final String PRODUCER_HOST_CFG_KEY = "quarkus.camel.nsq.test.producer-host";
    public static final String PRODUCER_PORT_CFG_KEY = "quarkus.camel.nsq.test.producer-port";

    public static final String CONSUMER_TOPIC = "consumer-topic";
    public static final String PRODUCER_TOPIC = "producer-topic";

    public static final Charset MESSAGE_CHARSET = StandardCharsets.UTF_8;

    @Inject
    NsqResource resource;

    @Override
    public void configure() {

        final String toUriFormat = "nsq://%s?servers={{%s}}:{{%s}}";
        from("direct:send").toF(toUriFormat, PRODUCER_TOPIC, PRODUCER_HOST_CFG_KEY, PRODUCER_PORT_CFG_KEY);

        final String fromUriFormat = "nsq://%s?servers={{%s}}:{{%s}}&lookupInterval=2000&autoFinish=false&requeueInterval=1000";
        fromF(fromUriFormat, CONSUMER_TOPIC, CONSUMER_HOST_CFG_KEY, CONSUMER_PORT_CFG_KEY).process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                byte[] messageBytes = exchange.getIn().getBody(byte[].class);
                String messageText = new String(messageBytes, MESSAGE_CHARSET);
                int attempts = exchange.getIn().getHeader(NsqConstants.NSQ_MESSAGE_ATTEMPTS, Integer.class);

                log(LOG, "Nsq consumer attempt %s to process \"%s\"", attempts, messageText);

                if (messageText.contains("Requeue") && attempts < 3) {
                    throw new Exception("Forced error");
                }
                if (attempts >= 3) {
                    resource.logNsqMessage("testRequeue", messageText);
                } else {
                    resource.logNsqMessage("testConsumer", messageText);
                }
            }
        });
    }

}
