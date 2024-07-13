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
package org.apache.camel.quarkus.component.servlet;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.core.ManagedServlet;
import io.undertow.servlet.core.ManagedServlets;
import io.undertow.servlet.spec.ServletContextImpl;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.http.common.HttpMessage;
import org.apache.camel.spi.MimeType;

import static org.apache.camel.quarkus.servlet.runtime.CamelServletConfig.ServletConfig.DEFAULT_SERVLET_NAME;

@ApplicationScoped
public class ServletProducers {
    @Singleton
    @Named("customServletExecutor")
    public Executor customServletExecutor() {
        return Executors.newSingleThreadExecutor(r -> new Thread(r, "custom-executor"));
    }

    @Singleton
    @Named("servletConfigInfoProcessor")
    public Processor servletConfigInfoProcessor() {
        return exchange -> {
            JsonObject json = new JsonObject();
            Message message = exchange.getMessage();
            HttpMessage httpMessage = exchange.getIn(HttpMessage.class);
            HttpServletRequest request = httpMessage.getRequest();
            String servletName = message.getHeader("servletName", DEFAULT_SERVLET_NAME, String.class);
            ServletContext servletContext = request.getServletContext();
            Deployment deployment = ((ServletContextImpl) servletContext).getDeployment();
            ManagedServlets servlets = deployment.getServlets();
            ManagedServlet servlet = servlets.getManagedServlet(servletName);
            ServletInfo servletInfo = servlet.getServletInfo();

            json.put("isAsync", request.isAsyncSupported());
            json.put("threadName", Thread.currentThread().getName());
            json.put("loadOnStartup", servletInfo.getLoadOnStartup());
            json.put("initParams", servletInfo.getInitParams());

            message.setHeader(Exchange.CONTENT_TYPE, MimeType.JSON.type());
            message.setBody(json.encode());
        };
    }
}
