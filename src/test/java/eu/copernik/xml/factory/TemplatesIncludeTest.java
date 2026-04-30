/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks whether {@code xsl:include} of a sibling stylesheet is resolved at compile time. The wrapper includes a sibling that, if fetched, contributes a
 * template emitting the leaked marker into the transform output.
 */
@Tag("trax")
class TemplatesIncludeTest {

    @Test
    void hardenedTemplatesBlocks() {
        AttackTestSupport.assertTemplatesBlocks(AttackTestSupport.resourceSource("with-include.xsl"));
    }
}
