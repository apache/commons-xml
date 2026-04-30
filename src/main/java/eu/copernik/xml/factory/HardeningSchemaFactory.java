/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import java.util.function.UnaryOperator;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;

import org.xml.sax.SAXException;

/**
 * {@link SchemaFactory} wrapper that rewrites every Source-taking entry point through {@link XmlFactories#harden(Source)} and applies provider-specific
 * decoration to every {@link Validator} and {@link ValidatorHandler} that the produced {@link Schema} hands out.
 *
 * <p>Three layers cooperate:</p>
 * <ol>
 *   <li>{@link HardeningSchemaFactory} rewrites the Source on every {@code newSchema(Source[])} entry point.</li>
 *   <li>{@link HardeningSchema} applies the provider-specific hardener to every Validator/ValidatorHandler the inner Schema produces (e.g. Xerces re-installs
 *       its {@code LSResourceResolver} and {@code SecurityManager} since it does not propagate them through Schema).</li>
 *   <li>{@link HardeningValidator} rewrites the Source on every {@link Validator#validate(Source)} call.</li>
 * </ol>
 */
final class HardeningSchemaFactory extends DelegatingSchemaFactory {

    private final UnaryOperator<Validator> validatorHardener;
    private final UnaryOperator<ValidatorHandler> handlerHardener;

    HardeningSchemaFactory(final SchemaFactory delegate) {
        this(delegate, UnaryOperator.identity(), UnaryOperator.identity());
    }

    HardeningSchemaFactory(final SchemaFactory delegate, final UnaryOperator<Validator> validatorHardener,
            final UnaryOperator<ValidatorHandler> handlerHardener) {
        super(delegate);
        this.validatorHardener = validatorHardener;
        this.handlerHardener = handlerHardener;
    }

    @Override
    public Schema newSchema() throws SAXException {
        return new HardeningSchema(super.newSchema(), validatorHardener, handlerHardener);
    }

    @Override
    public Schema newSchema(final Source[] schemas) throws SAXException {
        return new HardeningSchema(super.newSchema(harden(schemas)), validatorHardener, handlerHardener);
    }

    private static Source[] harden(final Source[] schemas) throws SAXException {
        final Source[] hardened = new Source[schemas.length];
        try {
            for (int i = 0; i < schemas.length; i++) {
                hardened[i] = XmlFactories.harden(schemas[i]);
            }
        } catch (final TransformerConfigurationException e) {
            throw new SAXException("Failed to harden schema source", e);
        }
        return hardened;
    }
}
