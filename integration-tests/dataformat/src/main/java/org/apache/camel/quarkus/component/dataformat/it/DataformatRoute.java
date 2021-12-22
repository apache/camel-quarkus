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
package org.apache.camel.quarkus.component.dataformat.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.YAMLLibrary;
import org.apache.camel.quarkus.component.dataformat.it.model.TestPojo;

public class DataformatRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:snakeyaml-dataformat-component-marshal")
                .to("dataformat:snakeYaml:marshal");

        from("direct:snakeyaml-dataformat-component-unmarshal")
                .to("dataformat:snakeYaml:unmarshal?unmarshalType=org.apache.camel.quarkus.component.dataformat.it.model.TestPojo");

        from("direct:snakeyaml-dsl-marshal")
                .marshal().yaml(YAMLLibrary.SnakeYAML);

        from("direct:snakeyaml-dsl-unmarshal")
                .unmarshal().yaml(YAMLLibrary.SnakeYAML, TestPojo.class);

        from("direct:ical-marshal")
                .marshal()
                .ical(true);

        from("direct:ical-unmarshal")
                .unmarshal()
                .ical(true);

    }
}
