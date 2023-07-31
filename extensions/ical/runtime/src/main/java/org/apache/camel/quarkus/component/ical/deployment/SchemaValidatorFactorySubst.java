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
package org.apache.camel.quarkus.component.ical.deployment;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import net.fortuna.ical4j.model.parameter.Schema;
import net.fortuna.ical4j.model.property.StructuredData;
import net.fortuna.ical4j.validate.Validator;
import net.fortuna.ical4j.validate.schema.SchemaValidatorFactory;

/**
 * Cuts out paths to optional JsonSchemaValidator. Only required if STRUCTURED-DATA elements are present
 * in the calendar definition. See RFC 9073.
 */
@TargetClass(value = SchemaValidatorFactory.class, onlyWith = IsJsonSkemaAbsent.class)
final class SchemaValidatorFactorySubstitutions {
    @Substitute
    public static Validator<StructuredData> newInstance(Schema schema) {
        throw new UnsupportedOperationException(
                "iCalendar JSON schema validation is unavailable. Add com.github.erosb:json-sKema to the application classpath");
    }
}

final class IsJsonSkemaAbsent implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("com.github.erosb.jsonsKema.Schema", false, Thread.currentThread().getContextClassLoader());
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
