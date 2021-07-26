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
package org.apache.camel.quarkus.component.avro.deployment;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.avro.Schema;
import org.apache.camel.dataformat.avro.AvroDataFormat;
import org.apache.camel.quarkus.component.avro.BuildTimeAvroDataFormat;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuildTimeAvroDataFormatTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addAsResource("schemas/a-user.avsc")
                    .addAsResource("schemas/another-user.avsc"));

    @BuildTimeAvroDataFormat("schemas/a-user.avsc")
    AvroDataFormat aUserBuildTimeAvroDataFormat;

    @BuildTimeAvroDataFormat("schemas/a-user.avsc")
    AvroDataFormat aUserBuildTimeAvroDataFormatBis;

    @BuildTimeAvroDataFormat("schemas/another-user.avsc")
    AvroDataFormat anotherUserBuildTimeAvroDataFormat;

    //@Test
    void buildTimeAvroDataFormatAnnotationsShouldBeProcessed() {
        assertNotNull(aUserBuildTimeAvroDataFormat);
        Object aUserObjectSchema = aUserBuildTimeAvroDataFormat.getSchema();
        assertNotNull(aUserObjectSchema);
        assertTrue(aUserObjectSchema instanceof Schema);
        Schema aUserSchema = (Schema) aUserObjectSchema;
        assertEquals("a.user", aUserSchema.getNamespace());

        Object aUserBisObjectSchema = aUserBuildTimeAvroDataFormatBis.getSchema();
        assertNotNull(aUserBisObjectSchema);
        assertNotSame(aUserObjectSchema, aUserBisObjectSchema);

        assertNotNull(anotherUserBuildTimeAvroDataFormat);
        Object anotherUserObjectSchema = anotherUserBuildTimeAvroDataFormat.getSchema();
        assertNotNull(anotherUserObjectSchema);
        assertTrue(anotherUserObjectSchema instanceof Schema);
        Schema anotherUserSchema = (Schema) anotherUserObjectSchema;
        assertEquals("another.user", anotherUserSchema.getNamespace());
    }

}
