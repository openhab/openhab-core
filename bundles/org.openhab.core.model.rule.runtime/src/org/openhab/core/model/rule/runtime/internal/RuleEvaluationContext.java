/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.model.rule.runtime.internal;

import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.impl.DefaultEvaluationContext;

public class RuleEvaluationContext extends DefaultEvaluationContext {

    private IEvaluationContext globalContext = null;

    public RuleEvaluationContext() {
        super(new DefaultEvaluationContext());
    }

    public void setGlobalContext(IEvaluationContext context) {
        this.globalContext = context;
    }

    @Override
    public Object getValue(QualifiedName qualifiedName) {
        Object value = super.getValue(qualifiedName);
        if (value == null && this.globalContext != null) {
            value = globalContext.getValue(qualifiedName);
        }
        return value;
    }

    @Override
    public void assignValue(QualifiedName qualifiedName, Object value) {
        try {
            super.assignValue(qualifiedName, value);
        } catch (IllegalStateException e) {
            if (globalContext != null) {
                globalContext.assignValue(qualifiedName, value);
            } else {
                throw e;
            }
        }
    }

}
