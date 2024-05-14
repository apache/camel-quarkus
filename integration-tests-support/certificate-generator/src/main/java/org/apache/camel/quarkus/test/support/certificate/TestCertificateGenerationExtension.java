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

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import me.escoffier.certs.AliasRequest;
import me.escoffier.certs.CertificateFiles;
import me.escoffier.certs.CertificateGenerator;
import me.escoffier.certs.CertificateRequest;
import me.escoffier.certs.junit5.Alias;
import me.escoffier.certs.junit5.Certificate;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.util.AnnotationUtils;
import org.testcontainers.DockerClientFactory;

/**
 * Extension is based on
 * https://github.com/cescoffier/certificate-generator/blob/main/certificate-generator-junit5/src/main/java/me/escoffier/certs/junit5/CertificateGenerationExtension.java
 *
 * Unfortunately there is no way of extending the original Extension with functionality of modifying CN and
 * SubjectAlternativeName
 * based on docker host (required for usage with external docker host)
 * Therefore I created a new annotation 'TestCertificates' which would use this new extension.
 */
public class TestCertificateGenerationExtension implements BeforeAllCallback, ParameterResolver {
    private static final Logger LOGGER = Logger.getLogger(TestCertificateGenerationExtension.class);

    public static TestCertificateGenerationExtension getInstance(ExtensionContext extensionContext) {
        return extensionContext.getStore(ExtensionContext.Namespace.GLOBAL)
                .get(TestCertificateGenerationExtension.class, TestCertificateGenerationExtension.class);
    }

    List<CertificateFiles> certificateFiles = new ArrayList<>();

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        extensionContext.getStore(ExtensionContext.Namespace.GLOBAL)
                .getOrComputeIfAbsent(TestCertificateGenerationExtension.class, c -> this);
        var maybe = AnnotationUtils.findAnnotation(extensionContext.getRequiredTestClass(), TestCertificates.class);
        if (maybe.isEmpty()) {
            return;
        }
        var annotation = maybe.get();

        //cn and alternativeSubjectName might be different (to reflect docker host)
        Optional<String> cn = resolveDockerHost();
        Optional<String> altSubName = cn.stream().map(h -> "IP:%s".formatted(h)).findAny();

        for (Certificate certificate : annotation.certificates()) {
            String baseDir = annotation.baseDir();
            File file = new File(baseDir);
            file.mkdirs();
            CertificateGenerator generator = new CertificateGenerator(file.toPath(), annotation.replaceIfExists());

            CertificateRequest request = new CertificateRequest()
                    .withName(certificate.name())
                    .withClientCertificate(certificate.client())
                    .withFormats(Arrays.asList(certificate.formats()))
                    .withCN(cn.orElse(certificate.cn()))
                    .withPassword(certificate.password().isEmpty() ? null : certificate.password())
                    .withDuration(Duration.ofDays(certificate.duration()));

            if (altSubName.isPresent()) {
                request.withSubjectAlternativeName(altSubName.get());
            }

            for (String san : certificate.subjectAlternativeNames()) {
                request.withSubjectAlternativeName(san);
            }

            for (Alias alias : certificate.aliases()) {
                AliasRequest nested = new AliasRequest()
                        .withCN(alias.cn())
                        .withPassword(alias.password())
                        .withClientCertificate(alias.client());
                request.withAlias(alias.name(), nested);
                for (String s : alias.subjectAlternativeNames()) {
                    nested.withSubjectAlternativeName(s);
                }
            }

            certificateFiles.addAll(generator.generate(request));
        }
    }

    private Optional<String> resolveDockerHost() {
        String dockerHost = DockerClientFactory.instance().dockerHostIpAddress();
        if (!dockerHost.equals("localhost") && !dockerHost.equals("127.0.0.1")) {
            return Optional.of(dockerHost);
        }
        return Optional.empty();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        throw new IllegalArgumentException("Not supported!");
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        throw new IllegalArgumentException("Not supported!");
    }
}
