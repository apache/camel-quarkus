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
package org.apache.camel.quarkus.component.groovy.runtime;

import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.Script;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RouteTemplateContext;

/**
 * A type of {@link Script} that is specific to Camel with fixed fields to be able to compile the script using
 * static compilation.
 */
public abstract class GroovyStaticScript extends Script {

    protected Map<String, Object> headers;
    protected Object body;
    protected Message in;
    protected Message request;
    protected Exchange exchange;
    protected Message out;
    protected Message response;
    protected CamelContext camelContext;
    protected RouteTemplateContext rtc;

    @SuppressWarnings("unchecked")
    @Override
    public void setBinding(Binding binding) {
        super.setBinding(binding);
        this.headers = (Map<String, Object>) getPropertyValue("headers");
        this.body = getPropertyValue("body");
        this.in = (Message) getPropertyValue("in");
        this.out = (Message) getPropertyValue("out");
        this.request = (Message) getPropertyValue("request");
        this.response = (Message) getPropertyValue("response");
        this.exchange = (Exchange) getPropertyValue("exchange");
        this.camelContext = (CamelContext) getPropertyValue("camelContext");
        this.rtc = (RouteTemplateContext) getPropertyValue("rtc");
    }

    private Object getPropertyValue(String name) {
        Binding binding = getBinding();
        return binding.hasVariable(name) ? binding.getProperty(name) : null;
    }
}
