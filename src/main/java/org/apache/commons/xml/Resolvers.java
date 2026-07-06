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

package org.apache.commons.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.EntityResolver2;

/**
 * Policy resolvers that fix the outcome of every external lookup.
 *
 * <p>Two flavours are exposed:</p>
 * <ul>
 *     <li><strong>Stateless singletons</strong> ({@link DenyAll}, {@link IgnoreAll}) fix the outcome unconditionally: {@link DenyAll} throws, {@link IgnoreAll}
 *         returns an empty input. Used on StAX hooks where any external fetch is a hardening violation ({@link DenyAll#XML}) or must continue with no
 *         replacement content ({@link IgnoreAll#XML}, Woodstox's DTD-subset and undeclared-entity hooks).</li>
 *     <li><strong>Fallback-deny floors</strong> ({@link FallbackDenyResolver} for {@code EntityResolver}, {@link FallbackDenyLSResourceResolver} for
 *         {@link LSResourceResolver}, {@link FallbackDenyURIResolver} for {@link URIResolver}) wrap an optional caller-supplied resolver: they consult it first
 *         and deny whatever it does not resolve. The hardened wrappers install one and route a caller-set resolver through {@code setDelegate} rather than
 *         replacing it, so a caller can opt specific resources in without removing the deny-all floor.</li>
 * </ul>
 */
final class Resolvers {

    /**
     * Refuses every external resource lookup with an exception. All members are single-method resolvers exposed as lambdas.
     */
    static final class DenyAll {

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
     * <p>Only an {@link XMLResolver} flavour is exposed: schema and XSLT compile paths must always deny imports, and SAX/DOM use {@link FallbackDenyResolver}.</p>
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

    /**
     * Entity resolver that consults an optional caller-supplied resolver and denies (throws) whatever the caller does not resolve.
     *
     * <p>This is the entity-resolution counterpart of the JAXP 1.5 {@code ACCESS_EXTERNAL_*} properties: a non-overridable floor. The hardened DOM and SAX
     * wrappers install one of these and, when the caller sets their own {@link EntityResolver}, route it through {@link #setDelegate} rather than letting it
     * replace the floor. A caller therefore opts a specific resource in by returning a non-{@code null} {@link InputSource} from their resolver; anything they
     * leave unresolved (a {@code null} return, or no caller resolver at all) goes to {@link #onUnresolved}, which denies by default.</p>
     *
     * <p>It extends {@link DefaultHandler2} so it is also usable as a {@link org.xml.sax.ext.LexicalHandler} (see {@code SAXParserHardener}'s Android subclass,
     * which needs {@code startDTD}/{@code endDTD}); {@link #getExternalSubset} therefore inherits the {@code DefaultHandler2} "no synthetic subset" default. Only
     * {@link #resolveEntity(String, String, String, String) resolveEntity} (the actual external fetch) reaches the deny fallback.</p>
     */
    static class FallbackDenyResolver extends DefaultHandler2 {

        /**
         * Caller-supplied resolver consulted first, or {@code null} for a pure deny-all floor.
         */
        private EntityResolver delegate;

        FallbackDenyResolver(final EntityResolver delegate) {
            this.delegate = delegate;
        }

        /**
         * Replaces the caller resolver consulted ahead of the floor; lets a single floor instance back successive {@code setEntityResolver} calls.
         *
         * @param delegate the caller-supplied resolver, or {@code null} for a pure deny-all floor.
         */
        final void setDelegate(final EntityResolver delegate) {
            this.delegate = delegate;
        }

        final EntityResolver getDelegate() {
            return delegate;
        }

        @Override
        public final InputSource resolveEntity(final String publicId, final String systemId) throws SAXException, IOException {
            return resolveEntity(null, publicId, null, systemId);
        }

        @Override
        public final InputSource resolveEntity(final String name, final String publicId, final String baseURI, final String systemId)
                throws SAXException, IOException {
            final InputSource resolved = resolveWithDelegate(name, publicId, baseURI, systemId);
            return resolved != null ? resolved : onUnresolved(name, publicId, baseURI, systemId);
        }

