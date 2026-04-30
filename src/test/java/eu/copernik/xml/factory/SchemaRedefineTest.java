/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks whether {@code xs:redefine schemaLocation="included.xsd"} that extends a complex type from a sibling schema is resolved at compile time. The wrapper
 * cannot compile unless the redefined type is fetched from the sibling schema.
 */
@Tag("schema")
class SchemaRedefineTest {

    private static final String RESOURCE = "with-redefine.xsd";

    @Test
    void hardenedSchemaBlocks() {
        AttackTestSupport.assertSchemaBlocks(AttackTestSupport.resourceSource(RESOURCE));
    }

    @Test
    void unconfiguredSchemaCompiles() {
        AttackTestSupport.assertPermissiveSchemaCompiles(AttackTestSupport.resourceSource(RESOURCE));
    }
}
