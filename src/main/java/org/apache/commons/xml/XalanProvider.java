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
import javax.xml.xpath.XPathFactory;

/**
 * Hardening recipes for the external Apache Xalan distribution (the {@code xalan:xalan} artifact).
 *
 * <p>Factory classes live under the {@code org.apache.xalan.*} and {@code org.apache.xpath.*} packages. Xalan ships only TrAX and XPath; its DOM, SAX, StAX and
 * Schema needs are served by whatever JDK or external Xerces is on the classpath.</p>
 *
 * <p>This class only handles XPath. TrAX hardening is capability-driven across all implementations and lives in {@link TransformerFactoryHardener}, which applies
 * to Xalan the same building blocks it applies to XSLTC (FSP, a deny-all {@link javax.xml.transform.URIResolver} for {@code xsl:import}/{@code xsl:include} and
 * {@code document()}, and a {@link HardeningTransformerFactory} wrapper so source parsing runs through an {@link XmlFactories}-hardened reader).</p>
 */
final class XalanProvider {

    static XPathFactory configure(final XPathFactory factory) {
        // Required: enables Xalan's secure-processing mode; XPathFactory has no property API for finer control.
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory;
    }

    private XalanProvider() {
    }
}
