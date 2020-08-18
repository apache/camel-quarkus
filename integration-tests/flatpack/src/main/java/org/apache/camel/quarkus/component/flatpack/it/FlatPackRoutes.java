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
package org.apache.camel.quarkus.component.flatpack.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.flatpack.FlatpackDataFormat;

public class FlatPackRoutes extends RouteBuilder {

    @Override
    public void configure() {
        // Testing delimited data format
        FlatpackDataFormat delimitedDataFormat = new FlatpackDataFormat();
        delimitedDataFormat.setDefinition("mappings/INVENTORY-Delimited.pzmap.xml");
        from("direct:delimited-unmarshal").unmarshal(delimitedDataFormat);
        from("direct:delimited-marshal").marshal(delimitedDataFormat);

        // Testing fixed length data format
        FlatpackDataFormat fixedLengthDataFormat = new FlatpackDataFormat();
        fixedLengthDataFormat.setDefinition("mappings/PEOPLE-FixedLength.pzmap.xml");
        fixedLengthDataFormat.setFixed(true);
        fixedLengthDataFormat.setIgnoreFirstRecord(false);
        from("direct:fixed-length-unmarshal").unmarshal(fixedLengthDataFormat);
        from("direct:fixed-length-marshal").marshal(fixedLengthDataFormat);
    }

}
