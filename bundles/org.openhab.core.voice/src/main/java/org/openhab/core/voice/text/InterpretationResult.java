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
package org.openhab.core.voice.text;

/**
 * Bundles results of an interpretation. Represents final outcome and user feedback. This class is immutable.
 *
 * @author Tilman Kamp - Initial contribution
 */
public final class InterpretationResult {

    /**
     * Represents successful parsing and interpretation.
     */
    public static final InterpretationResult OK = new InterpretationResult(true, "");

    /**
     * Represents a syntactical problem during parsing.
     */
    public static final InterpretationResult SYNTAX_ERROR = new InterpretationResult(false, "Syntax error.");

    /**
     * Represents a problem in the interpretation step after successful parsing.
     */
    public static final InterpretationResult SEMANTIC_ERROR = new InterpretationResult(false, "Semantic error.");

    private boolean success = false;
    private InterpretationException exception;
    private String response;

    /**
     * Constructs a successful result.
     *
     * @param response the textual response. Should be short, localized and understandable by non-technical users.
     */
    public InterpretationResult(String response) {
        super();
        this.response = response;
        this.success = true;
    }

    /**
     * Constructs an unsuccessful result.
     *
     * @param exception the responsible exception
     */
    public InterpretationResult(InterpretationException exception) {
        super();
        this.exception = exception;
        this.success = false;
    }

    /**
     * Constructs a result.
     *
     * @param success if the result represents a successful or unsuccessful interpretation
     * @param response the textual response. Should be short, localized and understandable by non-technical users.
     */
    public InterpretationResult(boolean success, String response) {
        super();
        this.success = success;
        this.response = response;
    }

    /**
     * @return if interpretation was successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return the exception
     */
    public InterpretationException getException() {
        return exception;
    }

    /**
     * @return the response
     */
    public String getResponse() {
        return response;
    }
}
