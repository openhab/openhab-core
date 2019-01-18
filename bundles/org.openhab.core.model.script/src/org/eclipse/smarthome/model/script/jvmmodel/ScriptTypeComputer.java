/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.script.jvmmodel;

import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.model.script.script.QuantityLiteral;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeExpectation;
import org.eclipse.xtext.xbase.typesystem.computation.XbaseTypeComputer;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

/**
 * Calculates the type information used by Xbase to select the correct method during script execution.
 *
 * @author Henning Treu - initial contribution
 *
 */
@SuppressWarnings("restriction")
public class ScriptTypeComputer extends XbaseTypeComputer {

    @Override
    public void computeTypes(XExpression expression, ITypeComputationState state) {
        if (expression instanceof QuantityLiteral) {
            _computeTypes((QuantityLiteral) expression, state);
        } else {
            super.computeTypes(expression, state);
        }
    }

    protected void _computeTypes(final QuantityLiteral assignment, ITypeComputationState state) {
        LightweightTypeReference qt = null;
        for (ITypeExpectation exp : state.getExpectations()) {
            if (exp.getExpectedType() == null) {
                continue;
            }

            if (exp.getExpectedType().isType(Number.class)) {
                qt = getRawTypeForName(Number.class, state);
            }
            if (exp.getExpectedType().isType(State.class)) {
                qt = getRawTypeForName(Number.class, state);
            }
            if (exp.getExpectedType().isType(Command.class)) {
                qt = getRawTypeForName(Number.class, state);
            }
        }
        if (qt == null) {
            qt = getRawTypeForName(QuantityType.class, state);
        }
        state.acceptActualType(qt);
    }

}
