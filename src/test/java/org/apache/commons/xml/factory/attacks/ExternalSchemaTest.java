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

import org.junit.jupiter.api.Test;

/**
 * Exercises {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_SCHEMA ACCESS_EXTERNAL_SCHEMA} via the three XSD compile-time vectors it guards:
 * {@code xs:import}, {@code xs:include} and {@code xs:redefine}.
 */
class ExternalSchemaTest {

    private static final String IMPORT_NAMESPACE = "http://example.org/external";

    private static String xsdWithImport(final String namespace, final String schemaLocation) {
        return "<?xml version=\"1.0\"?>\n"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "  <xs:import namespace=\"" + namespace + "\" schemaLocation=\"" + schemaLocation + "\"/>\n"
                + "  <xs:element name=\"root\" type=\"xs:string\"/>\n"
                + "</xs:schema>\n";
    }

    private static String xsdWithInclude(final String schemaLocation) {
        return "<?xml version=\"1.0\"?>\n"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "  <xs:include schemaLocation=\"" + schemaLocation + "\"/>\n"
                + "  <xs:element name=\"root\" type=\"xs:string\"/>\n"
                + "</xs:schema>\n";
    }

    private static String xsdWithRedefine(final String schemaLocation) {
        return "<?xml version=\"1.0\"?>\n"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "  <xs:redefine schemaLocation=\"" + schemaLocation + "\">\n"
                + "    <xs:complexType name=\"Placeholder\"><xs:sequence/></xs:complexType>\n"
                + "  </xs:redefine>\n"
                + "  <xs:element name=\"root\" type=\"xs:string\"/>\n"
                + "</xs:schema>\n";
    }

    @Test
    void schemaFactoryBlocksImport() {
        AttackTestSupport.assertSchemaCompilationBlocks(xsdWithImport(IMPORT_NAMESPACE, Payloads.UNREACHABLE_HTTP));
    }

    @Test
    void schemaFactoryBlocksInclude() {
        AttackTestSupport.assertSchemaCompilationBlocks(xsdWithInclude(Payloads.UNREACHABLE_HTTP));
    }

    @Test
    void schemaFactoryBlocksRedefine() {
        AttackTestSupport.assertSchemaCompilationBlocks(xsdWithRedefine(Payloads.UNREACHABLE_HTTP));
    }
}
