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
package org.apache.camel.quarkus.support.dsl.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.support.dsl.runtime.DslRecorder;
import org.apache.camel.spi.Resource;
import org.apache.camel.util.FileUtil;

public class DslSupportProcessor {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("import .*");
    public static final String CLASS_EXT = ".class";

    @BuildStep(onlyIf = NativeBuild.class)
    @Record(value = ExecutionTime.STATIC_INIT)
    void registerRoutesBuilder(List<DslGeneratedClassBuildItem> classes,
            CamelContextBuildItem context,
            DslRecorder recorder) throws Exception {
        RuntimeValue<CamelContext> camelContext = context.getCamelContext();
        for (DslGeneratedClassBuildItem clazz : classes) {
            recorder.registerRoutesBuilder(camelContext, clazz.getName(), clazz.getLocation(),
                    clazz.isInstantiateWithCamelContext());
        }
    }

    /**
     * @param  resource the resource from which the name is extracted.
     * @return          gives a name based on the location of the resource that can be used as a class name.
     */
    public static String determineName(Resource resource) {
        String str = FileUtil.onlyName(resource.getLocation(), true);
        StringBuilder sb = new StringBuilder();
        for (int i = 0, length = str.length(); i < length; i++) {
            char c = str.charAt(i);
            if ((i == 0 && Character.isJavaIdentifierStart(c)) || (i > 0 && Character.isJavaIdentifierPart(c))) {
                sb.append(c);
            } else {
                sb.append((int) c);
            }
        }
        return sb.toString();
    }

    /**
     * @param  contentResource the content of the resource from which the imports must be extracted.
     * @return                 the result of the extraction containing the extracted imports and the content of the resource
     *                         without
     *                         import statements.
     */
    public static ExtractImportResult extractImports(String contentResource) {
        List<String> imports = new ArrayList<>();
        Matcher m = IMPORT_PATTERN.matcher(contentResource);
        int beginIndex = 0;
        while (m.find()) {
            imports.add(m.group());
            beginIndex = m.end();
        }
        if (beginIndex > 0) {
            contentResource = contentResource.substring(beginIndex);
        }
        return new ExtractImportResult(imports, contentResource);
    }

    /**
     * {@code ExtractImportResult} represents the result of an imports' extraction.
     */
    public static class ExtractImportResult {

        /**
         * The list of extracted imports.
         */
        private final List<String> imports;
        /**
         * The content of the resource without import statement.
         */
        private final String content;

        public ExtractImportResult(List<String> imports, String content) {
            this.imports = imports;
            this.content = content;
        }

        public List<String> getImports() {
            return imports;
        }

        public String getContent() {
            return content;
        }
    }
}
