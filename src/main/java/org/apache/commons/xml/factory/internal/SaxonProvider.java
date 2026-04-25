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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.commons.xml.factory.XmlFactories;
import org.apache.commons.xml.factory.spi.XmlProvider;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * {@link XmlProvider} for Saxon-HE ({@code net.sf.saxon:Saxon-HE}).
 *
 * <p>Saxon supplies {@link TransformerFactory} and {@link XPathFactory} implementations; it does not ship a DOM, SAX, StAX or Schema factory of its own.</p>
 *
 * <p>Must be declared {@code public} so {@link java.util.ServiceLoader} can load it from {@code META-INF/services/}.</p>
 */
public final class SaxonProvider extends AbstractXmlProvider {

    /**
     * A Saxon {@link Configuration} that locks down every channel through which Saxon would otherwise reach external resources.
     *
     * <p>Three layers of restriction are applied:</p>
     *
     * <ol>
     *   <li><b>SAX layer.</b> Stylesheet and source-document reading is routed to {@link #makeParser} via the {@link #HARDENED} sentinel, which returns an
     *   {@link XMLReader} from a hardened {@link SAXParserFactory}. DOCTYPE, external entities and XInclude are refused at parse time.</li>
     *   <li><b>URI-resolution layer.</b> {@link Feature#ALLOWED_PROTOCOLS} is set to the empty string. This blocks XSLT inclusions {@code xsl:include},
     *   {@code xsl:import}, {@code xsl:source-document}, and the XPath/XSLT functions {@code fn:doc}, {@code fn:document}, {@code fn:unparsed-text},
     *   {@code fn:collection}, {@code fn:json-doc} and {@code fn:transform}.</li>
     *   <li><b>Extension-function layer.</b> {@link Feature#ALLOW_EXTERNAL_FUNCTIONS} is disabled, so reflection-based extension calls cannot be used to
     *       sidestep the URI restrictions.</li>
     * </ol>
     */
    private static class HardenedConfiguration extends  Configuration {
        private static final String HARDENED = "hardened";
        private final SAXParserFactory factory;

        private HardenedConfiguration(final SAXParserFactory factory) {
            this.factory = factory;
            // SAX layer: register the HARDENED sentinel for both stylesheet reading and source-document reading. Saxon will call makeParser(HARDENED) below
            // and receive an XMLReader from the supplied hardened SAXParserFactory.
            setStyleParserClass(HARDENED);
            setSourceParserClass(HARDENED);
            // Extension-function layer: turn off Saxon's reflection-based extension calls. Without this an attacker could bypass URI restrictions through
            // user-supplied Java extensions.
            setBooleanProperty(Feature.ALLOW_EXTERNAL_FUNCTIONS, false);
            // URI-resolution layer: empty string disallows every URI scheme. Saxon front-ends the existing ResourceResolver with a ProtocolRestrictor; a
            // later setResourceResolver call would cancel this filter, so this stays last in the constructor.
            setConfigurationProperty(Feature.ALLOWED_PROTOCOLS, "");
        }

        /**
         * Saxon's hook for instantiating a SAX parser by class name.
         *
         * <p>We use the {@value HARDENED} sentinel value to return a hardened parser instead.</p>
         */
        @Override
        public XMLReader makeParser(String className) throws TransformerFactoryConfigurationError {
            if (HARDENED.equals(className)) {
                try {
                    return factory.newSAXParser().getXMLReader();
                } catch (ParserConfigurationException | SAXException e) {
                    throw new TransformerFactoryConfigurationError(e);
                }
            }
            return super.makeParser(className);
        }
    }

    private static class SaxonProviderConfigurer {

        private static Configuration newHardenedConfiguration() {
            final SAXParserFactory parserFactory = XmlFactories.newSAXParserFactory();
            return new HardenedConfiguration(parserFactory);
        }

        private static TransformerFactory configure(final TransformerFactory factory) {
            ((SaxonTransformerFactory) factory).setConfiguration(newHardenedConfiguration());
            return factory;
        }

        private static XPathFactory configure(final XPathFactory factory) {
            ((XPathFactoryImpl) factory).setConfiguration(newHardenedConfiguration());
            return factory;
        }
    }

    /** Default constructor; invoked by {@link java.util.ServiceLoader} and the registry. */
    public SaxonProvider() {
        super("net.sf.saxon.TransformerFactoryImpl",
                "com.saxonica.config.ProfessionalTransformerFactory",
                "com.saxonica.config.EnterpriseTransformerFactory",
                "net.sf.saxon.xpath.XPathFactoryImpl");
    }

    @Override
    public TransformerFactory configure(final TransformerFactory factory) {
        try {
            return SaxonProviderConfigurer.configure(factory);
        } catch (LinkageError e) {
            // Unlikely, but protects method execution from missing optional dependency
            throw new IllegalStateException(e);
        }
    }

    @Override
    public XPathFactory configure(final XPathFactory factory) {
        try {
            return SaxonProviderConfigurer.configure(factory);
        } catch (LinkageError e) {
            // Unlikely, but protects method execution from missing optional dependency
            throw new IllegalStateException(e);
        }
    }
}
