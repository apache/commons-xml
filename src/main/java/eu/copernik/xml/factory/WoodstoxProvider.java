/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

/**
 * Hardening recipe for the FasterXML Woodstox StAX implementation ({@code com.ctc.wstx:woodstox-core}).
 *
 * <p>Woodstox is a StAX-only library, so this class only handles {@link XMLInputFactory}.</p>
 *
 * <p>Hardening recipe used below:</p>
 * <ul>
 *     <li><strong>{@link Limits#applyToWoodstox}</strong>: defense-in-depth. Woodstox already enforces its own caps (see {@code ReaderConfig.DEFAULT_*}), but
 *         they are looser than the JDK 25 secure values; this call aligns them so a Woodstox-backed factory enforces the same processing limits as the JDK
 *         parsers.</li>
 *     <li><strong>Three resolver hooks</strong>: required. Woodstox honours {@code IS_SUPPORTING_EXTERNAL_ENTITIES=true} and {@code SUPPORT_DTD=true} by
 *         default and routes lookups through three Woodstox-specific resolver properties; the hardened factory installs:
 *         <ul>
 *             <li>{@code com.ctc.wstx.dtdResolver} = {@link #DTD_SUBSET_ONLY}: returns an empty input for the external DTD subset so DOCTYPE-only
 *                 documents parse silently, but throws on external parameter entities (which share this hook). The {@code entityName} 4th argument is
 *                 the discriminator: {@code null} for the subset, the entity name for parameter entities (see {@code DefaultInputResolver} and
 *                 {@code ValidatingStreamReader.findDtdExtSubset}).</li>
 *             <li>{@code com.ctc.wstx.entityResolver} = {@link Resolvers.DenyAll#XML}: throws on declared external general entities.</li>
 *             <li>{@code com.ctc.wstx.undeclaredEntityResolver} = {@link Resolvers.IgnoreAll#XML}: silently drops references to entities the parser has not
 *                 seen declared, matching the SAX path's behaviour.</li>
 *         </ul>
 *     </li>
 * </ul>
 */
final class WoodstoxProvider {

    /** Woodstox property: resolver consulted for the external DTD subset and for external parameter entities. */
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
     * here rather than in {@link Resolvers}.</p>
     */
    static final XMLResolver DTD_SUBSET_ONLY = (publicID, systemID, baseURI, entityName) -> {
        if (entityName != null) {
            throw new XMLStreamException("External parameter entity '" + entityName + "' refused (publicID=" + publicID + ", systemID=" + systemID
                    + ", baseURI=" + baseURI + ")");
        }
        return Resolvers.IgnoreAll.XML.resolveEntity(publicID, systemID, baseURI, entityName);
    };

    static XMLInputFactory configure(final XMLInputFactory factory) {
        // Defense-in-depth: align Woodstox's built-in caps with the JDK 25 secure values; Woodstox's own defaults are functional but looser.
        Limits.applyToWoodstox(factory);
        // Required: empty external subset, throw on external parameter entities.
        factory.setProperty(WSTX_DTD_RESOLVER, DTD_SUBSET_ONLY);
        // Required: throw on declared external general entities.
        factory.setProperty(WSTX_ENTITY_RESOLVER, Resolvers.DenyAll.XML);
        // Required: silently drop undeclared entity references, matching the SAX path's tolerance.
        factory.setProperty(WSTX_UNDECLARED_ENTITY_RESOLVER, Resolvers.IgnoreAll.XML);
        return factory;
    }

    private WoodstoxProvider() {
    }
}
