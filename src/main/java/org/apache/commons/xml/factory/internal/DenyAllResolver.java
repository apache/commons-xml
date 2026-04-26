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
package org.apache.commons.xml.factory.internal;

import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * Stateless resolvers that refuse every external lookup.
 *
 * <p>Four typed singleton fields are exposed, one per resolver interface; pick the one matching the API you are configuring:</p>
 * <ul>
 *     <li>{@link #LS_RESOURCE}: {@link LSResourceResolver} for {@code SchemaFactory.setResourceResolver}.</li>
 *     <li>{@link #URI}: {@link URIResolver} for {@code TransformerFactory.setURIResolver} (and the same on each {@code Transformer} it produces).</li>
 *     <li>{@link #XML}: {@link XMLResolver} for {@code XMLInputFactory.setXMLResolver}.</li>
 *     <li>{@link #ENTITY2}: {@link EntityResolver2} for {@code DocumentBuilder.setEntityResolver} and {@code XMLReader.setEntityResolver}; SAX/DOM parsers
 *         that recognise {@link EntityResolver2} pick up the extended {@code getExternalSubset} and 4-arg {@code resolveEntity} hooks automatically.</li>
 * </ul>
 *
 * <p>{@link XMLResolver} and {@link EntityResolver2} both declare a 4-arg {@code resolveEntity(String, String, String, String)} with identical erasure but
 * different parameter semantics, return types ({@link Object} vs {@link InputSource}) and throws clauses ({@link XMLStreamException} vs {@link SAXException}),
 * so they cannot coexist on the same class. That is why {@link #XML} and {@link #ENTITY2} live in separate nested types.</p>
 */
class DenyAllResolver implements LSResourceResolver, URIResolver {

    /**
     * {@link EntityResolver2} flavour: refuses every external entity referenced by a SAX or DOM parser, including the external DTD subset and the 4-arg
     * {@code resolveEntity} that lets a parser pass through the entity name and base URI.
     */
    private static final class DenyAllEntityResolver2 extends DenyAllResolver implements EntityResolver2 {

        private DenyAllEntityResolver2() {
        }

        @Override
        public InputSource getExternalSubset(final String name, final String baseURI) throws SAXException {
            throw new SAXException(forbiddenMessage("external-subset", null, null, name, baseURI));
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
     * {@link XMLResolver} flavour: refuses every external entity referenced by a StAX parser.
     */
    private static final class DenyAllXMLResolver extends DenyAllResolver implements XMLResolver {

        private DenyAllXMLResolver() {
        }

        @Override
        public Object resolveEntity(final String publicID, final String systemID, final String baseURI, final String namespace) throws XMLStreamException {
            throw new XMLStreamException(forbiddenMessage(null, namespace, publicID, systemID, baseURI));
        }
    }

    /**
     * {@link EntityResolver2} singleton; refuses every external entity lookup performed by a SAX or DOM parser.
     */
    static final EntityResolver2 ENTITY2;

    /**
     * {@link LSResourceResolver} singleton; refuses every {@code xs:import}/{@code xs:include}/{@code xs:redefine} lookup at schema-compile time.
     */
    static final LSResourceResolver LS_RESOURCE;
    /**
     * {@link URIResolver} singleton; refuses every {@code xsl:import}/{@code xsl:include}/{@code document()} lookup during XSLT compile and transform.
     */
    static final URIResolver URI;
    /**
     * {@link XMLResolver} singleton; refuses every external entity lookup performed by a StAX parser.
     */
    static final XMLResolver XML;

    static {
        DenyAllEntityResolver2 resolver2 = new DenyAllEntityResolver2();
        ENTITY2 = resolver2;
        LS_RESOURCE = resolver2;
        URI = resolver2;
        XML = new DenyAllXMLResolver();
    }

    private static String forbiddenMessage(final String type, final String namespace, final String publicId, final String systemId, final String baseURI) {
        return String.format("External resource fetch forbidden by hardening: type=%s, namespace=%s, publicId=%s, systemId=%s, baseURI=%s", type, namespace,
                publicId, systemId, baseURI);
    }

    private DenyAllResolver() {
    }

    @Override
    public Source resolve(final String href, final String base) throws TransformerException {
        throw new TransformerException(forbiddenMessage("uri", null, null, href, base));
    }

    @Override
    public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI) {
        throw new SecurityException(forbiddenMessage(type, namespaceURI, publicId, systemId, baseURI));
    }
}
