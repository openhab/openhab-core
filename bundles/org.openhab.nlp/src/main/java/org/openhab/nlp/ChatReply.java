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
package org.openhab.nlp;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The complete DTO object representing a chat reply, returned by the REST API.
 * It includes the original query, language, natural language answer and hint, recognized intent (name and entities),
 * matched items and card.
 *
 * @author Yannick Schaus - Initial contribution
 */
@NonNullByDefault
public class ChatReply {

    public final String language;
    public final String query;
    public final Intent intent;
    public final @Nullable String answer;
    public final @Nullable String hint;
    public final String @Nullable [] matchedItemNames;

    /**
     * Constructs a ChatReply for the specified {@link Locale} and query
     *
     * @param locale
     * @param query the user query
     * @param answer the answer
     * @param hint the hint - usually an indication why the interpretation failed.
     * @param intent Sets the recognized intent
     * @param matchedItemNames Sets the item names matched by the intent entities
     */
    public ChatReply(Locale locale, String query, Intent intent, @Nullable String answer, @Nullable String hint,
            String @Nullable [] matchedItemNames) {
        this.answer = answer;
        this.hint = hint;
        this.intent = intent;
        this.matchedItemNames = matchedItemNames;
        this.language = locale.getLanguage();
        this.query = query;
    }
}
