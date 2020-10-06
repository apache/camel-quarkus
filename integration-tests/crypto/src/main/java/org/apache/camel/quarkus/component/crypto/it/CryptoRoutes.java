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

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.crypto.CryptoDataFormat;
import org.apache.camel.converter.crypto.PGPDataFormat;

public class CryptoRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Crypto component
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
        PGPDataFormat encrypt = getPgpDataFormat("pubring.pgp");
        PGPDataFormat decrypt = getPgpDataFormat("secring.pgp");
        from("direct:marshalPgp")
                .marshal(encrypt);

        from("direct:unmarshalPgp")
                .unmarshal(decrypt);
    }

    private CryptoDataFormat getCryptoDataFormat() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("DES");
        return new CryptoDataFormat("DES", generator.generateKey());
    }

    private PGPDataFormat getPgpDataFormat(String keyFile) {
        PGPDataFormat encrypt = new PGPDataFormat();
        encrypt.setKeyFileName(keyFile);
        encrypt.setKeyUserid("sdude@nowhere.net");
        encrypt.setPassword("sdude");
        return encrypt;
    }
}
