/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import java.io.IOException;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * {@link XMLReader} that forwards every method to a wrapped delegate.
 */
class DelegatingXMLReader implements XMLReader {

    private final XMLReader delegate;

    DelegatingXMLReader(final XMLReader delegate) {
        this.delegate = delegate;
    }

    @Override
    public ContentHandler getContentHandler() {
        return delegate.getContentHandler();
    }

    @Override
    public DTDHandler getDTDHandler() {
        return delegate.getDTDHandler();
    }

    @Override
    public EntityResolver getEntityResolver() {
        return delegate.getEntityResolver();
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return delegate.getErrorHandler();
    }

    @Override
    public boolean getFeature(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return delegate.getFeature(name);
    }

    @Override
    public Object getProperty(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return delegate.getProperty(name);
    }

    @Override
    public void parse(final InputSource input) throws IOException, SAXException {
        delegate.parse(input);
    }

    @Override
    public void parse(final String systemId) throws IOException, SAXException {
        delegate.parse(systemId);
    }

    @Override
    public void setContentHandler(final ContentHandler handler) {
        delegate.setContentHandler(handler);
    }

    @Override
    public void setDTDHandler(final DTDHandler handler) {
        delegate.setDTDHandler(handler);
    }

    @Override
    public void setEntityResolver(final EntityResolver resolver) {
        delegate.setEntityResolver(resolver);
    }

    @Override
    public void setErrorHandler(final ErrorHandler handler) {
        delegate.setErrorHandler(handler);
    }

    @Override
    public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        delegate.setFeature(name, value);
    }

    @Override
    public void setProperty(final String name, final Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        delegate.setProperty(name, value);
    }
}
