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
package org.openhab.nlp.internal.skill;

import java.util.Set;

import org.eclipse.smarthome.core.items.Item;
import org.openhab.nlp.AbstractItemIntentInterpreter;
import org.openhab.nlp.Intent;
import org.openhab.nlp.IntentInterpretation;
import org.openhab.nlp.ItemResolver;
import org.openhab.nlp.Skill;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This {@link Skill} is used to reply with a card containing a hourly chart of the matching item(s).
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(service = Skill.class)
public class HistoryHourlyGraphSkill extends AbstractItemIntentInterpreter {

    @Override
    public String getIntentId() {
        return "get-history-hourly";
    }

    @Override
    public IntentInterpretation interpret(Intent intent, String language) {
        IntentInterpretation interpretation = new IntentInterpretation();
        Set<Item> matchedItems = findItems(intent);

        if (intent.getEntities().isEmpty()) {
            interpretation.setAnswer(answerFormatter.getRandomAnswer("general_failure"));
            return interpretation;
        }
        if (matchedItems == null || matchedItems.isEmpty()) {
            interpretation.setAnswer(answerFormatter.getRandomAnswer("answer_nothing_found"));
            interpretation.setHint(answerFormatter.getStandardTagHint(intent.getEntities()));
        } else {
            interpretation.setMatchedItems(matchedItems);

            String period = "h";
            if (intent.getEntities().containsKey("period")) {
                period = intent.getEntities().get("period").concat(period);
            }

            interpretation.setIntentData(intent, matchedItems, period);
        }

        interpretation.setAnswer(answerFormatter.getRandomAnswer("info_found_simple"));

        return interpretation;
    }

    @Reference
    protected void setItemResolver(ItemResolver itemResolver) {
        this.itemResolver = itemResolver;
    }

    protected void unsetItemResolver(ItemResolver itemResolver) {
        this.itemResolver = null;
    }
}
