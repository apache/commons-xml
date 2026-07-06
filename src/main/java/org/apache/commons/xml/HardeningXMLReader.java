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

import org.xml.sax.EntityResolver;
import org.xml.sax.XMLReader;

/**
 * {@link XMLReader} wrapper that keeps a {@link Resolvers.FallbackDenyResolver} floor as the reader's entity resolver, non-overridable by the caller.
 *
 * <p>The floor is installed once and stays the reader's entity resolver for the wrapper's lifetime; {@link #setEntityResolver(EntityResolver)} routes the
 * caller's resolver through {@link Resolvers.FallbackDenyResolver#setDelegate} instead of replacing it. This includes the {@code DefaultHandler} that
 * {@link javax.xml.parsers.SAXParser#parse(org.xml.sax.InputSource, org.xml.sax.helpers.DefaultHandler) SAXParser.parse(source, handler)} installs as the
 * reader's entity resolver, which would otherwise silently replace the floor. {@link #getEntityResolver()} reports the caller's resolver unwrapped.</p>
 *
 * <p>A path that needs a non-deny floor (e.g. one that also permits the external DTD subset) passes a {@link Resolvers.FallbackDenyResolver} subclass to the
 * two-argument constructor; a single stable floor instance also lets that subclass double as a {@link org.xml.sax.ext.LexicalHandler}.</p>
 */
final class HardeningXMLReader extends DelegatingXMLReader {

    private final Resolvers.FallbackDenyResolver floor;

    HardeningXMLReader(final XMLReader delegate) {
        this(delegate, new Resolvers.FallbackDenyResolver(null));
    }

    HardeningXMLReader(final XMLReader delegate, final Resolvers.FallbackDenyResolver floor) {
        super(delegate);
        this.floor = floor;
        super.setEntityResolver(floor);
    }

    @Override
    public void setEntityResolver(final EntityResolver resolver) {
        floor.setDelegate(resolver);
    }

    @Override
    public EntityResolver getEntityResolver() {
        return floor.getDelegate();
    }
}
