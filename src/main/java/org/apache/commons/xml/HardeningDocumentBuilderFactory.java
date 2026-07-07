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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.EntityResolver;

/**
 * {@link DocumentBuilderFactory} wrapper that keeps a deny-all {@link EntityResolver} floor on every {@link DocumentBuilder} produced.
 *
 * <p>Wraps each produced builder in a {@link HardeningDocumentBuilder}; required when the underlying factory carries no resolver of its own and does not honor
 * JAXP 1.5 {@code ACCESS_EXTERNAL_*} (e.g. the external Xerces distribution). A caller-set resolver is routed through the floor rather than replacing it. Kept
 * as a standalone wrapper so any hardener can reuse the floor.</p>
 */
final class HardeningDocumentBuilderFactory extends DelegatingDocumentBuilderFactory {

    HardeningDocumentBuilderFactory(final DocumentBuilderFactory delegate) {
        super(delegate);
    }

    @Override
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return new HardeningDocumentBuilder(super.newDocumentBuilder());
    }
}
