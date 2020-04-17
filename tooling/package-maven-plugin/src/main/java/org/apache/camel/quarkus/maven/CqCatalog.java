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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.DefaultVersionManager;
import org.apache.camel.catalog.RuntimeProvider;
import org.apache.camel.catalog.impl.CatalogHelper;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.BaseModel;
import org.apache.camel.tooling.model.ComponentModel;

public class CqCatalog {

    private final DefaultCamelCatalog catalog;

    public CqCatalog(Path baseDir) {
        super();
        final DefaultCamelCatalog c = new DefaultCamelCatalog(true);
        c.setRuntimeProvider(new CqRuntimeProvider(c));
        c.setVersionManager(new CqVersionManager(c, baseDir));
        this.catalog = c;
    }

    public CqCatalog() {
        super();
        this.catalog = new DefaultCamelCatalog(true);
    }

    public String toCamelComponentArtifactIdBase(String cqArtifactIdBase) {
        if ("core".equals(cqArtifactIdBase)) {
            return "base";
        } else if ("reactive-executor".equals(cqArtifactIdBase)) {
            return "reactive-executor-vertx";
        } else {
            return cqArtifactIdBase;
        }
    }

    public List<String> toCamelArtifactIdBase(String cqArtifactIdBase) {
        if ("core".equals(cqArtifactIdBase)) {
            return Arrays.asList("camel-base", "camel-core-languages");
        } else if ("reactive-executor".equals(cqArtifactIdBase)) {
            return Collections.singletonList("camel-reactive-executor-vertx");
        } else {
            return Collections.singletonList("camel-" + cqArtifactIdBase);
        }
    }

    public List<WrappedModel> filterModels(String artifactIdBase) {
        List<String> camelArtifactIds = toCamelArtifactIdBase(artifactIdBase);
        return Stream.of(Kind.values())
                .flatMap(kind -> kind.all(this))
                .filter(wrappedModel -> camelArtifactIds.contains(wrappedModel.delegate.getArtifactId()))
                .collect(Collectors.toList());
    }

    enum Kind {
        component() {
            @Override
            public Optional<WrappedModel> load(CqCatalog catalog, String name) {
                final ArtifactModel<?> delegate = catalog.catalog.componentModel(name);
                return Optional.ofNullable(delegate == null ? null : new WrappedModel(catalog, this, delegate));
            }

            @Override
            protected Stream<WrappedModel> all(CqCatalog catalog) {
                return catalog.catalog.findComponentNames().stream()
                        .map(name -> new WrappedModel(catalog, this, catalog.catalog.componentModel(name)));
            }

            @Override
            protected String getJson(CqCatalog catalog, BaseModel<?> delegate) {
                return catalog.catalog.componentJSonSchema(delegate.getName());
            }
        },
        language() {
            @Override
            public Optional<WrappedModel> load(CqCatalog catalog, String name) {
                final ArtifactModel<?> delegate = catalog.catalog.languageModel(name);
                return Optional.ofNullable(delegate == null ? null : new WrappedModel(catalog, this, delegate));
            }

            @Override
            protected Stream<WrappedModel> all(CqCatalog catalog) {
                return catalog.catalog.findLanguageNames().stream()
                        .map(name -> new WrappedModel(catalog, this, catalog.catalog.languageModel(name)));
            }

            @Override
            protected String getJson(CqCatalog catalog, BaseModel<?> delegate) {
                return catalog.catalog.languageJSonSchema(delegate.getName());
            }
        },
        dataformat() {
            @Override
            public Optional<WrappedModel> load(CqCatalog catalog, String name) {
                final ArtifactModel<?> delegate = catalog.catalog.dataFormatModel(name);
                return Optional.ofNullable(delegate == null ? null : new WrappedModel(catalog, this, delegate));
            }

            @Override
            protected Stream<WrappedModel> all(CqCatalog catalog) {
                return catalog.catalog.findDataFormatNames().stream()
                        .map(name -> new WrappedModel(catalog, this, catalog.catalog.dataFormatModel(name)));
            }

            @Override
            protected String getJson(CqCatalog catalog, BaseModel<?> delegate) {
                return catalog.catalog.dataFormatJSonSchema(delegate.getName());
            }
        },
        other() {
            @Override
            public Optional<WrappedModel> load(CqCatalog catalog, String name) {
                final ArtifactModel<?> delegate = catalog.catalog.otherModel(name);
                return Optional.ofNullable(delegate == null ? null : new WrappedModel(catalog, this, delegate));
            }

            @Override
            protected Stream<WrappedModel> all(CqCatalog catalog) {
                return catalog.catalog.findOtherNames().stream()
                        .map(name -> new WrappedModel(catalog, this, catalog.catalog.otherModel(name)));
            }

            @Override
            protected String getJson(CqCatalog catalog, BaseModel<?> delegate) {
                return catalog.catalog.otherJSonSchema(delegate.getName());
            }
        };

