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
package org.apache.camel.quarkus.component.bindy.it.model;

import java.util.List;

import org.apache.camel.dataformat.bindy.annotation.KeyValuePairField;
import org.apache.camel.dataformat.bindy.annotation.Link;
import org.apache.camel.dataformat.bindy.annotation.Message;
import org.apache.camel.dataformat.bindy.annotation.OneToMany;

@Message(keyValuePairSeparator = "=", pairSeparator = "\\u0001")
public class MessageOrder {

    @Link
    Header header;

    @Link
    Trailer trailer;

    @KeyValuePairField(tag = 1)
    private String account;

    @KeyValuePairField(tag = 58)
    private String text;

    @OneToMany(mappedTo = "org.apache.camel.quarkus.component.bindy.it.model.Security")
    private List<Security> securities;

    public List<Security> getSecurities() {
        return securities;
    }

    public void setSecurities(List<Security> securities) {
        this.securities = securities;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Trailer getTrailer() {
        return trailer;
    }

    public void setTrailer(Trailer trailer) {
        this.trailer = trailer;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(MessageOrder.class.getName() + " --> 1: " + this.account + ", 58: " + this.text + System.lineSeparator());

        if (this.header != null) {
            sb.append("  " + this.header.toString() + System.lineSeparator());
        }

        if (this.securities != null) {
            for (Security sec : this.securities) {
                sb.append("  " + sec.toString() + System.lineSeparator());
            }
        }

        if (this.trailer != null) {
            sb.append("  " + this.trailer.toString() + System.lineSeparator());
        }

        return sb.toString();
    }

}
