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
package org.apache.camel.quarkus.component.openapi.java.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.AddToOpenAPIDefinitionBuildItem;
import io.smallrye.openapi.api.util.MergeUtil;
import io.smallrye.openapi.runtime.io.IOContext;
import io.smallrye.openapi.runtime.io.JsonIO;
import io.smallrye.openapi.runtime.io.OpenAPIDefinitionIO;
import io.swagger.v3.core.jackson.mixin.Components31Mixin;
import io.swagger.v3.core.jackson.mixin.ComponentsMixin;
import io.swagger.v3.core.jackson.mixin.DateSchemaMixin;
import io.swagger.v3.core.jackson.mixin.Discriminator31Mixin;
import io.swagger.v3.core.jackson.mixin.DiscriminatorMixin;
import io.swagger.v3.core.jackson.mixin.ExampleMixin;
import io.swagger.v3.core.jackson.mixin.ExtensionsMixin;
import io.swagger.v3.core.jackson.mixin.Info31Mixin;
import io.swagger.v3.core.jackson.mixin.InfoMixin;
import io.swagger.v3.core.jackson.mixin.LicenseMixin;
import io.swagger.v3.core.jackson.mixin.MediaTypeMixin;
import io.swagger.v3.core.jackson.mixin.OpenAPI31Mixin;
import io.swagger.v3.core.jackson.mixin.OpenAPIMixin;
import io.swagger.v3.core.jackson.mixin.OperationMixin;
import io.swagger.v3.core.jackson.mixin.Schema31Mixin;
import io.swagger.v3.core.jackson.mixin.SchemaConverterMixin;
import io.swagger.v3.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.main.DefaultRoutesCollector;
import org.apache.camel.main.RoutesConfigurer;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.openapi.BeanConfig;
import org.apache.camel.openapi.DefaultRestDefinitionsResolver;
import org.apache.camel.openapi.RestDefinitionsResolver;
import org.apache.camel.openapi.RestOpenApiReader;
import org.apache.camel.openapi.RestOpenApiSupport;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesBuilderClassBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.util.FileUtil;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.openapi.OpenApiHelper.clearVendorExtensions;

class OpenApiJavaProcessor {

    private static final String FEATURE = "camel-openapi-java";
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiJavaProcessor.class);
    private static final DotName SCHEMA = DotName.createSimple(Schema.class.getName());
    private static final DotName JSON_SERIALIZER = DotName.createSimple(JsonSerializer.class.getName());
    private static final Class<?>[] OPENAPI_ARRAY_TYPES = new Class<?>[] {
            Integer.class, Long.class, Float.class, Double.class, Boolean.class
    };
    private static final Class<?>[] OPENAPI_MIXIN_TYPES = new Class<?>[] {
            Components31Mixin.class, ComponentsMixin.class,
            DateSchemaMixin.class,
            Discriminator31Mixin.class, DiscriminatorMixin.class,
            ExampleMixin.class,
            ExtensionsMixin.class,
            Info31Mixin.class, InfoMixin.class,
            LicenseMixin.class,
            MediaTypeMixin.class,
            OpenAPI31Mixin.class, OpenAPIMixin.class,
            OperationMixin.class,
            Schema31Mixin.class, SchemaMixin.class,
            SchemaConverterMixin.class
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.swagger.core.v3", "swagger-models-jakarta"));
        indexDependency.produce(new IndexDependencyBuildItem("io.swagger.core.v3", "swagger-core-jakarta"));
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses, CombinedIndexBuildItem combinedIndex) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(SCHEMA.toString()).methods().fields().build());

        IndexView index = combinedIndex.getIndex();
        index.getAllKnownSubclasses(SCHEMA).stream().map(ClassInfo::toString)
                .forEach(name -> reflectiveClasses.produce(ReflectiveClassBuildItem.builder(name).methods().build()));
        index.getAllKnownSubclasses(JSON_SERIALIZER).stream().map(ClassInfo::toString)
                .forEach(name -> reflectiveClasses.produce(ReflectiveClassBuildItem.builder(name).methods().build()));

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(Discriminator.class).build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(OPENAPI_ARRAY_TYPES).methods().build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(OPENAPI_MIXIN_TYPES).methods().build());
    }

    @BuildStep(onlyIf = ExposeOpenApiEnabled.class)
    void exposeOpenAPI(List<CamelRoutesBuilderClassBuildItem> routesBuilderClasses,
            BuildProducer<AddToOpenAPIDefinitionBuildItem> openAPI,
            Capabilities capabilities) throws Exception {

        if (capabilities.isPresent(Capability.SMALLRYE_OPENAPI)) {
            RoutesConfigurer configurer = new RoutesConfigurer();
            List<RoutesBuilder> routes = new ArrayList<>();
            configurer.setRoutesBuilders(routes);
            configurer.setRoutesCollector(new DefaultRoutesCollector());
            configurer.setRoutesIncludePattern(
                    CamelSupport.getOptionalConfigValue("camel.main.routes-include-pattern", String.class, null));
            configurer.setRoutesExcludePattern(
                    CamelSupport.getOptionalConfigValue("camel.main.routes-exclude-pattern", String.class, null));

            final CamelContext ctx = new DefaultCamelContext();
            if (!routesBuilderClasses.isEmpty()) {
                final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (!(loader instanceof QuarkusClassLoader)) {
                    throw new IllegalStateException(
                            QuarkusClassLoader.class.getSimpleName() + " expected as the context class loader");
                }

                for (CamelRoutesBuilderClassBuildItem routesBuilderClass : routesBuilderClasses) {
                    final String className = routesBuilderClass.getDotName().toString();
                    final Class<?> cl = loader.loadClass(className);

                    if (RouteBuilder.class.isAssignableFrom(cl)) {
                        try {
                            final RouteBuilder rb = (RouteBuilder) cl.getDeclaredConstructor().newInstance();
                            routes.add(rb);
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new RuntimeException("Could not instantiate " + className, e);
                        }
                    }
                }
            }

            try {
                configurer.configureRoutes(ctx);
            } catch (Exception e) {
                LOGGER.warn("Failed to configure routes due to: {}.", e.getMessage(), e);
            }
            openAPI.produce(new AddToOpenAPIDefinitionBuildItem(new CamelRestOASFilter(ctx)));
        }
    }

    public static final class ExposeOpenApiEnabled implements BooleanSupplier {
        OpenApiJavaBuildTimeConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.expose().enabled();
        }
    }
}

