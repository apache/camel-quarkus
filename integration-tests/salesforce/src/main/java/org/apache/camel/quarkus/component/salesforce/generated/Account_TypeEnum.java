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
package org.apache.camel.quarkus.component.salesforce.generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Generated;

/**
 * Salesforce Enumeration DTO for picklist Type
 */
@Generated("org.apache.camel.maven.CamelSalesforceMojo")
public enum Account_TypeEnum {

    // Channel Partner / Reseller
    CHANNEL_PARTNER___RESELLER("Channel Partner / Reseller"),

    // Customer - Channel
    CUSTOMER___CHANNEL("Customer - Channel"),

    // Customer - Direct
    CUSTOMER___DIRECT("Customer - Direct"),

    // Installation Partner
    INSTALLATION_PARTNER("Installation Partner"),

    // Other
    OTHER("Other"),

    // Prospect
    PROSPECT("Prospect"),

    // Technology Partner
    TECHNOLOGY_PARTNER("Technology Partner");

    final String value;

    private Account_TypeEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static Account_TypeEnum fromValue(String value) {
        for (Account_TypeEnum e : Account_TypeEnum.values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException(value);
    }
}
