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
package org.apache.camel.quarkus.component.xml.jaxp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.xml.BytesSource;

public class XmlJaxpRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:documentConvert")
                .convertBodyTo(Document.class)
                .process(exchange -> {
                    Document document = exchange.getMessage().getBody(Document.class);
                    Element element = document.createElement("bar");
                    element.setTextContent("Bar Text");
                    document.getDocumentElement().appendChild(element);
                })
                .convertBodyTo(String.class);

        from("direct:elementConvert")
                .convertBodyTo(Document.class)
                .process(exchange -> {
                    Document document = exchange.getMessage().getBody(Document.class);
                    Element element = document.createElement("bar");
                    element.setTextContent("Bar Text");
                    document.getDocumentElement().appendChild(element);
                })
                .convertBodyTo(Element.class)
                .convertBodyTo(String.class);

        from("direct:byteSourceConvert")
                .convertBodyTo(byte[].class)
                .convertBodyTo(BytesSource.class)
                .convertBodyTo(String.class);

        from("direct:sourceConvert")
                .convertBodyTo(Source.class)
                .convertBodyTo(String.class);

        from("direct:domSourceConvert")
                .convertBodyTo(byte[].class)
                .convertBodyTo(DOMSource.class)
                .convertBodyTo(String.class);

        from("direct:saxSourceConvert")
                .convertBodyTo(SAXSource.class)
                .convertBodyTo(String.class);

        from("direct:staxSourceConvert")
                .convertBodyTo(StAXSource.class)
                .convertBodyTo(String.class);

        from("direct:streamSourceConvert")
                .convertBodyTo(StreamSource.class)
                .convertBodyTo(String.class);

        from("direct:streamSourceReaderConvert")
                .convertBodyTo(StreamSource.class)
                .convertBodyTo(Reader.class)
                .convertBodyTo(String.class);

        from("direct:xmlStreamReaderCharsetConvert")
                .convertBodyTo(InputStream.class, StandardCharsets.ISO_8859_1.name())
                .convertBodyTo(XMLStreamReader.class)
                .process(exchange -> {
                    Message message = exchange.getMessage();
                    XMLStreamReader reader = message.getBody(XMLStreamReader.class);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    exchange.setProperty(Exchange.CHARSET_NAME, StandardCharsets.UTF_8.name());
                    XMLStreamWriter writer = exchange.getContext().getTypeConverter().mandatoryConvertTo(XMLStreamWriter.class,
                            exchange, output);
                    while (reader.hasNext()) {
                        reader.next();
                        switch (reader.getEventType()) {
                        case XMLStreamConstants.START_DOCUMENT:
                            writer.writeStartDocument();
                            break;
                        case XMLStreamConstants.END_DOCUMENT:
                            writer.writeEndDocument();
                            break;
                        case XMLStreamConstants.START_ELEMENT:
                            writer.writeStartElement(reader.getName().getLocalPart());
                            break;
                        case XMLStreamConstants.CHARACTERS:
                            writer.writeCharacters(reader.getText());
                            break;
                        case XMLStreamConstants.END_ELEMENT:
                            writer.writeEndElement();
                            break;
                        default:
                            break;
                        }
                    }
                    reader.close();
                    writer.close();
                    message.setBody(output);
                })
                .convertBodyTo(String.class, StandardCharsets.UTF_8.name());

        from("direct:nodeListConvert")
                .convertBodyTo(Document.class)
                .process(exchange -> {
                    Message message = exchange.getMessage();
                    Document document = message.getBody(Document.class);
                    NodeList nodeList = document.getElementsByTagName("foo");
                    message.setBody(nodeList);
                })
                .convertBodyTo(Node.class)
                .convertBodyTo(String.class);
    }
}