        public abstract Optional<WrappedModel> load(CqCatalog catalog, String name);

        protected abstract Stream<WrappedModel> all(CqCatalog catalog);

        protected abstract String getJson(CqCatalog catalog, BaseModel<?> delegate);

        public String getPluralName() {
            return name() + "s";
        }
    }

    public static class WrappedModel implements Comparable<WrappedModel> {
        final ArtifactModel<?> delegate;
        final Kind kind;
        final CqCatalog catalog;
        final String supportLevel;
        final String target;

        public WrappedModel(CqCatalog catalog, Kind kind, ArtifactModel<?> delegate) {
            super();
            this.catalog = catalog;
            this.kind = kind;
            this.delegate = delegate;
            final JsonObject json = getJson().getAsJsonObject(kind.name());
            String sl = null;
            try {
                sl = json.get("supportLevel").getAsString();
            } catch (Exception ignored) {
            }
            this.supportLevel = sl;
            String t = null;
            try {
                t = json.get("compilationTarget").getAsString();
            } catch (Exception ignored) {
            }
            this.target = t;
        }

        public String getArtifactId() {
            return delegate.getArtifactId();
        }

        public String getArtifactIdBase() {
            final String artifactId = delegate.getArtifactId();
            if (artifactId.startsWith("camel-quarkus-")) {
                return artifactId.substring("camel-quarkus-".length());
            } else if (artifactId.startsWith("camel-")) {
                return artifactId.substring("camel-".length());
            }
            throw new IllegalStateException(
                    "Unexpected artifactId " + artifactId + "; expected one starting with camel-quarkus- or camel-");
        }

        public String getKind() {
            return kind.name();
        }

        public boolean isFirstScheme() {
            switch (kind) {
            case component:
                final String altSchemes = ((ComponentModel) delegate).getAlternativeSchemes();
                if (altSchemes == null || altSchemes.isEmpty()) {
                    return true;
                } else {
                    final String scheme = delegate.getName();
                    return altSchemes.equals(scheme) || altSchemes.startsWith(scheme + ",");
                }
            default:
                return true;
            }
        }

        public String getSyntax() {
            switch (kind) {
            case component:
                return ((ComponentModel) delegate).getSyntax();
            default:
                throw new UnsupportedOperationException(kind.getPluralName() + " do not have syntax");
            }
        }

        public String getFirstVersion() {
            return delegate.getFirstVersion();
        }

        public String getTitle() {
            return delegate.getTitle();
        }

        public String getDescription() {
            return delegate.getDescription();
        }

        public boolean isDeprecated() {
            return delegate.isDeprecated();
        }

        public JsonObject getJson() {
            final JsonParser jsonParser = new JsonParser();
            return (JsonObject) jsonParser.parse(kind.getJson(catalog, delegate));
        }

        @Override
        public String toString() {
            return "WrappedModel [scheme=" + delegate.getName() + ", kind=" + getKind() + "]";
        }

        @Override
        public int compareTo(WrappedModel other) {
            return this.getTitle().compareToIgnoreCase(other.getTitle());
        }

        public String getSupportLevel() {
            return supportLevel;
        }

        public String getTarget() {
            return target;
        }

    }

    static class CqVersionManager extends DefaultVersionManager {
        private final Path baseDir;

        public CqVersionManager(CamelCatalog camelCatalog, Path baseDir) {
            super(camelCatalog);
            this.baseDir = baseDir;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            try {
                return Files.newInputStream(baseDir.resolve(name));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    static class CqRuntimeProvider implements RuntimeProvider {

        private static final String COMPONENT_DIR = PrepareCatalogQuarkusMojo.CQ_CATALOG_DIR + "/components";
        private static final String DATAFORMAT_DIR = PrepareCatalogQuarkusMojo.CQ_CATALOG_DIR + "/dataformats";
        private static final String LANGUAGE_DIR = PrepareCatalogQuarkusMojo.CQ_CATALOG_DIR + "/languages";
        private static final String OTHER_DIR = PrepareCatalogQuarkusMojo.CQ_CATALOG_DIR + "/others";
        private static final String COMPONENTS_CATALOG = PrepareCatalogQuarkusMojo.CQ_CATALOG_DIR + "/components.properties";
        private static final String DATA_FORMATS_CATALOG = PrepareCatalogQuarkusMojo.CQ_CATALOG_DIR + "/dataformats.properties";
        private static final String LANGUAGE_CATALOG = PrepareCatalogQuarkusMojo.CQ_CATALOG_DIR + "/languages.properties";
        private static final String OTHER_CATALOG = PrepareCatalogQuarkusMojo.CQ_CATALOG_DIR + "/others.properties";

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

}
