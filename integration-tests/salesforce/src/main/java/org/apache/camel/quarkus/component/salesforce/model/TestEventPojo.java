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
package org.apache.camel.quarkus.component.salesforce.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TestEventPojo {

    private String CreatedById;
    private String Test_Field__c;
    private long CreatedDate;

    public String getCreatedById() {
        return CreatedById;
    }

    public void setCreatedById(String createdById) {
        CreatedById = createdById;
    }

    public String getTest_Field__c() {
        return Test_Field__c;
    }

    public void setTest_Field__c(String test_Field__c) {
        Test_Field__c = test_Field__c;
    }

    public long getCreatedDate() {
        return CreatedDate;
    }

    public void setCreatedDate(long createdDate) {
        CreatedDate = createdDate;
    }

    @Override
    public String toString() {
        return "TestEventPojo{" +
                "CreatedById='" + CreatedById + '\'' +
                ", Test_Field__c='" + Test_Field__c + '\'' +
                ", CreatedDate=" + CreatedDate +
                '}';
    }
}
