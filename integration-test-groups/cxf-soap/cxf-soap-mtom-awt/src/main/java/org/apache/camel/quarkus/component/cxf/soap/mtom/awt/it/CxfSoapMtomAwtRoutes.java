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
package org.apache.camel.quarkus.component.cxf.soap.mtom.awt.it;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.ws.handler.Handler;

import io.quarkus.runtime.LaunchMode;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.message.MessageContentsList;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class CxfSoapMtomAwtRoutes extends RouteBuilder {

    @Inject
    @Named("loggingMtomAwtFeatureClient")
    LoggingFeature loggingFeature;

    @Override
    public void configure() {

        from("direct:mtomAwtEnabledInvoker")
                .to("cxf:bean:soapMtomAwtEnabledClientEndpoint?dataFormat=POJO");

        from("direct:mtomAwtDisabledInvoker")
                .to("cxf:bean:soapMtomAwtDisabledClientEndpoint?dataFormat=POJO");

        from("cxf:bean:soapMtomAwtEnabledServerEndpoint?dataFormat=POJO")
                .to("direct:processAwtImage");

        from("cxf:bean:soapMtomAwtDisabledServerEndpoint?dataFormat=POJO")
                .to("direct:processAwtImage");

        from("direct:processAwtImage")
                .process(exchange -> {
                    String operationName = (String) exchange.getIn().getHeaders().get("operationName");
                    MessageContentsList list = exchange.getIn().getBody(MessageContentsList.class);
                    if ("uploadImage".equals(operationName)) {
                        exchange.getIn().getHeaders().put("image", list.get(0));
                        exchange.getIn().getHeaders().put("imageName", list.get(1));
                        exchange.getIn().getHeaders()
                                .put("operationName", "uploadImage(${header.image},${header.imageName})");
                    } else if ("downloadImage".equals(operationName)) {
                        exchange.getIn().setBody(list.get(0));
                    }
                })
                .recipientList((simple("bean:imageAwtService?method=${header.operationName}")));

    }

    @Produces
    @ApplicationScoped
    @Named("loggingMtomAwtFeatureClient")
    public LoggingFeature loggingFeature() {
        final LoggingFeature result = new LoggingFeature();
        result.setPrettyLogging(true);
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint soapMtomAwtEnabledClientEndpoint() {
        return commonCxfEndpoint(true, getServerUrl() + "/soapservice/mtom-awt-enabled-image-service");
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint soapMtomAwtDisabledClientEndpoint() {
        return commonCxfEndpoint(false, getServerUrl() + "/soapservice/mtom-awt-disabled-image-service");
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint soapMtomAwtEnabledServerEndpoint() {
        return commonCxfEndpoint(true, "/mtom-awt-enabled-image-service");
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint soapMtomAwtDisabledServerEndpoint() {
        return commonCxfEndpoint(false, "/mtom-awt-disabled-image-service");
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
