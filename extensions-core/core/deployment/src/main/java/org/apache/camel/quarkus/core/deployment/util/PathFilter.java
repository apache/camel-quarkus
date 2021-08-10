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
package org.apache.camel.quarkus.core.deployment.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.util.AntPathMatcher;
import org.apache.camel.util.ObjectHelper;
import org.jboss.jandex.DotName;

/**
 * A utility able to filter resource paths using Ant-like includes and excludes.
 */
public class PathFilter {
    private static final String CLASS_SUFFIX = ".class";
    private static final int CLASS_SUFFIX_LENGTH = CLASS_SUFFIX.length();

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> includePaths;
    private final List<String> includePatterns;
    private final List<String> excludePatterns;
    private final Predicate<String> stringPredicate;

    PathFilter(List<String> includePatterns, List<String> excludePatterns) {
        super();
        this.includePaths = includePatterns.stream().filter(path -> !isPattern(path)).collect(Collectors.toList());
        this.includePatterns = includePatterns;
        this.excludePatterns = excludePatterns;

        if (ObjectHelper.isEmpty(excludePatterns) && ObjectHelper.isEmpty(includePatterns)) {
            this.stringPredicate = path -> true;
        } else {
            this.stringPredicate = path -> {
                path = sanitize(path);
                // same logic as org.apache.camel.main.DefaultRoutesCollector so exclude
                // take precedence over include
                if (matchesAny(path, excludePatterns)) {
                    return false;
                }
                if (matchesAny(path, includePatterns)) {
                    return true;
                }
                return ObjectHelper.isEmpty(includePatterns);
            };
        }
    }

    public Predicate<String> asStringPredicate() {
        return stringPredicate;
    }

    public Predicate<DotName> asDotNamePredicate() {
        if (ObjectHelper.isEmpty(excludePatterns) && ObjectHelper.isEmpty(includePatterns)) {
            return dotName -> true;
        } else {
            return dotName -> stringPredicate.test(dotName.toString().replace('.', '/'));
        }
    }

    public Predicate<Path> asPathPredicate() {
        if (ObjectHelper.isEmpty(excludePatterns) && ObjectHelper.isEmpty(includePatterns)) {
            return path -> true;
        } else {
            return path -> stringPredicate.test(sanitize(path.toString()));
        }
    }

    public String[] scanClassNames(Stream<Path> archiveRootDirs) {
        final Set<String> selectedPaths = new TreeSet<>();
        archiveRootDirs.forEach(rootDir -> scanClassNames(rootDir, CamelSupport.safeWalk(rootDir),
                Files::isRegularFile, selectedPaths::add));
        /* Let's add the paths without wildcards even if they did not match any Jandex class
         * A workaround for https://github.com/apache/camel-quarkus/issues/2969 */
        addNonPatternPaths(selectedPaths);
        return selectedPaths.toArray(new String[0]);
    }

    void addNonPatternPaths(final Set<String> selectedPaths) {
        if (!includePaths.isEmpty()) {
            for (String path : includePaths) {
                if (!selectedPaths.contains(path) && !matchesAny(path, excludePatterns)) {
                    selectedPaths.add(path.replace('/', '.'));
                }
            }
        }
    }

    void scanClassNames(Path rootPath, Stream<Path> pathStream, Predicate<Path> isRegularFile,
            Consumer<String> resultConsumer) {
        pathStream
                .filter(isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(CLASS_SUFFIX))
                .map(rootPath::relativize)
                .map(Path::toString)
                .map(stringPath -> stringPath.substring(0, stringPath.length() - CLASS_SUFFIX_LENGTH))
                .filter(stringPredicate)
                .map(slashClassName -> slashClassName.replace(File.separator, "."))
                .forEach(resultConsumer::accept);
    }

    boolean matchesAny(String path, List<String> patterns) {
        for (String part : patterns) {
            if (matcher.match(part, path)) {
                return true;
            }
        }
        return false;
    }

    static String sanitize(String path) {
        path = path.trim();
        return (!path.isEmpty() && path.charAt(0) == File.separator.charAt(0))
                ? path.substring(1)
                : path;
    }

    static boolean isPattern(String path) {
        return path.indexOf('*') != -1 || path.indexOf('?') != -1;
    }

    public static class Builder {
        private List<String> includePatterns = new ArrayList<>();
        private List<String> excludePatterns = new ArrayList<>();

        public Builder patterns(boolean isInclude, Collection<String> patterns) {
            if (isInclude) {
                include(patterns);
            } else {
                exclude(patterns);
            }
            return this;
        }

        public Builder include(String pattern) {
            includePatterns.add(sanitize(pattern));
            return this;
        }

        public Builder include(Collection<String> patterns) {
            patterns.stream().forEach(this::include);
            return this;
        }

        public Builder include(Optional<? extends Collection<String>> patterns) {
            patterns.ifPresent(this::include);
            return this;
        }

        public Builder exclude(String pattern) {
            excludePatterns.add(sanitize(pattern));
            return this;
        }

        public Builder exclude(Collection<String> patterns) {
            patterns.stream().forEach(this::exclude);
            return this;
        }

        public Builder exclude(Optional<? extends Collection<String>> patterns) {
            patterns.ifPresent(this::exclude);
            return this;
        }

        public Builder combine(Builder other) {
            includePatterns.addAll(other.includePatterns);
            excludePatterns.addAll(other.excludePatterns);
            return this;
        }

        /**
         * @throws NullPointerException if this method is called more than once for the same {@link Builder} instance.
         * @return                      a new {@link PathFilter}
         */
        public PathFilter build() {
            final List<String> incl = includePatterns;
            includePatterns = null; // avoid leaking the collection trough reuse of the builder
            final List<String> excl = excludePatterns;
            excludePatterns = null; // avoid leaking the collection trough reuse of the builder
            return new PathFilter(incl, excl);
        }

    }

}
