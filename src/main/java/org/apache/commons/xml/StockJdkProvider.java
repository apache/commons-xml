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
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.XMLReader;

/**
 * Hardening recipes for the stock JDK's JAXP implementation.
 *
 * <p>This is the internal fork of Apache Xerces, Xalan and friends shipped inside {@code com.sun.org.apache.*} and {@code com.sun.xml.internal.*} packages.</p>
 *
 * <p>Hardening recipe applied to every factory below uses the same building blocks:</p>
 * <ul>
 *     <li><strong>FODP</strong> ({@link #FEATURE_OVERRIDE_DEFAULT_PARSER}, set to {@code false}): pins the internal {@link XMLReader} lookup to the JDK's
 *         bundled SAX parser instead of {@link SAXParserFactory#newInstance()}, blocking a sysprop swap to a third-party parser. Defense-in-depth.</li>
 *     <li><strong>FSP</strong> ({@link XMLConstants#FEATURE_SECURE_PROCESSING}, set to {@code true}): switches the JDK's {@code XMLSecurityManager} into secure
 *         mode, which is what enables the JDK-side processing limits in the first place. Required.</li>
 *     <li><strong>{@code Limits.applyToJdk*}</strong>: defense-in-depth, pinning the limits to JDK 25 secure values so older JDKs do not fall back to looser
 *         defaults.</li>
 *     <li><strong>{@code ACCESS_EXTERNAL_*}</strong>: already the FSP-secure default but set to {@code ""} explicitly so a sysprop ({@code
 *         javax.xml.accessExternal*}) cannot loosen them.</li>
 * </ul>
 */
final class StockJdkProvider {

    /**
     * {@code jdk.xml.overrideDefaultParser}: pin to the JDK's bundled SAX parser; defense-in-depth against a sysprop swap to a third-party parser.
     */
    private static final String FEATURE_OVERRIDE_DEFAULT_PARSER = "jdk.xml.overrideDefaultParser";

    static XPathFactory configure(final XPathFactory factory) {
        // Defense-in-depth: pin to the JDK's bundled SAX parser; see FEATURE_OVERRIDE_DEFAULT_PARSER.
        setFeature(factory, FEATURE_OVERRIDE_DEFAULT_PARSER, false);
        // Required: enables JDK XPath limits; XPathFactory has no property API for finer control.
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory;
    }

    private StockJdkProvider() {
    }
}
