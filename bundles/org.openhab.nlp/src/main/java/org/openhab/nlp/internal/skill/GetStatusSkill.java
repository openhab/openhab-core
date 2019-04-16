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

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.smarthome.core.items.Item;
import org.openhab.nlp.AbstractItemIntentInterpreter;
import org.openhab.nlp.Intent;
import org.openhab.nlp.IntentInterpretation;
import org.openhab.nlp.ItemResolver;
import org.openhab.nlp.Skill;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This {@link Skill} is used to present the status of objects to the user.
 *
 * @author Yannick Schaus - Initial contribution
 * @author David Graeff - Adapted for textual representation
 */
@Component(service = Skill.class, immediate = true)
public class GetStatusSkill extends AbstractItemIntentInterpreter {

    @Override
    public String getIntentId() {
        return "get-status";
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
            interpretation.filteredItems = matchedItems;
            interpretation.intent = intent;

            if (matchedItems.size() == 1) {
                String state = matchedItems.iterator().next().getState().toString();
                interpretation.setAnswer(answerFormatter.getRandomAnswer("get_value_single",
                        Collections.unmodifiableMap(Collections.singletonMap("value", state))));
            } else {
                String state = matchedItems.stream().map(item -> item.getState().toString())
                        .collect(Collectors.joining(", "));
                interpretation.setAnswer(answerFormatter.getRandomAnswer("get_value_single",
                        Collections.unmodifiableMap(Collections.singletonMap("value", state))));
            }
        }

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
