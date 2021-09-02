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
package org.apache.camel.quarkus.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.DefaultRuntimeProvider;
import org.apache.camel.catalog.DefaultVersionManager;
import org.apache.camel.catalog.Kind;
import org.apache.camel.catalog.RuntimeProvider;
import org.apache.camel.catalog.impl.CatalogHelper;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.LanguageModel;
import org.apache.camel.tooling.model.OtherModel;

public class CqCatalog {

    public enum Flavor {
        camel("org.apache.camel", "camel-catalog", "org/apache/camel/catalog") {
            @Override
            public RuntimeProvider createRuntimeProvider(DefaultCamelCatalog c) {
                return new DefaultRuntimeProvider(c);
            }
        },
        camelQuarkus("org.apache.camel.quarkus", "camel-quarkus-catalog", CQ_CATALOG_DIR) {
            @Override
            public RuntimeProvider createRuntimeProvider(DefaultCamelCatalog c) {
                return new CqRuntimeProvider(c);
            }
        };

        private final String groupId;
        private final String artifactId;
        private final String catalogDir;

        private Flavor(String groupId, String artifactId, String catalogDir) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.catalogDir = catalogDir;
        }

        public abstract RuntimeProvider createRuntimeProvider(DefaultCamelCatalog c);

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }
    }

    private final DefaultCamelCatalog catalog;
    private final Flavor flavor;
    public static final String CQ_CATALOG_DIR = "org/apache/camel/catalog/quarkus";

    private static final ThreadLocal<CqCatalog> threadLocalCamelCatalog = ThreadLocal.withInitial(CqCatalog::new);

    public static CqCatalog getThreadLocalCamelCatalog() {
        return threadLocalCamelCatalog.get();
    }

    public static CqCatalog findFirstFromClassPath() {
        try {
            Class.forName("org.apache.camel.catalog.quarkus.QuarkusRuntimeProvider");
            return new CqCatalog(Flavor.camelQuarkus,
                    name -> Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
        } catch (ClassNotFoundException e) {
            return new CqCatalog(Flavor.camel,
                    name -> Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
        }
    }

    public CqCatalog(Path baseDir, Flavor flavor) {
        this(flavor, name -> {
            try {
                return Files.newInputStream(baseDir.resolve(name));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CqCatalog(Flavor flavor, Function<String, InputStream> resourceLocator) {
        this.flavor = flavor;
        final DefaultCamelCatalog c = new DefaultCamelCatalog(true);
        c.setRuntimeProvider(flavor.createRuntimeProvider(c));
        c.setVersionManager(new CqVersionManager(c, resourceLocator));
        this.catalog = c;
    }

    public CqCatalog() {
        super();
        this.flavor = Flavor.camel;
        this.catalog = new DefaultCamelCatalog(true);
    }

    public static String toCamelComponentArtifactIdBase(String cqArtifactIdBase) {
        if ("core".equals(cqArtifactIdBase)) {
            return "base";
        } else if ("reactive-executor".equals(cqArtifactIdBase)) {
            return "reactive-executor-vertx";
        } else {
            return cqArtifactIdBase;
        }
    }

    public Stream<ArtifactModel<?>> filterModels(String cqArtifactIdBase) {
        final Predicate<ArtifactModel<?>> filter;
        switch (flavor) {
        case camel:
            if ("core".equals(cqArtifactIdBase)) {
                filter = model -> ("camel-base".equals(model.getArtifactId())
                        || "camel-core-languages".equals(model.getArtifactId())) && !"csimple".equals(model.getName());
            } else if ("csimple".equals(cqArtifactIdBase)) {
                filter = model -> "camel-core-languages".equals(model.getArtifactId()) && "csimple".equals(model.getName());
            } else if ("reactive-executor".equals(cqArtifactIdBase)) {
                filter = model -> "camel-reactive-executor-vertx".equals(model.getArtifactId());
            } else if ("qute".equals(cqArtifactIdBase)) {
                filter = model -> "camel-quarkus-qute-component".equals(model.getArtifactId());
            } else {
                filter = model -> ("camel-" + cqArtifactIdBase).equals(model.getArtifactId());
            }
            break;
        case camelQuarkus:
            filter = model -> ("camel-quarkus-" + cqArtifactIdBase).equals(model.getArtifactId());
            break;
        default:
            throw new IllegalStateException("Unexpected " + Flavor.class.getName() + " " + flavor);
        }
        return models().filter(filter);
    }

    public Stream<ArtifactModel<?>> models() {
        return kinds()
                .flatMap(kind -> models(kind));
    }

    public Stream<ArtifactModel<?>> models(org.apache.camel.catalog.Kind kind) {
        return catalog.findNames(kind).stream().map(name -> (ArtifactModel<?>) catalog.model(kind, name));
    }

    public void addComponent(String name, String className, String jsonSchema) {
        catalog.addComponent(name, className, jsonSchema);
    }

    public void store(Path baseDir) {
        models().forEach(m -> serialize(baseDir.resolve(flavor.catalogDir), m));
    }

    static void serialize(final Path catalogPath, ArtifactModel<?> model) {
        final Path out = catalogPath.resolve(model.getKind() + "s")
                .resolve(model.getName() + ".json");
        try {
            Files.createDirectories(out.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Could not create " + out.getParent(), e);
        }
        String rawJson;
        switch (Kind.valueOf(model.getKind())) {
        case component:
            rawJson = JsonMapper.createParameterJsonSchema((ComponentModel) model);
            break;
        case language:
            rawJson = JsonMapper.createParameterJsonSchema((LanguageModel) model);
            break;
        case dataformat:
            rawJson = JsonMapper.createParameterJsonSchema((DataFormatModel) model);
            break;
        case other:
            rawJson = JsonMapper.createJsonSchema((OtherModel) model);
            break;
        default:
            throw new IllegalStateException("Cannot serialize kind " + model.getKind());
        }

        try {
            Files.write(out, rawJson.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + out, e);
        }
    }

    public static Stream<Kind> kinds() {
        return Stream.of(Kind.values())
                .filter(kind -> kind != org.apache.camel.catalog.Kind.eip);
    }

    public static boolean isFirstScheme(ArtifactModel<?> model) {
        if (model.getKind().equals("component")) {
            final String altSchemes = ((ComponentModel) model).getAlternativeSchemes();
            if (altSchemes == null || altSchemes.isEmpty()) {
                return true;
            } else {
                final String scheme = model.getName();
                return altSchemes.equals(scheme) || altSchemes.startsWith(scheme + ",");
            }
        } else {
            return true;
        }
    }

    public static boolean hasAlternativeScheme(ArtifactModel<?> model, String scheme) {
        if (scheme.equals(model.getName())) {
            return true;
        } else if (model.getKind().equals("component")) {
            final String altSchemes = ((ComponentModel) model).getAlternativeSchemes();
            if (altSchemes == null || altSchemes.isEmpty()) {
                return false;
            } else {
                return altSchemes.endsWith("," + scheme) || altSchemes.indexOf("," + scheme + ",") > 0;
            }
        } else {
            return false;
        }
    }

    public static ArtifactModel<?> findFirstSchemeModel(ArtifactModel<?> model, List<ArtifactModel<?>> models) {
        if (model.getKind().equals("component")) {
            final String altSchemes = ((ComponentModel) model).getAlternativeSchemes();
            if (altSchemes == null || altSchemes.isEmpty()) {
                return model;
            } else {
                final String scheme = model.getName();
                return models.stream()
                        .filter(m -> "component".equals(m.getKind()))
                        .filter(CqCatalog::isFirstScheme)
                        .filter(m -> CqCatalog.hasAlternativeScheme(m, scheme))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Could not find first scheme model for scheme " + scheme + " in " + models));
            }
        } else {
            return model;
        }
    }

    public static List<ArtifactModel<?>> primaryModel(Stream<ArtifactModel<?>> input) {
        final List<ArtifactModel<?>> models = input
                .filter(CqCatalog::isFirstScheme)
                .filter(m -> !m.getName().startsWith("google-") || !m.getName().endsWith("-stream")) // ignore the
                // google stream
                // component
                // variants
                .collect(Collectors.toList());
        if (models.size() > 1) {
            List<ArtifactModel<?>> componentModels = models.stream()
                    .filter(m -> m.getKind().equals("component"))
                    .collect(Collectors.toList());
            if (componentModels.size() == 1) {
                /* If there is only one component take that one */
                return componentModels;
            }
        }
        return models;
    }

    public static ArtifactModel<?> toCamelDocsModel(ArtifactModel<?> m) {
        if ("imap".equals(m.getName())) {
            final ComponentModel clone = (ComponentModel) CqUtils.cloneArtifactModel(m);
            clone.setName("mail");
            clone.setTitle("Mail");
            return clone;
        }
        if (m.getName().startsWith("bindy")) {
            final DataFormatModel clone = (DataFormatModel) CqUtils.cloneArtifactModel(m);
            clone.setName("bindy");
            return clone;
        }
        return m;
    }

    static class CqVersionManager extends DefaultVersionManager {
        private final Function<String, InputStream> resourceLocator;

        public CqVersionManager(CamelCatalog camelCatalog, Function<String, InputStream> resourceLocator) {
            super(camelCatalog);
            this.resourceLocator = resourceLocator;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            return resourceLocator.apply(name);
        }

    }

    static class CqRuntimeProvider implements RuntimeProvider {

        private static final String COMPONENT_DIR = CQ_CATALOG_DIR + "/components";
        private static final String DATAFORMAT_DIR = CQ_CATALOG_DIR + "/dataformats";
        private static final String LANGUAGE_DIR = CQ_CATALOG_DIR + "/languages";
        private static final String OTHER_DIR = CQ_CATALOG_DIR + "/others";
        private static final String COMPONENTS_CATALOG = CQ_CATALOG_DIR + "/components.properties";
        private static final String DATA_FORMATS_CATALOG = CQ_CATALOG_DIR + "/dataformats.properties";
        private static final String LANGUAGE_CATALOG = CQ_CATALOG_DIR + "/languages.properties";
        private static final String OTHER_CATALOG = CQ_CATALOG_DIR + "/others.properties";

        private CamelCatalog camelCatalog;

        public CqRuntimeProvider(CamelCatalog camelCatalog) {
            this.camelCatalog = camelCatalog;
        }

        @Override
        public CamelCatalog getCamelCatalog() {
            return camelCatalog;
        }

        @Override
        public void setCamelCatalog(CamelCatalog camelCatalog) {
            this.camelCatalog = camelCatalog;
        }

        @Override
        public String getProviderName() {
            return "camel-quarkus";
        }

        @Override
        public String getProviderGroupId() {
            return "org.apache.camel.quarkus";
        }

        @Override
        public String getProviderArtifactId() {
            return "camel-quarkus-catalog";
        }

        @Override
        public String getComponentJSonSchemaDirectory() {
            return COMPONENT_DIR;
        }

        @Override
        public String getDataFormatJSonSchemaDirectory() {
            return DATAFORMAT_DIR;
        }

        @Override
        public String getLanguageJSonSchemaDirectory() {
            return LANGUAGE_DIR;
        }

        @Override
        public String getOtherJSonSchemaDirectory() {
            return OTHER_DIR;
        }

        protected String getComponentsCatalog() {
            return COMPONENTS_CATALOG;
        }

        protected String getDataFormatsCatalog() {
            return DATA_FORMATS_CATALOG;
        }

        protected String getLanguageCatalog() {
            return LANGUAGE_CATALOG;
        }

        protected String getOtherCatalog() {
            return OTHER_CATALOG;
        }

        @Override
        public List<String> findComponentNames() {
            List<String> names = new ArrayList<>();
            InputStream is = getCamelCatalog().getVersionManager().getResourceAsStream(getComponentsCatalog());
            if (is != null) {
                try {
                    CatalogHelper.loadLines(is, names);
                } catch (IOException e) {
                    // ignore
                }
            }
            return names;
        }

        @Override
        public List<String> findDataFormatNames() {
            List<String> names = new ArrayList<>();
            InputStream is = getCamelCatalog().getVersionManager().getResourceAsStream(getDataFormatsCatalog());
            if (is != null) {
                try {
                    CatalogHelper.loadLines(is, names);
                } catch (IOException e) {
                    // ignore
                }
            }
            return names;
        }

        @Override
        public List<String> findLanguageNames() {
            List<String> names = new ArrayList<>();
            InputStream is = getCamelCatalog().getVersionManager().getResourceAsStream(getLanguageCatalog());
            if (is != null) {
                try {
                    CatalogHelper.loadLines(is, names);
                } catch (IOException e) {
                    // ignore
                }
            }
            return names;
        }

        @Override
        public List<String> findOtherNames() {
            List<String> names = new ArrayList<>();
            InputStream is = getCamelCatalog().getVersionManager().getResourceAsStream(getOtherCatalog());
            if (is != null) {
                try {
                    CatalogHelper.loadLines(is, names);
                } catch (IOException e) {
                    // ignore
                }
            }
            return names;
        }
    }

    public static class GavCqCatalog extends CqCatalog implements AutoCloseable {

        private final FileSystem jarFileSystem;

        public static GavCqCatalog open(Path localRepository, Flavor flavor, String version) {
            final Path jarPath = CqUtils.copyJar(localRepository, flavor.getGroupId(), flavor.getArtifactId(), version);
            try {
                final FileSystem fs = FileSystems.newFileSystem(jarPath, (ClassLoader) null);
                return new GavCqCatalog(fs, flavor);
            } catch (IOException e) {
                throw new RuntimeException("Could not open file system " + jarPath, e);
            }
        }

        GavCqCatalog(FileSystem jarFileSystem, Flavor flavor) {
            super(jarFileSystem.getRootDirectories().iterator().next(), flavor);
            this.jarFileSystem = jarFileSystem;
        }

        @Override
        public void close() {
            try {
                jarFileSystem.close();
            } catch (IOException e) {
                throw new RuntimeException("Could not close catalog " + jarFileSystem, e);
            }
        }
    }

}
