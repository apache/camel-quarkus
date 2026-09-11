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
 * Supported Post-Quantum Cryptography algorithms for keypair generation.
 */
public enum PQCAlgorithm {
    // Signature algorithms (using NIST standardized names)
    MLDSA44("ML-DSA-44"),
    MLDSA65("ML-DSA-65"),
    MLDSA87("ML-DSA-87"),
    FALCON512("Falcon-512"),
    FALCON1024("Falcon-1024"),
    SPHINCSPLUS("SPHINCSPlus"),
    LMS("LMS"),
    XMSS("XMSS"),

    // Key encapsulation mechanisms
    KYBER512("Kyber512"),
    KYBER768("Kyber768"),
    KYBER1024("Kyber1024");

    private final String algorithmName;

    PQCAlgorithm(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }
}
