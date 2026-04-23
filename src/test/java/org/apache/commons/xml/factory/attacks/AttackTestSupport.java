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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.xml.factory.XmlFactories;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Shared fixtures for attack tests.
 *
 * <p>For each factory type the helpers parse the supplied payload and assert that either the parser throws a hardening-style exception or completes without
 * any attempt to resolve an external resource.</p>
 */
final class AttackTestSupport {

    /** Functional interface so test bodies can throw checked JAXP exceptions. */
    @FunctionalInterface
    interface ThrowingAction {
        void run() throws Exception;
    }

    /**
     * Keywords (lowercase) that indicate the exception comes from the hardening layer or the parser's refusal to handle a DOCTYPE/external reference, rather
     * than from a successful-but-late fetch.
     */
    private static final Set<String> HARDENING_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "doctype", "dtd", "disallow", "external", "entity", "entities", "secure", "access is not allowed",
            "support_dtd", "feature_secure_processing", "prohibited")));

    /**
     * Upper bound on elapsed time for any payload to be processed.
     *
     * <p>A hardened parser either rejects the DOCTYPE up-front (microseconds) or silently skips the DTD (milliseconds). A parser that actually attempts a
     * network fetch would either block on DNS/TCP timeouts or fail with a connection error: both incompatible with this budget.</p>
     */
    private static final long MAX_MILLIS = 2_000L;

    /**
     * Asserts that the supplied action threw an exception quickly and the exception chain indicates the hardening layer (not a network/file attempt).
     */
    private static void assertAttackBlocked(final ThrowingAction action) {
        final long start = System.nanoTime();
        Throwable thrown = null;
        try {
            action.run();
        } catch (final Throwable t) {
            thrown = t;
        }
        final long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        if (thrown == null) {
            fail("Expected hardening to reject the attack, but parsing completed successfully.");
            return;
        }
        if (elapsedMs >= MAX_MILLIS) {
            fail(String.format("Hardening rejected the payload only after %dms: that suggests an actual resolution attempt rather than an up-front block.",
                    elapsedMs), thrown);
        }
        if (!messageOrCauseContainsHardeningKeyword(thrown)) {
            fail("Exception does not look like it came from the hardening layer.", thrown);
        }
    }

    static void assertDomBlocks(final String payload) {
        assertAttackBlocked(() -> XmlFactories.newDocumentBuilderFactory().newDocumentBuilder().parse(inputSource(payload)));
    }

    static void assertSaxBlocks(final String payload) {
        assertAttackBlocked(() -> {
            final XMLReader reader = XmlFactories.newSAXParserFactory().newSAXParser().getXMLReader();
            reader.setContentHandler(new DefaultHandler());
            reader.setErrorHandler(new DefaultHandler());
            reader.parse(inputSource(payload));
        });
    }

    /**
     * Asserts that compiling the given schema via {@code SchemaFactory.newSchema(Source)} is blocked by the hardening layer.
     */
    static void assertSchemaCompilationBlocks(final String xsd) {
        assertAttackBlocked(() -> XmlFactories.newSchemaFactory().newSchema(source(xsd)));
    }

    /**
     * Asserts that a StAX parse of the supplied payload blocks DTD resolution.
     *
     * <p>StAX with {@code SUPPORT_DTD=false} either throws on entity references in content (Billion Laughs, external general entity) or silently skips the
     * DTD. For the benign-body payloads the skipped DTD never becomes an error, so instead of requiring an exception we plant a counting {@code XMLResolver}
     * and assert it is never invoked.</p>
     */
    static void assertStaxBlocks(final String payload) {
        final AtomicInteger resolutions = new AtomicInteger();
        final long start = System.nanoTime();
        Throwable thrown = null;
        try {
            final XMLInputFactory factory = XmlFactories.newXMLInputFactory();
            factory.setXMLResolver((publicID, systemID, baseURI, namespace) -> {
                resolutions.incrementAndGet();
                return new ByteArrayInputStream(new byte[0]);
            });
            final XMLStreamReader stream = factory.createXMLStreamReader(new StringReader(payload));
            try {
                while (stream.hasNext()) {
                    stream.next();
                }
            } finally {
                stream.close();
            }
            final XMLEventReader events = factory.createXMLEventReader(new StringReader(payload));
            try {
                while (events.hasNext()) {
                    events.nextEvent();
                }
            } finally {
                events.close();
            }
        } catch (final Throwable t) {
            thrown = t;
        }
        final long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        if (elapsedMs >= MAX_MILLIS) {
            fail(String.format("StAX parse took %dms: suggests an actual external resolution attempt.", elapsedMs));
        }
        assertEquals(0, resolutions.get(), String.format(
                "StAX parser invoked the external resolver %d time(s); hardening failed to block DTD processing. Exception (if any): %s",
                resolutions.get(),
                thrown));
    }

    /**
     * Asserts that compiling the given stylesheet via {@code TransformerFactory.newTransformer(Source)} is blocked by the hardening layer.
     */
    static void assertStylesheetCompilationBlocks(final String xslt) {
        assertAttackBlocked(() -> XmlFactories.newTransformerFactory().newTransformer(source(xslt)));
    }

    static void assertTransformerBlocks(final String payload) {
        assertAttackBlocked(() -> XmlFactories.newTransformerFactory().newTransformer().transform(source(payload), new StreamResult(new StringWriter())));
    }

    /**
     * Asserts that compiling the given stylesheet and then transforming the given input through it is blocked by the hardening layer. Used for attacks whose
     * external fetch is triggered at {@code Transformer.transform(...)} time rather than compile time (for example {@code document()} in an XSLT template).
     */
    static void assertTransformerBlocksWithStylesheet(final String xslt, final String input) {
        assertAttackBlocked(() -> XmlFactories.newTransformerFactory()
                .newTransformer(source(xslt))
                .transform(source(input), new StreamResult(new StringWriter())));
    }

    /**
     * Asserts that validating the given XML instance through a {@link javax.xml.validation.Validator} obtained from {@link Payloads#BENIGN_SCHEMA} is blocked
     * by the hardening layer. The schema itself is trusted and benign; the attack lives in the instance document.
     */
    static void assertValidatorBlocks(final String xml) {
        assertAttackBlocked(() -> XmlFactories.newSchemaFactory().newSchema(source(Payloads.BENIGN_SCHEMA)).newValidator().validate(source(xml)));
    }

    private static InputSource inputSource(final String xml) {
        return new InputSource(new StringReader(xml));
    }

    private static boolean messageOrCauseContainsHardeningKeyword(final Throwable t) {
        Throwable current = t;
        while (current != null) {
            final String msg = current.getMessage();
            if (msg != null) {
                final String lower = msg.toLowerCase(Locale.ROOT);
                if (HARDENING_KEYWORDS.stream().anyMatch(lower::contains)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static StreamSource source(final String xml) {
        return new StreamSource(new StringReader(xml));
    }

    private AttackTestSupport() {
    }

}
