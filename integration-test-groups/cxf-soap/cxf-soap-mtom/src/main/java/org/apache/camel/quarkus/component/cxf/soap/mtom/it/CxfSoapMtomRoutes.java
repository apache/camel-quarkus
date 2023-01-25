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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.activation.DataHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import jakarta.xml.ws.handler.Handler;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Element;

import com.sun.istack.ByteArrayDataSource;
import io.quarkus.runtime.LaunchMode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CxfPayload;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.staxutils.StaxUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import static org.apache.camel.component.cxf.common.message.CxfConstants.OPERATION_NAME;

@ApplicationScoped
public class CxfSoapMtomRoutes extends RouteBuilder {

    public static final String SERVICE_TYPES_NS = "http://it.mtom.soap.cxf.component.quarkus.camel.apache.org/";
    public static final String XOP_NS = "http://www.w3.org/2004/08/xop/include";

    public static final String RESP_UPLOAD_MSG = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<ns2:uploadImageResponse xmlns:ns2=\"http://it.mtom.soap.cxf.component.quarkus.camel.apache.org/\">" +
            "<return>%s</return>" +
            "</ns2:uploadImageResponse>";

    public static final String RESP_DOWNLOAD_MSG_MTOM_ENABLED = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<ns2:downloadImageResponse xmlns:ns2=\"http://it.mtom.soap.cxf.component.quarkus.camel.apache.org/\">" +
            "<arg0><content><xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\"" +
            " href=\"cid:%s\"/></content></arg0>" +
            "<arg1>%s</arg1>" +
            "</ns2:downloadImageResponse>";
    public static final String RESP_DOWNLOAD_MSG_MTOM_DISABLED = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<ns2:downloadImageResponse xmlns:ns2=\"http://it.mtom.soap.cxf.component.quarkus.camel.apache.org/\">" +
            "<arg0><content>cid:%s</content></arg0>" +
            "<arg1>%s</arg1>" +
            "</ns2:downloadImageResponse>";

    public static final String REQ_UPLOAD_MSG_MTOM_DISABLED = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<ns2:uploadImage xmlns:ns2=\"http://it.mtom.soap.cxf.component.quarkus.camel.apache.org/\">" +
            "<arg0><content>cid:%s</content></arg0>" +
            "<arg1>%s</arg1>" +
            "</ns2:uploadImage>";
    public static final String REQ_UPLOAD_MSG_MTOM_ENABLED = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<ns2:uploadImage xmlns:ns2=\"http://it.mtom.soap.cxf.component.quarkus.camel.apache.org/\">" +
            "<arg0><content><xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\"" +
            " href=\"cid:%s\"/></content></arg0>" +
            "<arg1>%s</arg1>" +
            "</ns2:uploadImage>";
    public static final String REQ_DOWNLOAD_MSG = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<ns2:downloadImage xmlns:ns2=\"http://it.mtom.soap.cxf.component.quarkus.camel.apache.org/\">" +
            "<arg0>%s</arg0>" +
            "</ns2:downloadImage>";

    /**
     * For transfering String response instead of parsing CXFPayload back in CxfSoapMtomResource class.
     */
    public static final String ROUTE_PAYLOAD_MODE_RESULT_HEADER_KEY_NAME = "routeResultPayloadModeHeaderKeyName";

    @Inject
    @Named("loggingMtomFeatureClient")
    LoggingFeature loggingFeature;

