/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import javax.xml.parsers.SAXParser;
import javax.xml.validation.Schema;

import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * {@link SAXParser} that forwards every method to a wrapped delegate.
 *
 * <p>The non-abstract {@code parse(...)} overloads inherited from {@link SAXParser} call {@code this.getXMLReader()} virtually, so a subclass that only
 * overrides {@link #getXMLReader()} can redirect every parse path through a different reader without having to override the overloads.</p>
 */
class DelegatingSAXParser extends SAXParser {

    private final SAXParser delegate;

    DelegatingSAXParser(final SAXParser delegate) {
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Parser getParser() throws SAXException {
        return delegate.getParser();
    }

    @Override
    public Object getProperty(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return delegate.getProperty(name);
    }

    @Override
    public Schema getSchema() {
        return delegate.getSchema();
    }

    @Override
    public XMLReader getXMLReader() throws SAXException {
        return delegate.getXMLReader();
    }

    @Override
    public boolean isNamespaceAware() {
        return delegate.isNamespaceAware();
    }

    @Override
    public boolean isValidating() {
        return delegate.isValidating();
    }

    @Override
    public boolean isXIncludeAware() {
        return delegate.isXIncludeAware();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public void setProperty(final String name, final Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        delegate.setProperty(name, value);
    }
}
