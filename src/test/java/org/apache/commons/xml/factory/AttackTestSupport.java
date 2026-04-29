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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
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
 *       output) or where the XML spec lets the parser silently skip undeclared entities once the external DTD subset has been refused (DOM with an external
 *       DTD subset).</li>
 * </ul>
 *
 * <p>DOM tests that depend on user-defined entity machinery should gate themselves with {@link org.junit.jupiter.api.Assumptions#assumeTrue} on
 * {@link #DOM_RESOLVES_INTERNAL_ENTITIES} so they skip on platforms (such as Android with KXmlParser) whose DOM parser does not surface the entity events that
 * the strict {@link #assertDomBlocks} assertion expects.</p>
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
 * <p>The two generic primitives {@link #assertParseFails} and {@link #assertParseSucceeds} are exposed for tests that need to compose a non-standard factory
 * call.</p>
 */
final class AttackTestSupport {

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
     * Benign payload used to probe whether the DOM parser inlines a user-defined internal general entity into the tree.
     */
    private static final String DOM_INTERNAL_ENTITY_PROBE =
            "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE root [<!ENTITY foo \"bar\">]>\n"
            + "<root>&foo;</root>";

    /**
     * Set to {@code true} when the platform's DOM parser supports user-defined internal entities.
     *
     * <p>Android's {@code KXmlParser} currently fails this test.</p>
     */
    static final boolean DOM_RESOLVES_INTERNAL_ENTITIES = probeDomResolvesInternalEntities();

    /** {@code true} when running on Android (Dalvik / ART), {@code false} on any standard JVM. Probed once via {@code Class.forName} on {@code android.os.Build}. */
    static final boolean IS_ANDROID = probeAndroid();

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
        assertParseFails(() -> XmlFactories.newDocumentBuilderFactory().newDocumentBuilder().parse(inputSource(payload)), "DOM", SAXException.class);
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
            if (doc.getDocumentElement() == null) {
                return "";
            }
            // Harmony's DOM returns null from getTextContent() on an element whose only children are unresolved EntityReference nodes.
            final String text = doc.getDocumentElement().getTextContent();
            return text == null ? "" : text;
        }, "DOM", SAXException.class);
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
            if (!IS_ANDROID) {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            }
            suppressException(() -> factory.setAttribute(JDK_ENTITY_EXPANSION_LIMIT, "0"));
            factory.newDocumentBuilder().parse(inputSource(payload));
        }, "DOM");
    }

    /**
     * Skeleton for every {@code assert*DoesNotLeak} helper.
     *
     * <p>Treats a thrown exception of one of the {@code expected} types as "hardening blocked at parse" (acceptable); otherwise asserts the captured output
     * omits {@link #LEAKED_MARKER}. A throw whose type does not match {@code expected} fails the test, so unrelated failures (e.g. a {@link HardeningException}
     * because no recipe matched the JAXP implementation) cannot be silently accepted as a clean block.</p>
     *
     * @param action      the parse to execute, returning the captured output text checked for {@link #LEAKED_MARKER}.
     * @param description short label naming the JAXP surface under test.
     * @param expected    the exception types any of which the hardening layer may surface as a clean rejection.
     */
    @SafeVarargs
    private static void assertNoLeak(final ThrowingSupplier<String> action, final String description, final Class<? extends Throwable>... expected) {
        final String output;
        try {
            output = action.get();
        } catch (final Throwable thrown) {
            if (Arrays.stream(expected).anyMatch(c -> c.isInstance(thrown))) {
                return; // hardening blocked at parse; acceptable outcome.
            }
            throw new AssertionError(blockedDescription(description) + " (got " + thrown.getClass().getName() + ")", thrown);
        }
        assertFalse(output.contains(LEAKED_MARKER),
                "Hardening did not block " + description + "; output contained marker '" + LEAKED_MARKER + "'.\nFull output:\n" + output);
    }

    /**
     * Asserts the supplied parsing action throws an exception of the {@code expected} type.
     *
     * @param action      the parse to execute.
     * @param description short label naming the JAXP surface under test.
     * @param expected    the exception type the hardening layer is expected to surface.
     */
    static void assertParseFails(final Executable action, final String description, final Class<? extends Throwable> expected) {
        assertThrows(expected, action, blockedDescription(description));
    }

    /**
     * Asserts the supplied parsing action throws an exception that matches one of the {@code expected} types.
     *
     * @param action      the parse to execute.
     * @param description short label naming the JAXP surface under test.
     * @param expected    the exception types any of which the hardening layer may surface.
     */
    @SafeVarargs
    static void assertParseFails(final Executable action, final String description, final Class<? extends Throwable>... expected) {
        final Throwable thrown = assertThrows(Exception.class, action, blockedDescription(description));
        if (Arrays.stream(expected).noneMatch(c -> c.isInstance(thrown))) {
            throw new AssertionError(blockedDescription(description) + " (got " + thrown.getClass().getName() + ")", thrown);
        }
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
    static void assertParseSucceeds(final Executable action, final String description) {
        assertDoesNotThrow(action, "Unconfigured factory should parse " + description + "; the wrapper or its external reference is broken.");
    }

    /**
     * Asserts a hardened SAX parse of the payload throws.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a parser from {@link XmlFactories#newSAXParserFactory()}; only a thrown exception passes.</p>
     */
    static void assertSaxBlocks(final String payload) {
        assertParseFails(() -> parseQuietly(XmlFactories.newSAXParserFactory().newSAXParser().getXMLReader(), payload), "SAX", SAXException.class);
    }

    /**
     * Asserts a hardened SAX parse either throws or completes without leaked content.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a parser from {@link XmlFactories#newSAXParserFactory()}; XML 1.0 §4.1 permits silent skipping of undeclared
     * entities when an external subset is declared but unread.</p>
     */
    static void assertSaxDoesNotLeak(final String payload) {
        assertNoLeak(() -> captureCharacters(XmlFactories.newSAXParserFactory().newSAXParser().getXMLReader(), payload), "SAX", SAXException.class);
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
            if (!IS_ANDROID) {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            }
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
        assertParseFails(() -> XmlFactories.newSchemaFactory().newSchema(xsd), "Schema compile", SAXException.class, SecurityException.class);
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
            if (!IS_ANDROID) {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            }
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
        assertParseFails(() -> consumeStreamReader(XmlFactories.newXMLInputFactory(), payload), "StAX stream", XMLStreamException.class);
        assertParseFails(() -> consumeEventReader(XmlFactories.newXMLInputFactory(), payload), "StAX event", XMLStreamException.class);
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
            // Xalan return `null` if the template fails
            if (templates == null) {
                throw new TransformerException("Transformer factory returned null");
            }
            templates.newTransformer().transform(streamSource("<root/>"), new StreamResult(new StringWriter()));
        }, "Templates", TransformerException.class);
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
            // Xalan return `null` if the template fails
            if (templates != null) {
                templates.newTransformer().transform(streamSource("<root/>"), new StreamResult(sink));
            }
            return sink.toString();
        }, "Templates", TransformerException.class);
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
                "Transformer", TransformerException.class);
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
        }, "Transformer", TransformerException.class);
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
                "Validator", SAXException.class, SecurityException.class);
    }

    /**
     * Asserts a hardened-in-place XMLReader parse of the payload throws.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a raw reader hardened via {@link XmlFactories#harden(XMLReader)}; only a thrown exception passes.</p>
     */
    static void assertXmlReaderBlocks(final String payload) {
        assertParseFails(() -> parseQuietly(rawHardenedReader(), payload), "XMLReader", SAXException.class);
    }

    /**
     * Asserts a hardened-in-place XMLReader parse either throws or completes without leaked content.
     *
     * <p>{@link XMLReader#parse(InputSource)} on a raw reader hardened via {@link XmlFactories#harden(XMLReader)}; XML 1.0 §4.1 permits silent skipping of
     * undeclared entities when an external subset is declared but unread.</p>
     */
    static void assertXmlReaderDoesNotLeak(final String payload) {
        assertNoLeak(() -> captureCharacters(rawHardenedReader(), payload), "XMLReader", SAXException.class);
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

    /**
     * Builds the failure message used by every {@code assert*Blocks(...)} helper.
     *
     * @param description short label naming the JAXP surface under test.
     * @return the assertion-failure message string.
     */
    private static String blockedDescription(final String description) {
        return "Hardening did not block " + description + "; parse completed successfully.";
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
        if (!IS_ANDROID) {
            parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        }
        final XMLReader reader = parserFactory.newSAXParser().getXMLReader();
        suppressException(() -> reader.setProperty(JDK_ENTITY_EXPANSION_LIMIT, "0"));
        return new SAXSource(reader, new InputSource(new StringReader(xml)));
    }

    private static boolean probeAndroid() {
        try {
            Class.forName("android.os.Build");
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean probeDomResolvesInternalEntities() {
        try {
            final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputSource(DOM_INTERNAL_ENTITY_PROBE));
            final Element root = doc.getDocumentElement();
            return root != null && "bar".equals(root.getTextContent());
        } catch (final Exception e) {
            return false;
        }
    }

    /** Builds a raw {@link XMLReader} from a deliberately permissive {@link SAXParserFactory} and hardens it via {@link XmlFactories#harden(XMLReader)}. */
    private static XMLReader rawHardenedReader() throws Exception {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        if (!IS_ANDROID) {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        }
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
    private static void suppressException(final Executable action) {
        try {
            action.execute();
        } catch (final Throwable e) {
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
