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
package org.apache.camel.quarkus.component.jaxb.it.writer;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.camel.converter.jaxb.JaxbXmlStreamWriterWrapper;

/**
 * Custom {@link JaxbXmlStreamWriterWrapper} that appends a suffix to all XML tags.
 */
public class CustomXmlStreamWriter implements JaxbXmlStreamWriterWrapper {
    private static final String MODIFIED_XML_TAG_SUFFIX = "-modified";

    @Override
    public XMLStreamWriter wrapWriter(XMLStreamWriter writer) {
        return new XMLStreamWriter() {
            @Override
            public void writeStartElement(String localName) throws XMLStreamException {
                writer.writeStartElement(localName + MODIFIED_XML_TAG_SUFFIX);
            }

            @Override
            public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
                writer.writeStartElement(namespaceURI, localName + MODIFIED_XML_TAG_SUFFIX);

            }

            @Override
            public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
                writer.writeStartElement(prefix, localName + MODIFIED_XML_TAG_SUFFIX, namespaceURI);
            }

            @Override
            public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
                writer.writeEmptyElement(namespaceURI, localName);
            }

            @Override
            public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
                writer.writeEmptyElement(prefix, localName, namespaceURI);
            }

            @Override
            public void writeEmptyElement(String localName) throws XMLStreamException {
                writer.writeEmptyElement(localName);
            }

            @Override
            public void writeEndElement() throws XMLStreamException {
                writer.writeEndElement();
            }

            @Override
            public void writeEndDocument() throws XMLStreamException {
                writer.writeEndDocument();
            }

            @Override
            public void close() throws XMLStreamException {
                writer.close();
            }

            @Override
            public void flush() throws XMLStreamException {
                writer.flush();
            }

            @Override
            public void writeAttribute(String localName, String value) throws XMLStreamException {
                writer.writeAttribute(localName, value);
            }

            @Override
            public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
                    throws XMLStreamException {
                writer.writeAttribute(prefix, namespaceURI, localName, value);

            }

            @Override
            public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
                writer.writeAttribute(namespaceURI, localName, value);
            }

            @Override
            public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
                writer.writeNamespace(prefix, namespaceURI);
            }

            @Override
            public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
                writer.writeDefaultNamespace(namespaceURI);
            }

            @Override
            public void writeComment(String data) throws XMLStreamException {
                writer.writeComment(data);
            }

            @Override
            public void writeProcessingInstruction(String target) throws XMLStreamException {
                writer.writeProcessingInstruction(target);
            }

            @Override
            public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
                writer.writeProcessingInstruction(target, data);

            }

            @Override
            public void writeCData(String data) throws XMLStreamException {
                writer.writeCData(data);
            }

            @Override
            public void writeDTD(String dtd) throws XMLStreamException {
                writer.writeDTD(dtd);
            }

            @Override
            public void writeEntityRef(String name) throws XMLStreamException {
                writer.writeEntityRef(name);
            }

            @Override
            public void writeStartDocument() throws XMLStreamException {
                writer.writeStartDocument();
            }

            @Override
            public void writeStartDocument(String version) throws XMLStreamException {
                writer.writeStartDocument(version);
            }

            @Override
            public void writeStartDocument(String encoding, String version) throws XMLStreamException {
                writer.writeStartDocument(encoding, version);
            }

            @Override
            public void writeCharacters(String text) throws XMLStreamException {
                writer.writeCharacters(text);
            }

            @Override
            public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
                writer.writeCharacters(text, start, len);
            }

            @Override
            public String getPrefix(String uri) throws XMLStreamException {
                return writer.getPrefix(uri);
            }

            @Override
            public void setPrefix(String prefix, String uri) throws XMLStreamException {
                writer.setPrefix(prefix, uri);
            }

            @Override
            public void setDefaultNamespace(String uri) throws XMLStreamException {
                writer.setDefaultNamespace(uri);
            }

            @Override
            public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
                writer.setNamespaceContext(context);
            }

            @Override
            public NamespaceContext getNamespaceContext() {
                return writer.getNamespaceContext();
            }

            @Override
            public Object getProperty(String name) throws IllegalArgumentException {
                return writer.getProperty(name);
            }
        };
    }
}
