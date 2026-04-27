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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks whether parsers can pull in an external DTD via a parameter-entity reference inside the internal subset.
 *
 * <p>The wrapper declares a parameter entity {@code %xxe;} pointing at {@code src/test/resources/leaked/referenced.dtd} and immediately references it in the
 * internal subset; once expanded, the entity declarations from {@code referenced.dtd} (in particular {@code <!ENTITY leaked "...">}) become part of the
 * document's DTD. Each wrapper body then references {@code &leaked;}, so the parse cannot succeed unless the parameter-entity expansion is allowed: a hardened
 * parser refuses, {@code &leaked;} is undefined, and the parse throws; an unconfigured parser fetches and resolves, and the parse succeeds.</p>
 *
 * <p>Each parser type is exercised twice as a pair (unconfigured factory, expected to parse; hardened factory, expected to throw):</p>
 *
 * <ul>
 *   <li>DOM, SAX and StAX direct XML parsing.</li>
 *   <li>{@code SchemaFactory.newSchema(Source)} compilation of an XSD whose source has the parameter-entity DOCTYPE.</li>
 *   <li>{@link javax.xml.validation.Validator#validate(javax.xml.transform.Source)} of an instance whose source has the parameter-entity DOCTYPE.</li>
 *   <li>Identity {@code Transformer} reading the input XML.</li>
 *   <li>{@code TransformerFactory.newTransformer(Source)} compilation of a stylesheet whose source has the parameter-entity DOCTYPE.</li>
 * </ul>
 */
class ExternalParameterEntityTest {

    private static final String INSERTION = "&leaked;";

    private static String withDoctype(final String rootQName, final String body) {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE " + rootQName + " [\n"
                + "  <!ENTITY % xxe SYSTEM \"" + AttackTestSupport.resourceUrl("referenced.dtd") + "\">\n"
                + "  %xxe;\n"
                + "]>\n"
                + body + "\n";
    }

    private static String xmlPayload() {
        return withDoctype("root", AttackTestSupport.xmlBody(INSERTION));
    }

    private static String xsdPayload() {
        return withDoctype("xs:schema", AttackTestSupport.xsdBody(INSERTION));
    }

    private static String xsltPayload() {
        return withDoctype("xsl:stylesheet", AttackTestSupport.xsltBody(INSERTION));
    }

    @Test
    @Tag("dom")
    void hardenedDomBlocks() {
        AttackTestSupport.assertDomBlocks(xmlPayload());
    }

    @Test
    @Tag("sax")
    void hardenedSaxBlocks() {
        AttackTestSupport.assertSaxBlocks(xmlPayload());
    }

    @Test
    @Tag("schema")
    void hardenedSchemaBlocks() {
        AttackTestSupport.assertSchemaBlocks(AttackTestSupport.streamSource(xsdPayload()));
    }

    @Test
    @Tag("stax")
    void hardenedStaxBlocks() {
        AttackTestSupport.assertStaxBlocks(xmlPayload());
    }

    @Test
    @Tag("trax")
    void hardenedTemplatesBlocks() {
        AttackTestSupport.assertTemplatesBlocks(AttackTestSupport.streamSource(xsltPayload()));
    }

    @Test
    @Tag("trax")
    void hardenedTransformerBlocks() {
        AttackTestSupport.assertTransformerBlocks(xmlPayload());
    }

    @Test
    @Tag("schema")
    void hardenedValidatorBlocks() {
        AttackTestSupport.assertValidatorBlocks(xmlPayload());
    }

    @Test
    @Tag("sax")
    void hardenedXmlReaderBlocks() {
        AttackTestSupport.assertXmlReaderBlocks(xmlPayload());
    }

    @Test
    @Tag("dom")
    void unconfiguredDomResolves() {
        AttackTestSupport.assertDomResolves(xmlPayload());
    }

    @Test
    @Tag("sax")
    void unconfiguredSaxResolves() {
        AttackTestSupport.assertSaxResolves(xmlPayload());
    }

    @Test
    @Tag("schema")
    void unconfiguredSchemaCompiles() {
        AttackTestSupport.assertSchemaCompiles(AttackTestSupport.streamSource(xsdPayload()));
    }

    @Test
    @Tag("stax")
    void unconfiguredStaxResolves() {
        AttackTestSupport.assertStaxResolves(xmlPayload());
    }

    @Test
    @Tag("trax")
    void unconfiguredTemplatesCompiles() {
        AttackTestSupport.assertTemplatesCompiles(xsltPayload());
    }

    @Test
    @Tag("trax")
    void unconfiguredTransformerSucceeds() {
        AttackTestSupport.assertTransformerSucceeds(xmlPayload());
    }

    @Test
    @Tag("schema")
    void unconfiguredValidatorAccepts() {
        AttackTestSupport.assertValidatorAccepts(xmlPayload());
    }
}
