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
package org.apache.camel.quarkus.component.pqc.it;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.bouncycastle.pqc.crypto.lms.LMOtsParameters;
import org.bouncycastle.pqc.crypto.lms.LMSigParameters;
import org.bouncycastle.pqc.jcajce.spec.*;

@Singleton
public class PqcProducers {

    //not-static to avoid buildtime initialization
    private final SecureRandom secureRandom = new SecureRandom();

    @Produces
    @Singleton
    @Named("dilithiumKeyPair")
    KeyPair dilithiumKeyPair() throws Exception {
        return generateKeyPair("Dilithium", DilithiumParameterSpec.dilithium2);
    }

    @Produces
    @Singleton
    @Named("falconKeyPair")
    public KeyPair falconKeyPair() throws Exception {
        return generateKeyPair("Falcon", FalconParameterSpec.falcon_512);
    }

    @Produces
    @Singleton
    @Named("sphincsKeyPair")
    public KeyPair sphincsKeyPair() throws Exception {
        return generateKeyPair("SPHINCSPlus", SPHINCSPlusParameterSpec.sha2_128f);
    }

    @Produces
    @Singleton
    @Named("lmsKeyPair")
    public KeyPair lmsKeyPair() throws Exception {
        return generateKeyPair("LMS", new LMSParameterSpec(LMSigParameters.lms_sha256_n32_h5, LMOtsParameters.sha256_n32_w4));
    }

    @Produces
    @Singleton
    @Named("xmssKeyPair")
    public KeyPair xmssKeyPair() throws Exception {
        return generateKeyPair("XMSS", XMSSParameterSpec.SHA2_10_256);
    }

    @Produces
    @Singleton
    @Named("kyberKeyPair")
    public KeyPair kyberKeyPair() throws Exception {
        return generateKeyPair("Kyber", KyberParameterSpec.kyber512);
    }

    @Produces
    @Singleton
    @Named("kyberWrongKeyPair") //second keypair for negative scenario
    public KeyPair kyberWrongKeyPair() throws Exception {
        return generateKeyPair("Kyber", KyberParameterSpec.kyber512);
    }

    private KeyPair generateKeyPair(String algorithm, AlgorithmParameterSpec spec) throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(algorithm, "BCPQC");
        gen.initialize(spec, secureRandom);
        return gen.generateKeyPair();
    }
}
