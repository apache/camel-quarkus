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
package org.apache.camel.quarkus.component.xslt.deployment;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.quarkus.component.xslt.CamelXsltTransformerFactory;
import org.apache.camel.support.builder.xml.XMLConverterHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins down the restrictions of {@link CamelXsltTransformerFactory}, which every {@code xslt:} endpoint transforms
 * with unless it is configured with a factory of its own.
 * <p>
 * The reference is the {@link TransformerFactory} Camel creates for an {@code xslt:} endpoint by default. Every
 * restriction is asserted against both, so that a change to what Camel applies shows up here as a failure.
 */
class CamelXsltTransformerFactoryTest {

    private static final String SECRET = "TOP-SECRET-CONTENT";

    private static final String INCLUDED_XSL = "<xsl:stylesheet version='1.0'"
            + " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
            + "<xsl:template name='included'><xsl:text>INCLUDED</xsl:text></xsl:template></xsl:stylesheet>";

    /** Copies {@code //data} and its {@code leak} attribute, which only a DTD that was read can default */
    private static final String COPY_DATA_XSL = "<xsl:stylesheet version='1.0'"
            + " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'><xsl:output method='text'/>"
            + "<xsl:template match='/'><xsl:value-of select='//data'/><xsl:value-of select='//data/@leak'/>"
            + "</xsl:template></xsl:stylesheet>";

    /** Calls {@link System#getProperty(String)} for a property a test sets, through the extension function syntax */
    private static final String EXTENSION_FUNCTION_XSL = "<xsl:stylesheet version='1.0'"
            + " xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns:sys='xalan://java.lang.System'>"
            + "<xsl:output method='text'/><xsl:template match='/'>"
            + "<xsl:value-of select=\"sys:getProperty('" + CamelXsltTransformerFactoryTest.class.getName() + "')\"/>"
            + "</xsl:template></xsl:stylesheet>";

    /** Features that have a bearing on what a template or a document being transformed is permitted to do */
    private static final List<String> SECURITY_FEATURES = List.of(
            XMLConstants.FEATURE_SECURE_PROCESSING,
            XMLConstants.USE_CATALOG,
            "jdk.xml.enableExtensionFunctions",
            "http://www.oracle.com/xml/jaxp/properties/enableExtensionFunctions",
            "jdk.xml.overrideDefaultParser");

    /** Attributes that have a bearing on what a template or a document being transformed is permitted to do */
    private static final List<String> SECURITY_ATTRIBUTES = List.of(
            XMLConstants.ACCESS_EXTERNAL_DTD,
            XMLConstants.ACCESS_EXTERNAL_STYLESHEET,
            "javax.xml.catalog.files",
            "javax.xml.catalog.resolve",
            "jdk.xml.dtd.support",
            "jdk.xml.jdkcatalog.resolve",
            "jdk.xml.extensionClassLoader",
            "jdk.xml.entityExpansionLimit",
            "jdk.xml.elementAttributeLimit",
            "jdk.xml.maxOccurLimit",
            "jdk.xml.totalEntitySizeLimit",
            "jdk.xml.maxGeneralEntitySizeLimit",
            "jdk.xml.maxParameterEntitySizeLimit",
            "jdk.xml.entityReplacementLimit",
            "jdk.xml.maxElementDepth",
            "jdk.xml.maxXMLNameLimit",
            "jdk.xml.xpathExprGrpLimit",
            "jdk.xml.xpathExprOpLimit",
            "jdk.xml.xpathTotalOpLimit");

    @TempDir
    static Path tempDir;

    private static String secretUri;
    private static String secretXmlUri;
    private static String secretDtdUri;
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

        // An attribute default rather than an entity, so that the document stays well formed whether or not the
        // DTD is read, and reading it shows up in the result
        final Path secretDtd = tempDir.resolve("secret.dtd");
        Files.writeString(secretDtd, "<!ATTLIST data leak CDATA '" + SECRET + "'>");
        secretDtdUri = secretDtd.toUri().toString();

