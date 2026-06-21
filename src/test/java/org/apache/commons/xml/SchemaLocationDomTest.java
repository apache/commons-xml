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

package org.apache.commons.xml;

import static org.apache.commons.xml.AttackTestSupport.LEAKED_MARKER;
import static org.apache.commons.xml.AttackTestSupport.resourceUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Checks that a hardened {@link DocumentBuilderFactory} performing JAXP 1.2 XSD validation does not fetch an external schema named by an
 * {@code xsi:noNamespaceSchemaLocation} hint in the instance document.
 *
 * <p>The instance is empty {@code <root/>}; the referenced schema declares a default {@code leak} attribute carrying {@link AttackTestSupport#LEAKED_MARKER}. A
 * parser that fetches the schema inlines that default into the DOM (the permissive control), while a hardened parser blocks the fetch via
 * {@code accessExternalSchema=""} and surfaces a {@code schema_reference} error instead.</p>
 *
 * <p>The guard only runs where the implementation honours {@link XMLConstants#ACCESS_EXTERNAL_SCHEMA}: the stock JDK does, external Xerces does not (it rejects
 * the property and would fetch regardless), so the test skips there.</p>
 */
@Tag("dom")
class SchemaLocationDomTest {

    /** JAXP 1.2 property selecting the schema language used by {@link DocumentBuilderFactory#setValidating(boolean)}. */
    private static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    /** Value of {@link XMLConstants#ACCESS_EXTERNAL_SCHEMA}, inlined because Android's {@code android.jar} does not expose the constant. */
    private static final String ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";

    private static final String INSTANCE = "schema-location-instance.xml";

    @Test
    void hardenedBlocksExternalSchemaFetch() {
        assumeTrue(supportsAccessExternalSchema(), "parser does not honour accessExternalSchema");
        final DocumentBuilderFactory factory = enableXsdValidation(XmlFactories.newDocumentBuilderFactory());
        // The schemaLocation fetch is denied, surfaced as a schema_reference / accessExternalSchema error rather than a silent skip.
        final SAXException thrown = assertThrows(SAXException.class, () -> parse(factory));
        assertTrue(thrown.getMessage() != null && thrown.getMessage().contains("accessExternalSchema"),
                "Block must be attributed to accessExternalSchema, got: " + thrown.getMessage());
    }

    @Test
    void unconfiguredFetchesExternalSchema() throws Exception {
        assumeTrue(supportsAccessExternalSchema(), "parser does not honour accessExternalSchema");
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        // Positive control: without hardening the external schema is fetched and its default attribute is inlined into the DOM.
        final Document document = parse(enableXsdValidation(factory));
        assertEquals(LEAKED_MARKER, document.getDocumentElement().getAttribute("leak"),
                "Permissive parse should have fetched the external schema and inlined its default attribute.");
    }

    private static DocumentBuilderFactory enableXsdValidation(final DocumentBuilderFactory factory) {
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        factory.setAttribute(SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return factory;
    }

    private static Document parse(final DocumentBuilderFactory factory) throws Exception {
        final DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            @Override
            public void error(final SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
        return builder.parse(new InputSource(resourceUrl(INSTANCE).toString()));
    }

    private static boolean supportsAccessExternalSchema() {
        try {
            DocumentBuilderFactory.newInstance().setAttribute(ACCESS_EXTERNAL_SCHEMA, "");
            return true;
        } catch (final Exception e) {
            return false;
        }
    }
}
