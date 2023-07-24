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
package org.apache.camel.quarkus.k.core;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.HasId;

public interface Source extends HasId {
    String getLocation();

    String getName();

    String getLanguage();

    SourceType getType();

    Optional<String> getLoader();

    List<String> getInterceptors();

    List<String> getPropertyNames();

    InputStream resolveAsInputStream(CamelContext ctx);

    default Reader resolveAsReader(CamelContext ctx) {
        return resolveAsReader(ctx, StandardCharsets.UTF_8);
    }

    default Reader resolveAsReader(CamelContext ctx, Charset charset) {
        return new InputStreamReader(resolveAsInputStream(ctx), charset);
    }
}
