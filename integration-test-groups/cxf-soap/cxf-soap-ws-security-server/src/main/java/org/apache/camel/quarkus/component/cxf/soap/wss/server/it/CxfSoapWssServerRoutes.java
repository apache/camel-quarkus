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
package org.apache.camel.quarkus.component.cxf.soap.wss.server.it;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.security.auth.callback.Callback;
import jakarta.security.auth.callback.CallbackHandler;
import jakarta.security.auth.callback.UnsupportedCallbackException;

import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CxfSoapWssServerRoutes extends RouteBuilder {

    /** Produced in CxfWssServerTestResource */
    @ConfigProperty(name = "camel-quarkus.wss.server.username", defaultValue = "cxf")
    String username;

    /** Produced in CxfWssServerTestResource */
    @ConfigProperty(name = "camel-quarkus.wss.server.password", defaultValue = "pwd")
    String password;

    @Override
    public void configure() {

        from("cxf:bean:wssRounderService?dataFormat=POJO")
                .log("exchange: ${exchange}")
                .process(exchange -> {
                    final Message message = exchange.getMessage();
                    final double body = message.getBody(double.class);
                    message.setBody(Math.round(body));
                });

    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint wssRounderService() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(WssRounderService.class);
        result.setAddress("/rounder");

        final LoggingFeature lf = new LoggingFeature();
        lf.setPrettyLogging(true);
        result.getFeatures().add(lf);

        final CallbackHandler passwordCallback = new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof WSPasswordCallback) {
                        final WSPasswordCallback pc = (WSPasswordCallback) callback;
                        if (username.equals(pc.getIdentifier())) {
                            pc.setPassword(password);
                            return;
                        }
                        break;
                    }
                }
            }
        };

        final Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "UsernameToken");
        props.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        props.put(ConfigurationConstants.USER, username);
        props.put(ConfigurationConstants.PW_CALLBACK_REF, passwordCallback);
        result.getInInterceptors().add(new WSS4JInInterceptor(props));

        return result;
    }

}
