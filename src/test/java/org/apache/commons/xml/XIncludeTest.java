/*
 * SPDX-FileCopyrightText: 2026 Ta Thien
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 *
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

package org.apache.commons.xml;

import static org.apache.commons.xml.AttackTestSupport.LEAKED_MARKER;
import static org.apache.commons.xml.AttackTestSupport.inputSource;
import static org.apache.commons.xml.AttackTestSupport.resourceUrl;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tests that XInclude resolution is denied by default on factories from {@link XmlFactories}, and that callers can
 * allow-list specific resources via an {@link EntityResolver}.
 *
 * <p>Each case is exercised in both {@code parse="xml"} and {@code parse="text"} modes, and for both DOM and SAX
 * paths. XInclude resolution requires namespace-aware processing; the baseline tests set it explicitly, and the
 * hardened factory tests rely on the underlying JAXP implementation being namespace-aware enough to recognise elements
 * in the {@code http://www.w3.org/2001/XInclude} namespace.</p>
 */
class XIncludeTest {

    /** Absolute URL of the XML fixture pulled in by {@code parse="xml"} includes; carries {@link AttackTestSupport#LEAKED_MARKER}. */
    private static final String REFERENCED_XML = resourceUrl("referenced.xml").toString();

    /** Absolute URL of the text fixture pulled in by {@code parse="text"} includes; carries {@link AttackTestSupport#LEAKED_MARKER}. */
    private static final String REFERENCED_TEXT = resourceUrl("referenced.txt").toString();

    /**
     * Resolves only the allowed URL; all other lookups throw. This mirrors the pattern a production caller would use
     * to allow-list trusted resources.
     */
    private static final class AllowListResolver implements EntityResolver {

        private final String allowedUrl;

        AllowListResolver(final String allowedUrl) {
            this.allowedUrl = allowedUrl;
        }

        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException, IOException {
            if (allowedUrl.equals(systemId)) {
                InputSource inputSource = new InputSource(new StringReader("<allowed xmlns:xi=\"http://www.w3.org/2001/XInclude\">resolved</allowed>"));
                inputSource.setPublicId(publicId);
                inputSource.setSystemId(systemId);
                return inputSource;
            }
            throw new SAXException("Blocked by allow-list resolver: " + systemId);
        }
    }

    /** XML wrapper for xi:include in the given {@code parse} mode referencing {@code href}. */
    private static String xiIncludeXml(final String href, final String parseMode) {
        return "<?xml version=\"1.0\"?>\n"
                + "<root xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "  <xi:include href=\"" + href + "\" parse=\"" + parseMode + "\"/>\n"
                + "</root>";
    }

    /**
     * Enables XInclude on the factory under test, skipping the test when the platform refuses.
     *
     * <p>On Android{@code setXIncludeAware(true)} always throws {@link UnsupportedOperationException}.</p>
     */
    private static void assumeXIncludeAware(final DocumentBuilderFactory factory) {
        try {
            factory.setXIncludeAware(true);
        } catch (final UnsupportedOperationException e) {
            Assumptions.abort("XInclude not supported on this platform");
        }
    }

    /**
     * Enables XInclude on the factory under test, skipping the test when the platform refuses.
     *
     * <p>On Android{@code setXIncludeAware(true)} always throws {@link UnsupportedOperationException}.</p>
     */
    private static void assumeXIncludeAware(final SAXParserFactory factory) {
        try {
            factory.setXIncludeAware(true);
        } catch (final UnsupportedOperationException e) {
            Assumptions.abort("XInclude not supported on this platform");
        }
    }

    //region Baseline: unhardened JAXP resolves the include

