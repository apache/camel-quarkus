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

import java.io.Serializable;
import java.util.Date;

public abstract class Employee implements Serializable, Cloneable {

    private Date joinDate;

    private String name;

    private String address;

    public Employee() {
    }

    public Employee(long empId, Date joinDate, String name, String address) {
        setEmpId(empId);
        this.joinDate = joinDate;
        this.name = name;
        this.address = address;
    }

    public abstract long getEmpId();

    public abstract void setEmpId(long empId);

    public Date getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "empId=" + getEmpId() +
                ", joinDate=" + joinDate +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
