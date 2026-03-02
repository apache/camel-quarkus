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
package org.apache.camel.quarkus.component.minio.it;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.minio.MinioConstants;

public class MinioRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        rest()
                .put("/api/minio")
                .to("direct:put");

        from("direct:put")
                .log(LoggingLevel.INFO, "Processing ${id}")
                .log(LoggingLevel.INFO, "Headers (before): ${headers}")
                .log(LoggingLevel.INFO, "Body (before): ${body}")

                // TODO: Remove this - https://github.com/apache/camel-quarkus/issues/8355
                .setHeader(MinioConstants.CONTENT_LENGTH).simple("${header.Content-Length}")
                .setHeader(MinioConstants.OBJECT_NAME, simple("${header.objectName}"))

                .log(LoggingLevel.INFO, "Headers (in between): ${headers}")
                .log(LoggingLevel.INFO, "Body (in between): ${body}")

                .toD("minio:${header.bucketName}?minioClient=#minioClient");
    }
}
