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

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.grpc.BindableService;
import org.apache.camel.CamelContext;
import org.apache.camel.component.grpc.GrpcConsumer;
import org.apache.camel.component.grpc.GrpcEndpoint;
import org.apache.camel.component.grpc.GrpcUtils;
import org.apache.camel.component.grpc.server.BindableServiceFactory;
import org.apache.camel.component.grpc.server.GrpcMethodHandler;

import static org.apache.camel.component.grpc.GrpcConstants.GRPC_BINDABLE_SERVICE_FACTORY_NAME;

/**
 * A custom BindableServiceFactory which finds, configures and returns the appropriate BindableService
 * that was dynamically generated at build time
 */
@Singleton
@Named(GRPC_BINDABLE_SERVICE_FACTORY_NAME)
public class QuarkusBindableServiceFactory implements BindableServiceFactory {

    @Inject
    Instance<CamelQuarkusBindableService> bindableServices;

    @Override
    public BindableService createBindableService(GrpcConsumer consumer) {
        GrpcEndpoint endpoint = (GrpcEndpoint) consumer.getEndpoint();
        CamelContext camelContext = endpoint.getCamelContext();

        Class<?> baseClass = GrpcUtils.constructGrpcImplBaseClass(endpoint.getServicePackage(), endpoint.getServiceName(),
                camelContext);

        // Find the BindableService implementation that was generated in GrpcProcessor and configure the GrpcMethodHandler
        CamelQuarkusBindableService bindableService = bindableServices.stream()
                .filter(service -> baseClass.isAssignableFrom(service.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to find generated class for service " + endpoint.getServiceName()));
        GrpcMethodHandler methodHandler = new GrpcMethodHandler(consumer);
        bindableService.setMethodHandler(methodHandler);
        return bindableService;
    }
}
