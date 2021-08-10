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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathFilterTest {

    @Test
    public void stringFilter() {
        assertFalse(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList("org/**"),
                Arrays.asList()));
        assertFalse(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList("org/**"),
                Arrays.asList("org/**", "org/acme/MyClass")));
        assertFalse(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList("org/acme/M*"),
                Arrays.asList("org/acme/MyClass")));
        assertFalse(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList(),
                Arrays.asList("org/acme/A*")));

        assertTrue(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList(),
                Arrays.asList()));
        assertTrue(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList(),
                Arrays.asList("org/**")));
        assertTrue(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList("org/acme/A*"),
                Arrays.asList("org/acme/MyClass")));
    }

    @Test
    public void pathFilter() {
        // windows support need trick with PAth to handle File.separator
        Predicate<Path> predicate = new PathFilter.Builder()
                .include(Paths.get("/foo/bar/star").toString().replace("star", "*"))
                .include(Paths.get("moo/mar/star").toString().replace("star", "*"))
                .exclude(Paths.get("/foo/baz/star").toString().replace("star", "*"))
                .exclude(Paths.get("moo/maz/star").toString().replace("star", "*"))
                .build().asPathPredicate();
        Assertions.assertEquals("foo\\bar\\*", Paths.get("foo/bar/star").toString().replace("star", "*"));
        assertTrue(predicate.test(Paths.get("/foo/bar/file")));
        assertTrue(predicate.test(Paths.get("foo/bar/file")));
        assertFalse(predicate.test(Paths.get("/foo/baz/file")));
        assertFalse(predicate.test(Paths.get("foo/baz/file")));

        assertTrue(predicate.test(Paths.get("/moo/mar/file")));
        assertTrue(predicate.test(Paths.get("moo/mar/file")));
        assertFalse(predicate.test(Paths.get("/moo/marz/file")));
        assertFalse(predicate.test(Paths.get("moo/maz/file")));
    }

    @Test
    public void dotNameFilter() {
        Predicate<DotName> predicate = new PathFilter.Builder()
                .include("foo/bar/*")
                .exclude("foo/baz/*")
                .build().asDotNamePredicate();
        assertTrue(predicate.test(DotName.createSimple("foo.bar.Class")));
        assertFalse(predicate.test(DotName.createSimple("foo.baz.Class")));
    }

    static boolean isPathIncluded(String path, List<String> excludePatterns, List<String> includePatterns) {
        return new PathFilter(includePatterns, excludePatterns).asStringPredicate().test(path);
    }

    @Test
    void scanClassNames() {
        final PathFilter filter = new PathFilter.Builder()
                .include(Paths.get("org/p1/star").toString().replace("star", "*"))
                .include(Paths.get("org/p2/starstar").toString().replaceAll("star", "*"))
                .exclude(Paths.get("org/p1/ExcludedClass").toString())
                .exclude(Paths.get("org/p2/excludedpackage/starstar").toString().replaceAll("star", "*"))
                .build();
        final Path rootPath = Paths.get("/foo");
        final Stream<Path> pathStream = Stream.of(
                "org/p1/Class1.class",
                "org/p1/Class1$Inner.class",
                "org/p1/Class1.txt",
                "org/p1/ExcludedClass.class",
                "org/p2/excludedpackage/ExcludedClass.class",
                "org/p2/excludedpackage/p/ExcludedClass.class",
                "org/p2/whatever/Class2.class")
                .map(rootPath::resolve);

        final Predicate<Path> isRegularFile = path -> path.getFileName().toString().contains(".");
        final Set<String> classNames = new TreeSet<>();
        filter.scanClassNames(rootPath, pathStream, isRegularFile, classNames::add);

        Assertions.assertEquals(new TreeSet<>(Arrays.asList(
                "org.p1.Class1",
                "org.p1.Class1$Inner",
                "org.p2.whatever.Class2")),
                classNames);

    }

    @Test
    void addNonPatternPaths() {
        final PathFilter pathFilter = new PathFilter.Builder()
                .include("org/p1/*")
                .include("org/p2/**")
                .include("org/p3/NonPatternClass")
                .exclude("org/p1/ExcludedClass")
                .exclude("org/p2/excludedpackage/**")
                .build();
        Set<String> selectedPaths = new TreeSet<>();
        pathFilter.addNonPatternPaths(selectedPaths);
        Assertions.assertEquals(new TreeSet<>(Arrays.asList("org.p3.NonPatternClass")), selectedPaths);
    }

}
