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
package org.apache.camel.quarkus.maven.processor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link DocumentationPostProcessor} transform AsciiDoc headings to add ID blocks.
 */
public class SectionIdPostProcessor implements DocumentationPostProcessor {
    private static final Pattern PATTERN_ASCIIDOC_HEADING = Pattern.compile("^=+ (.*)", Pattern.MULTILINE);
    private static final Pattern PATTERN_NON_ALPHA_NUMERIC = Pattern.compile("[^A-Za-z\\d-]");
    private static final Pattern PATTERN_MULTIPLE_HYPHEN = Pattern.compile("-+");
    private static final Pattern PATTERN_TRIM_HYPHEN = Pattern.compile("^-|-$");

    @Override
    public void process(AsciiDocFile file) {
        Matcher matcher = PATTERN_ASCIIDOC_HEADING.matcher(file.getContent());
        while (matcher.find()) {
            String headingMarkup = matcher.group(0);
            String heading = matcher.group(1);
            String fileName = file.getPath().getFileName().toString();
            String docType = fileName.substring(0, fileName.lastIndexOf('.'));
            String id = generateAsciiDocIdentifier(file.getCqExtension(), docType, heading.toLowerCase().trim());
            file.replace(headingMarkup, id + headingMarkup);
        }
    }

    private String generateAsciiDocIdentifier(String extension, String docType, String heading) {
        String sanitizedHeading = String.format("extensions-%s-%s-%s", extension, docType, heading);
        sanitizedHeading = PATTERN_NON_ALPHA_NUMERIC.matcher(sanitizedHeading).replaceAll("-");
        sanitizedHeading = PATTERN_MULTIPLE_HYPHEN.matcher(sanitizedHeading).replaceAll("-");
        sanitizedHeading = PATTERN_TRIM_HYPHEN.matcher(sanitizedHeading).replaceAll("");
        return String.format("[id=\"%s\"]\n", sanitizedHeading).toLowerCase();
    }
}
