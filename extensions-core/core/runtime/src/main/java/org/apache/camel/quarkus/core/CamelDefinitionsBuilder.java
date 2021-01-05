package org.apache.camel.quarkus.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Expression;
import org.apache.camel.Ordered;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.ErrorHandlerBuilder;
import org.apache.camel.builder.NoErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.RouteBuilderLifecycleStrategy;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.builder.TransformerBuilder;
import org.apache.camel.builder.ValidatorBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.model.InterceptDefinition;
import org.apache.camel.model.InterceptFromDefinition;
import org.apache.camel.model.InterceptSendToEndpointDefinition;
import org.apache.camel.model.OnCompletionDefinition;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.model.RouteTemplatesDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.language.CSimpleExpression;
import org.apache.camel.model.language.DatasonnetExpression;
import org.apache.camel.model.language.ExchangePropertyExpression;
import org.apache.camel.model.language.HeaderExpression;
import org.apache.camel.model.language.JoorExpression;
import org.apache.camel.model.language.JsonPathExpression;
import org.apache.camel.model.language.XPathExpression;
import org.apache.camel.model.rest.RestConfigurationDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.model.rest.RestsDefinition;
import org.apache.camel.support.builder.Namespaces;

public abstract class CamelDefinitionsBuilder implements Ordered {
    public enum Phase {
        BUILD_TIME, RUNTIME
    };

    private RoutesDefinition routeCollection = new RoutesDefinition();
    private RestsDefinition restCollection = new RestsDefinition();
    private List<TransformerBuilder> transformerBuilders = new ArrayList<>();
    private List<ValidatorBuilder> validatorBuilders = new ArrayList<>();
    private RestConfigurationDefinition restConfiguration = new RestConfigurationDefinition();
    private List<RouteBuilderLifecycleStrategy> lifecycleInterceptors = new ArrayList<>();
    private RouteTemplatesDefinition routeTemplateCollection = new RouteTemplatesDefinition();

    private DefinitionsBuilderContext builderContext;

    public CamelDefinitionsBuilder() {
    }

    public final CamelDefinitions build(DefinitionsBuilderContext builderContext) {
        this.builderContext = builderContext;
        configure(builderContext);
        this.builderContext = null;
        final RoutesDefinition routes = this.routeCollection;
        this.routeCollection = null; // avoid leaking the mutable collection
        final RestsDefinition rests = this.restCollection;
        this.restCollection = null; // avoid leaking the mutable collection
        final List<TransformerBuilder> transformerBuilders = this.transformerBuilders;
        this.transformerBuilders = null;
        final List<ValidatorBuilder> validatorBuilders = this.validatorBuilders;
        this.validatorBuilders = null;
        final RestConfigurationDefinition restConfiguration = this.restConfiguration;
        this.restConfiguration = null;
        final RouteTemplatesDefinition routeTemplateCollection = this.routeTemplateCollection;
        this.routeTemplateCollection = null;
        final List<RouteBuilderLifecycleStrategy> lifecycleInterceptors = this.lifecycleInterceptors;
        this.lifecycleInterceptors = null;
        return new CamelDefinitions(routes, rests,
                transformerBuilders, validatorBuilders,
                restConfiguration, routeTemplateCollection,
                lifecycleInterceptors);
    }

    protected abstract void configure(DefinitionsBuilderContext builderContext);

    public RoutesBuilder asRoutesBuilder() {
        return new CamelDefinitionsRouteBuilder(this);
    }

    /**
     * Override this method to define ordering of {@link RouteBuilder} classes that are added to Camel from various
     * runtimes such as camel-main, camel-spring-boot. This allows end users to control the ordering if some routes must
     * be added and started before others.
     * <p/>
     * Use low numbers for higher priority. Normally the sorting will start from 0 and move upwards. So if you want to
     * be last then use {@link Integer#MAX_VALUE} or eg {@link #LOWEST}.
     */
    @Override
    public int getOrder() {
        return LOWEST;
    }

    @Override
    public String toString() {
        return routeCollection.toString();
    }

    /**
     * Configures the REST services
     *
     * @return the builder
     */
    public RestConfigurationDefinition restConfiguration() {
        if (restConfiguration == null) {
            restConfiguration = new RestConfigurationDefinition();
        }

        return restConfiguration;
    }

