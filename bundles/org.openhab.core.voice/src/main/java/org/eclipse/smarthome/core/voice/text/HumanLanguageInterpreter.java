/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.voice.text;

import java.util.Locale;
import java.util.Set;

/**
 * This is the interface that a human language text interpreter has to implement.
 *
 * @author Tilman Kamp - Initial contribution
 * @author Laurent Garnier - extended interface to support chat and intent interpretation
 */
public interface HumanLanguageInterpreter {

    /**
     * Returns a simple string that uniquely identifies this service
     *
     * @return an id that identifies this service
     */
    public String getId();

    /**
     * Returns a localized human readable label that can be used within UIs.
     *
     * @param locale the locale to provide the label for
     * @return a localized string to be used in UIs
     */
    public String getLabel(Locale locale);

    /**
     * Interprets a human language text fragment of a given {@link Locale} for voice control
     * Returns a greeting message if the text empty.
     *
     * @param locale language of the text (given by a {@link Locale})
     * @param text the text to interpret
     * @return a human language response
     */
    String interpret(Locale locale, String text) throws InterpretationException;

    /**
     * Interprets a human language text fragment of a given {@link Locale} for chat dialog
     * Returns a greeting message if the text empty.
     *
     * @param locale language of the text (given by a {@link Locale})
     * @param text the text to interpret
     * @return a result including a human language response and a card
     */
    InterpretationResult interpretForChat(Locale locale, String text) throws InterpretationException;

    /**
     * Interprets an intent for voice control
     *
     * @param locale the expected language of the result (given by a {@link Locale})
     * @param intent the intent
     * @return a human language response
     */
    String interpretForVoice(Locale locale, Intent intent) throws InterpretationException;

    /**
     * Interprets an intent for chat dialog
     *
     * @param locale the expected language of the result (given by a {@link Locale})
     * @param intent the intent
     * @return a result including a human language response and a card
     */
    InterpretationResult interpretForChat(Locale locale, Intent intent) throws InterpretationException;

    /**
     * Gets the grammar of all commands of a given {@link Locale} of the interpreter
     *
     * @param locale language of the commands (given by a {@link Locale})
     * @param format the grammar format
     * @return a grammar of the specified format
     */
    String getGrammar(Locale locale, String format);

    /**
     * Gets all supported languages of the interpreter by their {@link Locale}s
     *
     * @return Set of supported languages (each given by a {@link Locale}) or null, if there is no constraint
     */
    Set<Locale> getSupportedLocales();

    /**
     * Gets all supported grammar format specifiers
     *
     * @return Set of supported grammars (each given by a short name)
     */
    Set<String> getSupportedGrammarFormats();

    /**
     * Get all supported intents suitable for chat dialog handled by the interpreter
     *
     * @return Set of supported intents ids suitable for chat dialog, or null if none
     */
    Set<String> getSupportedChatIntents();

    /**
     * Get all supported intents suitable for voice control handled by the interpreter
     *
     * @return Set of supported intents ids suitable for voice control, or null if none
     */
    Set<String> getSupportedVoiceIntents();

}
