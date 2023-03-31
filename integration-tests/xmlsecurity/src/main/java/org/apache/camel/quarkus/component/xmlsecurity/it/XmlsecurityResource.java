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
package org.apache.camel.quarkus.component.xmlsecurity.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/xmlsecurity")
public class XmlsecurityResource {

    @Inject
    ProducerTemplate template;

    @Path("/component/sign/enveloping")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String signEnveloping(String xml) throws Exception {
        return template.requestBody("direct:enveloping-sign", xml, String.class);
    }

    @Path("/component/verify/enveloping")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String verifyEnveloping(String xml) throws Exception {
        return template.requestBody("direct:enveloping-verify", xml, String.class);
    }

    @Path("/component/sign/enveloped")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String signEnveloped(String xml) throws Exception {
        return template.requestBody("direct:enveloped-sign", xml, String.class);
    }

    @Path("/component/verify/enveloped")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String verifyEnveloped(String xml) throws Exception {
        return template.requestBody("direct:enveloped-verify", xml, String.class);
    }

    @Path("/component/sign/plaintext")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String signPlainText(String xml) throws Exception {
        return template.requestBody("direct:plaintext-sign", xml, String.class);
    }

    @Path("/component/verify/plaintext")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String verifyPlainText(String xml) throws Exception {
        return template.requestBody("direct:plaintext-verify", xml, String.class);
    }

    @Path("/component/sign/canonicalization")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String signCanonicalization(String xml) throws Exception {
        return template.requestBody("direct:canonicalization-sign", xml, String.class);
    }

    @Path("/component/verify/canonicalization")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String verifyCanonicalization(String xml) throws Exception {
        return template.requestBody("direct:canonicalization-verify", xml, String.class);

    }

    @Path("/component/sign/signaturedigest")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String signSignatureAndDigestAlgorithm(String xml) throws Exception {
        return template.requestBody("direct:signaturedigestalgorithm-sign", xml, String.class);

    }

    @Path("/component/verify/signaturedigest")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String verifySignatureAndDigestAlgorithm(String xml) throws Exception {
        return template.requestBody("direct:signaturedigestalgorithm-verify", xml, String.class);
    }

    @Path("/component/sign/transformsxpath")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String signSignatureTransformsXPath(String xml) throws Exception {
        return template.requestBody("direct:transformsXPath-sign", xml, String.class);
    }

    @Path("/component/verify/transformsxpath")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String verifySignatureTransformsXPath(String xml) throws Exception {
        return template.requestBody("direct:transformsXPath-verify", xml, String.class);
    }

    @Path("/component/sign/transformsxsltxpath")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String signSignatureTransformsXsltXPath(String xml) throws Exception {
        return template.requestBody("direct:transformsXsltXPath-sign", xml, String.class);
    }

    @Path("/component/verify/transformsxsltxpath")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String verifySignatureTransformsXsltXPath(String xml) throws Exception {
        return template.requestBody("direct:transformsXsltXPath-verify", xml, String.class);
    }

    @Path("/dataformat/marshal")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String dataformatMarshal(String xml) throws Exception {
        return template.requestBody("direct:marshal", xml, String.class);
    }

    @Path("/dataformat/unmarshal")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String dataformatUnmarshal(String xml) throws Exception {
        return template.requestBody("direct:unmarshal", xml, String.class);
    }

    @Path("/dataformat/marshal/partial")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String dataformatMarshalPartialContent(String xml) throws Exception {
        return template.requestBody("direct:marshal-partial", xml, String.class);
    }

    @Path("/dataformat/unmarshal/partial")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String dataformatUnmarshalPartialContent(String xml) throws Exception {
        return template.requestBody("direct:unmarshal-partial", xml, String.class);
    }
}
