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

import javax.xml.stream.XMLInputFactory;

import org.apache.commons.xml.factory.spi.XmlProvider;

/**
 * {@link XmlProvider} for the FasterXML Woodstox StAX implementation ({@code com.ctc.wstx:woodstox-core}).
 *
 * <p>Woodstox is a StAX-only library, so this provider implements only {@link #configure(XMLInputFactory)}.</p>
 *
 * <p>Hardening uses the JAXP-spec StAX properties.</p>
 *
 * <p>Must be declared {@code public} so {@link java.util.ServiceLoader} can load it from {@code META-INF/services/}.</p>
 */
public final class WoodstoxProvider extends AbstractXmlProvider {

    /**
     * Default constructor; invoked by {@link java.util.ServiceLoader} and the registry.
     */
    public WoodstoxProvider() {
        super("com.ctc.wstx.stax.WstxInputFactory");
    }

    @Override
    public XMLInputFactory configure(final XMLInputFactory factory) {
        setProperty(factory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        setProperty(factory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        return factory;
    }
}
