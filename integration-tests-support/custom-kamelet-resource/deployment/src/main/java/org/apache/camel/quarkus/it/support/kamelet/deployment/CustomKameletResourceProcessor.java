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
package org.apache.camel.quarkus.it.support.kamelet.deployment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.quarkus.deployment.annotations.BuildStep;
import org.apache.camel.quarkus.component.kamelet.deployment.KameletResourceBuildItem;
import org.apache.camel.support.ResourceSupport;

public class CustomKameletResourceProcessor {
    private static final String KAMELET_ID = "custom-log";
    private static final String KAMELET_RES_SCHEME = "app";
    private static final String KAMELET_RES_LOCATION = "custom-log.kamelet.yaml";

    private static final String KAMELET = """
            apiVersion: camel.apache.org/v1alpha1
            kind: Kamelet
            metadata:
              name: custom-log
              labels:
                camel.apache.org/kamelet.type: "sink"
                camel.apache.org/kamelet.name: "custom-kamelet-resource"
                camel.apache.org/kamelet.version: "v1alpha1"
            spec:
              definition:
                title: "Logger"
                description: "Logger"
                properties:
                  loggerName:
                    title: Name of the logging category
                    description:  Name of the logging category
                    type: string
                    default: "logger"
                  showAll:
                    title: Show All
                    description: Show All
                    type: boolean
                    default: false
                  multiLine:
                    title: Multi Line
                    description: Multi Line
                    type: boolean
                    default: false
              dependencies:
                - "camel:log"
              template:
                from:
                  uri: "kamelet:source"
                  steps:
                    - to:
                        uri: "log"
                        parameters:
                          loggerName: "{{loggerName}}"
                          showAll: "{{showAll}}"
                          multiline: "{{multiLine}}"

            """;

    @BuildStep
    KameletResourceBuildItem resource() {
        return new KameletResourceBuildItem(KAMELET_ID, new ResourceSupport(KAMELET_RES_SCHEME, KAMELET_RES_LOCATION) {
            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(KAMELET.getBytes(StandardCharsets.UTF_8));
            }
        });
    }
}
