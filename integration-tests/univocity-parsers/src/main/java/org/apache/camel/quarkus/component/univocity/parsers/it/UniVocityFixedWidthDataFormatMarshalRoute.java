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

public class UniVocityFixedWidthDataFormatMarshalRoute extends RouteBuilder {

    @Override
    public void configure() {
        final Map<String, DataFormat> testsDataformat = new HashMap<>();

        // Default writing of fixed-width
        testsDataformat.put("default", new UniVocityFixedDataFormat()
                .setFieldLengths(new int[] { 3, 3, 5 }));

        // Write a fixed-width with specific headers
        testsDataformat.put("header", new UniVocityFixedDataFormat()
                .setFieldLengths(new int[] { 3, 5 })
                .setHeaders(new String[] { "A", "C" }));

        // Write a fixed-width with an advanced configuration
        testsDataformat.put("advanced", new UniVocityFixedDataFormat()
                .setFieldLengths(new int[] { 5, 5 })
                .setNullValue("N/A")
                .setEmptyValue("empty")
                .setPadding('_'));

        for (Map.Entry<String, DataFormat> testDataformat : testsDataformat.entrySet()) {
            from("direct:fixed-width-marshal-" + testDataformat.getKey()).marshal(testDataformat.getValue());
        }
    }

}
