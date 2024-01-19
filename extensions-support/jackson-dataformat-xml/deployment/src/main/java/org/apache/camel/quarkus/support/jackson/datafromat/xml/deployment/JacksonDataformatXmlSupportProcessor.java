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
package org.apache.camel.quarkus.support.jackson.datafromat.xml.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.ObjectCodec;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

public class JacksonDataformatXmlSupportProcessor {

    static final String SERVICES_PREFIX = "META-INF/services/";

    @BuildStep
    void serviceProviders(BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        Stream.of(
                JsonFactory.class,
                ObjectCodec.class)
                .map(Class::getName)
                .forEach(serviceName -> {
                    try {
                        final Set<String> names = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                                SERVICES_PREFIX + serviceName);
                        serviceProviders.produce(new ServiceProviderBuildItem(serviceName, new ArrayList<>(names)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}
