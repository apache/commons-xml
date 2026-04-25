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
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.xml.factory.spi.XmlProvider;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * {@link XmlProvider} for the external Apache Xerces distribution (the {@code xerces:xercesImpl} artifact).
 *
 * <p>Factory classes live in the {@code org.apache.xerces.*} package.</p>
 *
 * <p>External Xerces does not ship a {@code TransformerFactory}, {@code XMLInputFactory} or {@code XPathFactory}; this provider therefore only overrides the
 * DOM, SAX and Schema {@code configure} methods.</p>
 *
 * <p>For {@link SchemaFactory}, Xerces does not implement the JAXP 1.5 {@code ACCESS_EXTERNAL_*} properties and does not propagate SchemaFactory-level
 * hardening (including {@code disallow-doctype-decl} and the resource resolver) to {@link Validator} / {@link ValidatorHandler} instances produced by a
 * compiled {@link Schema}. {@link #configure(SchemaFactory)} installs a deny-all {@link LSResourceResolver} on the inner factory (which closes the
 * {@code xs:import}/{@code xs:include}/{@code xs:redefine} vectors at schema-compilation time) and returns a delegating wrapper whose
 * {@link Schema#newValidator()} and {@link Schema#newValidatorHandler()} re-install the factory's current resolver on every product (which closes the
 * DOCTYPE SYSTEM vector inside the instance document).</p>
 *
 * <p>Must be declared {@code public} so {@link java.util.ServiceLoader} can load it from {@code META-INF/services/}.</p>
 */
public final class XercesProvider extends AbstractXmlProvider {

    /**
     * Refuses every resource-resolution request with a {@link SecurityException}. Stateless, shared singleton.
     */
    private static final class DenyAllResourceResolver implements LSResourceResolver {

        static final DenyAllResourceResolver INSTANCE = new DenyAllResourceResolver();

        private DenyAllResourceResolver() {
        }

        @Override
        public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI) {
            throw new SecurityException(String.format("External %s resolution forbidden by hardening: namespace=%s, publicId=%s, systemId=%s, baseURI=%s",
                    type, namespaceURI, publicId, systemId, baseURI));
        }
    }

    /**
     * Delegating {@link Schema} that applies {@link XMLConstants#FEATURE_SECURE_PROCESSING FEATURE_SECURE_PROCESSING} and re-installs the resource resolver
     * captured at schema-creation time on every {@link Validator} / {@link ValidatorHandler} it produces.
     *
     * <p>Xerces honours {@code setResourceResolver} on validators, so any DOCTYPE SYSTEM or external-entity reference in the instance document is routed
     * through it. With {@link DenyAllResourceResolver} in place that means the fetch throws {@link SecurityException} before any I/O.</p>
     */
    private static final class HardeningSchema extends Schema {

        private final Schema delegate;
        private final LSResourceResolver resolver;
        private final Object securityManager;

        HardeningSchema(final Schema delegate, final LSResourceResolver resolver, final Object securityManager) {
            this.delegate = delegate;
            this.resolver = resolver;
            this.securityManager = securityManager;
        }

        @Override
        public Validator newValidator() {
            final Validator validator = delegate.newValidator();
            setFeature(validator, XMLConstants.FEATURE_SECURE_PROCESSING, true);
            setProperty(validator, XERCES_SECURITY_MANAGER_PROPERTY, securityManager);
            validator.setResourceResolver(resolver);
            return validator;
        }

        @Override
        public ValidatorHandler newValidatorHandler() {
            final ValidatorHandler handler = delegate.newValidatorHandler();
            setFeature(handler, XMLConstants.FEATURE_SECURE_PROCESSING, true);
            setProperty(handler, XERCES_SECURITY_MANAGER_PROPERTY, securityManager);
            handler.setResourceResolver(resolver);
            return handler;
        }
    }

    /**
     * Delegating {@link SchemaFactory} whose {@code newSchema(...)} methods capture the current resource resolver and wrap the result in
     * {@link HardeningSchema}. The non-abstract {@code newSchema(Source)} / {@code newSchema(URL)} / {@code newSchema(File)} overloads route through the
     * abstract {@code newSchema(Source[])} via the default implementations in {@link SchemaFactory} and therefore also produce a {@link HardeningSchema}.
     */
    private static final class HardeningSchemaFactory extends SchemaFactory {

        private final SchemaFactory delegate;
        private final Object securityManager;

        HardeningSchemaFactory(final SchemaFactory delegate, final Object securityManager) {
            this.delegate = delegate;
            this.securityManager = securityManager;
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
            return new HardeningSchema(delegate.newSchema(), delegate.getResourceResolver(), securityManager);
        }

        @Override
        public Schema newSchema(final Source[] schemas) throws SAXException {
            return new HardeningSchema(delegate.newSchema(schemas), delegate.getResourceResolver(), securityManager);
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

    private static final String FEATURE_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    /**
     * Xerces-specific factory/validator property whose value is an {@code org.apache.xerces.util.SecurityManager} instance carrying processing-limit
     * thresholds (entity-expansion limit, max occur limit, and so on).
     */
    private static final String XERCES_SECURITY_MANAGER_PROPERTY = "http://apache.org/xml/properties/security-manager";

    /**
     * Reads an integer JDK XML limit from the named system property, falling back to {@code defaultValue} if unset, blank, or unparsable.
     */
    private static int jdkLimit(final String systemPropertyName, final int defaultValue) {
        final String raw = System.getProperty(systemPropertyName);
        if (raw == null || raw.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    private final Object securityManager;

    /**
     * Default constructor; invoked by {@link java.util.ServiceLoader} and the registry.
     */
    public XercesProvider() {
        super("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl",
                "org.apache.xerces.jaxp.SAXParserFactoryImpl",
                "org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        Object sm;
        try {
            final Class<?> clazz = Class.forName("org.apache.xerces.util.SecurityManager");
            sm = clazz.getDeclaredConstructor().newInstance();
            clazz.getMethod("setEntityExpansionLimit", int.class).invoke(sm, jdkLimit("jdk.xml.entityExpansionLimit", 64000));
            clazz.getMethod("setMaxOccurNodeLimit", int.class).invoke(sm, jdkLimit("jdk.xml.maxOccurLimit", 5000));
        } catch (final ReflectiveOperationException e) {
            sm = null;
        }
        this.securityManager = sm;
    }

    @Override
    public DocumentBuilderFactory configure(final DocumentBuilderFactory factory) {
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeature(factory, FEATURE_DISALLOW_DOCTYPE, true);
        factory.setXIncludeAware(false);
        factory.setValidating(false);
        return factory;
    }

    @Override
    public SAXParserFactory configure(final SAXParserFactory factory) {
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeature(factory, FEATURE_DISALLOW_DOCTYPE, true);
        factory.setXIncludeAware(false);
        factory.setValidating(false);
        return factory;
    }

    @Override
    public SchemaFactory configure(final SchemaFactory factory) {
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Rejects DOCTYPE in the schema document itself; Xerces does not propagate this feature to Validators, so we do not bother setting it on them.
        setFeature(factory, FEATURE_DISALLOW_DOCTYPE, true);
        // Blocks xs:import/xs:include/xs:redefine resolution during schema compilation. The same resolver is captured at schema-creation time and re-installed
        // on every Validator/ValidatorHandler produced from the resulting Schema (see HardeningSchema), because Xerces does not propagate it automatically.
        factory.setResourceResolver(DenyAllResourceResolver.INSTANCE);
        return new HardeningSchemaFactory(factory, securityManager);
    }
}
