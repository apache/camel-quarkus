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
package org.apache.camel.catalog.quarkus;

import java.util.List;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QuarkusRuntimeProviderTest {

    static CamelCatalog catalog;

    @BeforeClass
    public static void createCamelCatalog() {
        catalog = new DefaultCamelCatalog();
        catalog.setRuntimeProvider(new QuarkusRuntimeProvider());
    }

    @Test
    public void testGetVersion() throws Exception {
        String version = catalog.getCatalogVersion();
        assertNotNull(version);

        String loaded = catalog.getLoadedVersion();
        assertNotNull(loaded);
        assertEquals(version, loaded);
    }

    @Test
    public void testProviderName() throws Exception {
        assertEquals("quarkus", catalog.getRuntimeProvider().getProviderName());
    }

    @Test
    public void testFindComponentNames() throws Exception {
        List<String> names = catalog.findComponentNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("aws-eks"));
        assertTrue(names.contains("bean"));
        assertTrue(names.contains("direct"));
        assertTrue(names.contains("imap"));
        assertTrue(names.contains("imaps"));
        assertTrue(names.contains("jdbc"));
        assertTrue(names.contains("log"));
        assertTrue(names.contains("servlet"));
        assertTrue(names.contains("twitter-search"));
        // camel-pax-logging does not work in quarkus
        assertFalse(names.contains("paxlogging"));
    }

    @Test
    public void testFindDataFormatNames() throws Exception {
        List<String> names = catalog.findDataFormatNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("csv"));
        assertTrue(names.contains("mime-multipart"));
        assertTrue(names.contains("zipfile"));
    }

    @Test
    public void testFindLanguageNames() throws Exception {
        List<String> names = catalog.findLanguageNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        // core languages
        assertTrue(names.contains("constant"));
        assertTrue(names.contains("simple"));

        // quarkus-bean
        assertTrue(names.contains("bean"));

        // spring expression language are not in quarkus
        assertFalse(names.contains("spel"));
    }

    @Test
    public void testFindOtherNames() throws Exception {
        List<String> names = catalog.findOtherNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("core-cloud"));
        assertTrue(names.contains("platform-http"));
        assertTrue(names.contains("reactive-executor"));

        assertFalse(names.contains("blueprint"));
        assertFalse(names.contains("hystrix"));
    }

    @Test
    public void testComponentArtifactId() throws Exception {
        String json = catalog.componentJSonSchema("salesforce");

        assertNotNull(json);
        assertTrue(json.contains("camel-quarkus-salesforce"));
    }

    @Test
    public void testDataFormatArtifactId() throws Exception {
        String json = catalog.dataFormatJSonSchema("csv");

        assertNotNull(json);
        assertTrue(json.contains("camel-quarkus-csv"));
    }

    @Test
    public void testLanguageArtifactId() throws Exception {
        String json = catalog.languageJSonSchema("bean");

        assertNotNull(json);
        assertTrue(json.contains("camel-quarkus-bean"));
    }

    @Test
    public void testOtherArtifactId() throws Exception {
        String json = catalog.otherJSonSchema("reactive-executor");

        assertNotNull(json);
        assertTrue(json.contains("camel-quarkus-reactive-executor"));
    }

}
