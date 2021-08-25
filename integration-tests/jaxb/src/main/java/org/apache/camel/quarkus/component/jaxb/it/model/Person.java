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
package org.apache.camel.quarkus.component.jaxb.it.model;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "firstName", "lastName", "age" })
@XmlRootElement(name = "person")
public class Person {

    @XmlElement(required = true, namespace = "http://example.com/a")
    protected String firstName = "John";
    @XmlElement(required = true, namespace = "http://example.com/a")
    protected String lastName = "Doe";
    @XmlElement(required = true, type = Integer.class, nillable = true)
    protected Integer age = 33;

    /**
     * Gets the value of the firstName property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the value of the firstName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFirstName(String value) {
        this.firstName = value;
    }

    /**
     * Gets the value of the lastName property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the value of the lastName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLastName(String value) {
        this.lastName = value;
    }

    /**
     * Gets the value of the age property.
     *
     * @return possible object is
     *         {@link Integer }
     */
    public Integer getAge() {
        return age;
    }

    /**
     * Sets the value of the age property.
     *
     * @param value allowed object is
     *              {@link Integer }
     */
    public void setAge(Integer value) {
        this.age = value;
    }

    public Person withFirstName(String value) {
        setFirstName(value);
        return this;
    }

    public Person withLastName(String value) {
        setLastName(value);
        return this;
    }

    public Person withAge(Integer value) {
        setAge(value);
        return this;
    }

    @Override
    public String toString() {
        return "Person{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", age=" + age +
                '}';
    }
}
