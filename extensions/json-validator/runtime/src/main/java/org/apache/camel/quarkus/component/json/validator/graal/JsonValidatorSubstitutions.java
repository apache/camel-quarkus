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
package org.apache.camel.quarkus.component.json.validator.graal;

import java.lang.reflect.Constructor;
import java.util.function.BooleanSupplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonValidator;
import com.networknt.schema.PatternValidator;
import com.networknt.schema.ValidationContext;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

/**
 * Removes references to PatternValidatorEcma262Deletion, which requires optional org.jruby.joni:joni
 */
public class JsonValidatorSubstitutions {
}

@TargetClass(value = PatternValidator.class, onlyWith = IsJoniAbsent.class)
final class PatternValidatorSubstitutions {
    @Alias
    private JsonValidator delegate;
    @Alias
    private ValidationContext validationContext;

    @Substitute
    @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
    public PatternValidatorSubstitutions(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema,
            ValidationContext validationContext) {
        try {
            Class<?> clazz = Class.forName("com.networknt.schema.PatternValidator$PatternValidatorJava");
            Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, JsonNode.class, JsonSchema.class,
                    ValidationContext.class);

            this.validationContext = validationContext;
            this.delegate = (JsonValidator) constructor.newInstance(schemaPath, schemaNode, parentSchema,
                    validationContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

@TargetClass(className = "com.networknt.schema.PatternValidator$PatternValidatorEcma262", onlyWith = IsJoniAbsent.class)
@Delete
final class PatternValidatorEcma262Deletion {
}

final class IsJoniAbsent implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("org.joni.Regex");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
