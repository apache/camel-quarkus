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
package org.apache.camel.quarkus.component.xslt.deployment;

import org.apache.camel.quarkus.component.xslt.RuntimeUriResolver;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Holds a pair of XSLT template URI and the unqualified translet name to use when creating a
 * {@link RuntimeUriResolver}.
 */
public final class UriResolverEntryBuildItem extends MultiBuildItem {
    private final String templateUri;
    private final String transletClassName;

    public UriResolverEntryBuildItem(String templateUri, String transletClassName) {
        this.templateUri = templateUri;
        this.transletClassName = transletClassName;
    }

    public String getTemplateUri() {
        return templateUri;
    }

    public String getTransletClassName() {
        return transletClassName;
    }

}
