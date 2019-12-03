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
package org.apache.camel.quarkus.component.dozer.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import com.github.dozermapper.core.builder.model.jaxb.AllowedExceptionsDefinition;
import com.github.dozermapper.core.builder.model.jaxb.ClassDefinition;
import com.github.dozermapper.core.builder.model.jaxb.ConfigurationDefinition;
import com.github.dozermapper.core.builder.model.jaxb.ConverterTypeDefinition;
import com.github.dozermapper.core.builder.model.jaxb.CopyByReferencesDefinition;
import com.github.dozermapper.core.builder.model.jaxb.CustomConvertersDefinition;
import com.github.dozermapper.core.builder.model.jaxb.FieldDefinition;
import com.github.dozermapper.core.builder.model.jaxb.FieldDefinitionDefinition;
import com.github.dozermapper.core.builder.model.jaxb.FieldExcludeDefinition;
import com.github.dozermapper.core.builder.model.jaxb.FieldType;
import com.github.dozermapper.core.builder.model.jaxb.MappingDefinition;
import com.github.dozermapper.core.builder.model.jaxb.MappingsDefinition;
import com.github.dozermapper.core.builder.model.jaxb.Relationship;
import com.github.dozermapper.core.builder.model.jaxb.Type;
import com.github.dozermapper.core.builder.model.jaxb.VariableDefinition;
import com.github.dozermapper.core.builder.model.jaxb.VariablesDefinition;
import com.sun.el.ExpressionFactoryImpl;

import org.apache.camel.component.dozer.CustomMapper;
import org.apache.camel.component.dozer.DozerConfiguration;
import org.apache.camel.component.dozer.ExpressionMapper;
import org.apache.camel.component.dozer.VariableMapper;
import org.apache.camel.converter.dozer.DozerBeanMapperConfiguration;
import org.apache.camel.converter.dozer.DozerThreadContextClassLoader;
import org.apache.camel.quarkus.component.dozer.CamelDozerConfig;
import org.apache.camel.quarkus.component.dozer.CamelDozerRecorder;
import org.apache.camel.quarkus.core.deployment.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelContextBuildItem;

class DozerProcessor {

    private static final String FEATURE = "camel-dozer";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void configureCamelDozer(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeImage,
            CamelDozerConfig camelDozerConfig) {

        // Add user Dozer mapping files to the image
        camelDozerConfig.mappingFiles
                .stream()
                .map(this::mappingPathToURI)
                // No scheme means classpath URI
                .filter(uri -> uri.getScheme() == null)
                .map(uri -> new NativeImageResourceBuildItem(uri.getPath()))
                .forEach(nativeImage::produce);

        // Add Dozer JAXB resources to the image
        nativeImage.produce(new NativeImageResourceBuildItem(
                "dtd/bean-mapping.dtd",
                "dtd/bean-mapping-6.0.0.dtd",
                "dtd/bean-mapping-6.2.0.dtd",
                "schema/bean-mapping.xsd",
                "schema/bean-mapping-6.0.0.xsd",
                "schema/bean-mapping-6.2.0.xsd",
                "com/github/dozermapper/core/builder/model/jaxb/jaxb.index"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                BigDecimal[].class,
                BigInteger[].class,
                Boolean[].class,
                Byte[].class,
                Calendar[].class,
                Character[].class,
                Class[].class,
                Date[].class,
                java.sql.Date[].class,
                Double[].class,
                File[].class,
                Float[].class,
                Integer[].class,
                Long[].class,
                Object[].class,
                Short[].class,
                String[].class,
                Time[].class,
                Timestamp[].class,
                URL[].class,
                DozerThreadContextClassLoader.class,
                ExpressionFactoryImpl.class));

        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false,
                        "com.github.dozermapper.core.builder.model.jaxb.package-info",
                        "com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                CustomMapper.class,
                DozerConfiguration.class,
                ExpressionMapper.class,
                VariableMapper.class));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
                AllowedExceptionsDefinition.class,
                ClassDefinition.class,
                ConfigurationDefinition.class,
                ConverterTypeDefinition.class,
                CopyByReferencesDefinition.class,
                CustomConvertersDefinition.class,
                FieldDefinition.class,
                FieldDefinitionDefinition.class,
                FieldExcludeDefinition.class,
                FieldType.class,
                MappingDefinition.class,
                MappingsDefinition.class,
                Relationship.class,
                Type.class,
                VariableDefinition.class,
                VariablesDefinition.class));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem configureCamelDozerBeanMappings(CamelDozerConfig camelDozerConfig,
            CamelDozerRecorder camelDozerRecorder) {

        CamelBeanBuildItem camelBeanBuildItem = null;

        if (!camelDozerConfig.mappingFiles.isEmpty()) {
            // Bind DozerBeanMapperConfiguration to the Camel registry for the user provided Dozer mapping files
            camelBeanBuildItem = new CamelBeanBuildItem(
                    "dozerBeanMappingConfiguration",
                    DozerBeanMapperConfiguration.class.getName(),
                    camelDozerRecorder.createDozerBeanMapperConfiguration(camelDozerConfig.mappingFiles));
        }

        return camelBeanBuildItem;
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configureDozerTypeConverterRegistry(CamelContextBuildItem camelContextBuildItem, CamelDozerConfig camelDozerConfig,
            CamelDozerRecorder camelDozerRecorder) {

        if (camelDozerConfig.typeConverterEnabled) {
            camelDozerRecorder.initializeDozerTypeConverter(camelContextBuildItem.getCamelContext());
        }
    }

    private URI mappingPathToURI(String mappingPath) {
        try {
            return new URI(mappingPath);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
