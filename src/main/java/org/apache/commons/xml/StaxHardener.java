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

import static org.apache.commons.xml.JaxpSetters.setOptionalProperty;
import static org.apache.commons.xml.JaxpSetters.trySetProperty;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

/**
 * Capability-driven hardening for any {@link XMLInputFactory} (StAX) on the classpath.
 *
 * <p>Rather than branching on the implementation class, {@link #harden(XMLInputFactory)} consolidates the JDK Zephyr and Woodstox recipes into one pass that
 * probes which properties each factory accepts and adapts:</p>
 * <ul>
 *     <li><strong>Limits</strong>: applied best-effort by {@link Limits#tryApply(XMLInputFactory)}, which sets both the JDK and the Woodstox limit properties;
 *         each implementation honors its own and rejects the other's.</li>
 *     <li><strong>External DTD subset</strong>: skipped via Zephyr's {@value #ZEPHYR_IGNORE_EXTERNAL_DTD} (best-effort), so a DOCTYPE-only document parses
 *         without a fetch attempt instead of tripping the deny-all resolver below. Woodstox skips it through {@value #WSTX_DTD_RESOLVER} instead.</li>
 *     <li><strong>External entities</strong>: denied through resolvers, leaving the standard {@code SUPPORT_DTD} / {@code IS_SUPPORTING_EXTERNAL_ENTITIES}
 *         defaults untouched. Woodstox exposes fine-grained hooks, so when all three apply the factory is Woodstox: {@value #WSTX_DTD_RESOLVER} (empty external
 *         subset, but a thrown error on external parameter entities, which share that hook), {@value #WSTX_ENTITY_RESOLVER} (throw on declared external general
 *         entities) and {@value #WSTX_UNDECLARED_ENTITY_RESOLVER} (silently drop undeclared references left by the skipped subset). Any factory that does not
 *         accept that trio (the JDK Zephyr, or an unrecognized implementation) instead gets a single deny-all {@link Resolvers.DenyAll#XML} through
 *         {@code setXMLResolver}.</li>
 * </ul>
 */
final class StaxHardener {

    /** Zephyr property: skip external DTD subset loading entirely, so a DOCTYPE-only document parses without a fetch attempt. */
    private static final String ZEPHYR_IGNORE_EXTERNAL_DTD = "http://java.sun.com/xml/stream/properties/ignore-external-dtd";

    /** Woodstox property: resolver consulted for the external DTD subset. */
    private static final String WSTX_DTD_RESOLVER = "com.ctc.wstx.dtdResolver";

    /** Woodstox property: resolver consulted for declared external general entities. */
    private static final String WSTX_ENTITY_RESOLVER = "com.ctc.wstx.entityResolver";

    /** Woodstox property: resolver consulted for undeclared entity references. */
    private static final String WSTX_UNDECLARED_ENTITY_RESOLVER = "com.ctc.wstx.undeclaredEntityResolver";

    /**
     * Hybrid Woodstox DTD resolver: returns the empty input for the external DTD subset, throws on external parameter entities.
     *
     * <p>Woodstox calls this hook with {@code entityName == null} for the subset and {@code entityName != null} for parameter-entity expansion; that
     * discriminator is Woodstox-specific (the JDK Zephyr's {@code XMLResolver} always receives {@code null} as the 4th argument), so the resolver lives
     * here and is applied best-effort, ignored by implementations that do not recognize the property.</p>
     */
    static final XMLResolver DTD_SUBSET_ONLY = (publicID, systemID, baseURI, entityName) -> {
        if (entityName != null) {
            throw new XMLStreamException("External parameter entity '" + entityName + "' refused (publicID=" + publicID + ", systemID=" + systemID
                    + ", baseURI=" + baseURI + ")");
        }
        return Resolvers.IgnoreAll.XML.resolveEntity(publicID, systemID, baseURI, entityName);
    };

    static XMLInputFactory harden(final XMLInputFactory factory) {
        // Optional, implementation-based: JDK limit properties or Woodstox limit properties.
        Limits.tryApply(factory);
        // Optional: Zephyr's StAX equivalent of XERCES_LOAD_EXTERNAL_DTD=false skips the external DTD subset entirely.
        setOptionalProperty(factory, ZEPHYR_IGNORE_EXTERNAL_DTD, true);

        // Woodstox-specific fine-grained resolvers
        if (!(trySetProperty(factory, WSTX_DTD_RESOLVER, DTD_SUBSET_ONLY)
                && trySetProperty(factory, WSTX_ENTITY_RESOLVER, Resolvers.DenyAll.XML)
                && trySetProperty(factory, WSTX_UNDECLARED_ENTITY_RESOLVER, Resolvers.IgnoreAll.XML))) {
            // Fallback: use deny-all resolver
            factory.setXMLResolver(Resolvers.DenyAll.XML);
        }
        return factory;
    }

    private StaxHardener() {
    }
}
