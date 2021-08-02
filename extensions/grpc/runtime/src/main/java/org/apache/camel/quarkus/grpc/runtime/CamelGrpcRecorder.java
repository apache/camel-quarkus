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
package org.apache.camel.quarkus.grpc.runtime;

import java.util.Map;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.component.grpc.GrpcAuthType;
import org.apache.camel.component.grpc.GrpcComponent;
import org.apache.camel.component.grpc.GrpcConfiguration;
import org.apache.camel.component.grpc.GrpcEndpoint;
import org.apache.camel.component.grpc.GrpcProducer;
import org.apache.camel.component.grpc.GrpcUtils;
import org.apache.camel.component.grpc.auth.jwt.JwtCallCredentials;
import org.apache.camel.component.grpc.auth.jwt.JwtHelper;
import org.apache.camel.component.grpc.client.GrpcExchangeForwarder;
import org.apache.camel.component.grpc.client.GrpcExchangeForwarderFactory;
import org.apache.camel.component.grpc.client.GrpcResponseAggregationStreamObserver;
import org.apache.camel.component.grpc.client.GrpcResponseRouterStreamObserver;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.support.SynchronousDelegateProducer;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Recorder
public class CamelGrpcRecorder {

    public RuntimeValue<GrpcComponent> createGrpcComponent() {
        return new RuntimeValue<>(new QuarkusGrpcComponent());
    }

    @Component("grpc")
    static final class QuarkusGrpcComponent extends GrpcComponent {

        @Override
        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
            GrpcConfiguration config = new GrpcConfiguration();
            config = parseConfiguration(config, uri);

