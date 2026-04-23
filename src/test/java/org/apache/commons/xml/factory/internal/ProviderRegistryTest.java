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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.xml.factory.UnsupportedXmlImplementationException;
import org.apache.commons.xml.factory.spi.XmlProvider;
import org.junit.jupiter.api.Test;

/**
 * Verifies registry dispatch without relying on any bundled provider.
 *
 * <p>Stub providers are injected through the package-private {@link ProviderRegistry#ProviderRegistry(java.util.List)} constructor so the tests exercise the
 * lookup and cache logic in isolation from the ServiceLoader-discovered classpath.</p>
 */
class ProviderRegistryTest {

    /** Stub that claims to support every non-null factory class. */
    private static final class MatchesAll implements XmlProvider {
        @Override
        public boolean supports(final Class<?> factoryClass) {
            return factoryClass != null;
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
    void providerForNullThrowsNpe() {
        final ProviderRegistry registry = new ProviderRegistry(Collections.emptyList());
        assertThrows(NullPointerException.class, () -> registry.providerFor(null));
    }

    @Test
    void providerForUnknownClassThrowsUnsupportedWithClassAttached() {
        final ProviderRegistry registry = new ProviderRegistry(Collections.emptyList());
        final UnsupportedXmlImplementationException thrown = assertThrows(
                UnsupportedXmlImplementationException.class,
                () -> registry.providerFor(String.class));
        assertSame(String.class, thrown.getUnsupportedFactoryClass());
    }

    @Test
    void providerForCachesResolvedProvider() {
        final XmlProvider stub = new MatchesAll();
        final ProviderRegistry registry = new ProviderRegistry(Collections.singletonList(stub));
        final XmlProvider first = registry.providerFor(String.class);
        final XmlProvider second = registry.providerFor(String.class);
        assertSame(first, second);
        assertSame(stub, first);
    }

    @Test
    void bundledProvidersAreConsultedInDeclaredOrder() {
        final XmlProvider winner = new MatchesAll();
        final XmlProvider shadowed = new MatchesAll();
        final ProviderRegistry registry = new ProviderRegistry(Arrays.asList(winner, shadowed));
        assertSame(winner, registry.providerFor(String.class));
    }

    @Test
    void nonMatchingBundledProviderFallsThroughToNext() {
        final XmlProvider noMatch = new MatchesNone();
        final XmlProvider match = new MatchesAll();
        final ProviderRegistry registry = new ProviderRegistry(Arrays.asList(noMatch, match));
        assertSame(match, registry.providerFor(String.class));
    }

    @Test
    void emptyBundledListWithNoServiceLoaderProvidersThrows() {
        final ProviderRegistry registry = new ProviderRegistry(Collections.emptyList());
        assertThrows(UnsupportedXmlImplementationException.class,
                () -> registry.providerFor(Integer.class));
    }
}
