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
package org.apache.camel.quarkus.component.avro.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem.Holder;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;
import org.apache.avro.generic.GenericContainer;
import org.apache.camel.quarkus.component.avro.AvroDataFormatProducer;
import org.apache.camel.quarkus.component.avro.AvroRecorder;
import org.apache.camel.quarkus.component.avro.AvroSchemaSubstitution;
import org.apache.camel.quarkus.component.avro.BuildTimeAvroDataFormat;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class AvroProcessor {

    private static final Logger LOG = Logger.getLogger(AvroProcessor.class);
    private static final String FEATURE = "camel-avro";
    private static DotName BUILD_TIME_AVRO_DATAFORMAT_ANNOTATION = DotName
            .createSimple(BuildTimeAvroDataFormat.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> reflectiveClasses() {
        List<ReflectiveClassBuildItem> items = new ArrayList<ReflectiveClassBuildItem>();
        items.add(new ReflectiveClassBuildItem(false, false, GenericContainer.class));
        return items;
    }

    @BuildStep
    void additionalBeanClasses(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        additionalBeanProducer.produce(new AdditionalBeanBuildItem(AvroDataFormatProducer.class));
    }

    @BuildStep
    AnnotationsTransformerBuildItem markFieldsAnnotatedWithBuildTimeAvroDataFormatAsInjectable() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.FIELD;
            }

            @Override
            public void transform(TransformationContext ctx) {
                FieldInfo fieldInfo = ctx.getTarget().asField();
                if (fieldInfo.annotation(BUILD_TIME_AVRO_DATAFORMAT_ANNOTATION) != null) {
                    ctx.transform().add(Inject.class).done();
                }
            }
        });
    }

    @BuildStep
    void overrideAvroSchemasSerialization(BuildProducer<ObjectSubstitutionBuildItem> substitutions) {
        Holder<Schema, byte[]> holder = new Holder(Schema.class, byte[].class, AvroSchemaSubstitution.class);
        substitutions.produce(new ObjectSubstitutionBuildItem(holder));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void recordAvroSchemasResigtration(BeanArchiveIndexBuildItem beanArchiveIndex,
            BeanContainerBuildItem beanContainer, AvroRecorder avroRecorder) {
        IndexView index = beanArchiveIndex.getIndex();
        for (AnnotationInstance annotation : index.getAnnotations(BUILD_TIME_AVRO_DATAFORMAT_ANNOTATION)) {
            String schemaResourceName = annotation.value().asString();
            FieldInfo fieldInfo = annotation.target().asField();
            String injectedFieldId = fieldInfo.declaringClass().name() + "." + fieldInfo.name();
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaResourceName)) {
                Schema avroSchema = new Schema.Parser().parse(is);
                avroRecorder.recordAvroSchemaResigtration(beanContainer.getValue(), injectedFieldId, avroSchema);
                LOG.debug("Parsed the avro schema at build time from resource named " + schemaResourceName);
            } catch (SchemaParseException | IOException ex) {
                final String message = "An issue occured while parsing schema resource on field " + injectedFieldId;
                throw new RuntimeException(message, ex);
            }
        }
    }

}
