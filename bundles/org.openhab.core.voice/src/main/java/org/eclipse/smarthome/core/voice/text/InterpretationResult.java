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

import org.eclipse.smarthome.core.voice.chat.Card;

/**
 * The complete object representing a chat reply.
 * It includes the natural language answer and hint, recognized intent (name and entities), matched items and card.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class InterpretationResult {

    Locale locale;
    String answer;
    String hint;
    Intent intent;
    String[] matchedItemNames;
    Card card;

    public InterpretationResult(Locale locale) {
        this.locale = locale;
    }

    public InterpretationResult(Locale locale, String answer) {
        this.locale = locale;
        this.answer = answer;
    }

    public InterpretationResult(Locale locale, Intent intent) {
        this.locale = locale;
        this.intent = intent;
    }

    /**
     * Gets the locale of the answer
     *
     * @return the locale of the answer
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Gets the natural language answer
     *
     * @return the answer
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Sets the natural language answer
     *
     * @param answer the answer
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * Gets the hint - usually an indication why the interpretation failed.
     *
     * @return the hint
     */
    public String getHint() {
        return hint;
    }

    /**
     * Sets the hint - usually an indication why the interpretation failed.
     *
     * @param hint the hint
     */
    public void setHint(String hint) {
        this.hint = hint;
    }

    /**
     * Gets the recognized intent
     *
     * @return the intent
     */
    public Intent getIntent() {
        return intent;
    }

    /**
     * Sets the recognized intent
     *
     * @param intent the intent
     */
    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    /**
     * Gets the item names matched by the intent entities
     *
     * @return the matched item names
     */
    public String[] getMatchedItemNames() {
        return matchedItemNames;
    }

    /**
     * Sets the item names matched by the intent entities
     *
     * @param matchedItemNames the matched item names
     */
    public void setMatchedItems(String[] matchedItemNames) {
        this.matchedItemNames = matchedItemNames;
    }

    /**
     * Gets the {@link Card} to present with the answer
     *
     * @return the card
     */
    public Card getCard() {
        return card;
    }

    /**
     * Sets the {@link Card} to present with the answer
     *
     * @return the card
     */
    public void setCard(Card card) {
        this.card = card;
    }

}
