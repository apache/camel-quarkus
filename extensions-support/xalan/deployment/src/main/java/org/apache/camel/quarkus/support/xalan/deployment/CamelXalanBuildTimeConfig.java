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
package org.apache.camel.quarkus.support.xalan.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.xalan", phase = ConfigPhase.BUILD_TIME)
public class CamelXalanBuildTimeConfig {

    /**
     * A fully qualified class name to set as the {@code javax.xml.transform.TransformerFactory} system property early
     * at the application startup.
     * <p>
     * The system property effectively overrides any service providers defined in
     * {@code META-INF/services/javax.xml.transform.TransformerFactory} files available in the class path. If the option
     * is not present in your {@code application.properties}, the default value is used and the service providers are
     * overridden anyway. To avoid overriding the service providers, set it to an empty value in
     * {@code application.properties}:
     * 
     * <pre>
     * quarkus.camel.xalan.transformer-factory =
     * </pre>
     * <p>
     * Note that any custom transformer factory you pass will only work in native mode if all necessary classes are
     * registered for reflection and all necessary resources are included in the native image. This may already be the
     * case for {@code com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl} if you depend on
     * {@code io.quarkus:quarkus-jaxb} or {@code org.apache.xalan.xsltc.trax.TransformerFactoryImpl} if you depend on
     * {@code org.apache.camel.quarkus:camel-quarkus-support-xalan}.
     */
    @ConfigItem(defaultValue = "org.apache.camel.quarkus.support.xalan.XalanTransformerFactory")
    public Optional<String> transformerFactory;
}
