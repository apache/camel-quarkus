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
package org.apache.camel.quarkus.test.annotation;

import java.io.File;
import java.nio.file.Path;

import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.apache.camel.util.FileUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@CamelQuarkusTest(routes = DumpRouteDiagramTest.Routes.class, dumpRouteDiagramFolder = DumpRouteDiagramTest.FOLDER)
class DumpRouteDiagramTest {
    static final String FOLDER = "target/camel-route-diagram-annotation";
    static final String ROUTE_ID = "annotation-diagram";

    @Inject
    CamelContext context;

    @BeforeAll
    static void deleteDiagramsOfPreviousRuns() {
        // Runs before the test instance is created, which is when the diagrams are dumped
        FileUtil.removeDir(new File(FOLDER));
    }

    @Test
    void diagramsAreDumped() {
        Path folder = Path.of(FOLDER);
        assertThat(folder.resolve(AnnotationApplicationRoutes.ROUTE_ID + ".png")).isNotEmptyFile();
        assertThat(folder.resolve(ROUTE_ID + ".png")).isNotEmptyFile();
        assertThat(folder.resolve(context.getName() + "-topology.png")).isNotEmptyFile();
    }

    public static class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:annotationDiagram")
                    .routeId(ROUTE_ID)
                    .to("direct:annotationApp");
        }
    }
}
