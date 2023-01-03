/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
@NonNullByDefault
public class HumanLanguageInterpreterStub implements HumanLanguageInterpreter {

    private static final Set<Locale> SUPPORTED_LOCALES = Set.of(Locale.ENGLISH);

    private static final String INTERPRETED_TEXT = "Interpreted text";
    private static final String EXCEPTION_MESSAGE = "interpretation exception";

    private static final String HLI_STUB_ID = "HLIStubID";
    private static final String HLI_STUB_LABEL = "HLIStubLabel";

    private boolean exceptionExpected;
    private String question = "";
    private String answer = "";

    @Override
    public String getId() {
        return HLI_STUB_ID;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return HLI_STUB_LABEL;
    }

    @Override
    public String interpret(Locale locale, String text) throws InterpretationException {
        question = text;
        if (exceptionExpected) {
            throw new InterpretationException(EXCEPTION_MESSAGE);
        } else {
            answer = INTERPRETED_TEXT;
            return answer;
        }
    }

    @Override
    public @Nullable String getGrammar(Locale locale, String format) {
        // This method will not be used in the tests
        return null;
    }

    @Override
    public Set<Locale> getSupportedLocales() {
        return SUPPORTED_LOCALES;
    }

    @Override
    public Set<String> getSupportedGrammarFormats() {
        // This method will not be used in the tests
        return Set.of();
    }

    public void setExceptionExpected(boolean exceptionExpected) {
        this.exceptionExpected = exceptionExpected;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    @Override
    public String toString() {
        return getId();
    }
}
