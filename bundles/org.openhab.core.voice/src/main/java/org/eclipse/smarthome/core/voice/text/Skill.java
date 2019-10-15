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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This interface must be implemented to add support for a certain intent.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Laurent Garnier - Moved from HABot + extended to distinguish chat dialog and voice control + getTrainingData
 *         removed + null annotations added
 */
@NonNullByDefault
public interface Skill {
    /**
     * Gets the internal name of the intent handled by this skill.
     *
     * @return the id of the intent covered by this skill
     */
    String getIntentId();

    /**
     * Interprets the recognized intent for voice control.
     *
     * @param intent the intent to interpret
     * @param language the language of the query
     * @return a human language response
     */
    String interpretForVoice(Intent intent, String language) throws InterpretationException;

    /**
     * Interprets the recognized intent for chat dialog.
     *
     * @param intent the intent to interpret
     * @param language the language of the query
     * @return the {@link InterpretationResult} containing the results of the interpretation
     */
    InterpretationResult interpretForChat(Intent intent, String language) throws InterpretationException;
}
