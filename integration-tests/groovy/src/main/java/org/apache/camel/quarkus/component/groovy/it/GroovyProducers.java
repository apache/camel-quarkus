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
package org.apache.camel.quarkus.component.groovy.it;

import java.util.Collections;
import java.util.Map;

import groovy.lang.GroovyShell;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.Exchange;
import org.apache.camel.language.groovy.GroovyShellFactory;
import org.codehaus.groovy.control.CompilerConfiguration;

@ApplicationScoped
public class GroovyProducers {

    @Named("customShell")
    public GroovyShellFactory createCustomShell() {
        return new GroovyShellFactory() {
            public GroovyShell createGroovyShell(Exchange exchange) {
                return new GroovyShell(new CompilerConfiguration());
            }

            public Map<String, Object> getVariables(Exchange exchange) {
                return Collections.singletonMap("hello", (Object) "Ahoj");
            }
        };
    }
}
