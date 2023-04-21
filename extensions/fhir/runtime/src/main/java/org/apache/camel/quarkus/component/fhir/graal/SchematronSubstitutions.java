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
package org.apache.camel.quarkus.component.fhir.graal;

import java.util.function.BooleanSupplier;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.schematron.SchematronBaseValidator;
import ca.uhn.fhir.validation.schematron.SchematronProvider;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

final class SchematronSubstitutions {
}

@TargetClass(value = SchematronProvider.class, onlyWith = IsSchematronAbsent.class)
final class SubstituteSchematronProvider {
    @Substitute
    public static boolean isSchematronAvailable(FhirContext theFhirContext) {
        return false;
    }
}

@TargetClass(value = SchematronBaseValidator.class, onlyWith = IsSchematronAbsent.class)
@Delete
final class DeleteSchematronBaseValidator {
}

final class IsSchematronAbsent implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("com.helger.schematron.ISchematronResource");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
