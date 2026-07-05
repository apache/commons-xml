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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Universal SAX factory wrapper that funnels every produced parser through {@link SAXParserHardener#hardenReader(XMLReader)}.
 *
 * <p>{@link SAXParserFactory} exposes only a feature API and no property API, so the per-parse hardening (limits, entity blocking, implementation-specific
 * fixups) has to run on each {@link XMLReader} the factory produces. This wrapper returns a {@link HardeningSAXParser}, which applies that hardening lazily to
 * both the SAX 2 {@link XMLReader} and the SAX 1 {@link org.xml.sax.Parser} it exposes.</p>
 */
final class HardeningSAXParserFactory extends DelegatingSAXParserFactory {

    HardeningSAXParserFactory(final SAXParserFactory delegate) {
        super(delegate);
    }

    @Override
    public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        return new HardeningSAXParser(super.newSAXParser());
    }
}
