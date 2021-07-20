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
package org.apache.camel.quarkus.component.netty;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.serialization.ClassResolver;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.apache.camel.CamelContext;
import org.apache.camel.component.netty.ChannelHandlerFactories;
import org.apache.camel.component.netty.ChannelHandlerFactory;
import org.apache.camel.component.netty.ClientInitializerFactory;
import org.apache.camel.component.netty.DefaultChannelHandlerFactory;
import org.apache.camel.component.netty.NettyCamelStateCorrelationManager;
import org.apache.camel.component.netty.NettyConsumer;
import org.apache.camel.component.netty.NettyProducer;
import org.apache.camel.component.netty.NettyServerBossPoolBuilder;
import org.apache.camel.component.netty.NettyWorkerPoolBuilder;
import org.apache.camel.component.netty.ServerInitializerFactory;
import org.apache.camel.component.netty.ShareableChannelHandlerFactory;
import org.apache.camel.component.netty.codec.DatagramPacketObjectDecoder;
import org.apache.camel.component.netty.codec.DatagramPacketObjectEncoder;
import org.apache.camel.component.netty.codec.DatagramPacketStringEncoder;
import org.apache.camel.component.netty.codec.ObjectDecoder;
import org.apache.camel.component.netty.codec.ObjectEncoder;
import org.apache.camel.component.netty.handlers.ClientChannelHandler;
import org.apache.camel.component.netty.handlers.ServerChannelHandler;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;

public class NettyProducers {

    @Singleton
    @Named
    public ChannelHandlerFactory tcpNullDelimitedHandler() {
        ByteBuf delimiter = Unpooled.wrappedBuffer(new byte[] { 0 });
        ByteBuf[] delimiters = new ByteBuf[] { delimiter, delimiter };
        return ChannelHandlerFactories.newDelimiterBasedFrameDecoder(4096, delimiters, "tcp");
    }

    @Singleton
    @Named
    public ChannelHandler bytesDecoder() {
        return new BytesDecoder();
    }

    @Singleton
    @Named
    public ChannelHandler bytesEncoder() {
        return new BytesEncoder();
    }

    @Singleton
    @Named
    public ChannelHandler tcpObjectDecoder(CamelContext context) {
        return new DefaultChannelHandlerFactory() {
            @Override
            public ChannelHandler newChannelHandler() {
                ClassLoader classLoader = context.getApplicationContextClassLoader();
                return new ObjectDecoder(ClassResolvers.weakCachingResolver(classLoader));
            }
        };
    }

    @Singleton
    @Named
    public ChannelHandler tcpObjectEncoder() {
        return new ShareableChannelHandlerFactory(new ObjectEncoder());
    }

    @Singleton
    @Named
    public DatagramPacketObjectDecoder udpObjectDecoder(CamelContext context) {
        ClassLoader classLoader = context.getApplicationContextClassLoader();
        return new SharableDatagramPacketObjectDecoder(ClassResolvers.weakCachingResolver(classLoader));
    }

    @Singleton
    @Named
    public DatagramPacketObjectEncoder udpObjectEncoder() {
        return new DatagramPacketObjectEncoder();
    }

    @Singleton
    @Named
    public SSLContextParameters sslContextParameters() {
        KeyStoreParameters keystoreParameters = new KeyStoreParameters();
        keystoreParameters.setResource("/ssl/keystore.p12");
        keystoreParameters.setPassword("changeit");

        KeyStoreParameters truststoreParameters = new KeyStoreParameters();
        truststoreParameters.setResource("/ssl/truststore.jks");
        truststoreParameters.setPassword("changeit");

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setKeyStore(truststoreParameters);
        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        keyManagersParameters.setKeyPassword("changeit");
        keyManagersParameters.setKeyStore(keystoreParameters);
        sslContextParameters.setKeyManagers(keyManagersParameters);

        return sslContextParameters;
    }

    @Singleton
    @Named
    public ServerInitializerFactory serverInitializerFactory() {
        return new MessageTransformingServerInitializerFactory(null);
    }

    @Singleton
    @Named
    public ClientInitializerFactory clientInitializerFactory() {
        return new MessageTransformingClientInitializerFactory(null);
    }

    @Singleton
    @Named
    public EventLoopGroup workerGroup() {
        NettyWorkerPoolBuilder builder = new NettyWorkerPoolBuilder();
        builder.setName("camel-quarkus-worker-pool");
        builder.setWorkerCount(5);
        return builder.build();
    }

    @Singleton
    @Named
    public EventLoopGroup clientWorkerGroup() {
        NettyWorkerPoolBuilder builder = new NettyWorkerPoolBuilder();
        builder.setName("camel-quarkus-client-worker-pool");
        builder.setWorkerCount(5);
        return builder.build();
    }

    @Singleton
    @Named
    public EventLoopGroup bossGroup() {
        NettyServerBossPoolBuilder builder = new NettyServerBossPoolBuilder();
        builder.setName("camel-quarkus-boss-pool");
        builder.setBossCount(5);
        return builder.build();
    }

    @Singleton
    @Named
    NettyCamelStateCorrelationManager correlationManager() {
        return new CustomCorrelationManager();
    }

