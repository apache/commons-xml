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
 *       libexpat &gt;= 2.4's built-in billion-laughs check (8 MiB activation threshold and 100x amplification factor).</li>
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
 *   <li>{@code hardened*} tests pull from {@code hardened*Payload} helpers: large fixture on Android (libexpat is the only defence), medium fixture on JDK
 *       (entity-expansion count limit is sufficient).</li>
 *   <li>{@code unconfigured*} positive controls pull from {@code medium*Payload} helpers regardless of platform: libexpat's billion-laughs check cannot be
 *       disabled from Java, so a permissive parse of the large fixture would still trip on Android.</li>
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

    private static final String MEDIUM_CONTENT = "&lol3;";

    private static final String MEDIUM_DTD =
            "  <!ENTITY lol \"A\">\n"
            + "  <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
            + "  <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n"
            + "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n";

    /**
     * Hardened-side payload for DOM/SAX/XmlReader
     *
     * <ul>
     *     <li>~10 MiB on Android to trip libexpats 8 MiB limit, and</li>
     *     <li>~4 KiB on JDK to trip JDK 25's {@code entityExpansionLimit = 2500}.</li>
     * </ul>
     */
    private static String hardenedXmlPayload() {
        return AttackTestSupport.IS_ANDROID
                ? withDoctype("root", LARGE_DTD, AttackTestSupport.xmlBody(LARGE_CONTENT))
                : mediumXmlPayload();
    }

    /**
     * Hardened-side XSD payload
     *
     * <ul>
     *     <li>~10 MiB on Android to trip libexpats 8 MiB limit, and</li>
     *     <li>~4 KiB on JDK to trip JDK 25's {@code entityExpansionLimit = 2500}.</li>
     * </ul>
     */
    private static String hardenedXsdPayload() {
        return AttackTestSupport.IS_ANDROID
                ? withDoctype("xs:schema", LARGE_DTD, AttackTestSupport.xsdBody(LARGE_CONTENT))
                : mediumXsdPayload();
    }

    /**
     * Hardened-side XSLT payload
     *
     * <ul>
     *     <li>~10 MiB on Android to trip libexpats 8 MiB limit, and</li>
     *     <li>~4 KiB on JDK to trip JDK 25's {@code entityExpansionLimit = 2500}, and</li>
     *     <li>stay under XSLTC's 60 KB constant-pool cap, and</li>
     * </ul>
     */
    private static String hardenedXsltPayload() {
        return AttackTestSupport.IS_ANDROID
                ? withDoctype("xsl:stylesheet", LARGE_DTD, AttackTestSupport.xsltBody(LARGE_CONTENT))
                : mediumXsltPayload();
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
