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
package org.apache.camel.quarkus.k.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.ServiceLoader;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.quarkus.k.core.Runtime;
import org.apache.camel.quarkus.k.listener.ContextConfigurer;
import org.apache.camel.quarkus.k.listener.SourcesConfigurer;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.IOHelper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class LoaderTest {
    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Application.class, LoaderSupport.class)
                    .addAsResource("MyRoutes.java")
                    .addAsResource("routes.groovy")
                    .addAsResource("routes.js")
                    .addAsResource("routes.jsh")
                    .addAsResource("routes.kts")
                    .addAsResource("routes.xml")
                    .addAsResource("routes.yaml"))
            .overrideConfigKey("quarkus.camel.routes-discovery.enabled", "false");

    @Test
    public void testServices() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/loader/services")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("services", String.class)).contains(
                ContextConfigurer.class.getName(),
                SourcesConfigurer.class.getName());
    }

    @Test
    public void testLoadGroovyRoutes() throws IOException {
        String code;
        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.groovy")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/camel-k/loader/load-routes/groovy/MyRoute")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("groovy");
        assertThat(p.getList("endpoints", String.class)).contains("direct://groovy", "log://groovy");
    }

    @Test
    public void testLoadJavaRoutes() throws IOException {
        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/MyRoutes.java")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/camel-k/loader/load-routes/java/MyRoutes")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("java");
        assertThat(p.getList("endpoints", String.class)).contains("direct://java", "log://java");
    }

    @Test
    public void testLoadJavascriptRoutes() throws IOException {
        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.js")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/camel-k/loader/load-routes/js/MyRoute")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("js");
        assertThat(p.getList("endpoints", String.class)).contains("direct://js", "log://js");
    }

    @Test
    public void testLoadJshRoutes() throws IOException {
        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.jsh")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/camel-k/loader/load-routes/jsh/routes")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("jsh");
        assertThat(p.getList("endpoints", String.class)).contains("direct://jsh", "log://jsh");
    }

    @Test
    public void testLoadKotlinRoutes() throws IOException {

        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.kts")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/camel-k/loader/load-routes/kts/MyRoute")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("kotlin");
        assertThat(p.getList("endpoints", String.class)).contains("direct://kotlin", "log://kotlin");
    }

    @Test
    public void testLoadXmlRoutes() throws IOException {
        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.xml")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/camel-k/loader/load-routes/xml/MyRoute")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("xml");
        assertThat(p.getList("endpoints", String.class)).contains("direct://xml", "log://xml");
    }

    @Test
    public void testLoadYamlRoutes() throws IOException {
        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.yaml")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/camel-k/loader/load-routes/yaml/MyRoute")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("yaml");
        assertThat(p.getList("endpoints", String.class)).contains("direct://yaml", "log://yaml");
    }

    @Path("/camel-k/loader")
    @ApplicationScoped
    public static class Application {

        @Inject
        CamelContext context;

        // k-core
        @GET
        @Path("/services")
        @Produces(MediaType.APPLICATION_JSON)
        public JsonObject getServices() {
            JsonArrayBuilder builder = Json.createArrayBuilder();

            ServiceLoader.load(Runtime.Listener.class).forEach(listener -> {
                builder.add(listener.getClass().getName());
            });

            return Json.createObjectBuilder()
                    .add("services", builder)
                    .build();
        }

        @POST
        @Path("/load-routes/{loaderName}/{name}")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.APPLICATION_JSON)
        public JsonObject loadRoutes(@PathParam("loaderName") String loaderName, @PathParam("name") String name, byte[] code)
                throws Exception {
            return LoaderSupport.inspectSource(context, name + "." + loaderName, code);
        }
    }

    static final class LoaderSupport {
        private LoaderSupport() {
        }

        public static JsonObject inspectSource(CamelContext context, String location, byte[] code) throws Exception {
            final Runtime runtime = Runtime.on(context);
            final ExtendedCamelContext ecc = runtime.getExtendedCamelContext();
            final RoutesLoader loader = PluginHelper.getRoutesLoader(ecc);
            final Collection<RoutesBuilder> builders = loader.findRoutesBuilders(ResourceHelper.fromBytes(location, code));

            for (RoutesBuilder builder : builders) {
                runtime.addRoutes(builder);
            }

            return Json.createObjectBuilder()
                    .add("components", extractComponents(context))
                    .add("routes", extractRoutes(context))
                    .add("endpoints", extractEndpoints(context))
                    .build();
        }

        public static JsonObject inspectSource(CamelContext context, String location, String code) throws Exception {
            return inspectSource(context, location, code.getBytes(StandardCharsets.UTF_8));
        }

        public static JsonArrayBuilder extractComponents(CamelContext context) {
            JsonArrayBuilder answer = Json.createArrayBuilder();
            context.getComponentNames().forEach(answer::add);

            return answer;
        }

        public static JsonArrayBuilder extractRoutes(CamelContext context) {
            JsonArrayBuilder answer = Json.createArrayBuilder();
            context.getRoutes().forEach(r -> answer.add(r.getId()));

            return answer;
        }

        public static JsonArrayBuilder extractEndpoints(CamelContext context) {
            JsonArrayBuilder answer = Json.createArrayBuilder();
            context.getEndpoints().forEach(e -> answer.add(e.getEndpointUri()));

            return answer;
        }
    }

}
