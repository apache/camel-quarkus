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
package org.apache.camel.quarkus.component.compression.it;

import java.util.Iterator;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipFileDataFormat;
import org.apache.camel.processor.aggregate.zipfile.ZipAggregationStrategy;

public class CompressionRouteBuilder extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:zipfile-compress")
                .marshal().zipFile();
        from("direct:zipfile-uncompress")
                .unmarshal().zipFile();

        from("direct:zip-deflater-compress")
                .marshal().zipDeflater();
        from("direct:zip-deflater-uncompress")
                .unmarshal().zipDeflater();

        from("direct:gzip-deflater-compress")
                .marshal().gzipDeflater();
        from("direct:gzip-deflater-uncompress")
                .unmarshal().gzipDeflater();

        from("direct:lzf-compress")
                .marshal().lzf();
        from("direct:lzf-uncompress")
                .unmarshal().lzf();

        // Test routes specific to camel-quarkus-zipfile
        ZipFileDataFormat zipFileDataformat = new ZipFileDataFormat();
        zipFileDataformat.setUsingIterator(true);

        from("direct:zipfile-splititerator")
                .unmarshal(zipFileDataformat)
                .split(bodyAs(Iterator.class))
                .streaming()
                .convertBodyTo(String.class)
                .to("mock:zipfile-splititerator")
                .end();

        from("direct:zipfile-aggregate")
                .aggregate(constant(true), new ZipAggregationStrategy())
                .completionSize(constant(2))
                .convertBodyTo(byte[].class)
                .to("mock:zipfile-aggregate");
    }

    @RegisterForReflection(targets = { Iterator.class })
    public class MyReflectionConfiguration {
    }
}
