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
package org.apache.camel.quarkus.component.milo.it;

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.component.milo.KeyStoreLoader;
import org.apache.camel.component.milo.server.MiloServerComponent;

import static org.apache.camel.quarkus.component.milo.it.MiloRoutes.SECURE_SERVER_ITEM_ID;

public class MiloProducers {
    public static final String CERT_BASE_DIR = "target/certs";
    public static final String CERT_KEYSTORE_URL = "file:%s/milo-keystore.p12".formatted(CERT_BASE_DIR);
    public static final String CERT_KEYSTORE_PASSWORD = "2s3cr3t";

    @ApplicationScoped
    @Identifier("milo-" + SECURE_SERVER_ITEM_ID)
    MiloServerComponent secureServerComponent() throws Exception {
        MiloServerComponent component = new MiloServerComponent();
        component.loadServerCertificate(loadDefaultTestKey());
        component.setDefaultCertificateValidator(CERT_BASE_DIR);
        return component;
    }

    static KeyStoreLoader.Result loadDefaultTestKey() throws Exception {
        KeyStoreLoader loader = new KeyStoreLoader();
        loader.setUrl(CERT_KEYSTORE_URL);
        loader.setKeyStorePassword(CERT_KEYSTORE_PASSWORD);
        loader.setKeyPassword(CERT_KEYSTORE_PASSWORD);
        return loader.load();
    }
}
