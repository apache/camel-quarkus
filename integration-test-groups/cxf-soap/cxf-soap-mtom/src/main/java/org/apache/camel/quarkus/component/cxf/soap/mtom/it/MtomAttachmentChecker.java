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
package org.apache.camel.quarkus.component.cxf.soap.mtom.it;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Asserts whether the attachment is present where expected (inline or multipart body) throws an IllegalStateException
 * to signal that something is wrong.
 */
public class MtomAttachmentChecker implements SOAPHandler<SOAPMessageContext> {
    boolean mtomEnabled;

    public MtomAttachmentChecker(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    static boolean walk(String localName, NodeList nodes) {
        boolean found = false;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (localName.equals(n.getLocalName())) {
                found = true;
                break;
            } else if (!found) {
                found = walk(localName, n.getChildNodes());
            }
        }

        return found;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext msgContext) {
        try {
            SOAPEnvelope envelope = msgContext.getMessage().getSOAPPart().getEnvelope();
            SOAPBody body = envelope.getBody();
            boolean found = walk("Include", body.getChildNodes());
            if (mtomEnabled) {
                // skip those messages which don't have attachments
                boolean skip = walk("uploadImageResponse", body.getChildNodes()) || walk("downloadImage", body.getChildNodes());
                if (!skip && !found) {
                    throw new IllegalStateException("The SOAP message should contain an <xop:Include> element");
                }
            } else if (found) {
                throw new IllegalStateException("The SOAP message shouldn't contain an <xop:Include> element");
            }

        } catch (SOAPException ex) {
            throw new WebServiceException(ex);
        }

        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }
}
