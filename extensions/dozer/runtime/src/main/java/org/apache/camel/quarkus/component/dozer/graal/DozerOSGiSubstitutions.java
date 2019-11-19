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
package org.apache.camel.quarkus.component.dozer.graal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.builder.xml.SchemaLSResourceResolver;
import com.github.dozermapper.core.config.BeanContainer;
import com.github.dozermapper.core.util.DefaultClassLoader;
import com.github.dozermapper.core.util.DozerClassLoader;
import com.github.dozermapper.core.util.RuntimeUtils;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

final class DozerOSGiSubstitutions {

}

@TargetClass(RuntimeUtils.class)
final class SubstituteRuntimeUtils {

    @Substitute
    public static boolean isOSGi() {
        return false;
    }
}

@TargetClass(DozerBeanMapperBuilder.class)
final class SubstituteDozerBeanMapperBuilder {

    @Alias
    private DozerClassLoader fluentDefinedClassLoader;

    @Substitute
    private DozerClassLoader getClassLoader() {
        // Substituted method impl without unwanted references to OSGiClassLoader
        if (fluentDefinedClassLoader == null) {
            return new DefaultClassLoader(DozerBeanMapperBuilder.class.getClassLoader());
        } else {
            return fluentDefinedClassLoader;
        }
    }
}

@TargetClass(SchemaLSResourceResolver.class)
final class SubstituteSchemaLSResourceResolver {

    @Alias
    private BeanContainer beanContainer;

    @Substitute
    private InputStream resolveFromClassPath(String systemId) throws IOException, URISyntaxException {
        // Substituted method impl without unwanted references to OSGi Bundle & Activator

        InputStream source;

        String xsdPath;
        URI uri = new URI(systemId);
        if (uri.getScheme().equalsIgnoreCase("file")) {
            xsdPath = uri.toString();
        } else {
            xsdPath = uri.getPath();
            if (xsdPath.charAt(0) == '/') {
                xsdPath = xsdPath.substring(1);
            }
        }

        ClassLoader localClassLoader = getClass().getClassLoader();

        //Attempt to find within this JAR
        URL url = localClassLoader.getResource(xsdPath);
        if (url == null) {
            //Attempt to find via user defined class loader
            DozerClassLoader dozerClassLoader = beanContainer.getClassLoader();

            url = dozerClassLoader.loadResource(xsdPath);
        }

        if (url == null) {
            throw new IOException(
                    "Could not resolve bean-mapping XML Schema [" + systemId + "]: not found in classpath; " + xsdPath);
        }

        try {
            source = url.openStream();
        } catch (IOException ex) {
            throw new IOException(
                    "Could not resolve bean-mapping XML Schema [" + systemId + "]: not found in classpath; " + xsdPath, ex);
        }

        return source;
    }
}
