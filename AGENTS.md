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
# Apache Camel Quarkus - AI Agent Guidelines
Rules & guidelines for AI agents working on this codebase.

## Project Overview
Apache Camel Quarkus provides Quarkus extensions for Apache Camel components, enabling users to build integration applications with fast boot times and low memory footprints.

## What NOT to do
- Do NOT directly modify generated files under `docs/modules` or `src/main/generated`
- Do NOT use dynamic class loading or reflection (impacts native compilation)
- Do NOT introduce Spring Boot or Spring unless explicitly required

## Technology Stack
- Java 17+
- GraalVM 25+ / Mandrel 25+
- Maven 3.9.11+ (For convenience the maven wrapper is provided in the root directory and can be invoked as `./mvnw` or `./mvnw.cmd`)
- Apache Camel 4.x, Quarkus 3.x. Check `pom.xml` for the current Camel, and Quarkus versions used.

## Repository Structure
```
extensions/          # Native-supported Camel component extensions
extensions-jvm/      # JVM-only extensions (not yet native-compatible)
extensions-core/     # Core Camel Quarkus extensions (core, yaml-dsl, etc.)
extensions-support/  # Shared support libraries used by multiple extensions
integration-tests/   # Integration tests JVM + native
integration-test-groups/  # Groupings of integration tests, primarilly for boosting CI speed & efficiency
integration-tests-jvm/   # JVM-only extension integration tests
integration-tests-support/ # Shared test utilities and test containers support
poms/bom/            # Runtime Bill of Materials
poms/bom-deployment/ # Deployment Bill of Materials
tooling/             # Maven plugin, scripts, templates, and tooling
catalog/             # Camel Quarkus catalog
docs/                # AsciiDoc documentation (Antora-based)
examples/            # Example projects (separate repo: camel-quarkus-examples)
```

## Key Files
- `pom.xml` - Project Maven dependency version properties and test container image names
- `poms/bom/pom.xml` - Runtime dependencies BOM
- `poms/bom-test/pom.xml` - Test dependencies BOM
- `tooling/scripts/test-categories.yaml` - Test categorization for GitHub Actions CI
- `docs/antora/antora.yml` - Antora documentation metadata. Note that `docs/local-build.sh` relies on an external `camel-website` repository and is not intended for self-contained local documentation generation within this project.

## Build Commands
```bash
./mvnw clean install -Dquickly                 # fast build, no tests
./mvnw clean install -T1C -Dquickly            # parallel fast build, no tests
./mvnw clean install                           # full build with JVM tests
./mvnw clean install -Dnative -Ddocker         # full build with native tests (very slow, -Ddocker can usually be omitted on MacOS)
./mvnw clean install -pl extensions/kafka -am  # single extension
./mvnw process-resources -Pformat              # format code & update metadata
```

**Tip:** Use `mvnd` (Maven Daemon) for faster builds. Use `-T1C` for parallel builds (1 thread per CPU core).

## Testing Commands
```bash
./mvnw test                              # unit tests
./mvnw verify                            # integration tests JVM mode
./mvnw verify -Dnative -Ddocker          # integration tests JVM + native mode (-Ddocker can usually be omitted on MacOS)
./mvnw test -Dtest=MyTest                # specific test
./mvnw test -pl integration-tests/kafka  # specific module
```

## Extension Structure
```
extensions/kafka/
├── pom.xml                           # Parent POM
├── runtime/
│   ├── pom.xml
│   └── src/
│       ├── main/java/org/apache/camel/quarkus/component/kafka/
│       ├── main/resources/META-INF/quarkus-extension.yaml
│       └── main/doc/                 # Extension documentation
│           ├── configuration.adoc
│           └── usage.adoc
└── deployment/
    ├── pom.xml
    └── src/
        ├── main/java/org/apache/camel/quarkus/component/kafka/deployment/
        │   └── KafkaProcessor.java
        └── test/java/
```

### Naming Conventions
Classes:
- Deployment: `*Processor.java` for build-time processing `@BuildStep`
- Runtime: `*Recorder.java` for recording and producing `RuntimeValue` used in deployment build steps
- Runtime: `*BuildTimeConfig.java` for `ConfigPhase.BUILD_TIME` & `ConfigPhase.BUILD_AND_RUN_TIME_FIXED` annotated `@ConfigMapping` interfaces
- Runtime: `*RunTimeConfig.java` for `ConfigPhase.RUN_TIME` annotated `@ConfigMapping` interfaces
- Runtime: `*Substitutions.java` for GraalVM substitutions that live in `src/main/java/.../graal/`
- Tests: `*Test.java` (JUnit 6), `*IT.java` for native tests

Packages:
- Runtime: `org.apache.camel.quarkus.component.<name>`
- Deployment: `org.apache.camel.quarkus.component.<name>.deployment`

Annotations:
- `@BuildStep` for deployment processors
- `@Record(ExecutionTime.STATIC_INIT)` for static init
- `@Record(ExecutionTime.RUNTIME_INIT)` for runtime init
- Recorder classes are annotated with `@Recorder`

Configuration:
- `@ConfigRoot(phase = ConfigPhase.BUILD_TIME)` for build-time configuration (read during build, baked into native image)
- `@ConfigRoot(phase = ConfigPhase.RUN_TIME)` for runtime configuration (can be changed at runtime)
- `@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)` for config read at build time but also available at runtime (value cannot change)
- Build-time config: extension enablement & configuration options used to bootstrap components at build time
- Runtime config: connection strings, credentials, timeouts, dynamic behavior
- Prefer runtime config when possible for flexibility; use build-time only when necessary for native compilation

