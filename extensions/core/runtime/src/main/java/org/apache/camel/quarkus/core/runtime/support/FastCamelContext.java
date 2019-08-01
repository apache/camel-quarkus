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
package org.apache.camel.quarkus.core.runtime.support;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.NoFactoryAvailableException;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.TypeConverter;
import org.apache.camel.impl.DefaultExecutorServiceManager;
import org.apache.camel.impl.DefaultModelJAXBContextFactory;
import org.apache.camel.impl.converter.DefaultTypeConverter;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.impl.engine.BaseRouteService;
import org.apache.camel.impl.engine.BeanProcessorFactoryResolver;
import org.apache.camel.impl.engine.BeanProxyFactoryResolver;
import org.apache.camel.impl.engine.DefaultAsyncProcessorAwaitManager;
import org.apache.camel.impl.engine.DefaultCamelBeanPostProcessor;
import org.apache.camel.impl.engine.DefaultCamelContextNameStrategy;
import org.apache.camel.impl.engine.DefaultClassResolver;
import org.apache.camel.impl.engine.DefaultEndpointRegistry;
import org.apache.camel.impl.engine.DefaultFactoryFinderResolver;
import org.apache.camel.impl.engine.DefaultInflightRepository;
import org.apache.camel.impl.engine.DefaultInjector;
import org.apache.camel.impl.engine.DefaultMessageHistoryFactory;
import org.apache.camel.impl.engine.DefaultNodeIdFactory;
import org.apache.camel.impl.engine.DefaultPackageScanClassResolver;
import org.apache.camel.impl.engine.DefaultProcessorFactory;
import org.apache.camel.impl.engine.DefaultRouteController;
import org.apache.camel.impl.engine.DefaultStreamCachingStrategy;
import org.apache.camel.impl.engine.DefaultTransformerRegistry;
import org.apache.camel.impl.engine.DefaultUnitOfWorkFactory;
import org.apache.camel.impl.engine.DefaultValidatorRegistry;
import org.apache.camel.impl.engine.EndpointKey;
import org.apache.camel.impl.engine.HeadersMapFactoryResolver;
import org.apache.camel.impl.engine.ReactiveExecutorResolver;
import org.apache.camel.impl.engine.RestRegistryFactoryResolver;
import org.apache.camel.impl.engine.ServicePool;
import org.apache.camel.impl.transformer.TransformerKey;
import org.apache.camel.impl.validator.ValidatorKey;
import org.apache.camel.processor.MulticastProcessor;
import org.apache.camel.quarkus.core.runtime.CamelRuntime;
import org.apache.camel.quarkus.core.runtime.support.FastModel.FastRouteContext;
import org.apache.camel.spi.AsyncProcessorAwaitManager;
import org.apache.camel.spi.BeanProcessorFactory;
import org.apache.camel.spi.BeanProxyFactory;
import org.apache.camel.spi.CamelBeanPostProcessor;
import org.apache.camel.spi.CamelContextNameStrategy;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.EndpointRegistry;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.FactoryFinder;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.HeadersMapFactory;
import org.apache.camel.spi.InflightRepository;
import org.apache.camel.spi.Injector;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.LanguageResolver;
import org.apache.camel.spi.ManagementNameStrategy;
import org.apache.camel.spi.MessageHistoryFactory;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.NodeIdFactory;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.ProcessorFactory;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.ReactiveExecutor;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.RestRegistryFactory;
import org.apache.camel.spi.RouteController;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.spi.TransformerRegistry;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.spi.UnitOfWorkFactory;
import org.apache.camel.spi.UuidGenerator;
import org.apache.camel.spi.ValidatorRegistry;

public class FastCamelContext extends AbstractCamelContext {

    private Object model;

    public FastCamelContext() {
        super(false);
        setInitialization(Initialization.Eager);
        setTracing(Boolean.FALSE);
        setDebugging(Boolean.FALSE);
        setMessageHistory(Boolean.FALSE);
    }

    public void setModel(Object model) {
        this.model = model;
    }

    public void clearModel() {
        this.model = null;
        for (BaseRouteService rs : getRouteServices().values()) {
            ((FastRouteContext) rs.getRouteContext()).clearModel();
        }
    }

    @Override
    public <T> T getExtension(Class<T> type) {
        if (type.isInstance(model)) {
            return type.cast(model);
        }
        return super.getExtension(type);
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
        return new FastUuidGenerator();
    }

    @Override
    protected ComponentResolver createComponentResolver() {
        return (name, context) -> resolve(Component.class, "component", name, context);
    }

    @Override
    protected LanguageResolver createLanguageResolver() {
        return (name, context) -> resolve(Language.class, "language", name, context);
    }

