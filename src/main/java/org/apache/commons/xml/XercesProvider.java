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

import static org.apache.commons.xml.JaxpSetters.setFeature;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Hardening recipes for the external Apache Xerces distribution (the {@code xerces:xercesImpl} artifact).
 *
 * <p>Factory classes live in the {@code org.apache.xerces.*} package. External Xerces does not ship a {@code TransformerFactory}, {@code XMLInputFactory} or
 * {@code XPathFactory}, so this class only handles SAX factories; Schema hardening is capability-driven across all implementations and lives in
 * {@link HardeningSchemaFactory}, DOM hardening in {@link DocumentBuilderHardener}.</p>
 *
 * <p>Hardening recipe applied to every factory below uses the same building blocks:</p>
 * <ul>
 *     <li><strong>FSP</strong> ({@link XMLConstants#FEATURE_SECURE_PROCESSING}, set to {@code true}): enables Xerces' built-in {@code SecurityManager}, which
 *         is what carries the processing limits. Required.</li>
 *     <li><strong>{@link Limits#applyToXerces}</strong>: defense-in-depth. Xerces' {@code SecurityManager} ships its own caps, but they are looser than even
 *         JDK 8's secure values; this call pins them to the JDK 25 secure values (entity-expansion limit and {@code maxOccurs} node limit, the only two its
 *         API exposes setters for).</li>
 *     <li><strong>{@link Resolvers.DenyAll#ENTITY2}</strong>: required. Xerces does not implement the JAXP 1.5 {@code ACCESS_EXTERNAL_*} properties, so an
 *         explicit deny-all resolver installed on every reader is the best way to block external entity and DTD fetching, without disabling those features
 *         altogether. {@link SAXParserFactory} carries no resolver, so it has to be set on each {@link SAXParser} produced.</li>
 * </ul>
 */
final class XercesProvider {

    /**
     * Xerces-specific property whose value is an {@code org.apache.xerces.util.SecurityManager} instance carrying processing-limit thresholds
     */
    static final String XERCES_SECURITY_MANAGER_PROPERTY = "http://apache.org/xml/properties/security-manager";

    /** Xerces feature: load the external DTD subset for non-validating parsers. */
    private static final String XERCES_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    static SAXParserFactory configure(final SAXParserFactory factory) {
        // Required: enables Xerces' built-in SecurityManager (which is what carries the limits).
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Useful: namespaces should be recognized by default
        factory.setNamespaceAware(true);
        // The remaining hardening (limits, entity resolver) lives in the XMLReader configure() because SAXParserFactory has no property API.
        return new HardeningSAXParserFactory(factory, XercesProvider::configure);
    }

    static XMLReader configure(final XMLReader reader) {
        // Required: enables the JDK XMLSecurityManager limits on a raw reader (e.g. one Saxon picked).
        setFeature(reader, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Let DOCTYPE-only documents parse silently without SSRF: skip the external DTD subset on non-validating parsers.
        setFeature(reader, XERCES_LOAD_EXTERNAL_DTD, false);
        try {
            // Defense-in-depth: tighten the SecurityManager Xerces already installed on the reader to JDK 25 limits.
            Limits.applyToXerces(reader.getProperty(XERCES_SECURITY_MANAGER_PROPERTY));
        } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new HardeningException("Failed to read Xerces security manager from XMLReader", e);
        }
        // Required: Xerces does not honour JAXP 1.5 ACCESS_EXTERNAL_*; the deny-all resolver is the only block.
        reader.setEntityResolver(Resolvers.DenyAll.ENTITY2);
        return reader;
    }

    private XercesProvider() {
    }
}
