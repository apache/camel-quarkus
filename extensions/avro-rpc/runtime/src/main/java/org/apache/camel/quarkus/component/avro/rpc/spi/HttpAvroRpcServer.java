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
package org.apache.camel.quarkus.component.avro.rpc.spi;

import java.io.IOException;

import javax.servlet.ServletException;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.Server;

public class HttpAvroRpcServer implements Server {

    private Undertow server;

    private int port;

    public HttpAvroRpcServer(Responder servletAvro, int port) throws IOException, ServletException {

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .setContextPath("/*")
                .setDeploymentName("avro-rpc-http")
                .addServletContextAttribute("avro-servlet-param", servletAvro)
                .addServlets(
                        Servlets.servlet("Avro-rpc servlet", AvroRpcServlet.class)
                                .addMapping("/*"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        HttpHandler servletHandler = manager.start();

        this.port = port;
        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(new PathHandler(servletHandler))
                .build();
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            throw new AvroRuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new AvroRuntimeException(e);
        }
    }

    @Override
    public void join() throws InterruptedException {
        throw new AvroRuntimeException(new UnsupportedOperationException());
    }
}