    /**
     * Creates a new route template
     *
     * @return the builder
     */
    public RouteTemplateDefinition routeTemplate(String id) {
        builderContext.context.ifPresent(routeTemplateCollection::setCamelContext);
        RouteTemplateDefinition answer = routeTemplateCollection.routeTemplate(id);
        configureRouteTemplate(answer);
        return answer;
    }

    /**
     * Creates a new REST service
     *
     * @return the builder
     */
    public RestDefinition rest() {
        builderContext.context.ifPresent(restCollection::setCamelContext);
        RestDefinition answer = restCollection.rest();
        configureRest(answer);
        return answer;
    }

    /**
     * Creates a new REST service
     *
     * @param path the base path
     * @return the builder
     */
    public RestDefinition rest(String path) {
        builderContext.context.ifPresent(restCollection::setCamelContext);
        RestDefinition answer = restCollection.rest(path);
        configureRest(answer);
        return answer;
    }

    /**
     * Create a new {@code TransformerBuilder}.
     *
     * @return the builder
     */
    public TransformerBuilder transformer() {
        TransformerBuilder tdb = new TransformerBuilder();
        transformerBuilders.add(tdb);
        return tdb;
    }

    /**
     * Create a new {@code ValidatorBuilder}.
     *
     * @return the builder
     */
    public ValidatorBuilder validator() {
        ValidatorBuilder vb = new ValidatorBuilder();
        validatorBuilders.add(vb);
        return vb;
    }

    /**
     * Creates a new route from the given URI input
     *
     * @param uri the from uri
     * @return the builder
     */
    public RouteDefinition from(String uri) {
        builderContext.context.ifPresent(routeCollection::setCamelContext);
        RouteDefinition answer = routeCollection.from(uri);
        configureRoute(answer);
        return answer;
    }

    /**
     * Creates a new route from the given URI input
     *
     * @param uri the String formatted from uri
     * @param args arguments for the string formatting of the uri
     * @return the builder
     */
    public RouteDefinition fromF(String uri, Object... args) {
        builderContext.context.ifPresent(routeCollection::setCamelContext);
        RouteDefinition answer = routeCollection.from(String.format(uri, args));
        configureRoute(answer);
        return answer;
    }

    /**
     * Creates a new route from the given endpoint
     *
     * @param endpoint the from endpoint
     * @return the builder
     */
    public RouteDefinition from(Endpoint endpoint) {
        builderContext.context.ifPresent(routeCollection::setCamelContext);
        RouteDefinition answer = routeCollection.from(endpoint);
        configureRoute(answer);
        return answer;
    }

    public RouteDefinition from(EndpointConsumerBuilder endpointDefinition) {
        builderContext.context.ifPresent(routeCollection::setCamelContext);
        RouteDefinition answer = routeCollection.from(endpointDefinition);
        configureRoute(answer);
        return answer;
    }

    /**
     * Adds a route for an interceptor that intercepts every processing step.
     *
     * @return the builder
     */
    public InterceptDefinition intercept() {
        if (!routeCollection.getRoutes().isEmpty()) {
            throw new IllegalArgumentException("intercept must be defined before any routes in the RouteBuilder");
        }
        builderContext.context.ifPresent(routeCollection::setCamelContext);
        return routeCollection.intercept();
    }

    /**
     * Adds a route for an interceptor that intercepts incoming messages on any inputs in this route
     *
     * @return the builder
     */
    public InterceptFromDefinition interceptFrom() {
        if (!routeCollection.getRoutes().isEmpty()) {
            throw new IllegalArgumentException("interceptFrom must be defined before any routes in the RouteBuilder");
        }
        builderContext.context.ifPresent(routeCollection::setCamelContext);
        return routeCollection.interceptFrom();
    }

    /**
     * Adds a route for an interceptor that intercepts incoming messages on the given endpoint.
     *
     * @param uri endpoint uri
     * @return the builder
     */
    public InterceptFromDefinition interceptFrom(String uri) {
        if (!routeCollection.getRoutes().isEmpty()) {
            throw new IllegalArgumentException("interceptFrom must be defined before any routes in the RouteBuilder");
        }
        builderContext.context.ifPresent(routeCollection::setCamelContext);
        return routeCollection.interceptFrom(uri);
    }

