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
package org.apache.camel.quarkus.component.bean.validator.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.component.bean.validator.BeanValidatorComponent;
import org.apache.camel.quarkus.component.bean.validator.BeanValidatorRecorder;
import org.apache.camel.quarkus.core.deployment.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;

class BeanValidatorProcessor {

    private static final String FEATURE = "camel-bean-validator";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /*
     * The bean-validator component is programmatically configured by the extension thus
     * we can safely prevent camel to instantiate a default instance.
     */
    @BuildStep
    CamelServiceFilterBuildItem serviceFilter() {
        return new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent("bean-validator"));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem beanValidatorComponent(BeanValidatorRecorder recorder) {
        return new CamelRuntimeBeanBuildItem(
                "bean-validator",
                BeanValidatorComponent.class.getName(),
                recorder.createBeanValidatorComponent());
    }
}
