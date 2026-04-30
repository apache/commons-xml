/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.XMLReader;

/**
 * {@link javax.xml.transform.TransformerFactory} wrapper that rewrites every Source-taking entry point through {@link XmlFactories#harden(Source)} before
 * delegating.
 *
 * <p>Used by providers whose underlying TrAX implementation pulls a fresh {@code SAXParserFactory.newInstance()} for any Source that is not already a
 * {@link SAXSource} carrying its own {@link XMLReader}, and only sets {@link javax.xml.XMLConstants#FEATURE_SECURE_PROCESSING FSP} on the resulting reader.
 * Wrapping the factory and rewriting the Source upstream guarantees the parse runs through an {@link XmlFactories}-hardened reader instead.</p>
 *
 * <p>Three layers cooperate:</p>
 * <ol>
 *   <li>{@link HardeningTransformerFactory} rewrites the Source on every entry point that compiles a stylesheet or transforms a one-shot input.</li>
 *   <li>{@link HardeningTemplates} returns a {@link HardeningTransformer} from {@link Templates#newTransformer()} so runtime source parsing is also covered, and
 *       restores the factory's URIResolver onto the produced Transformer (which the underlying impl typically does not propagate through {@code Templates}).</li>
 *   <li>{@link HardeningTransformer} rewrites the Source on every {@link Transformer#transform(Source, javax.xml.transform.Result)} call.</li>
 * </ol>
 *
 * <h2>Caveats</h2>
 * <ul>
 *   <li>A {@link SAXSource} that carries its own {@link XMLReader} is trusted as-is: the caller is expected to supply a hardened reader (via
 *       {@link XmlFactories#newSAXParserFactory()} or {@link XmlFactories#harden(XMLReader)}) in that case.</li>
 *   <li>{@link TransformerHandler} returned from {@code newTransformerHandler} is not wrapped: it processes incoming SAX events instead of reading a Source, so
 *       it has no inner Source-parsing path. A caller who pulls the inner {@link Transformer} via {@link TransformerHandler#getTransformer()} bypasses the
 *       runtime source rewrite.</li>
 * </ul>
 */
final class HardeningTransformerFactory extends DelegatingTransformerFactory {

    HardeningTransformerFactory(final SAXTransformerFactory delegate) {
        super(delegate);
    }

    @Override
    public Source getAssociatedStylesheet(final Source source, final String media, final String title, final String charset)
            throws TransformerConfigurationException {
        return super.getAssociatedStylesheet(XmlFactories.harden(source), media, title, charset);
    }

    @Override
    public Templates newTemplates(final Source source) throws TransformerConfigurationException {
        final Templates templates = super.newTemplates(XmlFactories.harden(source));
        return templates == null ? null : new HardeningTemplates(templates, getURIResolver());
    }

    @Override
    public Transformer newTransformer() throws TransformerConfigurationException {
        // Identity transformer: still parses runtime sources, so wrap it to harden Transformer.transform(Source, Result).
        final Transformer transformer = super.newTransformer();
        return transformer == null ? null : new HardeningTransformer(transformer);
    }

    @Override
    public Transformer newTransformer(final Source source) throws TransformerConfigurationException {
        final Transformer transformer = super.newTransformer(XmlFactories.harden(source));
        return transformer == null ? null : new HardeningTransformer(transformer);
    }

    @Override
    public TransformerHandler newTransformerHandler(final Source source) throws TransformerConfigurationException {
        return super.newTransformerHandler(XmlFactories.harden(source));
    }
}