    @ChannelHandler.Sharable
    static final class BytesDecoder extends MessageToMessageDecoder<ByteBuf> {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            if (msg.isReadable()) {
                byte[] bytes = new byte[msg.readableBytes()];
                int readerIndex = msg.readerIndex();
                msg.getBytes(readerIndex, bytes);
                out.add(bytes);
            }
        }
    }

    @ChannelHandler.Sharable
    static final class BytesEncoder extends MessageToMessageEncoder<byte[]> {

        @Override
        protected void encode(ChannelHandlerContext ctx, byte[] msg, List<Object> out) throws Exception {
            byte[] bytes = msg;
            ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(bytes.length);
            buf.writeBytes(bytes);
            out.add(buf);
        }
    }

    @ChannelHandler.Sharable
    static final class SharableDatagramPacketObjectDecoder extends DatagramPacketObjectDecoder {
        public SharableDatagramPacketObjectDecoder(ClassResolver resolver) {
            super(resolver);
        }
    }

    static final class MessageTransformingServerInitializerFactory extends ServerInitializerFactory {
        private final NettyConsumer consumer;

        public MessageTransformingServerInitializerFactory(NettyConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public ServerInitializerFactory createPipelineFactory(NettyConsumer consumer) {
            return new MessageTransformingServerInitializerFactory(consumer);
        }

        @Override
        protected void initChannel(Channel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();

            if (consumer.getConfiguration().getProtocol().equalsIgnoreCase("tcp")) {
                pipeline.addLast("custom-handler", tcpCustomChannelHandler());
            } else {
                pipeline.addLast("custom-handler", udpCustomChannelHandler());
            }
            pipeline.addLast("handler", new ServerChannelHandler(consumer));
        }

        private ChannelHandler tcpCustomChannelHandler() {
            return new MessageToMessageDecoder<ByteBuf>() {
                @Override
                protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf message, List<Object> out)
                        throws Exception {
                    String incomingPayload = message.toString(StandardCharsets.UTF_8);
                    String outgoingPayload = "Hello Camel Quarkus " + incomingPayload + " TCP Custom Server Initializer";
                    ByteBuf buffer = Unpooled.wrappedBuffer(outgoingPayload.getBytes(StandardCharsets.UTF_8));
                    out.add(buffer.retain());
                }
            };
        }

        private ChannelHandler udpCustomChannelHandler() {
            return new MessageToMessageDecoder<AddressedEnvelope<Object, InetSocketAddress>>() {
                @Override
                protected void decode(
                        ChannelHandlerContext channelHandlerContext,
                        AddressedEnvelope<Object, InetSocketAddress> message,
                        List<Object> out) throws Exception {
                    String incomingPayload = ((ByteBuf) message.content()).toString(StandardCharsets.UTF_8);
                    String outgoingPayload = "Hello Camel Quarkus " + incomingPayload + " UDP Custom Server Initializer";
                    ByteBuf buffer = Unpooled.wrappedBuffer(outgoingPayload.getBytes(StandardCharsets.UTF_8));
                    AddressedEnvelope<Object, InetSocketAddress> addressedEnvelop = new DefaultAddressedEnvelope<>(
                            buffer.retain(), message.recipient(), message.sender());
                    out.add(addressedEnvelop);
                }
            };
        }
    }

    static final class MessageTransformingClientInitializerFactory extends ClientInitializerFactory {
        private final NettyProducer producer;

        public MessageTransformingClientInitializerFactory(NettyProducer producer) {
            this.producer = producer;
        }

        @Override
        public ClientInitializerFactory createPipelineFactory(NettyProducer producer) {
            return new MessageTransformingClientInitializerFactory(producer);
        }

        @Override
        protected void initChannel(Channel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();
            if (producer.getConfiguration().getProtocol().equalsIgnoreCase("tcp")) {
                pipeline.addLast("decoder-delimiter", new DelimiterBasedFrameDecoder(1024, true, Delimiters.lineDelimiter()));
                pipeline.addLast("decoder-string", new StringDecoder(CharsetUtil.UTF_8));
                pipeline.addLast("custom-decoder", tcpCustomChannelHandler());
                pipeline.addLast("encoder-SD", new StringEncoder(CharsetUtil.UTF_8));
            } else {
                pipeline.addLast("encoder-datagram-packet", ChannelHandlerFactories.newDatagramPacketEncoder());
                pipeline.addLast("custom-handler", udpCustomChannelHandler());
                pipeline.addLast("encoder-datagram-packet-string", new DatagramPacketStringEncoder());
            }
            pipeline.addLast("handler", new ClientChannelHandler(producer));
        }

        private ChannelHandler tcpCustomChannelHandler() {
            return new MessageToMessageDecoder<String>() {
                @Override
                protected void decode(ChannelHandlerContext channelHandlerContext, String payload, List<Object> list)
                        throws Exception {
                    String outgoingPayload = payload + " Custom Client Initializer";
                    list.add(outgoingPayload);
                }
            };
        }

        private ChannelHandler udpCustomChannelHandler() {
            return new MessageToMessageEncoder<AddressedEnvelope<Object, InetSocketAddress>>() {
                @Override
                protected void encode(ChannelHandlerContext channelHandlerContext,
                        AddressedEnvelope<Object, InetSocketAddress> message, List<Object> out)
                        throws Exception {
                    String incomingPayload = ((ByteBuf) message.content()).toString(StandardCharsets.UTF_8);
                    String outgoingPayload = incomingPayload + " Custom Client Initializer";
                    ByteBuf buffer = Unpooled.wrappedBuffer(outgoingPayload.getBytes(StandardCharsets.UTF_8));
                    AddressedEnvelope<Object, InetSocketAddress> addressedEnvelop = new DefaultAddressedEnvelope<>(
                            buffer.retain(), message.recipient(), message.sender());
                    out.add(addressedEnvelop);
                }
            };
        }
    }
}
