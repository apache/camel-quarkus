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

import org.apache.camel.builder.RouteBuilder;

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
                .marshal().gzipDeflater();
        from("direct:lzf-uncompress")
                .unmarshal().gzipDeflater();
    }
}
