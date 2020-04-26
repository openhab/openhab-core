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
package org.openhab.core.automation.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * This class extends the {@link Exception} class functionality with functionality serving to accumulate the all
 * exceptions during the parsing process.
 *
 * @author Ana Dimova - Initial contribution
 */
@SuppressWarnings("serial")
public class ParsingException extends Exception {

    /**
     * Keeps all accumulated exceptions.
     */
    List<ParsingNestedException> exceptions;

    /**
     * Creates the holder for one exception during the parsing process.
     *
     * @param e is an exception during the parsing process.
     */
    public ParsingException(ParsingNestedException e) {
        exceptions = new ArrayList<>();
        exceptions.add(e);
    }

    /**
     * Creates a holder for several exceptions during the parsing process.
     *
     * @param exceptions is a list with exceptions during the parsing process.
     */
    public ParsingException(List<ParsingNestedException> exceptions) {
        this.exceptions = exceptions;
    }

    @Override
    public String getMessage() {
        StringBuilder writer = new StringBuilder();
        for (ParsingNestedException e : exceptions) {
            writer.append(e.getMessage() + "\n");
        }
        return writer.toString();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        int size = 0;
        for (ParsingNestedException e : exceptions) {
            size = size + e.getStackTrace().length;
        }
        int index = 0;
        StackTraceElement[] st = new StackTraceElement[size];
        for (int n = 0; n < exceptions.size(); n++) {
            StackTraceElement[] ste = exceptions.get(n).getStackTrace();
            System.arraycopy(ste, 0, st, index, ste.length);
            index += ste.length;
        }
        return st;
    }
}
