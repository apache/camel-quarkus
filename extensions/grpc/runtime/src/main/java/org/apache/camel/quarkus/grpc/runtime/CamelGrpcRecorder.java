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

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.component.grpc.GrpcComponent;
import org.apache.camel.component.grpc.GrpcConfiguration;
import org.apache.camel.component.grpc.GrpcConsumer;
import org.apache.camel.component.grpc.GrpcEndpoint;
import org.apache.camel.component.grpc.server.BindableServiceFactory;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.support.service.ServiceHelper;

import static org.apache.camel.component.grpc.GrpcConstants.GRPC_BINDABLE_SERVICE_FACTORY_NAME;

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

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return new QuarkusGrpcConsumer(this, processor, configuration);
        }
    }

    static final class QuarkusGrpcConsumer extends GrpcConsumer {

        public QuarkusGrpcConsumer(GrpcEndpoint endpoint, Processor processor, GrpcConfiguration configuration) {
            super(endpoint, processor, configuration);
        }

        @Override
        protected void doStart() throws Exception {
            // Quarkus gRPC extension handles server startup so we only need to configure the BindableService for this consumer endpoint
            ServiceHelper.startService(getProcessor());
            BindableServiceFactory bindableServiceFactory = CamelContextHelper.lookup(endpoint.getCamelContext(),
                    GRPC_BINDABLE_SERVICE_FACTORY_NAME, BindableServiceFactory.class);
            bindableServiceFactory.createBindableService(this);
        }
    }
}
