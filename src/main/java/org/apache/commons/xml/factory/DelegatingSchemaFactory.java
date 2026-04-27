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

import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * {@link SchemaFactory} subclass that forwards every method to a wrapped delegate.
 */
class DelegatingSchemaFactory extends SchemaFactory {

    private final SchemaFactory delegate;

    DelegatingSchemaFactory(final SchemaFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return delegate.getErrorHandler();
    }

    @Override
    public boolean getFeature(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return delegate.getFeature(name);
    }

    @Override
    public Object getProperty(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return delegate.getProperty(name);
    }

    @Override
    public LSResourceResolver getResourceResolver() {
        return delegate.getResourceResolver();
    }

    @Override
    public boolean isSchemaLanguageSupported(final String schemaLanguage) {
        return delegate.isSchemaLanguageSupported(schemaLanguage);
    }

    @Override
    public Schema newSchema() throws SAXException {
        return delegate.newSchema();
    }

    @Override
    public Schema newSchema(final Source[] schemas) throws SAXException {
        return delegate.newSchema(schemas);
    }

    @Override
    public void setErrorHandler(final ErrorHandler errorHandler) {
        delegate.setErrorHandler(errorHandler);
    }

    @Override
    public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        delegate.setFeature(name, value);
    }

    @Override
    public void setProperty(final String name, final Object object) throws SAXNotRecognizedException, SAXNotSupportedException {
        delegate.setProperty(name, object);
    }

    @Override
    public void setResourceResolver(final LSResourceResolver resourceResolver) {
        delegate.setResourceResolver(resourceResolver);
    }
}
