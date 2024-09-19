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
package org.apache.camel.quarkus.support.swagger.runtime.graal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.exception.ReadContentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SwaggerSubstitutions {
}

@TargetClass(OpenAPIParser.class)
final class OpenAPIParserSubstitutions {

    @Substitute
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        if (url.startsWith("resource:")) {
            url = url.replaceFirst("resource:", "");
        }

        SwaggerParseResult output = null;

        for (SwaggerParserExtension extension : OpenAPIV3Parser.getExtensions()) {
            output = extension.readLocation(url, auth, options);
            if (output != null && output.getOpenAPI() != null) {
                return output;
            }
        }

        return output;
    }
}

@TargetClass(OpenAPIV3Parser.class)
final class OpenAPIPV3ParserSubstitutions {
    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static Logger LOGGER = LoggerFactory.getLogger(OpenAPIV3Parser.class);

    @Substitute
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        try {
            if (url.startsWith("resource:")) {
                url = url.replaceFirst("resource:", "");
            }
            final String content = readContentFromLocation(url, emptyListIfNull(auth));
            LOGGER.debug("Loaded raw data: {}", content);
            return readContents(content, auth, options, url);
        } catch (ReadContentException e) {
            LOGGER.warn("Exception while reading:", e);
            return SwaggerParseResult.ofError(e.getMessage());
        }
    }

    @Alias
    private String readContentFromLocation(String location, List<AuthorizationValue> auth) {
        return null;
    }

    @Alias
    private <T> List<T> emptyListIfNull(List<T> list) {
        return null;
    }

    @Alias
    public SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options,
            String location) {
        return null;
    }
}

@TargetClass(Calendar.Builder.class)
final class CalendarBuilderSubstitutions {
    @Substitute
    public Calendar build() {
        throw new UnsupportedOperationException("Calendar::build is not supported");
    }
}

// Cuts out references to deprecated & removed Jackson methods
// TODO: Remove this https://github.com/apache/camel-quarkus/issues/6593
@TargetClass(ModelResolver.class)
final class ModelResolverSubstitutions {
    @Substitute
    protected Type findJsonValueType(final BeanDescription beanDesc) {
        try {
            Method m = BeanDescription.class.getMethod("findJsonValueAccessor");
            AnnotatedMember jsonValueMember = (AnnotatedMember) m.invoke(beanDesc);
            if (jsonValueMember != null) {
                return jsonValueMember.getType();
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
        return null;
    }
}
