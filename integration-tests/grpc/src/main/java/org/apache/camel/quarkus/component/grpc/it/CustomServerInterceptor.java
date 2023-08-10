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
package org.apache.camel.quarkus.component.grpc.it;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@Named
public class CustomServerInterceptor implements ServerInterceptor {
    public static final Context.Key<String> RESPONSE_KEY = Context.key("consumer-response");
    public static final Context.Key<String> PING_ID_CONTEXT_KEY = Context.key("pingId");
    public static final Metadata.Key<String> PING_ID_METADATA_KEY = Metadata.Key.of("pingId", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        Context context = Context.current()
                .withValue(PING_ID_CONTEXT_KEY, headers.get(PING_ID_METADATA_KEY))
                .withValue(RESPONSE_KEY, " PONG");
        return Contexts.interceptCall(context, call, headers, next);
    }
}
