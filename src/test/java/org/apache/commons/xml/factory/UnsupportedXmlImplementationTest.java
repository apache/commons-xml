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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.xml.factory.internal.CompositeProvider;
import org.junit.jupiter.api.Test;

/**
 * Verifies that an unknown factory class surfaces {@link IllegalStateException} with a message naming the class and pointing at the remediation path.
 */
class UnsupportedXmlImplementationTest {

    /**
     * A stand-in factory class whose fully qualified name is not matched by any bundled provider or {@code ServiceLoader} service on the test classpath.
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
    void registryRejectsUnknownFactory() {
        final IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> CompositeProvider.getInstance().configure(new FakeDocumentBuilderFactory()));
        assertNotNull(thrown.getMessage());
        assertTrue(thrown.getMessage().contains(FakeDocumentBuilderFactory.class.getName()),
                "Exception message must name the unsupported class: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("ServiceLoader")
                        || thrown.getMessage().contains("XmlProvider"),
                "Message should hint at the remediation path: " + thrown.getMessage());
    }
}
