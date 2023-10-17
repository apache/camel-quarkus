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

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.engine.DefaultInjector;
import org.apache.camel.spi.CamelBeanPostProcessor;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.support.ObjectHelper;
import org.apache.camel.support.PluginHelper;

/**
 * An {@link org.apache.camel.spi.Injector} that can delegate TypeConverter instance resolution to Arc.
 */
public class FastTypeConverterInjector extends DefaultInjector {
    private final CamelContext context;
    private final CamelBeanPostProcessor postProcessor;

    public FastTypeConverterInjector(CamelContext context) {
        super(context);
        this.context = context;
        this.postProcessor = PluginHelper.getBeanPostProcessor(context);
    }

    @Override
    public <T> T newInstance(Class<T> type, boolean postProcessBean) {
        // Try TypeConverter discovery from the Camel registry / Arc container
        T typeConverter = CamelContextHelper.findSingleByType(context, type);
        if (typeConverter == null) {
            // Fallback to the default injector behavior
            typeConverter = ObjectHelper.newInstance(type);
        }

        CamelContextAware.trySetCamelContext(typeConverter, context);
        if (postProcessBean) {
            try {
                postProcessor.postProcessBeforeInitialization(typeConverter, typeConverter.getClass().getName());
                postProcessor.postProcessAfterInitialization(typeConverter, typeConverter.getClass().getName());
            } catch (Exception e) {
                throw new RuntimeCamelException("Error during post processing of bean: " + typeConverter, e);
            }
        }
        return typeConverter;
    }
}