    @Test
    @Tag("dom")
    void baselineDomLeaksParseXml() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_XML, "xml"));

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        final Document doc = factory.newDocumentBuilder().parse(input);
        final String text = doc.getDocumentElement().getTextContent();
        assertTrue(text != null && text.contains(LEAKED_MARKER),
                "Baseline DOM parse=xml should leak marker; got: " + text);
    }

    @Test
    @Tag("dom")
    void baselineDomLeaksParseText() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_TEXT, "text"));

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        final Document doc = factory.newDocumentBuilder().parse(input);
        final String text = doc.getDocumentElement().getTextContent();
        assertTrue(text != null && text.contains(LEAKED_MARKER),
                "Baseline DOM parse=text should leak marker; got: " + text);
    }

    @Test
    @Tag("sax")
    void baselineSaxLeaksParseXml() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_XML, "xml"));

        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        final StringBuilder captured = new StringBuilder();
        final XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                captured.append(ch, start, length);
            }
        });
        reader.parse(input);
        assertTrue(captured.toString().contains(LEAKED_MARKER),
                "Baseline SAX parse=xml should leak marker; got: " + captured);
    }

    @Test
    @Tag("sax")
    void baselineSaxLeaksParseText() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_TEXT, "text"));

        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        final StringBuilder captured = new StringBuilder();
        final XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                captured.append(ch, start, length);
            }
        });
        reader.parse(input);
        assertTrue(captured.toString().contains(LEAKED_MARKER),
                "Baseline SAX parse=text should leak marker; got: " + captured);
    }

    //endregion

    //region Hardened factory: fails closed (throws)

    @Test
    @Tag("dom")
    void hardenedDomBlocksParseXml() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_XML, "xml"));

        final DocumentBuilderFactory factory = XmlFactories.newDocumentBuilderFactory();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        assertThrows(Exception.class, () -> {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(input);
        }, "Hardened DOM parse=xml should throw");
    }

    @Test
    @Tag("dom")
    void hardenedDomBlocksParseText() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_TEXT, "text"));

        final DocumentBuilderFactory factory = XmlFactories.newDocumentBuilderFactory();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        assertThrows(Exception.class, () -> {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(input);
        }, "Hardened DOM parse=text should throw");
    }

    @Test
    @Tag("sax")
    void hardenedSaxBlocksParseXml() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_XML, "xml"));

        final SAXParserFactory factory = XmlFactories.newSAXParserFactory();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        assertThrows(Exception.class, () -> {
            final XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.parse(input);
        }, "Hardened SAX parse=xml should throw");
    }

    @Test
    @Tag("sax")
    void hardenedSaxBlocksParseText() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_TEXT, "text"));

        final SAXParserFactory factory = XmlFactories.newSAXParserFactory();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        assertThrows(Exception.class, () -> {
            final XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.parse(input);
        }, "Hardened SAX parse=text should throw");
    }

    //endregion

    //region Hardened factory + allow-list resolver: allowed href works, non-allowed throws

    @Test
    @Tag("dom")
    void hardenedDomWithAllowListResolvesParseXml() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_XML, "xml"));

        final DocumentBuilderFactory factory = XmlFactories.newDocumentBuilderFactory();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new AllowListResolver(REFERENCED_XML));
        final Document doc = builder.parse(input);
        final String text = doc.getDocumentElement().getTextContent();
        assertTrue(text != null && text.contains("resolved"),
                "DOM parse=xml with allow-list should resolve; got: " + text);
    }

    @Test
    @Tag("dom")
    void hardenedDomWithAllowListResolvesParseText() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_TEXT, "text"));

        final DocumentBuilderFactory factory = XmlFactories.newDocumentBuilderFactory();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new AllowListResolver(REFERENCED_TEXT));
        final Document doc = builder.parse(input);
        final String text = doc.getDocumentElement().getTextContent();
        assertTrue(text != null && text.contains("resolved"),
                "DOM parse=text with allow-list should resolve; got: " + text);
    }

    @Test
    @Tag("dom")
    void hardenedDomWithAllowListBlocksNonAllowed() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_XML, "xml"));

        final DocumentBuilderFactory factory = XmlFactories.newDocumentBuilderFactory();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new AllowListResolver("/nonexistent"));
        assertThrows(Exception.class, () -> builder.parse(input),
                "DOM with allow-list should block non-allowed href");
    }

    @Test
    @Tag("sax")
    void hardenedSaxWithAllowListResolvesParseXml() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_XML, "xml"));

        final SAXParserFactory factory = XmlFactories.newSAXParserFactory();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        final XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setEntityResolver(new AllowListResolver(REFERENCED_XML));
        final StringBuilder captured = new StringBuilder();
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                captured.append(ch, start, length);
            }
        });
        reader.parse(input);
        assertTrue(captured.toString().contains("resolved"),
                "SAX parse=xml with allow-list should resolve; got: " + captured);
    }

    @Test
    @Tag("sax")
    void hardenedSaxWithAllowListResolvesParseText() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_TEXT, "text"));

        final SAXParserFactory factory = XmlFactories.newSAXParserFactory();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        final XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setEntityResolver(new AllowListResolver(REFERENCED_TEXT));
        final StringBuilder captured = new StringBuilder();
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                captured.append(ch, start, length);
            }
        });
        reader.parse(input);
        assertTrue(captured.toString().contains("resolved"),
                "SAX parse=text with allow-list should resolve; got: " + captured);
    }

    @Test
    @Tag("sax")
    void hardenedSaxWithAllowListBlocksNonAllowed() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_XML, "xml"));

        final SAXParserFactory factory = XmlFactories.newSAXParserFactory();
        factory.setNamespaceAware(true);
        assumeXIncludeAware(factory);
        final XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setEntityResolver(new AllowListResolver("/nonexistent"));
        assertThrows(Exception.class, () -> reader.parse(input),
                "SAX with allow-list should block non-allowed href");
    }

    //endregion

    //region harden(XMLReader): reader with XInclude enabled before hardening is blocked

    @Test
    @Tag("sax")
    void hardenReaderBlocksParseXml() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_XML, "xml"));

        // Reader from an unhardened factory that already has XInclude enabled
        final SAXParserFactory unhardenedFactory = SAXParserFactory.newInstance();
        unhardenedFactory.setNamespaceAware(true);
        assumeXIncludeAware(unhardenedFactory);
        final XMLReader reader = unhardenedFactory.newSAXParser().getXMLReader();
        XmlFactories.harden(reader);
        assertThrows(Exception.class, () -> reader.parse(input),
                "harden(reader) should block XInclude parse=xml on reader with XInclude already enabled");
    }

    @Test
    @Tag("sax")
    void hardenReaderBlocksParseText() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_TEXT, "text"));

        final SAXParserFactory unhardenedFactory = SAXParserFactory.newInstance();
        unhardenedFactory.setNamespaceAware(true);
        assumeXIncludeAware(unhardenedFactory);
        final XMLReader reader = unhardenedFactory.newSAXParser().getXMLReader();
        XmlFactories.harden(reader);
        assertThrows(Exception.class, () -> reader.parse(input),
                "harden(reader) should block XInclude parse=text on reader with XInclude already enabled");
    }

    @Test
    @Tag("sax")
    void hardenReaderAllowListResolvesParseXml() throws Exception {
        final InputSource input = inputSource(xiIncludeXml(REFERENCED_XML, "xml"));

        final SAXParserFactory unhardenedFactory = SAXParserFactory.newInstance();
        unhardenedFactory.setNamespaceAware(true);
        assumeXIncludeAware(unhardenedFactory);
        final XMLReader reader = unhardenedFactory.newSAXParser().getXMLReader();
        XmlFactories.harden(reader);
        reader.setEntityResolver(new AllowListResolver(REFERENCED_XML));
        final StringBuilder captured = new StringBuilder();
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                captured.append(ch, start, length);
            }
        });
        reader.parse(input);
        assertTrue(captured.toString().contains("resolved"),
                "harden(reader) + allow-list should resolve on reader with XInclude already enabled; got: " + captured);
    }

    //endregion
}