    @Override
    public void configure() {

        from("direct:invoker")
                .process(exchange -> {
                    Map<String, Object> headers = exchange.getIn().getHeaders();
                    String endpointDataFormat = headers.get("endpointDataFormat").toString();
                    boolean mtomEnabled = Boolean.parseBoolean(headers.get("mtomEnabled").toString());
                    headers.put("address", getServerUrl() + "/soapservice/mtom-" +
                            (mtomEnabled ? "enabled" : "disabled") + "-" + endpointDataFormat.toLowerCase() +
                            "-mode-image-service");
                    if ("PAYLOAD".equals(endpointDataFormat)) {
                        if ("uploadImage".equals(headers.get(OPERATION_NAME))) {
                            Object[] reqParams = exchange.getIn().getBody(Object[].class);
                            ImageFile image = (ImageFile) reqParams[0];
                            String imageName = (String) reqParams[1];
                            List<Source> elements = new ArrayList<>();
                            String reqMessage = mtomEnabled ? REQ_UPLOAD_MSG_MTOM_ENABLED : REQ_UPLOAD_MSG_MTOM_DISABLED;
                            elements.add(new DOMSource(StaxUtils
                                    .read(new StringReader(String.format(reqMessage, imageName, imageName)))
                                    .getDocumentElement()));
                            CxfPayload payload = new CxfPayload<>(
                                    new ArrayList<SoapHeader>(), elements, null);
                            exchange.getIn().setBody(payload);
                            exchange.getIn(AttachmentMessage.class).addAttachment(imageName,
                                    new DataHandler(new ByteArrayDataSource(image.getContent(), "application/octet-stream")));
                        } else if ("downloadImage".equals(headers.get(OPERATION_NAME))) {
                            Object[] reqParams = exchange.getIn().getBody(Object[].class);
                            String imageName = (String) reqParams[0];
                            List<Source> elements = new ArrayList<>();
                            elements.add(
                                    new DOMSource(StaxUtils.read(new StringReader(String.format(REQ_DOWNLOAD_MSG, imageName)))
                                            .getDocumentElement()));
                            CxfPayload payload = new CxfPayload<>(
                                    new ArrayList<SoapHeader>(), elements, null);
                            exchange.getIn().setBody(payload);
                        }
                    }
                })
                .choice().when(simple("${header.mtomEnabled} == 'true'"))
                .toD("cxf:bean:soapClientMtomEnabledEndpoint?address=${header.address}&mtomEnabled=${header.mtomEnabled}&dataFormat=${header.endpointDataFormat}")
                .otherwise()
                .toD("cxf:bean:soapClientMtomDisabledEndpoint?address=${header.address}&mtomEnabled=${header.mtomEnabled}&dataFormat=${header.endpointDataFormat}");

        from("cxf:bean:soapMtomEnabledServerPojoModeEndpoint?dataFormat=POJO")
                .to("direct:pojoModeProcessor");

        from("cxf:bean:soapMtomDisabledServerPojoModeEndpoint?dataFormat=POJO")
                .to("direct:pojoModeProcessor");

        from("direct:pojoModeProcessor")
                .process("pojoModeProcessor")
                .toD("bean:imageService?method=${header.operationName}");

        from("cxf:bean:soapMtomEnabledServerPayloadModeEndpoint?dataFormat=PAYLOAD")
                .process("payloadModeProcessor");

        from("cxf:bean:soapMtomDisabledServerPayloadModeEndpoint?dataFormat=PAYLOAD")
                .process("payloadModeProcessor");

    }

    @ApplicationScoped
    @Named("payloadModeProcessor")
    static class PayloadModeProcessor implements Processor {

        @Inject
        @Named("imageService")
        ImageService imageService;

