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

import javax.xml.parsers.DocumentBuilder;

import org.xml.sax.EntityResolver;

/**
 * {@link DocumentBuilder} wrapper that keeps a deny-all {@link EntityResolver} as a non-overridable floor.
 *
 * <p>A caller-set resolver is sandwiched inside a {@link Resolvers.FallbackDenyResolver} instead of replacing the deny-all one, so an external lookup the
 * caller's resolver does not satisfy is denied rather than fetched. {@link #reset()} re-establishes the bare deny-all floor, matching the just-constructed
 * state.</p>
 */
final class HardeningDocumentBuilder extends DelegatingDocumentBuilder {

    private final Resolvers.FallbackDenyResolver floor = new Resolvers.FallbackDenyResolver(null);

    HardeningDocumentBuilder(final DocumentBuilder delegate) {
        super(delegate);
        super.setEntityResolver(floor);
    }

    @Override
    public void setEntityResolver(final EntityResolver resolver) {
        floor.setDelegate(resolver);
    }

    @Override
    public void reset() {
        super.reset();
        floor.setDelegate(null);
        super.setEntityResolver(floor);
    }
}
