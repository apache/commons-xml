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

import org.apache.commons.xml.factory.internal.ProviderRegistry;
import org.apache.commons.xml.factory.spi.XmlProvider;

/**
 * Entry point for obtaining hardened JAXP factories.
 *
 * <p>Every method on this class returns a <em>fresh, hardened</em> factory instance. No caching or pooling is performed; callers on a hot path are responsible
 * for their own caching.</p>
 *
 * <p>Hardening blocks the secondary external-resource fetches an implementation would otherwise initiate while processing document content:</p>
 *
 * <ul>
 *   <li>DOCTYPE declarations and external DTDs,</li>
 *   <li>external general and parameter entities,</li>
 *   <li>{@code xsi:schemaLocation} and {@code xsi:noNamespaceSchemaLocation} hints,</li>
 *   <li>XInclude (where applicable),</li>
 *   <li>{@code xsl:import}, {@code xsl:include} and {@code document()} in XSLT,</li>
 *   <li>{@code doc()}, {@code collection()} and {@code unparsed-text()} in XPath 3.1+,</li>
 *   <li>{@code xs:import}, {@code xs:include} and {@code xs:redefine} in XML Schema.</li>
 * </ul>
 *
 * <p>A top-level URI passed directly by the caller is fetched as-is: {@code StreamSource(systemId)}, {@code DocumentBuilder.parse(String)}, or a
 * {@code SAXSource} built from a system id all cause the JAXP implementation to open that URI without consulting the hardening layer. Use a
 * {@link javax.xml.transform.URIResolver} or {@link org.xml.sax.EntityResolver} if you need to restrict the top-level fetch.</p>
 *
 * <p>The returned factories inherit the thread-safety properties of the underlying JAXP implementation, which in practice means they are <strong>not
 * guaranteed to be thread-safe</strong>. Create a new factory per thread or synchronise externally.</p>
 *
 * <p>This class itself is thread-safe: all methods are static and stateless.</p>
 */
public final class XmlFactories {

    /**
     * Returns a fresh, hardened {@link DocumentBuilderFactory}.
     *
     * <table>
     *   <caption>Hardening applied</caption>
     *   <tr><th>Setting</th><th>State</th></tr>
     *   <tr><td>DOCTYPE</td><td>forbidden</td></tr>
     *   <tr><td>XInclude</td><td>disabled</td></tr>
     *   <tr><td>DTD validation</td><td>disabled</td></tr>
     * </table>
     *
     * <p>Together these block <strong>every</strong> external reference the parser would follow while reading a document through this factory.</p>
     *
     * @return a hardened factory.
     * @throws UnsupportedXmlImplementationException if the underlying JAXP implementation is not recognised by any registered {@link XmlProvider}.
     */
    public static DocumentBuilderFactory newDocumentBuilderFactory() {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        ProviderRegistry.getInstance().providerFor(factory.getClass()).configure(factory);
        return factory;
    }

    /**
     * Returns a fresh, hardened {@link SAXParserFactory}.
     *
     * <table>
     *   <caption>Hardening applied</caption>
     *   <tr><th>Setting</th><th>State</th></tr>
     *   <tr><td>DOCTYPE</td><td>forbidden</td></tr>
     *   <tr><td>XInclude</td><td>disabled</td></tr>
     *   <tr><td>DTD validation</td><td>disabled</td></tr>
     * </table>
     *
     * <p>Together these block <strong>every</strong> external reference the parser would follow while reading a document through this factory.</p>
     *
     * @return a hardened factory.
     * @throws UnsupportedXmlImplementationException if the underlying JAXP implementation is not recognised by any registered {@link XmlProvider}.
     */
    public static SAXParserFactory newSAXParserFactory() {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        ProviderRegistry.getInstance().providerFor(factory.getClass()).configure(factory);
        return factory;
    }

    /**
     * Returns a fresh, hardened {@link SchemaFactory} configured for W3C XML Schema ({@link XMLConstants#W3C_XML_SCHEMA_NS_URI}).
     *
     * <table>
     *   <caption>Hardening applied</caption>
     *   <tr><th>Setting</th><th>State</th></tr>
     *   <tr><td>{@link XMLConstants#FEATURE_SECURE_PROCESSING}</td><td>enabled</td></tr>
     *   <tr><td>{@link XMLConstants#ACCESS_EXTERNAL_DTD}</td><td>blocked (set to {@code ""})</td></tr>
     *   <tr><td>{@link XMLConstants#ACCESS_EXTERNAL_SCHEMA}</td><td>blocked (set to {@code ""})</td></tr>
     * </table>
     *
     * <p>Together these block <strong>every</strong> external reference ({@code xs:import}, {@code xs:include}, {@code xs:redefine}, external DTDs) declared
     * inside the schema that the factory compiles.</p>
     *
     * <p><strong>Limitation.</strong> The schema passed to {@link SchemaFactory#newSchema(javax.xml.transform.Source)} is read by a parser the
     * implementation selects internally, which is not guaranteed to honour DOCTYPE-level hardening. Treat schemas as trusted, or pre-parse the schema through
     * a hardened {@link #newDocumentBuilderFactory()}/{@link #newSAXParserFactory()} and pass a {@link javax.xml.transform.dom.DOMSource} or
     * {@link javax.xml.transform.sax.SAXSource}.</p>
     *
     * <p></p>Validators created from the returned {@link javax.xml.validation.Schema} inherit the hardening for secondary fetches, but the XML input given to
     * {@link javax.xml.validation.Validator#validate(javax.xml.transform.Source) Validator.validate(Source)} is subject to the same top-level-URI caveat as
     * above.</p>
     *
     * @return a hardened factory.
     * @throws UnsupportedXmlImplementationException if the underlying Schema implementation is not recognised by any registered {@link XmlProvider}.
     */
    public static SchemaFactory newSchemaFactory() {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        ProviderRegistry.getInstance().providerFor(factory.getClass()).configure(factory);
        return factory;
    }

