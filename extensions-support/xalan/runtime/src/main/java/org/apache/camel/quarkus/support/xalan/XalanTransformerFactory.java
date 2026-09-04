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
package org.apache.camel.quarkus.support.xalan;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import org.apache.xalan.xsltc.trax.TrAXFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TransformerFactory} delegating to a {@link TransformerFactory} created via
 * {@code TransformerFactory.newInstance("org.apache.xalan.xsltc.trax.TransformerFactoryImpl", Thread.currentThread().getContextClassLoader())}
 * <p>
 * Xalan-J 2.7.x predates JAXP 1.5, so it cannot honour {@link XMLConstants#ACCESS_EXTERNAL_DTD} and
 * {@link XMLConstants#ACCESS_EXTERNAL_STYLESHEET} - {@code setAttribute()} throws
 * {@link IllegalArgumentException} for both. Its {@link XMLConstants#FEATURE_SECURE_PROCESSING} limits
 * extension functions only, and does not imply the external access restrictions the JDK applies under the
 * same feature. Because callers commonly harden a factory with
 * {@code try { setAttribute(ACCESS_EXTERNAL_DTD, "") } catch (Exception ignored) {}}, that hardening would
 * be lost silently. This factory therefore applies the deny-by-default part itself:
 * <ul>
 * <li>input documents passed to {@link Transformer#transform(Source, javax.xml.transform.Result)} are
 * parsed with an {@link XMLReader} that does not resolve external general entities, which is what upstream
 * Camel's {@code XmlConverter} does for the bodies it converts to a {@link SAXSource} itself,</li>
 * <li>resources fetched at transform time by the {@code document()} function are denied unless the
 * application's own {@link URIResolver} resolves them, on every entry point that hands out something to
 * transform with - {@link Transformer}, {@link Templates}, {@link TransformerHandler} and
 * {@link XMLFilter}.</li>
 * </ul>
 * <p>
 * One restriction cannot be reinstated here: Xalan resolves {@code xsl:import}/{@code xsl:include} directly
 * whenever the href is one it can dereference, ignoring both a refusal and a {@code null} from the
 * {@link URIResolver} it consulted first. Stylesheets are deployment owned rather than attacker controlled,
 * and camel-xslt resolves includes through its own unrestricted {@code XsltUriResolver} anyway, so this
 * matches what plain Camel does on the component path.
 */
public final class XalanTransformerFactory extends SAXTransformerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(XalanTransformerFactory.class);

    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";

    private final SAXTransformerFactory delegate;

    /**
     * The {@link URIResolver} set by the application, if any. The delegate factory keeps
     * {@link RestrictingUriResolver} installed at all times so that the restriction cannot be dropped by
     * an application calling {@link #setURIResolver(URIResolver)}.
     */
    private volatile URIResolver applicationUriResolver;

    private final RestrictingUriResolver restrictingUriResolver = new RestrictingUriResolver();

    public XalanTransformerFactory() {
        final SAXTransformerFactory factory = (SAXTransformerFactory) TransformerFactory.newInstance(
                "org.apache.xalan.xsltc.trax.TransformerFactoryImpl",
                Thread.currentThread().getContextClassLoader());
        try {
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerException e) {
            LOGGER.warn("Unsupported TransformerFactory feature " + javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING);
        }

        this.delegate = factory;
        this.delegate.setURIResolver(restrictingUriResolver);
    }

    @Override
    public Transformer newTransformer(Source source) throws TransformerConfigurationException {
        return secure(delegate.newTransformer(source));
    }

    @Override
    public Transformer newTransformer() throws TransformerConfigurationException {
        return secure(delegate.newTransformer());
    }

    @Override
    public Templates newTemplates(Source source) throws TransformerConfigurationException {
        return new SecuredTemplates(delegate.newTemplates(source), this);
    }

    /**
     * Xalan does not propagate the factory's {@link URIResolver} onto the transformers it produces, so
     * {@code document()} would be resolved unrestricted at transform time. Installing the resolver here is
     * what makes {@link XMLConstants#ACCESS_EXTERNAL_STYLESHEET} effective. Applications that set their own
     * resolver on the transformer - camel-xslt does so on every exchange - keep overriding it as before.
     */
    private Transformer secure(Transformer transformer) {
        transformer.setURIResolver(restrictingUriResolver);
        return new SecuredTransformer(transformer);
    }

    @Override
    public Source getAssociatedStylesheet(Source source, String media, String title, String charset)
            throws TransformerConfigurationException {
        return delegate.getAssociatedStylesheet(source, media, title, charset);
    }

    @Override
    public void setURIResolver(URIResolver resolver) {
        // Keep RestrictingUriResolver on the delegate; it consults this resolver first.
        this.applicationUriResolver = resolver;
    }

    @Override
    public URIResolver getURIResolver() {
        return applicationUriResolver;
    }

    @Override
    public void setFeature(String name, boolean value) throws TransformerConfigurationException {
        delegate.setFeature(name, value);
    }

    @Override
    public boolean getFeature(String name) {
        return delegate.getFeature(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public void setErrorListener(ErrorListener listener) {
        delegate.setErrorListener(listener);
    }

    @Override
    public ErrorListener getErrorListener() {
        return delegate.getErrorListener();
    }

    @Override
    public TransformerHandler newTransformerHandler(Source source) throws TransformerConfigurationException {
        return secure(delegate.newTransformerHandler(source));
    }

    @Override
    public TransformerHandler newTransformerHandler(Templates templates) throws TransformerConfigurationException {
        return secure(delegate.newTransformerHandler(unwrap(templates)));
    }

    @Override
    public TransformerHandler newTransformerHandler() throws TransformerConfigurationException {
        return secure(delegate.newTransformerHandler());
    }

    @Override
    public TemplatesHandler newTemplatesHandler() throws TransformerConfigurationException {
        return delegate.newTemplatesHandler();
    }

    @Override
    public XMLFilter newXMLFilter(Source source) throws TransformerConfigurationException {
        return secure(delegate.newXMLFilter(source));
    }

    @Override
    public XMLFilter newXMLFilter(Templates templates) throws TransformerConfigurationException {
        return secure(delegate.newXMLFilter(unwrap(templates)));
    }

    /**
     * The SAX push entry points hand the caller a {@link Transformer} to configure rather than one to call,
     * and Xalan only copies the factory's {@link URIResolver} onto some of them, so {@code document()} is
     * restricted here for the same reason it is in {@link #secure(Transformer)}. The document being
     * transformed is parsed by the {@link XMLReader} the caller drives the handler with, which is the
     * caller's own choice just as a {@link SAXSource} carrying a reader is.
     */
    private TransformerHandler secure(TransformerHandler handler) {
        handler.getTransformer().setURIResolver(restrictingUriResolver);
        return handler;
    }

    /**
     * {@link XMLFilter} has no accessor for the {@link Transformer} behind it, so the restriction can only be
     * installed on Xalan's own implementation. Guarded rather than cast blindly so that a Xalan upgrade
     * returning something else is reported instead of silently dropping the restriction.
     */
    private XMLFilter secure(XMLFilter filter) {
        if (filter instanceof TrAXFilter) {
            ((TrAXFilter) filter).getTransformer().setURIResolver(restrictingUriResolver);
        } else {
            LOGGER.warn("Expected an {} from the Xalan TransformerFactory but got {}. The document() function"
                    + " may resolve external resources when transforming through this XMLFilter.",
                    TrAXFilter.class.getName(), filter == null ? null : filter.getClass().getName());
        }
        return filter;
    }

    /**
     * Xalan casts {@link Templates} to its own {@code TemplatesImpl} internally, so the wrapper has to be
     * peeled off before handing one back to the delegate.
     */
    private static Templates unwrap(Templates templates) {
        return templates instanceof SecuredTemplates ? ((SecuredTemplates) templates).delegate : templates;
    }

    /**
     * Parses {@code source} with a hardened {@link XMLReader} unless it has already been parsed, or the
     * caller supplied its own reader. Mirrors what {@code XmlConverter.createSAXParserFactory()} does for
     * the bodies camel-xslt converts itself, so that {@link Source}-shaped bodies get the same treatment.
     */
    private static Source secureInputSource(Source source) throws TransformerException {
        if (source instanceof StreamSource) {
            final StreamSource streamSource = (StreamSource) source;
            final InputStream inputStream = streamSource.getInputStream();
            final Reader reader = streamSource.getReader();
            if (inputStream == null && reader == null) {
                // Nothing but a systemId; let the delegate resolve it as before
                return source;
            }
            final InputSource inputSource = new InputSource();
            inputSource.setSystemId(streamSource.getSystemId());
            if (inputStream != null) {
                inputSource.setByteStream(inputStream);
            } else {
                inputSource.setCharacterStream(reader);
            }
            return new SAXSource(createSecureXmlReader(), inputSource);
        }
        if (source instanceof SAXSource) {
            final SAXSource saxSource = (SAXSource) source;
            if (saxSource.getXMLReader() == null) {
                return new SAXSource(createSecureXmlReader(), saxSource.getInputSource());
            }
        }
        // DOMSource and StAXSource are already parsed; a caller supplied XMLReader is the caller's own choice
        return source;
    }

    private static XMLReader createSecureXmlReader() throws TransformerException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        setFeature(factory, javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeature(factory, EXTERNAL_GENERAL_ENTITIES, false);
        try {
            return factory.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException | SAXException e) {
            throw new TransformerException("Could not create a secure XMLReader for the input document", e);
        }
    }

    private static void setFeature(SAXParserFactory factory, String name, boolean value) {
        try {
            factory.setFeature(name, value);
        } catch (ParserConfigurationException | SAXException e) {
            LOGGER.warn("SAXParserFactory does not support the feature {} with value {}, due to {}."
                    + " External entities in XSLT input documents may be resolved.", name, value, e.getMessage());
        }
    }

    /**
     * Denies external references that the application's own {@link URIResolver} did not resolve. This is the
     * {@link XMLConstants#ACCESS_EXTERNAL_STYLESHEET} behaviour the JDK applies when
     * {@link XMLConstants#FEATURE_SECURE_PROCESSING} is enabled, which Xalan does not implement. It takes
     * effect for {@code document()} at transform time; see the class javadoc for why compile time
     * {@code xsl:import}/{@code xsl:include} cannot be covered.
     */
    private final class RestrictingUriResolver implements URIResolver {
        @Override
        public Source resolve(String href, String base) throws TransformerException {
            final URIResolver resolver = applicationUriResolver;
            if (resolver != null) {
                final Source source = resolver.resolve(href, base);
                if (source != null) {
                    return source;
                }
            }
            throw new TransformerException(
                    "Access to the external resource '" + href + "' (base '" + base + "') is not allowed."
                            + " Resolve it through a javax.xml.transform.URIResolver if it is required.");
        }
    }

    /**
     * Ensures {@link SecuredTransformer} is used for transformers obtained from compiled templates, which is
     * how camel-xslt gets hold of them.
     */
    private static final class SecuredTemplates implements Templates {
        private final Templates delegate;
        private final XalanTransformerFactory factory;

        SecuredTemplates(Templates delegate, XalanTransformerFactory factory) {
            this.delegate = delegate;
            this.factory = factory;
        }

        @Override
        public Transformer newTransformer() throws TransformerConfigurationException {
            return factory.secure(delegate.newTransformer());
        }

        @Override
        public Properties getOutputProperties() {
            return delegate.getOutputProperties();
        }
    }

    /**
     * Applies {@link XalanTransformerFactory#secureInputSource(Source)} to the document being transformed.
     */
    private static final class SecuredTransformer extends Transformer {
        private final Transformer delegate;

        SecuredTransformer(Transformer delegate) {
            this.delegate = delegate;
        }

        @Override
        public void transform(Source xmlSource, javax.xml.transform.Result outputTarget) throws TransformerException {
            delegate.transform(secureInputSource(xmlSource), outputTarget);
        }

        @Override
        public void setParameter(String name, Object value) {
            delegate.setParameter(name, value);
        }

        @Override
        public Object getParameter(String name) {
            return delegate.getParameter(name);
        }

        @Override
        public void clearParameters() {
            delegate.clearParameters();
        }

        @Override
        public void setURIResolver(URIResolver resolver) {
            delegate.setURIResolver(resolver);
        }

        @Override
        public URIResolver getURIResolver() {
            return delegate.getURIResolver();
        }

        @Override
        public void setOutputProperties(Properties oformat) {
            delegate.setOutputProperties(oformat);
        }

        @Override
        public Properties getOutputProperties() {
            return delegate.getOutputProperties();
        }

        @Override
        public void setOutputProperty(String name, String value) {
            delegate.setOutputProperty(name, value);
        }

        @Override
        public String getOutputProperty(String name) {
            return delegate.getOutputProperty(name);
        }

        @Override
        public void setErrorListener(ErrorListener listener) {
            delegate.setErrorListener(listener);
        }

        @Override
        public ErrorListener getErrorListener() {
            return delegate.getErrorListener();
        }

        @Override
        public void reset() {
            delegate.reset();
        }
    }
}
