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
package org.apache.camel.quarkus.support.httpclient5.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import org.apache.camel.quarkus.support.httpclient5.graal.BrotliAbsentBooleanSupplier;

class HttpClient5Processor {
    private static final String BROTLI_INPUT_STREAM_CLASS_NAME = "org.brotli.dec.BrotliInputStream";
    private static final String NTLM_ENGINE_IMPL = "org.apache.hc.client5.http.impl.auth.NTLMEngineImpl";

    @BuildStep
    NativeImageResourceBuildItem suffixListResource() {
        // Required by org.apache.hc.client5.http.psl.PublicSuffixMatcherLoader
        return new NativeImageResourceBuildItem("mozilla/public-suffix-list.txt");
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(NTLM_ENGINE_IMPL));
    }

    @BuildStep(onlyIf = { NativeOrNativeSourcesBuild.class, BrotliAbsentBooleanSupplier.class })
    void generateBrotliInputStreamClass(BuildProducer<GeneratedClassBuildItem> generatedClass) {
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(BROTLI_INPUT_STREAM_CLASS_NAME)
                .superClass(InputStream.class)
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, false))
                .build()) {

            /*
             * Creates a simplified impl of BrotliInputStream to satisfy the native compiler:
             *
             * public class BrotliInputStream extends InputStream {
             *   public BrotliInputStream() {
             *   }
             *
             *   public BrotliInputStream(InputStream stream) {
             *   }
             *
             *   public int read() {
             *     throw new UnsupportedOperationException();
             *   }
             * }
             */

            try (MethodCreator defaultConstructor = classCreator.getMethodCreator("<init>", void.class)) {
                defaultConstructor.setModifiers(Modifier.PUBLIC);
                defaultConstructor.invokeSpecialMethod(
                        MethodDescriptor.ofMethod(BROTLI_INPUT_STREAM_CLASS_NAME, "<init>", void.class),
                        defaultConstructor.getThis());
                defaultConstructor.returnValue(null);
            }

            try (MethodCreator constructorWithInputStreamArg = classCreator.getMethodCreator("<init>", void.class,
                    InputStream.class)) {
                constructorWithInputStreamArg.setModifiers(Modifier.PUBLIC);
                constructorWithInputStreamArg.invokeSpecialMethod(
                        MethodDescriptor.ofMethod(BROTLI_INPUT_STREAM_CLASS_NAME, "<init>", void.class),
                        constructorWithInputStreamArg.getThis());
                constructorWithInputStreamArg.returnValue(null);
            }

            try (MethodCreator readMethod = classCreator.getMethodCreator("read", int.class)) {
                readMethod.setModifiers(Modifier.PUBLIC);
                readMethod.addException(IOException.class);
                readMethod.throwException(UnsupportedOperationException.class,
                        "Cannot read from BrotliInputStream. Add org.brotli:dec to the application classpath");
            }
        }
    }
}
