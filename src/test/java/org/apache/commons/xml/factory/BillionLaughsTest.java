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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks whether parsers reject a Billion Laughs payload (nested entity expansion in the internal DTD subset).
 *
 * <p>Two fixtures are used:</p>
 *
 * <ul>
 *   <li>The <strong>medium</strong> fixture nests three levels of 16x expansion ({@code lol1} through {@code lol3}), so resolving {@code &lol3;} produces
 *       {@code 16 * 16 * 16 = 4096} leaf {@code &lol;} expansions and a total of {@code 1 + 16 + 256 + 4096 = 4369} entity-expansion events. That is
 *       comfortably over the {@code entityExpansionLimit = 2500} this library pins on every JAXP factory it returns (mirroring JDK 25's secure value), so the
 *       hardened side trips on every JDK from 8 to 25. The unconfigured side disables the limit explicitly via {@code setAttribute/setProperty} on the JDK
 *       property name, so it parses the ~4 KB of expanded {@code "A"} text and finishes immediately.</li>
 *   <li>The <strong>large</strong> fixture nests seven levels of 10x expansion ({@code lol1} through {@code lol7}), so resolving {@code &lol7;} produces
 *       {@code 10^7 = 10 000 000} leaf expansions, ~10 MB of {@code "A"} text, and an amplification factor in the tens of thousands. Sized to trip
 *       libexpat &gt;= 2.4's built-in billion-laughs check (8 MiB activation threshold and 100x amplification factor); used by the hardened DOM/SAX/XmlReader
 *       tests on Android, where the JDK count limit is unavailable and only libexpat's native check stands between the parser and the expansion.</li>
 * </ul>
 *
 * <p>Why a single character {@code "A"}: XSLTC compiles a stylesheet's expanded text into a JVM string constant, which is capped at 65535 bytes. A larger
 * expansion makes {@code newTransformer(stylesheet)} fail with a misleading "GregorSamsa" stub-class error even when entity limits are disabled. The medium
 * fixture is sized to stay well under that ceiling. The large fixture cannot be used for {@code Templates} / {@code Transformer} compilation for the same
 * reason.</p>
 *
 * <p>Which fixture each test uses:</p>
 *
 * <ul>
 *   <li>{@code hardenedSaxBlocks}, {@code hardenedXmlReaderBlocks}: large fixture on Android (libexpat is the only defence), medium fixture on JDK
 *       (entity-expansion count limit is sufficient). Selected by {@link AttackTestSupport#IS_ANDROID}.</li>
 *   <li>{@code hardenedDomBlocks} and {@code unconfiguredDomResolves}: gated on {@link AttackTestSupport#DOM_RESOLVES_INTERNAL_ENTITIES}. They run with the
 *       medium fixture on platforms whose DOM parser resolves user-defined entities (every JDK), and skip on platforms where it does not (Android with
 *       KXmlParser, where custom internal entities become unresolved {@code EntityReference} nodes and amplification cannot grow). When the probe lights up
 *       on a future Android the assertions will start running again without further changes.</li>
 *   <li>Every other test: medium fixture. {@code unconfigured*} positive controls cannot use the large fixture because libexpat's protection cannot be
 *       disabled from Java. {@code Templates} and {@code Transformer} tests cannot use it because of the XSLTC 60 KB cap. {@code Schema}, {@code StAX} and
 *       {@code Validator} hardened tests do not run on Android (no harmony {@code SchemaFactory} or {@code XMLInputFactory}), so the JDK count limit is the
 *       only relevant defence for them and the medium fixture is enough.</li>
 * </ul>
 *
 * <p>Each parser type is exercised twice as a pair (unconfigured factory, expected to parse; hardened factory, expected to throw):</p>
 *
 * <ul>
 *   <li>The {@code hardened*} side runs the payload through {@link org.apache.commons.xml.factory.XmlFactories} and asserts the parse throws. Hardening blocks
 *       at whichever layer fires first (DOCTYPE-disallow on DOM/SAX, FSP plus propagated entity-expansion limits elsewhere).</li>
 *   <li>The {@code unconfigured*} side runs the payload through a default factory with {@code FEATURE_SECURE_PROCESSING=false} and asserts the parse succeeds.
 *       Disabling FSP turns off the JDK's entity-expansion limit, which is the only safety net stopping a default factory from materialising the expansion.
 *       The {@code unconfigured*} name is slightly euphemistic here: the factory is actively configured to be permissive, not "left alone".</li>
 * </ul>
 */
class BillionLaughsTest {

    private static final String LARGE_DTD =
            "  <!ENTITY lol \"A\">\n"
            + "  <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
            + "  <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n"
            + "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n"
            + "  <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n"
            + "  <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n"
            + "  <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n"
            + "  <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\n";

    private static final String LARGE_CONTENT = "&lol7;";

    private static final String MEDIUM_DTD =
            "  <!ENTITY lol \"A\">\n"
            + "  <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
            + "  <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n"
            + "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n";

    private static final String MEDIUM_CONTENT = "&lol3;";

    /**
     * Hardened-side payload for DOM/SAX/XmlReader: large on Android (libexpat amplification check), medium on JDK (entity-expansion count limit).
     */
    private static String hardenedXmlPayload() {
        return AttackTestSupport.IS_ANDROID ? largeXmlPayload() : mediumXmlPayload();
    }

    /**
     * ~10 MB of expanded {@code "A"}; trips libexpat &gt;= 2.4's billion-laughs check. Not usable for Templates/Transformer (XSLTC 60 KB cap) or for any
     * unconfigured positive control.
     */
    private static String largeXmlPayload() {
        return withDoctype("root", LARGE_DTD, AttackTestSupport.xmlBody(LARGE_CONTENT));
    }

    /**
     * ~4 KB of expanded {@code "A"}; trips JDK 25's {@code entityExpansionLimit = 2500}. Universal payload that stays under XSLTC's 60 KB constant-pool cap.
     */
    private static String mediumXmlPayload() {
        return withDoctype("root", MEDIUM_DTD, AttackTestSupport.xmlBody(MEDIUM_CONTENT));
    }

    private static String withDoctype(final String rootQName, final String dtd, final String body) {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE " + rootQName + " [\n"
                + dtd
                + "]>\n"
                + body + "\n";
    }

    private static String xsdPayload() {
        return withDoctype("xs:schema", MEDIUM_DTD, AttackTestSupport.xsdBody(MEDIUM_CONTENT));
    }

    private static String xsltPayload() {
        return withDoctype("xsl:stylesheet", MEDIUM_DTD, AttackTestSupport.xsltBody(MEDIUM_CONTENT));
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
        AttackTestSupport.assertSchemaBlocks(AttackTestSupport.streamSource(xsdPayload()));
    }

    @Test
    @Tag("stax")
    void hardenedStaxBlocks() {
        AttackTestSupport.assertStaxBlocks(mediumXmlPayload());
    }

    @Test
    @Tag("trax")
    void hardenedTemplatesDoesNotLeak() {
        AttackTestSupport.assertTemplatesDoesNotLeak(AttackTestSupport.streamSource(xsltPayload()));
    }

    @Test
    @Tag("trax")
    void hardenedTransformerDoesNotLeak() {
        AttackTestSupport.assertTransformerDoesNotLeak(mediumXmlPayload());
    }

    @Test
    @Tag("schema")
    void hardenedValidatorBlocks() {
        AttackTestSupport.assertValidatorBlocks(mediumXmlPayload());
    }

    @Test
    @Tag("sax")
    void hardenedXmlReaderBlocks() {
        AttackTestSupport.assertXmlReaderBlocks(hardenedXmlPayload());
    }

    @Test
    @Tag("dom")
    void unconfiguredDomResolves() {
        Assumptions.assumeTrue(AttackTestSupport.DOM_RESOLVES_INTERNAL_ENTITIES,
                "Skipped: platform DOM does not resolve user-defined entities");
        AttackTestSupport.assertDomResolves(mediumXmlPayload());
    }

    @Test
    @Tag("sax")
    void unconfiguredSaxResolves() {
        AttackTestSupport.assertSaxResolves(mediumXmlPayload());
    }

    @Test
    @Tag("schema")
    void unconfiguredSchemaCompiles() {
        AttackTestSupport.assertSchemaCompiles(AttackTestSupport.streamSource(xsdPayload()));
    }

    @Test
    @Tag("stax")
    void unconfiguredStaxResolves() {
        AttackTestSupport.assertStaxResolves(mediumXmlPayload());
    }

    @Test
    @Tag("trax")
    void unconfiguredTemplatesCompiles() {
        AttackTestSupport.assertTemplatesCompiles(xsltPayload());
    }

    @Test
    @Tag("trax")
    void unconfiguredTransformerSucceeds() {
        AttackTestSupport.assertTransformerSucceeds(mediumXmlPayload());
    }

    @Test
    @Tag("schema")
    void unconfiguredValidatorAccepts() {
        AttackTestSupport.assertValidatorAccepts(mediumXmlPayload());
    }
}
