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
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Singleton;

import org.apache.avro.Schema;
import org.apache.camel.dataformat.avro.AvroDataFormat;

@Singleton
public class AvroDataFormatProducer {

    private final Map<String, Schema> schemaRegistry = new HashMap<>();

    public void registerAvroSchema(String injectedFieldId, Schema schema) {
        schemaRegistry.put(injectedFieldId, schema);
    }

    @Produces
    AvroDataFormat produceAvroDataFormat(InjectionPoint injectionPoint) {
        Member member = injectionPoint.getMember();
        if (member instanceof Field) {
            Field field = (Field) member;
            if (!Modifier.isStatic(member.getModifiers()) && field.getAnnotation(BuildTimeAvroDataFormat.class) != null) {
                String injectedFieldId = member.getDeclaringClass().getName() + "." + member.getName();
                Schema schema = schemaRegistry.get(injectedFieldId);
                return new AvroDataFormat(schema);
            }
        }
        String message = "AvroDataFormat beans can only be injected into non-static field annotated with @";
        throw new IllegalArgumentException(message + BuildTimeAvroDataFormat.class.getName());
    }
}
