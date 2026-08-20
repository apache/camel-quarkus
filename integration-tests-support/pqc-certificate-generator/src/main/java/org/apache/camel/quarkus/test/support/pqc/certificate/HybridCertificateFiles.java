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
package org.apache.camel.quarkus.test.support.pqc.certificate;

import java.nio.file.Path;

/**
 * Value object holding paths to generated certificate files.
 */
class HybridCertificateFiles {
    private final Path certificatePem;
    private final Path primaryKeyPem;
    private final Path pqcKeyPem;
    private final Path truststore;
    private final Path keystore;

    public HybridCertificateFiles(
            Path certificatePem,
            Path primaryKeyPem,
            Path pqcKeyPem,
            Path truststore,
            Path keystore) {
        this.certificatePem = certificatePem;
        this.primaryKeyPem = primaryKeyPem;
        this.pqcKeyPem = pqcKeyPem;
        this.truststore = truststore;
        this.keystore = keystore;
    }

    public Path getCertificatePem() {
        return certificatePem;
    }

    public Path getPrimaryKeyPem() {
        return primaryKeyPem;
    }

    public Path getPqcKeyPem() {
        return pqcKeyPem;
    }

    public Path getTruststore() {
        return truststore;
    }

    public Path getKeystore() {
        return keystore;
    }
}
