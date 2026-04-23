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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.xml.factory.UnsupportedXmlImplementationException;
import org.apache.commons.xml.factory.spi.XmlProvider;

/**
 * Internal lookup for the {@link XmlProvider} responsible for a given concrete JAXP factory class.
 *
 * <h2>Dispatch order</h2>
 *
 * <ol>
 *   <li>Bundled providers in a fixed order: {@link StockJdkProvider}, {@link XercesProvider}, {@link WoodstoxProvider}, {@link SaxonProvider}.</li>
 *   <li>Third-party providers discovered via {@link ServiceLoader}, in the order {@link ServiceLoader} yields them.</li>
 *   <li>If nothing matches, {@link UnsupportedXmlImplementationException}.</li>
 * </ol>
 *
 * <p>Bundled providers are consulted first so that a third-party provider on the classpath cannot intercept hardening of a factory class this library already
 * supports.</p>
 *
 * <p>Lookup results are cached keyed by factory class. The cache is thread-safe and only holds {@link XmlProvider} instances, never the factories
 * themselves.</p>
 */
public final class ProviderRegistry {

    private static final ProviderRegistry INSTANCE = new ProviderRegistry(bundledProviders());

    private final List<XmlProvider> bundled;

    private final ConcurrentMap<Class<?>, XmlProvider> cache = new ConcurrentHashMap<>();

    /**
     * Constructs a registry with the supplied bundled providers.
     *
     * <p>Package-private for test use; production code should call {@link #getInstance()}.</p>
     */
    ProviderRegistry(final List<XmlProvider> bundled) {
        this.bundled = Collections.unmodifiableList(new ArrayList<>(bundled));
    }

    /**
     * Gets the process-wide registry instance.
     *
     * @return the singleton.
     */
    public static ProviderRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Looks up the provider responsible for factories of the supplied concrete class.
     *
     * @param factoryClass the concrete factory class; never {@code null}.
     * @return the matching provider.
     * @throws UnsupportedXmlImplementationException if no provider matches.
     */
    public XmlProvider providerFor(final Class<?> factoryClass) {
        Objects.requireNonNull(factoryClass);
        return cache.computeIfAbsent(factoryClass, this::lookup);
    }

    private XmlProvider lookup(final Class<?> factoryClass) {
        for (final XmlProvider provider : bundled) {
            if (provider.supports(factoryClass)) {
                return provider;
            }
        }
        for (final XmlProvider provider : serviceLoaderProviders()) {
            if (provider.supports(factoryClass)) {
                return provider;
            }
        }
        throw new UnsupportedXmlImplementationException(factoryClass);
    }

    private static List<XmlProvider> bundledProviders() {
        return Arrays.asList(new StockJdkProvider(), new XercesProvider());
    }

    private static Iterable<XmlProvider> serviceLoaderProviders() {
        final ClassLoader loader = ProviderRegistry.class.getClassLoader();
        return ServiceLoader.load(XmlProvider.class, loader);
    }

}
