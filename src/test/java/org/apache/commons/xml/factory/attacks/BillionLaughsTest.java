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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks whether parsers reject a Billion Laughs payload (nested entity expansion in the internal DTD subset).
 *
 * <p>The payload nests three levels of 16x expansion ({@code lol1} through {@code lol3}), so resolving {@code &lol3;} produces
 * {@code 16 * 16 * 16 = 4096} leaf {@code &lol;} expansions and a total of {@code 1 + 16 + 256 + 4096 = 4369} entity-expansion events. That is comfortably over
 * the {@code entityExpansionLimit = 2500} this library pins on every JAXP factory it returns (mirroring JDK 25's secure value), so the hardened side trips on
 * every JDK from 8 to 25. The unconfigured side disables the limit explicitly via {@code setAttribute/setProperty} on the JDK property name, so it parses the
 * ~4 KB of expanded {@code "A"} text and finishes immediately.</p>
 *
 * <p>Why a single character {@code "A"}: XSLTC compiles a stylesheet's expanded text into a JVM string constant, which is capped at 65535 bytes. A larger
 * expansion makes {@code newTransformer(stylesheet)} fail with a misleading "GregorSamsa" stub-class error even when entity limits are disabled, so the payload
 * is sized to stay well under that ceiling.</p>
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

    private static final String INSERTION = "&lol3;";

    private static String withDoctype(final String rootQName, final String body) {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE " + rootQName + " [\n"
                + "  <!ENTITY lol \"A\">\n"
                + "  <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
                + "  <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n"
                + "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n"
                + "]>\n"
                + body + "\n";
    }

    private static String xmlPayload() {
        return withDoctype("root", AttackTestSupport.xmlBody(INSERTION));
    }

    private static String xsdPayload() {
        return withDoctype("xs:schema", AttackTestSupport.xsdBody(INSERTION));
    }

    private static String xsltPayload() {
        return withDoctype("xsl:stylesheet", AttackTestSupport.xsltBody(INSERTION));
    }

    @Test
    @Tag("dom")
    void hardenedDomBlocks() {
        AttackTestSupport.assertDomBlocks(xmlPayload());
    }

    @Test
    @Tag("sax")
    void hardenedSaxBlocks() {
        AttackTestSupport.assertSaxBlocks(xmlPayload());
    }

    @Test
    @Tag("schema")
    void hardenedSchemaBlocks() {
        AttackTestSupport.assertSchemaBlocks(AttackTestSupport.streamSource(xsdPayload()));
    }

    @Test
    @Tag("stax")
    void hardenedStaxBlocks() {
        AttackTestSupport.assertStaxBlocks(xmlPayload());
    }

    @Test
    @Tag("trax")
    void hardenedTemplatesDoesNotLeak() {
        AttackTestSupport.assertTemplatesDoesNotLeak(AttackTestSupport.streamSource(xsltPayload()));
    }

    @Test
    @Tag("trax")
    void hardenedTransformerDoesNotLeak() {
        AttackTestSupport.assertTransformerDoesNotLeak(xmlPayload());
    }

    @Test
    @Tag("schema")
    void hardenedValidatorBlocks() {
        AttackTestSupport.assertValidatorBlocks(xmlPayload());
    }

    @Test
    @Tag("sax")
    void hardenedXmlReaderBlocks() {
        AttackTestSupport.assertXmlReaderBlocks(xmlPayload());
    }

    @Test
    @Tag("dom")
    void unconfiguredDomResolves() {
        AttackTestSupport.assertDomResolves(xmlPayload());
    }

    @Test
    @Tag("sax")
    void unconfiguredSaxResolves() {
        AttackTestSupport.assertSaxResolves(xmlPayload());
    }

    @Test
    @Tag("schema")
    void unconfiguredSchemaCompiles() {
        AttackTestSupport.assertSchemaCompiles(AttackTestSupport.streamSource(xsdPayload()));
    }

    @Test
    @Tag("stax")
    void unconfiguredStaxResolves() {
        AttackTestSupport.assertStaxResolves(xmlPayload());
    }

    @Test
    @Tag("trax")
    void unconfiguredTemplatesCompiles() {
        AttackTestSupport.assertTemplatesCompiles(xsltPayload());
    }

    @Test
    @Tag("trax")
    void unconfiguredTransformerSucceeds() {
        AttackTestSupport.assertTransformerSucceeds(xmlPayload());
    }

    @Test
    @Tag("schema")
    void unconfiguredValidatorAccepts() {
        AttackTestSupport.assertValidatorAccepts(xmlPayload());
    }
}
