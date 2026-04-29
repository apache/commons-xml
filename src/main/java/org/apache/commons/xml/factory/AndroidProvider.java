/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.xml.factory;

import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * Hardening recipes for Android's Apache Harmony based DOM and SAX implementation.
 *
 * <p>Factory classes live in the {@code org.apache.harmony.xml.parsers.*} package, but DOM and SAX are backed by two different engines:</p>
 * <ul>
 *     <li>{@link SAXParserFactory} produces {@code org.apache.harmony.xml.ExpatReader}, a SAX wrapper around the platform's native Expat parser.</li>
 *     <li>{@link DocumentBuilderFactory} produces {@code DocumentBuilderImpl}, which builds the DOM tree on top of {@code com.android.org.kxml2.io.KXmlParser}
 *         (a kxml2 pull parser); it does not use Expat at all.</li>
 * </ul>
 *
 * <p>What the SAX/Expat surface exposes:</p>
 * <ul>
 *     <li>SAX features: only {@code namespaces}, {@code namespace-prefixes}, {@code string-interning}, {@code validation},
 *         {@code external-general-entities} and {@code external-parameter-entities}. The last three are read-only and cannot be enabled.</li>
 *     <li>SAX properties: only {@code lexical-handler}.</li>
 *     <li>{@link XMLConstants#FEATURE_SECURE_PROCESSING} and JAXP 1.5 {@code ACCESS_EXTERNAL_*} are not recognised.</li>
 *     <li>Entity expansion: native libexpat enforces a built-in Billion Laughs check (compiled-in activation threshold and amplification factor), so internal
 *         entity expansion is already bounded below us.</li>
 *     <li>Every external fetch (DTD subset, DOCTYPE {@code SYSTEM}, general/parameter entity) flows through the 2-arg
 *         {@link EntityResolver#resolveEntity}; {@code EntityResolver2.getExternalSubset} is never called.</li>
 * </ul>
 *
 * <p>What the DOM/KXmlParser surface exposes:</p>
 * <ul>
 *     <li>{@link DocumentBuilderFactory#setFeature} only recognises {@code namespaces} and {@code validation}.</li>
 *     <li>{@link DocumentBuilderFactory#setAttribute} always throws {@code IllegalArgumentException}.</li>
 *     <li>{@link XMLConstants#FEATURE_SECURE_PROCESSING} and JAXP 1.5 {@code ACCESS_EXTERNAL_*} are not recognised.</li>
 *     <li>KXmlParser already drops external DTD subsets and external general entities silently (no HTTP fetch), and aborts on recursive internal entity
 *     declarations.</li>
 * </ul>
 *
 * <p>The SAX path installs a {@link DtdAwareDenyResolver} as both {@link EntityResolver} and {@link LexicalHandler}: it allows the external subset to load
 * silently (so a DOCTYPE that names an external DTD but does not use it parses) and throws on every external general or parameter entity reference. The DOM
 * path needs no resolver: KXmlParser silently drops external DTDs and external general entities and does not consult the resolver.</p>
 */
final class AndroidProvider {

    /**
     * Resolver that denies every external resource lookup, except the external DTD subset declared by the DOCTYPE
     *
     * <p>Merely <em>declaring</em> an external subset does not cause the parse to throw.</p>
     */
    private static final class DtdAwareDenyResolver implements EntityResolver, LexicalHandler {

        private boolean inDtd;
        private String dtdPublicId;
        private String dtdSystemId;

        private static String forbiddenMessage(final String publicId, final String systemId) {
            return String.format("External Entity: failed to read external entity (publicId='%s', systemId='%s'); external entity access is denied.",
                    publicId, systemId);
        }

        @Override
        public void comment(final char[] ch, final int start, final int length) {
            // no-op
        }

        @Override
        public void endCDATA() {
            // no-op
        }

        @Override
        public void endDTD() {
            inDtd = false;
        }

        @Override
        public void endEntity(final String name) {
            // no-op
        }

        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException {
            if (inDtd && Objects.equals(publicId, dtdPublicId) && Objects.equals(systemId, dtdSystemId)) {
                return null;
            }
            throw new SAXException(forbiddenMessage(publicId, systemId));
        }

        @Override
        public void startCDATA() {
            // no-op
        }

        @Override
        public void startDTD(final String name, final String publicId, final String systemId) {
            inDtd = true;
            dtdPublicId = publicId;
            dtdSystemId = systemId;
        }

        @Override
        public void startEntity(final String name) {
            // no-op
        }
    }

    private static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";

    static DocumentBuilderFactory configure(final DocumentBuilderFactory factory) {
        return factory;
    }

    static SAXParserFactory configure(final SAXParserFactory factory) {
        return new HardeningSAXParserFactory(factory, AndroidProvider::configure);
    }

    static XMLReader configure(final XMLReader reader) {
        final DtdAwareDenyResolver resolver = new DtdAwareDenyResolver();
        reader.setEntityResolver(resolver);
        try {
            reader.setProperty(LEXICAL_HANDLER_PROPERTY, resolver);
        } catch (final SAXException e) {
            // ExpatReader recognises the lexical-handler property; if a future replacement does not, fall through and lose subset-vs-entity discrimination.
        }
        return reader;
    }

    private AndroidProvider() {
    }
}