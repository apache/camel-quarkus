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
package org.apache.camel.quarkus.component.avro;

import java.lang.reflect.Field;
import java.util.Collections;

import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.camel.dataformat.avro.AvroDataFormat;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AvroDataFormatProducerTest {

    private AvroDataFormatProducer instance;

    private InjectionPoint mockInjectionPoint;
    @SuppressWarnings("unused")
    private static AvroDataFormat injectedField;
    private Field injectedFieldMember;

    @BeforeEach
    public void setup() throws NoSuchFieldException, SecurityException {
        instance = new AvroDataFormatProducer(new AvroSchemaRegistry(Collections.emptyMap()));
        injectedFieldMember = AvroDataFormatProducerTest.class.getDeclaredField("injectedField");
        mockInjectionPoint = mock(InjectionPoint.class);
        when(mockInjectionPoint.getMember()).thenReturn(injectedFieldMember);
    }

    //@Test
    void produceAvroDataFormatFromNonStaticFieldShouldThrow() {
        IllegalArgumentException iaex = assertThrows(IllegalArgumentException.class, () -> {
            instance.produceAvroDataFormat(mockInjectionPoint);
        });
        assertNotNull(iaex.getMessage());
    }

}
