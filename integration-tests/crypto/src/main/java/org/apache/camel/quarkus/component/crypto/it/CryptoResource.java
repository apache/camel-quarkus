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
package org.apache.camel.quarkus.component.crypto.it;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.crypto.DigitalSignatureConstants;

@Path("/crypto")
public class CryptoResource {

    public static final String MESSAGE = "Hello Camel Quarkus Crypto";
    private static final String ALIAS = "bob";
    private static final String KEYSTORE = "crypto.jks";
    private static final String KEYSTORE_PASSWORD = "letmein";

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/signature/sign")
    @POST
    public byte[] sign() {
        Exchange exchange = producerTemplate.request("direct:sign", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setBody(MESSAGE);
            }
        });
        Message message = exchange.getMessage();
        return message.getHeader(DigitalSignatureConstants.SIGNATURE, byte[].class);
    }

    @Path("/signature/verify")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void verify(String signature) {
        producerTemplate.sendBodyAndHeader("direct:verify", MESSAGE, DigitalSignatureConstants.SIGNATURE, signature);
    }

    @Path("/encrypt")
    @POST
    public byte[] encryptPayload(String payload) {
        return producerTemplate.requestBody("direct:marshal", payload, byte[].class);
    }

    @Path("/decrypt")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String decryptPgpPayload(byte[] payload) {
        return producerTemplate.requestBody("direct:unmarshal", payload, String.class);
    }

    @Path("/encrypt/pgp")
    @POST
    public byte[] encryptPgpPayload(String payload) {
        return producerTemplate.requestBody("direct:marshalPgp", payload, byte[].class);
    }

    @Path("/decrypt/pgp")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String decryptPayload(byte[] payload) {
        return producerTemplate.requestBody("direct:unmarshalPgp", payload, String.class);
    }

    @javax.enterprise.inject.Produces
    public KeyStore keyStore() throws Exception {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = getClass().getResourceAsStream("/" + KEYSTORE)) {
            keystore.load(in, KEYSTORE_PASSWORD.toCharArray());
        }
        return keystore;
    }

    @javax.enterprise.inject.Produces
    @Named
    public PrivateKey myPrivateKey(KeyStore keyStore) throws Exception {
        return (PrivateKey) keyStore.getKey(ALIAS, KEYSTORE_PASSWORD.toCharArray());
    }

    @javax.enterprise.inject.Produces
    @Named
    public PublicKey myPublicKey(KeyStore keyStore) throws Exception {
        Certificate cert = keyStore.getCertificate(ALIAS);
        return cert.getPublicKey();
    }

    @javax.enterprise.inject.Produces
    @Named
    public SecureRandom customSecureRandom() {
        return new SecureRandom();
    }
}
