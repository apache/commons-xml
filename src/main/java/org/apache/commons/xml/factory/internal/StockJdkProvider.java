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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.xml.factory.spi.XmlProvider;

/**
 * {@link XmlProvider} for the stock JDK's JAXP implementation.
 *
 * <p>This is the internal fork of Apache Xerces, Xalan and friends shipped inside {@code com.sun.org.apache.*} and {@code com.sun.xml.internal.*} packages.</p>
 *
 * <p>Must be declared {@code public} so {@link java.util.ServiceLoader} can load it from {@code META-INF/services/}.</p>
 */
public final class StockJdkProvider extends AbstractXmlProvider {

    private static final String FEATURE_DISALLOW_DOCTYPE =
            "http://apache.org/xml/features/disallow-doctype-decl";

    /**
     * Pins TransformerFactory, XPathFactory and SchemaFactory to the JDK's bundled JAXP parser.
     *
     * <p>With this set to {@code false} the factories use {@code SAXParserFactoryImpl} directly when they need an internal {@link org.xml.sax.XMLReader};
     * with {@code true} they would route through {@link javax.xml.parsers.SAXParserFactory#newInstance()} and pick up whatever third-party parser is on the
     * classpath.</p>
     */
    private static final String FEATURE_OVERRIDE_DEFAULT_PARSER = "jdk.xml.overrideDefaultParser";

    /** Default constructor; invoked by {@link java.util.ServiceLoader} and the registry. */
    public StockJdkProvider() {
        super("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
                "com.sun.xml.internal.stream.XMLInputFactoryImpl",
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory");
    }

    @Override
    public DocumentBuilderFactory configure(final DocumentBuilderFactory factory) {
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeature(factory, FEATURE_DISALLOW_DOCTYPE, true);
        factory.setXIncludeAware(false);
        factory.setValidating(false);
        // Defence-in-depth: disallow-doctype-decl and validating=false already block external resolution on the normal parse path. These properties only fire
        // if the caller re-enables DOCTYPE, turns validation on, attaches a Schema, or registers an EntityResolver that returns null instead of throwing; the
        // default resolver then consults them and refuses the fetch before any I/O.
        //   - ACCESS_EXTERNAL_DTD:    protocols allowed for external DTDs declared by DOCTYPE SYSTEM/PUBLIC.
        //   - ACCESS_EXTERNAL_SCHEMA: protocols allowed for xsi:schemaLocation hints and for xs:import/xs:include/xs:redefine in a validating Schema.
        setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    @Override
    public SAXParserFactory configure(final SAXParserFactory factory) {
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeature(factory, FEATURE_DISALLOW_DOCTYPE, true);
        factory.setXIncludeAware(false);
        factory.setValidating(false);
        // No ACCESS_EXTERNAL_* here: the JAXP SAXParserFactory API exposes neither setAttribute nor setProperty, so these properties cannot be applied at
        // factory level. disallow-doctype-decl and validating=false already block external resolution on the normal parse path; callers who modify the posture
        // should set parser.setProperty(XMLConstants.ACCESS_EXTERNAL_{DTD,SCHEMA}, "") on each SAXParser returned by newSAXParser().
        return factory;
    }

    @Override
    public XMLInputFactory configure(final XMLInputFactory factory) {
        setProperty(factory, XMLInputFactory.SUPPORT_DTD, false);
        setProperty(factory, XMLInputFactory.IS_VALIDATING, false);
        return factory;
    }

    @Override
    public TransformerFactory configure(final TransformerFactory factory) {
        // Pin the internal SAX parser to the JDK's bundled implementation; see FEATURE_OVERRIDE_DEFAULT_PARSER.
        setFeature(factory, FEATURE_OVERRIDE_DEFAULT_PARSER, false);
        // The following properties are used to configure the SAX parsers
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return factory;
    }

    @Override
    public XPathFactory configure(final XPathFactory factory) {
        // Pin the internal SAX parser to the JDK's bundled implementation; see FEATURE_OVERRIDE_DEFAULT_PARSER.
        setFeature(factory, FEATURE_OVERRIDE_DEFAULT_PARSER, false);
        // The following properties are used to configure the SAX parsers
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory;
    }

    @Override
    public SchemaFactory configure(final SchemaFactory factory) {
        // Pin the internal SAX parser to the JDK's bundled implementation; see FEATURE_OVERRIDE_DEFAULT_PARSER.
        setFeature(factory, FEATURE_OVERRIDE_DEFAULT_PARSER, false);
        // The following properties are used to configure the SAX parsers
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setProperty(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setProperty(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }
}
