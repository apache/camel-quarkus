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
package org.apache.camel.quarkus.test.support.certificate;

public class CertificatesUtil {
    public static final String DEFAULT_CERTS_BASEDIR = "target/certs";

    private CertificatesUtil() {
    }

    public static String keystoreFile(String name, String extension) {
        return file(name + "-keystore", extension);
    }

    public static String caCrt(String name) {
        return file(name + "-ca", "crt");
    }

    public static String crt(String name) {
        return file(name, "crt");
    }

    public static String key(String name) {
        return file(name, "key");
    }

    private static String file(String name, String extension) {
        return DEFAULT_CERTS_BASEDIR + "/" + name + "." + extension;
    }
}
