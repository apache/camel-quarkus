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
package org.apache.camel.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Base for {@link CheckExtensionPagesMojo} and {@link UpdateExtensionDocPageMojo}.
 */
abstract class AbstractDocGeneratorMojo extends AbstractExtensionListMojo {
    public static final String DEFAULT_TEMPLATES_URI_BASE = "classpath:/doc-templates";
    /**
     */
    @Parameter(defaultValue = AbstractDocGeneratorMojo.DEFAULT_TEMPLATES_URI_BASE, required = true, property = "camel-quarkus.templatesUriBase")
    String templatesUriBase;
    /**
     * Directory where the changes should be performed. Default is the current directory of the current Java process.
     */
    @Parameter(property = "camel-quarkus.basedir", defaultValue = "${project.basedir}")
    File baseDir;

    protected static <T extends Writer> T evalTemplate(Configuration cfg, String templateUri, Map<String, Object> model,
            T out) {
        try {
            final Template template = cfg.getTemplate(templateUri);
            try {
                template.process(model, out);
            } catch (TemplateException e) {
                throw new RuntimeException("Could not process template " + templateUri + ":\n\n" + out.toString(), e);
            }
            return out;
        } catch (IOException e) {
            throw new RuntimeException("Could not evaluate template " + templateUri, e);
        }
    }

}
