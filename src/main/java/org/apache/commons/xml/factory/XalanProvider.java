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

import static org.apache.commons.xml.factory.JaxpSetters.setFeature;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

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
 *     <li><strong>{@link DenyAllResolver#URI}</strong>: required. Xalan does not implement the JAXP 1.5 {@code ACCESS_EXTERNAL_*} attributes; a deny-all
 *         {@link javax.xml.transform.URIResolver} blocks {@code xsl:include}, {@code xsl:import}, {@code xsl:source-document} during stylesheet compilation
 *         and {@code document()}, {@code unparsed-text()}, {@code collection()} at runtime.</li>
 *     <li>
 *         <p><strong>{@code Hardening*} wrappers + source rewriting</strong>: required. Xalan's source-document parsing path falls back to
 *         {@code SAXParserFactory.newInstance()} whenever the input is not a {@link DOMSource} or a {@link SAXSource} carrying its own {@link XMLReader}; it
 *         only sets FSP on the resulting reader. Three wrapper layers are needed:</p>
 *         <ol>
 *             <li>{@link HardeningTransformerFactory} rewrites the Source on every entry point that compiles a stylesheet.</li>
 *             <li>{@link HardeningTemplates} returns a hardened Transformer from {@link Templates#newTransformer()} so runtime source parsing is also
 *                 covered.</li>
 *             <li>{@link HardeningTransformer} rewrites the Source on {@link Transformer#transform(Source, Result)}.</li>
 *         </ol>
 *     </li>
 * </ul>
 *
 * <h2>Caveats</h2>
 * <ul>
 *     <li>Replacing the {@code URIResolver} on the factory or transformer cancels the deny-all hardening for XSLT URI fetches; Xalan exposes no
 *         tamper-resistant equivalent of JAXP 1.5 {@code ACCESS_EXTERNAL_STYLESHEET}.</li>
 *     <li>The wrappers trust a {@link SAXSource} that carries its own {@link XMLReader}: the caller is expected to supply a hardened reader (via
 *         {@link XmlFactories#newSAXParserFactory()} or {@link XmlFactories#harden(XMLReader)}) in that case.</li>
 *     <li>{@link TransformerHandler} returned from {@code newTransformerHandler} is not wrapped: it processes incoming SAX events instead of reading a Source,
 *         so it has no inner Source-parsing path. A caller who pulls the inner {@link Transformer} via {@link TransformerHandler#getTransformer()} bypasses
 *         the runtime source rewrite.</li>
 * </ul>
 */
final class XalanProvider {

    /**
     * {@link TransformerFactory} wrapper that rewrites every Source-taking entry point through {@link #hardenSource(Source)} before delegating.
     */
    private static final class HardeningTransformerFactory extends DelegatingTransformerFactory {

        HardeningTransformerFactory(final SAXTransformerFactory delegate) {
            super(delegate);
        }

        @Override
        public Source getAssociatedStylesheet(final Source source, final String media, final String title, final String charset)
                throws TransformerConfigurationException {
            return super.getAssociatedStylesheet(hardenSource(source), media, title, charset);
        }

        @Override
        public Templates newTemplates(final Source source) throws TransformerConfigurationException {
            final Templates templates = super.newTemplates(hardenSource(source));
            return templates == null ? null : new HardeningTemplates(templates);
        }

        @Override
        public Transformer newTransformer() throws TransformerConfigurationException {
            // Identity transformer: still parses runtime sources, so wrap it to harden Transformer.transform(Source, Result).
            final Transformer transformer = super.newTransformer();
            if (transformer == null) {
                return null;
            }
            return new HardeningTransformer(transformer);
        }

        @Override
        public Transformer newTransformer(final Source source) throws TransformerConfigurationException {
            final Transformer transformer = super.newTransformer(hardenSource(source));
            if (transformer == null) {
                return null;
            }
            return new HardeningTransformer(transformer);
        }

        @Override
        public TransformerHandler newTransformerHandler(final Source source) throws TransformerConfigurationException {
            return super.newTransformerHandler(hardenSource(source));
        }
    }

    /**
     * {@link Templates} wrapper whose only purpose is to return a {@link HardeningTransformer} from {@link Templates#newTransformer()}.
     */
    private static final class HardeningTemplates extends DelegatingTemplates {

        HardeningTemplates(final Templates delegate) {
            super(delegate);
        }

        @Override
        public Transformer newTransformer() throws TransformerConfigurationException {
            final Transformer transformer = super.newTransformer();
            if (transformer == null) {
                return null;
            }
            return new HardeningTransformer(transformer);
        }
    }

    /**
     * {@link Transformer} wrapper that rewrites the Source on every {@link Transformer#transform(Source, Result)} call.
     */
    private static final class HardeningTransformer extends DelegatingTransformer {

        HardeningTransformer(final Transformer delegate) {
            super(delegate);
        }

        @Override
        public void transform(final Source xmlSource, final Result outputTarget) throws TransformerException {
            super.transform(hardenSource(xmlSource), outputTarget);
        }
    }

    /**
     * Rewrites a Source to one whose XML reader is hardened by {@link XmlFactories}.
     *
     * <p>Pass-through cases (the caller is trusted to have arranged equivalent hardening already):</p>
     * <ul>
     *     <li>{@link DOMSource}: the tree is already built; no parser is invoked.</li>
     *     <li>{@link SAXSource} carrying its own {@link XMLReader}: the caller's reader is reused, matching Xalan's own contract.</li>
     * </ul>
     *
     * <p>Otherwise the source is converted via {@link SAXSource#sourceToInputSource} and re-wrapped in a {@link SAXSource} backed by an
     * {@link XmlFactories}-hardened reader.</p>
     */
    private static Source hardenSource(final Source source) throws TransformerConfigurationException {
        if (source instanceof DOMSource || source instanceof SAXSource && ((SAXSource) source).getXMLReader() != null) {
            return source;
        }
        final InputSource inputSource = SAXSource.sourceToInputSource(source);
        if (inputSource == null) {
            return source;
        }
        try {
            final XMLReader reader = XmlFactories.newSAXParserFactory().newSAXParser().getXMLReader();
            final SAXSource hardened = new SAXSource(reader, inputSource);
            hardened.setSystemId(source.getSystemId());
            return hardened;
        } catch (final ParserConfigurationException | SAXException e) {
            throw new TransformerConfigurationException("Failed to obtain a hardened XMLReader for Xalan source parsing", e);
        }
    }

    static TransformerFactory configure(final TransformerFactory factory) {
        // Required: enables Xalan's secure-processing mode (extension-function block)
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Required: Xalan does not honour JAXP 1.5 ACCESS_EXTERNAL_*; the deny-all URIResolver blocks:
        // xsl:import/xsl:include at compile time and document()/unparsed-text() at runtime.
        factory.setURIResolver(DenyAllResolver.URI);
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
