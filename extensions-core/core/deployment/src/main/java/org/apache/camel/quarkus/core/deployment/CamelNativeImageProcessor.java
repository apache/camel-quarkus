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
package org.apache.camel.quarkus.core.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Converter;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.TypeConverter;
import org.apache.camel.impl.engine.DefaultComponentResolver;
import org.apache.camel.impl.engine.DefaultDataFormatResolver;
import org.apache.camel.impl.engine.DefaultDevConsoleResolver;
import org.apache.camel.impl.engine.DefaultLanguageResolver;
import org.apache.camel.impl.engine.DefaultTransformerResolver;
import org.apache.camel.language.simple.SimpleNoFileLanguage;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.CamelConfig.ReflectionConfig;
import org.apache.camel.quarkus.core.CamelConfigFlags;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesBuilderClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.ScheduledPollConsumerScheduler;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.support.CamelContextHelper;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.ClassUtils.getPackageName;

public class CamelNativeImageProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelNativeImageProcessor.class);

    private static final DotName THROWABLE_TYPE = DotName.createSimple(Throwable.class.getName());

    private static final Set<String> EXCEPTION_REGISTRATION_METHODS = Set.of("onException", "doCatch", "exception",
            "throwException");

    private static final List<Class<?>> CAMEL_REFLECTIVE_CLASSES = List.<Class<?>> of(
            Endpoint.class,
            Consumer.class,
            Producer.class,
            TypeConverter.class,
            ExchangeFormatter.class,
            ScheduledPollConsumerScheduler.class,
            Component.class,
            CamelContext.class,
            StreamCachingStrategy.class,
            StreamCachingStrategy.SpoolUsedHeapMemoryLimit.class,
            PropertiesComponent.class,
            DataFormat.class);

    @BuildStep
    void reflectiveItems(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod) {

        final IndexView view = combinedIndex.getIndex();

        CAMEL_REFLECTIVE_CLASSES.stream()
                .map(Class::getName)
                .map(DotName::createSimple)
                .map(view::getAllKnownImplementations)
                .flatMap(Collection::stream)
                .filter(CamelSupport::isPublic)
                .forEach(v -> reflectiveClass
                        .produce(ReflectiveClassBuildItem.builder(v.name().toString()).methods().build()));

        DotName converter = DotName.createSimple(Converter.class.getName());
        List<ClassInfo> converterClasses = view.getAnnotations(converter)
                .stream()
                .filter(ai -> ai.target().kind() == Kind.CLASS)
                .filter(this::shouldRegisterConverter)
                .map(ai -> ai.target().asClass())
                .collect(Collectors.toList());

        LOGGER.debug("Converter classes: " + converterClasses);
        converterClasses
                .forEach(ci -> reflectiveClass
                        .produce(ReflectiveClassBuildItem.builder(ci.name().toString()).build()));

        view.getAnnotations(converter)
                .stream()
                .filter(ai -> ai.target().kind() == Kind.METHOD)
                .filter(ai -> converterClasses.contains(ai.target().asMethod().declaringClass()))
                .map(ai -> ai.target().asMethod())
                .forEach(mi -> reflectiveMethod.produce(new ReflectiveMethodBuildItem(mi)));

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(
                        "org.apache.camel.support.AbstractExchange",
                        org.apache.camel.support.MessageSupport.class.getName())
                        .methods().build());
    }

    private boolean shouldRegisterConverter(org.jboss.jandex.AnnotationInstance ai) {
        AnnotationValue av = ai.value("loader");
        boolean isLoader = av != null && av.asBoolean();
        // filter out camel-base converters which are automatically inlined in the
        // CoreStaticTypeConverterLoader
        // need to revisit with Camel 3.0.0-M3 which should improve this area
        if (ai.target().asClass().name().toString().startsWith("org.apache.camel.converter.")) {
            LOGGER.debug("Ignoring core " + ai + " " + ai.target().asClass().name());
            return false;
        } else if (isLoader) {
            LOGGER.debug("Ignoring " + ai + " " + ai.target().asClass().name());
            return false;
        } else {
            LOGGER.debug("Accepting " + ai + " " + ai.target().asClass().name());
            return true;
        }
    }

    @BuildStep
    void camelServices(
            List<CamelServiceBuildItem> camelServices,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        camelServices.forEach(service -> {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(service.type).methods().build());
        });

    }

    /*
     * Add camel catalog files to the native image.
     */
    @BuildStep(onlyIf = CamelConfigFlags.RuntimeCatalogEnabled.class)
    List<NativeImageResourceBuildItem> camelRuntimeCatalog(
            CamelConfig config,
            ApplicationArchivesBuildItem archives,
            List<CamelServicePatternBuildItem> servicePatterns) {

        List<NativeImageResourceBuildItem> resources = new ArrayList<>();

        final PathFilter pathFilter = servicePatterns.stream()
                .collect(
                        PathFilter.Builder::new,
                        (builder, patterns) -> builder.patterns(patterns.isInclude(), patterns.getPatterns()),
                        PathFilter.Builder::combine)
                .build();

        CamelConfig.RuntimeCatalogConfig runtimeCatalog = config.runtimeCatalog();
        CamelSupport.services(archives, pathFilter)
                .filter(service -> service.name != null && service.type != null && service.path != null)
                .forEach(service -> {

                    String packageName = getPackageName(service.type);
                    String jsonPath = "META-INF/%s/%s.json".formatted(packageName.replace('.', '/'), service.name);

                    if (runtimeCatalog.components()
                            && service.path.startsWith(DefaultComponentResolver.RESOURCE_PATH)) {
                        resources.add(new NativeImageResourceBuildItem(jsonPath));
                    }
                    if (runtimeCatalog.dataformats()
                            && service.path.startsWith(DefaultDataFormatResolver.DATAFORMAT_RESOURCE_PATH)) {
                        resources.add(new NativeImageResourceBuildItem(jsonPath));
                    }
                    if (runtimeCatalog.devconsoles()
                            && service.path.startsWith(DefaultDevConsoleResolver.DEV_CONSOLE_RESOURCE_PATH)) {
                        resources.add(new NativeImageResourceBuildItem(jsonPath));
                    }
                    if (runtimeCatalog.languages()
                            && service.path.startsWith(DefaultLanguageResolver.LANGUAGE_RESOURCE_PATH)) {
                        resources.add(new NativeImageResourceBuildItem(jsonPath));
                    }
                    if (runtimeCatalog.transformers()
                            && service.path
                                    .startsWith(DefaultTransformerResolver.DATA_TYPE_TRANSFORMER_RESOURCE_PATH)) {
                        resources.add(new NativeImageResourceBuildItem(jsonPath));
                    }
                });

        if (runtimeCatalog.models()) {
            for (ApplicationArchive archive : archives.getAllApplicationArchives()) {
                for (Path root : archive.getRootDirectories()) {
                    final Path resourcePath = root.resolve(CamelContextHelper.MODEL_DOCUMENTATION_PREFIX);

                    if (!Files.isDirectory(resourcePath)) {
                        continue;
                    }

                    try (Stream<Path> files = Files.walk(resourcePath)) {
                        List<String> items = files
                                .filter(Files::isRegularFile)
                                .map(root::relativize)
                                .map(Path::toString)
                                .collect(Collectors.toList());
                        LOGGER.debug("Register catalog json: {}", items);
                        resources.add(new NativeImageResourceBuildItem(items));
                    } catch (IOException e) {
                        throw new RuntimeException("Could not walk " + resourcePath, e);
                    }

                }
            }
        }

        return resources;
    }

    @BuildStep
    void reflection(CamelConfig config, ApplicationArchivesBuildItem archives,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        final ReflectionConfig reflectionConfig = config.native_().reflection();
        if (reflectionConfig.includePatterns().isEmpty()) {
            LOGGER.debug("No classes registered for reflection via quarkus.camel.native.reflection.include-patterns");
            return;
        }

        LOGGER.debug("Scanning resources for native inclusion from include-patterns {}",
                reflectionConfig.includePatterns().get());

        final PathFilter.Builder builder = new PathFilter.Builder();
        reflectionConfig.includePatterns().map(Collection::stream).orElseGet(Stream::empty)
                .map(className -> className.replace('.', '/'))
                .forEach(builder::include);
        reflectionConfig.excludePatterns().map(Collection::stream).orElseGet(Stream::empty)
                .map(className -> className.replace('.', '/'))
                .forEach(builder::exclude);
        final PathFilter pathFilter = builder.build();

        Stream<Path> archiveRootDirs = archives.getAllApplicationArchives().stream()
                .peek(archive -> LOGGER.debug("Scanning resources for native inclusion from archive at {}",
                        archive.getResolvedPaths()))
                .flatMap(archive -> archive.getRootDirectories().stream());
        String[] selectedClassNames = pathFilter.scanClassNames(archiveRootDirs);
        if (selectedClassNames.length > 0) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(selectedClassNames).methods().fields().build());
        }

    }

    @BuildStep
    void reflectiveRoutes(
            List<CamelRoutesBuilderClassBuildItem> camelRoutesBuilders,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        // Register routes as reflection aware as camel may use reflection
        // to perform post process tasks (i.e. for Camel's own DI)
        camelRoutesBuilders.forEach(camelRoutesBuilderClassBuildItem -> {
            reflectiveClass.produce(
                    // Register fields and methods as they may be used by the bean post processor to
                    // properly support @BindToRegistry
                    ReflectiveClassBuildItem.builder(camelRoutesBuilderClassBuildItem.getDotName().toString()).methods()
                            .fields().build());
        });
    }

    @BuildStep
    ReflectiveClassBuildItem registerSimpleNoFileLanguageForReflection() {
        // Required as SimpleNoFileLanguage loading & discovery is not done via the FactoryFinder
        return ReflectiveClassBuildItem.builder(SimpleNoFileLanguage.class).build();
    }

    /**
     * Scans RouteBuilder bytecode to auto-detect exception classes used in onException() and doCatch() calls, and
     * registers them for reflection. This is needed because Camel stores exception class names as strings in the route
     * model and resolves them back to Class objects at runtime via ClassLoader.loadClass().
     *
     * @see <a href="https://github.com/apache/camel-quarkus/issues/7841">camel-quarkus#7841</a>
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerOnExceptionClassesForReflection(
            CombinedIndexBuildItem combinedIndex,
            List<CamelRoutesBuilderClassBuildItem> camelRoutesBuilders,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        final IndexView index = combinedIndex.getIndex();
        final Set<String> confirmedClasses = new HashSet<>();

        for (CamelRoutesBuilderClassBuildItem routeBuilder : camelRoutesBuilders) {
            String className = routeBuilder.getDotName().toString();
            String resourceName = className.replace('.', '/') + ".class";

            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
                if (is == null) {
                    LOGGER.debug("Could not read bytecode for RouteBuilder: {}", className);
                    continue;
                }
                ClassReader reader = new ClassReader(is);
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                            String signature, String[] exceptions) {
                        return new ExceptionClassDetector(confirmedClasses, index);
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            } catch (IOException e) {
                LOGGER.debug("Failed to scan bytecode for RouteBuilder: {}", className, e);
            }
        }

        if (!confirmedClasses.isEmpty()) {
            LOGGER.debug("Auto-detected onException/doCatch classes for reflection: {}", confirmedClasses);
            for (String cls : confirmedClasses) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(cls).build());
            }
        }
    }

    /**
     * Checks whether the given class name is a Throwable subclass. First checks the Jandex index (covers application
     * and indexed dependency classes), then falls back to Class.forName() for JDK/unindexed classes.
     */
    static boolean isThrowable(String className, IndexView index) {
        // Check via Jandex index first (covers application and indexed dependency classes)
        ClassInfo classInfo = index.getClassByName(DotName.createSimple(className));
        if (classInfo != null) {
            return isThrowableInIndex(classInfo, index);
        }

        // Fall back to Class.forName() for JDK and unindexed library classes
        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            return Throwable.class.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isThrowableInIndex(ClassInfo classInfo, IndexView index) {
        DotName current = classInfo.name();
        while (current != null) {
            if (THROWABLE_TYPE.equals(current)) {
                return true;
            }
            ClassInfo info = index.getClassByName(current);
            if (info == null) {
                // Class not in index, try Class.forName() for the remaining hierarchy
                try {
                    Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(current.toString());
                    return Throwable.class.isAssignableFrom(clazz);
                } catch (ClassNotFoundException e) {
                    return false;
                }
            }
            current = info.superName();
        }
        return false;
    }

    /**
     * ASM MethodVisitor that detects Throwable class constants passed to onException/doCatch/exception method calls.
     * <p>
     * It tracks class constants loaded via LDC instructions in a pending set. When a method call instruction is
     * visited:
     * <ul>
     * <li>If the method is onException/doCatch/exception with a Class parameter, all pending Throwable classes are
     * confirmed.</li>
     * <li>The pending set is cleared after any method call, so unrelated class references are discarded.</li>
     * </ul>
     */
    static class ExceptionClassDetector extends MethodVisitor {
        private final Set<String> pendingClasses = new LinkedHashSet<>();
        private final Set<String> confirmedClasses;
        private final IndexView index;

        ExceptionClassDetector(Set<String> confirmedClasses, IndexView index) {
            super(Opcodes.ASM9);
            this.confirmedClasses = confirmedClasses;
            this.index = index;
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof Type type && type.getSort() == Type.OBJECT) {
                String className = type.getClassName();
                if (isThrowable(className, index)) {
                    pendingClasses.add(className);
                }
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                String descriptor, boolean isInterface) {
            if (EXCEPTION_REGISTRATION_METHODS.contains(name) && descriptor.contains("Ljava/lang/Class;")) {
                confirmedClasses.addAll(pendingClasses);
            }
            pendingClasses.clear();
        }
    }

}
