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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.XMLReader;

/**
 * Common scaffolding for {@link XmlProvider} implementations bundled with this library.
 *
 * <p>Subclasses pass the fully qualified class names of the factory implementations they handle to {@link #AbstractXmlProvider(String...)}. The inherited
 * {@link #supports(Class)} then returns {@code true} for exactly those classes, matching on {@link Class#getName()}.</p>
 */
abstract class AbstractXmlProvider implements XmlProvider {

    /** Action that may throw any exception; used to share a single try/catch around every JAXP setter. */
    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private static void apply(final Object factory, final String kind, final String name, final ThrowingAction action) {
        try {
            action.run();
        } catch (final Exception e) {
            throw new HardeningException("Failed to set " + kind + " '" + name + "' on " + factory.getClass().getName(), e);
        }
    }

    static void setAttribute(final DocumentBuilderFactory factory, final String attribute, final Object value) {
        apply(factory, "attribute", attribute, () -> factory.setAttribute(attribute, value));
    }

    static void setAttribute(final TransformerFactory factory, final String attribute, final Object value) {
        apply(factory, "attribute", attribute, () -> factory.setAttribute(attribute, value));
    }

    static void setFeature(final DocumentBuilderFactory factory, final String feature, final boolean value) {
        apply(factory, "feature", feature, () -> factory.setFeature(feature, value));
    }

    static void setFeature(final SAXParserFactory factory, final String feature, final boolean value) {
        apply(factory, "feature", feature, () -> factory.setFeature(feature, value));
    }

    static void setFeature(final TransformerFactory factory, final String feature, final boolean value) {
        apply(factory, "feature", feature, () -> factory.setFeature(feature, value));
    }

    static void setFeature(final XPathFactory factory, final String feature, final boolean value) {
        apply(factory, "feature", feature, () -> factory.setFeature(feature, value));
    }

    static void setFeature(final SchemaFactory factory, final String feature, final boolean value) {
        apply(factory, "feature", feature, () -> factory.setFeature(feature, value));
    }

    static void setFeature(final Validator validator, final String feature, final boolean value) {
        apply(validator, "feature", feature, () -> validator.setFeature(feature, value));
    }

    static void setFeature(final ValidatorHandler handler, final String feature, final boolean value) {
        apply(handler, "feature", feature, () -> handler.setFeature(feature, value));
    }

    static void setProperty(final XMLInputFactory factory, final String property, final Object value) {
        apply(factory, "property", property, () -> factory.setProperty(property, value));
    }

    static void setProperty(final SAXParser parser, final String property, final Object value) {
        apply(parser, "property", property, () -> parser.setProperty(property, value));
    }

    static void setProperty(final XMLReader reader, final String property, final Object value) {
        apply(reader, "property", property, () -> reader.setProperty(property, value));
    }

    static void setFeature(final XMLReader reader, final String feature, final boolean value) {
        apply(reader, "feature", feature, () -> reader.setFeature(feature, value));
    }

    static void setProperty(final SchemaFactory factory, final String property, final Object value) {
        apply(factory, "property", property, () -> factory.setProperty(property, value));
    }

    static void setProperty(final Validator validator, final String property, final Object value) {
        apply(validator, "property", property, () -> validator.setProperty(property, value));
    }

    static void setProperty(final ValidatorHandler handler, final String property, final Object value) {
        apply(handler, "property", property, () -> handler.setProperty(property, value));
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
