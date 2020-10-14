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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
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
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.support.jsse.SSLContextParameters;

import static io.netty.buffer.Unpooled.wrappedBuffer;

@RegisterForReflection
@ApplicationScoped
public class LumberjackService {

    private static List<Map<String, Map>> logs;

    /**
     * Send messages to Lumberjack server
     * 
     * @param  port
     * @param  sslContextParameters
     * @return
     * @throws InterruptedException
     */
    List<Integer> sendMessages(int port, SSLContextParameters sslContextParameters) throws InterruptedException {
        // init list of results
        initLogs();

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            // This list will hold the acknowledgment response sequence numbers
            List<Integer> responses = new ArrayList<>();

            // This initializer configures the SSL and an acknowledgment recorder
            ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    if (sslContextParameters != null) {
                        SSLEngine sslEngine = sslContextParameters.createSSLContext(null).createSSLEngine();
                        sslEngine.setUseClientMode(true);
                        pipeline.addLast(new SslHandler(sslEngine));
                    }

                    // Add the response recorder from channel
                    pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                            // read 2 first bytes
                            msg.readUnsignedByte();
                            msg.readUnsignedByte();
                            // getting the lumberjack window sizes
                            synchronized (responses) {
                                responses.add(msg.readInt());
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
     * init results from consumer
     */
    public void initLogs() {
        logs = new ArrayList<>();
    }

    /**
     * Gets responses from Lumberjack consumer
     * 
     * @param log
     */
    public static void addLog(Map<String, Map> log) {
        logs.add(log);
    }

    /**
     * gets all results from consumer
     * 
     * @return
     */
    public List<Map<String, Map>> getLogs() {
        return logs;
    }
}
