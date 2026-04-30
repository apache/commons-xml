/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.LexicalHandler;

/**
 * Hardening recipes for Android's Apache Harmony based DOM and SAX implementation.
 *
 * <p>Factory classes live in the {@code org.apache.harmony.xml.parsers.*} package, but DOM and SAX are backed by two different engines:</p>
 * <ul>
 *     <li>{@link SAXParserFactory} produces {@code org.apache.harmony.xml.ExpatReader}, a SAX wrapper around the platform's native Expat parser.</li>
 *     <li>{@link DocumentBuilderFactory} produces {@code DocumentBuilderImpl}, which builds the DOM tree on top of {@code com.android.org.kxml2.io.KXmlParser}
 *         (a kxml2 pull parser).</li>
 * </ul>
 *
 * <p>What the SAX/Expat surface exposes:</p>
 * <ul>
 *     <li>SAX features: only {@code namespaces}, {@code namespace-prefixes}, {@code string-interning}, {@code validation},
 *         {@code external-general-entities} and {@code external-parameter-entities}. The last three are read-only and cannot be enabled.
 *         Setting both {@code namespaces} and {@code namespace-prefixes} triggers an automatic exception at parse time.</li>
 *     <li>SAX properties: only {@code lexical-handler}.</li>
 *     <li>{@link XMLConstants#FEATURE_SECURE_PROCESSING} and JAXP 1.5 {@code ACCESS_EXTERNAL_*} are not recognised.</li>
 *     <li>Entity expansion: native libexpat enforces a built-in Billion Laughs check (compiled-in activation threshold and amplification factor), so internal
 *         entity expansion is already bounded below us.</li>
 *     <li>Every external fetch (DTD subset, DOCTYPE {@code SYSTEM}, general/parameter entity) flows through the 2-arg {@link EntityResolver#resolveEntity}.
 *     Without a resolver external fetches are ignored.</li>
 * </ul>
 *
 * <p>What the DOM/KXmlParser surface exposes:</p>
 * <ul>
 *     <li>{@link DocumentBuilderFactory#setFeature} only recognises {@code namespaces} and {@code validation}.</li>
 *     <li>{@link DocumentBuilderFactory#setAttribute} always throws {@code IllegalArgumentException}.</li>
 *     <li>{@link XMLConstants#FEATURE_SECURE_PROCESSING} and JAXP 1.5 {@code ACCESS_EXTERNAL_*} are not recognised.</li>
 *     <li>Entity expansion: KXmlParser does not support user-defined entities and they are silently dropped.</li>
 * </ul>
 *
 * <p>The SAX path installs a {@link DtdAwareDenyResolver} as both {@link EntityResolver} and {@link LexicalHandler}: it allows the external subset to load
 * silently (so a DOCTYPE that names an external DTD but does not use it parses) and throws on every external general or parameter entity reference.</p>
 */
final class AndroidProvider {

    /**
     * Resolver that denies every external resource lookup, except the external DTD subset declared by the DOCTYPE
     *
     * <p>Merely <em>declaring</em> an external subset does not cause the parse to throw.</p>
     */
    private static final class DtdAwareDenyResolver extends DefaultHandler2 {

        private static String forbiddenMessage(final String publicId, final String systemId) {
            return String.format("External Entity: failed to read external entity (publicId='%s', systemId='%s'); external entity access is denied.",
                    publicId, systemId);
        }

        private String dtdPublicId;
        private String dtdSystemId;
        private boolean inDtd;

        @Override
        public void endDTD() {
            inDtd = false;
        }

        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException {
            if (inDtd && Objects.equals(publicId, dtdPublicId) && Objects.equals(systemId, dtdSystemId)) {
                return null;
            }
            throw new SAXException(forbiddenMessage(publicId, systemId));
        }

        @Override
        public void startDTD(final String name, final String publicId, final String systemId) {
            inDtd = true;
            dtdPublicId = publicId;
            dtdSystemId = systemId;
        }
    }

    /**
     * {@link SAXParser} wrapper whose {@link #getXMLReader()} returns a {@link GuardedXMLReader}.
     */
    private static final class GuardedSAXParser extends DelegatingSAXParser {

        private final XMLReader guardedReader;

        GuardedSAXParser(final SAXParser delegate, final XMLReader guardedReader) {
            super(delegate);
            this.guardedReader = guardedReader;
        }

        @Override
        public XMLReader getXMLReader() {
            return guardedReader;
        }
    }

    /**
     * {@link SAXParserFactory} wrapper that produces {@link GuardedSAXParser}s.
     */
    private static final class GuardedSAXParserFactory extends DelegatingSAXParserFactory {

        GuardedSAXParserFactory(final SAXParserFactory delegate) {
            super(delegate);
        }

        @Override
        public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
            final SAXParser parser = super.newSAXParser();
            return new GuardedSAXParser(parser, configure(parser.getXMLReader()));
        }
    }

    /**
     * {@link XMLReader} wrapper that surfaces ExpatReader's conflicting-feature error at {@code setFeature} time rather than at {@code parse} time.
     *
     * <p>Android's {@code ExpatReader.parse()} throws {@link SAXNotSupportedException} when {@code namespaces} and {@code namespace-prefixes} are both
     * enabled. Reporting the error at configuration time lets consumers, such as Apache Xalan's identity transformer, catch the exception and still parse
     * the document. Without the wrapper, parsing fails.</p>
     */
    static final class GuardedXMLReader extends DelegatingXMLReader {

        GuardedXMLReader(final XMLReader delegate) {
            super(delegate);
        }

        @Override
        public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
            if (value
                    && (NAMESPACE_PREFIXES_FEATURE.equals(name) && super.getFeature(NAMESPACES_FEATURE)
                    || NAMESPACES_FEATURE.equals(name) && super.getFeature(NAMESPACE_PREFIXES_FEATURE))) {
                throw new SAXNotSupportedException("ExpatReader cannot have both '" + NAMESPACES_FEATURE + "' and '" + NAMESPACE_PREFIXES_FEATURE + "' " +
                        "enabled simultaneously");
            }
            super.setFeature(name, value);
        }
    }

    private static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";

    private static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";

    private static final String NAMESPACE_PREFIXES_FEATURE = "http://xml.org/sax/features/namespace-prefixes";

    static DocumentBuilderFactory configure(final DocumentBuilderFactory factory) {
        return factory;
    }

    static SAXParserFactory configure(final SAXParserFactory factory) {
        return new GuardedSAXParserFactory(factory);
    }

    static XMLReader configure(final XMLReader reader) {
        if (reader instanceof GuardedXMLReader) {
            return reader;
        }
        final DtdAwareDenyResolver resolver = new DtdAwareDenyResolver();
        reader.setEntityResolver(resolver);
        try {
            reader.setProperty(LEXICAL_HANDLER_PROPERTY, resolver);
        } catch (final SAXException e) {
            // ExpatReader recognises the lexical-handler property; if a future replacement does not, fall through and lose subset-vs-entity discrimination.
        }
        return new GuardedXMLReader(reader);
    }

    private AndroidProvider() {
    }
}