Maven version properties:
- `artifactId.version` for general version properties
- `*-maven-plugin.version` for plugin version properties
- Where appropriate, use `@sync` comments to automatically update version properties
- Dependency versions MUST always be defined in the root project pom.xml

## Creating Extensions

### 1. Scaffold Extension
For extensions providing native support:

```bash
./mvnw cq:create -N -Dcq.artifactIdBase=kafka
```

For extensions providing JVM only support:
```bash
./mvnw cq:create -N -Dcq.artifactIdBase=kafka -Dcq.nativeSupported=false
```

### 2. Add to Test Categories (native only)
Add the extension name to `tooling/scripts/test-categories.yaml`. Test categories should not be overloaded with too many extensions. But should be 'balanced' to ensure each one completes in roughly the same time as others.

### 3. Review Extension Metadata
View `extensions/kafka/runtime/src/main/resources/META-INF/quarkus-extension.yaml`

Update / sync with:
```bash
./mvnw -N cq:update-quarkus-metadata
```

### 4. Complete Integration Tests
Follow `docs/modules/ROOT/pages/contributor-guide/extension-testing.adoc`

### 5. Add Documentation
Follow `docs/modules/ROOT/pages/contributor-guide/extension-documentation.adoc`.

After making changes, run the following from the project root to sync and regenerate the project documentation:
```bash
./mvnw -pl extensions/kafka/deployment process-classes
```

### 6. Format & style
```bash
./mvnw process-resources -Pformat
```

## Integration Tests
Tests use the Quarkus test framework based on JUnit 6.

### Test Structure
Follow `docs/modules/root/contributor-guide/extension-testing.adoc`.

JVM mode tests should be annotated with `@QuarkusTest`.

```java
@QuarkusTest
class KafkaTest {
    // test code
}
```

Native mode tests should extend the JVM mode test and be annotated with `@QuarkusIntegrationTest`.

```java
@QuarkusIntegrationTest
class KafkaIT extends KafkaTest {
}
```

### Important Constraints

- **No `@Inject` in `@QuarkusIntegrationTest`**: Integration tests run the application as a separate process, so CDI injection does not work. Expose application state via JAX-RS endpoints (using `quarkus-resteasy`) and assert via RestAssured in tests. See `integration-test-groups/foundation/core/` for the pattern.
- **Every `@QuarkusTest` needs an IT class**: Each `@QuarkusTest` class must have a corresponding `@QuarkusIntegrationTest` class that extends it, to ensure tests also run in native mode.
- **Grouped test modules share one native application**: Modules under `integration-test-groups/` are compiled into a single grouped native binary per group. Properties in one module's `application.properties` affect ALL modules in the group. Avoid setting properties that alter global Camel behavior (e.g. `camel.main.duration-max-messages`).

## Troubleshooting Native Build Failures
Native compilation with GraalVM requires explicit registration of classes, resources, and proxies. Register these in the deployment module's `*Processor.java` using `@BuildStep` methods. Common patterns:

| Issue | Symptom | Build item | Example |
|-------|---------|------------|---------|
| Reflection | `ClassNotFoundException` / `NoSuchMethodException` in native mode | `ReflectiveClassBuildItem` | `extensions/sql/deployment/.../SqlProcessor.java` |
| Missing resources | `FileNotFoundException` in native mode | `NativeImageResourceBuildItem` / `NativeImageResourceDirectoryBuildItem` | `extensions/xj/deployment/.../XJProcessor.java` |
| Resource bundles | `MissingResourceException` in native mode | `NativeImageResourceBundleBuildItem` | `extensions/fhir/deployment/.../FhirProcessor.java` |
| Proxy classes | `ClassNotFoundException` for proxies | `NativeImageProxyDefinitionBuildItem` | `extensions/influxdb/deployment/.../InfluxdbProcessor.java` |
| Service providers | `ServiceConfigurationError` in native mode | `ServiceProviderBuildItem` | `extensions/fop/deployment/.../FopProcessor.java` |

Use `CombinedIndexBuildItem` to discover classes at build time — see `extensions/servicenow/deployment/.../ServicenowProcessor.java`.

See also https://quarkus.io/guides/writing-extensions.

## Common Tasks

### Update Camel Version
Edit both the project `camel-dependencies` parent version and properties in root `pom.xml`:

1. Parent `camel-dependencies` version (e.g., `<version>4.18.0</version>`)
2. Property `camel.major.minor` (e.g., `<camel.major.minor>4.18</camel.major.minor>`)
3. Property `camel.version` (defined as `${camel.major.minor}.0`)

### Update Quarkus Version
Edit `quarkus.version` in the project root `pom.xml`.

### Sync Dependency Versions
**When to sync dependency versions**: Whenever the Camel or Quarkus version is updated. Or when version properties are updated that influence other properties annotated with `@sync` comments.

```bash
./mvnw cq:sync-versions -N
```

### Regenerate camel-quarkus-bom
**When to regenerate:** Whenever a dependency version is updated in the root `pom.xml` or a new dependency is added to the project in `poms/bom/pom.xml`.

```bash
./mvnw clean install -pl poms/bom
```

### Promote an extension from JVM-only support to Native mode support
```bash
./mvnw -N cq:promote -Dcq.artifactIdBase=kafka
```

## Commit Messages
Reference GitHub issues when applicable.
```
Fixes #issueNumber. Brief description of the change

Longer detailed explanation of changes made.
```