    /**
     * Applies a route for an interceptor if an exchange is send to the given endpoint
     *
     * @param uri endpoint uri
     * @return the builder
     */
    public InterceptSendToEndpointDefinition interceptSendToEndpoint(String uri) {
        if (!routeCollection.getRoutes().isEmpty()) {
            throw new IllegalArgumentException("interceptSendToEndpoint must be defined before any routes in the RouteBuilder");
        }
        builderContext.context.ifPresent(routeCollection::setCamelContext);
        return routeCollection.interceptSendToEndpoint(uri);
    }

    /**
     * <a href="http://camel.apache.org/exception-clause.html">Exception clause</a> for catching certain exceptions and
     * handling them.
     *
     * @param exception exception to catch
     * @return the builder
     */
    public OnExceptionDefinition onException(Class<? extends Throwable> exception) {
        // is only allowed at the top currently
        if (!routeCollection.getRoutes().isEmpty()) {
            throw new IllegalArgumentException("onException must be defined before any routes in the RouteBuilder");
        }
        builderContext.context.ifPresent(routeCollection::setCamelContext);
        return routeCollection.onException(exception);
    }

    /**
     * <a href="http://camel.apache.org/exception-clause.html">Exception clause</a> for catching certain exceptions and
     * handling them.
     *
     * @param exceptions list of exceptions to catch
     * @return the builder
     */
    public OnExceptionDefinition onException(Class<? extends Throwable>... exceptions) {
        OnExceptionDefinition last = null;
        for (Class<? extends Throwable> ex : exceptions) {
            last = last == null ? onException(ex) : last.onException(ex);
        }
        return last != null ? last : onException(Exception.class);
    }

    /**
     * <a href="http://camel.apache.org/oncompletion.html">On completion</a> callback for doing custom routing when the
     * {@link org.apache.camel.Exchange} is complete.
     *
     * @return the builder
     */
    public OnCompletionDefinition onCompletion() {
        // is only allowed at the top currently
        if (!routeCollection.getRoutes().isEmpty()) {
            throw new IllegalArgumentException("onCompletion must be defined before any routes in the RouteBuilder");
        }
        builderContext.context.ifPresent(routeCollection::setCamelContext);
        return routeCollection.onCompletion();
    }

    /**
     * Adds the given {@link RouteBuilderLifecycleStrategy} to be used.
     */
    public void addLifecycleInterceptor(RouteBuilderLifecycleStrategy interceptor) {
        lifecycleInterceptors.add(interceptor);
    }

    /**
     * Adds the given {@link RouteBuilderLifecycleStrategy}.
     */
    public void removeLifecycleInterceptor(RouteBuilderLifecycleStrategy interceptor) {
        lifecycleInterceptors.remove(interceptor);
    }

    protected void configureRest(RestDefinition rest) {
        // noop
    }

    protected void configureRoute(RouteDefinition route) {
        // noop
    }

    protected void configureRouteTemplate(RouteTemplateDefinition routeTemplate) {
        // noop
    }

