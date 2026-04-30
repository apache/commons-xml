/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import static eu.copernik.xml.factory.JaxpSetters.setFeature;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.xpath.XPathFactory;

/**
 * Hardening recipes for the external Apache Xalan distribution (the {@code xalan:xalan} artifact).
 *
 * <p>Factory classes live under the {@code org.apache.xalan.*} and {@code org.apache.xpath.*} packages. Xalan ships only TrAX and XPath; its DOM, SAX, StAX and
 * Schema needs are served by whatever JDK or external Xerces is on the classpath.</p>
 *
 * <p>Hardening recipe applied to every factory below uses the same building blocks:</p>
 * <ul>
 *     <li><strong>FSP</strong> ({@link XMLConstants#FEATURE_SECURE_PROCESSING}, set to {@code true}): enables Xalan's secure-processing mode, which disables
 *         reflection-based extension functions. Required.</li>
 *     <li><strong>{@link Resolvers.DenyAll#URI}</strong>: required. Xalan does not implement the JAXP 1.5 {@code ACCESS_EXTERNAL_*} attributes; a deny-all
 *         {@link javax.xml.transform.URIResolver} blocks {@code xsl:include}, {@code xsl:import}, {@code xsl:source-document} during stylesheet compilation
 *         and {@code document()}, {@code unparsed-text()}, {@code collection()} at runtime.</li>
 *     <li><strong>{@link HardeningTransformerFactory} + {@link HardeningTemplates} + {@link HardeningTransformer}</strong>: required. Xalan's source-document
 *         parsing path falls back to {@code SAXParserFactory.newInstance()} whenever the input is not a {@link javax.xml.transform.dom.DOMSource} or a
 *         {@link javax.xml.transform.sax.SAXSource} carrying its own {@link org.xml.sax.XMLReader}; it only sets FSP on the resulting reader. The wrappers
 *         rewrite every Source through an {@link XmlFactories}-hardened reader so the Xalan internals never get to provision their own.</li>
 * </ul>
 *
 * <h2>Caveats</h2>
 * <ul>
 *     <li>Replacing the {@code URIResolver} on the factory or transformer cancels the deny-all hardening for XSLT URI fetches; Xalan exposes no
 *         tamper-resistant equivalent of JAXP 1.5 {@code ACCESS_EXTERNAL_STYLESHEET}.</li>
 * </ul>
 */
final class XalanProvider {

    static TransformerFactory configure(final TransformerFactory factory) {
        // Required: enables Xalan's secure-processing mode (extension-function block)
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Required: Xalan does not honour JAXP 1.5 ACCESS_EXTERNAL_*; the deny-all URIResolver blocks:
        // xsl:import/xsl:include at compile time and document()/unparsed-text() at runtime.
        factory.setURIResolver(Resolvers.DenyAll.URI);
        // Required: Xalan's internal SAX reader is sourced from SAXParserFactory.newInstance() and only carries FSP.
        // We replace it with our hardened factory
        return new HardeningTransformerFactory((SAXTransformerFactory) factory);
    }

    static XPathFactory configure(final XPathFactory factory) {
        // Required: enables Xalan's secure-processing mode; XPathFactory has no property API for finer control.
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory;
    }

    private XalanProvider() {
    }
}
