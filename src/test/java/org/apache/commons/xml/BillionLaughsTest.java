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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks whether parsers reject a Billion Laughs payload (nested entity expansion in the internal DTD subset).
 *
 * <p>The library does not pin a custom entity-expansion limit; each parser keeps its own secure-processing default, which varies by implementation: {@code 2500}
 * (stock JDK 25), {@code 64000} (older JDK), {@code 100000} (external Xerces and Woodstox). The hardened payload therefore has to exceed the largest of these,
 * while the permissive positive controls must stay small enough to run. Three fixtures cover that spread:</p>
 *
 * <ul>
 *   <li>The <strong>medium</strong> fixture nests three levels of 16x expansion ({@code lol1} through {@code lol3}), so resolving {@code &lol3;} produces
 *       {@code 16 * 16 * 16 = 4096} leaf {@code &lol;} expansions and {@code 1 + 16 + 256 + 4096 = 4369} entity-expansion events (~4 KB of {@code "A"}). Used by
 *       the {@code unconfigured*} positive controls: it parses/compiles quickly on every processor and stays under external Xerces' and Woodstox's default limit,
 *       which those parsers enforce even when the test tries to disable it (they ignore the JDK {@code entityExpansionLimit} knob).</li>
 *   <li>The <strong>huge</strong> fixture nests six levels of 10x expansion ({@code lol1} through {@code lol6}), so resolving {@code &lol6;} produces
 *       {@code 10^6} leaf expansions. Used by the {@code hardened*} tests on the JDK: it exceeds every implementation's own secure default (2500 / 64000 / 100000),
 *       so the hardened parser aborts on the entity-expansion limit during the parse, long before realizing that many characters.</li>
 *   <li>The <strong>large</strong> fixture nests seven levels of 10x expansion ({@code lol1} through {@code lol7}), so resolving {@code &lol7;} produces
 *       {@code 10^7 = 10 000 000} leaf expansions, ~10 MB of {@code "A"} text, and an amplification factor in the tens of thousands. Used by the {@code hardened*}
 *       tests on Android, sized to trip libexpat &gt;= 2.4's built-in billion-laughs check (8 MiB activation threshold and 100x amplification factor).</li>
 * </ul>
 *
 * <p>Why a single character {@code "A"}: XSLTC compiles a stylesheet's expanded text into JVM bytecode. A fully expanded {@code Templates} compile chokes on the
 * huge fixture (tens of seconds of literal-text chunking) and overflows XSLTC's 65535-byte constant/method limits with a "GregorSamsa" {@code ClassFormatError}
 * on the large fixture, a failure unrelated to entity hardening. The hardened {@code Templates} test still uses the huge fixture because its limit trips during
 * the <em>parse</em>, before XSLTC ever compiles; the permissive {@code Templates} control keeps the medium fixture (see {@code unconfiguredTemplatesCompiles}).</p>
 *
 * <p>Which fixture each test uses:</p>
 *
 * <ul>
 *   <li>{@code hardened*} tests pull from {@code hardened*Payload} helpers: large fixture on Android (libexpat is the only defense), huge fixture on the JDK
 *       (it must exceed the parser's own secure-processing limit, external Xerces' {@code 100000} included).</li>
 *   <li>{@code unconfigured*} positive controls pull from {@code medium*Payload} helpers regardless of platform: libexpat's billion-laughs check cannot be
 *       disabled from Java, so a permissive parse of the large fixture would still trip on Android, and Xerces/Woodstox enforce their own default limit.</li>
 * </ul>
 */
class BillionLaughsTest {

    private static final String LARGE_CONTENT = "&lol7;";

    private static final String LARGE_DTD =
            "  <!ENTITY lol \"A\">\n"
            + "  <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
            + "  <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n"
            + "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n"
            + "  <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n"
            + "  <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n"
            + "  <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n"
            + "  <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\n";

    private static final String HUGE_CONTENT = "&lol6;";

    private static final String HUGE_DTD =
            "  <!ENTITY lol \"A\">\n"
            + "  <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
            + "  <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n"
            + "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n"
            + "  <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n"
            + "  <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n"
            + "  <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n";

