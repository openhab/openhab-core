/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.hli;

import java.util.Locale;

/**
 * The complete DTO object representing an HABot chat reply, returned by the REST API.
 * It includes the original query, language, natural language answer and hint, recognized intent (name and entities),
 * matched items and card.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class ChatReply {

    private String language;
    private String query;
    private String answer;
    private String hint;
    private Intent intent;
    private String[] matchedItemNames;
    private Card card;

    /**
     * Constructs a ChatReply for the specified {@link Locale}
     *
     * @param locale the locale
     */
    public ChatReply(Locale locale) {
        this.language = locale.getLanguage();
    }

    /**
     * Constructs a ChatReply for the specified {@link Locale} and query
     *
     * @param locale the locale
     * @param query the user query
     */
    public ChatReply(Locale locale, String query) {
        this.language = locale.getLanguage();
        this.query = query;
    }

    /**
     * Gets the language of the reply
     *
     * @return the ISO-639 code of the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Gets the user's original query
     *
     * @return the user query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the user's original query
     *
     * @param query the user query
     */
    public void setQuery(String query) {
        this.query = query;
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
     * @param card the card
     */
    public void setCard(Card card) {
        this.card = card;
    }
}
