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
package org.apache.camel.quarkus.component.snakeyaml.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.snakeyaml.SnakeYAMLDataFormat;
import org.apache.camel.component.snakeyaml.TypeFilters;
import org.apache.camel.quarkus.component.snakeyaml.it.model.TestPojo;

public class SnakeYAMLRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        SnakeYAMLDataFormat yaml = new SnakeYAMLDataFormat();
        yaml.addTypeFilters(TypeFilters.types(TestPojo.class));

        from("direct:marshall")
                .marshal(yaml);

        from("direct:unmarshall")
                .unmarshal(yaml);
    }
}
