/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

/**
 * Thrown when a factory cannot be hardened.
 *
 * <p>Two failure modes share this type:</p>
 * <ul>
 *   <li>No bundled hardening recipe matches the concrete factory class.</li>
 *   <li>A recipe tried to apply a hardening setting and the implementation rejected it.</li>
 * </ul>
 *
 * <p>The message names the unsupported factory class or the specific feature, attribute or property that failed; the cause, when present, is the original
 * checked or unchecked exception from the JAXP implementation.</p>
 *
 * <p>Package-private by design: callers should catch {@link IllegalStateException}, which this extends.</p>
 */
class HardeningException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    HardeningException(final String message) {
        super(message);
    }

    HardeningException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
