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
package org.apache.camel.quarkus.component.headersmap.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.component.headersmap.FastHeadersMapFactory;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class HeadersmapTest {

    //@Test
    public void fastHeadersMapFactoryIsConfigured() {
        RestAssured.get("/headersmap/get")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    //@Test
    public void testLookupCaseAgnostic() {
        Map<String, Object> map = new FastHeadersMapFactory().newMap();
        assertNull(map.get("foo"));

        map.put("foo", "bar");

        assertEquals("bar", map.get("foo"));
        assertEquals("bar", map.get("Foo"));
        assertEquals("bar", map.get("FOO"));
    }

    //@Test
    public void testConstructFromOther() {
        Map<String, Object> other = new FastHeadersMapFactory().newMap();
        other.put("Foo", "bar");
        other.put("other", 123);

        Map<String, Object> map = new FastHeadersMapFactory().newMap(other);

        assertEquals("bar", map.get("FOO"));
        assertEquals("bar", map.get("foo"));
        assertEquals("bar", map.get("Foo"));

        assertEquals(123, map.get("OTHER"));
        assertEquals(123, map.get("other"));
        assertEquals(123, map.get("OthEr"));
    }

    //@Test
    public void testIsInstance() {
        Map<String, Object> map = new FastHeadersMapFactory().newMap();
        Map<String, Object> other = new FastHeadersMapFactory().newMap(map);
        other.put("Foo", "bar");
        other.put("other", 123);

        assertTrue(new FastHeadersMapFactory().isInstanceOf(map));
        assertTrue(new FastHeadersMapFactory().isInstanceOf(other));
        assertFalse(new FastHeadersMapFactory().isInstanceOf(new HashMap<>()));
    }

}
