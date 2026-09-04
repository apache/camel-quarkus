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
package org.apache.camel.quarkus.support.xalan.deployment;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import org.apache.camel.quarkus.support.xalan.XalanTransformerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Xalan-J 2.7.x cannot honour {@link XMLConstants#ACCESS_EXTERNAL_DTD} and
 * {@link XMLConstants#ACCESS_EXTERNAL_STYLESHEET}, and its
 * {@link XMLConstants#FEATURE_SECURE_PROCESSING} does not imply them either. These tests pin down the
 * external access restrictions {@link XalanTransformerFactory} applies in their place, so that the
 * behaviour stays in line with what upstream Camel gets from the JDK factory.
 */
class XalanTransformerFactoryExternalAccessTest {

    private static final String SECRET = "TOP-SECRET-CONTENT";

    private static final String INCLUDED_XSL = "<xsl:stylesheet version='1.0'"
            + " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
            + "<xsl:template name='included'><xsl:text>INCLUDED</xsl:text></xsl:template></xsl:stylesheet>";

    @TempDir
    static Path tempDir;

    private static String secretUri;
    private static String secretXmlUri;
    private static String includedUri;

    @BeforeAll
    static void writeExternalResources() throws IOException {
        final Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, SECRET);
        secretUri = secret.toUri().toString();

        // document() parses what it fetches, so its target has to be well formed XML for the test to fail
        // when the restriction is absent rather than when the content cannot be parsed
        final Path secretXml = tempDir.resolve("secret.xml");
        Files.writeString(secretXml, "<s>" + SECRET + "</s>");
        secretXmlUri = secretXml.toUri().toString();

        final Path included = tempDir.resolve("included.xsl");
        Files.writeString(included, INCLUDED_XSL);
        includedUri = included.toUri().toString();
    }

    /** Copies the value of {@code //data} into the output, so an expanded entity would show up there */
    private static final String COPY_DATA_XSL = "<xsl:stylesheet version='1.0'"
            + " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'><xsl:output method='text'/>"
            + "<xsl:template match='/'><xsl:value-of select='//data'/></xsl:template></xsl:stylesheet>";

    private static String externalEntityDocument() {
        return "<?xml version='1.0'?><!DOCTYPE r [<!ENTITY xxe SYSTEM '" + secretUri + "'>]>"
                + "<r><data>&xxe;</data></r>";
    }

    private static String documentFunctionXsl() {
        return "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
                + "<xsl:output method='text'/><xsl:template match='/'>"
                + "<xsl:value-of select=\"document('" + secretXmlUri + "')\"/></xsl:template></xsl:stylesheet>";
    }

    private static String includingXsl() {
        return "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
                + "<xsl:include href='" + includedUri + "'/><xsl:output method='text'/>"
                + "<xsl:template match='/'><xsl:call-template name='included'/></xsl:template></xsl:stylesheet>";
    }

    private static String transform(TransformerFactory factory, String xsl, Source input) throws Exception {
        final Transformer transformer = factory.newTemplates(new StreamSource(new StringReader(xsl))).newTransformer();
        final StringWriter result = new StringWriter();
        transformer.transform(input, new StreamResult(result));
        return result.toString();
    }

    private static Source stylesheetSource() {
        return new StreamSource(new StringReader(documentFunctionXsl()));
    }

    /** Either the transform fails or it yields nothing; what must not happen is the secret coming back */
    private static void assertDenied(ThrowingSupplier<String> transformation) {
        String result;
        try {
            result = transformation.get();
        } catch (Exception e) {
            // A refusal Xalan reports as a failure rather than as an empty result
            return;
        }
        assertFalse(result.contains(SECRET), "An external resource was resolved into the transformation result");
    }

    private static String pushThroughHandler(SaxFactoryFunction<TransformerHandler> handlerFactory) throws Exception {
        return pushThroughHandler(new XalanTransformerFactory(), handlerFactory);
    }

    /**
     * Drives a {@link TransformerHandler} the way the SAX push API is meant to be used: the caller parses the
     * input document with a reader of its own and feeds the events in.
     */
    private static String pushThroughHandler(XalanTransformerFactory factory,
            SaxFactoryFunction<TransformerHandler> handlerFactory) throws Exception {
        final TransformerHandler handler = handlerFactory.apply(factory);
        final StringWriter result = new StringWriter();
        handler.setResult(new StreamResult(result));

        final XMLReader reader = namespaceAwareReader();
        reader.setContentHandler(handler);
        reader.parse(new InputSource(new StringReader("<r/>")));
        return result.toString();
    }

    private static String pushThroughFilter(SaxFactoryFunction<XMLFilter> filterFactory) throws Exception {
        return pushThroughFilter(new XalanTransformerFactory(), filterFactory);
    }

    private static String pushThroughFilter(XalanTransformerFactory factory,
            SaxFactoryFunction<XMLFilter> filterFactory) throws Exception {
        final XMLFilter filter = filterFactory.apply(factory);
        final StringWriter result = new StringWriter();

        // The filter transforms and passes the events on; an identity handler serialises what comes out
        final TransformerHandler output = (TransformerHandler) ((SAXTransformerFactory) TransformerFactory
                .newInstance("org.apache.xalan.xsltc.trax.TransformerFactoryImpl", null)).newTransformerHandler();
        output.setResult(new StreamResult(result));

        filter.setParent(namespaceAwareReader());
        filter.setContentHandler(output);
        filter.parse(new InputSource(new StringReader("<r/>")));
        return result.toString();
    }

    private static XMLReader namespaceAwareReader() throws Exception {
        final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        return parserFactory.newSAXParser().getXMLReader();
    }

    @FunctionalInterface
    private interface SaxFactoryFunction<T> {
        T apply(SAXTransformerFactory factory) throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * A body that is already {@link Source} shaped bypasses the hardened SAX conversion camel-xslt applies to
     * String, byte[] and InputStream bodies, so the factory has to parse it safely itself.
     */
    @Test
    void externalEntityInSourceShapedInputIsNotResolved() throws Exception {
        final String result = transform(new XalanTransformerFactory(), COPY_DATA_XSL,
                new StreamSource(new StringReader(externalEntityDocument())));

        assertFalse(result.contains(SECRET), "The external entity was resolved into the transformation result");
    }

    /** The same document reaching the transformer the way camel-xslt hands it over must stay safe too */
    @Test
    void externalEntityInSaxSourceInputIsNotResolved() throws Exception {
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        final SAXSource saxSource = new SAXSource(saxParserFactory.newSAXParser().getXMLReader(),
                new InputSource(new StringReader(externalEntityDocument())));

        final String result = transform(new XalanTransformerFactory(), COPY_DATA_XSL, saxSource);

        assertFalse(result.contains(SECRET), "The external entity was resolved into the transformation result");
    }

    /** Hardening the input document must not stop ordinary transformations from working */
    @Test
    void ordinaryTransformationIsUnaffected() throws Exception {
        final String result = transform(new XalanTransformerFactory(), COPY_DATA_XSL,
                new StreamSource(new StringReader("<r><data>HELLO</data></r>")));

        assertEquals("HELLO", result.trim());
    }

    /**
     * {@code document()} is resolved at transform time, where Xalan does not consult the factory's
     * {@link URIResolver} on its own. Whether the refusal surfaces as an exception or as an empty result is
     * Xalan's business; what matters is that the resource is not fetched.
     */
    @Test
    void documentFunctionIsDeniedWithoutApplicationUriResolver() {
        assertDenied(() -> transform(new XalanTransformerFactory(), documentFunctionXsl(),
                new StreamSource(new StringReader("<r/>"))));
    }

    /**
     * {@code document()} has to be denied on every entry point handing out something to transform with, not
     * only on {@link Transformer} and {@link javax.xml.transform.Templates}. Xalan copies the factory's
     * {@link URIResolver} onto some of these and not others, so each one is pinned separately.
     */
    @Test
    void documentFunctionIsDeniedInTransformerHandlerFromSource() {
        assertDenied(() -> pushThroughHandler(factory -> factory.newTransformerHandler(stylesheetSource())));
    }

    @Test
    void documentFunctionIsDeniedInTransformerHandlerFromTemplates() {
        assertDenied(() -> pushThroughHandler(
                factory -> factory.newTransformerHandler(factory.newTemplates(stylesheetSource()))));
    }

    @Test
    void documentFunctionIsDeniedInXmlFilterFromSource() {
        assertDenied(() -> pushThroughFilter(factory -> factory.newXMLFilter(stylesheetSource())));
    }

    @Test
    void documentFunctionIsDeniedInXmlFilterFromTemplates() {
        assertDenied(() -> pushThroughFilter(
                factory -> factory.newXMLFilter(factory.newTemplates(stylesheetSource()))));
    }

    /**
     * The counterpart to the four tests above: an application that resolves the reference itself still gets
     * it, which is also what proves those tests deny a fetch rather than merely exercising a broken path.
     */
    @Test
    void applicationUriResolverResolvesDocumentInXmlFilter() throws Exception {
        final XalanTransformerFactory factory = new XalanTransformerFactory();
        factory.setURIResolver((href, base) -> new StreamSource(new StringReader("<s>" + SECRET + "</s>"), href));

        assertTrue(pushThroughFilter(factory, f -> f.newXMLFilter(stylesheetSource())).contains(SECRET),
                "The application URIResolver did not resolve document()");
    }

    /**
     * Xalan dereferences an {@code xsl:include} href itself whenever it can, ignoring both a refusal and a
     * {@code null} from the {@link URIResolver} it consulted first, so compile time includes cannot be
     * restricted here. Pinned so that a Xalan upgrade changing this does not go unnoticed. Stylesheets are
     * deployment owned, and camel-xslt resolves includes through its own unrestricted resolver anyway.
     */
    @Test
    void externalIncludeCannotBeRestricted() throws Exception {
        final String result = transform(new XalanTransformerFactory(), includingXsl(),
                new StreamSource(new StringReader("<r/>")));

        assertEquals("INCLUDED", result.trim());
    }

    /**
     * camel-xslt resolves includes through its own {@link URIResolver} (and camel-quarkus resolves them
     * through {@code BuildTimeUriResolver} at build time). Those must keep working, otherwise the extension
     * would be stricter than plain Camel rather than on a par with it. The href deliberately uses a scheme
     * Xalan cannot dereference on its own, so only the application resolver can satisfy it.
     */
    @Test
    void applicationUriResolverResolvesIncludesXalanCannot() throws Exception {
        final TransformerFactory factory = new XalanTransformerFactory();
        factory.setURIResolver((href, base) -> "buildtime:included.xsl".equals(href)
                ? new StreamSource(new StringReader(INCLUDED_XSL), href)
                : null);

        final String xsl = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
                + "<xsl:include href='buildtime:included.xsl'/><xsl:output method='text'/>"
                + "<xsl:template match='/'><xsl:call-template name='included'/></xsl:template></xsl:stylesheet>";

        assertEquals("INCLUDED", transform(factory, xsl, new StreamSource(new StringReader("<r/>"))).trim());
    }

    /** The resolver an application sets must be the one that is handed back to it */
    @Test
    void applicationUriResolverIsVisibleToTheApplication() {
        final TransformerFactory factory = new XalanTransformerFactory();
        final URIResolver resolver = (href, base) -> null;
        factory.setURIResolver(resolver);

        assertEquals(resolver, factory.getURIResolver());
    }

    /**
     * Secure processing is what the external access restrictions above stand in for, so it must stay on by
     * default.
     */
    @Test
    void secureProcessingIsEnabledByDefault() {
        assertDoesNotThrow(() -> new XalanTransformerFactory().getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));
    }
}
