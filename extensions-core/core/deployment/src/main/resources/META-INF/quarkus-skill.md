<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

### Defining routes

Prefer the type-safe **Endpoint DSL**: extend `org.apache.camel.builder.endpoint.EndpointRouteBuilder` (included in `camel-quarkus-core`, no extra dependency) and override `configure()`. Components and their options are fluent methods, so a wrong option is a compile error instead of a runtime failure. No annotation needed; add `@ApplicationScoped` only when the builder itself injects beans or config.

```java
public class MyRoute extends EndpointRouteBuilder {
    @Override
    public void configure() {
        from(timer("tick").period(5000))
            .log("Hello ${body}");
    }
}
```

Beware: builder methods for **all** components compile via `camel-quarkus-core`, even when the component's extension is missing from the `pom.xml`. The route then fails at runtime. Always add the extension that provides each component used; when its artifact name is not obvious from the scheme (e.g. `smtp` is in `camel-quarkus-mail`), find it in the Quarkus platform catalog.

Plain `RouteBuilder` with string URIs (`from("timer:tick?period=5000")`) also works.

YAML DSL needs `camel-quarkus-yaml-dsl`. Route files are auto-discovered from the default `camel.main.routes-include-pattern` (`classpath:camel/*,classpath:camel-template/*,classpath:camel-rest/*`), so a file under `src/main/resources/camel/` loads with no configuration. Setting that property in `application.properties` **replaces** the defaults, so keep the default patterns if you add your own, or other route files stop loading.

### Dependencies and configuration

- Import the `io.quarkus.platform:quarkus-camel-bom` in `<dependencyManagement>` (next to `quarkus-bom`) and declare `camel-quarkus-*` dependencies **without versions**.
- Configure components in `application.properties` with `camel.component.<name>.<option> = value` instead of Java configuration code.
- In dev and test mode, Quarkus **Dev Services** auto-start backing services (Kafka, databases, Infinispan, ...) and inject their connection config. Do not hardcode that detail (Kafka `brokers`, a JDBC URL, ...): setting it yourself disables the Dev Services wiring. Leave it unset in dev/test and configure it only for production (e.g. with a `%prod.` profile).
- Components that talk to a backend (kafka, sql, jms, aws, ...) need that backend actually running. In dev/test, Dev Services starts it — but only when a container runtime (Podman or Docker) is reachable, which a sandbox often lacks. A valid URI is not a working route: when a runtime is up, smoke-test end to end (send a message, confirm it is consumed) before calling it done. When none is reachable, Dev Services cannot start the backend — report that as an environment limitation; do not hardcode connection details to fake it, and do not keep retrying a send that cannot succeed.
- Before using an extension, read its reference page: `https://camel.apache.org/camel-quarkus/latest/reference/extensions/<name>.html`. It documents the extension's usage patterns, configuration, and limitations (such as JVM-only support or native mode constraints).

### Endpoint options: never guess

Do not invent URI parameters. Look them up, in this order:

1. **Preferred**: the Dev MCP tools `camelCatalogDoc` (component documentation, with an `optionsFilter` keyword) and `camelValidateEndpointUri` (validates a URI: unknown options with suggestions, invalid values, missing required options). They use the exact Camel version and classpath of the application. They require dev mode: start it first if it is not running.
2. When dev mode is not an option, the versioned docs: `https://camel.apache.org/components/<camel-major.minor>.x/<name>-component.html`
3. Last resort, offline: the component JAR embeds its full catalog in `META-INF/org/apache/camel/component/<name>/<name>.json` (in the local Maven repository once the `camel-quarkus-<name>` dependency is added).

After writing or changing routes with string URIs or YAML, validate endpoint URIs and simple expressions (no compile or run needed; Endpoint DSL options are already checked by the compiler):

```shell
mvn org.apache.camel:camel-report-maven-plugin:<camel-version>:validate
```

Use the project's Camel version (see the `quarkus-camel-bom` import). It also flags duplicate route ids, `direct:`/`seda:` endpoints with no consumer, and bad Camel options in `application.properties`.

### CDI patterns

```java
@Inject CamelContext context;
@Inject ProducerTemplate producerTemplate;
```

Expose processors to routes by name with `@Named("myProcessor")` + `@ApplicationScoped`, then `.process("myProcessor")`. Use `@ActivateRequestContext` on methods called from routes that touch request-scoped beans. Never use Spring annotations (`@Component`, `@Autowired`): CDI only.

### Testing

Before writing any Camel Quarkus test, read the testing guide:
https://camel.apache.org/camel-quarkus/latest/user-guide/testing.html

The mistakes to avoid:

- JVM tests: prefer `@CamelQuarkusTest` (from the `camel-quarkus-junit` test dependency). It configures Camel test features through annotation attributes instead of a base class, and implies `@QuarkusTest`. JVM-only. Plain `@QuarkusTest` on its own is fine when you need no Camel-specific test features. The `CamelQuarkusTestSupport` base class is still supported; **never extend `CamelTestSupport`** (not suited to Quarkus).
- Native tests are `@QuarkusIntegrationTest` extending the JVM test class. The app runs as a separate process, so **`@Inject` does not work there**: assert through REST endpoints (RestAssured) or external effects instead.
- With `@CamelQuarkusTest`, declare test routes with the `routes` attribute and inject `ProducerTemplate` / `MockEndpoint` (`@Inject` / `@EndpointInject`). Advise application routes with `advices` / `replaceRouteFromWith`, or per test method through an injected `CamelTestHelper`; `mockEndpoints` / `mockEndpointsAndSkip` need isolated mode (`@TestProfile`). With the `CamelQuarkusTestSupport` base class instead, override `createRouteBuilder()` for test routes and use its `adviceRoute("route-id", route -> ...)` helper; never advise routes created via `createRouteBuilder()`.

### Native mode pitfalls

- Classes reached via reflection (custom type converters, processors referenced by class name) need `@RegisterForReflection`.
- Exception classes used in `onException(MyException.class)` must also be registered with `@RegisterForReflection`.
- Resources read with `getResource()` / `getResourceAsStream()` must be listed in `quarkus.native.resources.includes` in `application.properties`.
- Dynamic class loading does not work in native images.
