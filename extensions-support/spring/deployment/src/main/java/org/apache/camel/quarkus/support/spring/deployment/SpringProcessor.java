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
package org.apache.camel.quarkus.support.spring.deployment;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.gizmo.ClassCreator;

public class SpringProcessor {

    @BuildStep(onlyIf = NativeBuild.class)
    void generateKParameterClass(BuildProducer<GeneratedClassBuildItem> generatedClass) {
        // TODO: Investigate removing this. See https://github.com/apache/camel-quarkus/issues/534
        // The native image build fails with a NoClassDefFoundError without this. Possibly similar to https://github.com/oracle/graal/issues/656.

        try {
            Class.forName("kotlin.reflect.KParameter");
        } catch (ClassNotFoundException e) {
            ClassCreator.builder()
                    .className("kotlin.reflect.KParameter")
                    .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, false))
                    .setFinal(true)
                    .superClass(Object.class)
                    .build()
                    .close();
        }

        try {
            Class.forName("kotlin.reflect.KCallable");
        } catch (ClassNotFoundException e) {
            ClassCreator.builder()
                    .className("kotlin.reflect.KCallable")
                    .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, false))
                    .setFinal(true)
                    .superClass(Object.class)
                    .build()
                    .close();
        }
    }
}