    /**
     * Returns a value builder for the given header
     */
    public ValueBuilder header(String name) {
        Expression exp = new HeaderExpression(name);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a value builder for the given exchange property
     */
    public ValueBuilder exchangeProperty(String name) {
        Expression exp = new ExchangePropertyExpression(name);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a predicate and value builder for the inbound body on an exchange
     */
    public ValueBuilder body() {
        return Builder.body();
    }

    /**
     * Returns a predicate and value builder for the inbound message body as a specific type
     */
    public <T> ValueBuilder bodyAs(Class<T> type) {
        return Builder.bodyAs(type);
    }

    /**
     * Returns a value builder for the given system property
     */
    public ValueBuilder systemProperty(String name) {
        return Builder.systemProperty(name);
    }

    /**
     * Returns a value builder for the given system property
     */
    public ValueBuilder systemProperty(String name, String defaultValue) {
        return Builder.systemProperty(name, defaultValue);
    }

    /**
     * Returns a constant expression value builder
     */
    public ValueBuilder constant(Object value) {
        return Builder.constant(value);
    }

    /**
     * Returns a JOOR expression value builder
     */
    public ValueBuilder joor(String value) {
        JoorExpression exp = new JoorExpression(value);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a JOOR expression value builder
     */
    public ValueBuilder joor(String value, Class<?> resultType) {
        JoorExpression exp = new JoorExpression(value);
        exp.setResultType(resultType);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a JSonPath expression value builder
     *
     * @param value The JSonPath expression
     * @param resultType The result type that the JSonPath expression will return.
     */
    public ValueBuilder jsonpath(String value, Class<?> resultType) {
        JsonPathExpression exp = new JsonPathExpression(value);
        exp.setResultType(resultType);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a compiled simple expression value builder
     */
    public ValueBuilder csimple(String value) {
        CSimpleExpression exp = new CSimpleExpression(value);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a compiled simple expression value builder
     */
    public ValueBuilder csimple(String value, Class<?> resultType) {
        CSimpleExpression exp = new CSimpleExpression(value);
        exp.setResultType(resultType);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a Datasonnet expression value builder
     */
    public ValueBuilder datasonnet(String value) {
        DatasonnetExpression exp = new DatasonnetExpression(value);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a Datasonnet expression value builder
     */
    public ValueBuilder datasonnet(Expression value) {
        DatasonnetExpression exp = new DatasonnetExpression(value);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a Datasonnet expression value builder
     */
    public ValueBuilder datasonnet(String value, Class<?> resultType) {
        DatasonnetExpression exp = new DatasonnetExpression(value);
        exp.setResultType(resultType);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a Datasonnet expression value builder
     */
    public ValueBuilder datasonnet(Expression value, Class<?> resultType) {
        DatasonnetExpression exp = new DatasonnetExpression(value);
        exp.setResultType(resultType);
        return new ValueBuilder(exp);
    }

    /**
     * Returns a simple expression value builder
     */
    public SimpleBuilder simple(String value) {
        return SimpleBuilder.simple(value);
    }

    /**
     * Returns a simple expression value builder
     */
    public SimpleBuilder simple(String value, Class<?> resultType) {
        return SimpleBuilder.simple(value, resultType);
    }

    /**
     * Returns a simple expression value builder, using String.format style
     */
    public SimpleBuilder simpleF(String format, Object... values) {
        return SimpleBuilder.simpleF(format, values);
    }

    /**
     * Returns a simple expression value builder, using String.format style
     */
    public SimpleBuilder simpleF(String format, Class<?> resultType, Object... values) {
        return SimpleBuilder.simpleF(format, resultType, values);
    }

    /**
     * Returns a xpath expression value builder
     *
     * @param value the XPath expression
     * @return the builder
     */
    public ValueBuilder xpath(String value) {
        return xpath(value, null, null);
    }

    /**
     * Returns a xpath expression value builder
     *
     * @param value the XPath expression
     * @param resultType the result type that the XPath expression will return.
     * @return the builder
     */
    public ValueBuilder xpath(String value, Class<?> resultType) {
        return xpath(value, resultType, null);
    }

    /**
     * Returns a xpath expression value builder
     *
     * @param value the XPath expression
     * @param namespaces namespace mappings
     * @return the builder
     */
    public ValueBuilder xpath(String value, Namespaces namespaces) {
        return xpath(value, null, namespaces);
    }

    /**
     * Returns a xpath expression value builder
     *
     * @param value the XPath expression
     * @param resultType the result type that the XPath expression will return.
     * @param namespaces namespace mappings
     * @return the builder
     */
    public ValueBuilder xpath(final String value, Class<?> resultType, Namespaces namespaces) {
        final String newValue = builderContext.getContext()
                .map(ctx -> {
                    // the value may contain property placeholders as it may be used
                    // directly from Java DSL
                    try {
                        return ctx.resolvePropertyPlaceholders(value);
                    } catch (Exception e) {
                        throw RuntimeCamelException.wrapRuntimeCamelException(e);
                    }
                })
                .orElse(value);
        XPathExpression exp = new XPathExpression(newValue);
        exp.setResultType(resultType);
        if (namespaces != null) {
            exp.setNamespaces(namespaces.getNamespaces());
        }
        return new ValueBuilder(exp);
    }

    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a> value builder
     * <p/>
     * This method accepts dual parameters. Either an bean instance or a reference to a bean (String).
     *
     * @param beanOrBeanRef either an instanceof a bean or a reference to bean to lookup in the Registry
     * @return the builder
     */
    public ValueBuilder method(Object beanOrBeanRef) {
        return method(beanOrBeanRef, null);
    }

    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a> value builder
     * <p/>
     * This method accepts dual parameters. Either an bean instance or a reference to a bean (String).
     *
     * @param beanOrBeanRef either an instanceof a bean or a reference to bean to lookup in the Registry
     * @param method name of method to invoke
     * @return the builder
     */
    public ValueBuilder method(Object beanOrBeanRef, String method) {
        return Builder.bean(beanOrBeanRef, method);
    }

    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a> value builder
     *
     * @param beanType the Class of the bean which we want to invoke
     * @return the builder
     */
    public ValueBuilder method(Class<?> beanType) {
        return Builder.bean(beanType);
    }

    /**
     * Returns a <a href="http://camel.apache.org/bean-language.html">method call expression</a> value builder
     *
     * @param beanType the Class of the bean which we want to invoke
     * @param method name of method to invoke
     * @return the builder
     */
    public ValueBuilder method(Class<?> beanType, String method) {
        return Builder.bean(beanType, method);
    }

    /**
     * Returns an expression value builder that replaces all occurrences of the regular expression with the given
     * replacement
     */
    public ValueBuilder regexReplaceAll(Expression content, String regex, String replacement) {
        return Builder.regexReplaceAll(content, regex, replacement);
    }

    /**
     * Returns an expression value builder that replaces all occurrences of the regular expression with the given
     * replacement
     */
    public ValueBuilder regexReplaceAll(Expression content, String regex, Expression replacement) {
        return Builder.regexReplaceAll(content, regex, replacement);
    }

    /**
     * Returns a exception expression value builder
     */
    public ValueBuilder exceptionMessage() {
        return Builder.exceptionMessage();
    }

    /**
     * Creates a default <a href="http://camel.apache.org/error-handler.html">error handler</a>.
     *
     * @return the builder
     */
    public DefaultErrorHandlerBuilder defaultErrorHandler() {
        return new DefaultErrorHandlerBuilder();
    }

    /**
     * Creates a disabled <a href="http://camel.apache.org/error-handler.html">error handler</a> for removing the
     * default error handler
     *
     * @return the builder
     */
    public NoErrorHandlerBuilder noErrorHandler() {
        return new NoErrorHandlerBuilder();
    }

    protected ErrorHandlerBuilder createErrorHandlerBuilder() {
        return new DefaultErrorHandlerBuilder();
    }

    public static final class CamelDefinitionsRouteBuilder extends RouteBuilder {
        private final CamelDefinitionsBuilder definitionsBuilder;

        public CamelDefinitionsRouteBuilder(CamelDefinitionsBuilder definitionsBuilder) {
            this.definitionsBuilder = definitionsBuilder;
        }

        @Override
        public void configure() throws Exception {
            final DefinitionsBuilderContext builderContext = DefinitionsBuilderContext.runtime(getContext());
            CamelDefinitions definitions = definitionsBuilder.build(builderContext);
            getRouteCollection().setRoutes(definitions.getRouteCollection().getRoutes());
            getRestCollection().setRests(definitions.getRestCollection().getRests());
        }

    }

    public static class DefinitionsBuilderContext {

        public static DefinitionsBuilderContext buildTime() {
            return new DefinitionsBuilderContext(Phase.BUILD_TIME, Optional.empty());
        }

        public static DefinitionsBuilderContext runtime(CamelContext context) {
            return new DefinitionsBuilderContext(Phase.RUNTIME, Optional.of(context));
        }

        private final Phase phase;
        private final Optional<CamelContext> context;

        private DefinitionsBuilderContext(Phase phase, Optional<CamelContext> context) {
            this.phase = phase;
            this.context = context;
        }

        /**
         * @return the phase in which the {@link CamelDefinitionsBuilder#configure(DefinitionsBuilderContext)} is called.
         */
        public Phase getPhase() {
            return phase;
        }

        /**
         * @return an empty {@link Optional} in {@link Phase#BUILD_TIME} and a non-empty {@link Optional} otherwise
         */
        public Optional<CamelContext> getContext() {
            return context;
        }
    }

}
