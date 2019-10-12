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

import java.io.InputStream;
import java.util.Locale;

/**
 * This interface must be implemented to add support for a certain intent.
 * It covers both the training data to supply to OpenNLP for the intent categorization (and token extraction),
 * and the method to call when the intent is recognized.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Laurent Garnier - extended interface to distinguish chat dialog and voice control
 */
public interface Skill {
    /**
     * Gets the internal name of the intent handled by this skill.
     *
     * @return the id of the intent covered by this skill
     */
    String getIntentId();

    /**
     * Gets the id of the interpreter applicable for this skill.
     *
     * @return the id of the interpreter applicable for this skill
     */
    String getHliId();

    /**
     * Returns whether this skill is suitable for chat dialog
     *
     * @return true if the skill is suitable for chat dialog; false if not
     */
    boolean isSuitableForChat();

    /**
     * Returns whether this skill is suitable for voice control
     *
     * @return true if the skill is suitable for voice control; false if not
     */
    boolean isSuitableForVoice();

    /**
     * Get an input stream containing the training data for the specified intent to feed to the OpenNLP document
     * categorizer. The data consists in a series of sentences to be associated with the intent, and containing named
     * entities with standard OpenNLP tags. Example:
     * what's the &lt;START:object&gt;temperature&lt;END&gt; in the &lt;START:location&gt;living room&lt;END&gt;
     *
     * @param language the language (ISO-639 code) containing the expected language for the NLP training data
     * @throws UnsupportedLanguageException if the specified language is not supported by this skill
     * @return the input stream containing the training data
     */
    InputStream getTrainingData(String language) throws UnsupportedLanguageException;

    /**
     * Interprets the recognized intent for voice control.
     *
     * @param intent the intent to interpret
     * @param locale the language of the query
     * @return a human language response
     */
    String interpretForVoice(Intent intent, Locale locale) throws InterpretationException;

    /**
     * Interprets the recognized intent for chat dialog.
     *
     * @param intent the intent to interpret
     * @param locale the language of the query
     * @return the {@link InterpretationResult} containing the results of the interpretation
     */
    InterpretationResult interpretForChat(Intent intent, Locale locale) throws InterpretationException;
}
