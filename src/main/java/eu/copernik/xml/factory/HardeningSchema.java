/*
 * SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.copernik.xml.factory;

import java.util.function.UnaryOperator;

import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;

/**
 * {@link Schema} wrapper that applies provider-specific decoration to every {@link Validator} and {@link ValidatorHandler} the inner Schema produces, then
 * wraps each {@link Validator} in {@link HardeningValidator} so {@link Validator#validate(javax.xml.transform.Source)} runs through
 * {@link XmlFactories#harden(javax.xml.transform.Source)}.
 */
final class HardeningSchema extends Schema {

    private final Schema delegate;
    private final UnaryOperator<Validator> validatorHardener;
    private final UnaryOperator<ValidatorHandler> handlerHardener;

    HardeningSchema(final Schema delegate, final UnaryOperator<Validator> validatorHardener, final UnaryOperator<ValidatorHandler> handlerHardener) {
        this.delegate = delegate;
        this.validatorHardener = validatorHardener;
        this.handlerHardener = handlerHardener;
    }

    @Override
    public Validator newValidator() {
        return new HardeningValidator(validatorHardener.apply(delegate.newValidator()));
    }

    @Override
    public ValidatorHandler newValidatorHandler() {
        return handlerHardener.apply(delegate.newValidatorHandler());
    }
}
