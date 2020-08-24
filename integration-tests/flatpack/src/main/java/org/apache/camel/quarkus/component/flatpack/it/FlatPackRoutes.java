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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.flatpack.FlatpackException;
import org.apache.camel.dataformat.flatpack.FlatpackDataFormat;

public class FlatPackRoutes extends RouteBuilder {

    @Override
    public void configure() {
        FlatpackRowStore flatpackRowStore = new FlatpackRowStore();
        bindToRegistry("flatpackRowStore", flatpackRowStore);

        onException(FlatpackException.class).maximumRedeliveries(1).handled(true).process(e -> {
            FlatpackException cause = e.getProperty(Exchange.EXCEPTION_CAUGHT, FlatpackException.class);
            e.getMessage().setBody(cause.getMessage());
        });

        // Testing delimited data format
        FlatpackDataFormat delimitedDataFormat = new FlatpackDataFormat();
        delimitedDataFormat.setDefinition("INVENTORY-Delimited.pzmap.xml");
        from("direct:delimited-unmarshal").unmarshal(delimitedDataFormat);
        from("direct:delimited-marshal").marshal(delimitedDataFormat);

        // Testing fixed length data format
        FlatpackDataFormat fixedLengthDataFormat = new FlatpackDataFormat();
        fixedLengthDataFormat.setDefinition("PEOPLE-FixedLength.pzmap.xml");
        fixedLengthDataFormat.setFixed(true);
        fixedLengthDataFormat.setIgnoreFirstRecord(false);
        from("direct:fixed-length-unmarshal").unmarshal(fixedLengthDataFormat);
        from("direct:fixed-length-marshal").marshal(fixedLengthDataFormat);

        from("direct:delimited").to("flatpack:delim:INVENTORY-Delimited.pzmap.xml").bean(flatpackRowStore, "flush");
        from("flatpack:delim:INVENTORY-Delimited.pzmap.xml").bean(flatpackRowStore, "store");

        from("direct:fixed").to("flatpack:fixed:PEOPLE-FixedLength.pzmap.xml").bean(flatpackRowStore, "flush");
        from("flatpack:fixed:PEOPLE-FixedLength.pzmap.xml").bean(flatpackRowStore, "store");

        from("direct:header-and-trailer").to("flatpack:fixed:PEOPLE-HeaderAndTrailer.pzmap.xml").bean(flatpackRowStore,
                "flush");
        from("flatpack:fixed:PEOPLE-HeaderAndTrailer.pzmap.xml").bean(flatpackRowStore, "store");

        from("direct:no-descriptor").to("flatpack:foo").bean(flatpackRowStore, "flush");
        from("flatpack:foo").bean(flatpackRowStore, "store");
    }

    @RegisterForReflection
    public static class FlatpackRowStore {
        List<Map<?, ?>> rows = new LinkedList<>();

        public void store(Map<?, ?> m) {
            rows.add(m);
        }

        public List<Map<?, ?>> flush() {
            List<Map<?, ?>> current = rows;
            rows = new LinkedList<>();
            return current;
        }
    }

}
