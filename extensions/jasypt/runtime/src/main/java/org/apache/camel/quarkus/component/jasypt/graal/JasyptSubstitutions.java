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
package org.apache.camel.quarkus.component.jasypt.graal;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Inject;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.normalization.Normalizer;
import org.jasypt.salt.RandomSaltGenerator;

import static java.text.Normalizer.normalize;
import static java.text.Normalizer.Form.NFC;

public class JasyptSubstitutions {
    @TargetClass(Normalizer.class)
    static final class NormalizerSubstitutions {
        @Substitute
        public static char[] normalizeToNfc(final char[] message) {
            final String messageStr = new String(message);
            final String result;
            try {
                result = normalize(messageStr, NFC);
            } catch (final Exception e) {
                throw new EncryptionInitializationException("Could not perform a valid UNICODE normalization", e);
            }
            return result.toCharArray();
        }
    }

    @TargetClass(RandomIvGenerator.class)
    static final class RandomIvGeneratorSubstitutions {
        @Alias
        @InjectAccessors(SecureRandomAccessor.class)
        private SecureRandom random;
        @Inject
        @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
        String secureRandomAlgorithm;

        @Substitute
        @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
        public RandomIvGeneratorSubstitutions(final String secureRandomAlgorithm) {
            this.secureRandomAlgorithm = secureRandomAlgorithm;
        }
    }

    @TargetClass(RandomSaltGenerator.class)
    static final class RandomSaltGeneratorSubstitutions {
        @Alias
        @InjectAccessors(SecureRandomAccessor.class)
        private SecureRandom random;
        @Inject
        @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
        String secureRandomAlgorithm;

        @Substitute
        @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
        public RandomSaltGeneratorSubstitutions(final String secureRandomAlgorithm) {
            this.secureRandomAlgorithm = secureRandomAlgorithm;
        }
    }
}

class SecureRandomAccessor {
    private static volatile SecureRandom RANDOM;
    private static final String DEFAULT_ALGORITHM = "SHA1PRNG";

    static SecureRandom get(Object instance) {
        SecureRandom result = RANDOM;
        if (result == null) {
            result = initializeOnce(getAlgorithm(instance));
        }
        return result;
    }

    static void set(Object instance, SecureRandom secureRandom) {
        throw new UnsupportedOperationException();
    }

    private static synchronized SecureRandom initializeOnce(String algorithm) {
        SecureRandom result = RANDOM;
        if (result != null) {
            return result;
        }

        try {
            result = SecureRandom.getInstance(algorithm);
            RANDOM = result;
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionInitializationException(e);
        }
    }

    private static String getAlgorithm(Object instance) {
        if (instance instanceof JasyptSubstitutions.RandomIvGeneratorSubstitutions) {
            return ((JasyptSubstitutions.RandomIvGeneratorSubstitutions) (instance)).secureRandomAlgorithm;
        } else if (instance instanceof JasyptSubstitutions.RandomSaltGeneratorSubstitutions) {
            return ((JasyptSubstitutions.RandomSaltGeneratorSubstitutions) (instance)).secureRandomAlgorithm;
        } else {
            return DEFAULT_ALGORITHM;
        }
    }
}
