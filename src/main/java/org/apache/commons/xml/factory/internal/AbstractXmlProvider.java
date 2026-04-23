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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.commons.xml.factory.spi.XmlProvider;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Common scaffolding for {@link XmlProvider} implementations bundled with this library.
 *
 * <p>Subclasses pass the fully qualified class names of the factory implementations they handle to {@link #AbstractXmlProvider(String...)}. The inherited
 * {@link #supports(Class)} then returns {@code true} for exactly those classes, matching on {@link Class#getName()}.</p>
 *
 * <p>The class also exposes the JAXP {@code setFeature}/{@code setAttribute}/{@code setProperty} helpers every provider uses. Each helper wraps the checked
 * exception documented by the corresponding JAXP setter in a {@link HardeningException} whose message names the feature, property or attribute that failed.
 * Every other throwable (including {@link LinkageError} and {@link AbstractMethodError}, which typically signal a JAXP implementation on the classpath older
 * than the one this library was compiled against) is left to propagate so the caller sees the underlying problem.</p>
 */
abstract class AbstractXmlProvider implements XmlProvider {

    private static HardeningException fail(final Object factory, final String kind, final String name, final Throwable cause) {
        return new HardeningException("Failed to set " + kind + " '" + name + "' on " + factory.getClass().getName(), cause);
    }

    static void setAttribute(final DocumentBuilderFactory factory, final String attribute, final Object value) {
        try {
            factory.setAttribute(attribute, value);
        } catch (final IllegalArgumentException e) {
            throw fail(factory, "attribute", attribute, e);
        }
    }

    static void setAttribute(final TransformerFactory factory, final String attribute, final Object value) {
        try {
            factory.setAttribute(attribute, value);
        } catch (final IllegalArgumentException e) {
            throw fail(factory, "attribute", attribute, e);
        }
    }

    static void setFeature(final DocumentBuilderFactory factory, final String feature, final boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (final ParserConfigurationException e) {
            throw fail(factory, "feature", feature, e);
        }
    }

    static void setFeature(final SAXParserFactory factory, final String feature, final boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (final ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            throw fail(factory, "feature", feature, e);
        }
    }

    static void setFeature(final TransformerFactory factory, final String feature, final boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (final TransformerConfigurationException e) {
            throw fail(factory, "feature", feature, e);
        }
    }

    static void setFeature(final XPathFactory factory, final String feature, final boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (final XPathFactoryConfigurationException e) {
            throw fail(factory, "feature", feature, e);
        }
    }

    static void setFeature(final SchemaFactory factory, final String feature, final boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
            throw fail(factory, "feature", feature, e);
        }
    }

    static void setProperty(final XMLInputFactory factory, final String property, final Object value) {
        try {
            factory.setProperty(property, value);
        } catch (final IllegalArgumentException e) {
            throw fail(factory, "property", property, e);
        }
    }

    static void setProperty(final SchemaFactory factory, final String property, final Object value) {
        try {
            factory.setProperty(property, value);
        } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
            throw fail(factory, "property", property, e);
        }
    }
    private final Set<String> supported;

    protected AbstractXmlProvider(final String... supportedClassNames) {
        this.supported = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(supportedClassNames)));
    }

    @Override
    public final boolean supports(final Class<?> factoryClass) {
        return factoryClass != null && supported.contains(factoryClass.getName());
    }
}
