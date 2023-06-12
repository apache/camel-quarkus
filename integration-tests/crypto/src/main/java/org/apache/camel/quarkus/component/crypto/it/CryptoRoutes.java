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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.crypto.DigitalSignatureConstants;
import org.apache.camel.converter.crypto.CryptoDataFormat;

public class CryptoRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Crypto component using raw keys
        final KeyPair keys = getKeyPair();
        from("direct:sign-raw")
                .setHeader(DigitalSignatureConstants.SIGNATURE_PRIVATE_KEY, constant(keys.getPrivate()))
                .to("crypto:sign:raw");

        from("direct:verify-raw")
                .setHeader(DigitalSignatureConstants.SIGNATURE_PUBLIC_KEY_OR_CERT, constant(keys.getPublic()))
                .to("crypto:verify:raw");

        // Crypto component using keys from a keystore
        from("direct:sign")
                .to("crypto:sign:basic?privateKey=#myPrivateKey&algorithm=SHA1withDSA&provider=SUN&secureRandom=#customSecureRandom");

        from("direct:verify")
                .to("crypto:verify:basic?publicKey=#myPublicKey&algorithm=SHA1withDSA&provider=SUN&secureRandom=#customSecureRandom");

        // Crypto data format
        CryptoDataFormat cryptoDataFormat = getCryptoDataFormat();
        from("direct:marshal")
                .marshal(cryptoDataFormat);

        from("direct:unmarshal")
                .unmarshal(cryptoDataFormat);

        // PGP data format
        from("direct:marshalPgp")
                .marshal().pgp("pubring.pgp", "sdude@nowhere.net", "sdude");

        from("direct:unmarshalPgp")
                .unmarshal().pgp("secring.pgp", "sdude@nowhere.net", "sdude");
    }

    private CryptoDataFormat getCryptoDataFormat() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("DES");
        return new CryptoDataFormat("DES", generator.generateKey());
    }

    private KeyPair getKeyPair() throws NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }

}
