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
package org.apache.camel.quarkus.component.support.langchain4j.deployment;

import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.runtime.configuration.ConfigurationException;
import org.apache.camel.quarkus.component.support.langchain4j.deployment.SupportQuarkusLangchain4jProcessor.AugmentorDefinition;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.support.langchain4j.deployment.SupportQuarkusLangchain4jProcessor.resolveDesignatedDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The designated-default rules: two augmentors with no default marked must fail the build —
 * an unmarked ambiguity would make the unqualified {@code Instance<RetrievalAugmentor>} lookup
 * unresolvable and silently disable RAG for every AI service.
 */
class RagAugmentorDefaultResolutionTest {

    @Test
    void singleAugmentorIsImplicitlyDefault() {
        assertEquals("products", resolveDesignatedDefault(augmentors("products", false), false));
    }

    @Test
    void multipleAugmentorsWithOneMarkedResolveToIt() {
        Map<String, AugmentorDefinition> effective = augmentors("products", false, "support", true);
        assertEquals("support", resolveDesignatedDefault(effective, false));
    }

    @Test
    void multipleAugmentorsWithNoneMarkedFailTheBuild() {
        Map<String, AugmentorDefinition> effective = augmentors("products", false, "support", false);
        ConfigurationException e = assertThrows(ConfigurationException.class,
                () -> resolveDesignatedDefault(effective, false));
        assertTrue(e.getMessage().contains("none is marked default"), e.getMessage());
        assertTrue(e.getMessage().contains(".default=true"), e.getMessage());
    }

    @Test
    void multipleAugmentorsMarkedDefaultFailTheBuild() {
        Map<String, AugmentorDefinition> effective = augmentors("products", true, "support", true);
        ConfigurationException e = assertThrows(ConfigurationException.class,
                () -> resolveDesignatedDefault(effective, false));
        assertTrue(e.getMessage().contains("Multiple retrieval augmentors are marked default"), e.getMessage());
    }

    @Test
    void userProvidedAugmentorSuppressesAnyDefault() {
        assertNull(resolveDesignatedDefault(augmentors("products", false, "support", false), true));
    }

    @Test
    void userProvidedAugmentorConflictsWithMarkedDefault() {
        ConfigurationException e = assertThrows(ConfigurationException.class,
                () -> resolveDesignatedDefault(augmentors("products", true), true));
        assertTrue(e.getMessage().contains("already provides a RetrievalAugmentor"), e.getMessage());
    }

    private static Map<String, AugmentorDefinition> augmentors(Object... nameAndDefaultPairs) {
        Map<String, AugmentorDefinition> map = new LinkedHashMap<>();
        for (int i = 0; i < nameAndDefaultPairs.length; i += 2) {
            String name = (String) nameAndDefaultPairs[i];
            boolean markedDefault = (Boolean) nameAndDefaultPairs[i + 1];
            map.put(name, new AugmentorDefinition(name + "-store", null, markedDefault));
        }
        return map;
    }
}
