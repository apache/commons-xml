/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.xml.factory;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Entry point for obtaining hardened JAXP factories.
 *
 * <p>Every method on this class returns a <em>fresh, hardened</em> factory instance. No caching or pooling is performed; callers on a hot path are responsible
 * for their own caching.</p>
 *
 * <h2>Hardening guarantees</h2>
 *
 * <p>Every factory returned by this class makes the same three guarantees, regardless of which JAXP implementation is on the classpath:</p>
 *
 * <ul>
 *   <li><strong>External DTDs are not fetched.</strong></li>
 *   <li><strong>External entities are not resolved.</strong></li>
 *   <li><strong>Internal entity expansion is bounded</strong> by the JDK's default limit, so DoS payloads such as Billion Laughs are rejected before they
 *       exhaust resources.</li>
 * </ul>
 *
 * <p>The guarantees hold whether or not the caller opts into DTD validation
 * ({@link javax.xml.parsers.DocumentBuilderFactory#setValidating(boolean) setValidating(true)}) or attaches a compiled XSD via
 * {@link javax.xml.parsers.DocumentBuilderFactory#setSchema(javax.xml.validation.Schema) setSchema}: every external resource the validation would otherwise
 * fetch (the DTD itself, an {@code xsi:schemaLocation} hint, an external entity referenced from the DTD) remains blocked.</p>
 *
 * <p>Each method on this class adds factory-specific guarantees on top of the three above, documented on the corresponding {@code newXxxFactory()} method.</p>
 *
 * <h2>Caller-supplied URIs</h2>
 *
 * <p>A top-level URI passed directly by the caller is fetched as-is: {@code StreamSource(systemId)}, {@code DocumentBuilder.parse(String)}, or a
 * {@code SAXSource} built from a system id all cause the JAXP implementation to open that URI without consulting the hardening layer. Use a
 * {@link javax.xml.transform.URIResolver} or {@link org.xml.sax.EntityResolver} if you need to restrict the top-level fetch.</p>
 *
 * <h2>Thread safety</h2>
 *
 * <p>The returned factories inherit the thread-safety properties of the underlying JAXP implementation, which in practice means they are <strong>not
 * guaranteed to be thread-safe</strong>. Create a new factory per thread or synchronise externally.</p>
 *
 * <p>This class itself is thread-safe: all methods are static and stateless.</p>
 */
public final class XmlFactories {

    static DocumentBuilderFactory dispatch(final DocumentBuilderFactory factory) {
        switch (factory.getClass().getName()) {
            case "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl":
                return StockJdkProvider.configure(factory);
            case "org.apache.harmony.xml.parsers.DocumentBuilderFactoryImpl":
                return AndroidProvider.configure(factory);
            case "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl":
                return XercesProvider.configure(factory);
            default:
                throw noProvider(factory);
        }
    }

    private static SAXParserFactory dispatch(final SAXParserFactory factory) {
        switch (factory.getClass().getName()) {
            case "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl":
                return StockJdkProvider.configure(factory);
            case "org.apache.harmony.xml.parsers.SAXParserFactoryImpl":
                return AndroidProvider.configure(factory);
            case "org.apache.xerces.jaxp.SAXParserFactoryImpl":
                return XercesProvider.configure(factory);
            default:
                throw noProvider(factory);
        }
    }

    private static XMLInputFactory dispatch(final XMLInputFactory factory) {
        switch (factory.getClass().getName()) {
            case "com.sun.xml.internal.stream.XMLInputFactoryImpl":
                return StockJdkProvider.configure(factory);
            case "com.ctc.wstx.stax.WstxInputFactory":
                return WoodstoxProvider.configure(factory);
            default:
                throw noProvider(factory);
        }
    }

    private static TransformerFactory dispatch(final TransformerFactory factory) {
        switch (factory.getClass().getName()) {
            case "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl":
                return StockJdkProvider.configure(factory);
            case "org.apache.xalan.processor.TransformerFactoryImpl":
            case "org.apache.xalan.xsltc.trax.TransformerFactoryImpl":
                return XalanProvider.configure(factory);
            case "net.sf.saxon.TransformerFactoryImpl":
            case "com.saxonica.config.ProfessionalTransformerFactory":
            case "com.saxonica.config.EnterpriseTransformerFactory":
                return SaxonProvider.configure(factory);
            default:
                throw noProvider(factory);
        }
    }

    private static XPathFactory dispatch(final XPathFactory factory) {
        switch (factory.getClass().getName()) {
            case "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl":
                return StockJdkProvider.configure(factory);
            case "org.apache.xpath.jaxp.XPathFactoryImpl":
                return XalanProvider.configure(factory);
            case "net.sf.saxon.xpath.XPathFactoryImpl":
                return SaxonProvider.configure(factory);
            default:
                throw noProvider(factory);
        }
    }

    private static SchemaFactory dispatch(final SchemaFactory factory) {
        switch (factory.getClass().getName()) {
            case "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory":
                return StockJdkProvider.configure(factory);
            case "org.apache.xerces.jaxp.validation.XMLSchemaFactory":
                return XercesProvider.configure(factory);
            default:
                throw noProvider(factory);
        }
    }

    /**
     * Rewrites a {@link Source} so that any SAX parsing it triggers runs through an {@link XmlFactories}-hardened {@link XMLReader}.
     *
     * <p>Only {@link StreamSource} and {@link SAXSource} without a reader are enriched with a hardened reader. Other kinds of sources are returned as-is.</p>
     *
     * @param source the source to harden; never {@code null}.
     * @return a hardened source.
     * @throws TransformerConfigurationException if a hardened reader cannot be obtained.
     */
    public static Source harden(final Source source) throws TransformerConfigurationException {
        if (source instanceof StreamSource || source instanceof SAXSource && ((SAXSource) source).getXMLReader() == null) {
            try {
                final XMLReader reader = newSAXParserFactory().newSAXParser().getXMLReader();
                final InputSource inputSource = SAXSource.sourceToInputSource(source);
                return inputSource == null ? source : new SAXSource(reader, inputSource);
            } catch (final ParserConfigurationException | SAXException e) {
                throw new TransformerConfigurationException("Failed to obtain a hardened XMLReader for source parsing", e);
            }
        }
        return source;
    }

    /**
     * Hardens an existing {@link XMLReader}.
     *
     * @param reader the reader to harden; never {@code null}.
     * @return a hardened reader.
     * @throws IllegalStateException if the reader's concrete class is not recognised by any bundled hardening recipe, or if the matching recipe cannot apply
     *         its settings to it.
     */
    public static XMLReader harden(final XMLReader reader) {
        switch (reader.getClass().getName()) {
            case "com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser":
                return StockJdkProvider.configure(reader);
            case "org.apache.harmony.xml.ExpatReader":
            case "org.apache.commons.xml.factory.AndroidProvider$GuardedXMLReader":
                return AndroidProvider.configure(reader);
            case "org.apache.xerces.jaxp.SAXParserImpl$JAXPSAXParser":
                return XercesProvider.configure(reader);
            default:
                throw noProvider(reader);
        }
    }

    /**
     * Returns a fresh, hardened {@link DocumentBuilderFactory}.
     *
     * <p>Beyond the three universal guarantees on {@link XmlFactories}, XInclude resolution is disabled. Calling
     * {@link DocumentBuilderFactory#setXIncludeAware(boolean) setXIncludeAware(true)} on the returned factory does not re-enable resolution; a parse that
     * encounters an {@code xi:include} element fails.</p>
     *
     * @return a hardened factory.
     * @throws IllegalStateException if the underlying JAXP implementation is not recognised by any bundled hardening recipe, or if the matching recipe cannot
     *         apply its settings to it.
     */
    public static DocumentBuilderFactory newDocumentBuilderFactory() {
        return dispatch(DocumentBuilderFactory.newInstance());
    }

    /**
     * Returns a fresh, hardened {@link SAXParserFactory}.
     *
     * <p>Beyond the three universal guarantees on {@link XmlFactories}, XInclude resolution is disabled. Calling
     * {@link SAXParserFactory#setXIncludeAware(boolean) setXIncludeAware(true)} on the returned factory does not re-enable resolution; a parse that encounters
     * an {@code xi:include} element fails.</p>
     *
     * @return a hardened factory.
     * @throws IllegalStateException if the underlying JAXP implementation is not recognised by any bundled hardening recipe, or if the matching recipe cannot
     *         apply its settings to it.
     */
    public static SAXParserFactory newSAXParserFactory() {
        return dispatch(SAXParserFactory.newInstance());
    }

    /**
     * Returns a fresh, hardened {@link SchemaFactory} configured for W3C XML Schema ({@link XMLConstants#W3C_XML_SCHEMA_NS_URI}).
     *
     * <p>Beyond the three universal guarantees on {@link XmlFactories}:</p>
     *
     * <ul>
     *   <li>{@code xs:import}, {@code xs:include} and {@code xs:redefine} schemaLocation URIs are not resolved during schema compilation, and</li>
     *   <li>{@code xsi:schemaLocation} / {@code xsi:noNamespaceSchemaLocation} hints in instance documents are not resolved during validation.</li>
     * </ul>
     *
     * <p></p>The same guarantees apply to {@link javax.xml.validation.Validator} and {@link javax.xml.validation.ValidatorHandler} instances produced from the
     * resulting {@link javax.xml.validation.Schema}.</p>
     *
     * @return a hardened factory.
     * @throws IllegalStateException if the underlying Schema implementation is not recognised by any bundled hardening recipe, or if the matching recipe
     *         cannot apply its settings to it.
     */
    public static SchemaFactory newSchemaFactory() {
        return dispatch(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI));
    }

    /**
     * Returns a fresh, hardened {@link TransformerFactory}.
     *
     * <p>Beyond the three universal guarantees on {@link XmlFactories}: {@code xsl:import}, {@code xsl:include} and {@code document()} URIs are not
     * resolved.</p>
     *
     * <p>The guarantees apply to every parser the factory creates internally, both for stylesheet compilation and for source-document reading at
     * {@code Transformer.transform(Source, Result)} time.</p>
     *
     * @return a hardened factory.
     * @throws IllegalStateException if the underlying TrAX implementation is not recognised by any bundled hardening recipe, or if the matching recipe cannot
     *         apply its settings to it.
     */
    public static TransformerFactory newTransformerFactory() {
        return dispatch(TransformerFactory.newInstance());
    }

    /**
     * Returns a fresh, hardened {@link XMLInputFactory}.
     *
     * <p>The three universal guarantees on {@link XmlFactories} apply; StAX exposes no additional vectors beyond them.</p>
     *
     * @return a hardened factory.
     * @throws IllegalStateException if the underlying StAX implementation is not recognised by any bundled hardening recipe, or if the matching recipe cannot
     *         apply its settings to it.
     */
    public static XMLInputFactory newXMLInputFactory() {
        return dispatch(XMLInputFactory.newInstance());
    }

    /**
     * Returns a fresh, hardened {@link XPathFactory} for the default XPath object model.
     *
     * <p>Beyond the three universal guarantees on {@link XmlFactories}, URI-fetching XPath 3.1+ functions ({@code doc()}, {@code collection()},
     * {@code unparsed-text()}) are not resolved.</p>
     *
     * @return a hardened factory.
     * @throws IllegalStateException if the underlying XPath implementation is not recognised by any bundled hardening recipe, or if the matching recipe cannot
     *         apply its settings to it.
     */
    public static XPathFactory newXPathFactory() {
        return dispatch(XPathFactory.newInstance());
    }

    private static HardeningException noProvider(final Object factory) {
        return new HardeningException("No hardening recipe for JAXP factory class " + factory.getClass().getName());
    }

    private XmlFactories() {
        // static only
    }
}
