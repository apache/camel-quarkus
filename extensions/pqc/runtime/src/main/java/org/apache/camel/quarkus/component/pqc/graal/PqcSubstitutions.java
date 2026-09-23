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
package org.apache.camel.quarkus.component.pqc.graal;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.AlgorithmParameterSpec;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.component.pqc.PQCKeyEncapsulationAlgorithms;
import org.apache.camel.component.pqc.PQCParameterSpecResolver;
import org.apache.camel.component.pqc.crypto.PQCDefaultPicnicMaterial;
import org.apache.camel.component.pqc.crypto.kem.PQCDefaultCMCEMaterial;
import org.apache.camel.component.pqc.crypto.kem.PQCDefaultFRODOMaterial;
import org.apache.camel.util.SecureRandomHelper;
import org.bouncycastle.jcajce.spec.CMCEParameterSpec;
import org.bouncycastle.jcajce.spec.MLDSAParameterSpec;
import org.bouncycastle.jcajce.spec.MLKEMParameterSpec;
import org.bouncycastle.jcajce.spec.SLHDSAParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.BIKEParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.FalconParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.HQCParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.NTRULPRimeParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.NTRUParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.SABERParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.SNTRUPrimeParameterSpec;

/**
 * Removes references to BouncyCastle parameter specs that no longer exist in the BouncyCastle version managed by
 * Quarkus. Algorithms without a replacement spec are not supported in native mode.
 */
// TODO: Remove this - https://github.com/apache/camel-quarkus/issues/9231
final class PqcSubstitutions {
}

@TargetClass(PQCParameterSpecResolver.class)
final class SubstitutePQCParameterSpecResolver {
    @Substitute
    private static AlgorithmParameterSpec doResolve(String algorithm, String parameterSpec) {
        switch (algorithm) {
        // Signature algorithms
        case "MLDSA":
            return MLDSAParameterSpec.fromName(parameterSpec);
        case "SLHDSA":
            return SLHDSAParameterSpec.fromName(parameterSpec);
        case "FALCON":
            return FalconParameterSpec.fromName(parameterSpec);
        // Key encapsulation algorithms
        case "MLKEM":
            return MLKEMParameterSpec.fromName(parameterSpec);
        case "NTRU":
            return NTRUParameterSpec.fromName(parameterSpec);
        case "NTRULPRime":
            return NTRULPRimeParameterSpec.fromName(parameterSpec);
        case "SNTRUPrime":
            return SNTRUPrimeParameterSpec.fromName(parameterSpec);
        case "BIKE":
            return BIKEParameterSpec.fromName(parameterSpec);
        case "HQC":
            return HQCParameterSpec.fromName(parameterSpec);
        case "CMCE":
            return CMCEParameterSpec.fromName(parameterSpec);
        case "SABER":
            return SABERParameterSpec.fromName(parameterSpec);
        case "DILITHIUM":
        case "SPHINCSPLUS":
        case "PICNIC":
        case "KYBER":
        case "FRODO":
            throw new UnsupportedOperationException(
                    "The parameterSpec option is not supported for algorithm " + algorithm + " in native mode");
        default:
            throw new IllegalStateException("Unsupported algorithm: " + algorithm);
        }
    }
}

@TargetClass(PQCDefaultPicnicMaterial.class)
final class SubstitutePQCDefaultPicnicMaterial {
    @Substitute
    protected static KeyPairGenerator prepareKeyPair() {
        throw new UnsupportedOperationException("The PICNIC algorithm is not supported in native mode");
    }
}

@TargetClass(PQCDefaultFRODOMaterial.class)
final class SubstitutePQCDefaultFRODOMaterial {
    @Substitute
    protected static KeyPairGenerator prepareKeyPair() {
        throw new UnsupportedOperationException("The FRODO algorithm is not supported in native mode");
    }
}

@TargetClass(PQCDefaultCMCEMaterial.class)
final class SubstitutePQCDefaultCMCEMaterial {
    @Substitute
    protected static KeyPairGenerator prepareKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(PQCKeyEncapsulationAlgorithms.CMCE.getAlgorithm(),
                PQCKeyEncapsulationAlgorithms.CMCE.getBcProvider());
        kpg.initialize(CMCEParameterSpec.mceliece8192128f, SecureRandomHelper.getSecureRandom());
        return kpg;
    }
}
