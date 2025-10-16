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
package org.apache.camel.quarkus.grpc.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.deployment.util.ProcessUtil;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathFilter;
import io.quarkus.runtime.util.HashUtil;
import io.smallrye.common.cpu.CPU;
import io.smallrye.common.os.OS;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import static java.util.Arrays.asList;

/**
 * Custom {@link CodeGenProvider} for Camel Quarkus gRPC. Based on the original Quarkus gRPC implementation:
 * <p>
 * <a href=
 * "https://github.com/quarkusio/quarkus/tree/main/extensions/grpc/codegen">https://github.com/quarkusio/quarkus/tree/main/extensions/grpc/codegen</a>
 * <p>
 * The implementation does not include additional logic for features not supported in Camel gRPC, such as integration
 * with Mutiny and CDI.
 */
public class CamelQuarkusGrpcCodegenProvider implements CodeGenProvider {
    private static final Logger LOG = Logger.getLogger(CamelQuarkusGrpcCodegenProvider.class);
    private static final String EXE = "exe";
    private static final String PROTO = ".proto";
    private static final String PROTOC = "protoc";
    private static final String PROTOC_GROUP_ID = "com.google.protobuf";
    private static final String SCAN_DEPENDENCIES_FOR_PROTO = "quarkus.camel.grpc.codegen.scan-for-proto";
    private static final String SCAN_DEPENDENCIES_FOR_PROTO_INCLUDE_PATTERN = "quarkus.camel.grpc.codegen.scan-for-proto-includes.\"%s\"";
    private static final String SCAN_DEPENDENCIES_FOR_PROTO_EXCLUDE_PATTERN = "quarkus.camel.grpc.codegen.scan-for-proto-excludes.\"%s\"";
    private static final String SCAN_FOR_IMPORTS = "quarkus.camel.grpc.codegen.scan-for-imports";

    private Executables executables;

    @Override
    public String providerId() {
        return "camel-quarkus-grpc";
    }

    @Override
    public String[] inputExtensions() {
        return new String[] { "proto" };
    }

    @Override
    public String inputDirectory() {
        return "proto";
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        final Config config = context.config();
        if (!config.getValue("quarkus.camel.grpc.codegen.enabled", Boolean.class)) {
            LOG.info("Skipping " + this.getClass() + " invocation on user's request");
            return false;
        }

        Path outDir = context.outDir();
        Path workDir = context.workDir();
        Set<String> protoDirs = new HashSet<>();

        try {
            List<String> protoFiles = new ArrayList<>();
            if (Files.isDirectory(context.inputDir())) {
                try (Stream<Path> protoFilesPaths = Files.walk(context.inputDir())) {
                    protoFilesPaths
                            .filter(Files::isRegularFile)
                            .filter(s -> s.toString().endsWith(PROTO))
                            .map(Path::normalize)
                            .map(Path::toAbsolutePath)
                            .map(Path::toString)
                            .forEach(protoFiles::add);
                    protoDirs.add(context.inputDir().normalize().toAbsolutePath().toString());
                }
            }
            Path dirWithProtosFromDependencies = workDir.resolve("protoc-protos-from-dependencies");
            Collection<Path> protoFilesFromDependencies = gatherProtosFromDependencies(dirWithProtosFromDependencies, protoDirs,
                    context);
            if (!protoFilesFromDependencies.isEmpty()) {
                protoFilesFromDependencies.stream()
                        .map(Path::normalize)
                        .map(Path::toAbsolutePath)
                        .map(Path::toString)
                        .forEach(protoFiles::add);
            }

            if (!protoFiles.isEmpty()) {
                initExecutables(workDir, context.applicationModel());

                Path protocDependenciesDir = workDir.resolve("protoc-dependencies");
                Collection<String> protosToImport = gatherDirectoriesWithImports(protocDependenciesDir, context);

                if (!protoFilesFromDependencies.isEmpty() && !protosToImport.isEmpty()) {
                    // Clean up potential duplicates that can exist when scan-for-imports artifacts clash with scan-for-proto configs
                    protoFiles.forEach(file -> {
                        if (!file.contains(context.inputDir().toString())) {
                            String relativeFileName = file.replace(dirWithProtosFromDependencies.toString(), "");
                            Path path = Paths.get(protocDependenciesDir.toAbsolutePath().toString(), relativeFileName);
                            if (Files.exists(path)) {
                                LOG.debugf("Cleaning up duplicate proto file %s", path);
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    LOG.errorf("Failed cleaning up duplicate proto file %s", e);
                                }
                            }
                        }
                    });
                }

                List<String> command = new ArrayList<>();
                command.add(executables.protoc.toString());
                for (String protoImportDir : protosToImport) {
                    command.add(String.format("-I=%s", escapeWhitespace(protoImportDir)));
                }
                for (String protoDir : protoDirs) {
                    command.add(String.format("-I=%s", escapeWhitespace(protoDir)));
                }

                command.addAll(asList("--plugin=protoc-gen-grpc=" + executables.grpc,
                        "--grpc_out=" + outDir,
                        "--java_out=" + outDir));
                command.addAll(protoFiles);

                ProcessBuilder processBuilder = new ProcessBuilder(command);

                final Process process = ProcessUtil.launchProcess(processBuilder, context.shouldRedirectIO());
                int resultCode = process.waitFor();
                if (resultCode != 0) {
                    throw new CodeGenException("Failed to generate Java classes from proto files: " + protoFiles +
                            " to " + outDir.toAbsolutePath() + " with command " + String.join(" ", command));
                }
                return true;
            }
        } catch (IOException | InterruptedException e) {
            throw new CodeGenException(
                    "Failed to generate java files from proto file in " + context.inputDir().toAbsolutePath(), e);
        }