    @Override
    protected DataFormatResolver createDataFormatResolver() {
        return new DataFormatResolver() {
            @Override
            public DataFormat resolveDataFormat(String name, CamelContext context) {
                return createDataFormat(name, context);
            }

            @Override
            public DataFormat createDataFormat(String name, CamelContext context) {
                return resolve(DataFormat.class, "dataformat", name, context);
            }
        };
    }

    @Override
    protected TypeConverter createTypeConverter() {
        // lets use the new fast type converter registry
        return new DefaultTypeConverter(
                this, getPackageScanClassResolver(),
                getInjector(), getDefaultFactoryFinder(), isLoadTypeConverters());
    }

    @Override
    protected TypeConverterRegistry createTypeConverterRegistry() {
        TypeConverter typeConverter = getTypeConverter();
        if (typeConverter instanceof TypeConverterRegistry) {
            return (TypeConverterRegistry) typeConverter;
        }
        return null;
    }

    @Override
    protected Injector createInjector() {
        FactoryFinder finder = getDefaultFactoryFinder();
        try {
            return (Injector) finder.newInstance("Injector");
        } catch (NoFactoryAvailableException e) {
            // lets use the default injector
            return new DefaultInjector(this);
        }
    }

    @Override
    protected CamelBeanPostProcessor createBeanPostProcessor() {
        return new DefaultCamelBeanPostProcessor(this);
    }

    @Override
    protected ModelJAXBContextFactory createModelJAXBContextFactory() {
        return new DefaultModelJAXBContextFactory();
    }

    @Override
    protected NodeIdFactory createNodeIdFactory() {
        return new DefaultNodeIdFactory();
    }

    @Override
    protected FactoryFinderResolver createFactoryFinderResolver() {
        return new DefaultFactoryFinderResolver();
    }

    @Override
    protected ClassResolver createClassResolver() {
        return new DefaultClassResolver(this);
    }

    @Override
    protected ProcessorFactory createProcessorFactory() {
        return new DefaultProcessorFactory();
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
    protected ServicePool<Producer> createProducerServicePool() {
        return new ServicePool<>(Endpoint::createProducer, Producer::getEndpoint, 100);
    }

    @Override
    protected ServicePool<PollingConsumer> createPollingConsumerServicePool() {
        return new ServicePool<>(Endpoint::createPollingConsumer, PollingConsumer::getEndpoint, 100);
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
        return new HeadersMapFactoryResolver().resolve(this);
    }

    @Override
    protected BeanProxyFactory createBeanProxyFactory() {
        return new BeanProxyFactoryResolver().resolve(this);
    }

    @Override
    protected BeanProcessorFactory createBeanProcessorFactory() {
        return new BeanProcessorFactoryResolver().resolve(this);
    }

    @Override
    protected RestRegistryFactory createRestRegistryFactory() {
        return new RestRegistryFactoryResolver().resolve(this);
    }

    @Override
    protected EndpointRegistry<EndpointKey> createEndpointRegistry(Map<EndpointKey, Endpoint> endpoints) {
        return new DefaultEndpointRegistry(this, endpoints);
    }

    @Override
    protected StreamCachingStrategy createStreamCachingStrategy() {
        return new DefaultStreamCachingStrategy();
    }

    @Override
    protected TransformerRegistry<TransformerKey> createTransformerRegistry() throws Exception {
        return new DefaultTransformerRegistry(this);
    }

    @Override
    protected ValidatorRegistry<ValidatorKey> createValidatorRegistry() throws Exception {
        return new DefaultValidatorRegistry(this);
    }

    @Override
    protected ReactiveExecutor createReactiveExecutor() {
        return new ReactiveExecutorResolver().resolve(this);
    }

    public AsyncProcessor createMulticast(Collection<Processor> processors, ExecutorService executor,
            boolean shutdownExecutorService) {
        return new MulticastProcessor(this, processors, (AggregationStrategy) null, true, executor, shutdownExecutorService,
                false, false, 0L, (Processor) null, false, false);
    }

    protected <T> T resolve(Class<T> clazz, String type, String name, CamelContext context) {
        T result = context.getRegistry().lookupByNameAndType(name, clazz);
        if (result instanceof CamelContextAware) {
            ((CamelContextAware) result).setCamelContext(context);
        }
        PropertiesComponent comp = getPropertiesComponent();
        if (comp instanceof org.apache.camel.component.properties.PropertiesComponent) {
            Properties props = ((org.apache.camel.component.properties.PropertiesComponent) comp).getInitialProperties();
            if (props != null) {
                String pfx = CamelRuntime.PFX_CAMEL + type + "." + name;
                log.debug("Binding {} {} with prefix {}", type, name, pfx);
                RuntimeSupport.bindProperties(this, props, result, pfx);
            }
        }
        return result;
    }

    @Override
    public void doInit() throws Exception {
        super.doInit();

        forceLazyInitialization();
    }

}
