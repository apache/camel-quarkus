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
package org.apache.camel.quarkus.component.cxf.soap.rest.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.jboss.eap.quickstarts.wscalculator.calculator.AddOperands;
import org.jboss.eap.quickstarts.wscalculator.calculator.Operands;
import org.jboss.eap.quickstarts.wscalculator.calculator.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = CxfRestTestResource.class)
class CxfSoapRestTest {

    @Test
    public void headersPropagation() throws Exception {
        int firstOperand = 5;
        int secondOperand = 23;
        AddOperands addOperands = new AddOperands();
        Operands operands = new Operands();
        operands.setA(firstOperand);
        operands.setB(secondOperand);
        addOperands.setArg0(operands);
        String payload = new ObjectMapper().writeValueAsString(addOperands);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/cxf-soap-rest/post")
                .andReturn();
        Assertions.assertSame(firstOperand + secondOperand, response.as(Result.class).getResult(),
                "The expected sum is incorrect.");
    }
}
