/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks whether {@code xs:include schemaLocation="included.xsd"} of a sibling schema sharing the wrapper's target namespace is resolved at compile time. The
 * wrapper references an element defined only in the included schema, so compilation cannot succeed unless the schemaLocation is fetched.
 */
@Tag("schema")
class SchemaIncludeTest {

    private static final String RESOURCE = "with-include.xsd";

    @Test
    void hardenedSchemaBlocks() {
        AttackTestSupport.assertSchemaBlocks(AttackTestSupport.resourceSource(RESOURCE));
    }

    @Test
    void unconfiguredSchemaCompiles() {
        AttackTestSupport.assertPermissiveSchemaCompiles(AttackTestSupport.resourceSource(RESOURCE));
    }
}
