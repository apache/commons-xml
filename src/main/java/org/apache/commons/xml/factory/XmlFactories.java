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
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.xml.factory.internal.CompositeProvider;
import org.apache.commons.xml.factory.spi.XmlProvider;
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

    /**
     * Hardens an existing {@link XMLReader}.
     *
     * @param reader the reader to harden; never {@code null}.
     * @return a hardened reader.
     * @throws IllegalStateException if no registered {@link XmlProvider} recognises the reader's concrete class, or if the recognised provider cannot apply the
     *         hardening settings to it.
     */
    public static XMLReader harden(final XMLReader reader) {
        return CompositeProvider.getInstance().configure(reader);
    }

    /**
     * Returns a fresh, hardened {@link DocumentBuilderFactory}.
     *
     * <p>Beyond the three universal guarantees on {@link XmlFactories}, XInclude resolution is disabled. Calling
     * {@link DocumentBuilderFactory#setXIncludeAware(boolean) setXIncludeAware(true)} on the returned factory does not re-enable resolution; a parse that
     * encounters an {@code xi:include} element fails.</p>
     *
     * @return a hardened factory.
     * @throws IllegalStateException if no registered {@link XmlProvider} recognises the underlying JAXP implementation, or if the recognised provider cannot
     *         apply the hardening settings to it.
     */
    public static DocumentBuilderFactory newDocumentBuilderFactory() {
        return CompositeProvider.getInstance().configure(DocumentBuilderFactory.newInstance());
    }

    /**
     * Returns a fresh, hardened {@link SAXParserFactory}.
     *
     * <p>Beyond the three universal guarantees on {@link XmlFactories}, XInclude resolution is disabled. Calling
     * {@link SAXParserFactory#setXIncludeAware(boolean) setXIncludeAware(true)} on the returned factory does not re-enable resolution; a parse that encounters
     * an {@code xi:include} element fails.</p>
     *
     * @return a hardened factory.
     * @throws IllegalStateException if no registered {@link XmlProvider} recognises the underlying JAXP implementation, or if the recognised provider cannot
     *         apply the hardening settings to it.
     */
    public static SAXParserFactory newSAXParserFactory() {
        return CompositeProvider.getInstance().configure(SAXParserFactory.newInstance());
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
     * @throws IllegalStateException if no registered {@link XmlProvider} recognises the underlying Schema implementation, or if the recognised provider cannot
     *         apply the hardening settings to it.
     */
    public static SchemaFactory newSchemaFactory() {
        return CompositeProvider.getInstance().configure(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI));
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
     * @throws IllegalStateException if no registered {@link XmlProvider} recognises the underlying TrAX implementation, or if the recognised provider cannot
     *         apply the hardening settings to it.
     */
    public static TransformerFactory newTransformerFactory() {
        return CompositeProvider.getInstance().configure(TransformerFactory.newInstance());
    }

    /**
     * Returns a fresh, hardened {@link XMLInputFactory}.
     *
     * <p>The three universal guarantees on {@link XmlFactories} apply; StAX exposes no additional vectors beyond them.</p>
     *
     * @return a hardened factory.
     * @throws IllegalStateException if no registered {@link XmlProvider} recognises the underlying StAX implementation, or if the recognised provider cannot
     *         apply the hardening settings to it.
     */
    public static XMLInputFactory newXMLInputFactory() {
        return CompositeProvider.getInstance().configure(XMLInputFactory.newInstance());
    }

    /**
     * Returns a fresh, hardened {@link XPathFactory} for the default XPath object model.
     *
     * <p>Beyond the three universal guarantees on {@link XmlFactories}, URI-fetching XPath 3.1+ functions ({@code doc()}, {@code collection()},
     * {@code unparsed-text()}) are not resolved.</p>
     *
     * @return a hardened factory.
     * @throws IllegalStateException if no registered {@link XmlProvider} recognises the underlying XPath implementation, or if the recognised provider cannot
     *         apply the hardening settings to it.
     */
    public static XPathFactory newXPathFactory() {
        return CompositeProvider.getInstance().configure(XPathFactory.newInstance());
    }

    private XmlFactories() {
        // static only
    }
}
