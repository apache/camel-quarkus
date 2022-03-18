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

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.gizmo.ClassCreator;

public class SpringKotlinProcessor {

    @BuildStep(onlyIf = NativeBuild.class)
    void generateKotlinReflectClasses(
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            CurateOutcomeBuildItem curateOutcome) {

        // TODO: Investigate removing this. See https://github.com/apache/camel-quarkus/issues/534
        // The native image build fails with a NoClassDefFoundError without this. Possibly similar to https://github.com/oracle/graal/issues/656.

        // If Kotlin is on the application classpath we don't need to do anything.
        // This check is preferable to trying to discover Kotlin via Class.forname etc,
        // which is not reliable for Gradle builds as kotlin-stdlib is part of the Gradle distribution.
        // Thus such classes will be discoverable at build time but not at runtime, which leads to native build issues.
        ApplicationModel model = curateOutcome.getApplicationModel();
        if (isKotlinStdlibAvailable(model)) {
            return;
        }

        createClass(generatedClass, "kotlin.reflect.KParameter", Object.class.getName(), true);
        createClass(generatedClass, "kotlin.reflect.KCallable", Object.class.getName(), false);
        createClass(generatedClass, "kotlin.reflect.KFunction", "kotlin.reflect.KCallable", false);
    }

    private boolean isKotlinStdlibAvailable(ApplicationModel applicationModel) {
        return applicationModel
                .getDependencies()
                .stream()
                .anyMatch(dependency -> dependency.getArtifactId().startsWith("kotlin-stdlib"));
    }

    private void createClass(
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            String clasName,
            String superClassName,
            boolean isFinal) {
        ClassCreator.builder()
                .className(clasName)
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, false))
                .setFinal(isFinal)
                .superClass(superClassName)
                .build()
                .close();
    }
}
