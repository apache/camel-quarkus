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
package org.apache.camel.quarkus.component.jolt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeepCopySubstitutionTest {

    @SuppressWarnings("unchecked")
    //@Test
    void nominalSchemaDeepCopyShouldSucceed() {
        LinkedHashMap<String, Object> originalLinkedHashMap = new LinkedHashMap<>();
        ArrayList<Integer> originalArrayList = new ArrayList<>(Arrays.asList(new Integer(10)));

        originalLinkedHashMap.put("boolean", new Boolean(true));
        originalLinkedHashMap.put("string", "original");
        originalLinkedHashMap.put("list", originalArrayList);
        originalLinkedHashMap.put("null", null);

        Object copiedLinkedHashMapObject = DeepCopySubstitution.simpleDeepCopy(originalLinkedHashMap);

        assertTrue(copiedLinkedHashMapObject instanceof LinkedHashMap);
        LinkedHashMap<String, Object> copiedLinkHashMap = (LinkedHashMap<String, Object>) copiedLinkedHashMapObject;
        Object copiedArrayListObject = copiedLinkHashMap.get("list");
        assertTrue(copiedArrayListObject instanceof ArrayList);
        ArrayList<Integer> copiedArrayList = (ArrayList<Integer>) copiedArrayListObject;

        assertTrue(Objects.deepEquals(originalLinkedHashMap, copiedLinkHashMap));

        assertNotSame(originalLinkedHashMap, copiedLinkHashMap);
        assertNotSame(originalLinkedHashMap.get("boolean"), copiedLinkHashMap.get("boolean"));
        assertNotSame(originalLinkedHashMap.get("string"), copiedLinkHashMap.get("string"));
        assertNotSame(originalArrayList, copiedArrayList);
        assertNotSame(originalArrayList.get(0), copiedArrayList.get(0));
        assertNull(copiedLinkHashMap.get("null"));
    }

}
