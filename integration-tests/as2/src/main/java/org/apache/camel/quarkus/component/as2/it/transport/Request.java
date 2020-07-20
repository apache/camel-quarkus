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
package org.apache.camel.quarkus.component.as2.it.transport;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.component.as2.api.AS2Charset;
import org.apache.camel.component.as2.api.AS2EncryptionAlgorithm;
import org.apache.camel.component.as2.api.AS2MediaType;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.quarkus.component.as2.it.As2CertificateHelper;
import org.apache.http.entity.ContentType;

public class Request {

    private AS2MessageStructure messageStructure;
    private String messageStructureKey;
    private ContentType contentType;
    private String contentTypeKey;
    private Map<String, Object> headers = new HashMap<>();
    private String ediMessage;
    private AS2EncryptionAlgorithm encryptionAlgorithm;

    public Request() {
    }

    public Request(String ediMessage) {
        this.ediMessage = ediMessage;
    }

    public AS2MessageStructure getMessageStructure() {
        return messageStructure;
    }

    public void setMessageStructure(AS2MessageStructure messageStructure) {
        this.messageStructure = messageStructure;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void addField(String key, Object value) {
        this.headers.put(key, value);
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public String getMessageStructureKey() {
        return messageStructureKey;
    }

    public void setMessageStructureKey(String messageStructureKey) {
        this.messageStructureKey = messageStructureKey;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getContentTypeKey() {
        return contentTypeKey;
    }

    public void setContentTypeKey(String contentTypeKey) {
        this.contentTypeKey = contentTypeKey;
    }

    public String getEdiMessage() {
        return ediMessage;
    }

    public void setEdiMessage(String ediMessage) {
        this.ediMessage = ediMessage;
    }

    public Request withHeaders(Map<String, Object> headers) {
        this.headers = headers;
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            if (entry.getValue() instanceof AS2MessageStructure) {
                setMessageStructure((AS2MessageStructure) entry.getValue());
                setMessageStructureKey(entry.getKey());
            }
        }
        return this;
    }

    public Map<String, Object> collectHeaders() {
        Map<String, Object> retVal = new HashMap<>(headers);
        if (getMessageStructure() != null) {
            retVal.put(getMessageStructureKey(), getMessageStructure());
        }
        retVal.put("CamelAS2.ediMessageContentType",
                org.apache.http.entity.ContentType.create(AS2MediaType.APPLICATION_EDIFACT, AS2Charset.US_ASCII));
        if (getEncryptionAlgorithm() != null) {
            retVal.put("CamelAS2.encryptingCertificateChain", As2CertificateHelper.getCertList());
            retVal.put("CamelAS2.encryptingAlgorithm", getEncryptionAlgorithm());
        }
        return retVal;
    }

    public AS2EncryptionAlgorithm getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(AS2EncryptionAlgorithm encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }
}
