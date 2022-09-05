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
package org.apache.camel.quarkus.component.validator.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.ConfigProvider;

public class ValidatorRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {
        // validator from the classpath resource
        from("direct:classpath")
                .to("validator:message.xsd");

        // validator from the filesytem
        String xsdLocation = createTempXsd("message.xsd");

        from("direct:filesystem")
                .to("validator:file:" + xsdLocation);
        // validator from a http endpoint.
        String serverURL = ConfigProvider.getConfig()
                .getConfigValue("xsd.server-url")
                .getRawValue();

        from("direct:http")
                .toD("validator:" + serverURL + "/xsd");

    }

    public String createTempXsd(String sourceXsd) {
        Path tempXsd = null;
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(sourceXsd)) {
            tempXsd = Files.createTempFile("temp", ".xsd");
            Files.copy(resourceAsStream, tempXsd, StandardCopyOption.REPLACE_EXISTING);
            return tempXsd.toAbsolutePath().toString();
        } catch (IOException e) {
            if (tempXsd != null) {
                throw new RuntimeException("Could not read " + sourceXsd + " from classpath or copy it to " + tempXsd,
                        e);
            } else {
                throw new RuntimeException("Failed to create a temp xsd file" + e);
            }
        }

    }
}
