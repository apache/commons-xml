/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks that a document carrying a {@code <!DOCTYPE root SYSTEM "...">} declaration but no body references parses successfully through the hardened
 * factories.
 *
 * <p>The SYSTEM identifier points at a deliberately bogus URL ({@code http://invalid.example.invalid/...}) under the IANA-reserved {@code .invalid} TLD: any
 * attempt to fetch it would raise a network error long before the test could complete, so a passing test proves the parser did not even try. The hardening
 * contract being verified is "skip the external DTD silently when nothing in the body needs it" rather than "reject every DOCTYPE".</p>
 */
class DoctypeOnlyTest {

    private static final String BOGUS_DTD_URL = "http://invalid.example.invalid/bogus.dtd";

    private static String payload() {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE root SYSTEM \"" + BOGUS_DTD_URL + "\">\n"
                + AttackTestSupport.xmlBody("hello") + "\n";
    }

    private static String xsdPayload() {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE xs:schema SYSTEM \"" + BOGUS_DTD_URL + "\">\n"
                + AttackTestSupport.xsdBody("hello") + "\n";
    }

    private static String xsltPayload() {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE xsl:stylesheet SYSTEM \"" + BOGUS_DTD_URL + "\">\n"
                + AttackTestSupport.xsltBody("hello") + "\n";
    }

    @Test
    @Tag("dom")
    void hardenedDomParses() {
        Assumptions.assumeTrue(AttackTestSupport.DOM_RESOLVES_INTERNAL_ENTITIES,
                "Skipped: platform DOM does not resolve user-defined entities");
        AttackTestSupport.assertDomParses(payload());
    }

    @Test
    @Tag("sax")
    void hardenedSaxParses() {
        AttackTestSupport.assertSaxParses(payload());
    }

    @Test
    @Tag("schema")
    void hardenedSchemaCompiles() {
        AttackTestSupport.assertSchemaCompiles(AttackTestSupport.streamSource(xsdPayload()));
    }

    @Test
    @Tag("stax")
    void hardenedStaxParses() {
        AttackTestSupport.assertStaxParses(payload());
    }

    @Test
    @Tag("trax")
    void hardenedTemplatesCompiles() {
        AttackTestSupport.assertTemplatesCompiles(AttackTestSupport.streamSource(xsltPayload()));
    }

    @Test
    @Tag("trax")
    void hardenedTransformerTransforms() {
        AttackTestSupport.assertTransformerTransforms(payload());
    }

    @Test
    @Tag("schema")
    void hardenedValidatorValidates() {
        AttackTestSupport.assertValidatorValidates(payload());
    }

    @Test
    @Tag("sax")
    void hardenedXmlReaderParses() {
        AttackTestSupport.assertXmlReaderParses(payload());
    }
}
