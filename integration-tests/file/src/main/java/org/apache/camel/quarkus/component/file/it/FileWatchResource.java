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
package org.apache.camel.quarkus.component.file.it;

import java.util.Map;

import io.methvin.watcher.hashing.FileHash;
import io.methvin.watcher.hashing.FileHasher;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.file.watch.FileWatchConstants;
import org.apache.camel.component.file.watch.constants.FileEventEnum;
import org.apache.camel.util.ObjectHelper;

@Path("/file-watch")
@ApplicationScoped
public class FileWatchResource {
    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get-events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvent(
            @QueryParam("path") String path,
            @QueryParam("include") String include,
            @QueryParam("fileHasher") String fileHasher) {

        String uri = "file-watch:" + path;
        boolean firstParam = true;

        if (ObjectHelper.isNotEmpty(include)) {
            uri += "?" + "antInclude=" + include;
            firstParam = false;
        }

        if (ObjectHelper.isNotEmpty(fileHasher)) {
            uri += (firstParam ? "?" : "&") + "fileHasher=" + fileHasher;
        }

        final Exchange exchange = consumerTemplate.receiveNoWait(uri);
        if (exchange == null) {
            return Response.noContent().build();
        } else {
            final Message message = exchange.getMessage();
            return Response.ok()
                    .entity(Map.of(
                            "type", message.getHeader(FileWatchConstants.EVENT_TYPE_HEADER, FileEventEnum.class).toString(),
                            "path", message.getHeader(FileWatchConstants.FILE_ABSOLUTE_PATH, String.class)))
                    .build();
        }
    }

    @Identifier("customFileHasher")
    @Singleton
    FileHasher customFileHasher() {
        return new FileHasher() {
            @Override
            public FileHash hash(java.nio.file.Path path) {
                return FileHash.fromLong(1L);
            }
        };
    }
}
