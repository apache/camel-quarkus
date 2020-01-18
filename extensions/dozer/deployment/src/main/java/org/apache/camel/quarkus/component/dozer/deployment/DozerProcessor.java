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

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;
import com.sun.el.ExpressionFactoryImpl;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.jaxb.deployment.JaxbFileRootBuildItem;
import org.apache.camel.component.dozer.DozerConfiguration;
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
    JaxbFileRootBuildItem dozerJaxbFileRoot() {
        return new JaxbFileRootBuildItem("com/github/dozermapper/core/builder/model/jaxb");
    }

    @BuildStep(applicationArchiveMarkers = { "com/github/dozermapper/core" })
    void configureCamelDozer(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeImage,
            CamelDozerConfig camelDozerConfig) {

        // Add user Dozer mapping files to the image
        camelDozerConfig.mappingFiles.orElse(Collections.emptyList())
                .stream()
                .map(this::mappingPathToURI)
                // No scheme means classpath URI
                .filter(uri -> uri.getScheme() == null)
                .map(uri -> new NativeImageResourceBuildItem(uri.getPath()))
                .forEach(nativeImage::produce);

        // Add Dozer DTD & XSD resources to the image
        nativeImage.produce(new NativeImageResourceBuildItem(
                "dtd/bean-mapping.dtd",
                "dtd/bean-mapping-6.0.0.dtd",
                "dtd/bean-mapping-6.2.0.dtd",
                "schema/bean-mapping.xsd",
                "schema/bean-mapping-6.0.0.xsd",
                "schema/bean-mapping-6.2.0.xsd"));

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
                        "com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, DozerConfiguration.class));

        if (camelDozerConfig.mappingFiles.isPresent()) {
            // Register for reflection any classes participating in Dozer mapping
            Mapper mapper = DozerBeanMapperBuilder.create()
                    .withMappingFiles(camelDozerConfig.mappingFiles.get())
                    .build();

            mapper.getMappingMetadata()
                    .getClassMappings()
                    .stream()
                    .map(metadata -> new ReflectiveClassBuildItem(true, false, metadata.getSourceClassName(),
                            metadata.getDestinationClassName()))
                    .forEach(reflectiveClass::produce);
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem configureCamelDozerBeanMappings(CamelDozerConfig camelDozerConfig,
            CamelDozerRecorder camelDozerRecorder) {

        CamelBeanBuildItem camelBeanBuildItem = null;

        if (camelDozerConfig.mappingFiles.isPresent()) {
            // Bind DozerBeanMapperConfiguration to the Camel registry for the user provided Dozer mapping files
            camelBeanBuildItem = new CamelBeanBuildItem(
                    "dozerBeanMappingConfiguration",
                    DozerBeanMapperConfiguration.class.getName(),
                    camelDozerRecorder.createDozerBeanMapperConfiguration(camelDozerConfig.mappingFiles.get()));
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
