package org.apache.camel.quarkus.component.mail.deployment;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.mail.Provider;
import javax.mail.Session;

import io.quarkus.arc.ResourceProvider;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import org.apache.camel.quarkus.core.runtime.CamelConfig.Runtime;

class MailProcessor {

    private static final String FEATURE = "camel-mail";

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    BuildProducer<SubstrateResourceBuildItem> resource;

    @Inject
    BuildProducer<ServiceProviderBuildItem> services;

    @Inject
    ApplicationArchivesBuildItem applicationArchivesBuildItem;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void process() throws IOException {
        List<String> providers = resources("META-INF/services/javax.mail.Provider")
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .collect(Collectors.toList());

        List<String> imp1 = providers.stream()
                .map(this::loadClass)
                .map(this::instantiate)
                .map(Provider.class::cast)
                .map(Provider::getClassName)
                .collect(Collectors.toList());

        List<String> imp2 = Stream.of("META-INF/javamail.default.providers", "META-INF/javamail.providers")
                .flatMap(this::resources)
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .flatMap(s -> Stream.of(s.split(";")))
                .map(String::trim)
                .filter(s -> s.startsWith("class="))
                .map(s -> s.substring("class=".length()))
                .collect(Collectors.toList());

        List<String> imp3 = resources("META-INF/mailcap")
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .flatMap(s -> Stream.of(s.split(";")))
                .map(String::trim)
                .filter(s -> s.startsWith("x-java-content-handler="))
                .map(s -> s.substring("x-java-content-handler=".length()))
                .collect(Collectors.toList());

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false,
                Stream.concat(providers.stream(), Stream.concat(imp1.stream(), Stream.concat(imp2.stream(), imp3.stream())))
                        .distinct()
                        .toArray(String[]::new)));

        resource.produce(new SubstrateResourceBuildItem(
                "META-INF/services/javax.mail.Provider",
                "META-INF/javamail.charset.map",
                "META-INF/javamail.default.address.map",
                "META-INF/javamail.default.providers",
                "META-INF/javamail.address.map",
                "META-INF/javamail.providers",
                "META-INF/mailcap"));
    }

    private Stream<URL> resources(String path) {
        try {
            return enumerationAsStream(getClass().getClassLoader().getResources(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> loadClass(String name) {
        try {
            return getClass().getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<String> lines(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<String> lines(URL url) {
        try (InputStream is = url.openStream()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                return br.lines().collect(Collectors.toList()).stream();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
private <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                            public T next() {
                                return e.nextElement();
                            }
                            public boolean hasNext() {
                                return e.hasMoreElements();
                            }
                        },
                        Spliterator.ORDERED), false);
    }
}
