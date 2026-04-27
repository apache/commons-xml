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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * {@link XmlProvider} for the external Apache Xerces distribution (the {@code xerces:xercesImpl} artifact).
 *
 * <p>Factory classes live in the {@code org.apache.xerces.*} package. External Xerces does not ship a {@code TransformerFactory}, {@code XMLInputFactory} or
 * {@code XPathFactory}, so this provider only overrides DOM, SAX and Schema {@code configure} methods.</p>
 *
 * <p>Hardening recipe applied to every factory below uses the same building blocks:</p>
 * <ul>
 *     <li><strong>FSP</strong> ({@link XMLConstants#FEATURE_SECURE_PROCESSING}, set to {@code true}): enables Xerces' built-in {@code SecurityManager}, which
 *         is what carries the processing limits. Required.</li>
 *     <li><strong>{@link Limits#applyToXerces}</strong>: defense-in-depth. Xerces' {@code SecurityManager} ships its own caps, but they are looser than even
 *         JDK 8's secure values; this call pins them to the JDK 25 secure values (entity-expansion limit and {@code maxOccurs} node limit, the only two its
 *         API exposes setters for).</li>
 *     <li>
 *         <p><strong>{@code HardeningXxx} wrappers + {@link DenyAllResolver}</strong>: required. Xerces does not implement the JAXP 1.5
 *         {@code ACCESS_EXTERNAL_*} properties, so an explicit resolver installed on every parser/validator is the best way to block external
 *         entity, DTD and schema fetching, without disabling those features altogether. The wrappers exist for two reasons:</p>
 *         <ol>
 *             <li>{@link DocumentBuilderFactory} / {@link SAXParserFactory} carry no resolver, so it has to be set on each
 *             {@link DocumentBuilder} / {@link SAXParser} produced;</li>
 *             <li>Xerces' {@link Schema} does not propagate the {@link SchemaFactory}'s resolver or security manager to its
 *             {@link Validator} / {@link ValidatorHandler} products, so the wrapper re-installs both on every product.</li>
 *         </ol>
 *     </li>
 * </ul>
 *
 * <p>Must be declared {@code public} so {@link java.util.ServiceLoader} can load it from {@code META-INF/services/}.</p>
 */
public final class XercesProvider extends AbstractXmlProvider {

    /**
     * Hardened Xerces {@link DocumentBuilderFactory} wrapper.
     *
     * <p>Sets the deny-all {@link EntityResolver} on every {@link DocumentBuilder} produced; required because {@link DocumentBuilderFactory} carries no
     * resolver of its own and Xerces does not honour JAXP 1.5 {@code ACCESS_EXTERNAL_*}.</p>
     */
    private static final class HardeningDocumentBuilderFactory extends DelegatingDocumentBuilderFactory {

        private final EntityResolver resolver;

        HardeningDocumentBuilderFactory(final DocumentBuilderFactory delegate, final EntityResolver resolver) {
            super(delegate);
            this.resolver = resolver;
        }

        @Override
        public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
            final DocumentBuilder builder = super.newDocumentBuilder();
            builder.setEntityResolver(resolver);
            return builder;
        }
    }

    /**
     * Hardened Xerces {@link Schema} wrapper.
     *
     * <p>Required because Xerces' {@link Schema} does not propagate the {@link SchemaFactory}'s resolver or security manager to
     * {@link Validator} / {@link ValidatorHandler} products, so this wrapper re-installs the deny-all resolver (closes the DOCTYPE SYSTEM vector
     * inside the instance document) and pins the JDK 25 limits on every product.</p>
     */
    private static final class HardeningSchema extends Schema {

        private final Schema delegate;
        private final LSResourceResolver resolver;

        HardeningSchema(final Schema delegate, final LSResourceResolver resolver) {
            this.delegate = delegate;
            this.resolver = resolver;
        }

        @Override
        public Validator newValidator() {
            final Validator validator = delegate.newValidator();
            try {
                // Defense-in-depth: pin the validator's security manager to JDK 25 limits.
                Limits.applyToXerces(validator.getProperty(XERCES_SECURITY_MANAGER_PROPERTY));
            } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
                throw new HardeningException("Failed to read Xerces security manager from Validator", e);
            }
            // Required: re-install the deny-all resolver since SchemaFactory-level hardening does not propagate to Validators.
            validator.setResourceResolver(resolver);
            return validator;
        }

        @Override
        public ValidatorHandler newValidatorHandler() {
            final ValidatorHandler handler = delegate.newValidatorHandler();
            try {
                // Defense-in-depth: pin the handler's security manager to JDK 25 limits.
                Limits.applyToXerces(handler.getProperty(XERCES_SECURITY_MANAGER_PROPERTY));
            } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
                throw new HardeningException("Failed to read Xerces security manager from ValidatorHandler", e);
            }
            // Required: re-install the deny-all resolver since SchemaFactory-level hardening does not propagate to ValidatorHandlers.
            handler.setResourceResolver(resolver);
            return handler;
        }
    }

    /**
     * Hardened Xerces {@link SchemaFactory} wrapper.
     *
     * <p>Wraps every produced {@link Schema} in {@link HardeningSchema} so the SchemaFactory's resolver and security manager are re-installed on each
     * {@link Validator} / {@link ValidatorHandler}.</p>
     */
    private static final class HardeningSchemaFactory extends DelegatingSchemaFactory {

        HardeningSchemaFactory(final SchemaFactory delegate) {
            super(delegate);
        }

        @Override
        public Schema newSchema() throws SAXException {
            return new HardeningSchema(super.newSchema(), super.getResourceResolver());
        }

        @Override
        public Schema newSchema(final Source[] schemas) throws SAXException {
            return new HardeningSchema(super.newSchema(schemas), super.getResourceResolver());
        }
    }

    /**
     * Xerces-specific property whose value is an {@code org.apache.xerces.util.SecurityManager} instance carrying processing-limit thresholds
     */
    static final String XERCES_SECURITY_MANAGER_PROPERTY = "http://apache.org/xml/properties/security-manager";

    /** Xerces feature: load the external DTD subset for non-validating parsers. */
    private static final String XERCES_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private static Object newSecurityManager() {
        try {
            return Class.forName("org.apache.xerces.util.SecurityManager").getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new HardeningException("Failed to instantiate org.apache.xerces.util.SecurityManager; expected Xerces to be on the classpath", e);
        }
    }

    /**
     * Default constructor; invoked by {@link java.util.ServiceLoader} and the registry.
     */
    public XercesProvider() {
        super("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl",
                "org.apache.xerces.jaxp.SAXParserFactoryImpl",
                "org.apache.xerces.jaxp.SAXParserImpl$JAXPSAXParser",
                "org.apache.xerces.jaxp.validation.XMLSchemaFactory");
    }

    @Override
    public DocumentBuilderFactory configure(final DocumentBuilderFactory factory) {
        // Required: enables Xerces' built-in SecurityManager (which is what carries the limits).
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Let DOCTYPE-only documents parse silently without SSRF: skip the external DTD subset on non-validating parsers.
        setFeature(factory, XERCES_LOAD_EXTERNAL_DTD, false);
        // Defense-in-depth: install a fresh SecurityManager pinned to JDK 25 limits, replacing Xerces' built-in caps which are looser than even JDK 8.
        final Object securityManager = newSecurityManager();
        Limits.applyToXerces(securityManager);
        factory.setAttribute(XERCES_SECURITY_MANAGER_PROPERTY, securityManager);
        // Required: Xerces does not honour JAXP 1.5 ACCESS_EXTERNAL_*; the wrapper installs a deny-all resolver on every DocumentBuilder.
        return new HardeningDocumentBuilderFactory(factory, DenyAllResolver.ENTITY2);
    }

    @Override
    public SAXParserFactory configure(final SAXParserFactory factory) {
        // Required: enables Xerces' built-in SecurityManager (which is what carries the limits).
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // The remaining hardening (limits, entity resolver) lives in the XMLReader configure() because SAXParserFactory has no property API.
        return new HardeningSAXParserFactory(factory, this);
    }

    @Override
    public XMLReader configure(final XMLReader reader) {
        // Required: enables the JDK XMLSecurityManager limits on a raw reader (e.g. one Saxon picked).
        setFeature(reader, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Let DOCTYPE-only documents parse silently without SSRF: skip the external DTD subset on non-validating parsers.
        setFeature(reader, XERCES_LOAD_EXTERNAL_DTD, false);
        try {
            // Defense-in-depth: tighten the SecurityManager Xerces already installed on the reader to JDK 25 limits.
            Limits.applyToXerces(reader.getProperty(XERCES_SECURITY_MANAGER_PROPERTY));
        } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new HardeningException("Failed to read Xerces security manager from XMLReader", e);
        }
        // Required: Xerces does not honour JAXP 1.5 ACCESS_EXTERNAL_*; the deny-all resolver is the only block.
        reader.setEntityResolver(DenyAllResolver.ENTITY2);
        return reader;
    }

    @Override
    public SchemaFactory configure(final SchemaFactory factory) {
        // Required: enables Xerces' built-in SecurityManager (which is what carries the limits).
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        try {
            // Defense-in-depth: pin the factory's security manager to JDK 25 limits; Xerces' own defaults are looser than even JDK 8.
            Limits.applyToXerces(factory.getProperty(XERCES_SECURITY_MANAGER_PROPERTY));
        } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new HardeningException("Failed to read Xerces security manager from SchemaFactory", e);
        }
        // Required: Xerces does not honour JAXP 1.5 ACCESS_EXTERNAL_*; install the deny-all resolver so xs:import/xs:include/xs:redefine cannot fetch.
        factory.setResourceResolver(DenyAllResolver.LS_RESOURCE);
        // Required: Xerces' Schema does not propagate the SchemaFactory's resolver or security manager to Validator/ValidatorHandler; the wrapper re-installs.
        return new HardeningSchemaFactory(factory);
    }
}
