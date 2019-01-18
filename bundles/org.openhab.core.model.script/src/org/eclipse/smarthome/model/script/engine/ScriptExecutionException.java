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

/**
 * Exception that is thrown on errors during script execution.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class ScriptExecutionException extends ScriptException {

    private static final long serialVersionUID = 149490362444673405L;

    public ScriptExecutionException(final String message, final int line, final int column, final int length) {
        super(message, null, line, column, length);
    }

    public ScriptExecutionException(final ScriptError scriptError) {
        super(scriptError);
    }

    public ScriptExecutionException(final String message, final Throwable cause, final int line, final int column,
            final int length) {
        super(cause, message, null, line, column, length);
    }

    public ScriptExecutionException(final String message) {
        super(message);
    }

    public ScriptExecutionException(String message, Throwable exception) {
        super(message, exception);
    }
}
