/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * {@link SAXParserFactory} subclass that forwards every method to a wrapped delegate.
 */
class DelegatingSAXParserFactory extends SAXParserFactory {

    private final SAXParserFactory delegate;

    DelegatingSAXParserFactory(final SAXParserFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean getFeature(final String name) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        return delegate.getFeature(name);
    }

    @Override
    public Schema getSchema() {
        return delegate.getSchema();
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
    public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        return delegate.newSAXParser();
    }

    @Override
    public void setFeature(final String name, final boolean value) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        delegate.setFeature(name, value);
    }

    @Override
    public void setNamespaceAware(final boolean awareness) {
        delegate.setNamespaceAware(awareness);
    }

    @Override
    public void setSchema(final Schema schema) {
        delegate.setSchema(schema);
    }

    @Override
    public void setValidating(final boolean validating) {
        delegate.setValidating(validating);
    }

    @Override
    public void setXIncludeAware(final boolean state) {
        delegate.setXIncludeAware(state);
    }
}