            Endpoint endpoint = new QuarkusGrpcEndpoint(uri, this, config);
            setProperties(endpoint, parameters);
            return endpoint;
        }
    }

    static final class QuarkusGrpcEndpoint extends GrpcEndpoint {

        public QuarkusGrpcEndpoint(String uri, GrpcComponent component, GrpcConfiguration config) throws Exception {
            super(uri, component, config);
        }

        public Producer createProducer() throws Exception {
            GrpcProducer producer = new QuarkusGrpcProducer(this, this.configuration);
            return this.configuration.isSynchronous() ? new SynchronousDelegateProducer(producer) : producer;
        }
    }

    // Allow producer SSL configuration to do fallback when unsupported native providers are specified
    // Most of GrpcProducer is reproduced due to much of the configuration being done by directly accessing private fields
    // TODO: Remove when https://github.com/apache/camel-quarkus/issues/2966 is resolved
    static final class QuarkusGrpcProducer extends GrpcProducer {

        private static final Logger LOG = LoggerFactory.getLogger(QuarkusGrpcProducer.class);
        private ManagedChannel channel;
        private Object grpcStub;
        private GrpcExchangeForwarder forwarder;
        private GrpcResponseRouterStreamObserver globalResponseObserver;

        public QuarkusGrpcProducer(GrpcEndpoint endpoint, GrpcConfiguration configuration) {
            super(endpoint, configuration);
        }

        @Override
        protected void doStart() throws Exception {
            if (channel == null) {
                CallCredentials callCreds = null;
                initializeChannel();

                if (configuration.getAuthenticationType() == GrpcAuthType.GOOGLE) {
                    ObjectHelper.notNull(configuration.getKeyCertChainResource(), "serviceAccountResource");

                    Credentials creds = GoogleCredentials.fromStream(
                            ResourceHelper.resolveResourceAsInputStream(endpoint.getCamelContext(),
                                    configuration.getServiceAccountResource()));
                    callCreds = MoreCallCredentials.from(creds);
                } else if (configuration.getAuthenticationType() == GrpcAuthType.JWT) {
                    ObjectHelper.notNull(configuration.getJwtSecret(), "jwtSecret");

                    String jwtToken = JwtHelper.createJwtToken(configuration.getJwtAlgorithm(), configuration.getJwtSecret(),
                            configuration.getJwtIssuer(), configuration.getJwtSubject());
                    callCreds = new JwtCallCredentials(jwtToken);
                }

                if (configuration.isSynchronous()) {
                    LOG.debug("Getting synchronous method stub from channel");
                    grpcStub = GrpcUtils.constructGrpcBlockingStub(endpoint.getServicePackage(), endpoint.getServiceName(),
                            channel,
                            callCreds, endpoint.getCamelContext());
                } else {
                    LOG.debug("Getting asynchronous method stub from channel");
                    grpcStub = GrpcUtils.constructGrpcAsyncStub(endpoint.getServicePackage(), endpoint.getServiceName(),
                            channel,
                            callCreds, endpoint.getCamelContext());
                }
                forwarder = GrpcExchangeForwarderFactory.createExchangeForwarder(configuration, grpcStub);

                if (configuration.getStreamRepliesTo() != null) {
                    this.globalResponseObserver = new GrpcResponseRouterStreamObserver(configuration, getEndpoint());
                }

                if (this.globalResponseObserver != null) {
                    ServiceHelper.startService(this.globalResponseObserver);
                }
            }
        }

        @Override
        protected void doStop() throws Exception {
            if (this.globalResponseObserver != null) {
                ServiceHelper.stopService(this.globalResponseObserver);
            }
            if (channel != null) {
                forwarder.shutdown();
                forwarder = null;

                LOG.debug("Terminating channel to the remote gRPC server");
                channel.shutdown().shutdownNow();
                channel = null;
                grpcStub = null;
                globalResponseObserver = null;
            }
            super.doStop();
        }

        @Override
        protected void initializeChannel() throws Exception {
            NettyChannelBuilder channelBuilder;

            if (!ObjectHelper.isEmpty(configuration.getHost()) && !ObjectHelper.isEmpty(configuration.getPort())) {
                LOG.info("Creating channel to the remote gRPC server {}:{}", configuration.getHost(), configuration.getPort());
                channelBuilder = NettyChannelBuilder.forAddress(configuration.getHost(), configuration.getPort());
            } else {
                throw new IllegalArgumentException("No connection properties (host or port) specified");
            }
            if (configuration.getNegotiationType() == NegotiationType.TLS) {
                ObjectHelper.notNull(configuration.getKeyCertChainResource(), "keyCertChainResource");
                ObjectHelper.notNull(configuration.getKeyResource(), "keyResource");

                SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient()
                        .sslProvider(SslProvider.OPENSSL)
                        .keyManager(
                                ResourceHelper.resolveResourceAsInputStream(endpoint.getCamelContext(),
                                        configuration.getKeyCertChainResource()),
                                ResourceHelper.resolveResourceAsInputStream(endpoint.getCamelContext(),
                                        configuration.getKeyResource()),
                                configuration.getKeyPassword());

                if (ObjectHelper.isNotEmpty(configuration.getTrustCertCollectionResource())) {
                    sslContextBuilder = sslContextBuilder
                            .trustManager(ResourceHelper.resolveResourceAsInputStream(endpoint.getCamelContext(),
                                    configuration.getTrustCertCollectionResource()));
                }

                channelBuilder = channelBuilder.sslContext(GrpcSslContexts.configure(sslContextBuilder).build());
            }

            channel = channelBuilder.negotiationType(configuration.getNegotiationType())
                    .flowControlWindow(configuration.getFlowControlWindow())
                    .userAgent(configuration.getUserAgent())
                    .maxInboundMessageSize(configuration.getMaxMessageSize())
                    .intercept(configuration.getClientInterceptors())
                    .build();
        }

        @Override
        public boolean process(Exchange exchange, AsyncCallback callback) {
            StreamObserver<Object> streamObserver = this.globalResponseObserver;
            if (globalResponseObserver == null) {
                streamObserver = new GrpcResponseAggregationStreamObserver(exchange, callback);
            }

            return forwarder.forward(exchange, streamObserver, callback);
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            forwarder.forward(exchange);
        }
    }
}
