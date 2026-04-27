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

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.XMLFilter;

/**
 * {@link SAXTransformerFactory} subclass that forwards every method to a wrapped delegate.
 */
class DelegatingTransformerFactory extends SAXTransformerFactory {

    private final SAXTransformerFactory delegate;

    DelegatingTransformerFactory(final SAXTransformerFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public Source getAssociatedStylesheet(final Source source, final String media, final String title, final String charset)
            throws TransformerConfigurationException {
        return delegate.getAssociatedStylesheet(source, media, title, charset);
    }

    @Override
    public Object getAttribute(final String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public ErrorListener getErrorListener() {
        return delegate.getErrorListener();
    }

    @Override
    public boolean getFeature(final String name) {
        return delegate.getFeature(name);
    }

    @Override
    public URIResolver getURIResolver() {
        return delegate.getURIResolver();
    }

    @Override
    public Templates newTemplates(final Source source) throws TransformerConfigurationException {
        return delegate.newTemplates(source);
    }

    @Override
    public TemplatesHandler newTemplatesHandler() throws TransformerConfigurationException {
        return delegate.newTemplatesHandler();
    }

    @Override
    public Transformer newTransformer() throws TransformerConfigurationException {
        return delegate.newTransformer();
    }

    @Override
    public Transformer newTransformer(final Source source) throws TransformerConfigurationException {
        return delegate.newTransformer(source);
    }

    @Override
    public TransformerHandler newTransformerHandler() throws TransformerConfigurationException {
        return delegate.newTransformerHandler();
    }

    @Override
    public TransformerHandler newTransformerHandler(final Source source) throws TransformerConfigurationException {
        return delegate.newTransformerHandler(source);
    }

    @Override
    public TransformerHandler newTransformerHandler(final Templates templates) throws TransformerConfigurationException {
        return delegate.newTransformerHandler(templates);
    }

    @Override
    public XMLFilter newXMLFilter(final Source source) throws TransformerConfigurationException {
        return delegate.newXMLFilter(source);
    }

    @Override
    public XMLFilter newXMLFilter(final Templates templates) throws TransformerConfigurationException {
        return delegate.newXMLFilter(templates);
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void setErrorListener(final ErrorListener listener) {
        delegate.setErrorListener(listener);
    }

    @Override
    public void setFeature(final String name, final boolean value) throws TransformerConfigurationException {
        delegate.setFeature(name, value);
    }

    @Override
    public void setURIResolver(final URIResolver resolver) {
        delegate.setURIResolver(resolver);
    }
}
