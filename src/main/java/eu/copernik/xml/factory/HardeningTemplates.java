/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;

/**
 * {@link Templates} wrapper whose only purpose is to return a {@link HardeningTransformer} from {@link Templates#newTransformer()}, with the factory's
 * compile-time {@link URIResolver} pre-installed.
 *
 * <p>Both Apache Xalan 2.7 and stock-JDK XSLTC fail to propagate the factory's URIResolver through {@code Templates.newTransformer()}: the produced runtime
 * Transformer has a null URIResolver unless the caller sets one, leaving runtime {@code document()} calls unguarded. Snapshotting the resolver at compile time
 * and restoring it onto the runtime Transformer matches the JAXP-conformant intuition that the factory's resolver is the default for any Transformer the
 * factory ultimately produces.</p>
 */
final class HardeningTemplates extends DelegatingTemplates {

    /** Compile-time URIResolver snapshot; the underlying impl does not propagate the factory's resolver onto Transformers obtained from Templates. */
    private final URIResolver uriResolver;

    HardeningTemplates(final Templates delegate, final URIResolver uriResolver) {
        super(delegate);
        this.uriResolver = uriResolver;
    }

    @Override
    public Transformer newTransformer() throws TransformerConfigurationException {
        final Transformer transformer = super.newTransformer();
        if (transformer == null) {
            return null;
        }
        transformer.setURIResolver(uriResolver);
        return new HardeningTransformer(transformer);
    }
}