        final Path included = tempDir.resolve("included.xsl");
        Files.writeString(included, INCLUDED_XSL);
        includedUri = included.toUri().toString();
    }

    private static String documentFunctionXsl() {
        return "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
                + "<xsl:output method='text'/><xsl:template match='/'>"
                + "<xsl:value-of select=\"document('" + secretXmlUri + "')\"/></xsl:template></xsl:stylesheet>";
    }

    private static String includingXsl(String element, String href) {
        return "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
                + "<xsl:" + element + " href='" + href + "'/><xsl:output method='text'/>"
                + "<xsl:template match='/'><xsl:call-template name='included'/></xsl:template></xsl:stylesheet>";
    }

    private static Source source(String xml) {
        return new StreamSource(new StringReader(xml));
    }

    private static String transform(TransformerFactory factory, String xsl, Source input) throws Exception {
        final Transformer transformer = factory.newTemplates(source(xsl)).newTransformer();
        final StringWriter result = new StringWriter();
        transformer.transform(input, new StreamResult(result));
        return result.toString();
    }

    /** Either the transformation fails or it yields nothing; what must not happen is the secret coming back */
    private static void assertDenied(ThrowingSupplier<String> transformation) {
        String result;
        try {
            result = transformation.get();
        } catch (Exception e) {
            return;
        }
        assertFalse(result.contains(SECRET), "An external resource was resolved into the transformation result");
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static TransformerFactory camelDefaultFactory() {
        return new XMLConverterHelper().createTransformerFactory();
    }

    /** The factory of this extension, and the one Camel creates by default as the reference */
    static Stream<Named<Supplier<TransformerFactory>>> factories() {
        return Stream.of(
                Named.of("CamelXsltTransformerFactory", CamelXsltTransformerFactory::new),
                Named.of("Camel default", CamelXsltTransformerFactoryTest::camelDefaultFactory));
    }

    /** An attribute the JDK running the test does not know is reported as such rather than failing the comparison */
    private static Object attribute(TransformerFactory factory, String name) {
        try {
            return String.valueOf(factory.getAttribute(name));
        } catch (IllegalArgumentException e) {
            return "unsupported";
        }
    }

    /**
     * Camel's default factory is only a meaningful reference while it is the implementation built into the JDK,
     * which is the one {@link CamelXsltTransformerFactory} always delegates to.
     */
    @Test
    void camelDefaultFactoryIsTheJdkImplementation() {
        assertEquals(TransformerFactory.newDefaultInstance().getClass(), camelDefaultFactory().getClass());
    }

    @Test
    void securitySettingsMatchCamelDefaultFactory() {
        final TransformerFactory expected = camelDefaultFactory();
        final TransformerFactory actual = new CamelXsltTransformerFactory();

        for (String feature : SECURITY_FEATURES) {
            assertEquals(expected.getFeature(feature), actual.getFeature(feature), feature);
        }
        for (String attribute : SECURITY_ATTRIBUTES) {
            assertEquals(attribute(expected, attribute), attribute(actual, attribute), attribute);
        }
    }

    /**
     * camel-xslt reads this attribute back to restrict the resolver it installs for {@code document()}, so it has
     * to be reported and not only applied.
     */
    @Test
    void externalAccessRestrictionsAreReported() {
        final TransformerFactory factory = new CamelXsltTransformerFactory();

        assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD));
        assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET));
    }

    @Test
    void ordinaryTransformationIsUnaffected() throws Exception {
        assertEquals("HELLO", transform(new CamelXsltTransformerFactory(), COPY_DATA_XSL,
                source("<r><data>HELLO</data></r>")).trim());
    }

    /**
     * A body that is already {@link Source} shaped bypasses the hardened conversion camel-xslt applies to String,
     * byte[] and InputStream bodies, so the factory is all that stands between it and the transformer.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void externalEntityInInputIsNotResolved(Supplier<TransformerFactory> factory) {
        assertDenied(() -> transform(factory.get(), COPY_DATA_XSL,
                source("<?xml version='1.0'?><!DOCTYPE r [<!ENTITY xxe SYSTEM '" + secretUri + "'>]>"
                        + "<r><data>&xxe;</data></r>")));
    }

    @ParameterizedTest
    @MethodSource("factories")
    void externalDtdInInputIsNotLoaded(Supplier<TransformerFactory> factory) {
        assertDenied(() -> transform(factory.get(), COPY_DATA_XSL,
                source("<?xml version='1.0'?><!DOCTYPE r SYSTEM '" + secretDtdUri + "'><r><data>HELLO</data></r>")));
    }

    @ParameterizedTest
    @MethodSource("factories")
    void externalParameterEntityInInputIsNotLoaded(Supplier<TransformerFactory> factory) {
        assertDenied(() -> transform(factory.get(), COPY_DATA_XSL,
                source("<?xml version='1.0'?><!DOCTYPE r [<!ENTITY % p SYSTEM '" + secretDtdUri + "'> %p;]>"
                        + "<r><data>HELLO</data></r>")));
    }

    @ParameterizedTest
    @MethodSource("factories")
    void documentFunctionIsDeniedWithoutUriResolver(Supplier<TransformerFactory> factory) {
        assertDenied(() -> transform(factory.get(), documentFunctionXsl(), source("<r/>")));
    }

    @ParameterizedTest
    @MethodSource("factories")
    void externalIncludeIsDeniedWithoutUriResolver(Supplier<TransformerFactory> factory) {
        assertDenied(() -> transform(factory.get(), includingXsl("include", includedUri),
                source("<r/>")));
    }

    @ParameterizedTest
    @MethodSource("factories")
    void externalImportIsDeniedWithoutUriResolver(Supplier<TransformerFactory> factory) {
        assertDenied(() -> transform(factory.get(), includingXsl("import", includedUri),
                source("<r/>")));
    }

    /**
     * The secure-processing feature can be switched off through {@code quarkus.camel.xslt.features} to permit
     * extension functions. That must not lift the external access restrictions along with it.
     */
    @Test
    void externalAccessStaysDeniedWhenSecureProcessingIsDisabled() throws Exception {
        final TransformerFactory factory = new CamelXsltTransformerFactory();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);

        assertDenied(() -> transform(factory, documentFunctionXsl(), source("<r/>")));
        assertDenied(() -> transform(factory, COPY_DATA_XSL,
                source("<?xml version='1.0'?><!DOCTYPE r SYSTEM '" + secretDtdUri + "'><r><data>HELLO</data></r>")));
    }

    /**
     * camel-xslt resolves includes through its own {@link javax.xml.transform.URIResolver}, and the extension
     * resolves them through {@code BuildTimeUriResolver} when compiling translets. Those must keep working.
     */
    @Test
    void uriResolverResolvesIncludes() throws Exception {
        final TransformerFactory factory = new CamelXsltTransformerFactory();
        factory.setURIResolver((href, base) -> "classpath:included.xsl".equals(href)
                ? new StreamSource(new StringReader(INCLUDED_XSL), href)
                : null);

        assertEquals("INCLUDED",
                transform(factory, includingXsl("include", "classpath:included.xsl"), source("<r/>")).trim());
    }

    /** camel-xslt installs a {@link javax.xml.transform.URIResolver} on every transformer it uses */
    @Test
    void uriResolverResolvesDocumentFunction() throws Exception {
        final Transformer transformer = new CamelXsltTransformerFactory().newTemplates(source(documentFunctionXsl()))
                .newTransformer();
        transformer.setURIResolver((href, base) -> new StreamSource(new StringReader("<s>RESOLVED</s>"), href));

        final StringWriter result = new StringWriter();
        transformer.transform(source("<r/>"), new StreamResult(result));

        assertEquals("RESOLVED", result.toString().trim());
    }

    @ParameterizedTest
    @MethodSource("factories")
    void secureProcessingIsEnabledByDefault(Supplier<TransformerFactory> factory) {
        assertTrue(factory.get().getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));
    }

    /** A template must not get to call into Java unless secure-processing was switched off for it */
    @ParameterizedTest
    @MethodSource("factories")
    void extensionFunctionIsDeniedByDefault(Supplier<TransformerFactory> factory) {
        withSecretSystemProperty(() -> assertDenied(() -> transform(factory.get(), EXTENSION_FUNCTION_XSL, source("<r/>"))));
    }

    /** The counterpart, which is also what proves the test above denies a call rather than exercising a broken one */
    @Test
    void extensionFunctionIsPermittedWhenSecureProcessingIsDisabled() {
        withSecretSystemProperty(() -> {
            final TransformerFactory factory = new CamelXsltTransformerFactory();
            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
                assertEquals(SECRET, transform(factory, EXTENSION_FUNCTION_XSL, source("<r/>")).trim());
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }

    private static void withSecretSystemProperty(Runnable test) {
        final String key = CamelXsltTransformerFactoryTest.class.getName();
        System.setProperty(key, SECRET);
        try {
            test.run();
        } finally {
            System.clearProperty(key);
        }
    }

    /**
     * Translets compiled at build time can only be loaded by the implementation that compiled them, so the
     * JAXP lookup that another implementation on the class path would win must not be involved.
     */
    @Test
    void jaxpLookupIsBypassed() throws Exception {
        final String key = "javax.xml.transform.TransformerFactory";
        final String original = System.getProperty(key);
        System.setProperty(key, "org.acme.DoesNotExist");
        try {
            assertEquals("HELLO", transform(new CamelXsltTransformerFactory(), COPY_DATA_XSL,
                    source("<r><data>HELLO</data></r>")).trim());
        } finally {
            if (original == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, original);
            }
        }
    }
}
