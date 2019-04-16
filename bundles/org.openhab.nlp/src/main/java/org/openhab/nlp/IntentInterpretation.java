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

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.Item;

/**
 * A intent interpretation is the result of the interpretation of an {@link Intent} by a {@link Skill}.
 * It contains a natural language answer, an optional "hint" (second line of the answer), and a {@link Card} to present
 * to the user.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class IntentInterpretation {

    public @Nullable String answer;
    public @Nullable String hint;

    public @Nullable Collection<Item> matchedItems;

    public @Nullable Intent intent;
    public @Nullable Collection<Item> filteredItems;
    public @Nullable String period;

    public void setIntentData(Intent intent, Collection<Item> filteredItems) {
        this.intent = intent;
        this.filteredItems = filteredItems;
    }

    public void setIntentData(Intent intent, Collection<Item> filteredItems, String period) {
        this.intent = intent;
        this.filteredItems = filteredItems;
        this.period = period;
    }

    /**
     * Retrieves the items matched as part of the interpretation.
     *
     * @return the collection of matched items
     */
    public Collection<Item> getMatchedItems() {
        return matchedItems;
    }

    /**
     * Sets the collection of items matched as part of the interpretation.
     *
     * @param matchedItems the collection of matched items
     */
    public void setMatchedItems(Collection<Item> matchedItems) {
        this.matchedItems = matchedItems;
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
}