        return false;
    }

    private static void copySanitizedProtoFile(ResolvedDependency artifact, Path protoPath, Path outProtoPath)
            throws IOException {
        boolean genericServicesFound = false;

        try (var reader = Files.newBufferedReader(protoPath);
                var writer = Files.newBufferedWriter(outProtoPath)) {

            String line = reader.readLine();
            while (line != null) {
                // filter java_generic_services to avoid "Tried to write the same file twice"
                // when set to true. Generic services are deprecated and replaced by classes generated by
                // this plugin
                if (!line.contains("java_generic_services")) {
                    writer.write(line);
                    writer.newLine();
                } else {
                    genericServicesFound = true;
                }

                line = reader.readLine();
            }
        }

        if (genericServicesFound) {
            LOG.infof("Ignoring option java_generic_services in %s:%s%s.", artifact.getGroupId(), artifact.getArtifactId(),
                    protoPath);
        }
    }

    private Collection<Path> gatherProtosFromDependencies(Path workDir, Set<String> protoDirectories,
            CodeGenContext context) throws CodeGenException {
        if (context.test()) {
            return Collections.emptyList();
        }
        Config properties = context.config();
        String scanDependencies = properties.getValue(SCAN_DEPENDENCIES_FOR_PROTO, String.class);

        if ("none".equalsIgnoreCase(scanDependencies)) {
            return Collections.emptyList();
        }
        boolean scanAll = "all".equalsIgnoreCase(scanDependencies);

        List<String> dependenciesToScan = asList(scanDependencies.split(","));

        ApplicationModel appModel = context.applicationModel();
        List<Path> protoFilesFromDependencies = new ArrayList<>();
        for (ResolvedDependency artifact : appModel.getRuntimeDependencies()) {
            String packageId = String.format("%s:%s", artifact.getGroupId(), artifact.getArtifactId());
            Collection<String> includes = properties
                    .getOptionalValues(String.format(SCAN_DEPENDENCIES_FOR_PROTO_INCLUDE_PATTERN, packageId), String.class)
                    .orElse(List.of());

            Collection<String> excludes = properties
                    .getOptionalValues(String.format(SCAN_DEPENDENCIES_FOR_PROTO_EXCLUDE_PATTERN, packageId), String.class)
                    .orElse(List.of());

            if (scanAll
                    || dependenciesToScan.contains(packageId)) {
                extractProtosFromArtifact(workDir, protoFilesFromDependencies, protoDirectories, artifact, includes, excludes,
                        true);
            }
        }
        return protoFilesFromDependencies;
    }

    @Override
    public boolean shouldRun(Path sourceDir, Config config) {
        return CodeGenProvider.super.shouldRun(sourceDir, config)
                || isGeneratingFromAppDependenciesEnabled(config);
    }

    private boolean isGeneratingFromAppDependenciesEnabled(Config config) {
        return config.getOptionalValue(SCAN_DEPENDENCIES_FOR_PROTO, String.class)
                .filter(value -> !"none".equals(value)).isPresent();
    }

    private Collection<String> gatherDirectoriesWithImports(Path workDir, CodeGenContext context) throws CodeGenException {
        Config properties = context.config();

        String scanForImports = properties.getOptionalValue(SCAN_FOR_IMPORTS, String.class)
                .orElse("com.google.protobuf:protobuf-java");

        if ("none".equals(scanForImports.toLowerCase(Locale.getDefault()))) {
            return Collections.emptyList();
        }

        boolean scanAll = "all".equals(scanForImports.toLowerCase(Locale.getDefault()));
        List<String> dependenciesToScan = Arrays.asList(scanForImports.split(","));

        Set<String> importDirectories = new HashSet<>();
        ApplicationModel appModel = context.applicationModel();
        for (ResolvedDependency artifact : appModel.getRuntimeDependencies()) {
            if (scanAll
                    || dependenciesToScan.contains(
                            String.format("%s:%s", artifact.getGroupId(), artifact.getArtifactId()))) {
                extractProtosFromArtifact(workDir, new ArrayList<>(), importDirectories, artifact, List.of(),
                        List.of(), false);
            }
        }
        return importDirectories;
    }

    private void extractProtosFromArtifact(Path workDir, Collection<Path> protoFiles,
            Set<String> protoDirectories, ResolvedDependency artifact, Collection<String> filesToInclude,
            Collection<String> filesToExclude, boolean isDependency) throws CodeGenException {

        try {
            artifact.getContentTree(new PathFilter(filesToInclude, filesToExclude)).walk(
                    pathVisit -> {
                        Path path = pathVisit.getPath();
                        if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(PROTO)) {
                            Path root = pathVisit.getRoot();
                            if (Files.isDirectory(root)) {
                                protoFiles.add(path);
                                protoDirectories.add(path.getParent().normalize().toAbsolutePath().toString());
                            } else { // archive
                                Path relativePath = path.getRoot().relativize(path);
                                Path protoUnzipDir = workDir
                                        .resolve(HashUtil.sha1(root.normalize().toAbsolutePath().toString()))
                                        .normalize().toAbsolutePath();
                                try {
                                    Files.createDirectories(protoUnzipDir);
                                    protoDirectories.add(protoUnzipDir.toString());
                                } catch (IOException e) {
                                    throw new GrpcCodeGenException("Failed to create directory: " + protoUnzipDir, e);
                                }
                                Path outPath = protoUnzipDir;
                                for (Path part : relativePath) {
                                    outPath = outPath.resolve(part.toString());
                                }
                                try {
                                    Files.createDirectories(outPath.getParent());
                                    if (isDependency) {
                                        copySanitizedProtoFile(artifact, path, outPath);
                                    } else {
                                        Files.copy(path, outPath, StandardCopyOption.REPLACE_EXISTING);
                                    }
                                    protoFiles.add(outPath);
                                } catch (IOException e) {
                                    throw new GrpcCodeGenException("Failed to extract proto file" + path + " to target: "
                                            + outPath, e);
                                }
                            }
                        }
                    });
        } catch (GrpcCodeGenException e) {
            throw new CodeGenException(e.getMessage(), e);
        }
    }

    private String escapeWhitespace(String path) {
        if (io.smallrye.common.os.OS.current() != OS.WINDOWS) {
            return path.replace(" ", "\\ ");
        } else {
            return path;
        }
    }

    private void initExecutables(Path workDir, ApplicationModel model) throws CodeGenException {
        if (executables == null) {
            Path protocPath;
            String protocPathProperty = System.getProperty("quarkus.grpc.protoc-path");
            String classifier = System.getProperty("quarkus.grpc.protoc-os-classifier", osClassifier());
            if (protocPathProperty == null) {
                protocPath = findArtifactPath(model, PROTOC_GROUP_ID, PROTOC, classifier, EXE);
            } else {
                protocPath = Paths.get(protocPathProperty);
            }
            Path protocExe = makeExecutableFromPath(workDir, PROTOC_GROUP_ID, PROTOC, classifier, "exe", protocPath);

            Path protocGrpcPluginExe = prepareExecutable(workDir, model,
                    "io.grpc", "protoc-gen-grpc-java", classifier, "exe");

            executables = new Executables(protocExe, protocGrpcPluginExe);
        }
    }

    private Path prepareExecutable(Path buildDir, ApplicationModel model,
            String groupId, String artifactId, String classifier, String packaging) throws CodeGenException {
        Path artifactPath = findArtifactPath(model, groupId, artifactId, classifier, packaging);

        return makeExecutableFromPath(buildDir, groupId, artifactId, classifier, packaging, artifactPath);
    }

    private Path makeExecutableFromPath(Path buildDir, String groupId, String artifactId, String classifier, String packaging,
            Path artifactPath) throws CodeGenException {
        Path exe = buildDir.resolve(String.format("%s-%s-%s-%s", groupId, artifactId, classifier, packaging));

        if (Files.exists(exe)) {
            return exe;
        }

        if (artifactPath == null) {
            String location = String.format("%s:%s:%s:%s", groupId, artifactId, classifier, packaging);
            throw new CodeGenException("Failed to find " + location + " among dependencies");
        }

        try {
            Files.copy(artifactPath, exe, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CodeGenException("Failed to copy file: " + artifactPath + " to " + exe, e);
        }
        if (!exe.toFile().setExecutable(true)) {
            throw new CodeGenException("Failed to make the file executable: " + exe);
        }
        return exe;
    }

    private static Path findArtifactPath(ApplicationModel model, String groupId, String artifactId, String classifier,
            String packaging) {
        Path artifactPath = null;

        for (ResolvedDependency artifact : model.getDependencies()) {
            if (groupId.equals(artifact.getGroupId())
                    && artifactId.equals(artifact.getArtifactId())
                    && classifier.equals(artifact.getClassifier())
                    && packaging.equals(artifact.getType())) {
                artifactPath = artifact.getResolvedPaths().getSinglePath();
            }
        }
        return artifactPath;
    }

    private String osClassifier() throws CodeGenException {
        String architecture = getArchitecture();
        switch (OS.current()) {
        case LINUX:
            return "linux-" + architecture;
        case WINDOWS:
            return "windows-" + architecture;
        case MAC:
            return "osx-" + architecture;
        default:
            throw new CodeGenException(
                    "Unsupported OS, please use maven plugin instead to generate Java classes from proto files");
        }
    }

    public static String getArchitecture() {
        return switch (CPU.host()) {
        case x64 -> "x86_64";
        case x86 -> "x86_32";
        case arm -> "arm_32";
        case aarch64 -> "aarch_64";
        case mips -> "mips_32";
        case mipsel -> "mipsel_32";
        case mips64 -> "mips_64";
        case mips64el -> "mipsel_64";
        case ppc32 -> "ppc_32";
        case ppc32le -> "ppcle_32";
        case ppc -> "ppc_64";
        case ppcle -> "ppcle_64";
        default -> null;
        };
    }

    private static class Executables {
        final Path protoc;
        final Path grpc;

        Executables(Path protoc, Path grpc) {
            this.protoc = protoc;
            this.grpc = grpc;
        }
    }

    private static class GrpcCodeGenException extends RuntimeException {
        private GrpcCodeGenException(String message, Exception cause) {
            super(message, cause);
        }
    }

}
