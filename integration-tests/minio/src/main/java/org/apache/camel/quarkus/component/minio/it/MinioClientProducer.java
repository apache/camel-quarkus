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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import io.minio.MinioClient;
import org.eclipse.microprofile.config.ConfigProvider;

public class MinioClientProducer {

    @Produces
    @ApplicationScoped
    @Named("minioClient")
    public MinioClient produceMinioClient() {
        return MinioClient.builder()
                .endpoint("http://" + ConfigProvider.getConfig().getValue("minio.server.host", String.class),
                        ConfigProvider.getConfig().getValue("minio.server.port", Integer.class), false)
                .credentials(MinioResource.SERVER_ACCESS_KEY, MinioResource.SERVER_SECRET_KEY)
                .build();
    }
}
