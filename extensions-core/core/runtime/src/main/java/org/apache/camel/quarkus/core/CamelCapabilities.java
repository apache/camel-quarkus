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
package org.apache.camel.quarkus.core;

public final class CamelCapabilities {
    public static final String BEAN = "org.apache.camel.bean";
    public static final String CORE = "org.apache.camel";
    public static final String XML = "org.apache.camel.xml";
    public static final String XML_IO_DSL = "org.apache.camel.xml.io.dsl";
    public static final String XML_JAXB = "org.apache.camel.xml.jaxb";
    public static final String XML_JAXP = "org.apache.camel.xml.jaxp";

    private CamelCapabilities() {
    }
}
