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
package org.apache.camel.quarkus.component.bean;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.quarkus.component.bean.model.Employee;
import org.apache.camel.spi.Registry;

/**
 * A bean referenced from a route (and from nowhere else) by name.
 */
@ApplicationScoped
@Named("parametersBean")
@RegisterForReflection // Let Quarkus register this class for reflection during the native build
public class ParametersBean {

    public String parameterTypes(String employeeAsString) {
        return "employeeAsString: " + employeeAsString;
    }

    public String parameterTypes(Employee employee) {
        return "Employee: " + employee.getFirstName();
    }

    public String parameterBindingAnnotations(
            @Header("parameterBinding.greeting") String greeting,
            @Body String body) {
        return greeting + " " + body + " from parameterBindingAnnotations";
    }

    public String parameterLiterals(String body, boolean bool) {
        return "Hello " + body + " from parameterLiterals(*, " + bool + ")";
    }

    public String multiArgMethod(String body, Exchange exchange, Registry registry) {
        return "Hello " + body + " from multiArgMethod: " + (exchange != null ? "got exchange" : "") + " "
                + (registry != null ? "got registry" : "");
    }

}
