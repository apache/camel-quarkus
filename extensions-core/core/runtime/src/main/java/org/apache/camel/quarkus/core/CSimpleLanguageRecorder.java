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
package org.apache.camel.quarkus.core;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.language.csimple.CSimpleExpression;
import org.apache.camel.language.csimple.CSimpleLanguage;
import org.apache.camel.language.csimple.CSimpleLanguage.Builder;

@Recorder
public class CSimpleLanguageRecorder {

    public RuntimeValue<CSimpleLanguage> configureCSimpleLanguage(
            RuntimeValue<CamelContext> context,
            Map<String, String> compiledExpressions) {
        final Builder builder = CSimpleLanguage.builder();

        for (Entry<String, String> en : compiledExpressions.entrySet()) {
            final String clazz = en.getValue();
            try {
                final Class<CSimpleExpression> cl = (Class<CSimpleExpression>) Class.forName(clazz);
                final CSimpleExpression expression = cl.getConstructor(CamelContext.class).newInstance(context.getValue());
                builder.expression(expression);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException("Could not load " + clazz, e);
            }
        }

        return new RuntimeValue<>(builder.build());
    }

}
