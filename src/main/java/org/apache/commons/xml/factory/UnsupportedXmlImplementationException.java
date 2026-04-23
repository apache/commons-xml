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

/**
 * Thrown when no {@link org.apache.commons.xml.factory.spi.XmlProvider XmlProvider} is able to harden a JAXP factory of the given concrete class.
 */
public class UnsupportedXmlImplementationException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private final Class<?> unsupportedFactoryClass;

    /**
     * Constructs a new exception naming the unsupported factory class.
     *
     * <p>The detail message identifies the class and suggests remediation steps.</p>
     *
     * @param unsupportedFactoryClass the concrete factory class that no provider recognises.
     */
    public UnsupportedXmlImplementationException(final Class<?> unsupportedFactoryClass) {
        super(buildMessage(unsupportedFactoryClass));
        this.unsupportedFactoryClass = unsupportedFactoryClass;
    }

    private static String buildMessage(final Class<?> factoryClass) {
        return String.format("No XmlProvider supports JAXP factory class %s. Add a supported JAXP implementation (for example Apache Xerces) to the " +
                "classpath, or register a custom XmlProvider via META-INF/services/org.apache.commons.xml.factory.spi.XmlProvider.", factoryClass.getName());
    }

    /**
     * Gets the concrete factory class that no registered provider could configure.
     *
     * @return the unsupported factory class.
     */
    public Class<?> getUnsupportedFactoryClass() {
        return unsupportedFactoryClass;
    }
}
