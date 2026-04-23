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

class ExternalGeneralEntityTest {

    private static final String INSERTION = "&xxe;";

    private static String withDoctype(final String rootQName, final String body) {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE " + rootQName + " [\n"
                + "  <!ENTITY xxe SYSTEM \"" + Payloads.UNREACHABLE_FILE + "\">\n"
                + "]>\n"
                + body + "\n";
    }

    @Test
    void domBlocks() {
        AttackTestSupport.assertDomBlocks(withDoctype("root", Payloads.xmlBody(INSERTION)));
    }

    @Test
    void saxBlocks() {
        AttackTestSupport.assertSaxBlocks(withDoctype("root", Payloads.xmlBody(INSERTION)));
    }

    @Test
    void schemaFactoryBlocks() {
        AttackTestSupport.assertSchemaCompilationBlocks(withDoctype("xs:schema", Payloads.xsdBody(INSERTION)));
    }

    @Test
    void staxBlocks() {
        AttackTestSupport.assertStaxBlocks(withDoctype("root", Payloads.xmlBody(INSERTION)));
    }

    @Test
    void transformerBlocks() {
        AttackTestSupport.assertTransformerBlocks(withDoctype("root", Payloads.xmlBody(INSERTION)));
    }

    @Test
    void transformerStylesheetBlocks() {
        AttackTestSupport.assertStylesheetCompilationBlocks(withDoctype("xsl:stylesheet", Payloads.xsltBody(INSERTION)));
    }

    @Test
    void validatorBlocks() {
        AttackTestSupport.assertValidatorBlocks(withDoctype("root", Payloads.xmlBody(INSERTION)));
    }
}
