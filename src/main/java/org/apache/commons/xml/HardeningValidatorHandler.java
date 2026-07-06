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

import javax.xml.validation.ValidatorHandler;

import org.w3c.dom.ls.LSResourceResolver;

/**
 * {@link ValidatorHandler} wrapper that keeps a deny-all {@link LSResourceResolver} floor a caller cannot remove.
 *
 * <p>Blocks {@code xsi:schemaLocation} resolution during SAX-driven validation. A caller-set resolver is routed through a {@link
 * Resolvers.FallbackDenyLSResourceResolver} rather than replacing the floor, so a schema the caller does not resolve is denied instead of fetched.</p>
 */
final class HardeningValidatorHandler extends DelegatingValidatorHandler {

    private final Resolvers.FallbackDenyLSResourceResolver floor = new Resolvers.FallbackDenyLSResourceResolver(null);

    HardeningValidatorHandler(final ValidatorHandler delegate) {
        super(delegate);
        super.setResourceResolver(floor);
    }

    @Override
    public void setResourceResolver(final LSResourceResolver resourceResolver) {
        floor.setDelegate(resourceResolver);
    }

    @Override
    public LSResourceResolver getResourceResolver() {
        return floor.getDelegate();
    }
}
