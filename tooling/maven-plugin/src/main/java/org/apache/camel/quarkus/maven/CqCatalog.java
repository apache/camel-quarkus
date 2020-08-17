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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.DefaultVersionManager;
import org.apache.camel.catalog.Kind;
import org.apache.camel.catalog.RuntimeProvider;
import org.apache.camel.catalog.impl.CatalogHelper;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.ComponentModel;

public class CqCatalog {

    private final DefaultCamelCatalog catalog;

    private static final ThreadLocal<CqCatalog> threadLocalCamelCatalog = ThreadLocal.withInitial(CqCatalog::new);

    public static CqCatalog getThreadLocalCamelCatalog() {
        return threadLocalCamelCatalog.get();
    }

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

    public static String toCamelComponentArtifactIdBase(String cqArtifactIdBase) {
        if ("core".equals(cqArtifactIdBase)) {
            return "base";
        } else if ("reactive-executor".equals(cqArtifactIdBase)) {
            return "reactive-executor-vertx";
        } else {
            return cqArtifactIdBase;
        }
    }

    public static List<String> toCamelArtifactIdBase(String cqArtifactIdBase) {
        if ("core".equals(cqArtifactIdBase)) {
            return Arrays.asList("camel-base", "camel-core-languages");
        } else if ("reactive-executor".equals(cqArtifactIdBase)) {
            return Collections.singletonList("camel-reactive-executor-vertx");
        } else {
            return Collections.singletonList("camel-" + cqArtifactIdBase);
        }
    }

    public Stream<ArtifactModel<?>> filterModels(String artifactIdBase) {
        List<String> camelArtifactIds = toCamelArtifactIdBase(artifactIdBase);
        return models()
                .filter(model -> camelArtifactIds.contains(model.getArtifactId()));
    }

    public Stream<ArtifactModel<?>> models() {
        return kinds()
                .flatMap(kind -> models(kind));
    }

    public Stream<ArtifactModel<?>> models(org.apache.camel.catalog.Kind kind) {
        return catalog.findNames(kind).stream().map(name -> (ArtifactModel<?>) catalog.model(kind, name));
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

    public static List<ArtifactModel<?>> primaryModel(Stream<ArtifactModel<?>> input) {
        final List<ArtifactModel<?>> models = input
                .filter(CqCatalog::isFirstScheme)
                .filter(m -> !m.getName().startsWith("google-") || !m.getName().endsWith("-stream")) // ignore the google stream component variants
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
