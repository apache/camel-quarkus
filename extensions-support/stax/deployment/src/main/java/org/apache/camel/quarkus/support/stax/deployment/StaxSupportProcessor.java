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
package org.apache.camel.quarkus.support.stax.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import org.codehaus.stax2.validation.XMLValidationSchemaFactory;

import static io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem.SPI_ROOT;

public class StaxSupportProcessor {

    @BuildStep
    void registerServices(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        Stream.concat(
                Stream.of(
                        XMLEventFactory.class,
                        XMLInputFactory.class,
                        XMLOutputFactory.class)
                        .map(Class::getName),
                Stream.of(
                        XMLValidationSchemaFactory.INTERNAL_ID_SCHEMA_DTD,
                        XMLValidationSchemaFactory.INTERNAL_ID_SCHEMA_RELAXNG,
                        XMLValidationSchemaFactory.INTERNAL_ID_SCHEMA_W3C)
                        .map(schemaId -> XMLValidationSchemaFactory.class.getName() + "." + schemaId))
                .forEach(serviceName -> {
                    try {
                        final Set<String> names = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                                SPI_ROOT + serviceName);
                        serviceProvider.produce(new ServiceProviderBuildItem(serviceName, new ArrayList<>(names)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
