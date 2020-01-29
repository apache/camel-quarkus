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
package org.apache.camel.quarkus.component.xslt.deployment;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.StringHelper;

public class BuildTimeUriResolver {

    public ResolutionResult resolve(String templateUri) {
        if (templateUri == null) {
            throw new RuntimeException("href parameter cannot be null");
        }
        templateUri = templateUri.trim();

        final String scheme = ResourceHelper.getScheme(templateUri);
        final String effectiveScheme = scheme == null ? "classpath:" : scheme;

        if (!"classpath:".equals(effectiveScheme)) {
            throw new RuntimeException("URI scheme '" + effectiveScheme
                    + "' not supported for build time compilation of XSL templates. Only 'classpath:' URIs are supported");
        }

        final String compacted = compact(templateUri, scheme);
        final String transletName = toTransletName(compacted);
        final URL url = Thread.currentThread().getContextClassLoader().getResource(compacted);
        if (url == null) {
            throw new IllegalStateException("Could not find the XSLT resource " + compacted + " in the classpath");
        }
        try {
            final Source src = new StreamSource(url.openStream());
            // TODO: if the XSLT file is under target/classes of the current project we should mark it for exclusion
            // from the application archive. See https://github.com/apache/camel-quarkus/issues/438
            return new ResolutionResult(templateUri, transletName, src);
        } catch (IOException e) {
            throw new RuntimeException("Could not read the class path resource " + templateUri, e);
        }

    }

    private String toTransletName(final String compacted) {
        final String fileName = FileUtil.stripPath(compacted);
        final String name = FileUtil.stripExt(fileName, true);
        return StringHelper.capitalize(name, true);
    }

    private static String compact(String href, String scheme) {
        final String afterScheme = scheme != null ? StringHelper.after(href, scheme) : href;
        final String compacted = FileUtil.compactPath(afterScheme, '/');
        return compacted;
    }

    static final class ResolutionResult {
        final String templateUri;
        final String transletClassName;
        final Source source;

        public ResolutionResult(String templateUri, String transletClassName, Source source) {
            this.templateUri = templateUri;
            this.transletClassName = transletClassName;
            this.source = source;
        }

        public UriResolverEntryBuildItem toBuildItem() {
            return new UriResolverEntryBuildItem(templateUri, transletClassName);
        }

    }

}
