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
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.xml.factory.spi.XmlProvider;

/**
 * Internal {@link XmlProvider} that routes each {@code configure} call to the bundled or {@link ServiceLoader}-discovered provider responsible for the concrete
 * JAXP factory class.
 *
 * <h2>Dispatch order</h2>
 *
 * <p>Providers are consulted in a single fixed list, built once when this class is loaded:</p>
 *
 * <ol>
 *   <li>Bundled providers in declared order: {@link StockJdkProvider}, {@link XercesProvider}, {@link WoodstoxProvider}, {@link SaxonProvider}.</li>
 *   <li>Third-party providers discovered via {@link ServiceLoader}, in the order {@link ServiceLoader} yields them.</li>
 * </ol>
 *
 * <p>Bundled providers come first so that a third-party provider on the classpath cannot intercept hardening of a factory class this library already
 * supports.</p>
 *
 * <p>If no provider matches, {@link HardeningException} is thrown.</p>
 *
 * <p>Although this class implements {@link XmlProvider}, its {@link #supports(Class)} method returns {@code false}: callers ask the composite to
 * {@code configure}, never to advertise support. The interface implementation lets {@link org.apache.commons.xml.factory.XmlFactories} treat hardening
 * uniformly without an extra abstraction.</p>
 */
public final class CompositeProvider implements XmlProvider {

    private static final CompositeProvider INSTANCE = new CompositeProvider(defaultProviders());

    private final List<XmlProvider> providers;

    /**
     * Constructs a composite over the supplied provider list.
     *
     * <p>Package-private for test use; production code should call {@link #getInstance()}.</p>
     */
    CompositeProvider(final List<XmlProvider> providers) {
        this.providers = Collections.unmodifiableList(new ArrayList<>(providers));
    }

    /**
     * Gets the process-wide composite instance.
     *
     * @return the singleton.
     */
    public static CompositeProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Stub implementation: always returns {@code false}.
     *
     * <p>The {@link XmlProvider} contract requires this method, but the class is internal and nothing in the library invokes it.</p>
     *
     * @param factoryClass ignored.
     * @return {@code false}.
     */
    @Override
    public boolean supports(final Class<?> factoryClass) {
        return false;
    }

    @Override
    public DocumentBuilderFactory configure(final DocumentBuilderFactory factory) {
        return providerFor(factory.getClass()).configure(factory);
    }

    @Override
    public SAXParserFactory configure(final SAXParserFactory factory) {
        return providerFor(factory.getClass()).configure(factory);
    }

    @Override
    public XMLInputFactory configure(final XMLInputFactory factory) {
        return providerFor(factory.getClass()).configure(factory);
    }

    @Override
    public TransformerFactory configure(final TransformerFactory factory) {
        return providerFor(factory.getClass()).configure(factory);
    }

    @Override
    public XPathFactory configure(final XPathFactory factory) {
        return providerFor(factory.getClass()).configure(factory);
    }

    @Override
    public SchemaFactory configure(final SchemaFactory factory) {
        return providerFor(factory.getClass()).configure(factory);
    }

    private XmlProvider providerFor(final Class<?> factoryClass) {
        for (final XmlProvider provider : providers) {
            if (provider.supports(factoryClass)) {
                return provider;
            }
        }
        throw new HardeningException(String.format(
                "No XmlProvider supports JAXP factory class %s. Add a supported JAXP implementation to the classpath, "
                        + "or register a custom XmlProvider via ServiceLoader.",
                factoryClass.getName()));
    }

    private static List<XmlProvider> defaultProviders() {
        final List<XmlProvider> all = new ArrayList<>();
        all.add(new StockJdkProvider());
        all.add(new XercesProvider());
        all.add(new WoodstoxProvider());
        all.add(new SaxonProvider());
        ServiceLoader.load(XmlProvider.class, CompositeProvider.class.getClassLoader()).forEach(all::add);
        return all;
    }
}
