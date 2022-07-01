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
package org.apache.camel.quarkus.component.google.pubsub.graalvm;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.google.api.core.ApiFunction;
import com.google.api.gax.grpc.ChannelPrimer;
import com.google.api.gax.grpc.GrpcHeaderInterceptor;
import com.google.api.gax.grpc.GrpcInterceptorProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import org.threeten.bp.Duration;

/**
 * Cut out unsupported and optional features that are only present in grpc-alts.
 *
 * Camel Google PubSub only requires access to FixedTransportChannelProvider, but we leave
 * InstantiatingGrpcChannelProvider in a functional state in case some other library
 * needs to use it.
 */
final class GaxGrpcSubstitutions {
}

@TargetClass(InstantiatingGrpcChannelProvider.class)
final class InstantiatingGrpcChannelProviderSubstitutions {
    @Alias
    private Executor executor;
    @Alias
    private HeaderProvider headerProvider;
    @Alias
    private GrpcInterceptorProvider interceptorProvider;
    @Alias
    private String endpoint;
    @Alias
    private Integer maxInboundMessageSize;
    @Alias
    private Integer maxInboundMetadataSize;
    @Alias
    private Duration keepAliveTime;
    @Alias
    private Duration keepAliveTimeout;
    @Alias
    private Boolean keepAliveWithoutCalls;
    @Alias
    private ChannelPrimer channelPrimer;
    @Alias
    private ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder> channelConfigurator;

    @Substitute
    private ManagedChannel createSingleChannel() throws IOException {
        GrpcHeaderInterceptor headerInterceptor = new GrpcHeaderInterceptor(headerProvider.getHeaders());
        ClientInterceptor metadataHandlerInterceptor = new GrpcMetadataHandlerInterceptorTarget();

        int colon = endpoint.lastIndexOf(':');
        if (colon < 0) {
            throw new IllegalStateException("invalid endpoint - should have been validated: " + endpoint);
        }
        int port = Integer.parseInt(endpoint.substring(colon + 1));
        String serviceAddress = endpoint.substring(0, colon);

        ManagedChannelBuilder<?> builder;
        ChannelCredentials channelCredentials;
        try {
            channelCredentials = createMtlsChannelCredentials();
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }

        if (channelCredentials != null) {
            builder = Grpc.newChannelBuilder(endpoint, channelCredentials);
        } else {
            builder = ManagedChannelBuilder.forAddress(serviceAddress, port);
        }

        builder.disableServiceConfigLookUp();

        builder = builder.intercept(new GrpcChannelUUIDInterceptorTarget())
                .intercept(headerInterceptor)
                .intercept(metadataHandlerInterceptor)
                .userAgent(headerInterceptor.getUserAgentHeader())
                .executor(executor);

        if (maxInboundMetadataSize != null) {
            builder.maxInboundMetadataSize(maxInboundMetadataSize);
        }
        if (maxInboundMessageSize != null) {
            builder.maxInboundMessageSize(maxInboundMessageSize);
        }
        if (keepAliveTime != null) {
            builder.keepAliveTime(keepAliveTime.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (keepAliveTimeout != null) {
            builder.keepAliveTimeout(keepAliveTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (keepAliveWithoutCalls != null) {
            builder.keepAliveWithoutCalls(keepAliveWithoutCalls);
        }
        if (interceptorProvider != null) {
            builder.intercept(interceptorProvider.getInterceptors());
        }
        if (channelConfigurator != null) {
            builder = channelConfigurator.apply(builder);
        }

        ManagedChannel managedChannel = builder.build();
        if (channelPrimer != null) {
            channelPrimer.primeChannel(managedChannel);
        }
        return managedChannel;
    }

    @Alias
    ChannelCredentials createMtlsChannelCredentials() throws IOException, GeneralSecurityException {
        throw new UnsupportedOperationException();
    }
}

@TargetClass(className = "com.google.api.gax.grpc.GrpcMetadataHandlerInterceptor")
final class GrpcMetadataHandlerInterceptorTarget implements ClientInterceptor {

    @Alias()
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            final CallOptions callOptions, Channel next) {
        throw new UnsupportedOperationException();
    }
}

@TargetClass(className = "com.google.api.gax.grpc.GrpcChannelUUIDInterceptor")
final class GrpcChannelUUIDInterceptorTarget implements ClientInterceptor {
    @Alias
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor,
            CallOptions callOptions, Channel channel) {
        throw new UnsupportedOperationException();
    }
}
