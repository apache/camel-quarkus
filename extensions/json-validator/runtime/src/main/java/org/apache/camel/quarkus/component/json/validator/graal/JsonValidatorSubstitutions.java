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

import com.networknt.schema.ValidationContext;
import com.networknt.schema.regex.RegularExpression;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Removes references to PatternValidatorEcma262Deletion, which requires optional org.jruby.joni:joni
 */
public class JsonValidatorSubstitutions {
}

@TargetClass(value = RegularExpression.class, onlyWith = IsJoniAbsent.class)
@Substitute
interface RegularExpressionSubstitutions {
    @Substitute
    boolean matches(String value);

    @Substitute
    static RegularExpression compile(String regex, ValidationContext validationContext) {
        if (null == regex)
            return s -> true;
        try {
            Class<?> clazz = Class.forName("com.networknt.schema.regex.JDKRegularExpression");
            Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);

            return (RegularExpression) constructor.newInstance(regex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
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
