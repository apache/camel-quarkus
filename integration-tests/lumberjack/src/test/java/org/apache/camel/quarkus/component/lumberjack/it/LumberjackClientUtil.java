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
package org.apache.camel.quarkus.component.lumberjack.it;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.mockito.Mockito;

import static io.netty.buffer.Unpooled.wrappedBuffer;

public class LumberjackClientUtil {

    /**
     * Send payload to Lumberjack server
     * 
     * @param  port
     * @param  withSslContextParameters
     * @return
     * @throws InterruptedException
     */
    public static List<LumberjackAckResponse> sendMessages(int port, boolean withSslContextParameters)
            throws InterruptedException {
        final SSLContextParameters sslContextParameters = withSslContextParameters ? createClientSSLContextParameters() : null;
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            // This list will hold the acknowledgment response sequence numbers
            List<LumberjackAckResponse> responses = new ArrayList<>();

            // This initializer configures the SSL and an acknowledgment recorder
            ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    if (sslContextParameters != null) {
                        CamelContext mockContext = Mockito.mock(CamelContext.class);
                        Mockito.when(mockContext.resolvePropertyPlaceholders(Mockito.anyString()))
                                .thenAnswer(i -> i.getArguments()[0]);
                        SSLEngine sslEngine = sslContextParameters.createSSLContext(mockContext).createSSLEngine();
                        sslEngine.setUseClientMode(true);
                        pipeline.addLast(new SslHandler(sslEngine));
                    }

                    // Add the response recorder from channel
                    pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                            short version = msg.readUnsignedByte();
                            short frame = msg.readUnsignedByte();
                            int sequence = msg.readInt();
                            int remaining = msg.readableBytes();
                            // getting the lumberjack window sizes
                            synchronized (responses) {
                                responses.add(new LumberjackAckResponse(version, frame, sequence, remaining));
                            }
                        }
                    });
                }
            };

            // Connect to the server
            Channel channel = new Bootstrap() //
                    .group(eventLoopGroup) //
                    .channel(NioSocketChannel.class) //
                    .handler(initializer) //
                    .connect("127.0.0.1", port).sync().channel(); //

            // Send the 2 window frames
            TimeUnit.MILLISECONDS.sleep(500);
            channel.writeAndFlush(readSample("io/window10.bin"));
            TimeUnit.MILLISECONDS.sleep(500);
            channel.writeAndFlush(readSample("io/window15.bin"));
            TimeUnit.MILLISECONDS.sleep(500);
            channel.close();

            synchronized (responses) {
                return responses;
            }
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    /**
     * read window frame and writes in ByteBuf
     * 
     * @param  resource
     * @return
     */
    private static ByteBuf readSample(String resource) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int input;
            while ((input = stream.read()) != -1) {
                output.write(input);
            }
            return wrappedBuffer(output.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Cannot read sample data", e);
        }
    }

    /**
     * Creates SSL Context Parameters for the client
     *
     * @return
     */
    public static SSLContextParameters createClientSSLContextParameters() {
        SSLContextParameters sslContextParameters = new SSLContextParameters();

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        KeyStoreParameters trustStore = new CustomKeyStoreParameters();
        trustStore.setPassword("changeit");
        trustStore.setResource("ssl/keystore.jks");
        trustManagersParameters.setKeyStore(trustStore);
        sslContextParameters.setTrustManagers(trustManagersParameters);

        return sslContextParameters;
    }

    private static class CustomKeyStoreParameters extends KeyStoreParameters {

        @Override
        protected InputStream resolveResource(String resource) throws IOException {
            return this.getClass().getClassLoader().getResourceAsStream(resource);
        }
    }
}
