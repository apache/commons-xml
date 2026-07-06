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

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.validation.Schema;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * {@link DocumentBuilder} that forwards every method to a wrapped delegate.
 *
 * <p>The non-abstract {@code parse(...)} overloads inherited from {@link DocumentBuilder} call {@code this.parse(InputSource)} virtually, so a subclass that
 * only overrides {@link #parse(InputSource)} and the resolver setter redirects every parse path through the delegate without overriding the overloads.</p>
 */
class DelegatingDocumentBuilder extends DocumentBuilder {

    private final DocumentBuilder delegate;

    DelegatingDocumentBuilder(final DocumentBuilder delegate) {
        this.delegate = delegate;
    }

    @Override
    public Document parse(final InputSource is) throws SAXException, IOException {
        return delegate.parse(is);
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
    public void setEntityResolver(final EntityResolver er) {
        delegate.setEntityResolver(er);
    }

    @Override
    public void setErrorHandler(final ErrorHandler eh) {
        delegate.setErrorHandler(eh);
    }

    @Override
    public Document newDocument() {
        return delegate.newDocument();
    }

    @Override
    public DOMImplementation getDOMImplementation() {
        return delegate.getDOMImplementation();
    }

    @Override
    public Schema getSchema() {
        return delegate.getSchema();
    }

    @Override
    public void reset() {
        delegate.reset();
    }
}
