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
package org.apache.camel.quarkus.component.bean;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.language.bean.Bean;
import org.apache.camel.language.simple.Simple;

/**
 * A bean using language annotations in parameter bindings.
 */
@ApplicationScoped
@Named("withLanguageParamBindingsBean")
@RegisterForReflection
public class WithLanguageParamBindingsBean {
    public String hello(@Simple("${routeId}") String routeId,
            @Bean(ref = "calledFromLanguageAnnotatedParamBean") String valueFromBean) {
        return "wlpb-hello(" + routeId + "," + valueFromBean + ")";
    }
}
