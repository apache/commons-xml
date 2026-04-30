/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * {@link Validator} wrapper that rewrites the Source on every {@link Validator#validate(Source)} and {@link Validator#validate(Source, Result)} call through
 * {@link XmlFactories#harden(Source)} before delegating.
 */
final class HardeningValidator extends Validator {

    private final Validator delegate;

    HardeningValidator(final Validator delegate) {
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
    public void reset() {
        delegate.reset();
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

    @Override
    public void validate(final Source source, final Result result) throws SAXException, IOException {
        try {
            delegate.validate(XmlFactories.harden(source), result);
        } catch (final TransformerConfigurationException e) {
            throw new SAXException("Failed to harden source for validation", e);
        }
    }
}
