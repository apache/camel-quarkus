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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.escoffier.certs.junit5.Certificate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Based on
 * https://github.com/cescoffier/certificate-generator/blob/main/certificate-generator-junit5/src/main/java/me/escoffier/certs/junit5/Certificates.java
 * Generates certificates before the tests via 'TestCertificateGenerationExtension' so the new certificates
 * are customized to fullfill a remote docker host (if required).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TestCertificateGenerationExtension.class)
@Inherited
public @interface TestCertificates {

    /**
     * The base directory in which certificates will be generated.
     * Default value is `target/classes/certs`
     */
    String baseDir() default CertificatesUtil.DEFAULT_CERTS_BASEDIR;

    /**
     * The certificates to generate.
     * Must not be empty.
     */
    Certificate[] certificates();

    /**
     * Whether to replace the certificates if they already exist.
     */
    boolean replaceIfExists() default false;

    /**
     * Whether certificate is used in docker container. If so, the cn and subject alt name has to equal docker host
     * (which might differ in case of external docker host)
     */
    boolean docker() default false;
}
