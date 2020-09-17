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
package org.apache.camel.quarkus.component.univocity.parsers.it;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.univocity.UniVocityTsvDataFormat;
import org.apache.camel.spi.DataFormat;

public class UniVocityTsvDataFormatMarshalRoute extends RouteBuilder {

    @Override
    public void configure() {
        final Map<String, DataFormat> testsDataformat = new HashMap<>();

        // Default writing of TSV
        testsDataformat.put("default", new UniVocityTsvDataFormat());

        // Write a TSV with specific headers
        testsDataformat.put("header", new UniVocityTsvDataFormat()
                .setHeaders(new String[] { "A", "C" }));

        // Write a TSV with an advanced configuration
        testsDataformat.put("advanced", new UniVocityTsvDataFormat()
                .setNullValue("N/A")
                .setEmptyValue("empty"));

        for (Map.Entry<String, DataFormat> testDataformat : testsDataformat.entrySet()) {
            from("direct:tsv-marshal-" + testDataformat.getKey()).marshal(testDataformat.getValue());
        }
    }

}
