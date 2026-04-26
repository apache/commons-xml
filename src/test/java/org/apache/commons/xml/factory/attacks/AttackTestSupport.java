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
package org.apache.commons.xml.factory.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.xml.factory.XmlFactories;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Shared fixtures for attack tests.
 *
 * <p>The hardened-side helpers come in two flavours, distinguished by their suffix:</p>
 *
 * <ul>
 *   <li>{@code assert*Blocks(...)} runs the payload through a hardened factory from {@link XmlFactories} and asserts the parse throws. Used when the hardening
 *       layer is expected to reject the attack outright.</li>
 *   <li>{@code assert*DoesNotLeak(...)} runs the payload through a hardened factory and asserts the {@link #LEAKED_MARKER} string does not appear in the
 *       output. The parse may either throw or succeed silently. Used where the JAXP API does not give a clean throw hook (SAX content stream, transformer
 *       output) or where the XML spec relaxes the constraint that would otherwise force a throw (DOM with an external DTD subset).</li>
 * </ul>
 *
 * <p>The unconfigured-side positive controls keep verbs that match the JAXP type they exercise: {@code assert*Resolves(payload)} for direct parsing,
 * {@code assert*Compiles(...)} for {@link SchemaFactory} / {@link TransformerFactory} compilation, {@code assertTransformerSucceeds(payload)} for
 * {@code Transformer.transform}, {@code assertValidatorAccepts(payload)} for {@code Validator.validate}.</p>
 *
 * <p>Schema and Templates assertions take a {@link Source} so the same helper covers both inline-string payloads and resource-backed wrappers; build the
 * source via {@link #streamSource(String)} for a string payload or {@link #resourceSource(String)} for a file under {@code src/test/resources/leaked/}. The
 * resource form preserves the system id so relative {@code xs:include} / {@code xs:import} / {@code xs:redefine} / {@code xsl:include} / {@code xsl:import}
 * URIs resolve normally.</p>
 *
 * <p>The two generic primitives {@link #assertParseFails(ThrowingAction, String)} and {@link #assertParseSucceeds(ThrowingAction, String)} are exposed for
 * tests that need to compose a non-standard factory call.</p>
 */
final class AttackTestSupport {

    /**
     * Action that may throw any exception; lets test bodies forward checked JAXP exceptions.
     */
    @FunctionalInterface
    interface ThrowingAction {
        void run() throws Exception;
    }

    /**
     * Action that may throw any exception and returns the captured output text used for the leaked-marker check.
     */
    @FunctionalInterface
    interface ThrowingStringSupplier {
        String get() throws Exception;
    }

    /**
     * Trivial W3C XML Schema that validates {@link #xmlBody(String)} output.
     */
    static final String BENIGN_SCHEMA =
            "<?xml version=\"1.0\"?>\n"
            + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
            + "  <xs:element name=\"root\">\n"
            + "    <xs:complexType>\n"
            + "      <xs:sequence>\n"
            + "        <xs:element name=\"child\" type=\"xs:string\"/>\n"
            + "      </xs:sequence>\n"
            + "    </xs:complexType>\n"
            + "  </xs:element>\n"
            + "</xs:schema>\n";

    /**
     * URL form of the JDK's entity-expansion limit property.
     */
    private static final String JDK_ENTITY_EXPANSION_LIMIT = "http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit";

    /**
     * Text planted in every fixture under {@code src/test/resources/leaked/}.
     *
     * <p>Tests that capture a parser's output assert this string is absent: it can only appear if the hardened parser fetched the external resource, so its
     * presence is the leak signal.</p>
     */
    static final String LEAKED_MARKER = "All your base are belong to us";

    /**
     * Asserts a hardened DOM parse of the payload throws.
     *
     * <p>{@link DocumentBuilder#parse(InputSource)} via {@link XmlFactories#newDocumentBuilderFactory()}; only a thrown exception passes.</p>
     */
    static void assertDomBlocks(final String payload) {
        assertParseFails(() -> XmlFactories.newDocumentBuilderFactory().newDocumentBuilder().parse(inputSource(payload)), "DOM");
    }

    /**
     * Asserts a hardened DOM parse either throws or completes without leaked content.
     *
     * <p>{@link DocumentBuilder#parse(InputSource)} via {@link XmlFactories#newDocumentBuilderFactory()}; XML 1.0 §4.1 permits silent skipping of undeclared
     * entities when an external subset is declared but unread.</p>
     */
    static void assertDomDoesNotLeak(final String payload) {
        assertNoLeak(() -> {
            final Document doc = XmlFactories.newDocumentBuilderFactory().newDocumentBuilder().parse(inputSource(payload));
            return doc.getDocumentElement() == null ? "" : doc.getDocumentElement().getTextContent();
        }, "DOM");
    }

    /**
     * Asserts a hardened DOM parse succeeds.
     *
     * <p>{@link DocumentBuilder#parse(InputSource)} via {@link XmlFactories#newDocumentBuilderFactory()}; positive control for DOCTYPE-only payloads.</p>
     */
    static void assertDomParses(final String payload) {
        assertParseSucceeds(() -> XmlFactories.newDocumentBuilderFactory().newDocumentBuilder().parse(inputSource(payload)), "DOM");
    }

    /**
     * Asserts a permissive DOM parse succeeds.
     *
     * <p>{@link DocumentBuilder#parse(InputSource)} via {@link DocumentBuilderFactory#newInstance()} with FSP off; positive control proving the payload is
     * well-formed.</p>
     */
    static void assertDomResolves(final String payload) {
        assertParseSucceeds(() -> {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            suppressException(() -> factory.setAttribute(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            factory.newDocumentBuilder().parse(inputSource(payload));
        }, "DOM");
    }

    /**
     * Skeleton for every {@code assert*DoesNotLeak} helper.
     *
     * <p>Treats any thrown exception as "hardening blocked at parse" (acceptable); otherwise asserts the captured output omits {@link #LEAKED_MARKER}.</p>
     */
    private static void assertNoLeak(final ThrowingStringSupplier action, final String description) {
        final String output;
        try {
            output = action.get();
        } catch (final Exception e) {
            return; // hardening blocked at parse; acceptable outcome.
        }
        assertFalse(output.contains(LEAKED_MARKER),
                "Hardening did not block " + description + "; output contained marker '" + LEAKED_MARKER + "'.\nFull output:\n" + output);
    }

    /**
     * Asserts the supplied action throws.
     *
     * <p>Generic primitive underlying every {@code assert*Blocks(...)} helper; exposed for tests that compose a non-standard hardened-side call.</p>
     *
     * @param action      the parse to execute.
     * @param description short label included in the failure message.
     */
    static void assertParseFails(final ThrowingAction action, final String description) {
        assertThrows(Exception.class, action::run, "Hardening did not block " + description + "; parse completed successfully.");
    }

    /**
     * Asserts the supplied action does not throw.
     *
     * <p>Generic primitive underlying every {@code assert*Parses(...)} / {@code assert*Compiles(...)} / {@code assert*Resolves(...)} /
     * {@code assert*Succeeds(...)} / {@code assertValidatorAccepts(...)} helper; exposed for tests that compose a non-standard call.</p>
     *
     * @param action      the parse to execute.
     * @param description short label included in the failure message.
     */
    static void assertParseSucceeds(final ThrowingAction action, final String description) {
        assertDoesNotThrow(action::run, "Unconfigured factory should parse " + description + "; the wrapper or its external reference is broken.");
    }

    /**
     * Asserts a hardened SAX parse of the payload throws.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a parser from {@link XmlFactories#newSAXParserFactory()}; only a thrown exception passes.</p>
     */
    static void assertSaxBlocks(final String payload) {
        assertParseFails(() -> parseQuietly(XmlFactories.newSAXParserFactory().newSAXParser().getXMLReader(), payload), "SAX");
    }

    /**
     * Asserts a hardened SAX parse either throws or completes without leaked content.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a parser from {@link XmlFactories#newSAXParserFactory()}; XML 1.0 §4.1 permits silent skipping of undeclared
     * entities when an external subset is declared but unread.</p>
     */
    static void assertSaxDoesNotLeak(final String payload) {
        assertNoLeak(() -> captureCharacters(XmlFactories.newSAXParserFactory().newSAXParser().getXMLReader(), payload), "SAX");
    }

    /**
     * Asserts a hardened SAX parse succeeds.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a parser from {@link XmlFactories#newSAXParserFactory()}; positive control for DOCTYPE-only payloads.</p>
     */
    static void assertSaxParses(final String payload) {
        assertParseSucceeds(() -> parseQuietly(XmlFactories.newSAXParserFactory().newSAXParser().getXMLReader(), payload), "SAX");
    }

    /**
     * Asserts a permissive SAX parse succeeds.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a parser from {@link SAXParserFactory#newInstance()} with FSP off; positive control proving the payload is
     * well-formed.</p>
     */
    static void assertSaxResolves(final String payload) {
        assertParseSucceeds(() -> {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            final XMLReader reader = factory.newSAXParser().getXMLReader();
            suppressException(() -> reader.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            parseQuietly(reader, payload);
        }, "SAX");
    }

    /**
     * Asserts a hardened Schema compilation throws.
     *
     * <p>{@link SchemaFactory#newSchema(Source)} via {@link XmlFactories#newSchemaFactory()}; only a thrown exception passes.</p>
     */
    static void assertSchemaBlocks(final Source xsd) {
        assertParseFails(() -> XmlFactories.newSchemaFactory().newSchema(xsd), "Schema compile");
    }

    /**
     * Asserts a permissive Schema compilation succeeds.
     *
     * <p>{@link SchemaFactory#newSchema(Source)} via {@link SchemaFactory#newInstance(String)} with FSP off; positive control proving the wrapper is
     * well-formed.</p>
     */
    static void assertSchemaCompiles(final Source xsd) {
        assertParseSucceeds(() -> {
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            suppressException(() -> factory.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            factory.newSchema(xsd);
        }, "Schema compile");
    }

    /**
     * Asserts a hardened StAX parse of the payload throws.
     *
     * <p>{@link XMLStreamReader} and {@link XMLEventReader} from {@link XmlFactories#newXMLInputFactory()}; both flavours are exercised and either must
     * throw.</p>
     */
    static void assertStaxBlocks(final String payload) {
        assertParseFails(() -> consumeStreamReader(XmlFactories.newXMLInputFactory(), payload), "StAX stream");
        assertParseFails(() -> consumeEventReader(XmlFactories.newXMLInputFactory(), payload), "StAX event");
    }

    /**
     * Asserts a hardened StAX parse succeeds.
     *
     * <p>{@link XMLStreamReader} from {@link XmlFactories#newXMLInputFactory()}; positive control for DOCTYPE-only payloads.</p>
     */
    static void assertStaxParses(final String payload) {
        assertParseSucceeds(() -> consumeStreamReader(XmlFactories.newXMLInputFactory(), payload), "StAX");
    }

    /**
     * Asserts a permissive StAX parse succeeds.
     *
     * <p>{@link XMLStreamReader} from {@link XMLInputFactory#newInstance()} with FSP off; positive control proving the payload is well-formed.</p>
     */
    static void assertStaxResolves(final String payload) {
        assertParseSucceeds(() -> {
            final XMLInputFactory factory = XMLInputFactory.newInstance();
            suppressException(() -> factory.setProperty(XMLConstants.FEATURE_SECURE_PROCESSING, false));
            // URL form of the JDK property; JDK 8's XMLSecurityManager.getIndex only matches this form, JDK 11+ accepts both.
            suppressException(() -> factory.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            consumeStreamReader(factory, payload);
        }, "StAX");
    }

    /**
     * Asserts a hardened Templates compile-and-transform throws.
     *
     * <p>{@link TransformerFactory#newTemplates(Source)} via {@link XmlFactories#newTransformerFactory()} followed by transform; either step throwing
     * passes.</p>
     */
    static void assertTemplatesBlocks(final Source xslt) {
        assertParseFails(() -> {
            final Templates templates = XmlFactories.newTransformerFactory().newTemplates(xslt);
            templates.newTransformer().transform(streamSource("<root/>"), new StreamResult(new StringWriter()));
        }, "Templates");
    }

    /**
     * Asserts a permissive Templates compilation succeeds.
     *
     * <p>{@link TransformerFactory#newTransformer(Source)} via {@link TransformerFactory#newInstance()} with FSP off; positive control proving the stylesheet
     * is well-formed.</p>
     */
    static void assertTemplatesCompiles(final String xslt) {
        assertParseSucceeds(() -> {
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            factory.newTransformer(permissiveSaxSource(xslt));
        }, "Templates compile");
    }

    /**
     * Asserts a hardened Templates compile-and-transform either throws or completes without leaked content.
     *
     * <p>{@link TransformerFactory#newTemplates(Source)} via {@link XmlFactories#newTransformerFactory()} followed by transform; Saxon's TrAX path may swallow
     * the entity-expansion error and produce empty output.</p>
     */
    static void assertTemplatesDoesNotLeak(final Source xslt) {
        assertNoLeak(() -> {
            final StringWriter sink = new StringWriter();
            final Templates templates = XmlFactories.newTransformerFactory().newTemplates(xslt);
            templates.newTransformer().transform(streamSource("<root/>"), new StreamResult(sink));
            return sink.toString();
        }, "Templates");
    }

    /**
     * Asserts a hardened identity Transformer of the payload throws.
     *
     * <p>{@link Transformer#transform(Source, javax.xml.transform.Result)} on the identity transformer from {@link XmlFactories#newTransformerFactory()}; only
     * a thrown exception passes.</p>
     */
    static void assertTransformerBlocks(final String payload) {
        assertParseFails(
                () -> XmlFactories.newTransformerFactory().newTransformer().transform(streamSource(payload), new StreamResult(new StringWriter())),
                "Transformer");
    }

    /**
     * Asserts a hardened identity Transformer either throws or completes without leaked content.
     *
     * <p>{@link Transformer#transform(Source, javax.xml.transform.Result)} via {@link XmlFactories#newTransformerFactory()}; Saxon's TrAX path may swallow
     * internal errors and produce empty output.</p>
     */
    static void assertTransformerDoesNotLeak(final String payload) {
        assertNoLeak(() -> {
            final StringWriter sink = new StringWriter();
            XmlFactories.newTransformerFactory().newTransformer().transform(streamSource(payload), new StreamResult(sink));
            return sink.toString();
        }, "Transformer");
    }

    /**
     * Asserts a permissive identity Transformer succeeds.
     *
     * <p>{@link Transformer#transform(Source, javax.xml.transform.Result)} via {@link TransformerFactory#newInstance()} with FSP off; positive control proving
     * the payload is well-formed.</p>
     */
    static void assertTransformerSucceeds(final String payload) {
        assertParseSucceeds(() -> {
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            factory.newTransformer().transform(permissiveSaxSource(payload), new StreamResult(new StringWriter()));
        }, "Transformer");
    }

    /**
     * Asserts a permissive Validator validation succeeds.
     *
     * <p>{@link Validator#validate(Source)} on a validator from {@link #BENIGN_SCHEMA} compiled via {@link SchemaFactory#newInstance(String)} with FSP off;
     * positive control proving the instance is well-formed.</p>
     */
    static void assertValidatorAccepts(final String xml) {
        assertParseSucceeds(() -> {
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            suppressException(() -> factory.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            factory.newSchema(streamSource(BENIGN_SCHEMA)).newValidator().validate(streamSource(xml));
        }, "Validator");
    }

    /**
     * Asserts a hardened Validator validation throws.
     *
     * <p>{@link Validator#validate(Source)} on a validator from {@link #BENIGN_SCHEMA} compiled via {@link XmlFactories#newSchemaFactory()}; only a thrown
     * exception passes (the schema is benign; the attack lives in the instance document).</p>
     */
    static void assertValidatorBlocks(final String xml) {
        assertParseFails(
                () -> XmlFactories.newSchemaFactory().newSchema(streamSource(BENIGN_SCHEMA)).newValidator().validate(streamSource(xml)),
                "Validator");
    }

    /**
     * Asserts a hardened-in-place XMLReader parse of the payload throws.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a raw reader hardened via {@link XmlFactories#harden(XMLReader)}; only a thrown exception passes.</p>
     */
    static void assertXmlReaderBlocks(final String payload) {
        assertParseFails(() -> parseQuietly(rawHardenedReader(), payload), "XMLReader");
    }

    /**
     * Asserts a hardened-in-place XMLReader parse either throws or completes without leaked content.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a raw reader hardened via {@link XmlFactories#harden(XMLReader)}; XML 1.0 §4.1 permits silent skipping of
     * undeclared entities when an external subset is declared but unread.</p>
     */
    static void assertXmlReaderDoesNotLeak(final String payload) {
        assertNoLeak(() -> captureCharacters(rawHardenedReader(), payload), "XMLReader");
    }

    /**
     * Asserts a hardened-in-place XMLReader parse succeeds.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a raw reader hardened via {@link XmlFactories#harden(XMLReader)}; positive control for DOCTYPE-only
     * payloads.</p>
     */
    static void assertXmlReaderParses(final String payload) {
        assertParseSucceeds(() -> parseQuietly(rawHardenedReader(), payload), "XMLReader");
    }

    /** Parses the payload through the supplied reader and returns the accumulated character data, used by the SAX-based {@code DoesNotLeak} helpers. */
    private static String captureCharacters(final XMLReader reader, final String payload) throws Exception {
        final StringBuilder text = new StringBuilder();
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                text.append(ch, start, length);
            }
        });
        reader.setErrorHandler(new DefaultHandler());
        reader.parse(inputSource(payload));
        return text.toString();
    }

    /** Drains every {@link XMLEventReader} event from the factory's reader for the payload. */
    private static void consumeEventReader(final XMLInputFactory factory, final String payload) throws Exception {
        final XMLEventReader events = factory.createXMLEventReader(new StringReader(payload));
        try {
            while (events.hasNext()) {
                events.nextEvent();
            }
        } finally {
            events.close();
        }
    }

    /** Drains every {@link XMLStreamReader} event from the factory's reader for the payload. */
    private static void consumeStreamReader(final XMLInputFactory factory, final String payload) throws Exception {
        final XMLStreamReader stream = factory.createXMLStreamReader(new StringReader(payload));
        try {
            while (stream.hasNext()) {
                stream.next();
            }
        } finally {
            stream.close();
        }
    }

    /** Builds an {@link InputSource} backed by a {@link StringReader} over the payload. */
    static InputSource inputSource(final String xml) {
        return new InputSource(new StringReader(xml));
    }

    /** Parses the payload through the supplied reader, discarding events; used by the SAX-based {@code Parses} / {@code Blocks} helpers. */
    private static void parseQuietly(final XMLReader reader, final String payload) throws Exception {
        reader.setContentHandler(new DefaultHandler());
        reader.setErrorHandler(new DefaultHandler());
        reader.parse(inputSource(payload));
    }

    /** Builds a {@link SAXSource} wrapping the payload, parsed by a deliberately permissive SAX parser; used by the unconfigured-side TrAX controls. */
    private static SAXSource permissiveSaxSource(final String xml) throws ParserConfigurationException, org.xml.sax.SAXException {
        final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        final XMLReader reader = parserFactory.newSAXParser().getXMLReader();
        suppressException(() -> reader.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
        return new SAXSource(reader, new InputSource(new StringReader(xml)));
    }

    /** Builds a raw {@link XMLReader} from a deliberately permissive {@link SAXParserFactory} and hardens it via {@link XmlFactories#harden(XMLReader)}. */
    private static XMLReader rawHardenedReader() throws Exception {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        return XmlFactories.harden(factory.newSAXParser().getXMLReader());
    }

    /** Opens the named test resource as a {@link StreamSource} preserving its system id, so relative includes/imports/redefines resolve normally. */
    static StreamSource resourceSource(final String name) {
        final URL url = resourceUrl(name);
        try {
            return new StreamSource(url.openStream(), url.toString());
        } catch (final IOException e) {
            throw new AssertionError("Failed to open " + name, e);
        }
    }

    /** Resolves a fixture under {@code src/test/resources/leaked/} to a {@link URL}, failing the test if the resource is missing. */
    static URL resourceUrl(final String name) {
        final URL url = AttackTestSupport.class.getResource("/leaked/" + name);
        assertNotNull(url, "test resource not found: " + name);
        return url;
    }

    /** Builds a {@link StreamSource} backed by a {@link StringReader} over the payload. */
    static StreamSource streamSource(final String xml) {
        return new StreamSource(new StringReader(xml));
    }

    /** Runs the action and silently swallows any thrown exception; used to apply best-effort permissive-side flags that may not be supported. */
    private static void suppressException(final ThrowingAction action) {
        try {
            action.run();
        } catch (final Exception e) {
            // Ignore
        }
    }

    /** XML body wrapping the supplied text in a benign {@code <root>/<child>} element pair, validated by {@link #BENIGN_SCHEMA}. */
    static String xmlBody(final String text) {
        return "<root><child>" + text + "</child></root>";
    }

    /** XSD body embedding the supplied text in an annotation, used as the carrier for DOCTYPE-based attacks against schema compilation. */
    static String xsdBody(final String text) {
        return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "  <xs:annotation><xs:documentation>" + text + "</xs:documentation></xs:annotation>\n"
                + "  <xs:element name=\"root\" type=\"xs:string\"/>\n"
                + "</xs:schema>";
    }

    /** XSLT body embedding the supplied text inside a single {@code xsl:template match="/"}, used as the carrier for DOCTYPE-based attacks against TrAX. */
    static String xsltBody(final String text) {
        return "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"
                + "  <xsl:template match=\"/\">" + text + "</xsl:template>\n"
                + "</xsl:stylesheet>";
    }

    private AttackTestSupport() {
    }

}
