/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Public-API smoke tests for {@link XmlFactories}.
 *
 * <p>Attack tests live in the {@code attacks} sub-package; this file only verifies that fresh factories are returned, that they report safe defaults, and that
 * a benign document still parses successfully.</p>
 */
class XmlFactoriesTest {

    private static final String BENIGN_XML =
            "<?xml version=\"1.0\"?>\n<root><child>hello</child></root>\n";

    @Test
    void newDocumentBuilderFactoryReturnsFreshInstance() {
        final DocumentBuilderFactory a = XmlFactories.newDocumentBuilderFactory();
        final DocumentBuilderFactory b = XmlFactories.newDocumentBuilderFactory();
        assertNotNull(a);
        assertNotNull(b);
        assertNotSame(a, b);
    }

    @Test
    void newDocumentBuilderFactoryDisablesXIncludeAndValidation() {
        final DocumentBuilderFactory factory = XmlFactories.newDocumentBuilderFactory();
        assertFalse(factory.isXIncludeAware(), "XInclude must be off by default");
        assertFalse(factory.isValidating(), "Validation must be off by default");
    }

    @Test
    void newDocumentBuilderFactoryEnablesSecureProcessing() throws Exception {
        final DocumentBuilderFactory factory = XmlFactories.newDocumentBuilderFactory();
        assertTrue(factory.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING),
                "FEATURE_SECURE_PROCESSING must be on");
    }

    @Test
    void benignDocumentParses() throws Exception {
        final Document doc = XmlFactories.newDocumentBuilderFactory().newDocumentBuilder().parse(new InputSource(new StringReader(BENIGN_XML)));
        assertNotNull(doc);
        assertNotNull(doc.getDocumentElement());
    }

    @Test
    void newSAXParserFactoryReturnsFreshInstance() {
        final SAXParserFactory a = XmlFactories.newSAXParserFactory();
        final SAXParserFactory b = XmlFactories.newSAXParserFactory();
        assertNotSame(a, b);
        assertFalse(a.isValidating());
        assertFalse(a.isXIncludeAware());
    }

    @Test
    void newXMLInputFactoryReturnsFreshInstance() {
        final XMLInputFactory a = XmlFactories.newXMLInputFactory();
        final XMLInputFactory b = XmlFactories.newXMLInputFactory();
        assertNotSame(a, b);
        assertEquals(Boolean.TRUE, a.getProperty(XMLInputFactory.SUPPORT_DTD));
        assertEquals(Boolean.FALSE, a.getProperty(XMLInputFactory.IS_VALIDATING));
    }

    @Test
    void newTransformerFactoryReturnsFreshInstance() {
        final TransformerFactory a = XmlFactories.newTransformerFactory();
        final TransformerFactory b = XmlFactories.newTransformerFactory();
        assertNotSame(a, b);
    }

    @Test
    void newXPathFactoryReturnsFreshInstance() throws Exception {
        final XPathFactory a = XmlFactories.newXPathFactory();
        final XPathFactory b = XmlFactories.newXPathFactory();
        assertNotSame(a, b);
        assertTrue(a.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));
    }

    @Test
    void newSchemaFactoryReturnsFreshInstance() throws Exception {
        final SchemaFactory a = XmlFactories.newSchemaFactory();
        final SchemaFactory b = XmlFactories.newSchemaFactory();
        assertNotSame(a, b);
        assertTrue(a.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));
    }

}
