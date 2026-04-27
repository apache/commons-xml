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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;

/**
 * Verifies composite dispatch without relying on any bundled provider.
 *
 * <p>Stub providers are injected through the package-private {@link CompositeProvider#CompositeProvider(java.util.List)} constructor so the tests exercise the
 * lookup, dispatch and cache logic in isolation from the ServiceLoader-discovered classpath.</p>
 */
class CompositeProviderTest {

    /**
     * Stand-in factory whose concrete class is not matched by any bundled provider on the test classpath.
     */
    private static final class FakeFactory extends DocumentBuilderFactory {

        @Override
        public DocumentBuilder newDocumentBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAttribute(final String name, final Object value) {
            // no-op
        }

        @Override
        public Object getAttribute(final String name) {
            return null;
        }

        @Override
        public void setFeature(final String name, final boolean value) {
            // no-op
        }

        @Override
        public boolean getFeature(final String name) {
            return false;
        }
    }

    /** Stub that records every {@code supports} and {@code configure} invocation and claims to support all classes. */
    private static final class CountingMatchAll implements XmlProvider {

        int supportsCount;

        int configureCount;

        @Override
        public boolean supports(final Class<?> factoryClass) {
            supportsCount++;
            return true;
        }

        @Override
        public DocumentBuilderFactory configure(final DocumentBuilderFactory factory) {
            configureCount++;
            return factory;
        }
    }

    /** Stub that matches nothing. */
    private static final class MatchesNone implements XmlProvider {
        @Override
        public boolean supports(final Class<?> factoryClass) {
            return false;
        }
    }

    @Test
    void unknownFactoryClassThrowsHardeningException() {
        final CompositeProvider composite = new CompositeProvider(Collections.emptyList());
        final FakeFactory factory = new FakeFactory();
        final HardeningException thrown = assertThrows(HardeningException.class, () -> composite.configure(factory));
        assertTrue(thrown.getMessage().contains(FakeFactory.class.getName()),
                "Exception message must name the unsupported class: " + thrown.getMessage());
    }

    @Test
    void scansProvidersOnEveryInvocation() {
        final CountingMatchAll provider = new CountingMatchAll();
        final CompositeProvider composite = new CompositeProvider(Collections.singletonList(provider));
        final FakeFactory factory = new FakeFactory();
        composite.configure(factory);
        composite.configure(factory);
        assertEquals(2, provider.supportsCount, "supports() must be invoked once per configure call (no caching)");
        assertEquals(2, provider.configureCount);
    }

    @Test
    void bundledProvidersAreConsultedInDeclaredOrder() {
        final CountingMatchAll winner = new CountingMatchAll();
        final CountingMatchAll shadowed = new CountingMatchAll();
        final CompositeProvider composite = new CompositeProvider(Arrays.asList(winner, shadowed));
        final FakeFactory factory = new FakeFactory();
        assertSame(factory, composite.configure(factory));
        assertEquals(1, winner.configureCount);
        assertEquals(0, shadowed.configureCount);
    }

    @Test
    void nonMatchingBundledProviderFallsThroughToNext() {
        final XmlProvider noMatch = new MatchesNone();
        final CountingMatchAll match = new CountingMatchAll();
        final CompositeProvider composite = new CompositeProvider(Arrays.asList(noMatch, match));
        composite.configure(new FakeFactory());
        assertEquals(1, match.configureCount);
    }

    @Test
    void supportsAlwaysReturnsFalse() {
        final CompositeProvider composite = new CompositeProvider(Collections.singletonList(new CountingMatchAll()));
        assertFalse(composite.supports(FakeFactory.class));
    }
}
