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

    private static class HardenedConfiguration extends  Configuration {
        private static final String HARDENED = "hardened";
        private final SAXParserFactory factory;

        private HardenedConfiguration(final SAXParserFactory factory) {
            this.factory = factory;
            setStyleParserClass(HARDENED);
            setSourceParserClass(HARDENED);
            setBooleanProperty(Feature.ALLOW_EXTERNAL_FUNCTIONS, false);
        }

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
