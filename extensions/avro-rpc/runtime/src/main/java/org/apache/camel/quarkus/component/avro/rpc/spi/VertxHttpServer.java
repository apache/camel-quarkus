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

import io.quarkus.arc.Arc;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.ResponderServlet;
import org.apache.avro.ipc.Server;

public class VertxHttpServer implements Server {

    private int port;

    private ResponderServlet responder;

    public VertxHttpServer(Responder servletAvro, int port) throws Exception {
        this.port = port;
        this.responder = new ResponderServlet(servletAvro);
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void start() {
        Vertx vertx = Arc.container().instance(Vertx.class).get();

        vertx.createHttpServer().requestHandler(rc -> {
            rc.bodyHandler(handler -> {
                HttpServletRequestFromBytes req = new HttpServletRequestFromBytes(handler.getBytes(), rc.method().name());
                HttpServletResponseWithBytes resp = new HttpServletResponseWithBytes();
                vertx.executeBlocking(
                        promise -> {
                            try {
                                responder.service(req, resp);
                                promise.complete(Buffer.buffer(resp.getBytes()));
                            } catch (Exception e) {
                                promise.fail(e);
                            }
                        },
                        false,
                        result -> {
                            if (result.succeeded()) {
                                rc.response().end((Buffer) result.result());
                            } else {
                                rc.response().setStatusCode(500).end();
                            }
                        });
            });
        }).listen(port);
    }

    @Override
    public void close() {
        //do nothing
    }

    @Override
    public void join() throws InterruptedException {
        throw new AvroRuntimeException(new UnsupportedOperationException());
    }

}
