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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.StringHelper;

public class BuildTimeUriResolver {

    /** A directory to use as a base for resolving {@code file:} URIs */
    private final Path baseDir;

    public BuildTimeUriResolver(Path baseDir) {
        this.baseDir = baseDir;
    }

    public ResolutionResult resolve(String templateUri) throws TransformerException {
        if (templateUri == null) {
            throw new RuntimeException("href parameter cannot be null");
        }
        templateUri = templateUri.trim();

        final String scheme = ResourceHelper.getScheme(templateUri);
        final String effectiveScheme = scheme == null ? "classpath:" : scheme;

        final String transletName;
        final Source src;
        if ("file:".equals(effectiveScheme)) {
            final String compacted = compact(templateUri, scheme);
            transletName = toTransletName(compacted);
            final Path resolvedPath = baseDir.resolve(compacted);
            try {
                src = new StreamSource(Files.newInputStream(resolvedPath));
            } catch (IOException e) {
                throw new TransformerException("Could not read from file " + templateUri, e);
            }
        } else if ("classpath:".equals(effectiveScheme)) {
            final String compacted = compact(templateUri, scheme);
            transletName = toTransletName(compacted);
            final URL url = getClass().getClassLoader().getResource(compacted);
            try {
                src = new StreamSource(url.openStream());
            } catch (IOException e) {
                throw new TransformerException("Could not read the class path resource " + templateUri, e);
            }

            // TODO: if the XSLT file is under target/classes of the current project we should mark it for exclusion
            //       from the application archive. See https://github.com/apache/camel-quarkus/issues/438
        } else {
            try {
                final URL url = new URL(templateUri);
                final String path = url.getPath();
                transletName = toTransletName(path);
                src = new StreamSource(url.openStream());
            } catch (MalformedURLException e) {
                throw new TransformerException("Invalid URL " + templateUri, e);
            } catch (IOException e) {
                throw new TransformerException("Could not read from URL " + templateUri, e);
            }
        }
        return new ResolutionResult(templateUri, transletName, src);
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
