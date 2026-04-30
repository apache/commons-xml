/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;

/**
 * {@link DocumentBuilderFactory} subclass that forwards every method to a wrapped delegate.
 */
class DelegatingDocumentBuilderFactory extends DocumentBuilderFactory {

    private final DocumentBuilderFactory delegate;

    DelegatingDocumentBuilderFactory(final DocumentBuilderFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object getAttribute(final String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public boolean getFeature(final String name) throws ParserConfigurationException {
        return delegate.getFeature(name);
    }

    @Override
    public Schema getSchema() {
        return delegate.getSchema();
    }

    @Override
    public boolean isCoalescing() {
        return delegate.isCoalescing();
    }

    @Override
    public boolean isExpandEntityReferences() {
        return delegate.isExpandEntityReferences();
    }

    @Override
    public boolean isIgnoringComments() {
        return delegate.isIgnoringComments();
    }

    @Override
    public boolean isIgnoringElementContentWhitespace() {
        return delegate.isIgnoringElementContentWhitespace();
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
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return delegate.newDocumentBuilder();
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void setCoalescing(final boolean coalescing) {
        delegate.setCoalescing(coalescing);
    }

    @Override
    public void setExpandEntityReferences(final boolean expandEntityRef) {
        delegate.setExpandEntityReferences(expandEntityRef);
    }

    @Override
    public void setFeature(final String name, final boolean value) throws ParserConfigurationException {
        delegate.setFeature(name, value);
    }

    @Override
    public void setIgnoringComments(final boolean ignoreComments) {
        delegate.setIgnoringComments(ignoreComments);
    }

    @Override
    public void setIgnoringElementContentWhitespace(final boolean whitespace) {
        delegate.setIgnoringElementContentWhitespace(whitespace);
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
