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
package org.apache.commons.xml.factory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks that a document carrying a {@code <!DOCTYPE root SYSTEM "...">} declaration but no body references parses successfully through the hardened
 * factories.
 *
 * <p>The SYSTEM identifier points at a deliberately bogus URL ({@code http://invalid.example.invalid/...}) under the IANA-reserved {@code .invalid} TLD: any
 * attempt to fetch it would raise a network error long before the test could complete, so a passing test proves the parser did not even try. The hardening
 * contract being verified is "skip the external DTD silently when nothing in the body needs it" rather than "reject every DOCTYPE".</p>
 *
 * <p>Coverage is limited to DOM and SAX. StAX is intentionally omitted: JDK Zephyr ({@code com.sun.xml.internal.stream}) routes the external DTD subset load
 * through the same {@link javax.xml.stream.XMLResolver} it uses for general entities, with no equivalent of the Xerces
 * {@code nonvalidating/load-external-dtd} switch, so a deny-all resolver fires on the DOCTYPE itself. Hardened StAX parsers therefore still throw on a
 * SYSTEM-bearing DOCTYPE.</p>
 */
class DoctypeOnlyTest {

    private static final String BOGUS_DTD_URL = "http://invalid.example.invalid/bogus.dtd";

    private static String payload() {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE root SYSTEM \"" + BOGUS_DTD_URL + "\">\n"
                + "<root>hello</root>\n";
    }

    @Test
    @Tag("dom")
    void hardenedDomParses() {
        AttackTestSupport.assertDomParses(payload());
    }

    @Test
    @Tag("sax")
    void hardenedSaxParses() {
        AttackTestSupport.assertSaxParses(payload());
    }

    @Test
    @Tag("sax")
    void hardenedXmlReaderParses() {
        AttackTestSupport.assertXmlReaderParses(payload());
    }
}
