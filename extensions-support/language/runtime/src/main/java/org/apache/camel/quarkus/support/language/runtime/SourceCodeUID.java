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
package org.apache.camel.quarkus.support.language.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * {@code SourceCodeUID} is the root class of all implementations of a unique identifier of a source code expressed
 * in a given language.
 * <p>
 * To be able to retrieve a generated class thanks to its source code, an algorithm is applied to the source code to
 * get a unique name that can be used as simple class name of the generated class.
 * <p>
 * The current algorithm, hashes first the source code to avoid having an identifier too long, then the result is
 * encoded in base 64 to ensure that all characters are easy to read and finally converted in
 * such way that it can be used as a Java identifier such as a class name.
 */
public abstract class SourceCodeUID {

    /**
     * The prefix of the Java identifier.
     */
    private final String prefix;

    protected SourceCodeUID(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return the source code to transform into an id.
     */
    protected abstract String getSourceCode();

    /**
     * @return the source code as bytes.
     */
    private byte[] asBytes() {
        return getSourceCode().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @return the hash of the source code.
     */
    private byte[] asHash() {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(asBytes());
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the hash of the source code encoded in base 64.
     */
    private byte[] asHashInBase64() {
        return Base64.getEncoder().encode(asHash());
    }

    /**
     * @return the source code converted as a Java identifier.
     */
    public String asJavaIdentifier() {
        final StringBuilder sb = new StringBuilder(prefix);
        final String str = new String(asHashInBase64(), StandardCharsets.UTF_8);
        for (int i = 0, length = str.length(); i < length; i++) {
            char c = str.charAt(i);
            if (i > 0 && Character.isJavaIdentifierPart(c)) {
                sb.append(c);
            } else {
                sb.append((int) c);
            }
        }
        return sb.toString();
    }

    /**
     * @return the value of {@link #asJavaIdentifier()}.
     */
    @Override
    public final String toString() {
        return asJavaIdentifier();
    }
}
