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
