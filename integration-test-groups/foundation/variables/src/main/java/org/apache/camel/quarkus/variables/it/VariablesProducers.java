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
package org.apache.camel.quarkus.variables.it;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Named;
import org.apache.camel.Variable;
import org.apache.camel.spi.VariableRepository;
import org.apache.camel.support.service.ServiceSupport;

public class VariablesProducers {

    //repository is created as alternative to be disabled by default
    //QuarkusTestProfile is used to enable the bean for the appropriate tests.
    @Named("global-variable-repository")
    @Alternative
    @ApplicationScoped
    public static class MyGlobalRepo extends ServiceSupport implements VariableRepository {

        private Object value;

        @Override
        public String getId() {
            return "myGlobal";
        }

        @Override
        public Object getVariable(String name) {
            if (value != null) {
                return "!" + value + "!";
            }
            return null;
        }

        @Override
        public void setVariable(String name, Object value) {
            this.value = value;
        }

        @Override
        public Object removeVariable(String name) {
            return null;
        }
    }

    @Named("my-bean")
    @ApplicationScoped
    @RegisterForReflection(methods = true)
    public static class MyBean {
        public boolean matches(@Variable("location") String location) {
            return "Medford".equals(location);
        }
    }

}
