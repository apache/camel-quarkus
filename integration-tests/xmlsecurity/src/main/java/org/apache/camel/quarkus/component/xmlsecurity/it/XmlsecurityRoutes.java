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

import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class XmlsecurityRoutes extends RouteBuilder {

    @Inject
    @Named("key")
    SecretKey key;

    @Override
    public void configure() throws Exception {
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("test", "http://test/test");

        from("direct:enveloping-sign")
                .to("xmlsecurity-sign:enveloping?keyAccessor=#accessor");

        from("direct:enveloping-verify")
                .to("xmlsecurity-verify:enveloping?keySelector=#selector");

        from("direct:enveloped-sign")
                .to("xmlsecurity-sign:enveloped?keyAccessor=#accessor&parentLocalName=root");

        from("direct:enveloped-verify")
                .to("xmlsecurity-verify:enveloped?keySelector=#selector");

        from("direct:plaintext-sign")
                .to("xmlsecurity-sign:plaintext?keyAccessor=#accessor&plainText=true&plainTextEncoding=UTF-8");

        from("direct:plaintext-verify")
                .to("xmlsecurity-verify:plaintext?keySelector=#selector");

        from("direct:canonicalization-sign")
                .to("xmlsecurity-sign:canonicalization?keyAccessor=#accessor&canonicalizationMethod=#canonicalizationMethod"
                        + "&signatureAlgorithm=http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");

        from("direct:canonicalization-verify")
                .to("xmlsecurity-verify:canonicalization?keySelector=#selector");

        from("direct:signaturedigestalgorithm-sign")
                .to("xmlsecurity-sign:signaturedigestalgorithm?keyAccessor=#accessor"
                        + "&signatureAlgorithm=http://www.w3.org/2001/04/xmldsig-more#rsa-sha512&digestAlgorithm=http://www.w3.org/2001/04/xmlenc#sha512");

        from("direct:signaturedigestalgorithm-verify")
                .to("xmlsecurity-verify:signaturedigestalgorithm?keySelector=#selector");

        from("direct:transformsXPath-sign")
                .to("xmlsecurity-sign:transformsXPath?keyAccessor=#accessor&transformMethods=#transformsXPath");

        from("direct:transformsXPath-verify")
                .to("xmlsecurity-verify:transformsXPath?keySelector=#selector");

        from("direct:transformsXsltXPath-sign")
                .to("xmlsecurity-sign:transformsXsltXPath?keyAccessor=#accessor&transformMethods=#transformsXsltXPath&cryptoContextProperties=#cryptoContextProperties");

        from("direct:transformsXsltXPath-verify")
                .to("xmlsecurity-verify:transformsXsltXPath?keySelector=#selector&secureValidation=false&cryptoContextProperties=#cryptoContextProperties");

        from("direct:marshal")
                .marshal().xmlSecurity(key.getEncoded());

        from("direct:unmarshal")
                .unmarshal().xmlSecurity(key.getEncoded());

        from("direct:marshal-partial")
                .marshal().xmlSecurity("//root/test:child-2", namespaces, true, key.getEncoded());

        from("direct:unmarshal-partial")
                .unmarshal().xmlSecurity("//root/test:child-2", namespaces, true, key.getEncoded());
    }
}
