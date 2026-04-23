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
 * Exercises {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_STYLESHEET ACCESS_EXTERNAL_STYLESHEET} via the three XSLT vectors it guards:
 * {@code xsl:include} and {@code xsl:import} at compile time, and the {@code document()} function at transform time.
 */
class ExternalStylesheetTest {

    private static final String INPUT = "<root/>";

    private static String xsltWithDocumentCall(final String uri) {
        return "<?xml version=\"1.0\"?>\n"
                + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"
                + "  <xsl:template match=\"/\">\n"
                + "    <xsl:copy-of select=\"document('" + uri + "')\"/>\n"
                + "  </xsl:template>\n"
                + "</xsl:stylesheet>\n";
    }

    private static String xsltWithImport(final String href) {
        return "<?xml version=\"1.0\"?>\n"
                + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"
                + "  <xsl:import href=\"" + href + "\"/>\n"
                + "</xsl:stylesheet>\n";
    }

    private static String xsltWithInclude(final String href) {
        return "<?xml version=\"1.0\"?>\n"
                + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"
                + "  <xsl:include href=\"" + href + "\"/>\n"
                + "</xsl:stylesheet>\n";
    }

    @Test
    void transformerBlocksDocument() {
        AttackTestSupport.assertTransformerBlocksWithStylesheet(xsltWithDocumentCall(Payloads.UNREACHABLE_HTTP), INPUT);
    }

    @Test
    void transformerStylesheetBlocksImport() {
        AttackTestSupport.assertStylesheetCompilationBlocks(xsltWithImport(Payloads.UNREACHABLE_HTTP));
    }

    @Test
    void transformerStylesheetBlocksInclude() {
        AttackTestSupport.assertStylesheetCompilationBlocks(xsltWithInclude(Payloads.UNREACHABLE_HTTP));
    }
}
