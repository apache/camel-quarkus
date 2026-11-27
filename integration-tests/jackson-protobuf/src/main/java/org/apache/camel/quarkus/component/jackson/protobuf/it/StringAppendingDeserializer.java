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
package org.apache.camel.quarkus.component.jackson.protobuf.it;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Simple deserializer to append a String onto the text field on a {@link Pojo} instance
 */
public class StringAppendingDeserializer extends StdDeserializer<Pojo> {

    public static final String STRING_TO_APPEND = " Custom Deserialized";

    public StringAppendingDeserializer() {
        super(Pojo.class);
    }

    @Override
    public Pojo deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode jsonNode = p.readValueAsTree();
        String text = jsonNode.get("text").asString();
        Pojo pojo = new Pojo();
        pojo.setText(text + STRING_TO_APPEND);
        return pojo;
    }
}
