/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import java.util.function.UnaryOperator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Universal SAX wrapper that defers per-parser hardening to a supplied {@link XMLReader} hardener.
 *
 * <p>{@link SAXParserFactory} has no property API, only a feature API. Therefore, complex configuration must be performed on each new
 * {@link XMLReader} parser.</p>
 */
final class HardeningSAXParserFactory extends DelegatingSAXParserFactory {

    private final UnaryOperator<XMLReader> hardener;

    HardeningSAXParserFactory(final SAXParserFactory delegate, final UnaryOperator<XMLReader> hardener) {
        super(delegate);
        this.hardener = hardener;
    }

    @Override
    public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        final SAXParser parser = super.newSAXParser();
        hardener.apply(parser.getXMLReader());
        return parser;
    }
}
