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
package org.apache.camel.quarkus.component.openapi.java.graal;

import java.util.List;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.apicurio.datamodels.core.models.ExtensibleNode;
import org.apache.camel.openapi.RestOpenApiReader;

@TargetClass(RestOpenApiReader.class)
public final class RestOpenApiReaderSubstitutions {
    @Substitute
    private static void convertAndSetItemsEnum(ExtensibleNode items, List<String> allowableValues, Class<?> type) {
        throw new UnsupportedOperationException("RestOpenApiReader::convertAndSetItemsEnum should not be invoked");
    }
}
