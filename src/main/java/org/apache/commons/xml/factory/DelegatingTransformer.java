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

import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

/**
 * {@link Transformer} subclass that forwards every method to a wrapped delegate.
 */
class DelegatingTransformer extends Transformer {

    private final Transformer delegate;

    DelegatingTransformer(final Transformer delegate) {
        this.delegate = delegate;
    }

    @Override
    public void clearParameters() {
        delegate.clearParameters();
    }

    @Override
    public ErrorListener getErrorListener() {
        return delegate.getErrorListener();
    }

    @Override
    public Properties getOutputProperties() {
        return delegate.getOutputProperties();
    }

    @Override
    public String getOutputProperty(final String name) {
        return delegate.getOutputProperty(name);
    }

    @Override
    public Object getParameter(final String name) {
        return delegate.getParameter(name);
    }

    @Override
    public URIResolver getURIResolver() {
        return delegate.getURIResolver();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public void setErrorListener(final ErrorListener listener) {
        delegate.setErrorListener(listener);
    }

    @Override
    public void setOutputProperties(final Properties properties) {
        delegate.setOutputProperties(properties);
    }

    @Override
    public void setOutputProperty(final String name, final String value) {
        delegate.setOutputProperty(name, value);
    }

    @Override
    public void setParameter(final String name, final Object value) {
        delegate.setParameter(name, value);
    }

    @Override
    public void setURIResolver(final URIResolver resolver) {
        delegate.setURIResolver(resolver);
    }

    @Override
    public void transform(final Source xmlSource, final Result outputTarget) throws TransformerException {
        delegate.transform(xmlSource, outputTarget);
    }
}
