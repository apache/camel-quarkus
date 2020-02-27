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
package org.apache.camel.quarkus.component.reactive.streams;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.reactive.streams.ReactiveStreamsComponent;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsServiceFactory;
import org.apache.camel.component.reactive.streams.engine.DefaultCamelReactiveStreamsServiceFactory;
import org.apache.camel.support.service.ServiceHelper;

@Recorder
public class ReactiveStreamsRecorder {
    public RuntimeValue<CamelReactiveStreamsServiceFactory> createDefaultReactiveStreamsServiceFactory() {
        return new RuntimeValue<>(new DefaultCamelReactiveStreamsServiceFactory());
    }

    public RuntimeValue<ReactiveStreamsComponent> createReactiveStreamsComponent(
            RuntimeValue<CamelReactiveStreamsServiceFactory> serviceFactory) {
        return new RuntimeValue<>(new QuarkusReactiveStreamsComponent(serviceFactory.getValue()));
    }

    @SuppressWarnings("unchecked")
    public void publishCamelReactiveStreamsService(
            BeanContainer beanContainer,
            RuntimeValue<CamelContext> camelContext,
            RuntimeValue<CamelReactiveStreamsServiceFactory> serviceFactory) {

        // register to the container
        beanContainer.instance(ReactiveStreamsProducers.class).init(
                camelContext.getValue(),
                serviceFactory.getValue());
    }

    private static class QuarkusReactiveStreamsComponent extends ReactiveStreamsComponent {
        private final CamelReactiveStreamsServiceFactory reactiveStreamServiceFactory;
        private final Object lock;
        private CamelReactiveStreamsService reactiveStreamService;

        public QuarkusReactiveStreamsComponent(CamelReactiveStreamsServiceFactory reactiveStreamServiceFactory) {
            this.reactiveStreamServiceFactory = reactiveStreamServiceFactory;
            this.lock = new Object();
        }

        @Override
        public CamelReactiveStreamsService getReactiveStreamsService() {
            synchronized (this.lock) {
                if (reactiveStreamService == null) {
                    this.reactiveStreamService = reactiveStreamServiceFactory.newInstance(
                            getCamelContext(),
                            getReactiveStreamsEngineConfiguration());

                    try {
                        // Start the service and add it to the Camel context to expose managed attributes
                        getCamelContext().addService(this.reactiveStreamService, true, true);
                    } catch (Exception e) {
                        throw new RuntimeCamelException(e);
                    }
                }
            }

            return this.reactiveStreamService;
        }

        @Override
        protected void doStop() throws Exception {
            ServiceHelper.stopService(this.reactiveStreamService);
            this.reactiveStreamService = null;

            super.doStop();
        }
    }
}
