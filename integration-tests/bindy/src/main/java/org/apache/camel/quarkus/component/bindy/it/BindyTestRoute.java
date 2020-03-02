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
package org.apache.camel.quarkus.component.bindy.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.bindy.kvp.BindyKeyValuePairDataFormat;
import org.apache.camel.model.dataformat.BindyDataFormat;
import org.apache.camel.model.dataformat.BindyType;
import org.apache.camel.quarkus.component.bindy.it.model.CsvOrder;
import org.apache.camel.quarkus.component.bindy.it.model.FixedLengthOrder;
import org.apache.camel.quarkus.component.bindy.it.model.MessageOrder;

public class BindyTestRoute extends RouteBuilder {

    @Override
    public void configure() {
        BindyDataFormat bindyCsvDataFormat = new BindyDataFormat();
        bindyCsvDataFormat.setClassType(CsvOrder.class);
        bindyCsvDataFormat.setType(BindyType.Csv.name());
        from("direct:jsonToCsv").marshal(bindyCsvDataFormat);
        from("direct:csvToJson").unmarshal(bindyCsvDataFormat);

        BindyDataFormat bindyFixedLengthDataFormat = new BindyDataFormat();
        bindyFixedLengthDataFormat.setClassType(FixedLengthOrder.class);
        bindyFixedLengthDataFormat.setType(BindyType.Fixed.name());
        from("direct:jsonToFixedLength").marshal(bindyFixedLengthDataFormat);
        from("direct:fixedLengthToJson").unmarshal(bindyFixedLengthDataFormat);

        BindyKeyValuePairDataFormat bindyMessageDataFormat = new BindyKeyValuePairDataFormat(MessageOrder.class);
        from("direct:jsonToMessage").marshal(bindyMessageDataFormat);
        from("direct:messageToJson").unmarshal(bindyMessageDataFormat);
    }
}
