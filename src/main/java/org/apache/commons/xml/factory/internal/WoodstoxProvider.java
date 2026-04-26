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

import javax.xml.stream.XMLInputFactory;

import org.apache.commons.xml.factory.spi.XmlProvider;

/**
 * {@link XmlProvider} for the FasterXML Woodstox StAX implementation ({@code com.ctc.wstx:woodstox-core}).
 *
 * <p>Woodstox is a StAX-only library, so this provider implements only {@link #configure(XMLInputFactory)}.</p>
 *
 * <p>Hardening recipe used below:</p>
 * <ul>
 *     <li><strong>{@link Limits#applyToWoodstox}</strong>: defense-in-depth. Woodstox already enforces its own caps (see {@code ReaderConfig.DEFAULT_*}), but
 *         they are looser than the JDK 25 secure values; this call aligns them so a Woodstox-backed factory enforces the same processing limits as the JDK
 *         parsers.</li>
 *     <li><strong>{@link DenyAllResolver#XML}</strong>: required. Woodstox honours {@code IS_SUPPORTING_EXTERNAL_ENTITIES=true} and {@code SUPPORT_DTD=true} by
 *         default and resolves external entities/DTDs through whatever {@code XMLResolver} the factory carries; without an explicit deny-all resolver, those
 *         lookups would proceed.</li>
 * </ul>
 *
 * <p>Must be declared {@code public} so {@link java.util.ServiceLoader} can load it from {@code META-INF/services/}.</p>
 */
public final class WoodstoxProvider extends AbstractXmlProvider {

    /**
     * Default constructor; invoked by {@link java.util.ServiceLoader} and the registry.
     */
    public WoodstoxProvider() {
        super("com.ctc.wstx.stax.WstxInputFactory");
    }

    @Override
    public XMLInputFactory configure(final XMLInputFactory factory) {
        // Defense-in-depth: align Woodstox's built-in caps with the JDK 25 secure values; Woodstox's own defaults are functional but looser.
        Limits.applyToWoodstox(factory);
        // Required: Woodstox resolves external entities and DTDs by default; an explicit deny-all resolver is the only way to block the lookups.
        factory.setXMLResolver(DenyAllResolver.XML);
        return factory;
    }
}
