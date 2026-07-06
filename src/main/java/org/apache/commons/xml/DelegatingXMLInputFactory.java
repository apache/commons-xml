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

import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.EventFilter;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;

/**
 * {@link XMLInputFactory} that forwards every method to a wrapped delegate.
 */
class DelegatingXMLInputFactory extends XMLInputFactory {

    private final XMLInputFactory delegate;

    DelegatingXMLInputFactory(final XMLInputFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public XMLStreamReader createXMLStreamReader(final Reader reader) throws XMLStreamException {
        return delegate.createXMLStreamReader(reader);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(final Source source) throws XMLStreamException {
        return delegate.createXMLStreamReader(source);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(final InputStream stream) throws XMLStreamException {
        return delegate.createXMLStreamReader(stream);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(final InputStream stream, final String encoding) throws XMLStreamException {
        return delegate.createXMLStreamReader(stream, encoding);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(final String systemId, final InputStream stream) throws XMLStreamException {
        return delegate.createXMLStreamReader(systemId, stream);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(final String systemId, final Reader reader) throws XMLStreamException {
        return delegate.createXMLStreamReader(systemId, reader);
    }

    @Override
    public XMLEventReader createXMLEventReader(final Reader reader) throws XMLStreamException {
        return delegate.createXMLEventReader(reader);
    }

    @Override
    public XMLEventReader createXMLEventReader(final String systemId, final Reader reader) throws XMLStreamException {
        return delegate.createXMLEventReader(systemId, reader);
    }

    @Override
    public XMLEventReader createXMLEventReader(final XMLStreamReader reader) throws XMLStreamException {
        return delegate.createXMLEventReader(reader);
    }

    @Override
    public XMLEventReader createXMLEventReader(final Source source) throws XMLStreamException {
        return delegate.createXMLEventReader(source);
    }

    @Override
    public XMLEventReader createXMLEventReader(final InputStream stream) throws XMLStreamException {
        return delegate.createXMLEventReader(stream);
    }

    @Override
    public XMLEventReader createXMLEventReader(final InputStream stream, final String encoding) throws XMLStreamException {
        return delegate.createXMLEventReader(stream, encoding);
    }

    @Override
    public XMLEventReader createXMLEventReader(final String systemId, final InputStream stream) throws XMLStreamException {
        return delegate.createXMLEventReader(systemId, stream);
    }

    @Override
    public XMLStreamReader createFilteredReader(final XMLStreamReader reader, final StreamFilter filter) throws XMLStreamException {
        return delegate.createFilteredReader(reader, filter);
    }

    @Override
    public XMLEventReader createFilteredReader(final XMLEventReader reader, final EventFilter filter) throws XMLStreamException {
        return delegate.createFilteredReader(reader, filter);
    }

    @Override
    public XMLResolver getXMLResolver() {
        return delegate.getXMLResolver();
    }

    @Override
    public void setXMLResolver(final XMLResolver resolver) {
        delegate.setXMLResolver(resolver);
    }

    @Override
    public XMLReporter getXMLReporter() {
        return delegate.getXMLReporter();
    }

    @Override
    public void setXMLReporter(final XMLReporter reporter) {
        delegate.setXMLReporter(reporter);
    }

    @Override
    public void setProperty(final String name, final Object value) {
        delegate.setProperty(name, value);
    }

    @Override
    public Object getProperty(final String name) {
        return delegate.getProperty(name);
    }

    @Override
    public boolean isPropertySupported(final String name) {
        return delegate.isPropertySupported(name);
    }

    @Override
    public void setEventAllocator(final XMLEventAllocator allocator) {
        delegate.setEventAllocator(allocator);
    }

    @Override
    public XMLEventAllocator getEventAllocator() {
        return delegate.getEventAllocator();
    }
}
