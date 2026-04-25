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
package org.apache.commons.xml.factory.attacks;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import javax.xml.xpath.XPathFactory;

import org.apache.commons.xml.factory.internal.CompositeProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Exercises Saxon's XPath 3.1 URI-fetching functions and verifies that a hardened Saxon {@link XPathFactory} refuses them before any I/O.
 *
 * <p>Saxon's XPath 3.1 engine ships four functions that open arbitrary URIs during expression evaluation:
 * {@code doc()}, {@code collection()}, {@code unparsed-text()} and {@code json-doc()}. None of them require a context node; each is triggered purely by the
 * string URI they receive. They are <em>not</em> classified as extension functions in Saxon's vocabulary, so disabling {@code ALLOW_EXTERNAL_FUNCTIONS} is
 * not sufficient to block them; a complete hardening has to close the URI-resolution path too.</p>
 *
 * <p>The JDK's built-in XPathFactory and Xalan have no equivalent vectors.</p>
 *
 * <p>Skipped when Saxon is not on the classpath; only runs under the {@code test-saxon} surefire profile.</p>
 */
class SaxonXPathExternalCallsTest {

    private static final String SAXON_XPATH_FACTORY_CLASS = "net.sf.saxon.xpath.XPathFactoryImpl";

    @BeforeAll
    static void requireSaxon() {
        try {
            Class.forName(SAXON_XPATH_FACTORY_CLASS);
        } catch (final ClassNotFoundException e) {
            assumeTrue(false, "Saxon not on the classpath; skipping Saxon XPath external-call tests");
        }
    }

    @Test
    void xpathBlocksCollectionCall() {
        AttackTestSupport.assertXPathBlocks(hardenedSaxonXPathFactory(), "collection('" + Payloads.UNREACHABLE_HTTP + "')");
    }

    @Test
    void xpathBlocksDocCall() {
        AttackTestSupport.assertXPathBlocks(hardenedSaxonXPathFactory(), "doc('" + Payloads.UNREACHABLE_HTTP + "')");
    }

    @Test
    void xpathBlocksJsonDocCall() {
        AttackTestSupport.assertXPathBlocks(hardenedSaxonXPathFactory(), "json-doc('" + Payloads.UNREACHABLE_HTTP + "')");
    }

    @Test
    void xpathBlocksUnparsedTextCall() {
        AttackTestSupport.assertXPathBlocks(hardenedSaxonXPathFactory(), "unparsed-text('" + Payloads.UNREACHABLE_HTTP + "')");
    }

    /**
     * Instantiates Saxon's {@code net.sf.saxon.xpath.XPathFactoryImpl} reflectively. Saxon 12.9 does not ship a {@code META-INF/services} entry for
     * {@code javax.xml.xpath.XPathFactory}, so {@code XPathFactory.newInstance(Saxon-URI)} cannot find it; direct instantiation bypasses that lookup.
     */
    private static XPathFactory hardenedSaxonXPathFactory() {
        final XPathFactory xpf;
        try {
            xpf = (XPathFactory) Class.forName(SAXON_XPATH_FACTORY_CLASS).getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("Cannot instantiate " + SAXON_XPATH_FACTORY_CLASS, e);
        }
        return CompositeProvider.getInstance().configure(xpf);
    }
}
