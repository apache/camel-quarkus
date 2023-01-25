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
package org.apache.camel.quarkus.component.soap.it.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.datatype.XMLGregorianCalendar;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "customer", propOrder = {
        "name",
        "address",
        "numOrders",
        "revenue",
        "test",
        "birthDate",
        "type"
})
public class Customer {

    protected String name;
    @XmlElement(nillable = true)
    protected List<String> address;
    protected int numOrders;
    protected double revenue;
    protected BigDecimal test;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar birthDate;
    @XmlSchemaType(name = "string")
    protected CustomerType type;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public List<String> getAddress() {
        if (address == null) {
            address = new ArrayList<>();
        }
        return this.address;
    }

    public int getNumOrders() {
        return numOrders;
    }

    public void setNumOrders(int value) {
        this.numOrders = value;
    }

    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double value) {
        this.revenue = value;
    }

    public BigDecimal getTest() {
        return test;
    }

    public void setTest(BigDecimal value) {
        this.test = value;
    }

    public XMLGregorianCalendar getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(XMLGregorianCalendar value) {
        this.birthDate = value;
    }

    public CustomerType getType() {
        return type;
    }

    public void setType(CustomerType value) {
        this.type = value;
    }

}
