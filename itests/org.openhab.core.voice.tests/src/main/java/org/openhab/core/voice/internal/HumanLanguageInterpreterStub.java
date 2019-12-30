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
package org.openhab.core.voice.internal;

import java.util.Locale;
import java.util.Set;

import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;

/**
 * A {@link HumanLanguageInterpreter} stub used for the tests. Since the tests do not cover
 * all the voice's features, some of the methods are not needed.
 * That's why their implementation is left empty.
 *
 * @author Mihaela Memova - Initial contribution
 *
 * @author Velin Yordanov - migrated from groovy to java
 */
public class HumanLanguageInterpreterStub implements HumanLanguageInterpreter {

    private static final String INTERPRETED_TEXT = "Interpreted text";
    private static final String EXCEPTION_MESSAGE = "Exception message";

    private static final String HLI_STUB_ID = "HLIStubID";
    private static final String HLI_STUB_LABEL = "HLIStubLabel";

    private boolean isInterpretationExceptionExpected;

    @Override
    public String getId() {
        return HLI_STUB_ID;
    }

    public void setIsInterpretationExceptionExpected(boolean value) {
        isInterpretationExceptionExpected = value;
    }

    @Override
    public String getLabel(Locale locale) {
        return HLI_STUB_LABEL;
    }

    @Override
    public String interpret(Locale locale, String text) throws InterpretationException {
        if (isInterpretationExceptionExpected) {
            throw new InterpretationException(EXCEPTION_MESSAGE);
        } else {
            return INTERPRETED_TEXT;
        }
    }

    @Override
    public String getGrammar(Locale locale, String format) {
        // This method will not be used in the tests
        return null;
    }

    @Override
    public Set<Locale> getSupportedLocales() {
        // This method will not be used in the tests
        return null;
    }

    @Override
    public Set<String> getSupportedGrammarFormats() {
        // This method will not be used in the tests
        return null;
    }

    @Override
    public String toString() {
        return getId();
    }
}
