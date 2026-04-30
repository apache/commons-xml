/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import java.util.Properties;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

/**
 * {@link Templates} wrapper that forwards every method to a wrapped delegate.
 */
class DelegatingTemplates implements Templates {

    private final Templates delegate;

    DelegatingTemplates(final Templates delegate) {
        this.delegate = delegate;
    }

    @Override
    public Properties getOutputProperties() {
        return delegate.getOutputProperties();
    }

    @Override
    public Transformer newTransformer() throws TransformerConfigurationException {
        return delegate.newTransformer();
    }
}
