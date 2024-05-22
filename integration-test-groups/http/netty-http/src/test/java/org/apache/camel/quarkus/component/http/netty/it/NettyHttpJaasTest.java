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
package org.apache.camel.quarkus.component.http.netty.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import me.escoffier.certs.Format;
import me.escoffier.certs.junit5.Certificate;
import org.apache.camel.quarkus.component.http.common.HttpTestResource;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@TestCertificates(certificates = {
        @Certificate(name = HttpTestResource.KEYSTORE_NAME, formats = {
                Format.PKCS12 }, password = HttpTestResource.KEYSTORE_PASSWORD) })
@QuarkusTest
@QuarkusTestResource(NettyHttpJaasTestResource.class)
public class NettyHttpJaasTest {
    @ParameterizedTest
    @CsvSource({
            "admin,wrongjaaspass,401",
            "admin,adminjaaspass,200"
    })
    public void testJaas(String user, String password, int responseCode) {
        RestAssured
                .given()
                .queryParam("test-port", ConfigProvider.getConfig().getValue("camel.netty-http.port", Integer.class))
                .when()
                .get("/test/client/netty-http/jaas/{user}/{password}", user, password)
                .then()
                .statusCode(responseCode);
    }
}
