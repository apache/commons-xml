/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks that a plain document without a {@code DOCTYPE} declaration parses cleanly through every hardened JAXP surface.
 *
 * <p>This is the realistic 99% case for hardened input; the hardening contract being verified is "documents without a DOCTYPE parse cleanly through every
 * JAXP surface" so accidental tightening (e.g. a resolver that refuses the synthetic-external-subset hook) is caught.</p>
 */
class NoDoctypeTest {

    private static String payload() {
        return "<?xml version=\"1.0\"?>\n"
                + AttackTestSupport.xmlBody("hello") + "\n";
    }

    private static String xsdPayload() {
        return "<?xml version=\"1.0\"?>\n"
                + AttackTestSupport.xsdBody("hello") + "\n";
    }

    private static String xsltPayload() {
        return "<?xml version=\"1.0\"?>\n"
                + AttackTestSupport.xsltBody("hello") + "\n";
    }

    @Test
    @Tag("dom")
    void hardenedDomParses() {
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
