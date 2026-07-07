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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;

/**
 * {@link XMLInputFactory} wrapper that keeps the {@link Resolvers.FallbackDenyXMLResolver} floors {@link StaxHardener} installs on the entity-resolution hooks
 * non-removable by the caller.
 *
 * <p>Every resolver-valued entry point ({@link #setXMLResolver(XMLResolver)}, {@code setProperty(XMLInputFactory.RESOLVER, ...)} and the Woodstox
 * {@code com.ctc.wstx.*Resolver} keys) is routed uniformly: a caller who supplies their own {@link Resolvers.FallbackDenyXMLResolver} takes control and it is
 * passed straight to the delegate; otherwise the current resolver on that hook is read, and if it is one of our floors the caller's resolver is set as its
 * {@link Resolvers.FallbackDenyXMLResolver#setDelegate delegate} (an opt-in the floor cannot be removed by), or, if the hook is empty, the caller's resolver is
 * wrapped in a fresh floor. This matters because Woodstox does not chain resolvers: when a resolver returns {@code null}, {@code DefaultInputResolver} falls
 * through to fetching the systemId URL itself, so a caller-set resolver that returns {@code null} must still land behind the floor. {@link #getXMLResolver()} and
 * {@code getProperty} report the caller's resolver unwrapped.</p>
 */
final class HardeningXMLInputFactory extends DelegatingXMLInputFactory {

    HardeningXMLInputFactory(final XMLInputFactory delegate) {
        super(delegate);
    }

    @Override
    public void setXMLResolver(final XMLResolver resolver) {
        if (resolver instanceof Resolvers.FallbackDenyXMLResolver) {
            // The caller supplies their own floor: hand it to the delegate as-is.
            super.setXMLResolver(resolver);
        } else {
            final XMLResolver current = super.getXMLResolver();
            if (current instanceof Resolvers.FallbackDenyXMLResolver) {
                ((Resolvers.FallbackDenyXMLResolver) current).setDelegate(resolver);
            } else {
                super.setXMLResolver(new Resolvers.FallbackDenyXMLResolver(resolver));
            }
        }
    }

    @Override
    public XMLResolver getXMLResolver() {
        return unwrap(super.getXMLResolver());
    }

    @Override
    public void setProperty(final String name, final Object value) {
        if (XMLInputFactory.RESOLVER.equals(name)) {
            setXMLResolver((XMLResolver) value);
        } else if (isWstxResolverProperty(name) && (value instanceof XMLResolver || value == null)) {
            setResolverProperty(name, (XMLResolver) value);
        } else {
            super.setProperty(name, value);
        }
    }

    @Override
    public Object getProperty(final String name) {
        if (XMLInputFactory.RESOLVER.equals(name)) {
            return getXMLResolver();
        }
        if (isWstxResolverProperty(name)) {
            final Object current = super.getProperty(name);
            return current instanceof XMLResolver ? unwrap((XMLResolver) current) : current;
        }
        return super.getProperty(name);
    }

    /**
     * Routes a caller-set resolver for the property {@code name} behind the floor currently installed on that hook.
     *
     * @param name     the resolver-valued property being set.
     * @param resolver the caller's resolver, or their own {@link Resolvers.FallbackDenyXMLResolver} to take control.
     */
    private void setResolverProperty(final String name, final XMLResolver resolver) {
        if (resolver instanceof Resolvers.FallbackDenyXMLResolver) {
            // The caller supplies their own floor: hand it to the delegate as-is.
            super.setProperty(name, resolver);
        } else {
            final Object current = super.getProperty(name);
            if (current instanceof Resolvers.FallbackDenyXMLResolver) {
                ((Resolvers.FallbackDenyXMLResolver) current).setDelegate(resolver);
            } else {
                super.setProperty(name, new Resolvers.FallbackDenyXMLResolver(resolver));
            }
        }
    }

    private static boolean isWstxResolverProperty(final String name) {
        return StaxHardener.WSTX_DTD_RESOLVER.equals(name)
                || StaxHardener.WSTX_ENTITY_RESOLVER.equals(name)
                || StaxHardener.WSTX_UNDECLARED_ENTITY_RESOLVER.equals(name);
    }

    private static XMLResolver unwrap(final XMLResolver resolver) {
        return resolver instanceof Resolvers.FallbackDenyXMLResolver ? ((Resolvers.FallbackDenyXMLResolver) resolver).getDelegate() : resolver;
    }
}
