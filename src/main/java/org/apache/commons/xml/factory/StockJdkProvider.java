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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.XMLReader;

/**
 * {@link XmlProvider} for the stock JDK's JAXP implementation.
 *
 * <p>This is the internal fork of Apache Xerces, Xalan and friends shipped inside {@code com.sun.org.apache.*} and {@code com.sun.xml.internal.*} packages.</p>
 *
 * <p>Hardening recipe applied to every factory below uses the same building blocks:</p>
 * <ul>
 *     <li><strong>FODP</strong> ({@link #FEATURE_OVERRIDE_DEFAULT_PARSER}, set to {@code false}): pins the internal {@link XMLReader} lookup to the JDK's
 *         bundled SAX parser instead of {@link SAXParserFactory#newInstance()}, blocking a sysprop swap to a third-party parser. Defense-in-depth.</li>
 *     <li><strong>FSP</strong> ({@link XMLConstants#FEATURE_SECURE_PROCESSING}, set to {@code true}): switches the JDK's {@code XMLSecurityManager} into secure
 *         mode, which is what enables the JDK-side processing limits in the first place. Required.</li>
 *     <li><strong>{@code Limits.applyToJdk*}</strong>: required on {@link XMLInputFactory} (it rejects FSP); elsewhere defense-in-depth, pinning the limits to
 *         JDK 25 secure values so older JDKs do not fall back to looser defaults.</li>
 *     <li><strong>{@code ACCESS_EXTERNAL_*}</strong>: already the FSP-secure default but set to {@code ""} explicitly so a sysprop ({@code
 *         javax.xml.accessExternal*}) cannot loosen them. {@link XMLInputFactory} has no equivalent property, so the StAX path uses {@link DenyAllResolver}
 *         instead.</li>
 * </ul>
 *
 * <p>SAX hardening lives in {@link #configure(XMLReader)}: {@link SAXParserFactory} has no property API, so the {@link HardeningSAXParserFactory} wrapper
 * funnels each produced parser's {@link XMLReader} through that method.</p>
 *
 * <p>Must be declared {@code public} so {@link java.util.ServiceLoader} can load it from {@code META-INF/services/}.</p>
 */
final class StockJdkProvider extends AbstractXmlProvider {

    /**
     * {@code jdk.xml.overrideDefaultParser}: pin to the JDK's bundled SAX parser; defense-in-depth against a sysprop swap to a third-party parser.
     */
    private static final String FEATURE_OVERRIDE_DEFAULT_PARSER = "jdk.xml.overrideDefaultParser";

    /**
     * Xerces feature: load the external DTD subset for non-validating parsers.
     */
    private static final String XERCES_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    /**
     * Default constructor; invoked by {@link java.util.ServiceLoader} and the registry.
     */
    public StockJdkProvider() {
        super("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser",
                "com.sun.xml.internal.stream.XMLInputFactoryImpl",
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory");
    }

    @Override
    public DocumentBuilderFactory configure(final DocumentBuilderFactory factory) {
        // Required: enables the JDK XMLSecurityManager limits.
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Let DOCTYPE-only documents parse silently without SSRF: skip the external DTD subset on non-validating parsers.
        setFeature(factory, XERCES_LOAD_EXTERNAL_DTD, false);
        // Defense-in-depth: pin to JDK 25 limits so older JDKs do not fall back to looser secure values.
        Limits.applyToJdkDom(factory);
        // Defense-in-depth: already FSP-secure defaults, set explicitly so they are not relaxed via system property.
        setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    @Override
    public SAXParserFactory configure(final SAXParserFactory factory) {
        // Required: enables the JDK XMLSecurityManager limits.
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // The remaining hardening (limits, ACCESS_EXTERNAL_*) lives in the XMLReader configure() because SAXParserFactory has no property API.
        return new HardeningSAXParserFactory(factory, this);
    }

    @Override
    public XMLReader configure(final XMLReader reader) {
        // Required: enables the JDK XMLSecurityManager limits on a raw reader (e.g. one Saxon picked).
        setFeature(reader, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Let DOCTYPE-only documents parse silently without SSRF: skip the external DTD subset on non-validating parsers.
        setFeature(reader, XERCES_LOAD_EXTERNAL_DTD, false);
        // Defense-in-depth: pin to JDK 25 limits so older JDKs do not fall back to looser secure values.
        Limits.applyToJdkXmlReader(reader);
        // Defense-in-depth: already FSP-secure defaults, set explicitly so they are not relaxed via system property.
        setProperty(reader, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setProperty(reader, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return reader;
    }

    @Override
    public XMLInputFactory configure(final XMLInputFactory factory) {
        // Required: XMLInputFactory rejects FSP, so the limits below are the only way to enable JDK XMLSecurityManager caps on the StAX path.
        Limits.applyToJdkStax(factory);
        // Required: XMLInputFactory has no ACCESS_EXTERNAL_* either; an explicit deny-all resolver is the only way to block external entity fetching.
        factory.setXMLResolver(DenyAllResolver.XML);
        return factory;
    }

    @Override
    public TransformerFactory configure(final TransformerFactory factory) {
        // Defense-in-depth: pin to the JDK's bundled SAX parser; see FEATURE_OVERRIDE_DEFAULT_PARSER.
        setFeature(factory, FEATURE_OVERRIDE_DEFAULT_PARSER, false);
        // Required: enables the JDK XMLSecurityManager limits used by the internal SAX parser.
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Defense-in-depth: pin to JDK 25 limits so older JDKs do not fall back to looser secure values.
        Limits.applyToJdkTransformer(factory);
        // Defense-in-depth: already FSP-secure defaults, set explicitly so they are not relaxed via system property.
        setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return factory;
    }

    @Override
    public XPathFactory configure(final XPathFactory factory) {
        // Defense-in-depth: pin to the JDK's bundled SAX parser; see FEATURE_OVERRIDE_DEFAULT_PARSER.
        setFeature(factory, FEATURE_OVERRIDE_DEFAULT_PARSER, false);
        // Required: enables JDK XPath limits; XPathFactory has no property API for finer control.
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory;
    }

    @Override
    public SchemaFactory configure(final SchemaFactory factory) {
        // Defense-in-depth: pin to the JDK's bundled SAX parser; see FEATURE_OVERRIDE_DEFAULT_PARSER.
        setFeature(factory, FEATURE_OVERRIDE_DEFAULT_PARSER, false);
        // Required: enables the JDK XMLSecurityManager limits; propagates to Validator and ValidatorHandler.
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Defense-in-depth: pin to JDK 25 limits so older JDKs do not fall back to looser secure values.
        Limits.applyToJdkSchema(factory);
        // Defense-in-depth: already FSP-secure defaults, set explicitly so they are not relaxed via system property.
        setProperty(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setProperty(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }
}
