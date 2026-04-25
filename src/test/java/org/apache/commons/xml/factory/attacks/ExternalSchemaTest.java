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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.xml.factory.XmlFactories;
import org.junit.jupiter.api.Test;

/**
 * Checks whether schemas can pull in external schemas at compile time.
 *
 * <p>Each wrapper under {@code src/test/resources/leaked/} references an element or type defined only in {@code included.xsd}, so the wrapper cannot compile
 * unless the external schemaLocation is actually resolved.</p>
 *
 * <p>Each case is exercised twice as a pair:</p>
 *
 * <ul>
 *   <li>An {@code unconfigured*} test compiles the wrapper through a default {@link SchemaFactory#newInstance(String)} and asserts compilation succeeds. This
 *       is the positive control: it proves the wrapper itself is well-formed and that the resolution would succeed if hardening were not in the way, so the
 *       paired {@code hardened*} test is genuinely exercising hardening rather than passing for an unrelated reason.</li>
 *   <li>A {@code hardened*} test compiles the wrapper through {@link XmlFactories#newSchemaFactory()} and asserts compilation throws. If hardening blocks the
 *       schemaLocation, compilation throws (either with an access-restriction message or with an unresolved-reference message); if hardening fails to block,
 *       compilation succeeds and the test fails.</li>
 * </ul>
 *
 * <p>Cases covered:</p>
 *
 * <ul>
 *   <li>{@code xs:include schemaLocation="included.xsd"} of a sibling schema sharing the wrapper's target namespace.</li>
 *   <li>{@code xs:import namespace="..." schemaLocation="included.xsd"} of a sibling schema in a different target namespace.</li>
 *   <li>{@code xs:redefine schemaLocation="included.xsd"} that extends a complex type from the sibling schema.</li>
 * </ul>
 */
class ExternalSchemaTest {

    private static void assertSchemaCompileFails(final String resource) {
        final URL url = AttackTestSupport.resourceUrl(resource);
        assertThrows(Exception.class, () -> {
            final StreamSource src = new StreamSource(url.openStream(), url.toString());
            XmlFactories.newSchemaFactory().newSchema(src);
        }, "Hardening did not block the external schemaLocation; schema compiled successfully from " + resource);
    }

    private static void assertSchemaCompiles(final String resource) {
        final URL url = AttackTestSupport.resourceUrl(resource);
        assertDoesNotThrow(() -> {
            final StreamSource src = new StreamSource(url.openStream(), url.toString());
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(src);
        }, "Unconfigured SchemaFactory should compile " + resource + "; the wrapper or its sibling include/import/redefine target is broken.");
    }

    @Test
    void hardenedBlocksImport() {
        assertSchemaCompileFails("with-import.xsd");
    }

    @Test
    void hardenedBlocksInclude() {
        assertSchemaCompileFails("with-include.xsd");
    }

    @Test
    void hardenedBlocksRedefine() {
        assertSchemaCompileFails("with-redefine.xsd");
    }

    @Test
    void unconfiguredCompilesImport() {
        assertSchemaCompiles("with-import.xsd");
    }

    @Test
    void unconfiguredCompilesInclude() {
        assertSchemaCompiles("with-include.xsd");
    }

    @Test
    void unconfiguredCompilesRedefine() {
        assertSchemaCompiles("with-redefine.xsd");
    }
}
