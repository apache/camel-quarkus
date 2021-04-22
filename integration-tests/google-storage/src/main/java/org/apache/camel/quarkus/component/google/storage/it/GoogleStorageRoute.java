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
package org.apache.camel.quarkus.component.google.storage.it;

import org.apache.camel.builder.RouteBuilder;

public class GoogleStorageRoute extends RouteBuilder {

    @Override
    public void configure() {

        from("google-storage://" + GoogleStorageResource.TEST_BUCKET3 + "?autoCreateBucket=true" +
                "&destinationBucket=" + GoogleStorageResource.DEST_BUCKET +
                "&moveAfterRead=true" +
                "&includeBody=true")
                        .id(GoogleStorageResource.POLLING_ROUTE_NAME)
                        .to(GoogleStorageResource.DIRECT_POLLING);
    }

}
