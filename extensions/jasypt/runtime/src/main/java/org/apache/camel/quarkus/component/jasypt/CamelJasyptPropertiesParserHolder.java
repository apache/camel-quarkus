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
package org.apache.camel.quarkus.component.jasypt;

import org.apache.camel.component.jasypt.JasyptPropertiesParser;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

class CamelJasyptPropertiesParserHolder {
    private static volatile JasyptPropertiesParser INSTANCE;
    private static volatile StandardPBEStringEncryptor ENCRYPTOR;

    private CamelJasyptPropertiesParserHolder() {
        // Utility class
    }

    static JasyptPropertiesParser getJasyptPropertiesParser() {
        if (INSTANCE == null) {
            synchronized (CamelJasyptPropertiesParserHolder.class) {
                if (INSTANCE == null) {
                    INSTANCE = new JasyptPropertiesParser();
                }
            }
        }
        return INSTANCE;
    }

    static void setEncryptor(StandardPBEStringEncryptor encryptor) {
        synchronized (CamelJasyptPropertiesParserHolder.class) {
            getJasyptPropertiesParser().setEncryptor(encryptor);
            ENCRYPTOR = encryptor;
        }
    }
}
