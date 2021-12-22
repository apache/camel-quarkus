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
import org.apache.camel.dataformat.univocity.UniVocityFixedDataFormat;
import org.apache.camel.spi.DataFormat;

public class UniVocityFixedWidthDataFormatUnmarshalRoute extends RouteBuilder {

    @Override
    public void configure() {
        final Map<String, DataFormat> testsDataformat = new HashMap<>();

        // Default reading of fixed-width
        testsDataformat.put("default", new UniVocityFixedDataFormat()
                .setFieldLengths(new int[] { 3, 3, 5 }));

        // Reading fixed-width as Map
        testsDataformat.put("map", new UniVocityFixedDataFormat()
                .setFieldLengths(new int[] { 3, 3, 5 })
                .setAsMap(true)
                .setHeaderExtractionEnabled(true));

        // Reading fixed-width as Map with specific headers
        testsDataformat.put("mapWithHeaders", new UniVocityFixedDataFormat()
                .setFieldLengths(new int[] { 3, 3, 5 })
                .setAsMap(true)
                .setHeaders(new String[] { "A", "B", "C" }));

        // Reading fixed-width using an iterator
        testsDataformat.put("lazy", new UniVocityFixedDataFormat()
                .setFieldLengths(new int[] { 3, 3, 5 })
                .setLazyLoad(true));

        // Reading fixed-width using advanced configuration
        testsDataformat.put("advanced", new UniVocityFixedDataFormat()
                .setFieldLengths(new int[] { 3, 3 })
                .setNullValue("N/A")
                .setPadding('_')
                .setComment('!')
                .setSkipEmptyLines(true));

        for (Map.Entry<String, DataFormat> testDataformat : testsDataformat.entrySet()) {
            from("direct:fixed-width-unmarshal-" + testDataformat.getKey()).unmarshal(testDataformat.getValue());
        }
    }

}
