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
package org.apache.camel.component.nats.graal;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;

/**
 * TODO: https://github.com/apache/camel-quarkus/issues/5233
 *
 * Remove this when net.i2p.crypto:eddsa >= 0.3.1 is released.
 */
@TargetClass(value = EdDSAEngine.class, onlyWith = IsEddsaCryptoAvailable.class)
public final class EdDSAEngineSubstitutions {
    @Alias
    private EdDSAKey key;
    @Alias
    private MessageDigest digest;

    @Alias
    public void reset() {
    }

    /**
     * Fix for JDK 17 to avoid importing JDK internal API sun.security.x509.X509Key.
     * Based on the original change: https://github.com/str4d/ed25519-java/commit/35c34a549cc933dc2d1d23ad4bfa88187fe77e7a
     */
    @Substitute
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        reset();
        if (publicKey instanceof EdDSAPublicKey) {
            key = (EdDSAPublicKey) publicKey;

            if (digest == null) {
                // Instantiate the digest from the key parameters
                try {
                    digest = MessageDigest.getInstance(key.getParams().getHashAlgorithm());
                } catch (NoSuchAlgorithmException e) {
                    throw new InvalidKeyException(
                            "cannot get required digest " + key.getParams().getHashAlgorithm() + " for private key.");
                }
            } else if (!key.getParams().getHashAlgorithm().equals(digest.getAlgorithm()))
                throw new InvalidKeyException("Key hash algorithm does not match chosen digest");
        } else if (publicKey.getFormat().equals("X.509")) {
            // X509Certificate will sometimes contain an X509Key rather than the EdDSAPublicKey itself; the contained
            // key is valid but needs to be instanced as an EdDSAPublicKey before it can be used.
            EdDSAPublicKey parsedPublicKey;
            try {
                parsedPublicKey = new EdDSAPublicKey(new X509EncodedKeySpec(publicKey.getEncoded()));
            } catch (InvalidKeySpecException ex) {
                throw new InvalidKeyException("cannot handle X.509 EdDSA public key: " + publicKey.getAlgorithm());
            }
            engineInitVerify(parsedPublicKey);
        } else {
            throw new InvalidKeyException("cannot identify EdDSA public key: " + publicKey.getClass());
        }
    }
}

final class IsEddsaCryptoAvailable implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("net.i2p.crypto.eddsa.EdDSAEngine", false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
