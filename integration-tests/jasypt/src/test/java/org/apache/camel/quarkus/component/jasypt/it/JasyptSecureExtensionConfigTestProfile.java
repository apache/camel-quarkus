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
package org.apache.camel.quarkus.component.jasypt.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;

@QuarkusTest
public class JasyptSecureExtensionConfigTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.datasource.devservices.enabled", "true",
                // camel
                "quarkus.datasource.devservices.username",
                "ENC(ehEx3VxgoFVuReSFsgyUrw==)",
                "quarkus.datasource.username", "${camel-jasypt::${quarkus.datasource.devservices.username}}",
                // c4m31s3cr3t
                "quarkus.datasource.devservices.password",
                "ENC(ySRd4rq2bAuKEDe7wvcS37xERpu5+DgK)",
                "quarkus.datasource.password", "${camel-jasypt::${quarkus.datasource.devservices.password}}",
                "camel.component.direct.timeout", "ENC(FGlWjTf42zBT4vCRCztncA==)");
    }
}
