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

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.voice.chat.Card;

/**
 * The complete object representing a chat reply.
 * It includes the natural language answer and hint, recognized intent (name and entities), matched items and card.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class InterpretationResult {

    String language;
    @Nullable
    String answer;
    @Nullable
    String hint;
    @Nullable
    Intent intent;
    String[] matchedItemNames;
    Set<Item> matchedItems;
    @Nullable
    Card card;

    public InterpretationResult(String language, String answer) {
        this.language = language;
        this.answer = answer;
        this.matchedItemNames = new String[0];
        this.matchedItems = Collections.emptySet();
    }

    public InterpretationResult(String language, Intent intent) {
        this.language = language;
        this.intent = intent;
        this.matchedItemNames = new String[0];
        this.matchedItems = Collections.emptySet();
    }

    /**
     * Gets the language of the answer
     *
     * @return the language of the answer
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Gets the natural language answer
     *
     * @return the answer
     */
    public @Nullable String getAnswer() {
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
    public @Nullable String getHint() {
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
    public @Nullable Intent getIntent() {
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
     * Gets the items matched by the intent entities
     *
     * @return the matched items
     */
    public Set<Item> getMatchedItems() {
        return matchedItems;
    }

    /**
     * Sets the items matched by the intent entities
     *
     * @param matchedItems the matched items
     */
    public void setMatchedItems(Set<Item> matchedItems) {
        this.matchedItems = matchedItems;
    }

    /**
     * Gets the {@link Card} to present with the answer
     *
     * @return the card
     */
    public @Nullable Card getCard() {
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