class CamelRestOASFilter implements OASFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiJavaProcessor.class);
    private final CamelContext context;
    private final RestOpenApiReader reader = new RestOpenApiReader();
    private final RestDefinitionsResolver resolver = new DefaultRestDefinitionsResolver();

    public CamelRestOASFilter(CamelContext context) {
        this.context = context;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        try {
            final List<RestDefinition> rests = resolver.getRestDefinitions(context, null);

            if (rests == null || rests.isEmpty()) {
                LOGGER.warn("Unable to find Camel REST definitions to expose on the Quarkus OpenAPI endpoint.");
                return;
            }

            final BeanConfig bc = new BeanConfig();
            final Info info = new Info();
            final RestConfiguration rc = context.getRestConfiguration();

            initOpenApi(bc, info, rc,
                    Optional.ofNullable(rc.getApiProperties()).orElseGet(HashMap::new));
            final io.swagger.v3.oas.models.OpenAPI openApi = reader.read(context, rests, bc, null, context.getClassResolver());
            if (!rc.isApiVendorExtension()) {
                clearVendorExtensions(openApi);
            }

            // dump to json
            final ObjectMapper mapper = new ObjectMapper(new JsonFactory());
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            String jsonContent = RestOpenApiSupport.getJsonFromOpenAPIAsString(openApi, bc);
            final JsonNode node = mapper.readTree(jsonContent);

            OpenAPI oai = new OpenAPIDefinitionIO(IOContext.forJson(JsonIO.newInstance(null))).readObject(node);
            MergeUtil.merge(openAPI, oai);
        } catch (Exception e) {
            LOGGER.warn("Error generating OpenAPI from Camel Rest DSL due to: {}. This exception is ignored.", e.getMessage(),
                    e);
        }
    }

    /**
     * This consumes a property object, if non-null, by converting it to a string and running the given
     * string consumer on the value.
     */
    private static final BiConsumer<Object, Consumer<String>> consumeProperty = (a, b) -> Optional.ofNullable(a)
            .map(String.class::cast)
            .ifPresent(b);

    private static void initOpenApi(BeanConfig bc, Info info, RestConfiguration rc, Map<String, Object> config) {
        Config c = ConfigProvider.getConfig();
        String host = c.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        String port = c.getOptionalValue("quarkus.http.port", String.class).orElse("8080");
        bc.setHost(host + ":" + port);
        bc.setBasePath("/");

        String contextPath = Optional.ofNullable(rc.getContextPath())
                .orElseGet(() -> c.getOptionalValue("camel.rest.context-path", String.class).orElse(null));
        rc.setContextPath(contextPath);
        c.getOptionalValue("quarkus.http.root-path", String.class).ifPresent(s -> {
            rc.setContextPath(contextPath == null ? s : s + "/" + FileUtil.stripLeadingSeparator(contextPath));
        });

        // configure openApi options
        consumeProperty.accept(config.get("openapi.version"), bc::setVersion);
        consumeProperty.accept(config.get("base.path"), bc::setBasePath);
        consumeProperty.accept(config.get("host"), bc::setHost);
        consumeProperty.accept(config.get("api.version"), info::setVersion);
        consumeProperty.accept(config.get("api.description"), info::setDescription);
        consumeProperty.accept(config.get("api.termsOfService"), info::setTermsOfService);
        consumeProperty.accept(config.get("api.license.name"), bc::setLicense);
        consumeProperty.accept(config.get("api.license.url"), bc::setLicenseUrl);
        consumeProperty.accept(config.get("api.title"), s -> {
            bc.setTitle(s);
            info.setTitle(s);
        });
        Optional.of(config.getOrDefault("schemes", config.getOrDefault("schemas", "http")))
                .map(String.class::cast)
                .map(v -> v.split(","))
                .ifPresent(bc::setSchemes);
        Optional.ofNullable(config.get("api.contact.name"))
                .map(String.class::cast)
                .map(name -> new Contact()
                        .name(name)
                        .email((String) config.get("api.contact.email"))
                        .url((String) config.get("api.contact.url")))
                .ifPresent(info::setContact);
    }
}
