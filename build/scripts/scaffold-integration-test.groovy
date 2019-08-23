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
import java.util.stream.Collectors
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import freemarker.cache.ClassTemplateLoader
import freemarker.cache.FileTemplateLoader
import freemarker.cache.MultiTemplateLoader
import freemarker.cache.TemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateException
import freemarker.template.TemplateExceptionHandler

import io.quarkus.maven.CreateExtensionMojo.TemplateParams
import io.quarkus.maven.utilities.PomTransformer
import io.quarkus.maven.utilities.PomTransformer.Transformation

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/* Keep in sync with the current file name */
@groovy.transform.Field static final String currentScript = "scaffold-integration-test.groovy"
@groovy.transform.Field static final Logger log = LoggerFactory.getLogger('scaffold-integration-test')
final Path extensionsDir = project.basedir.toPath()
final Path templatesDir = extensionsDir.resolve('../build/create-extension-templates')
final String artifactIdBase = properties['quarkus.artifactIdBase']
final Path itestDir = extensionsDir.resolve('../integration-tests/' + artifactIdBase)
final Charset charset = StandardCharsets.UTF_8

final nameBase = properties['quarkus.nameBase'] == null ? artifactIdBase.split(' ').collect{it.capitalize()}.join(' ') : artifactIdBase

Files.createDirectories(itestDir)

final Configuration cfg = new Configuration(Configuration.VERSION_2_3_28)
cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
cfg.setTemplateLoader(createTemplateLoader(templatesDir))
cfg.setDefaultEncoding('UTF-8')
cfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX)
cfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX)

TemplateParams model = new TemplateParams()

model.artifactIdBase = artifactIdBase
model.artifactIdBaseCamelCase = toCapCamelCase(model.artifactIdBase)
model.version = project.version

model.nameBase = nameBase

model.javaPackageBase = getJavaPackage('org.apache.camel.quarkus', 'component', artifactIdBase)
final String javaPackageBasePath = model.javaPackageBase.replace('.', '/')

evalTemplate(cfg, "integration-test-pom.xml", itestDir.resolve('pom.xml'), charset, model)
evalTemplate(cfg, "TestResource.java", itestDir.resolve('src/main/java/' + javaPackageBasePath + '/it/'+ model.artifactIdBaseCamelCase + 'Resource.java'), charset, model)
evalTemplate(cfg, "TestRouteBuilder.java", itestDir.resolve('src/main/java/' + javaPackageBasePath + '/it/'+ model.artifactIdBaseCamelCase + 'RouteBuilder.java'), charset, model)
evalTemplate(cfg, "Test.java", itestDir.resolve('src/test/java/' + javaPackageBasePath + '/it/'+ model.artifactIdBaseCamelCase + 'Test.java'), charset, model)
evalTemplate(cfg, "IT.java", itestDir.resolve('src/test/java/' + javaPackageBasePath + '/it/'+ model.artifactIdBaseCamelCase + 'IT.java'), charset, model)

log.info(String.format("Adding module [%s] to [%s]", model.artifactIdBase, itestDir.resolve('../pom.xml')))
new PomTransformer(itestDir.resolve('../pom.xml'), charset).transform(Transformation.addModule(model.artifactIdBase))

static TemplateLoader createTemplateLoader(Path templatesDir) throws IOException {
    return new MultiTemplateLoader([new FileTemplateLoader(templatesDir.toFile())] as TemplateLoader[])
}

static void evalTemplate(Configuration cfg, String templateUri, Path dest, Charset charset, TemplateParams model)
        throws IOException, TemplateException {
    log.info("Adding '{}'", dest)
    final Template template = cfg.getTemplate(templateUri)
    Files.createDirectories(dest.getParent())
    Writer out
    try {
        out = Files.newBufferedWriter(dest)
        template.process(model, out)
    } finally {
        out.close()
    }
}

static String toCapCamelCase(String artifactIdBase) {
    final StringBuilder sb = new StringBuilder(artifactIdBase.length())
    for (String segment : artifactIdBase.split("[.\\-]+")) {
        sb.append(Character.toUpperCase(segment.charAt(0)))
        if (segment.length() > 1) {
            sb.append(segment.substring(1))
        }
    }
    return sb.toString()
}

static String getJavaPackage(String groupId, String javaPackageInfix, String artifactId) {
    final Stack<String> segments = new Stack<>()
    for (String segment : groupId.split("[.\\-]+")) {
        if (segments.isEmpty() || !segments.peek().equals(segment)) {
            segments.add(segment)
        }
    }
    if (javaPackageInfix != null) {
        for (String segment : javaPackageInfix.split("[.\\-]+")) {
            segments.add(segment)
        }
    }
    for (String segment : artifactId.split("[.\\-]+")) {
        if (!segments.contains(segment)) {
            segments.add(segment)
        }
    }
    return segments.stream() //
            .map{s -> s.toLowerCase(Locale.ROOT)} //
            .map{s -> javax.lang.model.SourceVersion.isKeyword(s) ? s + "_" : s} //
            .collect(Collectors.joining("."))
}

