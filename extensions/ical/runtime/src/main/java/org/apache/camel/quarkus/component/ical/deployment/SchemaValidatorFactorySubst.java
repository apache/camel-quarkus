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

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import net.fortuna.ical4j.model.parameter.Schema;
import net.fortuna.ical4j.model.property.StructuredData;
import net.fortuna.ical4j.validate.Validator;
import net.fortuna.ical4j.validate.schema.SchemaValidatorFactory;

@TargetClass(value = SchemaValidatorFactory.class)
final class SchemaValidatorFactorySubstitutions {

    @Substitute
    public static Validator<StructuredData> newInstance(Schema schema) {
        //see https://github.com/apache/camel-quarkus/issues/5099 for more details
        //Method causes error (it is unclear to me, why it is happening):
        //UnresolvedElementException: Discovered unresolved method during parsing: net.fortuna.ical4j.validate.schema.JsonSchemaValidator.<init>(java.net.URL).
        // This error is reported at image build time because class net.fortuna.ical4j.validate.schema.SchemaValidatorFactory
        // is registered for linking at image build time by command line

        throw new RuntimeException("Feature is not supported.");
    }
}
