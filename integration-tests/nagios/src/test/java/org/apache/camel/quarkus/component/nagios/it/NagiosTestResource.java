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
package org.apache.camel.quarkus.component.nagios.it;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty.NettyConsumer;
import org.apache.camel.component.netty.ServerInitializerFactory;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.commons.codec.digest.DigestUtils;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;

import static org.apache.camel.quarkus.component.nagios.it.NagiosResource.NSCA_HOST_CFG_KEY;
import static org.apache.camel.quarkus.component.nagios.it.NagiosResource.NSCA_PORT_CFG_KEY;

public class NagiosTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(NagiosTestResource.class);
    private static final int INIT_VECTOR_PLUS_TIMESTAMP_SIZE_IN_BYTES = 128 + Integer.BYTES;

    private int nscaPort;
    private CamelContext context;

    private MockNscaServerInitializerFactory mockNscaServer = new MockNscaServerInitializerFactory();

    @Override
    public Map<String, String> start() {
        nscaPort = AvailablePortFinder.getNextAvailable();

        try {
            context = new DefaultCamelContext();
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    context.getRegistry().bind("mockNscaServer", mockNscaServer);
                    fromF("netty:tcp://0.0.0.0:%s?serverInitializerFactory=#mockNscaServer", nscaPort)
                            .log("This log statement is here just because a route definition needs an output");
                }
            });
            context.start();
        } catch (Exception e) {
            LOG.error("An issue occured while starting the NagiosTestResource route", e);
        }
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NSCA_PORT_CFG_KEY, "" + nscaPort);
        properties.put(NSCA_HOST_CFG_KEY, "localhost");
        return properties;
    }

    @Override
    public void stop() {
        if (context != null) {
            context.shutdown();
        }
        AvailablePortFinder.releaseReservedPorts();
    }

    @Override
    public void inject(Object testInstance) {
        Class<?> c = testInstance.getClass();
        if (c == NagiosTest.class || c == NagiosIT.class) {
            ((NagiosTest) testInstance).setMockNscaServer(mockNscaServer);
        }
    }

    public static class MockNscaServerInitializerFactory extends ServerInitializerFactory {

        private volatile String actualFrameDigest;

        protected void initChannel(Channel ch) {

            ch.pipeline().addFirst("mock-nsca-handler", new ByteToMessageDecoder() {
                @Override
                public void channelActive(ChannelHandlerContext ctx) {
                    // Send the init sequence required by the nsca protocol (this one is empty for test reproducibility)
                    final ByteBuf initVectorAndTimeStamp = ctx.alloc().buffer(INIT_VECTOR_PLUS_TIMESTAMP_SIZE_IN_BYTES);
                    initVectorAndTimeStamp.writeBytes(new byte[INIT_VECTOR_PLUS_TIMESTAMP_SIZE_IN_BYTES]);
                    ctx.writeAndFlush(initVectorAndTimeStamp);
                }

                @Override
                protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
                    byte[] bytes = new byte[in.readableBytes()];
                    in.readBytes(bytes);
                    actualFrameDigest = DigestUtils.md5Hex(bytes);
                }
            });
        }

        void verifyFrameReceived(String expectedFrameDigest) {
            if (expectedFrameDigest == null) {
                throw new IllegalArgumentException("argument expectedFrameDigest can't be null");
            }
            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
                LOG.infof("ExpectedFrameDigest=%s and actualFrameDigest=%s", expectedFrameDigest, actualFrameDigest);
                return expectedFrameDigest.equals(actualFrameDigest);
            });
        }

        @Override
        public ServerInitializerFactory createPipelineFactory(NettyConsumer consumer) {
            return this;
        }
    }
}
