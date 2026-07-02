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
package org.apache.camel.quarkus.component.a2a.it;

import java.util.Iterator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.a2a.A2AConstants;
import org.apache.camel.component.a2a.model.StreamResponse;
import org.apache.camel.component.a2a.model.Task;
import org.apache.camel.component.a2a.model.TaskPushNotificationConfig;
import org.apache.camel.component.a2a.model.TextPart;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/a2a")
@ApplicationScoped
public class A2aResource {

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "quarkus.http.test-port", defaultValue = "8081")
    int httpPort;

    @Path("/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String sendMessage(String message) {
        Exchange result = producerTemplate.request("direct:send-message", exchange -> {
            exchange.getMessage().setBody(message);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        Task task = result.getMessage().getBody(Task.class);
        return String.format("{\"taskId\":\"%s\",\"contextId\":\"%s\",\"state\":\"%s\"}",
                task.id(), task.contextId(), task.status().state().name());
    }

    @Path("/send-payload")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String sendMessagePayload(String message) {
        Exchange result = producerTemplate.request("direct:send-message-payload", exchange -> {
            exchange.getMessage().setBody(message);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        return result.getMessage().getBody(String.class);
    }

    @Path("/send-raw")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String sendMessageRaw(String message) {
        Exchange result = producerTemplate.request("direct:send-message-raw", exchange -> {
            exchange.getMessage().setBody(message);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        return result.getMessage().getBody(String.class);
    }

    @Path("/get-task")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getTask(@QueryParam("taskId") String taskId) {
        Exchange result = producerTemplate.request("direct:get-task", exchange -> {
            exchange.getMessage().setHeader(A2AConstants.TASK_ID, taskId);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        Task task = result.getMessage().getBody(Task.class);
        return String.format("{\"taskId\":\"%s\",\"state\":\"%s\"}",
                task.id(), task.status().state().name());
    }

    @Path("/cancel-task")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String cancelTask(@QueryParam("taskId") String taskId) {
        Exchange result = producerTemplate.request("direct:cancel-task", exchange -> {
            exchange.getMessage().setHeader(A2AConstants.TASK_ID, taskId);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        Task task = result.getMessage().getBody(Task.class);
        return String.format("{\"taskId\":\"%s\",\"state\":\"%s\"}",
                task.id(), task.status().state().name());
    }

    @Path("/create-rest-endpoint")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String createRestEndpoint() {
        try {
            camelContext.getEndpoint("a2a:test?protocolBinding=REST&validateAuth=false");
            return "unexpected-success";
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            return cause.getMessage();
        }
    }

    @Path("/send-stream")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String sendMessageStream(String message) {
        Exchange result = producerTemplate.request("direct:send-message-stream", exchange -> {
            exchange.getMessage().setBody(message);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        Iterator<StreamResponse> events = result.getMessage().getBody(Iterator.class);
        int statusUpdates = 0;
        int artifactUpdates = 0;
        while (events.hasNext()) {
            StreamResponse event = events.next();
            if (event.getStatusUpdate() != null) {
                statusUpdates++;
            }
            if (event.getArtifactUpdate() != null) {
                artifactUpdates++;
            }
        }
        return String.format("{\"statusUpdates\":%d,\"artifactUpdates\":%d}", statusUpdates, artifactUpdates);
    }

    @Path("/list-tasks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public String listTasks(@QueryParam("contextId") String contextId) {
        Exchange result = producerTemplate.request("direct:list-tasks", exchange -> {
            if (contextId != null) {
                exchange.getMessage().setHeader(A2AConstants.LIST_CONTEXT_ID, contextId);
            }
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        Object body = result.getMessage().getBody();
        if (body instanceof List) {
            return String.format("{\"count\":%d}", ((List<Task>) body).size());
        }
        return "{\"count\":0}";
    }

    @Path("/send-context")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String sendMessageWithContext(String message, @QueryParam("contextId") String contextId) {
        Exchange result = producerTemplate.request("direct:send-message", exchange -> {
            exchange.getMessage().setBody(message);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
            if (contextId != null) {
                exchange.getMessage().setHeader(A2AConstants.CONTEXT_ID, contextId);
            }
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        Task task = result.getMessage().getBody(Task.class);
        return String.format("{\"taskId\":\"%s\",\"contextId\":\"%s\",\"state\":\"%s\"}",
                task.id(), task.contextId(), task.status().state().name());
    }

    @Path("/send-pojo")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String sendToPojo(String message) {
        Exchange result = producerTemplate.request("direct:send-to-pojo", exchange -> {
            exchange.getMessage().setBody(message);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        Task task = result.getMessage().getBody(Task.class);
        String latestText = "";
        if (task.latest() != null && task.latest().parts() != null) {
            for (Object part : task.latest().parts()) {
                if (part instanceof TextPart tp) {
                    latestText = tp.text();
                    break;
                }
            }
        }
        return String.format("{\"taskId\":\"%s\",\"state\":\"%s\",\"response\":\"%s\"}",
                task.id(), task.status().state().name(), latestText);
    }

    @Path("/send-to-push")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String sendToPush(String message) {
        Exchange result = producerTemplate.request("direct:send-to-push", exchange -> {
            exchange.getMessage().setBody(message);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        Task task = result.getMessage().getBody(Task.class);
        return String.format("{\"taskId\":\"%s\",\"contextId\":\"%s\",\"state\":\"%s\"}",
                task.id(), task.contextId(), task.status().state().name());
    }

    @Path("/push-config/create")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String createPushConfig(@QueryParam("taskId") String taskId, @QueryParam("url") String url) {
        Exchange result = producerTemplate.request("direct:push-config-create", exchange -> {
            exchange.getMessage().setHeader(A2AConstants.TASK_ID, taskId);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
            TaskPushNotificationConfig config = new TaskPushNotificationConfig();
            config.setTaskId(taskId);
            config.setUrl(url);
            exchange.getMessage().setBody(config);
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        TaskPushNotificationConfig config = result.getMessage().getBody(TaskPushNotificationConfig.class);
        return String.format("{\"id\":\"%s\",\"taskId\":\"%s\",\"url\":\"%s\"}",
                config.getId(), config.getTaskId(), config.getUrl());
    }

    @Path("/push-config/get")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getPushConfig(@QueryParam("taskId") String taskId, @QueryParam("configId") String configId) {
        Exchange result = producerTemplate.request("direct:push-config-get", exchange -> {
            exchange.getMessage().setHeader(A2AConstants.TASK_ID, taskId);
            exchange.getMessage().setHeader(A2AConstants.PUSH_CONFIG_ID, configId);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        TaskPushNotificationConfig config = result.getMessage().getBody(TaskPushNotificationConfig.class);
        return String.format("{\"id\":\"%s\",\"taskId\":\"%s\",\"url\":\"%s\"}",
                config.getId(), config.getTaskId(), config.getUrl());
    }

    @Path("/push-config/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public String getPushConfigCount(@QueryParam("taskId") String taskId) {
        Exchange result = producerTemplate.request("direct:push-config-list", exchange -> {
            exchange.getMessage().setHeader(A2AConstants.TASK_ID, taskId);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        Object body = result.getMessage().getBody();
        if (body instanceof List) {
            return String.format("{\"count\":%d}", ((List<TaskPushNotificationConfig>) body).size());
        }
        return "{\"count\":0}";
    }

    @Path("/push-config/delete")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public String deletePushConfig(@QueryParam("taskId") String taskId, @QueryParam("configId") String configId) {
        Exchange result = producerTemplate.request("direct:push-config-delete", exchange -> {
            exchange.getMessage().setHeader(A2AConstants.TASK_ID, taskId);
            exchange.getMessage().setHeader(A2AConstants.PUSH_CONFIG_ID, configId);
            exchange.getMessage().setHeader("CamelA2APort", httpPort);
        });
        if (result.getException() != null) {
            return String.format("{\"error\":\"%s\"}", result.getException().getMessage());
        }
        return "{\"deleted\":true}";
    }
}
