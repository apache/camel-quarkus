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
package org.apache.camel.quarkus.component.ssh.runtime;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * We're substituting those offending methods that would require the presence of
 * net.i2p.crypto:eddsa library which is not supported by Camel SSH component.
 * This substitution only applies when EdDSA classes are present on the classpath.
 */
@TargetClass(className = "net.i2p.crypto.eddsa.EdDSAEngine", onlyWith = SubstituteEdDSAEngine.EdDSAPresent.class)
final class SubstituteEdDSAEngine {

    static class EdDSAPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("net.i2p.crypto.eddsa.EdDSAEngine", false,
                        Thread.currentThread().getContextClassLoader());
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }

    @Alias
    private MessageDigest digest;

    @Alias
    private Object key; // net.i2p.crypto.eddsa.EdDSAKey

    @Alias
    private void reset() {
    }

    @Substitute
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        // This method body references EdDSA classes that only exist when net.i2p.crypto:eddsa is present.
        // GraalVM will only compile this substitution when EdDSAPresent condition is true.
        reset();
        Class<?> eddsaPublicKeyClass;
        try {
            eddsaPublicKeyClass = Class.forName("net.i2p.crypto.eddsa.EdDSAPublicKey");
        } catch (ClassNotFoundException e) {
            throw new InvalidKeyException("EdDSAPublicKey class not found", e);
        }

        if (eddsaPublicKeyClass.isInstance(publicKey)) {
            key = publicKey;

            if (digest == null) {
                // Instantiate the digest from the key parameters - using reflection
                try {
                    Object params = publicKey.getClass().getMethod("getParams").invoke(publicKey);
                    String hashAlg = (String) params.getClass().getMethod("getHashAlgorithm").invoke(params);
                    digest = MessageDigest.getInstance(hashAlg);
                } catch (Exception e) {
                    throw new InvalidKeyException("cannot get required digest for private key", e);
                }
            } else {
                try {
                    Object params = publicKey.getClass().getMethod("getParams").invoke(publicKey);
                    String hashAlg = (String) params.getClass().getMethod("getHashAlgorithm").invoke(params);
                    if (!hashAlg.equals(digest.getAlgorithm())) {
                        throw new InvalidKeyException("Key hash algorithm does not match chosen digest");
                    }
                } catch (Exception e) {
                    throw new InvalidKeyException("cannot verify hash algorithm", e);
                }
            }
        } //following line differs from the original method
        else if (publicKey.getFormat().equals("X.509")) {
            // X509Certificate will sometimes contain an X509Key rather than the EdDSAPublicKey itself; the contained
            // key is valid but needs to be instanced as an EdDSAPublicKey before it can be used.
            try {
                Object parsedPublicKey = eddsaPublicKeyClass
                        .getConstructor(X509EncodedKeySpec.class)
                        .newInstance(new X509EncodedKeySpec(publicKey.getEncoded()));
                engineInitVerify((PublicKey) parsedPublicKey);
            } catch (Exception ex) {
                throw new InvalidKeyException("cannot handle X.509 EdDSA public key: " + publicKey.getAlgorithm(), ex);
            }
        } else {
            throw new InvalidKeyException("cannot identify EdDSA public key: " + publicKey.getClass());
        }
    }
}
