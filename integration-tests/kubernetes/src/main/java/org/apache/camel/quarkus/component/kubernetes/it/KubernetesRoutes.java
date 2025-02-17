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
package org.apache.camel.quarkus.component.kubernetes.it;

import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;

@RegisterForReflection(targets = AtomicReference.class)
public class KubernetesRoutes extends RouteBuilder {
    @BindToRegistry
    private final AtomicReference<String> namespace = new AtomicReference<>();

    @Override
    public void configure() {
        from("direct:start")
                .toD("${header.componentName}:local");

        from("kubernetes-config-maps:local?resourceName=camel-configmap-watched")
                .id("configmap-listener")
                .autoStartup(false)
                .filter().simple("${body.metadata.namespace} == ${bean:namespace.get}")
                .to("seda:configMapEvents");

        from("kubernetes-custom-resources:local?crdName=camel-cr=watched&crdGroup=test.com&crdScope=Namespaced&crdVersion=v1&crdPlural=testcrs")
                .id("custom-resource-listener")
                .autoStartup(false)
                .to("seda:customResourceEvents");

        from("kubernetes-deployments:local?resourceName=camel-deployment-watched")
                .id("deployment-listener")
                .autoStartup(false)
                .filter().simple("${body.metadata.namespace} == ${bean:namespace.get}")
                .to("seda:deploymentEvents");

        from("kubernetes-pods:local?resourceName=camel-pod-watched")
                .id("pod-listener")
                .autoStartup(false)
                .filter().simple("${body.metadata.namespace} == ${bean:namespace.get}")
                .to("seda:podEvents");
    }
}
