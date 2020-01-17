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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.camel.util.AntPathMatcher;
import org.apache.camel.util.ObjectHelper;
import org.jboss.jandex.DotName;

/**
 * A utility able to filter resource paths using Ant-like includes and excludes.
 */
public class PathFilter {
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> includePatterns;
    private final List<String> excludePatterns;
    private final Predicate<String> stringPredicate;

    PathFilter(List<String> includePatterns, List<String> excludePatterns) {
        this.includePatterns = includePatterns;
        this.excludePatterns = excludePatterns;

        if (ObjectHelper.isEmpty(excludePatterns) && ObjectHelper.isEmpty(includePatterns)) {
            this.stringPredicate = path -> true;
        } else {
            this.stringPredicate = path -> {
                path = sanitize(path);
                // same logic as  org.apache.camel.main.DefaultRoutesCollector so exclude
                // take precedence over include
                for (String part : excludePatterns) {
                    if (matcher.match(part, path)) {
                        return false;
                    }
                }
                for (String part : includePatterns) {
                    if (matcher.match(part, path)) {
                        return true;
                    }
                }
                return ObjectHelper.isEmpty(includePatterns);
            };
        }
        ;
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

    static String sanitize(String path) {
        path = path.trim();
        return (!path.isEmpty() && path.charAt(0) == '/')
                ? path.substring(1)
                : path;
    }

    public static class Builder {
        private List<String> includePatterns = new ArrayList<String>();
        private List<String> excludePatterns = new ArrayList<String>();

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
            patterns.stream().map(PathFilter::sanitize).forEach(includePatterns::add);
            return this;
        }

        public Builder include(Optional<? extends Collection<String>> patterns) {
            patterns.ifPresent(ps -> include(ps));
            return this;
        }

        public Builder exclude(String pattern) {
            excludePatterns.add(sanitize(pattern));
            return this;
        }

        public Builder exclude(Collection<String> patterns) {
            patterns.stream().map(PathFilter::sanitize).forEach(excludePatterns::add);
            return this;
        }

        public Builder exclude(Optional<? extends Collection<String>> patterns) {
            patterns.ifPresent(ps -> exclude(ps));
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