        @Override
        public void process(Exchange exchange) throws Exception {
            CxfPayload<SoapHeader> in = exchange.getIn().getBody(CxfPayload.class);
            String operation = in.getBody().get(0).getLocalName();
            if ("uploadImage".equals(operation)) {
                Map<String, String> ns = new HashMap<>();
                ns.put("ns2", SERVICE_TYPES_NS);
                ns.put("xop", XOP_NS);

                XPathUtils xu = new XPathUtils(ns);
                Element body = new XmlConverter().toDOMElement(in.getBody().get(0));
                Element ele = (Element) xu.getValue("//ns2:uploadImage/arg1", body,
                        XPathConstants.NODE);
                String imageName = ele.getTextContent();
                DataHandler dr = exchange.getIn(AttachmentMessage.class).getAttachment(imageName);
                String uploadStatus = imageService.uploadImage(
                        new ImageFile(IOUtils.readBytesFromStream(dr.getInputStream())), imageName);
                List<Source> elements = new ArrayList<>();
                elements.add(new DOMSource(StaxUtils.read(new StringReader(String.format(RESP_UPLOAD_MSG, uploadStatus)))
                        .getDocumentElement()));
                CxfPayload payload = new CxfPayload<>(
                        new ArrayList<SoapHeader>(), elements, null);
                exchange.getIn().setBody(payload);
                // We have correctly uploaded the image, so we can put the upload status in the header, so we don't mess with CXFPayload in CxfSoapMtomResource
                exchange.getIn().setHeader(ROUTE_PAYLOAD_MODE_RESULT_HEADER_KEY_NAME, uploadStatus);
            } else if ("downloadImage".equals(operation)) {
                Map<String, String> ns = new HashMap<>();
                ns.put("ns2", SERVICE_TYPES_NS);
                ns.put("xop", XOP_NS);

                XPathUtils xu = new XPathUtils(ns);
                Element body = new XmlConverter().toDOMElement(in.getBody().get(0));
                Element ele = (Element) xu.getValue("//ns2:downloadImage/arg0", body,
                        XPathConstants.NODE);
                String imageName = ele.getTextContent();
                List<Source> elements = new ArrayList<>();
                boolean mtomEnabled = Boolean.parseBoolean(exchange.getIn().getHeaders().get("mtomEnabled").toString());
                String respMessage = mtomEnabled ? RESP_DOWNLOAD_MSG_MTOM_ENABLED : RESP_DOWNLOAD_MSG_MTOM_DISABLED;
                elements.add(
                        new DOMSource(
                                StaxUtils.read(new StringReader(String.format(respMessage, imageName, imageName)))
                                        .getDocumentElement()));
                ImageFile imageFile = imageService.downloadImage(imageName);
                CxfPayload payload = new CxfPayload<>(
                        new ArrayList<SoapHeader>(), elements, null);
                exchange.getIn().setBody(payload);
                exchange.getIn(AttachmentMessage.class).addAttachment(imageName, new DataHandler(
                        new ByteArrayDataSource(imageFile.getContent(), "application/octet-stream")));
            }

        }
    }

    @ApplicationScoped
    @Named("pojoModeProcessor")
    static class PojoModeProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            String operationName = (String) exchange.getIn().getHeaders().get("operationName");
            MessageContentsList list = (MessageContentsList) exchange.getIn().getBody();
            if ("uploadImage".equals(operationName)) {
                exchange.getIn().getHeaders().put("image", list.get(0));
                exchange.getIn().getHeaders().put("imageName", list.get(1));
                exchange.getIn().getHeaders()
                        .put("operationName", "uploadImage(${header.image},${header.imageName})");
            } else if ("downloadImage".equals(operationName)) {
                exchange.getIn().setBody(list.get(0));
            }
        }

    }

    @Produces
    @ApplicationScoped
    @Named("loggingMtomFeatureClient")
    public LoggingFeature loggingFeature() {
        final LoggingFeature result = new LoggingFeature();
        result.setPrettyLogging(true);
        return result;
    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapClientMtomEnabledEndpoint() {
        return commonCxfEndpoint(true, "");
    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapClientMtomDisabledEndpoint() {
        return commonCxfEndpoint(false, "");
    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapMtomDisabledServerPayloadModeEndpoint() {
        return commonCxfEndpoint(false, "/mtom-disabled-payload-mode-image-service");
    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapMtomEnabledServerPayloadModeEndpoint() {
        return commonCxfEndpoint(true, "/mtom-enabled-payload-mode-image-service");
    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapMtomEnabledServerPojoModeEndpoint() {
        return commonCxfEndpoint(true, "/mtom-enabled-pojo-mode-image-service");
    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapMtomDisabledServerPojoModeEndpoint() {
        return commonCxfEndpoint(false, "/mtom-disabled-pojo-mode-image-service");
    }

    CxfEndpoint commonCxfEndpoint(boolean mtomEnabled, String address) {
        final CxfEndpoint result = new CxfEndpoint();
        result.getFeatures().add(loggingFeature);
        result.setServiceClass(IImageService.class);
        result.setMtomEnabled(mtomEnabled);
        result.setAddress(address);
        List<Handler> handlers = new ArrayList<>();
        handlers.add(new MtomAttachmentChecker(mtomEnabled));
        result.setHandlers(handlers);
        return result;
    }

    private static String getServerUrl() {
        Config config = ConfigProvider.getConfig();
        final int port = LaunchMode.current().equals(LaunchMode.TEST) ? config.getValue("quarkus.http.test-port", Integer.class)
                : config.getValue("quarkus.http.port", Integer.class);
        return String.format("http://localhost:%d", port);
    }

}
