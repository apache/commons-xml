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
package org.apache.commons.xml.factory.spi;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

/**
 * Service-provider interface implemented by a single JAXP implementation's hardening logic.
 *
 * <p>Each provider understands one or more concrete factory classes and applies the implementation-specific feature and property settings needed to turn that
 * factory into a safe-by-default parser.</p>
 *
 * <h2>Dispatch</h2>
 *
 * <p>Providers are consulted in a fixed order by {@link org.apache.commons.xml.factory.XmlFactories}. The first provider whose {@link #supports(Class)} method
 * returns {@code true} for a vanilla factory's concrete class is asked to configure that factory. Bundled providers are consulted before
 * {@link java.util.ServiceLoader}-discovered providers, so a user-supplied provider cannot shadow hardening for a JAXP implementation that this library ships
 * support for.</p>
 *
 * <h2>Implementor contract</h2>
 *
 * <ul>
 *   <li>{@link #supports(Class)} must return {@code true} <em>only</em> for factory classes for which this provider can implement <em>every</em> relevant
 *       {@code configure} method.</li>
 *   <li>Each {@code configure} method must either complete successfully with the factory fully hardened, or throw. Returning a partially-configured factory is
 *       a bug; callers rely on the contract that a factory observed after a successful {@code configure} call has every relevant feature locked down. Wrap any
 *       checked exception (for example {@link javax.xml.parsers.ParserConfigurationException}, {@link javax.xml.stream.XMLStreamException},
 *       {@link javax.xml.transform.TransformerConfigurationException}) in an unchecked exception whose message names the feature or property that failed.</li>
 *   <li>Providers need not be thread-safe themselves, but they must not hold mutable global state. Each {@code configure} call operates on a caller-supplied
 *       factory.</li>
 * </ul>
 *
 * <p>The default {@code configure} implementations throw {@link UnsupportedOperationException}. A provider should override only the methods corresponding to
 * factory types it actually supports.</p>
 */
public interface XmlProvider {

    /**
     * Indicates whether this provider can harden a factory of the given concrete class.
     *
     * <p>Implementations typically match on {@link Class#getName()}; they must not return {@code true} for a class they cannot fully configure.</p>
     *
     * @param factoryClass the concrete factory class to test; never {@code null}.
     * @return {@code true} if this provider is responsible for factories of this class.
     */
    boolean supports(Class<?> factoryClass);

    /**
     * Hardens a {@link DocumentBuilderFactory}.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}; providers that match DOM factories must override.</p>
     *
     * @param factory the vanilla factory to harden in place; never {@code null}.
     */
    default void configure(final DocumentBuilderFactory factory) {
        throw new UnsupportedOperationException();
    }

    /**
     * Hardens a {@link SAXParserFactory}.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}; providers that match SAX factories must override.</p>
     *
     * @param factory the vanilla factory to harden in place; never {@code null}.
     */
    default void configure(final SAXParserFactory factory) {
        throw new UnsupportedOperationException();
    }

    /**
     * Hardens an {@link XMLInputFactory}.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}; providers that match StAX input factories must override.</p>
     *
     * @param factory the vanilla factory to harden in place; never {@code null}.
     */
    default void configure(final XMLInputFactory factory) {
        throw new UnsupportedOperationException();
    }

    /**
     * Hardens a {@link TransformerFactory}.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}; providers that match TrAX factories must override.</p>
     *
     * @param factory the vanilla factory to harden in place; never {@code null}.
     */
    default void configure(final TransformerFactory factory) {
        throw new UnsupportedOperationException();
    }

    /**
     * Hardens an {@link XPathFactory}.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}; providers that match XPath factories must override.</p>
     *
     * @param factory the vanilla factory to harden in place; never {@code null}.
     */
    default void configure(final XPathFactory factory) {
        throw new UnsupportedOperationException();
    }

    /**
     * Hardens a {@link SchemaFactory}.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}; providers that match Schema factories must override.</p>
     *
     * @param factory the vanilla factory to harden in place; never {@code null}.
     */
    default void configure(final SchemaFactory factory) {
        throw new UnsupportedOperationException();
    }
}
