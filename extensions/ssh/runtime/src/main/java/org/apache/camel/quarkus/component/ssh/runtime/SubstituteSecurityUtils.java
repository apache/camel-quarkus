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

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.security.SecurityUtils;

/**
 * We're substituting those offending methods that would require the presence of
 * net.i2p.crypto:eddsa library which is not supported by Camel SSH component
 */
@TargetClass(SecurityUtils.class)
final class SubstituteSecurityUtils {

    @Substitute
    public static boolean compareEDDSAPPublicKeys(PublicKey k1, PublicKey k2) {
        throw new UnsupportedOperationException("EdDSA Signer not available");
    }

    @Substitute
    public static boolean compareEDDSAPrivateKeys(PrivateKey k1, PrivateKey k2) {
        throw new UnsupportedOperationException("EdDSA Signer not available");
    }

    @Substitute
    public static PublicKey generateEDDSAPublicKey(String keyType, byte[] seed) throws GeneralSecurityException {
        throw new UnsupportedOperationException("EdDSA Signer not available");
    }

    @Substitute
    public static org.apache.sshd.common.signature.Signature getEDDSASigner() {
        throw new UnsupportedOperationException("EdDSA Signer not available");
    }

    @Substitute
    public static <B extends Buffer> B putRawEDDSAPublicKey(B buffer, PublicKey key) {
        throw new UnsupportedOperationException("EdDSA Signer not available");
    }

    @Substitute
    public static PublicKey recoverEDDSAPublicKey(PrivateKey key) throws GeneralSecurityException {
        throw new UnsupportedOperationException("EdDSA Signer not available");
    }

}
