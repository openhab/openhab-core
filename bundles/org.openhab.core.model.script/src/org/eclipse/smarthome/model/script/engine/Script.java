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
package org.eclipse.smarthome.model.script.engine;

import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;

/**
 * This interface is implemented by Eclipse SmartHome scripts.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@SuppressWarnings("restriction")
public interface Script {

    public static final String SCRIPT_FILEEXT = "script";

    /**
     * Executes the script instance and returns the execution result
     *
     * @return the execution result or <code>null</code>, if the script does not have a return value
     * @throws ScriptExecutionException if an error occurs during the execution
     */
    public Object execute() throws ScriptExecutionException;

    /**
     * Executes the script instance with a given evaluation context and returns the execution result
     *
     * @param evaluationContext the evaluation context is a map of variables (name, object)
     *            that should be available during the script execution
     * @return the execution result or <code>null</code>, if the script does not have a return value
     * @throws ScriptExecutionException if an error occurs during the execution
     */
    public Object execute(IEvaluationContext evaluationContext) throws ScriptExecutionException;
}
