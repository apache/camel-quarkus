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
package org.apache.camel.quarkus.component.dozer.it.model;

import java.io.File;
import java.net.URL;
import java.util.Date;

public class CustomerB {

    private String firstName;
    private String lastName;
    private Address address;
    private Date created;
    private URL internalUrl;
    private File internalFile;
    private String internalFileAsString;
    private Class internalClass;
    private String internalClassAsString;
    private Custom internal;

    public CustomerB() {
    }

    public CustomerB(String firstName, String lastName, Address address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    public void setInternalsAsString() {
        if (internalClass != null) {
            internalClassAsString = internalClass.getCanonicalName();
        }
        if (internalFile != null) {
            internalFileAsString = internalFile.toString();
        }
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public URL getInternalUrl() {
        return internalUrl;
    }

    public void setInternalUrl(URL internalUrl) {
        this.internalUrl = internalUrl;
    }

    public void setInternalFile(File internalFile) {
        this.internalFile = internalFile;
    }

    public void setInternalClass(Class internalClass) {
        this.internalClass = internalClass;
    }

    public String getInternalFileAsString() {
        return internalFileAsString;
    }

    public String getInternalClassAsString() {
        return internalClassAsString;
    }

    public Custom getInternal() {
        return internal;
    }

    public void setInternal(Custom internal) {
        this.internal = internal;
    }

    public static class Address {

        private String street;
        private String zip;

        public Address() {
        }

        public Address(String street, String zip) {
            this.street = street;
            this.zip = zip;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }
    }

    public static class Custom {

        private String text;

        public Custom(String text) {
            this.text = "hello " + text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

    }

}
