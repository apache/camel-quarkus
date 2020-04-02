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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.Producer;
import org.apache.camel.component.rest.RestProducer;
import org.apache.camel.component.rest.RestProducerBindingProcessor;
import org.apache.camel.spi.BeanIntrospection;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.support.AsyncProcessorConverterHelper;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.support.service.ServiceHelper;

public class QuarkusRestProducer extends RestProducer {
    private final RestConfiguration configuration;
    private final CamelContext camelContext;

    // the producer of the Camel component that is used as the HTTP client to call the REST service
    private AsyncProcessor producer;
    // if binding is enabled then this processor should be used to wrap the call with binding before/after
    private AsyncProcessor binding;

    public QuarkusRestProducer(Endpoint endpoint, Producer producer, RestConfiguration configuration) {
        super(endpoint, producer, configuration);

        this.configuration = configuration;
        this.camelContext = endpoint.getCamelContext();
        this.producer = AsyncProcessorConverterHelper.convert(producer);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        // create binding processor (returns null if binding is not in use)
        binding = createBindingProcessor();

        ServiceHelper.startService(binding, producer);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        ServiceHelper.stopService(producer, binding);
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        try {
            prepareExchange(exchange);
            if (binding != null) {
                return binding.process(exchange, callback);
            } else {
                // no binding in use call the producer directly
                return producer.process(exchange, callback);
            }
        } catch (Throwable e) {
            exchange.setException(e);
            callback.done(true);
            return true;
        }
    }

    @Override
    protected AsyncProcessor createBindingProcessor() throws Exception {
        // these options can be overridden per endpoint
        String mode = configuration.getBindingMode().name();
        if (getBindingMode() != null) {
            mode = getBindingMode().name();
        }
        boolean skip = configuration.isSkipBindingOnErrorCode();
        if (getSkipBindingOnErrorCode() != null) {
            skip = getSkipBindingOnErrorCode();
        }

        if (mode == null || "off".equals(mode)) {
            // binding mode is off
            return null;
        }

        // setup json data format
        String name = configuration.getJsonDataFormat();
        if (name != null) {
            // must only be a name, not refer to an existing instance
            Object instance = camelContext.getRegistry().lookupByName(name);
            if (instance != null) {
                throw new IllegalArgumentException(
                        "JsonDataFormat name: " + name + " must not be an existing bean instance from the registry");
            }
        } else {
            name = "json-jackson";
        }
        // this will create a new instance as the name was not already pre-created
        DataFormat json = camelContext.resolveDataFormat(name);
        DataFormat outJson = camelContext.resolveDataFormat(name);

        // is json binding required?
        if (mode.contains("json") && json == null) {
            throw new IllegalArgumentException("JSon DataFormat " + name + " not found.");
        }

        BeanIntrospection beanIntrospection = camelContext.adapt(ExtendedCamelContext.class).getBeanIntrospection();
        if (json != null) {
            Class<?> clazz = null;
            if (getType() != null) {
                String typeName = getType().endsWith("[]") ? getType().substring(0, getType().length() - 2) : getType();
                clazz = camelContext.getClassResolver().resolveMandatoryClass(typeName);
            }
            if (clazz != null) {
                beanIntrospection.setProperty(camelContext, json, "unmarshalType", clazz);
                beanIntrospection.setProperty(camelContext, json, "useList", getType().endsWith("[]"));
            }
            setAdditionalConfiguration(configuration, camelContext, json, "json.in.");

            Class<?> outClazz = null;
            if (getOutType() != null) {
                String typeName = getOutType().endsWith("[]") ? getOutType().substring(0, getOutType().length() - 2)
                        : getOutType();
                outClazz = camelContext.getClassResolver().resolveMandatoryClass(typeName);
            }
            if (outClazz != null) {
                beanIntrospection.setProperty(camelContext, outJson, "unmarshalType", outClazz);
                beanIntrospection.setProperty(camelContext, outJson, "useList", getOutType().endsWith("[]"));
            }
            setAdditionalConfiguration(configuration, camelContext, outJson, "json.out.");
        }

        // setup xml data format
        name = configuration.getXmlDataFormat();
        if (name != null) {
            // must only be a name, not refer to an existing instance
            Object instance = camelContext.getRegistry().lookupByName(name);
            if (instance != null) {
                throw new IllegalArgumentException(
                        "XmlDataFormat name: " + name + " must not be an existing bean instance from the registry");
            }
        } else {
            name = "jaxb";
        }
        // this will create a new instance as the name was not already pre-created
        DataFormat jaxb = camelContext.resolveDataFormat(name);
        DataFormat outJaxb = camelContext.resolveDataFormat(name);

        // is xml binding required?
        if (mode.contains("xml") && jaxb == null) {
            throw new IllegalArgumentException("XML DataFormat " + name + " not found.");
        }

        if (jaxb != null) {
            throw new IllegalArgumentException(
                    "Unsupported XmlDataFormat name: " + name + ": Please add a dependency to camel-quarkus-xml-jaxb");
        }

        return new RestProducerBindingProcessor(producer, camelContext, json, jaxb, outJson, outJaxb, mode, skip, getOutType());
    }

    private void setAdditionalConfiguration(RestConfiguration config, CamelContext context,
            DataFormat dataFormat, String prefix) throws Exception {
        if (config.getDataFormatProperties() != null && !config.getDataFormatProperties().isEmpty()) {
            // must use a copy as otherwise the options gets removed during introspection setProperties
            Map<String, Object> copy = new HashMap<>();

            // filter keys on prefix
            // - either its a known prefix and must match the prefix parameter
            // - or its a common configuration that we should always use
            for (Map.Entry<String, Object> entry : config.getDataFormatProperties().entrySet()) {
                String key = entry.getKey();
                String copyKey;
                boolean known = isKeyKnownPrefix(key);
                if (known) {
                    // remove the prefix from the key to use
                    copyKey = key.substring(prefix.length());
                } else {
                    // use the key as is
                    copyKey = key;
                }
                if (!known || key.startsWith(prefix)) {
                    copy.put(copyKey, entry.getValue());
                }
            }

            // set reference properties first as they use # syntax that fools the regular properties setter
            PropertyBindingSupport.bindProperties(context, dataFormat, copy);
        }
    }

    private boolean isKeyKnownPrefix(String key) {
        return key.startsWith("json.in.") || key.startsWith("json.out.") || key.startsWith("xml.in.")
                || key.startsWith("xml.out.");
    }
}
