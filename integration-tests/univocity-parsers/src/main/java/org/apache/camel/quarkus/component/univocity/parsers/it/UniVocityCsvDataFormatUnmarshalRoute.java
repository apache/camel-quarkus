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
import org.apache.camel.dataformat.univocity.UniVocityCsvDataFormat;
import org.apache.camel.spi.DataFormat;

public class UniVocityCsvDataFormatUnmarshalRoute extends RouteBuilder {

    @Override
    public void configure() {
        final Map<String, DataFormat> testsDataformat = new HashMap<>();

        // Default reading of CSV
        testsDataformat.put("default", new UniVocityCsvDataFormat());

        // Reading CSV as Map
        UniVocityCsvDataFormat map = new UniVocityCsvDataFormat();
        map.setAsMap(true);
        map.setHeaderExtractionEnabled(true);
        testsDataformat.put("map", map);

        // Reading CSV as Map with specific headers
        UniVocityCsvDataFormat mapWithHeaders = new UniVocityCsvDataFormat();
        mapWithHeaders.setAsMap(true);
        mapWithHeaders.setHeaders("A,B,C");
        testsDataformat.put("mapWithHeaders", mapWithHeaders);

        // Reading CSV using an iterator
        UniVocityCsvDataFormat lazy = new UniVocityCsvDataFormat();
        lazy.setLazyLoad(true);
        testsDataformat.put("lazy", lazy);

        // Reading CSV using advanced configuration
        UniVocityCsvDataFormat advanced = new UniVocityCsvDataFormat();
        advanced.setNullValue("N/A");
        advanced.setDelimiter(';');
        advanced.setIgnoreLeadingWhitespaces(true);
        advanced.setIgnoreTrailingWhitespaces(false);
        advanced.setComment('!');
        advanced.setSkipEmptyLines(true);
        testsDataformat.put("advanced", advanced);

        for (Map.Entry<String, DataFormat> testDataformat : testsDataformat.entrySet()) {
            from("direct:csv-unmarshal-" + testDataformat.getKey()).unmarshal(testDataformat.getValue());
        }
    }

}
