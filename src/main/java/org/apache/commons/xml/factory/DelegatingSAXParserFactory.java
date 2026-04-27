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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * {@link SAXParserFactory} subclass that forwards every method to a wrapped delegate.
 */
class DelegatingSAXParserFactory extends SAXParserFactory {

    private final SAXParserFactory delegate;

    DelegatingSAXParserFactory(final SAXParserFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean getFeature(final String name) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        return delegate.getFeature(name);
    }

    @Override
    public Schema getSchema() {
        return delegate.getSchema();
    }

    @Override
    public boolean isNamespaceAware() {
        return delegate.isNamespaceAware();
    }

    @Override
    public boolean isValidating() {
        return delegate.isValidating();
    }

    @Override
    public boolean isXIncludeAware() {
        return delegate.isXIncludeAware();
    }

    @Override
    public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        return delegate.newSAXParser();
    }

    @Override
    public void setFeature(final String name, final boolean value) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        delegate.setFeature(name, value);
    }

    @Override
    public void setNamespaceAware(final boolean awareness) {
        delegate.setNamespaceAware(awareness);
    }

    @Override
    public void setSchema(final Schema schema) {
        delegate.setSchema(schema);
    }

    @Override
    public void setValidating(final boolean validating) {
        delegate.setValidating(validating);
    }

    @Override
    public void setXIncludeAware(final boolean state) {
        delegate.setXIncludeAware(state);
    }
}
