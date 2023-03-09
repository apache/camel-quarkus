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
package org.apache.camel.quarkus.component.smallrye.reactive.messaging.it;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.camel.CamelContext;
import org.apache.camel.component.file.GenericFile;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

@Singleton
public class FilesMessageConsumer {

    private List<String> fileBodies = new ArrayList<>();

    @Inject
    CamelContext context;

    @Incoming("files")
    public CompletionStage<Void> consumeFile(Message<GenericFile<File>> msg) {
        try {
            GenericFile<File> file = msg.getPayload();
            String content = context.getTypeConverter().convertTo(String.class, file);
            fileBodies.add(content);
            return msg.ack();
        } catch (Exception e) {
            e.printStackTrace();
            return msg.nack(e);
        }
    }

    public List<String> getFileBodies() {
        return fileBodies;
    }
}
