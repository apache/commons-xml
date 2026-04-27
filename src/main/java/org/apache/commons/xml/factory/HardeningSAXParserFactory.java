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
package org.apache.commons.xml.factory;

import java.util.function.UnaryOperator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Universal SAX wrapper that defers per-parser hardening to a supplied {@link XMLReader} hardener.
 *
 * <p>{@link SAXParserFactory} has no property API, only a feature API. Therefore, complex configuration must be performed on each new
 * {@link XMLReader} parser.</p>
 */
final class HardeningSAXParserFactory extends DelegatingSAXParserFactory {

    private final UnaryOperator<XMLReader> hardener;

    HardeningSAXParserFactory(final SAXParserFactory delegate, final UnaryOperator<XMLReader> hardener) {
        super(delegate);
        this.hardener = hardener;
    }

    @Override
    public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        final SAXParser parser = super.newSAXParser();
        hardener.apply(parser.getXMLReader());
        return parser;
    }
}
