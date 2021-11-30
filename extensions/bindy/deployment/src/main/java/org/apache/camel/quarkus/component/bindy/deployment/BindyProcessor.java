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
package org.apache.camel.quarkus.component.bindy.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.dataformat.bindy.annotation.BindyConverter;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;
import org.apache.camel.dataformat.bindy.annotation.FixedLengthRecord;
import org.apache.camel.dataformat.bindy.annotation.FormatFactories;
import org.apache.camel.dataformat.bindy.annotation.Link;
import org.apache.camel.dataformat.bindy.annotation.Message;
import org.apache.camel.dataformat.bindy.annotation.OneToMany;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.logging.Logger;

import static com.ibm.icu.util.VersionInfo.ICU_VERSION;

class BindyProcessor {

    private static final Logger LOG = Logger.getLogger(BindyProcessor.class);

    private static final String FEATURE = "camel-bindy";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceDirectoryBuildItem resourceBundles() {
        String resourcePath = String.format("com/ibm/icu/impl/data/icudt%db/brkitr", ICU_VERSION.getMajor());
        return new NativeImageResourceDirectoryBuildItem(resourcePath);
    }

    @BuildStep
    void registerReflectiveClasses(CombinedIndexBuildItem index, BuildProducer<ReflectiveClassBuildItem> producer) {

        // BreakIterators are needed when counting graphemes
        producer.produce(new ReflectiveClassBuildItem(false, false, "com.ibm.icu.text.BreakIteratorFactory"));

        IndexView idx = index.getIndex();

        // Registering the class for classes annotated with @CsvRecord, @FixedLengthRecord or @Message
        Stream.of(CsvRecord.class, FixedLengthRecord.class, Message.class)
                .map(clazz -> DotName.createSimple(clazz.getName()))
                .flatMap(dotName -> idx.getAnnotations(dotName).stream())
                .filter(anno -> anno.target() != null && anno.target().kind() == Kind.CLASS)
                .map(anno -> anno.target().asClass().name().toString())
                .forEach(className -> {
                    LOG.debugf("Registering root model class as reflective: %s", className);
                    producer.produce(new ReflectiveClassBuildItem(false, true, className));
                });

        // Registering the class for fields annotated with @Link
        Stream.of(Link.class)
                .map(clazz -> DotName.createSimple(clazz.getName()))
                .flatMap(dotName -> idx.getAnnotations(dotName).stream())
                .filter(anno -> anno.target() != null && anno.target().kind() == Kind.FIELD)
                .forEach(anno -> {
                    String className = anno.target().asField().type().name().toString();
                    LOG.debugf("Registering @Link model class as reflective: %s", className);
                    producer.produce(new ReflectiveClassBuildItem(false, true, className));
                });

        // Registering the class of the first parameterized type argument for fields annotated with @OnetoMany
        Stream.of(OneToMany.class)
                .map(clazz -> DotName.createSimple(clazz.getName()))
                .flatMap(dotName -> idx.getAnnotations(dotName).stream())
                .filter(anno -> anno.target() != null && anno.target().kind() == Kind.FIELD)
                .filter(anno -> anno.target().asField().type().kind() == org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE)
                .forEach(anno -> {
                    ParameterizedType fieldType = anno.target().asField().type().asParameterizedType();
                    if (fieldType.arguments().size() >= 1) {
                        String className = fieldType.arguments().get(0).name().toString();
                        LOG.debugf("Registering @OneToMany model class as reflective: %s", className);
                        producer.produce(new ReflectiveClassBuildItem(false, true, className));
                    }
                });

        // Registering the @BindyConverter.value() class for fields annotated with @BindyConverter
        Stream.of(BindyConverter.class)
                .map(clazz -> DotName.createSimple(clazz.getName()))
                .flatMap(dotName -> idx.getAnnotations(dotName).stream())
                .forEach(anno -> {
                    String className = anno.value().asClass().name().toString();
                    LOG.debugf("Registering @BindyConverter class as reflective: %s", className);
                    producer.produce(new ReflectiveClassBuildItem(true, false, className));
                });

        // Registering @FormatFactories.value() classes for fields annotated with @FormatFactories
        Stream.of(FormatFactories.class)
                .map(clazz -> DotName.createSimple(clazz.getName()))
                .flatMap(dotName -> idx.getAnnotations(dotName).stream())
                .forEach(anno -> {
                    for (org.jboss.jandex.Type t : anno.value().asClassArray()) {
                        LOG.debugf("Registering @FormatFactories class as reflective: %s", t.name().toString());
                        producer.produce(new ReflectiveClassBuildItem(false, false, t.name().toString()));
                    }
                });

        // Registering @DataField.method() classes for fields annotated with @DataField and using the method parameter
        Stream.of(DataField.class)
                .map(clazz -> DotName.createSimple(clazz.getName()))
                .flatMap(dotName -> idx.getAnnotations(dotName).stream())
                .filter(anno -> anno.value("method") != null && !anno.value("method").asString().isEmpty())
                .filter(anno -> anno.target() != null && anno.target().kind() == Kind.FIELD)
                .forEach(anno -> {
                    String method = anno.value("method").asString();
                    String methodClazz;
                    if (method.contains(".")) {
                        methodClazz = method.substring(0, method.lastIndexOf('.'));
                    } else {
                        methodClazz = anno.target().asField().type().toString();
                    }
                    LOG.debugf("Registering @DataField.method() class as reflective: %s", methodClazz);
                    producer.produce(new ReflectiveClassBuildItem(true, false, methodClazz));
                });
    }
}
