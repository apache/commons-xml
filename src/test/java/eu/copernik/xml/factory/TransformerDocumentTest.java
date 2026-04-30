/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks whether the XSLT {@code document()} function is resolved at transform time. Compilation succeeds (the function call is just XPath); the leak vector
 * fires only when {@code Transformer.transform} is called and the function evaluates its URI argument.
 */
@Tag("trax")
class TransformerDocumentTest {

    @Test
    void hardenedTransformerBlocks() {
        AttackTestSupport.assertTemplatesBlocks(AttackTestSupport.resourceSource("with-document.xsl"));
    }
}
