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
package org.apache.camel.quarkus.component.mapstruct;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.mapstruct.MapstructComponent;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.support.SimpleTypeConverter;
import org.apache.camel.support.SimpleTypeConverter.ConversionMethod;

@Recorder
public class MapStructRecorder {

    public RuntimeValue<?> createMapper(String mapperClassName) {
        try {
            Object mapper = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass(mapperClassName)
                    .getDeclaredConstructor()
                    .newInstance();
            return new RuntimeValue<>(mapper);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException
                | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public RuntimeValue<ConversionMethodInfo> createConversionMethodInfo(
            Class<?> from,
            Class<?> to,
            boolean cdiBean,
            RuntimeValue<?> mapper,
            String conversionMethodClassName) {
        return new RuntimeValue<>(
                new ConversionMethodInfo(from, to, cdiBean, mapper, conversionMethodClassName));
    }

    public RuntimeValue<MapstructComponent> createMapStructComponent(
            Set<String> mapperPackages,
            Set<RuntimeValue<ConversionMethodInfo>> conversionMethodInfos) {
        String packages = String.join(",", mapperPackages);
        CamelQuarkusMapStructMapperFinder finder = new CamelQuarkusMapStructMapperFinder(packages,
                conversionMethodInfos.size());
        MapstructComponent component = new MapstructComponent();
        component.setMapperPackageName(packages);
        component.setMapStructConverter(finder);
        return new RuntimeValue<>(component);
    }

    public void registerMapStructTypeConverters(
            RuntimeValue<TypeConverterRegistry> typeConverterRegistryRuntimeValue,
            Set<RuntimeValue<ConversionMethodInfo>> conversionMethods,
            BeanContainer container) {
        TypeConverterRegistry registry = typeConverterRegistryRuntimeValue.getValue();
        conversionMethods.forEach(c -> {
            try {
                ConversionMethodInfo info = c.getValue();

                // Create the ConversionMethod for the SimpleTypeConverter
                Object conversionMethod;
                Class<?> conversionMethodClass = Thread.currentThread().getContextClassLoader()
                        .loadClass(info.getConversionMethodClassName());

                if (info.isCdiBean()) {
                    conversionMethod = container.beanInstance(conversionMethodClass);
                } else if (info.getMapper() != null) {
                    // Pass the Mapper instance created at build time
                    Object mapper = info.getMapper().getValue();
                    conversionMethod = conversionMethodClass.getDeclaredConstructor(mapper.getClass()).newInstance(mapper);
                } else {
                    // Default no-args constructor uses a Mapper instance declared in the Mapper interface
                    conversionMethod = conversionMethodClass.getDeclaredConstructor().newInstance();
                }

                registry.addTypeConverter(info.getToClass(), info.getFromClass(),
                        new SimpleTypeConverter(false, (ConversionMethod) conversionMethod));
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException
                    | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
