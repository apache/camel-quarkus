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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import org.apache.camel.CatalogCamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Expression;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.FailedToStartRouteException;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.RouteTemplateContext;
import org.apache.camel.StartupStep;
import org.apache.camel.TypeConverter;
import org.apache.camel.ValueHolder;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.apache.camel.catalog.impl.DefaultRuntimeCamelCatalog;
import org.apache.camel.component.microprofile.config.CamelMicroProfilePropertiesSource;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.impl.engine.DefaultAsyncProcessorAwaitManager;
import org.apache.camel.impl.engine.DefaultBeanIntrospection;
import org.apache.camel.impl.engine.DefaultCamelBeanPostProcessor;
import org.apache.camel.impl.engine.DefaultCamelContextNameStrategy;
import org.apache.camel.impl.engine.DefaultComponentNameResolver;
import org.apache.camel.impl.engine.DefaultComponentResolver;
import org.apache.camel.impl.engine.DefaultConfigurerResolver;
import org.apache.camel.impl.engine.DefaultDataFormatResolver;
import org.apache.camel.impl.engine.DefaultEndpointRegistry;
import org.apache.camel.impl.engine.DefaultExchangeFactoryManager;
import org.apache.camel.impl.engine.DefaultExecutorServiceManager;
import org.apache.camel.impl.engine.DefaultHeadersMapFactory;
import org.apache.camel.impl.engine.DefaultInflightRepository;
import org.apache.camel.impl.engine.DefaultInjector;
import org.apache.camel.impl.engine.DefaultInterceptEndpointFactory;
import org.apache.camel.impl.engine.DefaultLanguageResolver;
import org.apache.camel.impl.engine.DefaultMessageHistoryFactory;
import org.apache.camel.impl.engine.DefaultNodeIdFactory;
import org.apache.camel.impl.engine.DefaultPackageScanClassResolver;
import org.apache.camel.impl.engine.DefaultPackageScanResourceResolver;
import org.apache.camel.impl.engine.DefaultReactiveExecutor;
import org.apache.camel.impl.engine.DefaultResourceLoader;
import org.apache.camel.impl.engine.DefaultRouteController;
import org.apache.camel.impl.engine.DefaultRouteFactory;
import org.apache.camel.impl.engine.DefaultRoutesLoader;
import org.apache.camel.impl.engine.DefaultStreamCachingStrategy;
import org.apache.camel.impl.engine.DefaultTracer;
import org.apache.camel.impl.engine.DefaultTransformerRegistry;
import org.apache.camel.impl.engine.DefaultUnitOfWorkFactory;
import org.apache.camel.impl.engine.DefaultUriFactoryResolver;
import org.apache.camel.impl.engine.DefaultValidatorRegistry;
import org.apache.camel.impl.engine.PrototypeExchangeFactory;
import org.apache.camel.impl.engine.PrototypeProcessorExchangeFactory;
import org.apache.camel.impl.engine.RouteService;
import org.apache.camel.impl.engine.TransformerKey;
import org.apache.camel.impl.engine.ValidatorKey;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.FaultToleranceConfigurationDefinition;
import org.apache.camel.model.HystrixConfigurationDefinition;
import org.apache.camel.model.Model;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ModelLifecycleStrategy;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.model.Resilience4jConfigurationDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RouteDefinitionHelper;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.model.cloud.ServiceCallConfigurationDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.model.transformer.TransformerDefinition;
import org.apache.camel.model.validator.ValidatorDefinition;
import org.apache.camel.processor.DefaultAnnotationBasedProcessorFactory;
import org.apache.camel.processor.DefaultDeferServiceFactory;
import org.apache.camel.processor.DefaultInternalProcessorFactory;
import org.apache.camel.processor.DefaultProcessorFactory;
import org.apache.camel.reifier.errorhandler.ErrorHandlerReifier;
import org.apache.camel.reifier.language.ExpressionReifier;
import org.apache.camel.reifier.transformer.TransformerReifier;
import org.apache.camel.reifier.validator.ValidatorReifier;
import org.apache.camel.spi.AnnotationBasedProcessorFactory;
import org.apache.camel.spi.AsyncProcessorAwaitManager;
import org.apache.camel.spi.BeanIntrospection;
import org.apache.camel.spi.BeanProcessorFactory;
import org.apache.camel.spi.BeanProxyFactory;
import org.apache.camel.spi.CamelBeanPostProcessor;
import org.apache.camel.spi.CamelContextNameStrategy;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.spi.ComponentNameResolver;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.ConfigurerResolver;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.DeferServiceFactory;
import org.apache.camel.spi.EndpointRegistry;
import org.apache.camel.spi.ExchangeFactory;
import org.apache.camel.spi.ExchangeFactoryManager;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.HeadersMapFactory;
import org.apache.camel.spi.InflightRepository;
import org.apache.camel.spi.Injector;
import org.apache.camel.spi.InterceptEndpointFactory;
import org.apache.camel.spi.InternalProcessorFactory;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.LanguageResolver;
import org.apache.camel.spi.LocalBeanRepositoryAware;
import org.apache.camel.spi.ManagementNameStrategy;
import org.apache.camel.spi.MessageHistoryFactory;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.ModelReifierFactory;
import org.apache.camel.spi.ModelToXMLDumper;
import org.apache.camel.spi.NodeIdFactory;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.PackageScanResourceResolver;
import org.apache.camel.spi.ProcessorExchangeFactory;
import org.apache.camel.spi.ProcessorFactory;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.ReactiveExecutor;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.ResourceLoader;
import org.apache.camel.spi.RestBindingJaxbDataFormatFactory;
import org.apache.camel.spi.RestRegistryFactory;
import org.apache.camel.spi.RouteController;
import org.apache.camel.spi.RouteFactory;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.StartupStepRecorder;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.spi.Tracer;
import org.apache.camel.spi.Transformer;
import org.apache.camel.spi.TransformerRegistry;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.spi.UnitOfWorkFactory;
import org.apache.camel.spi.UriFactoryResolver;
import org.apache.camel.spi.UuidGenerator;
import org.apache.camel.spi.Validator;
import org.apache.camel.spi.ValidatorRegistry;
import org.apache.camel.spi.XMLRoutesDefinitionLoader;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.support.DefaultUuidGenerator;
import org.apache.camel.support.LocalBeanRegistry;
import org.apache.camel.support.NormalizedUri;
import org.apache.camel.support.ResolverHelper;
import org.apache.camel.support.SimpleUuidGenerator;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastCamelContext extends AbstractCamelContext implements CatalogCamelContext, ModelCamelContext {
    private static final Logger LOG = LoggerFactory.getLogger(FastCamelContext.class);
    private static final UuidGenerator UUID = new SimpleUuidGenerator();

    private final String version;
    private final ModelToXMLDumper modelDumper;
    private Model model;

    public FastCamelContext(FactoryFinderResolver factoryFinderResolver, String version, ModelToXMLDumper modelDumper) {
        super(false);

        this.version = version;
        this.modelDumper = modelDumper;
        this.model = new FastModel(this);

        setFactoryFinderResolver(factoryFinderResolver);
        setTracing(Boolean.FALSE);
        setDebugging(Boolean.FALSE);
        setMessageHistory(Boolean.FALSE);
    }

    private static ValueHolder<String> createValidatorKey(ValidatorDefinition def) {
        return new ValidatorKey(new DataType(def.getType()));
    }

    private static ValueHolder<String> createTransformerKey(TransformerDefinition def) {
        return ObjectHelper.isNotEmpty(def.getScheme()) ? new TransformerKey(def.getScheme())
                : new TransformerKey(new DataType(def.getFromType()), new DataType(def.getToType()));
    }

    @Override
    public void build() {
        super.build();
        // we are fast build so the time should be reset to 0
        resetBuildTime();
    }

    @Override
    public <T> T getExtension(Class<T> type) {
        if (type.isInstance(model)) {
            return type.cast(model);
        }
        return super.getExtension(type);
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String addRouteFromTemplate(String routeId, String routeTemplateId, Map<String, Object> parameters)
            throws Exception {
        return model.addRouteFromTemplate(routeId, routeTemplateId, parameters);
    }

    @Override
    public String addRouteFromTemplate(String routeId, String routeTemplateId, RouteTemplateContext routeTemplateContext)
            throws Exception {
        return model.addRouteFromTemplate(routeId, routeTemplateId, routeTemplateContext);
    }

    @Override
    protected Registry createRegistry() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ManagementNameStrategy createManagementNameStrategy() {
        return null;
    }

    @Override
    protected ShutdownStrategy createShutdownStrategy() {
        return new NoShutdownStrategy();
    }

    @Override
    protected UuidGenerator createUuidGenerator() {
        return new DefaultUuidGenerator();
    }

    @Override
    protected ComponentResolver createComponentResolver() {
        // components are automatically discovered by build steps so we can reduce the
        // operations done by the standard resolver by looking them up directly from the
        // registry
        return (name, context) -> context.getRegistry().lookupByNameAndType(name, Component.class);
    }

    @Override
    protected LanguageResolver createLanguageResolver() {
        return new DefaultLanguageResolver();
    }

    @Override
    protected DataFormatResolver createDataFormatResolver() {
        return new DefaultDataFormatResolver();
    }

    @Override
    protected TypeConverter createTypeConverter() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected TypeConverterRegistry createTypeConverterRegistry() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Injector createInjector() {
        return new DefaultInjector(this);
    }

    @Override
    protected CamelBeanPostProcessor createBeanPostProcessor() {
        return new DefaultCamelBeanPostProcessor(this);
    }

    @Override
    protected ModelJAXBContextFactory createModelJAXBContextFactory() {
        return new DisabledModelJAXBContextFactory();
    }

    @Override
    protected NodeIdFactory createNodeIdFactory() {
        return new DefaultNodeIdFactory();
    }

    @Override
    protected FactoryFinderResolver createFactoryFinderResolver() {
        throw new UnsupportedOperationException(
                "FactoryFinderResolver should have been set in the FastCamelContext constructor");
    }

    @Override
    protected ClassResolver createClassResolver() {
        return new CamelQuarkusClassResolver(Objects.requireNonNull(getApplicationContextClassLoader(),
                "applicationContextClassLoader must be set before calling createClassResolver()"));
    }

    @Override
    protected ProcessorFactory createProcessorFactory() {
        return new DefaultProcessorFactory();
    }

    @Override
    protected InternalProcessorFactory createInternalProcessorFactory() {
        return new DefaultInternalProcessorFactory();
    }

    @Override
    protected InterceptEndpointFactory createInterceptEndpointFactory() {
        return new DefaultInterceptEndpointFactory();
    }

    @Override
    protected RouteFactory createRouteFactory() {
        return new DefaultRouteFactory();
    }

    @Override
    protected MessageHistoryFactory createMessageHistoryFactory() {
        return new DefaultMessageHistoryFactory();
    }

    @Override
    protected InflightRepository createInflightRepository() {
        return new DefaultInflightRepository();
    }

    @Override
    protected AsyncProcessorAwaitManager createAsyncProcessorAwaitManager() {
        return new DefaultAsyncProcessorAwaitManager();
    }

    @Override
    protected RouteController createRouteController() {
        return new DefaultRouteController(this);
    }

    @Override
    protected PackageScanClassResolver createPackageScanClassResolver() {
        return new DefaultPackageScanClassResolver();
    }

    @Override
    protected ExecutorServiceManager createExecutorServiceManager() {
        return new DefaultExecutorServiceManager(this);
    }

    @Override
    protected UnitOfWorkFactory createUnitOfWorkFactory() {
        return new DefaultUnitOfWorkFactory();
    }

    protected CamelContextNameStrategy createCamelContextNameStrategy() {
        return new DefaultCamelContextNameStrategy();
    }

    @Override
    protected HeadersMapFactory createHeadersMapFactory() {
        return new DefaultHeadersMapFactory();
    }

    @Override
    protected BeanProxyFactory createBeanProxyFactory() {
        return ResolverHelper.resolveService(
                getCamelContextReference(),
                getBootstrapFactoryFinder(),
                BeanProxyFactory.FACTORY,
                BeanProxyFactory.class)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot find BeanProxyFactory on classpath. Add camel-bean to classpath."));
    }

    @Override
    protected AnnotationBasedProcessorFactory createAnnotationBasedProcessorFactory() {
        return new DefaultAnnotationBasedProcessorFactory();
    }

    @Override
    protected DeferServiceFactory createDeferServiceFactory() {
        return new DefaultDeferServiceFactory();
    }

    @Override
    protected BeanProcessorFactory createBeanProcessorFactory() {
        return ResolverHelper.resolveService(
                getCamelContextReference(),
                getBootstrapFactoryFinder(),
                BeanProcessorFactory.FACTORY,
                BeanProcessorFactory.class)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot find BeanProcessorFactory on classpath. Add camel-bean to classpath."));
    }

    @Override
    protected BeanIntrospection createBeanIntrospection() {
        return new DefaultBeanIntrospection();
    }

    @Override
    protected PropertiesComponent createPropertiesComponent() {
        org.apache.camel.component.properties.PropertiesComponent pc = new org.apache.camel.component.properties.PropertiesComponent();
        pc.setAutoDiscoverPropertiesSources(false);
        pc.addPropertiesSource(new CamelMicroProfilePropertiesSource());

        return pc;

    }

    @Override
    protected PackageScanResourceResolver createPackageScanResourceResolver() {
        return new DefaultPackageScanResourceResolver();
    }

    @Override
    protected XMLRoutesDefinitionLoader createXMLRoutesDefinitionLoader() {
        return new DisabledXMLRoutesDefinitionLoader();
    }

    @Override
    protected RoutesLoader createRoutesLoader() {
        return new DefaultRoutesLoader();
    }

    @Override
    protected ResourceLoader createResourceLoader() {
        return new DefaultResourceLoader();
    }

    @Override
    protected ModelToXMLDumper createModelToXMLDumper() {
        return modelDumper;
    }

    @Override
    protected RuntimeCamelCatalog createRuntimeCamelCatalog() {
        return new DefaultRuntimeCamelCatalog();
    }

    @Override
    protected Tracer createTracer() {
        Tracer tracer = null;
        if (getRegistry() != null) {
            Map<String, Tracer> map = this.getRegistry().findByTypeWithName(Tracer.class);
            if (map.size() == 1) {
                tracer = map.values().iterator().next();
            }
        }

        if (tracer == null) {
            tracer = getExtension(Tracer.class);
        }

        if (tracer == null) {
            tracer = new DefaultTracer();
            setExtension(Tracer.class, tracer);
        }

        return tracer;
    }

    @Override
    protected RestRegistryFactory createRestRegistryFactory() {
        return ResolverHelper.resolveService(
                getCamelContextReference(),
                getBootstrapFactoryFinder(),
                RestRegistryFactory.FACTORY,
                RestRegistryFactory.class)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot find RestRegistryFactory on classpath. Add camel-rest to classpath."));
    }

    @Override
    protected EndpointRegistry<NormalizedUri> createEndpointRegistry(Map<NormalizedUri, Endpoint> endpoints) {
        return new DefaultEndpointRegistry(this, endpoints);
    }

    @Override
    protected StreamCachingStrategy createStreamCachingStrategy() {
        return new DefaultStreamCachingStrategy();
    }

    @Override
    protected TransformerRegistry<TransformerKey> createTransformerRegistry() {
        return new DefaultTransformerRegistry(this);
    }

    @Override
    protected ValidatorRegistry<ValidatorKey> createValidatorRegistry() {
        return new DefaultValidatorRegistry(this);
    }

    @Override
    protected ReactiveExecutor createReactiveExecutor() {
        return new DefaultReactiveExecutor();
    }

    @Override
    protected ConfigurerResolver createConfigurerResolver() {
        return new DefaultConfigurerResolver();
    }

    @Override
    protected UriFactoryResolver createUriFactoryResolver() {
        return new DefaultUriFactoryResolver();
    }

    @Override
    protected HealthCheckRegistry createHealthCheckRegistry() {
        return ResolverHelper.resolveService(
                getCamelContextReference(),
                getBootstrapFactoryFinder(),
                HealthCheckRegistry.FACTORY,
                HealthCheckRegistry.class)
                .orElse(null);
    }

    @Override
    protected ComponentNameResolver createComponentNameResolver() {
        return new DefaultComponentNameResolver();
    }

    @Override
    protected RestBindingJaxbDataFormatFactory createRestBindingJaxbDataFormatFactory() {
        return ResolverHelper.resolveService(
                getCamelContextReference(),
                getBootstrapFactoryFinder(),
                RestBindingJaxbDataFormatFactory.FACTORY,
                RestBindingJaxbDataFormatFactory.class)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot find RestBindingJaxbDataFormatFactory on classpath. Add camel-jaxb to classpath."));
    }

    @Override
    public void setTypeConverterRegistry(TypeConverterRegistry typeConverterRegistry) {
        super.setTypeConverterRegistry(typeConverterRegistry);

        typeConverterRegistry.setCamelContext(this);
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

    @Override
    public String getEipParameterJsonSchema(String eipName) throws IOException {
        // the eip json schema may be in some of the sub-packages so look until
        // we find it
        String[] subPackages = new String[] { "", "/config", "/dataformat", "/language", "/loadbalancer", "/rest" };
        for (String sub : subPackages) {
            String path = CamelContextHelper.MODEL_DOCUMENTATION_PREFIX + sub + "/" + eipName + ".json";
            InputStream inputStream = getClassResolver().loadResourceAsStream(path);
            if (inputStream != null) {
                try {
                    return IOHelper.loadText(inputStream);
                } finally {
                    IOHelper.close(inputStream);
                }
            }
        }
        return null;
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

    //
    // ModelCamelContext
    //

    @Override
    public Processor createErrorHandler(Route route, Processor processor) throws Exception {
        return ErrorHandlerReifier.reifier(route, route.getErrorHandlerFactory())
                .createErrorHandler(processor);
    }

    @Override
    public void disposeModel() {
        this.model = null;
    }

    @Override
    public String getTestExcludeRoutes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startRouteDefinitions() throws Exception {
        List<RouteDefinition> routeDefinitions = model.getRouteDefinitions();
        if (routeDefinitions != null) {
            startRouteDefinitions(routeDefinitions);
        }
    }

    @Override
    protected ExchangeFactory createExchangeFactory() {
        Optional<ExchangeFactory> result = ResolverHelper.resolveService(
                getCamelContextReference(),
                getBootstrapFactoryFinder(),
                ExchangeFactory.FACTORY,
                ExchangeFactory.class);

        return result.orElseGet(PrototypeExchangeFactory::new);
    }

    @Override
    protected ExchangeFactoryManager createExchangeFactoryManager() {
        return new DefaultExchangeFactoryManager();
    }

    @Override
    protected ProcessorExchangeFactory createProcessorExchangeFactory() {
        Optional<ProcessorExchangeFactory> result = ResolverHelper.resolveService(
                getCamelContextReference(),
                getBootstrapFactoryFinder(),
                ProcessorExchangeFactory.FACTORY,
                ProcessorExchangeFactory.class);

        return result.orElseGet(PrototypeProcessorExchangeFactory::new);
    }

    @Override
    public List<RouteDefinition> getRouteDefinitions() {
        return model.getRouteDefinitions();
    }

    @Override
    public RouteDefinition getRouteDefinition(String id) {
        return model.getRouteDefinition(id);
    }

    @Override
    public void addRouteDefinitions(Collection<RouteDefinition> routeDefinitions) throws Exception {
        model.addRouteDefinitions(routeDefinitions);
    }

    @Override
    public void addRouteDefinition(RouteDefinition routeDefinition) throws Exception {
        model.addRouteDefinition(routeDefinition);
    }

    @Override
    public void removeRouteDefinitions(Collection<RouteDefinition> routeDefinitions) throws Exception {
        model.removeRouteDefinitions(routeDefinitions);
    }

    @Override
    public void removeRouteDefinition(RouteDefinition routeDefinition) throws Exception {
        model.removeRouteDefinition(routeDefinition);
    }

    @Override
    public List<RouteTemplateDefinition> getRouteTemplateDefinitions() {
        return model.getRouteTemplateDefinitions();
    }

    @Override
    public RouteTemplateDefinition getRouteTemplateDefinition(String id) {
        return model.getRouteTemplateDefinition(id);
    }

    @Override
    public void addRouteTemplateDefinitions(Collection<RouteTemplateDefinition> routeTemplateDefinitions) throws Exception {
        model.addRouteTemplateDefinitions(routeTemplateDefinitions);
    }

    @Override
    public void addRouteTemplateDefinition(RouteTemplateDefinition routeTemplateDefinition) throws Exception {
        model.addRouteTemplateDefinition(routeTemplateDefinition);
    }

    @Override
    public void removeRouteTemplateDefinitions(Collection<RouteTemplateDefinition> routeTemplateDefinitions) throws Exception {
        model.removeRouteTemplateDefinitions(routeTemplateDefinitions);
    }

    @Override
    public void removeRouteTemplateDefinition(RouteTemplateDefinition routeTemplateDefinition) throws Exception {
        model.removeRouteTemplateDefinition(routeTemplateDefinition);
    }

    @Override
    public void addRouteTemplateDefinitionConverter(String templateIdPattern, RouteTemplateDefinition.Converter converter) {
        model.addRouteTemplateDefinitionConverter(templateIdPattern, converter);
    }

    @Override
    public List<RestDefinition> getRestDefinitions() {
        return model.getRestDefinitions();
    }

    @Override
    public void addRestDefinitions(Collection<RestDefinition> restDefinitions, boolean addToRoutes) throws Exception {
        model.addRestDefinitions(restDefinitions, addToRoutes);
    }

    @Override
    public Map<String, DataFormatDefinition> getDataFormats() {
        return model.getDataFormats();
    }

    @Override
    public void setDataFormats(Map<String, DataFormatDefinition> dataFormats) {
        model.setDataFormats(dataFormats);
    }

    @Override
    public DataFormatDefinition resolveDataFormatDefinition(String name) {
        return model.resolveDataFormatDefinition(name);
    }

    @Override
    public ProcessorDefinition getProcessorDefinition(String id) {
        return model.getProcessorDefinition(id);
    }

    @Override
    public <T extends ProcessorDefinition<T>> T getProcessorDefinition(String id, Class<T> type) {
        return model.getProcessorDefinition(id, type);
    }

    @Override
    public HystrixConfigurationDefinition getHystrixConfiguration(String id) {
        return model.getHystrixConfiguration(id);
    }

    @Override
    public void setHystrixConfiguration(HystrixConfigurationDefinition configuration) {
        model.setHystrixConfiguration(configuration);
    }

    @Override
    public void setHystrixConfigurations(List<HystrixConfigurationDefinition> configurations) {
        model.setHystrixConfigurations(configurations);
    }

    @Override
    public void addHystrixConfiguration(String id, HystrixConfigurationDefinition configuration) {
        model.addHystrixConfiguration(id, configuration);
    }

    @Override
    public Resilience4jConfigurationDefinition getResilience4jConfiguration(String id) {
        return model.getResilience4jConfiguration(id);
    }

    @Override
    public void setResilience4jConfiguration(Resilience4jConfigurationDefinition configuration) {
        model.setResilience4jConfiguration(configuration);
    }

    @Override
    public void setResilience4jConfigurations(List<Resilience4jConfigurationDefinition> configurations) {
        model.setResilience4jConfigurations(configurations);
    }

    @Override
    public void addResilience4jConfiguration(String id, Resilience4jConfigurationDefinition configuration) {
        model.addResilience4jConfiguration(id, configuration);
    }

    @Override
    public FaultToleranceConfigurationDefinition getFaultToleranceConfiguration(String id) {
        return model.getFaultToleranceConfiguration(id);
    }

    @Override
    public void setFaultToleranceConfiguration(FaultToleranceConfigurationDefinition configuration) {
        model.setFaultToleranceConfiguration(configuration);
    }

    @Override
    public void setFaultToleranceConfigurations(List<FaultToleranceConfigurationDefinition> configurations) {
        model.setFaultToleranceConfigurations(configurations);
    }

    @Override
    public void addFaultToleranceConfiguration(String id, FaultToleranceConfigurationDefinition configuration) {
        model.addFaultToleranceConfiguration(id, configuration);
    }

    @Override
    public List<ValidatorDefinition> getValidators() {
        return model.getValidators();
    }

    @Override
    public void setValidators(List<ValidatorDefinition> validators) {
        model.setValidators(validators);
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return model.getTransformers();
    }

    @Override
    public void setTransformers(List<TransformerDefinition> transformers) {
        model.setTransformers(transformers);
    }

    @Override
    public ServiceCallConfigurationDefinition getServiceCallConfiguration(String serviceName) {
        return model.getServiceCallConfiguration(serviceName);
    }

    @Override
    public void setServiceCallConfiguration(ServiceCallConfigurationDefinition configuration) {
        model.setServiceCallConfiguration(configuration);
    }

    @Override
    public void setServiceCallConfigurations(List<ServiceCallConfigurationDefinition> configurations) {
        model.setServiceCallConfigurations(configurations);
    }

    @Override
    public void addServiceCallConfiguration(String serviceName, ServiceCallConfigurationDefinition configuration) {
        model.addServiceCallConfiguration(serviceName, configuration);
    }

    @Override
    public void setRouteFilterPattern(String include, String exclude) {
        model.setRouteFilterPattern(include, exclude);
    }

    @Override
    public Function<RouteDefinition, Boolean> getRouteFilter() {
        return model.getRouteFilter();
    }

    @Override
    public void setRouteFilter(Function<RouteDefinition, Boolean> filter) {
        model.setRouteFilter(filter);
    }

    @Override
    public ModelReifierFactory getModelReifierFactory() {
        return model.getModelReifierFactory();
    }

    @Override
    public void setModelReifierFactory(ModelReifierFactory modelReifierFactory) {
        model.setModelReifierFactory(modelReifierFactory);
    }

    public void startRouteDefinitions(List<RouteDefinition> routeDefinitions) throws Exception {
        if (model == null && isLightweight()) {
            throw new IllegalStateException("Access to model not supported in lightweight mode");
        }

        // indicate we are staring the route using this thread so
        // we are able to query this if needed
        boolean alreadyStartingRoutes = isStartingRoutes();
        if (!alreadyStartingRoutes) {
            setStartingRoutes(true);
        }

        PropertiesComponent pc = getCamelContextReference().getPropertiesComponent();
        // route templates supports binding beans that are local for the template only
        // in this local mode then we need to check for side-effects (see further)
        LocalBeanRepositoryAware localBeans = null;
        if (getCamelContextReference().getRegistry() instanceof LocalBeanRepositoryAware) {
            localBeans = (LocalBeanRepositoryAware) getCamelContextReference().getRegistry();
        }
        try {
            RouteDefinitionHelper.forceAssignIds(getCamelContextReference(), routeDefinitions);
            for (RouteDefinition routeDefinition : routeDefinitions) {
                // assign ids to the routes and validate that the id's is all unique
                String duplicate = RouteDefinitionHelper.validateUniqueIds(routeDefinition, routeDefinitions);
                if (duplicate != null) {
                    throw new FailedToStartRouteException(
                            routeDefinition.getId(),
                            "duplicate id detected: " + duplicate + ". Please correct ids to be unique among all your routes.");
                }

                // if the route definition was created via a route template then we need to prepare its parameters when the route is being created and started
                if (routeDefinition.isTemplate() != null && routeDefinition.isTemplate()
                        && routeDefinition.getTemplateParameters() != null) {

                    // apply configurer if any present
                    if (routeDefinition.getRouteTemplateContext().getConfigurer() != null) {
                        routeDefinition.getRouteTemplateContext().getConfigurer()
                                .accept(routeDefinition.getRouteTemplateContext());
                    }

                    // copy parameters/bean repository to not cause side-effect
                    Map<String, Object> params = new HashMap<>(routeDefinition.getTemplateParameters());
                    LocalBeanRegistry bbr = (LocalBeanRegistry) routeDefinition.getRouteTemplateContext()
                            .getLocalBeanRepository();
                    LocalBeanRegistry bbrCopy = new LocalBeanRegistry();

                    // make all bean in the bean repository use unique keys (need to add uuid counter)
                    // so when the route template is used again to create another route, then there is
                    // no side-effect from previously used values that Camel may use in its endpoint
                    // registry and elsewhere
                    if (bbr != null && !bbr.isEmpty()) {
                        for (Map.Entry<String, Object> param : params.entrySet()) {
                            Object value = param.getValue();
                            if (value instanceof String) {
                                String oldKey = (String) value;
                                boolean clash = bbr.keys().stream().anyMatch(k -> k.equals(oldKey));
                                if (clash) {
                                    String newKey = oldKey + "-" + UUID.generateUuid();
                                    LOG.debug(
                                            "Route: {} re-assigning local-bean id: {} to: {} to ensure ids are globally unique",
                                            routeDefinition.getId(), oldKey, newKey);
                                    bbrCopy.put(newKey, bbr.remove(oldKey));
                                    param.setValue(newKey);
                                }
                            }
                        }
                        // the remainder of the local beans must also have their ids made global unique
                        for (String oldKey : bbr.keySet()) {
                            String newKey = oldKey + "-" + UUID.generateUuid();
                            LOG.debug(
                                    "Route: {} re-assigning local-bean id: {} to: {} to ensure ids are globally unique",
                                    routeDefinition.getId(), oldKey, newKey);
                            bbrCopy.put(newKey, bbr.get(oldKey));
                            if (!params.containsKey(oldKey)) {
                                // if a bean was bound as local bean with a key and it was not defined as template parameter
                                // then store it as if it was a template parameter with same key=value which allows us
                                // to use this local bean in the route without any problem such as:
                                //   to("bean:{{myBean}}")
                                // and myBean is the local bean id.
                                params.put(oldKey, newKey);
                            }
                        }
                    }

                    Properties prop = new Properties();
                    prop.putAll(params);
                    pc.setLocalProperties(prop);

                    // we need to shadow the bean registry on the CamelContext with the local beans from the route template context
                    if (localBeans != null && bbrCopy != null) {
                        localBeans.setLocalBeanRepository(bbrCopy);
                    }

                    // need to reset auto assigned ids, so there is no clash when creating routes
                    ProcessorDefinitionHelper.resetAllAutoAssignedNodeIds(routeDefinition);
                }

                // must ensure route is prepared, before we can start it
                if (!routeDefinition.isPrepared()) {
                    RouteDefinitionHelper.prepareRoute(getCamelContextReference(), routeDefinition);
                    routeDefinition.markPrepared();
                }

                StartupStepRecorder recorder = getCamelContextReference().adapt(ExtendedCamelContext.class)
                        .getStartupStepRecorder();
                StartupStep step = recorder.beginStep(Route.class, routeDefinition.getRouteId(), "Create Route");
                Route route = model.getModelReifierFactory().createRoute(this, routeDefinition);
                recorder.endStep(step);

                RouteService routeService = new RouteService(route);
                startRouteService(routeService, true);

                // clear local after the route is created via the reifier
                pc.setLocalProperties(null);
                if (localBeans != null) {
                    localBeans.setLocalBeanRepository(null);
                }
            }
        } finally {
            if (!alreadyStartingRoutes) {
                setStartingRoutes(false);
            }
            pc.setLocalProperties(null);
            if (localBeans != null) {
                localBeans.setLocalBeanRepository(null);
            }
        }
    }

    @Override
    public Expression createExpression(ExpressionDefinition definition) {
        return ExpressionReifier.reifier(this, definition).createExpression();
    }

    @Override
    public Predicate createPredicate(ExpressionDefinition definition) {
        return ExpressionReifier.reifier(this, definition).createPredicate();
    }

    @Override
    public RouteDefinition adviceWith(RouteDefinition definition, AdviceWithRouteBuilder builder) throws Exception {
        return AdviceWith.adviceWith(definition, this, builder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerValidator(ValidatorDefinition def) {
        model.getValidators().add(def);
        Validator validator = ValidatorReifier.reifier(this, def).createValidator();
        getValidatorRegistry().put(createValidatorKey(def), validator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerTransformer(TransformerDefinition def) {
        model.getTransformers().add(def);
        Transformer transformer = TransformerReifier.reifier(this, def).createTransformer();
        getTransformerRegistry().put(createTransformerKey(def), transformer);
    }

    @Override
    public void addModelLifecycleStrategy(ModelLifecycleStrategy modelLifecycleStrategy) {
        model.addModelLifecycleStrategy(modelLifecycleStrategy);
    }

    @Override
    public List<ModelLifecycleStrategy> getModelLifecycleStrategies() {
        return model.getModelLifecycleStrategies();
    }
}
