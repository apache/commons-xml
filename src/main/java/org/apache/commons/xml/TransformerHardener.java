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

package org.apache.commons.xml;

import static org.apache.commons.xml.JaxpSetters.setFeature;
import static org.apache.commons.xml.JaxpSetters.setOptionalAttribute;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;

/**
 * Capability-driven hardening for any {@link TransformerFactory} on the classpath.
 *
 * <p>Rather than branching on the implementation class, {@link #harden(TransformerFactory)} probes what the factory supports and adapts:</p>
 * <ul>
 *     <li><strong>Saxon</strong> ({@code net.sf.saxon}): recognized by class name and handed to {@link SaxonProvider#configure(TransformerFactory)}. Unlike XSLTC
 *         and Xalan, Saxon reaches external resources through several channels (the {@link URIResolver}, a collection finder, an unparsed-text resolver) on top
 *         of reflection-based extension functions, none of which the standard JAXP knobs can close; only a locked-down Saxon {@code Configuration} can. This is
 *         the TrAX counterpart of the Android special case in {@link DocumentBuilderHardener}, kept as a documented class-name exception because the required
 *         hardening surface is reachable only through a vendor API.</li>
 *     <li><strong>FSP</strong> ({@link XMLConstants#FEATURE_SECURE_PROCESSING}): required. On XSLTC it enables the runtime evaluator limits; on Xalan it disables
 *         reflection-based extension functions.</li>
 *     <li><strong>Limits</strong>: applied best-effort by {@link Limits#tryApply(TransformerFactory)}. XSLTC honours the JDK attribute limits; Xalan ignores them
 *         (its caps come from FSP).</li>
 *     <li><strong>{@code ACCESS_EXTERNAL_DTD}</strong> (set to {@code ""}): required on XSLTC. XSLTC copies this factory attribute onto the reader that parses the
 *         stylesheet ({@code Util.getInputSource}), overwriting the {@code ACCESS_EXTERNAL_DTD} the wrapper's hardened reader had already set; without it a
 *         permissive default re-opens the external-DTD/entity channel during stylesheet compilation. Xalan rejects the attribute (best-effort, ignored) and closes
 *         that channel through the hardened reader instead. Its sibling {@code ACCESS_EXTERNAL_STYLESHEET} is <em>not</em> set: the deny-all resolver below already
 *         guards the only channel it covers.</li>
 *     <li><strong>{@link Resolvers.DenyAll#URI}</strong>: required. A deny-all {@link URIResolver} blocks {@code xsl:import}/{@code xsl:include} at compile time
 *         and {@code document()} at runtime, the one channel both XSLTC and Xalan route through.</li>
 *     <li><strong>{@link HardeningTransformerFactory}</strong>: required. Both implementations fall back to {@code SAXParserFactory.newInstance()} to parse a
 *         stylesheet or source document that does not carry its own reader, and only set FSP on it; wrapping the factory rewrites every {@link Source} through an
 *         {@link XmlFactories}-hardened reader instead. On Xalan that reader (its deny-all {@link org.xml.sax.EntityResolver} or its own
 *         {@code ACCESS_EXTERNAL_DTD}) is what blocks external DTDs and entities.</li>
 * </ul>
 *
 * <h2>Caveats</h2>
 * <ul>
 *     <li>Replacing the {@code URIResolver} on the returned factory or on a produced transformer cancels the deny-all block for XSLT URI fetches; neither XSLTC
 *         nor Xalan exposes a tamper-resistant equivalent of JAXP 1.5 {@code ACCESS_EXTERNAL_STYLESHEET}.</li>
 * </ul>
 */
final class TransformerHardener {

    /**
     * Class names of Saxon's {@link TransformerFactory} (open-source and commercial editions), hardened through a Saxon {@code Configuration} rather than the
     * standard JAXP knobs.
     */
    private static final Set<String> SAXON_TRANSFORMER_FACTORIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "net.sf.saxon.TransformerFactoryImpl",
            "com.saxonica.config.ProfessionalTransformerFactory",
            "com.saxonica.config.EnterpriseTransformerFactory")));

    static TransformerFactory harden(final TransformerFactory factory) {
        if (SAXON_TRANSFORMER_FACTORIES.contains(factory.getClass().getName())) {
            // Saxon: only a locked-down Configuration can close all of its resource-resolution channels and its extension-function surface.
            return SaxonProvider.configure(factory);
        }
        // Required: enables secure processing (XSLTC runtime limits; Xalan's extension-function block).
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Best-effort: JDK's XSLTC honors the JDK attribute limits, pinning them to JDK 25 secure values; Xalan ignores them.
        Limits.tryApply(factory);
        // Required on JDK's XSLTC: it copies this factory attribute onto the reader that parses the stylesheet (Util.getInputSource).
        // A permissive default here would re-open the external-DTD/entity channel.
        // Xalan rejects the attribute and blocks that channel through a deny-all resolver instead.
        setOptionalAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        // Required: the one channel both implementations honor; blocks xsl:import/include at compile time and document() at runtime.
        factory.setURIResolver(Resolvers.DenyAll.URI);
        // Required: source/stylesheet parsing provisions its own SAX reader otherwise; the wrapper routes every Source through a hardened one.
        return new HardeningTransformerFactory((SAXTransformerFactory) factory);
    }

    private TransformerHardener() {
    }
}
