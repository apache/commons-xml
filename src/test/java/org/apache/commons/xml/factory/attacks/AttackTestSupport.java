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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.xml.factory.XmlFactories;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Shared fixtures for attack tests.
 *
 * <p>For every JAXP factory type the class exposes a paired set of helpers:</p>
 *
 * <ul>
 *   <li>{@code assert*Blocks(payload)} runs the payload through a hardened factory from {@link XmlFactories} and asserts the parse throws. Used by the
 *       {@code hardened*} side of test pairs to check that the hardening layer rejected the attack.</li>
 *   <li>{@code assert*Resolves(payload)} / {@code assert*Compiles(payload)} / similar run the payload through a default factory from
 *       {@link DocumentBuilderFactory#newInstance()} and friends, asserting the parse succeeds. Used by the {@code unconfigured*} side of test pairs as a
 *       positive control: it proves the payload is well-formed and the external resolution would happen without hardening.</li>
 * </ul>
 *
 * <p>The two generic primitives {@link #assertParseFails(ThrowingAction, String)} and {@link #assertParseSucceeds(ThrowingAction, String)} are exposed for
 * tests that need to compose a non-standard factory call.</p>
 */
final class AttackTestSupport {

    /**
     * Functional interface so test bodies can throw checked JAXP exceptions.
     */
    @FunctionalInterface
    interface ThrowingAction {
        void run() throws Exception;
    }

    /**
     * URL form of the JDK's entity-expansion limit property.
     */
    private static final String JDK_ENTITY_EXPANSION_LIMIT = "http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit";

    static void assertDomBlocks(final String payload) {
        assertParseFails(() -> XmlFactories.newDocumentBuilderFactory().newDocumentBuilder().parse(inputSource(payload)), "DOM");
    }

    static void assertDomResolves(final String payload) {
        assertParseSucceeds(() -> {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            suppressException(() -> factory.setAttribute(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            factory.newDocumentBuilder().parse(inputSource(payload));
        }, "DOM");
    }

    /**
     * Generic positive assertion for the file-based attack tests: runs the supplied parse action and asserts it throws. Used by the {@code hardened*} side of
     * test pairs, where the wrapper deliberately requires the external resolution to succeed, so a throw means hardening prevented the resolution.
     *
     * @param action      the parse to execute.
     * @param description short label included in the failure message, for example {@code "DOM"} or {@code "validator"}.
     */
    static void assertParseFails(final ThrowingAction action, final String description) {
        assertThrows(Exception.class, action::run, "Hardening did not block " + description + "; parse completed successfully.");
    }

    /**
     * Generic positive control for the file-based attack tests: runs the supplied parse action and asserts it does not throw. Used by the
     * {@code unconfigured*} side of test pairs to prove the wrapper is well-formed and the external resolution would succeed without hardening.
     *
     * @param action      the parse to execute.
     * @param description short label included in the failure message, for example {@code "DOM"} or {@code "validator"}.
     */
    static void assertParseSucceeds(final ThrowingAction action, final String description) {
        assertDoesNotThrow(action::run, "Unconfigured factory should parse " + description + "; the wrapper or its external reference is broken.");
    }

    static void assertSaxBlocks(final String payload) {
        assertParseFails(() -> {
            final XMLReader reader = XmlFactories.newSAXParserFactory().newSAXParser().getXMLReader();
            reader.setContentHandler(new DefaultHandler());
            reader.setErrorHandler(new DefaultHandler());
            reader.parse(inputSource(payload));
        }, "SAX");
    }

    static void assertSaxResolves(final String payload) {
        assertParseSucceeds(() -> {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            final XMLReader reader = factory.newSAXParser().getXMLReader();
            suppressException(() -> reader.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            reader.setContentHandler(new DefaultHandler());
            reader.setErrorHandler(new DefaultHandler());
            reader.parse(inputSource(payload));
        }, "SAX");
    }

    /**
     * Asserts that compiling the given schema via {@code SchemaFactory.newSchema(Source)} from {@link XmlFactories} throws.
     */
    static void assertSchemaCompilationBlocks(final String xsd) {
        assertParseFails(() -> XmlFactories.newSchemaFactory().newSchema(streamSource(xsd)), "Schema compile");
    }

    /**
     * Asserts that compiling the given schema via a default {@link SchemaFactory#newInstance(String)} succeeds. Positive control for
     * {@link #assertSchemaCompilationBlocks(String)}.
     */
    static void assertSchemaCompilationSucceeds(final String xsd) {
        assertParseSucceeds(() -> {
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            suppressException(() -> factory.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            factory.newSchema(streamSource(xsd));
        }, "Schema compile");
    }

    /**
     * Asserts that a StAX parse of the supplied payload through a hardened factory from {@link XmlFactories} blocks DTD resolution.
     *
     * <p>StAX with {@code SUPPORT_DTD=false} either throws on entity references in content or silently skips the DTD. For the benign-body payloads the
     * skipped DTD never becomes an error, so instead of requiring an exception we plant a counting {@code XMLResolver} and assert it is never invoked.</p>
     */
    static void assertStaxBlocks(final String payload) {
        final AtomicInteger resolutions = new AtomicInteger();
        Throwable thrown = null;
        try {
            final XMLInputFactory factory = XmlFactories.newXMLInputFactory();
            factory.setXMLResolver((publicID, systemID, baseURI, namespace) -> {
                resolutions.incrementAndGet();
                return new ByteArrayInputStream(new byte[0]);
            });
            consumeStreamReader(factory, payload);
            consumeEventReader(factory, payload);
        } catch (final Throwable t) {
            thrown = t;
        }
        assertEquals(0, resolutions.get(), String.format("StAX parser invoked the external resolver %d time(s); hardening failed to block DTD processing. " +
                "Exception (if any): %s", resolutions.get(), thrown));
    }

    /**
     * Positive control for {@link #assertStaxBlocks(String)}: parses the payload through the default {@link XMLInputFactory#newInstance()} and asserts the
     * parse does not throw. The default factory has {@code SUPPORT_DTD=true}, so the external DTD reference gets fetched and any entity references resolve.
     */
    static void assertStaxResolves(final String payload) {
        assertParseSucceeds(() -> {
            final XMLInputFactory factory = XMLInputFactory.newInstance();
            suppressException(() -> factory.setProperty(XMLConstants.FEATURE_SECURE_PROCESSING, false));
            // URL form of the JDK property; JDK 8's XMLSecurityManager.getIndex only matches this form, JDK 11+ accepts both.
            suppressException(() -> factory.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            consumeStreamReader(factory, payload);
            //consumeEventReader(factory, payload);
        }, "StAX");
    }

    /**
     * Asserts that compiling the given stylesheet via {@code TransformerFactory.newTransformer(Source)} from {@link XmlFactories} throws.
     */
    static void assertStylesheetCompilationBlocks(final String xslt) {
        assertParseFails(() -> XmlFactories.newTransformerFactory().newTransformer(streamSource(xslt)), "stylesheet compile");
    }

    /**
     * Asserts that compiling the given stylesheet via a default {@link TransformerFactory#newInstance()} succeeds. Positive control for
     * {@link #assertStylesheetCompilationBlocks(String)}.
     */
    static void assertStylesheetCompilationSucceeds(final String xslt) {
        assertParseSucceeds(() -> {
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            factory.newTransformer(permissiveSaxSource(xslt));
        }, "stylesheet compile");
    }

    static void assertTransformerBlocks(final String payload) {
        assertParseFails(() -> XmlFactories.newTransformerFactory().newTransformer().transform(streamSource(payload), new StreamResult(new StringWriter())),
                "transformer");
    }

    static void assertTransformerSucceeds(final String payload) {
        assertParseSucceeds(() -> {
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            factory.newTransformer().transform(permissiveSaxSource(payload), new StreamResult(new StringWriter()));
        }, "transformer");
    }

    /**
     * Positive control for {@link #assertValidatorBlocks(String)}: validates the same payload through a Validator obtained from a default
     * {@link SchemaFactory#newInstance(String)}, asserting it accepts the input.
     */
    static void assertValidatorAccepts(final String xml) {
        assertParseSucceeds(() -> {
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            suppressException(() -> factory.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            factory.newSchema(streamSource(Payloads.BENIGN_SCHEMA)).newValidator().validate(streamSource(xml));
        }, "validator");
    }

    /**
     * Asserts that validating the given XML instance through a {@link javax.xml.validation.Validator} obtained from {@link Payloads#BENIGN_SCHEMA} via the
     * hardened factory throws. The schema itself is trusted and benign; the attack lives in the instance document.
     */
    static void assertValidatorBlocks(final String xml) {
        assertParseFails(() -> XmlFactories.newSchemaFactory().newSchema(streamSource(Payloads.BENIGN_SCHEMA)).newValidator().validate(streamSource(xml)),
                "validator");
    }

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

    static InputSource inputSource(final String xml) {
        return new InputSource(new StringReader(xml));
    }

    /**
     * Builds a {@link javax.xml.transform.sax.SAXSource} backed by a permissively configured {@link XMLReader}. Used by the unconfigured
     * {@link #assertStylesheetCompilationSucceeds(String)} and {@link #assertTransformerSucceeds(String)} helpers because some {@link TransformerFactory}
     * implementations (notably Saxon) do not propagate FSP-off or entity-limit overrides set on the factory through to the underlying SAX parser they create.
     * Wrapping the source in a SAXSource forces the supplied reader to be used and bypasses that gap.
     *
     * <p>The reader's entity-expansion and size limits are set to {@code 0} (no limit) via the JDK's {@code jdk.xml.*} property names, which are tolerated if
     * unrecognised by the underlying parser. Implementations that don't recognise them fall back to their own defaults; for the {@link BillionLaughsTest}
     * payload, only the JDK's 64000 default actually fires below the 65536 expansion events the test produces, and removing it via these properties is what
     * lets the unconfigured side complete.</p>
     */
    private static javax.xml.transform.sax.SAXSource permissiveSaxSource(final String xml)
            throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException {
        final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        final XMLReader reader = parserFactory.newSAXParser().getXMLReader();
        suppressException(() -> reader.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
        return new javax.xml.transform.sax.SAXSource(reader, new InputSource(new StringReader(xml)));
    }

    /**
     * Returns the URL of a fixture under {@code src/test/resources/leaked/} on the test classpath. Fails the test if the resource is not present.
     *
     * @param name the file name within the {@code leaked} directory, for example {@code "referenced.xml"}.
     * @return a URL that can be used as a system id, opened for reading, or string-concatenated into an XPath URI.
     */
    static URL resourceUrl(final String name) {
        final URL url = AttackTestSupport.class.getResource("/leaked/" + name);
        assertNotNull(url, "test resource not found: " + name);
        return url;
    }

    static StreamSource streamSource(final String xml) {
        return new StreamSource(new StringReader(xml));
    }

    private static void suppressException(ThrowingAction action) {
        try {
            action.run();
        } catch (final Exception e) {
            // Ignore
        }
    }

    private AttackTestSupport() {
    }

}
