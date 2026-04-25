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
package org.apache.commons.xml.factory.attacks;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.xml.factory.XmlFactories;
import org.junit.jupiter.api.Test;

/**
 * Checks whether stylesheets can access external resources.
 *
 * <p>Each fixture under {@code src/test/resources/leaked/} references a sibling file that, if hardening fails to block, would emit the
 * {@link #MARKER} string into the transform output. The assertion is deterministic: hardening either throws, or the call returns and the marker does not
 * appear in the output.</p>
 *
 * <p>Cases covered:</p>
 *
 * <ul>
 *   <li>{@code xsl:include} of a sibling stylesheet, resolved at compile time.</li>
 *   <li>{@code xsl:import} of a sibling stylesheet, resolved at compile time.</li>
 *   <li>{@code document()} call referencing a sibling XML file, resolved at transform time.</li>
 * </ul>
 */
class ExternalStylesheetTest {

    private static final String INPUT = "<root/>";

    private static final String MARKER = "All your base are belong to us";

    private static void assertStylesheetExcludesMarker(final String resource) {
        final URL url = AttackTestSupport.resourceUrl(resource);
        final String output;
        try {
            final StreamSource src = new StreamSource(url.openStream(), url.toString());
            final Templates templates = XmlFactories.newTransformerFactory().newTemplates(src);
            final StringWriter sink = new StringWriter();
            templates.newTransformer().transform(new StreamSource(new StringReader(INPUT)), new StreamResult(sink));
            output = sink.toString();
        } catch (final Throwable t) {
            return; // hardening blocked at compile or transform; acceptable outcome.
        }
        assertFalse(output.contains(MARKER),
                "Hardening did not block the external reference; output contained marker '" + MARKER + "'.\nFull output:\n" + output);
    }

    @Test
    void transformerBlocksDocument() {
        assertStylesheetExcludesMarker("with-document.xsl");
    }

    @Test
    void transformerStylesheetBlocksImport() {
        assertStylesheetExcludesMarker("with-import.xsl");
    }

    @Test
    void transformerStylesheetBlocksInclude() {
        assertStylesheetExcludesMarker("with-include.xsl");
    }
}
