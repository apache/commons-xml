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

package org.apache.commons.xml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParser;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Checks that a caller-supplied {@link EntityResolver} cannot remove the hardened deny-all floor.
 *
 * <p>A hardened DOM/SAX parser wraps the caller's resolver in a {@link Resolvers.FallbackDenyResolver} and delegates to it: a resource the caller resolves
 * (returns a non-null {@link InputSource}) is allowed, but anything it does not resolve ({@code null}) is denied instead of fetched. The payload declares an
 * external general entity in the internal subset and references it, so the entity expands to {@link AttackTestSupport#LEAKED_MARKER} only if the systemId is
 * actually resolved.</p>
 *
 * <p>The deny assertions hold on every provider (Xerces denies via the floor, the stock JDK via {@code ACCESS_EXTERNAL_*}); the resolve assertions exercise the
 * opt-in path that the floor must not break.</p>
 */
class EntityResolverFloorTest {

    /** systemId the allow-list resolver permits. */
    private static final String ALLOWED = AttackTestSupport.resourceUrl("referenced.txt").toString();

    /** systemId the allow-list resolver does not resolve (and the floor must deny). */
    private static final String UNLISTED = AttackTestSupport.resourceUrl("referenced.xml").toString();

    /** Resolves only {@link #ALLOWED}; returns {@code null} for anything else. */
    private static final EntityResolver ALLOW_LIST = (publicId, systemId) ->
            ALLOWED.equals(systemId) ? new InputSource(new URL(systemId).openStream()) : null;

    private static String payload(final String entitySystemId) {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE root [\n  <!ENTITY xxe SYSTEM \"" + entitySystemId + "\">\n]>\n"
                + "<root>&xxe;</root>";
    }

    /** Error handler that re-throws so a denied fetch surfaces as a thrown exception rather than a swallowed error. */
    private static DefaultHandler strict() {
        return new DefaultHandler() {
            @Override
            public void error(final SAXParseException e) throws SAXException {
                throw e;
            }
        };
    }

    private static DocumentBuilder hardenedBuilder() throws Exception {
        final DocumentBuilder builder = XmlFactories.newDocumentBuilderFactory().newDocumentBuilder();
        builder.setErrorHandler(strict());
        return builder;
    }

    private static XMLReader hardenedReader() throws Exception {
        final XMLReader reader = XmlFactories.newSAXParserFactory().newSAXParser().getXMLReader();
        reader.setErrorHandler(strict());
        return reader;
    }

    @Test
    @Tag("dom")
    void domResolvesAllowListed() throws Exception {
        Assumptions.assumeTrue(AttackTestSupport.DOM_RESOLVES_INTERNAL_ENTITIES, "platform DOM does not resolve user-defined entities");
        final DocumentBuilder builder = hardenedBuilder();
        builder.setEntityResolver(ALLOW_LIST);
        final Document doc = builder.parse(AttackTestSupport.inputSource(payload(ALLOWED)));
        assertTrue(doc.getDocumentElement().getTextContent().contains(AttackTestSupport.LEAKED_MARKER),
                "allow-listed external entity should resolve through the caller's resolver");
    }

    @Test
    @Tag("dom")
    void domDeniesUnlisted() throws Exception {
        Assumptions.assumeTrue(AttackTestSupport.DOM_RESOLVES_INTERNAL_ENTITIES, "platform DOM does not resolve user-defined entities");
        final DocumentBuilder builder = hardenedBuilder();
        builder.setEntityResolver(ALLOW_LIST);
        assertThrows(SAXException.class, () -> builder.parse(AttackTestSupport.inputSource(payload(UNLISTED))));
    }

    @Test
    @Tag("sax")
    void saxReaderResolvesAllowListed() throws Exception {
        final XMLReader reader = hardenedReader();
        reader.setEntityResolver(ALLOW_LIST);
        final StringBuilder text = new StringBuilder();
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                text.append(ch, start, length);
            }
        });
        reader.parse(AttackTestSupport.inputSource(payload(ALLOWED)));
        assertTrue(text.toString().contains(AttackTestSupport.LEAKED_MARKER),
                "allow-listed external entity should resolve through the caller's resolver");
    }

    @Test
    @Tag("sax")
    void saxReaderDeniesUnlisted() throws Exception {
        final XMLReader reader = hardenedReader();
        reader.setEntityResolver(ALLOW_LIST);
        reader.setContentHandler(new DefaultHandler());
        assertThrows(SAXException.class, () -> reader.parse(AttackTestSupport.inputSource(payload(UNLISTED))));
    }

    @Test
    @Tag("sax")
    void saxParseWithHandlerDoesNotBypass() throws Exception {
        // SAXParser.parse(source, handler) installs the handler as the reader's entity resolver; the handler does not resolve it (returns null), so the
        // deny-all floor must still block the external entity rather than letting the parser fetch it.
        final SAXParser parser = XmlFactories.newSAXParserFactory().newSAXParser();
        final StringBuilder text = new StringBuilder();
        final DefaultHandler handler = new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                text.append(ch, start, length);
            }
        };
        try {
            parser.parse(AttackTestSupport.inputSource(payload(ALLOWED)), handler);
        } catch (final SAXException e) {
            return; // blocked at parse: acceptable
        }
        assertFalse(text.toString().contains(AttackTestSupport.LEAKED_MARKER), "parse(source, handler) leaked the external entity:\n" + text);
    }
}
