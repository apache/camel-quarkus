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

/**
 * Classical/primary algorithms for hybrid certificates.
 * Used for TLS handshake compatibility.
 */
public enum PrimaryAlgorithm {
    RSA_2048("RSA", 2048, "SHA256withRSA"),
    RSA_4096("RSA", 4096, "SHA256withRSA"),
    ECDSA_256("EC", 256, "SHA256withECDSA"),
    ECDSA_384("EC", 384, "SHA384withECDSA"),
    NONE(null, 0, null); // For PQC_ONLY mode

    private final String algorithmName;
    private final int keySize;
    private final String signatureAlgorithm;

    PrimaryAlgorithm(String algorithmName, int keySize, String signatureAlgorithm) {
        this.algorithmName = algorithmName;
        this.keySize = keySize;
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public int getKeySize() {
        return keySize;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }
}
