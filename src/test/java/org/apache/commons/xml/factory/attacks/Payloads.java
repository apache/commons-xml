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

/**
 * Shared building blocks for attack-test payloads: a benign reference schema and body-shape helpers for XML, XSLT and XSD.
 *
 * <p>Each attack test class combines one of the body-shape helpers with an attack-specific DOCTYPE prologue of its own.</p>
 */
final class Payloads {

    /** Trivial W3C XML Schema that validates {@link #xmlBody(String)} output. */
    static final String BENIGN_SCHEMA =
            "<?xml version=\"1.0\"?>\n"
            + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
            + "  <xs:element name=\"root\">\n"
            + "    <xs:complexType>\n"
            + "      <xs:sequence>\n"
            + "        <xs:element name=\"child\" type=\"xs:string\"/>\n"
            + "      </xs:sequence>\n"
            + "    </xs:complexType>\n"
            + "  </xs:element>\n"
            + "</xs:schema>\n";

    private Payloads() {
    }

    static String xmlBody(final String text) {
        return "<root><child>" + text + "</child></root>";
    }

    static String xsltBody(final String text) {
        return "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"
                + "  <xsl:template match=\"/\">" + text + "</xsl:template>\n"
                + "</xsl:stylesheet>";
    }

    static String xsdBody(final String text) {
        return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "  <xs:annotation><xs:documentation>" + text + "</xs:documentation></xs:annotation>\n"
                + "  <xs:element name=\"root\" type=\"xs:string\"/>\n"
                + "</xs:schema>";
    }
}
