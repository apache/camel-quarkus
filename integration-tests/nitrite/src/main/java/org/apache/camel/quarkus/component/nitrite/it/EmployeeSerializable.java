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
package org.apache.camel.quarkus.component.nitrite.it;

import java.util.Date;

import org.dizitart.no2.IndexType;
import org.dizitart.no2.objects.Id;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;

@Indices({
        @Index(value = "address", type = IndexType.NonUnique),
        @Index(value = "name", type = IndexType.Unique)
})
public class EmployeeSerializable extends Employee {

    @Id
    private long empId;

    public EmployeeSerializable() {
    }

    public EmployeeSerializable(long empId, Date joinDate, String name, String address) {
        super(empId, joinDate, name, address);
    }

    @Override
    public long getEmpId() {
        return empId;
    }

    @Override
    public void setEmpId(long empId) {
        this.empId = empId;
    }

}
