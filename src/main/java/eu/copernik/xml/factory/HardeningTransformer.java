/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * {@link Transformer} wrapper that rewrites the Source on every {@link Transformer#transform(Source, Result)} call through
 * {@link XmlFactories#harden(Source)} before delegating.
 */
final class HardeningTransformer extends DelegatingTransformer {

    HardeningTransformer(final Transformer delegate) {
        super(delegate);
    }

    @Override
    public void transform(final Source xmlSource, final Result outputTarget) throws TransformerException {
        try {
            super.transform(XmlFactories.harden(xmlSource), outputTarget);
        } catch (final TransformerConfigurationException e) {
            throw new TransformerException(e);
        }
    }
}