    /**
     * Returns a fresh, hardened {@link TransformerFactory}.
     *
     * <table>
     *   <caption>Hardening applied</caption>
     *   <tr><th>Setting</th><th>State</th></tr>
     *   <tr><td>{@link XMLConstants#FEATURE_SECURE_PROCESSING}</td><td>enabled</td></tr>
     *   <tr><td>{@link XMLConstants#ACCESS_EXTERNAL_DTD}</td><td>blocked (set to {@code ""})</td></tr>
     *   <tr><td>{@link XMLConstants#ACCESS_EXTERNAL_STYLESHEET}</td><td>blocked (set to {@code ""})</td></tr>
     * </table>
     *
     * <p>Together these block <strong>every</strong> external reference ({@code xsl:import}, {@code xsl:include}, {@code document()}, external DTDs) declared
     * in the stylesheet that the factory compiles.</p>
     *
     * <p><strong>Limitation.</strong> The stylesheet passed to {@link TransformerFactory#newTransformer(javax.xml.transform.Source)} and the XML input passed
     * to {@code Transformer.transform(Source, Result)} are read by a parser the JAXP implementation selects internally, which is not guaranteed to honour
     * DOCTYPE-level hardening. Treat stylesheets as trusted, or pre-parse the stylesheet and input through a hardened
     * {@link #newDocumentBuilderFactory()}/{@link #newSAXParserFactory()} and pass them as {@link javax.xml.transform.dom.DOMSource} or
     * {@link javax.xml.transform.sax.SAXSource}.</p>
     *
     * @return a hardened factory.
     * @throws UnsupportedXmlImplementationException if the underlying TrAX implementation is not recognised by any registered {@link XmlProvider}.
     */
    public static TransformerFactory newTransformerFactory() {
        final TransformerFactory factory = TransformerFactory.newInstance();
        ProviderRegistry.getInstance().providerFor(factory.getClass()).configure(factory);
        return factory;
    }

    /**
     * Returns a fresh, hardened {@link XMLInputFactory}.
     *
     * <table>
     *   <caption>Hardening applied</caption>
     *   <tr><th>Setting</th><th>State</th></tr>
     *   <tr><td>DOCTYPE</td><td>processing disabled</td></tr>
     *   <tr><td>XInclude</td><td>N/A</td></tr>
     *   <tr><td>DTD validation</td><td>disabled</td></tr>
     * </table>
     *
     * <p>StAX has no XInclude, so nothing further is needed. Together these block <strong>every</strong> external reference the parser would follow while
     * reading a document through this factory.</p>
     *
     * <p></p>Unlike DOM and SAX, a DOCTYPE is tolerated rather than rejected: the declaration is read, but the DTD body is never processed, so entity
     * references declared in the DTD fail at the reference site. The settings map to {@link XMLInputFactory#SUPPORT_DTD} and
     * {@link XMLInputFactory#IS_VALIDATING}.</p>
     *
     * @return a hardened factory.
     * @throws UnsupportedXmlImplementationException if the underlying StAX implementation is not recognised by any registered {@link XmlProvider}.
     */
    public static XMLInputFactory newXMLInputFactory() {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        ProviderRegistry.getInstance().providerFor(factory.getClass()).configure(factory);
        return factory;
    }

    /**
     * Returns a fresh, hardened {@link XPathFactory} for the default XPath object model.
     *
     * <table>
     *   <caption>Hardening applied</caption>
     *   <tr><th>Setting</th><th>State</th></tr>
     *   <tr><td>{@link XMLConstants#FEATURE_SECURE_PROCESSING}</td><td>enabled</td></tr>
     * </table>
     *
     * <p>This blocks external-URI access from XPath 3.1+ functions such as {@code doc()}, {@code collection()} and {@code unparsed-text()} where the
     * underlying implementation honours the flag.</p>
     *
     * @return a hardened factory.
     * @throws UnsupportedXmlImplementationException if the underlying XPath implementation is not recognised by any registered {@link XmlProvider}.
     */
    public static XPathFactory newXPathFactory() {
        final XPathFactory factory = XPathFactory.newInstance();
        ProviderRegistry.getInstance().providerFor(factory.getClass()).configure(factory);
        return factory;
    }

    private XmlFactories() {
        // static only
    }
}
