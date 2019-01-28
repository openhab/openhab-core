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
package org.eclipse.smarthome.core.voice.text;

/**
 * An exception used by {@link HumanLanguageInterpreter}s, if an error occurs.
 *
 * @author Tilman Kamp - Initial contribution and API
 *
 */
public class InterpretationException extends Exception {

    private static final long serialVersionUID = 76120119745036525L;

    /**
     * Constructs a new interpretation exception.
     *
     * @param msg the textual response. Should be short, localized and understandable by non-technical users.
     */
    public InterpretationException(String msg) {
        super(msg);
    }

}
