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
package org.apache.camel.quarkus.core.deployment;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.deployment.spi.CamelSerializationBuildItem;
import org.apache.camel.support.DefaultExchangeHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelSerializationProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelSerializationProcessor.class);
    private static final String[] BASE_SERIALIZATION_CLASSES = {
            // JDK classes
            ArrayList.class.getName(),
            BigInteger.class.getName(),
            Boolean.class.getName(),
            Byte.class.getName(),
            Character.class.getName(),
            Collections.EMPTY_LIST.getClass().getName(),
            Date.class.getName(),
            Double.class.getName(),
            Exception.class.getName(),
            Float.class.getName(),
            HashMap.class.getName(),
            Integer.class.getName(),
            LinkedHashMap.class.getName(),
            Long.class.getName(),
            Number.class.getName(),
            RuntimeException.class.getName(),
            StackTraceElement.class.getName(),
            StackTraceElement[].class.getName(),
            String.class.getName(),
            Throwable.class.getName(),

            // Camel classes
            CamelExecutionException.class.getName(),
            DefaultExchangeHolder.class.getName(),
            RuntimeCamelException.class.getName(),
    };

    @BuildStep
    void produceSerializationBuildItem(CamelConfig config, BuildProducer<CamelSerializationBuildItem> serializationBuildItems) {
        final CamelConfig.ReflectionConfig reflectionConfig = config.native_.reflection;
        if (reflectionConfig.serializationEnabled) {
            LOGGER.debug(
                    "Registration of basic types for serialization is enabled via quarkus.camel.native.reflection.serialization-enabled");
            serializationBuildItems.produce(new CamelSerializationBuildItem());
        }
    }

    @BuildStep
    void baseSerializationClasses(List<CamelSerializationBuildItem> serializationRequests,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        if (!serializationRequests.isEmpty()) {
            //required for serialization of BigInteger
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, byte[].class));
            reflectiveClasses.produce(ReflectiveClassBuildItem.serializationClass(BASE_SERIALIZATION_CLASSES));
        }
    }
}