    private static final String MEDIUM_CONTENT = "&lol3;";

    private static final String MEDIUM_DTD =
            "  <!ENTITY lol \"A\">\n"
            + "  <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
            + "  <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n"
            + "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n";

    /**
     * Hardened-side payload for DOM/SAX/XmlReader.
     *
     * <ul>
     *     <li>~10 MiB on Android to trip libexpat's 8 MiB limit, and</li>
     *     <li>~1 MB (10^6 expansions) on the JDK, so it exceeds every implementation's own secure-processing default: 2500 (stock JDK 25), 64000 (older JDK) and
     *         100000 (external Xerces, Woodstox). The parser aborts at its limit well before realizing that many characters.</li>
     * </ul>
     */
    private static String hardenedXmlPayload() {
        return AttackTestSupport.IS_ANDROID
                ? withDoctype("root", LARGE_DTD, AttackTestSupport.xmlBody(LARGE_CONTENT))
                : withDoctype("root", HUGE_DTD, AttackTestSupport.xmlBody(HUGE_CONTENT));
    }

    /**
     * Hardened-side XSD payload; sized like {@link #hardenedXmlPayload()} (10^6 expansions on the JDK, ~10 MiB on Android) so it exceeds every implementation's
     * secure-processing default, external Xerces' {@code 100000} included.
     */
    private static String hardenedXsdPayload() {
        return AttackTestSupport.IS_ANDROID
                ? withDoctype("xs:schema", LARGE_DTD, AttackTestSupport.xsdBody(LARGE_CONTENT))
                : withDoctype("xs:schema", HUGE_DTD, AttackTestSupport.xsdBody(HUGE_CONTENT));
    }

    /**
     * Hardened-side XSLT payload; sized like {@link #hardenedXmlPayload()} so it exceeds every implementation's secure-processing default. The hardened parser
     * aborts on the entity-expansion limit during the <em>parse</em> of the stylesheet, before XSLTC would reach its 65535-byte constant-pool cap, so this stays
     * a genuine entity-limit test on every processor.
     */
    private static String hardenedXsltPayload() {
        return AttackTestSupport.IS_ANDROID
                ? withDoctype("xsl:stylesheet", LARGE_DTD, AttackTestSupport.xsltBody(LARGE_CONTENT))
                : withDoctype("xsl:stylesheet", HUGE_DTD, AttackTestSupport.xsltBody(HUGE_CONTENT));
    }

    /**
     * Unconfigured-side payload for DOM/SAX/XmlReader
     *
     * <p>~4 KB of expanded {@code "A"}, which:</p>
     * <ul>
     *     <li>trips JDK 25's {@code entityExpansionLimit = 2500}, but</li>
     *     <li>does not trip libexpat's immutable 8 MiB limit.</li>
     * </ul>
     */
    private static String mediumXmlPayload() {
        return withDoctype("root", MEDIUM_DTD, AttackTestSupport.xmlBody(MEDIUM_CONTENT));
    }

    /**
     * Unconfigured-side XSD payload
     *
     * <p>~4 KB of expanded {@code "A"}, which:</p>
     * <ul>
     *     <li>trips JDK 25's {@code entityExpansionLimit = 2500}, but</li>
     *     <li>does not trip libexpat's immutable 8 MiB limit.</li>
     * </ul>
     */
    private static String mediumXsdPayload() {
        return withDoctype("xs:schema", MEDIUM_DTD, AttackTestSupport.xsdBody(MEDIUM_CONTENT));
    }

    /**
     * Unconfigured-side XSLT payload
     *
     * <p>~4 KB of expanded {@code "A"}, which:</p>
     * <ul>
     *     <li>trips JDK 25's {@code entityExpansionLimit = 2500}, but</li>
     *     <li>stays under XSLTC's 60 KB constant-pool cap, and</li>
     *     <li>does not trip libexpat's immutable 8 MiB limit.</li>
     * </ul>
     */
    private static String mediumXsltPayload() {
        return withDoctype("xsl:stylesheet", MEDIUM_DTD, AttackTestSupport.xsltBody(MEDIUM_CONTENT));
    }

