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
package org.apache.camel.quarkus.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.camel.CatalogCamelContext;
import org.apache.camel.Component;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.microprofile.config.CamelMicroProfilePropertiesSource;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultComponentResolver;
import org.apache.camel.impl.engine.DefaultDataFormatResolver;
import org.apache.camel.impl.engine.DefaultLanguageResolver;
import org.apache.camel.impl.engine.DefaultShutdownStrategy;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.quarkus.CamelQuarkusBeanPostProcessor;
import org.apache.camel.spi.CamelBeanPostProcessor;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.spi.ComponentNameResolver;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.ManagementNameStrategy;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.ModelToXMLDumper;
import org.apache.camel.spi.PackageScanResourceResolver;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.util.IOHelper;

public class FastCamelContext extends DefaultCamelContext implements CatalogCamelContext, ModelCamelContext {
    private final String version;
    private final ModelToXMLDumper modelDumper;

    public FastCamelContext(FactoryFinderResolver factoryFinderResolver, String version, ModelToXMLDumper modelDumper) {
        super(false);

        this.version = version;
        this.modelDumper = modelDumper;

        ExtendedCamelContext extendedCamelContext = getCamelContextExtension();
        extendedCamelContext.addContextPlugin(FactoryFinderResolver.class, factoryFinderResolver);
        setTracing(Boolean.FALSE);
        setDebugging(Boolean.FALSE);
        setMessageHistory(Boolean.FALSE);
    }

    @Override
    public void build() {
        super.build();
        // we are fast build so the time should be reset to 0
        resetBuildTime();
    }

    @Override
    public String getVersion() {
        // Build time optimized version resolution
        return version;
    }

    @Override
    protected Registry createRegistry() {
        // Registry creation is done at build time
        throw new UnsupportedOperationException(
                "In case that the test based on CamelQuarkusTestSupport throws this exception, " +
                        "be aware that re-starting of context is not possible.");
    }

    @Override
    protected ManagementNameStrategy createManagementNameStrategy() {
        return null;
    }

    @Override
    protected ShutdownStrategy createShutdownStrategy() {
        return new DefaultShutdownStrategy();
    }

    @Override
    protected ComponentResolver createComponentResolver() {
        // components are automatically discovered by build steps so we can reduce the
        // operations done by the standard resolver by looking them up directly from the
        // registry
        return (name, context) -> context.getRegistry().lookupByNameAndType(name, Component.class);
    }

    @Override
    protected ComponentNameResolver createComponentNameResolver() {
        // Component names are discovered at build time
        return null;
    }

    @Override
    protected TypeConverter createTypeConverter() {
        // TypeConverter impls are resolved at build time
        throw new UnsupportedOperationException();
    }

    @Override
    protected TypeConverterRegistry createTypeConverterRegistry() {
        // TypeConverterRegistry creation is done at build time
        return null;
    }

    @Override
    protected CamelBeanPostProcessor createBeanPostProcessor() {
        // TODO: Investigate optimizing this
        // https://github.com/apache/camel-quarkus/issues/2171
        return new CamelQuarkusBeanPostProcessor(this);
    }

    @Override
    protected ModelJAXBContextFactory createModelJAXBContextFactory() {
        // Disabled by default and is provided by the xml-jaxb extension if present on the classpath
        return null;
    }

    protected FactoryFinderResolver createFactoryFinderResolver() {
        // FactoryFinderResolver is initialized at build time
        return null;
    }

    @Override
    protected ClassResolver createClassResolver() {
        return new CamelQuarkusClassResolver(Objects.requireNonNull(getApplicationContextClassLoader(),
                "applicationContextClassLoader must be set before calling createClassResolver()"));
    }

    @Override
    protected PropertiesComponent createPropertiesComponent() {
        org.apache.camel.component.properties.PropertiesComponent pc = new org.apache.camel.component.properties.PropertiesComponent();
        pc.setAutoDiscoverPropertiesSources(true);
        pc.addPropertiesSource(new CamelMicroProfilePropertiesSource());
        return pc;
    }

    @Override
    protected ModelToXMLDumper createModelToXMLDumper() {
        return modelDumper;
    }

    @Override
    protected PackageScanResourceResolver createPackageScanResourceResolver() {
        return new CamelQuarkusPackageScanResourceResolver();
    }

    @Override
    public void doInit() throws Exception {
        super.doInit();
        forceLazyInitialization();
    }

    @Override
    public String getComponentParameterJsonSchema(String componentName) throws IOException {
        Class<?> clazz;

        Object instance = getRegistry().lookupByNameAndType(componentName, Component.class);
        if (instance != null) {
            clazz = instance.getClass();
        } else {
            clazz = getFactoryFinder(DefaultComponentResolver.RESOURCE_PATH).findClass(componentName).orElse(null);
            if (clazz == null) {
                instance = hasComponent(componentName);
                if (instance != null) {
                    clazz = instance.getClass();
                } else {
                    return null;
                }
            }
        }

        // special for ActiveMQ as it is really just JMS
        if ("ActiveMQComponent".equals(clazz.getSimpleName())) {
            return getComponentParameterJsonSchema("jms");
        } else {
            return getJsonSchema(clazz.getPackage().getName(), componentName);
        }
    }

    @Override
    public String getDataFormatParameterJsonSchema(String dataFormatName) throws IOException {
        Class<?> clazz;

        Object instance = getRegistry().lookupByNameAndType(dataFormatName, DataFormat.class);
        if (instance != null) {
            clazz = instance.getClass();
        } else {
            clazz = getFactoryFinder(DefaultDataFormatResolver.DATAFORMAT_RESOURCE_PATH).findClass(dataFormatName).orElse(null);
            if (clazz == null) {
                return null;
            }
        }

        return getJsonSchema(clazz.getPackage().getName(), dataFormatName);
    }

    @Override
    public String getLanguageParameterJsonSchema(String languageName) throws IOException {
        Class<?> clazz;

        Object instance = getRegistry().lookupByNameAndType(languageName, Language.class);
        if (instance != null) {
            clazz = instance.getClass();
        } else {
            clazz = getFactoryFinder(DefaultLanguageResolver.LANGUAGE_RESOURCE_PATH).findClass(languageName).orElse(null);
            if (clazz == null) {
                return null;
            }
        }

        return getJsonSchema(clazz.getPackage().getName(), languageName);
    }

    private String getJsonSchema(String packageName, String name) throws IOException {
        String path = packageName.replace('.', '/') + "/" + name + ".json";
        InputStream inputStream = getClassResolver().loadResourceAsStream(path);

        if (inputStream != null) {
            try {
                return IOHelper.loadText(inputStream);
            } finally {
                IOHelper.close(inputStream);
            }
        }

        return null;
    }

    @Override
    public String getTestExcludeRoutes() {
        throw new UnsupportedOperationException();
    }
}
