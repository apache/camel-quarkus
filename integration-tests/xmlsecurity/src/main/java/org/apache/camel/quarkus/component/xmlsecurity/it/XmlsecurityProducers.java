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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.XPathType;

import org.w3c.dom.Node;

import org.apache.camel.Message;
import org.apache.camel.component.xmlsecurity.api.KeyAccessor;
import org.apache.camel.component.xmlsecurity.api.XmlSignatureHelper;

@ApplicationScoped
public class XmlsecurityProducers {

    @Produces
    @Named("accessor")
    public KeyAccessor getAccessor(KeyPair keyPair) {
        return new KeyAccessor() {
            @Override
            public KeySelector getKeySelector(Message message) throws Exception {
                return KeySelector.singletonKeySelector(keyPair.getPrivate());
            }

            @Override
            public KeyInfo getKeyInfo(Message mess, Node messageBody, KeyInfoFactory keyInfoFactory) throws Exception {
                return null;
            }
        };
    }

    @Produces
    @Named("selector")
    public KeySelector getSelector(KeyPair keyPair) {
        return new KeySelector() {
            @Override
            public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod algorithmMethod,
                    XMLCryptoContext xmlCryptoContext) {
                return () -> keyPair.getPublic();
            }
        };
    }

    @Produces
    @Named("canonicalizationMethod")
    public AlgorithmMethod getCanonicalizationMethod() {
        List<String> inclusivePrefixes = new ArrayList<>();
        inclusivePrefixes.add("ds");
        return XmlSignatureHelper.getCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, inclusivePrefixes);
    }

    @Produces
    @Named("transformsXPath")
    public List<AlgorithmMethod> getTransformsXPathMethod() {
        List<XmlSignatureHelper.XPathAndFilter> list = new ArrayList<>(3);
        XmlSignatureHelper.XPathAndFilter xpath1 = new XmlSignatureHelper.XPathAndFilter("//n0:ToBeSigned",
                XPathType.Filter.INTERSECT.toString());
        list.add(xpath1);
        XmlSignatureHelper.XPathAndFilter xpath2 = new XmlSignatureHelper.XPathAndFilter("//n0:NotToBeSigned",
                XPathType.Filter.SUBTRACT.toString());
        list.add(xpath2);
        XmlSignatureHelper.XPathAndFilter xpath3 = new XmlSignatureHelper.XPathAndFilter("//n0:ReallyToBeSigned",
                XPathType.Filter.UNION.toString());
        list.add(xpath3);

        List<AlgorithmMethod> result = new ArrayList<>();
        result.add(XmlSignatureHelper.getCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE));

        Map<String, String> map = new HashMap<>();
        map.put("n0", "http://test/test");
        result.add(XmlSignatureHelper.getXPath2Transform(list, map));

        return result;
    }

    @Produces
    @Named("transformsXsltXPath")
    public List<AlgorithmMethod> getTransformsXsltXPathMethod() throws Exception {
        AlgorithmMethod transformXslt = XmlSignatureHelper.getXslTransform("/xslt-test.xsl");
        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("n0", "https://org.apache/camel/xmlsecurity/test");
        AlgorithmMethod transformXpath = XmlSignatureHelper.getXPathTransform("//n0:XMLSecurity/n0:Content", namespaceMap);
        List<AlgorithmMethod> result = new ArrayList<>();
        result.add(XmlSignatureHelper.getCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE));
        result.add(transformXslt);
        result.add(transformXpath);
        return result;
    }

    @Singleton
    @Produces
    public KeyPair getKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen;
        keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    @Singleton
    @Produces
    @Named("key")
    public SecretKey key() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        keyGenerator.generateKey();
        return keyGenerator.generateKey();
    }

    @Produces
    @Named("cryptoContextProperties")
    public Map<String, Object> cryptoContextProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("org.apache.jcp.xml.dsig.secureValidation", Boolean.FALSE);
        return properties;
    }
}
