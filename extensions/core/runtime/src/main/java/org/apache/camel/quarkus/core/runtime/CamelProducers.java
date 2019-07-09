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
package org.apache.camel.quarkus.core.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.Registry;

@ApplicationScoped
public class CamelProducers {

    CamelRuntime camelRuntime;

    @Produces
    public CamelContext getCamelContext() {
        return camelRuntime.getContext();
    }

    @Produces
    public Registry getCamelRegistry() {
        return camelRuntime.getRegistry();
    }

    @Produces
    public ProducerTemplate getCamelProducerTemplate() {
        return camelRuntime.getContext().createProducerTemplate();
    }

    @Produces
    public ConsumerTemplate getCamelConsumerTemplate() {
        return camelRuntime.getContext().createConsumerTemplate();
    }

    @Produces
    public CamelConfig.BuildTime getCamelBuildTimeConfig() {
        return camelRuntime.getBuildTimeConfig();
    }

    @Produces
    public CamelConfig.Runtime getCamelRuntimeConfig() {
        return camelRuntime.getRuntimeConfig();
    }

    public void setCamelRuntime(CamelRuntime camelRuntime) {
        this.camelRuntime = camelRuntime;
    }

}
