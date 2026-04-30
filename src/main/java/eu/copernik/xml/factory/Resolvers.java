/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * Stateless resolver singletons that fix the outcome of every external lookup.
 *
 * <p>Two flavours are exposed, each as a typed singleton field per resolver interface:</p>
 * <ul>
 *     <li>{@link DenyAll} refuses every lookup with an exception. Use this on schema/XSLT compile paths and on parser entity hooks where any external fetch is
 *         a hardening violation.</li>
 *     <li>{@link IgnoreAll} returns an empty input. Use this on Woodstox's DTD-subset and undeclared-entity hooks where the parse must continue with no
 *         replacement content.</li>
 * </ul>
 *
 * <p>{@link XMLResolver} and {@link EntityResolver2} both declare a 4-arg {@code resolveEntity(String, String, String, String)} with identical erasure but
 * different parameter semantics, return types ({@link Object} vs {@link InputSource}) and throws clauses ({@link XMLStreamException} vs {@link SAXException}),
 * so they cannot coexist on the same class. Each flavour therefore exposes its {@code XMLResolver} and {@code EntityResolver2} singletons separately.</p>
 */
final class Resolvers {

    /**
     * Refuses every external resource lookup with an exception.
     *
     * <p>The single-method resolvers ({@link LSResourceResolver}, {@link URIResolver}, {@link XMLResolver}) are exposed as lambdas; {@link EntityResolver2}
     * declares three methods, so it lives in a private nested class.</p>
     */
    static final class DenyAll {

        /**
         * {@link EntityResolver2}: refuses every external entity lookup performed by a SAX or DOM parser.
         */
        private static final class DenyAllEntityResolver2 implements EntityResolver2 {

            private DenyAllEntityResolver2() {
            }

            @Override
            public InputSource getExternalSubset(final String name, final String baseURI) {
                // Canonical EntityResolver2 "no synthetic subset" signal; matches the behaviour of an absent resolver. Blocking happens in resolveEntity below.
                return null;
            }

            @Override
            public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException {
                throw new SAXException(forbiddenMessage(null, null, publicId, systemId, null));
            }

            @Override
            public InputSource resolveEntity(final String name, final String publicId, final String baseURI, final String systemId) throws SAXException {
                throw new SAXException(forbiddenMessage(name, null, publicId, systemId, baseURI));
            }
        }

        /**
         * Refuses every external entity lookup performed by a SAX or DOM parser, including the external DTD subset.
         */
        static final EntityResolver2 ENTITY2 = new DenyAllEntityResolver2();

        /**
         * Refuses every {@code xs:import}/{@code xs:include}/{@code xs:redefine} lookup at schema-compile time.
         */
        static final LSResourceResolver LS_RESOURCE = (type, namespaceURI, publicId, systemId, baseURI) -> {
            throw new SecurityException(forbiddenMessage(type, namespaceURI, publicId, systemId, baseURI));
        };

        /**
         * Refuses every {@code xsl:import}/{@code xsl:include}/{@code document()} lookup during XSLT compile and transform.
         */
        static final URIResolver URI = (href, base) -> {
            throw new TransformerException(forbiddenMessage("uri", null, null, href, base));
        };

        /**
         * Refuses every external entity lookup performed by a StAX parser.
         */
        static final XMLResolver XML = (publicID, systemID, baseURI, namespace) -> {
            throw new XMLStreamException(forbiddenMessage(null, namespace, publicID, systemID, baseURI));
        };

        private DenyAll() {
        }
    }

    /**
     * Returns an empty input for every external resource lookup so the parse can continue without replacement content.
     *
     * <p>Only an {@link XMLResolver} flavour is exposed: schema and XSLT compile paths must always deny imports, and SAX/DOM use the deny-all hooks plus
     * {@link AndroidProvider}'s subset-aware resolver where needed.</p>
     */
    static final class IgnoreAll {

        /**
         * Empty {@link ByteArrayInputStream} shared across every call. {@code read()} on a zero-length array always returns {@code -1}, so reusing the
         * instance is safe even though the type is technically stateful.
         */
        private static final InputStream EMPTY = new ByteArrayInputStream(new byte[0]);

        /**
         * Returns an empty input for every external entity lookup performed by a StAX parser.
         */
        static final XMLResolver XML = (publicID, systemID, baseURI, namespace) -> EMPTY;

        private IgnoreAll() {
        }
    }

    private static String forbiddenMessage(final String type, final String namespace, final String publicId, final String systemId, final String baseURI) {
        return String.format("External resource fetch forbidden by hardening: type=%s, namespace=%s, publicId=%s, systemId=%s, baseURI=%s", type, namespace,
                publicId, systemId, baseURI);
    }

    private Resolvers() {
    }
}
