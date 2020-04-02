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
package org.apache.camel.quarkus.component.rest;

import java.util.Map;
import java.util.Set;

import org.apache.camel.Component;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.NoFactoryAvailableException;
import org.apache.camel.NoSuchBeanException;
import org.apache.camel.Producer;
import org.apache.camel.component.rest.RestComponent;
import org.apache.camel.component.rest.RestEndpoint;
import org.apache.camel.spi.FactoryFinder;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.spi.RestProducerFactory;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.support.RestProducerFactoryHelper.setupComponent;

public class QuarkusRestEndpoint extends RestEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(QuarkusRestEndpoint.class);

    public QuarkusRestEndpoint(String endpointUri, RestComponent component) {
        super(endpointUri, component);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Producer createProducer() throws Exception {
        if (ObjectHelper.isEmpty(getHost())) {
            // hostname must be provided
            throw new IllegalArgumentException("Hostname must be configured on either restConfiguration"
                    + " or in the rest endpoint uri as a query parameter with name host, eg rest:" + getMethod() + ":"
                    + getPath() + "?host=someserver");
        }

        RestProducerFactory apiDocFactory = null;
        RestProducerFactory factory = null;

        if (getApiDoc() != null) {
            LOG.debug("Discovering camel-openapi-java on classpath for using api-doc: {}", getApiDoc());
            // lookup on classpath using factory finder to automatic find it (just add camel-openapi-java to classpath etc)
            FactoryFinder finder = null;
            try {
                finder = getCamelContext().adapt(ExtendedCamelContext.class).getFactoryFinder(RESOURCE_PATH);
                apiDocFactory = finder.newInstance(DEFAULT_API_COMPONENT_NAME, RestProducerFactory.class).orElse(null);
                if (apiDocFactory == null) {
                    throw new NoFactoryAvailableException("Cannot find camel-openapi-java on classpath");
                }
                getParameters().put("apiDoc", getApiDoc());
            } catch (NoFactoryAvailableException e) {
                try {
                    LOG.debug("Discovering camel-swagger-java on classpath as fallback for using api-doc: {}", getApiDoc());
                    Object instance = finder.newInstance("swagger").get();
                    if (instance instanceof RestProducerFactory) {
                        // this factory from camel-swagger-java will facade the http component in use
                        apiDocFactory = (RestProducerFactory) instance;
                    }
                    getParameters().put("apiDoc", getApiDoc());
                } catch (Exception ex) {

                    throw new IllegalStateException(
                            "Cannot find camel-openapi-java neither camel-swagger-java on classpath to use with api-doc: "
                                    + getApiDoc());
                }

            }
        }

        String pname = getProducerComponentName();
        if (pname != null) {
            Object comp = getCamelContext().getRegistry().lookupByName(pname);
            if (comp instanceof RestProducerFactory) {
                factory = (RestProducerFactory) comp;
            } else {
                comp = setupComponent(getProducerComponentName(), getCamelContext(),
                        (Map<String, Object>) getParameters().get("component"));
                if (comp instanceof RestProducerFactory) {
                    factory = (RestProducerFactory) comp;
                }
            }

            if (factory == null) {
                if (comp != null) {
                    throw new IllegalArgumentException("Component " + pname + " is not a RestProducerFactory");
                } else {
                    throw new NoSuchBeanException(getProducerComponentName(), RestProducerFactory.class.getName());
                }
            }
        }

        // try all components
        if (factory == null) {
            for (String name : getCamelContext().getComponentNames()) {
                Component comp = setupComponent(name, getCamelContext(),
                        (Map<String, Object>) getParameters().get("component"));
                if (comp instanceof RestProducerFactory) {
                    factory = (RestProducerFactory) comp;
                    pname = name;
                    break;
                }
            }
        }

        // fallback to use consumer name as it may be producer capable too
        if (pname == null && getConsumerComponentName() != null) {
            String cname = getConsumerComponentName();
            Object comp = getCamelContext().getRegistry().lookupByName(cname);
            if (comp instanceof RestProducerFactory) {
                factory = (RestProducerFactory) comp;
                pname = cname;
            } else {
                comp = setupComponent(cname, getCamelContext(), (Map<String, Object>) getParameters().get("component"));
                if (comp instanceof RestProducerFactory) {
                    factory = (RestProducerFactory) comp;
                    pname = cname;
                }
            }
        }

        getParameters().put("producerComponentName", pname);

        // lookup in registry
        if (factory == null) {
            Set<RestProducerFactory> factories = getCamelContext().getRegistry().findByType(RestProducerFactory.class);
            if (factories != null && factories.size() == 1) {
                factory = factories.iterator().next();
            }
        }

        // no explicit factory found then try to see if we can find any of the default rest producer components
        // and there must only be exactly one so we safely can pick this one
        if (factory == null) {
            RestProducerFactory found = null;
            String foundName = null;
            for (String name : DEFAULT_REST_PRODUCER_COMPONENTS) {
                Object comp = setupComponent(name, getCamelContext(), (Map<String, Object>) getParameters().get("component"));
                if (comp instanceof RestProducerFactory) {
                    if (found == null) {
                        found = (RestProducerFactory) comp;
                        foundName = name;
                    } else {
                        throw new IllegalArgumentException(
                                "Multiple RestProducerFactory found on classpath. Configure explicit which component to use");
                    }
                }
            }
            if (found != null) {
                LOG.debug("Auto discovered {} as RestProducerFactory", foundName);
                factory = found;
            }
        }

        if (factory != null) {
            LOG.debug("Using RestProducerFactory: {}", factory);

            RestConfiguration config = CamelContextHelper.getRestConfiguration(getCamelContext(), pname);

            Producer producer;
            if (apiDocFactory != null) {
                // wrap the factory using the api doc factory which will use the factory
                getParameters().put("restProducerFactory", factory);
                producer = apiDocFactory.createProducer(getCamelContext(), getHost(), getMethod(), getPath(), getUriTemplate(),
                        getQueryParameters(), getConsumes(), getProduces(), config, getParameters());
            } else {
                producer = factory.createProducer(getCamelContext(), getHost(), getMethod(), getPath(), getUriTemplate(),
                        getQueryParameters(), getConsumes(), getProduces(), config, getParameters());
            }

            QuarkusRestProducer answer = new QuarkusRestProducer(this, producer, config);
            answer.setOutType(getOutType());
            answer.setType(getInType());
            answer.setBindingMode(getBindingMode());

            return answer;
        } else {
            throw new IllegalStateException("Cannot find RestProducerFactory in Registry or as a Component to use");
        }
    }
}
