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
package org.apache.camel.quarkus.component.langchain4j.tools.it;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;

@Path("/langchain4j-tools")
@ApplicationScoped
public class Langchain4jToolsResource {
    private static final ChatMessage SYSTEM_MESSAGE = new SystemMessage("You provide information about a " +
            "specific user name by querying a database users table using the given user_id column value. " +
            "Respond with the text in the format: The user name is <the users name here>");

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    DataSource dataSource;

    void init(@Observes StartupEvent event) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("sql/db-init.sql")) {
                if (stream == null) {
                    throw new IllegalStateException("Cannot find db-init.sql init file");
                }

                // Read database init script and strip comments
                String script = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^--.*(?:\\r?\\n)?",
                        "");
                for (String sql : script.split("(?<=;)")) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
            }
        }
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendChatMessages(String message) throws Exception {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(SYSTEM_MESSAGE);
        chatMessages.add(new UserMessage(message));
        String result = producerTemplate.requestBody("direct:start", chatMessages, String.class);
        return Response.ok(result).build();
    }
}
