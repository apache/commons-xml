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
 * <p>The payload nests four levels of 16/15x expansion ({@code lol1} through {@code lol4}), so resolving {@code &lol4;} produces
 * {@code 15 * 16 * 16 * 16 = 61440} leaf {@code &lol;} expansions and a total of {@code 1 + 15 + 240 + 3840 + 61440 = 65536} entity-expansion events. That is
 * just over the JDK's default {@code jdk.xml.entityExpansionLimit = 64000}, so a parser with the limit enforced rejects the payload, and a parser with the
 * limit disabled materialises only ~60 KB of {@code "A"} text and finishes immediately.</p>
 *
 * <p>Why a single character {@code "A"} and only 15x at the outer level: XSLTC compiles a stylesheet's expanded text into a JVM string constant, which is
 * capped at 65535 bytes. A larger expansion makes {@code newTransformer(stylesheet)} fail with a misleading "GregorSamsa" stub-class error even when entity
 * limits are disabled, so the payload is sized to stay just under that ceiling.</p>
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

    private static final String INSERTION = "&lol4;";

    private static String withDoctype(final String rootQName, final String body) {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE " + rootQName + " [\n"
                + "  <!ENTITY lol \"A\">\n"
                + "  <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
                + "  <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n"
                + "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n"
                + "  <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n"
                + "]>\n"
                + body + "\n";
    }

    private static String xmlPayload() {
        return withDoctype("root", Payloads.xmlBody(INSERTION));
    }

    private static String xsdPayload() {
        return withDoctype("xs:schema", Payloads.xsdBody(INSERTION));
    }

    private static String xsltPayload() {
        return withDoctype("xsl:stylesheet", Payloads.xsltBody(INSERTION));
    }

    @Test
    @Tag("dom")
    void hardenedDomBlocksBillionLaughs() {
        AttackTestSupport.assertDomBlocks(xmlPayload());
    }

    @Test
    @Tag("sax")
    void hardenedSaxBlocksBillionLaughs() {
        AttackTestSupport.assertSaxBlocks(xmlPayload());
    }

    @Test
    @Tag("schema")
    void hardenedSchemaBlocksBillionLaughs() {
        AttackTestSupport.assertSchemaCompilationBlocks(xsdPayload());
    }

    @Test
    @Tag("stax")
    void hardenedStaxBlocksBillionLaughs() {
        AttackTestSupport.assertStaxBlocks(xmlPayload());
    }

    @Test
    @Tag("trax")
    void hardenedStylesheetBlocksBillionLaughs() {
        AttackTestSupport.assertStylesheetCompilationBlocks(xsltPayload());
    }

    @Test
    @Tag("trax")
    void hardenedTransformerBlocksBillionLaughs() {
        AttackTestSupport.assertTransformerBlocks(xmlPayload());
    }

    @Test
    @Tag("schema")
    void hardenedValidatorBlocksBillionLaughs() {
        AttackTestSupport.assertValidatorBlocks(xmlPayload());
    }

    @Test
    @Tag("dom")
    void unconfiguredDomResolvesBillionLaughs() {
        AttackTestSupport.assertDomResolves(xmlPayload());
    }

    @Test
    @Tag("sax")
    void unconfiguredSaxResolvesBillionLaughs() {
        AttackTestSupport.assertSaxResolves(xmlPayload());
    }

    @Test
    @Tag("schema")
    void unconfiguredSchemaCompilesBillionLaughs() {
        AttackTestSupport.assertSchemaCompilationSucceeds(xsdPayload());
    }

    @Test
    @Tag("stax")
    void unconfiguredStaxResolvesBillionLaughs() {
        AttackTestSupport.assertStaxResolves(xmlPayload());
    }

    @Test
    @Tag("trax")
    void unconfiguredStylesheetCompilesBillionLaughs() {
        AttackTestSupport.assertStylesheetCompilationSucceeds(xsltPayload());
    }

    @Test
    @Tag("trax")
    void unconfiguredTransformerSucceedsBillionLaughs() {
        AttackTestSupport.assertTransformerSucceeds(xmlPayload());
    }

    @Test
    @Tag("schema")
    void unconfiguredValidatorAcceptsBillionLaughs() {
        AttackTestSupport.assertValidatorAccepts(xmlPayload());
    }
}
