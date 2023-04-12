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
package org.apache.camel.quarkus.component.cxf.soap.deployment;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.quarkiverse.cxf.deployment.CxfDeploymentUtils;
import io.quarkiverse.cxf.deployment.CxfEndpointImplementationBuildItem;
import io.quarkiverse.cxf.deployment.CxfRouteRegistrationRequestorBuildItem;
import io.quarkiverse.cxf.deployment.ServiceSeiBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.apache.camel.quarkus.core.deployment.util.PathFilter.Builder;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class CxfSoapProcessor {

    private static final String FEATURE = "camel-cxf-soap";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    SystemPropertyBuildItem ehcacheAgentSizeOfBypass() {
        return new SystemPropertyBuildItem("org.ehcache.sizeof.AgentSizeOf.bypass", "true");
    }

    @BuildStep
    void registerForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods) {

        IndexView index = combinedIndex.getIndex();

        Stream.of("org.apache.wss4j.dom.handler.WSHandler") // can we remove this?
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownSubclasses(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);

        reflectiveMethods.produce(new ReflectiveMethodBuildItem("org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory",
                "getServiceFactory", new String[0]));

    }

    /**
     * {@code quarkus-cxf} takes care for generating ancillary classes only for services having an implementation
     * annotated with <code>@WebService</code> or similar. That does not catch Camel service implementations. So we need
     * to pass a {@link ServiceSeiBuildItem} holding a service interface to {@code quarkus-cxf} ourselves. That's enough
     * for {@code quarkus-cxf} to generate the necessary ancillary classes for us.
     *
     * @param combinedIndex
     * @param endpointImplementations
     * @param serviceSeis
     */
    @BuildStep
    void serviceSeis(
            CxfBuildTimeConfig cxfBuildTimeConfig,
            CombinedIndexBuildItem combinedIndex,
            List<CxfEndpointImplementationBuildItem> endpointImplementations,
            BuildProducer<ServiceSeiBuildItem> serviceSeis) {

        final Builder b = new PathFilter.Builder();
        cxfBuildTimeConfig.classGeneration.excludePatterns
                .stream()
                .map(pattern -> pattern.replace('.', '/'))
                .forEach(b::include);
        final Predicate<DotName> seiFilter = b.build().asDotNamePredicate().negate();

        final Set<String> alreadyRegisteredInterfaces = new LinkedHashSet<>();
        IndexView index = combinedIndex.getIndex();
        endpointImplementations.stream()
                .map(CxfEndpointImplementationBuildItem::getImplementor)
                .forEach(impl -> {
                    walkParents(index, DotName.createSimple(impl), alreadyRegisteredInterfaces);
                });

        CxfDeploymentUtils.webServiceAnnotations(index)
                .map(annotation -> annotation.target().asClass())
                .filter(wsClassInfo -> Modifier.isInterface(wsClassInfo.flags()))
                .map(wsClassInfo -> wsClassInfo.name())
                .filter(seiFilter)
                .map(DotName::toString)
                .filter(intf -> !alreadyRegisteredInterfaces.contains(intf))
                .map(ServiceSeiBuildItem::new)
                .forEach(serviceSeis::produce);

    }

    static void walkParents(IndexView index, DotName className, Set<String> alreadyRegisteredInterfaces) {
        if (className.toString().startsWith("java.")) {
            /* java.* classes incl. java.lang.Object are definitely not something we look for */
            return;
        }
        final ClassInfo cl = index.getClassByName(className);
        if (cl == null) {
            throw new IllegalStateException("Failed to look up " + className + " in Jandex");
        }
        if (Modifier.isInterface(cl.flags())) {
            alreadyRegisteredInterfaces.add(className.toString());
        }
        if (cl.superName() != null) {
            walkParents(index, cl.superName(), alreadyRegisteredInterfaces);
        }
        cl.interfaceNames().stream().forEach(intf -> walkParents(index, intf, alreadyRegisteredInterfaces));
    }

    @BuildStep
    CxfRouteRegistrationRequestorBuildItem requestCxfRouteRegistration() {
        return new CxfRouteRegistrationRequestorBuildItem(FEATURE);
    }

}