    private static String withDoctype(final String rootQName, final String dtd, final String body) {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE " + rootQName + " [\n"
                + dtd
                + "]>\n"
                + body + "\n";
    }

    @Test
    @Tag("dom")
    void hardenedDomBlocks() {
        Assumptions.assumeTrue(AttackTestSupport.DOM_RESOLVES_INTERNAL_ENTITIES,
                "Skipped: platform DOM does not resolve user-defined entities");
        AttackTestSupport.assertDomBlocks(hardenedXmlPayload());
    }

    @Test
    @Tag("sax")
    void hardenedSaxBlocks() {
        AttackTestSupport.assertSaxBlocks(hardenedXmlPayload());
    }

    @Test
    @Tag("schema")
    void hardenedSchemaBlocks() {
        AttackTestSupport.assertSchemaBlocks(AttackTestSupport.streamSource(hardenedXsdPayload()));
    }

    @Test
    @Tag("stax")
    void hardenedStaxBlocks() {
        AttackTestSupport.assertStaxBlocks(hardenedXmlPayload());
    }

    @Test
    @Tag("trax")
    void hardenedTemplatesBlocks() {
        AttackTestSupport.assertTemplatesBlocks(AttackTestSupport.streamSource(hardenedXsltPayload()));
    }

    @Test
    @Tag("trax")
    void hardenedTransformerBlocks() {
        AttackTestSupport.assertTransformerBlocks(hardenedXmlPayload());
    }

    @Test
    @Tag("schema")
    void hardenedValidatorBlocks() {
        AttackTestSupport.assertValidatorBlocks(hardenedXmlPayload());
    }

    @Test
    @Tag("sax")
    void hardenedXmlReaderBlocks() {
        AttackTestSupport.assertXmlReaderBlocks(hardenedXmlPayload());
    }

    @Test
    @Tag("dom")
    void unconfiguredDomParses() {
        Assumptions.assumeTrue(AttackTestSupport.DOM_RESOLVES_INTERNAL_ENTITIES,
                "Skipped: platform DOM does not resolve user-defined entities");
        AttackTestSupport.assertPermissiveDomParses(mediumXmlPayload());
    }

    @Test
    @Tag("sax")
    void unconfiguredSaxParses() {
        AttackTestSupport.assertPermissiveSaxParses(mediumXmlPayload());
    }

    @Test
    @Tag("schema")
    void unconfiguredSchemaCompiles() {
        AttackTestSupport.assertPermissiveSchemaCompiles(AttackTestSupport.streamSource(mediumXsdPayload()));
    }

    @Test
    @Tag("stax")
    void unconfiguredStaxParses() {
        AttackTestSupport.assertPermissiveStaxParses(mediumXmlPayload());
    }

    @Test
    @Tag("trax")
    void unconfiguredTemplatesCompiles() {
        // Unlike the other permissive controls, this one keeps the small MEDIUM fixture rather than mirroring the hardened HUGE payload. The hardened side aborts
        // during the parse (its entity-expansion limit trips long before the document is fully expanded), but this permissive side disables that limit, so XSLTC
        // receives the fully expanded stylesheet and compiles it into bytecode. At the HUGE (10^6) size that compile is pathologically slow (tens of seconds of
        // literal-text chunking) and at the LARGE (10^7) size it overflows XSLTC's 65535-byte constant/method limits with a "GregorSamsa" ClassFormatError, a
        // failure unrelated to entity hardening. The MEDIUM fixture stays well under that ceiling and compiles quickly on every processor.
        AttackTestSupport.assertPermissiveTemplatesCompiles(mediumXsltPayload());
    }

    @Test
    @Tag("trax")
    void unconfiguredTransformerTransforms() {
        AttackTestSupport.assertPermissiveTransformerTransforms(mediumXmlPayload());
    }

    @Test
    @Tag("schema")
    void unconfiguredValidatorValidates() {
        AttackTestSupport.assertPermissiveValidatorValidates(mediumXmlPayload());
    }
}
