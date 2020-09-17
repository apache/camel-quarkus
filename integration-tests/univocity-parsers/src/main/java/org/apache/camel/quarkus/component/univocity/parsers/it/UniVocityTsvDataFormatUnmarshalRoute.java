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

public class UniVocityTsvDataFormatUnmarshalRoute extends RouteBuilder {

    @Override
    public void configure() {
        final Map<String, DataFormat> testsDataformat = new HashMap<>();

        // Default reading of TSV
        testsDataformat.put("default", new UniVocityTsvDataFormat());

        // Reading TSV as Map
        testsDataformat.put("map", new UniVocityTsvDataFormat()
                .setAsMap(true)
                .setHeaderExtractionEnabled(true));

        // Reading TSV as Map with specific headers
        testsDataformat.put("mapWithHeaders", new UniVocityTsvDataFormat()
                .setAsMap(true)
                .setHeaders(new String[] { "A", "B", "C" }));

        // Reading TSV using an iterator
        testsDataformat.put("lazy", new UniVocityTsvDataFormat()
                .setLazyLoad(true));

        // Reading TSV using advanced configuration
        testsDataformat.put("advanced", new UniVocityTsvDataFormat()
                .setNullValue("N/A")
                .setIgnoreLeadingWhitespaces(true)
                .setIgnoreTrailingWhitespaces(false)
                .setComment('!')
                .setSkipEmptyLines(true));

        for (Map.Entry<String, DataFormat> testDataformat : testsDataformat.entrySet()) {
            from("direct:tsv-unmarshal-" + testDataformat.getKey()).unmarshal(testDataformat.getValue());
        }
    }

}