        /**
         * Outcome when neither the caller delegate nor this resolver provides the entity. Denies by default; a subclass may permit specific lookups (e.g. the
         * external DTD subset) by returning {@code null} or an {@link InputSource} instead of calling {@code super}.
         *
         * @param name     the entity name, or {@code null} on the 2-arg resolution path.
         * @param publicId the public identifier, or {@code null} if none.
         * @param baseURI  the base URI for relative resolution, or {@code null}.
         * @param systemId the system identifier of the unresolved entity.
         * @return an {@link InputSource} to permit the lookup, or {@code null} to skip it silently; the default implementation never returns normally.
         * @throws SAXException to deny the lookup (the default behavior).
         * @throws IOException  if a subclass opens a stream that fails.
         */
        protected InputSource onUnresolved(final String name, final String publicId, final String baseURI, final String systemId)
                throws SAXException, IOException {
            throw new SAXException(forbiddenMessage(name, null, publicId, systemId, baseURI));
        }

        private InputSource resolveWithDelegate(final String name, final String publicId, final String baseURI, final String systemId)
                throws SAXException, IOException {
            if (delegate instanceof EntityResolver2) {
                return ((EntityResolver2) delegate).resolveEntity(name, publicId, baseURI, systemId);
            }
            return delegate != null ? delegate.resolveEntity(publicId, systemId) : null;
        }
    }

    /**
     * {@link LSResourceResolver} floor: consults an optional caller-supplied resolver and denies (throws) whatever the caller does not resolve.
     *
     * <p>The schema-compile counterpart of {@link FallbackDenyResolver}. The hardened {@link javax.xml.validation.SchemaFactory}, {@link
     * javax.xml.validation.Validator} and {@link javax.xml.validation.ValidatorHandler} wrappers install one of these and route a caller-set resolver through
     * {@link #setDelegate} rather than letting it replace the floor. A caller opts a specific resource in by returning a non-{@code null} {@link LSInput};
     * anything left unresolved is denied.</p>
     */
    static final class FallbackDenyLSResourceResolver implements LSResourceResolver {

        private LSResourceResolver delegate;

        FallbackDenyLSResourceResolver(final LSResourceResolver delegate) {
            this.delegate = delegate;
        }

        void setDelegate(final LSResourceResolver delegate) {
            this.delegate = delegate;
        }

        LSResourceResolver getDelegate() {
            return delegate;
        }

        @Override
        public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI) {
            final LSInput resolved = delegate != null ? delegate.resolveResource(type, namespaceURI, publicId, systemId, baseURI) : null;
            if (resolved != null) {
                return resolved;
            }
            throw new SecurityException(forbiddenMessage(type, namespaceURI, publicId, systemId, baseURI));
        }
    }

    /**
     * {@link URIResolver} floor: consults an optional caller-supplied resolver and denies (throws) whatever the caller does not resolve.
     *
     * <p>The XSLT counterpart of {@link FallbackDenyResolver}, guarding {@code xsl:import}/{@code xsl:include} at compile time and {@code document()} at
     * transform time. The hardened {@link javax.xml.transform.TransformerFactory} and {@link javax.xml.transform.Transformer} wrappers install one of these and
     * route a caller-set resolver through {@link #setDelegate} rather than letting it replace the floor. A caller opts a specific URI in by returning a
     * non-{@code null} {@link Source}; anything left unresolved is denied.</p>
     */
    static final class FallbackDenyURIResolver implements URIResolver {

        private URIResolver delegate;

        FallbackDenyURIResolver(final URIResolver delegate) {
            this.delegate = delegate;
        }

        void setDelegate(final URIResolver delegate) {
            this.delegate = delegate;
        }

        URIResolver getDelegate() {
            return delegate;
        }

        @Override
        public Source resolve(final String href, final String base) throws TransformerException {
            final Source resolved = delegate != null ? delegate.resolve(href, base) : null;
            if (resolved != null) {
                return resolved;
            }
            throw new TransformerException(forbiddenMessage("uri", null, null, href, base));
        }
    }

    private static String forbiddenMessage(final String type, final String namespace, final String publicId, final String systemId, final String baseURI) {
        return String.format("External resource fetch forbidden by hardening: type=%s, namespace=%s, publicId=%s, systemId=%s, baseURI=%s", type, namespace,
                publicId, systemId, baseURI);
    }

    private Resolvers() {
    }
}
