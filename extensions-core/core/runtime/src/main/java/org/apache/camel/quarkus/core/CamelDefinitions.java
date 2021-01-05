package org.apache.camel.quarkus.core;

import java.util.Arrays;
import java.util.List;

import org.apache.camel.NamedNode;
import org.apache.camel.builder.RouteBuilderLifecycleStrategy;
import org.apache.camel.builder.TransformerBuilder;
import org.apache.camel.builder.ValidatorBuilder;
import org.apache.camel.model.RouteTemplatesDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.rest.RestConfigurationDefinition;
import org.apache.camel.model.rest.RestsDefinition;

public final class CamelDefinitions {

    final RoutesDefinition routeCollection;
    final RestsDefinition restCollection;
    final List<TransformerBuilder> transformerBuilders;
    final List<ValidatorBuilder> validatorBuilders;
    final RestConfigurationDefinition restConfiguration;
    final RouteTemplatesDefinition routeTemplateCollection;
    final List<RouteBuilderLifecycleStrategy> lifecycleInterceptors;

    public CamelDefinitions(RoutesDefinition routeCollection, RestsDefinition restCollection,
            List<TransformerBuilder> transformerBuilders, List<ValidatorBuilder> validatorBuilders,
            RestConfigurationDefinition restConfiguration, RouteTemplatesDefinition routeTemplateCollection,
            List<RouteBuilderLifecycleStrategy> lifecycleInterceptors) {
        this.routeCollection = routeCollection;
        this.restCollection = restCollection;
        this.transformerBuilders = transformerBuilders;
        this.validatorBuilders = validatorBuilders;
        this.restConfiguration = restConfiguration;
        this.routeTemplateCollection = routeTemplateCollection;
        this.lifecycleInterceptors = lifecycleInterceptors;
    }

    public RoutesDefinition getRouteCollection() {
        return routeCollection;
    }

    public RestsDefinition getRestCollection() {
        return restCollection;
    }

    public List<TransformerBuilder> getTransformerBuilders() {
        return transformerBuilders;
    }

    public List<ValidatorBuilder> getValidatorBuilders() {
        return validatorBuilders;
    }

    public RestConfigurationDefinition getRestConfiguration() {
        return restConfiguration;
    }

    public RouteTemplatesDefinition getRouteTemplateCollection() {
        return routeTemplateCollection;
    }

    public List<RouteBuilderLifecycleStrategy> getLifecycleInterceptors() {
        return lifecycleInterceptors;
    }

    public List<NamedNode> getAllNamedNodes() {
        return Arrays.asList(new NamedNode[]{routeCollection, restCollection, routeTemplateCollection});
    }
}
