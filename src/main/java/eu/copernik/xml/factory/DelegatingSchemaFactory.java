/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * {@link SchemaFactory} subclass that forwards every method to a wrapped delegate.
 */
class DelegatingSchemaFactory extends SchemaFactory {

    private final SchemaFactory delegate;

    DelegatingSchemaFactory(final SchemaFactory delegate) {
        this.delegate = delegate;
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
    public LSResourceResolver getResourceResolver() {
        return delegate.getResourceResolver();
    }

    @Override
    public boolean isSchemaLanguageSupported(final String schemaLanguage) {
        return delegate.isSchemaLanguageSupported(schemaLanguage);
    }

    @Override
    public Schema newSchema() throws SAXException {
        return delegate.newSchema();
    }

    @Override
    public Schema newSchema(final Source[] schemas) throws SAXException {
        return delegate.newSchema(schemas);
    }

    @Override
    public void setErrorHandler(final ErrorHandler errorHandler) {
        delegate.setErrorHandler(errorHandler);
    }

    @Override
    public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        delegate.setFeature(name, value);
    }

    @Override
    public void setProperty(final String name, final Object object) throws SAXNotRecognizedException, SAXNotSupportedException {
        delegate.setProperty(name, object);
    }

    @Override
    public void setResourceResolver(final LSResourceResolver resourceResolver) {
        delegate.setResourceResolver(resourceResolver);
    }
}
