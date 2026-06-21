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

import static java.util.Arrays.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.vafer.jdependency.Clazz;
import org.vafer.jdependency.Clazzpath;

/**
 * Pins the class-level shading footprint of {@link DocumentBuilderHardener#newInstance()}.
 *
 * <p>jdependency (the same analysis the {@code maven-shade-plugin}'s {@code minimizeJar} uses) is deliberately class-grained: when a consumer shades the
 * library and only references {@link DocumentBuilderHardener}, the shade plugin copies the transitive closure of that one class. This test computes that
 * closure and asserts it equals the intended minimal set, so the footprint cannot silently grow back to the whole library.</p>
 *
 * <p>jdependency is ASM-based and follows genuine usage (instructions, field and method descriptors, signatures), not orphan constant-pool entries. Reading
 * another class's {@code static final} constant inlines the value but leaves a {@code CONSTANT_Class} entry for the owning class that no instruction
 * references; jdependency (like the shade plugin) ignores it, so such a reference does not enlarge the footprint. What this test catches is a <em>real</em>
 * reference leaking in: for example reusing the shared {@code Resolvers} aggregator for a single deny-all resolver would drag its whole nested-class tree (and
 * anything it transitively touches) into the closure. {@link DocumentBuilderHardener} therefore inlines its own deny-all {@link org.xml.sax.EntityResolver}
 * lambda rather than reusing {@code Resolvers}.</p>
 */
class ShadingFootprintTest {

    private static final String PACKAGE_PREFIX = "org.apache.commons.xml";

    private static final String ENTRY_POINT = PACKAGE_PREFIX + ".DocumentBuilderHardener";

    /**
     * The complete set of {@code org.apache.commons.xml} classes (nested classes included) a shading consumer copies when it references only
     * {@link DocumentBuilderHardener#newInstance()}. Keep this list and the production code in lockstep: a change here should be a deliberate footprint
     * decision, not an accident.
     */
    private static final Set<String> EXPECTED_REACHABLE = new TreeSet<>(asList(
            PACKAGE_PREFIX + ".DocumentBuilderHardener",
            PACKAGE_PREFIX + ".DocumentBuilderHardener$DelegatingDocumentBuilderFactory",
            PACKAGE_PREFIX + ".DocumentBuilderHardener$HardeningDocumentBuilderFactory",
            PACKAGE_PREFIX + ".HardeningException",
            PACKAGE_PREFIX + ".JaxpSetters",
            PACKAGE_PREFIX + ".JaxpSetters$ThrowingAction",
            PACKAGE_PREFIX + ".Limits"));

    @Test
    void newInstanceShadesOnlyTheMinimalClassSet() throws Exception {
        final Clazzpath clazzpath = new Clazzpath();
        clazzpath.addClazzpathUnit(mainOutputDirectory());

        final Clazz entryPoint = clazzpath.getClazz(ENTRY_POINT);
        assertNotNull(entryPoint, "Entry point not found on the analysed classpath; is " + mainOutputDirectory() + " the compiled main output?");

        final Set<String> reachable = new TreeSet<>();
        reachable.add(entryPoint.getName());
        for (final Clazz dependency : entryPoint.getTransitiveDependencies()) {
            // Only our own classes are shaded; JDK and provided-scope (Saxon) classes are never copied.
            if (dependency.getName().startsWith(PACKAGE_PREFIX)) {
                reachable.add(dependency.getName());
            }
        }

        assertEquals(EXPECTED_REACHABLE, reachable,
                "Shading footprint of DocumentBuilderHardener.newInstance() changed. If this is intentional, update EXPECTED_REACHABLE; "
                        + "otherwise a new class-level dependency leaked into the minimal DOM path.");
    }

    /**
     * Locates the compiled main output directory ({@code target/classes}) from this library's own code source, so the test is independent of the working
     * directory surefire happens to run in.
     */
    private static Path mainOutputDirectory() throws URISyntaxException {
        final File codeSource = new File(DocumentBuilderHardener.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        // When run from the exploded classes directory this already is target/classes; guard against a jar by falling back to the conventional path.
        return codeSource.isDirectory() ? codeSource.toPath() : Paths.get("target", "classes");
    }
}
