/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;

/**
 * Verifies that an unknown factory class surfaces {@link IllegalStateException} with a message naming the class.
 */
class UnsupportedXmlImplementationTest {

    /**
     * A stand-in factory class whose fully qualified name is not matched by any bundled hardening recipe.
     */
    public static final class FakeDocumentBuilderFactory extends DocumentBuilderFactory {

        @Override
        public DocumentBuilder newDocumentBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAttribute(final String name, final Object value) {
            // no-op
        }

        @Override
        public Object getAttribute(final String name) {
            return null;
        }

        @Override
        public void setFeature(final String name, final boolean value) {
            // no-op
        }

        @Override
        public boolean getFeature(final String name) {
            return false;
        }
    }

    @Test
    void dispatchRejectsUnknownFactory() {
        final IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> XmlFactories.dispatch(new FakeDocumentBuilderFactory()));
        assertNotNull(thrown.getMessage());
        assertTrue(thrown.getMessage().contains(FakeDocumentBuilderFactory.class.getName()),
                "Exception message must name the unsupported class: " + thrown.getMessage());
    }
}
