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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openhab.nlp.AbstractItemIntentInterpreter;
import org.openhab.nlp.Intent;
import org.openhab.nlp.IntentInterpretation;
import org.openhab.nlp.ItemResolver;
import org.openhab.nlp.Skill;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This {@link Skill} deactivates objects - sends the OFF command to all matching items.
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(service = Skill.class)
public class DeactivateObjectSkill extends AbstractItemIntentInterpreter {

    private EventPublisher eventPublisher;

    @Override
    public String getIntentId() {
        return "deactivate-object";
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
            interpretation.setAnswer(answerFormatter.getRandomAnswer("nothing_deactivated"));
            interpretation.setHint(answerFormatter.getStandardTagHint(intent.getEntities()));
            return interpretation;
        }

        interpretation.setMatchedItems(matchedItems);

        // filter out the items which can't receive an OFF command
        List<Item> filteredItems = matchedItems.stream()
                .filter(i -> !(i instanceof GroupItem) && i.getAcceptedCommandTypes().contains(OnOffType.class))
                .collect(Collectors.toList());

        interpretation.filteredItems = filteredItems;
        interpretation.intent = intent;

        if (filteredItems.isEmpty()) {
            interpretation.setAnswer(answerFormatter.getRandomAnswer("nothing_deactivated"));
            interpretation.setHint(answerFormatter.getStandardTagHint(intent.getEntities()));
            return interpretation;
        }
        if (filteredItems.size() == 1) {
            if (filteredItems.get(0).getState().equals(OnOffType.OFF)) {
                interpretation.setAnswer(answerFormatter.getRandomAnswer("switch_already_off"));
            } else {
                eventPublisher.post(ItemEventFactory.createCommandEvent(filteredItems.get(0).getName(), OnOffType.OFF));
                interpretation.setAnswer(answerFormatter.getRandomAnswer("switch_deactivated"));
            }
            return interpretation;
        }
        for (Item item : filteredItems) {
            eventPublisher.post(ItemEventFactory.createCommandEvent(item.getName(), OnOffType.OFF));
        }
        interpretation.setAnswer(answerFormatter.getRandomAnswer("switches_deactivated",
                Collections.unmodifiableMap(Collections.singletonMap("count", String.valueOf(filteredItems.size())))));

        return interpretation;
    }

    @Reference
    protected void setItemResolver(ItemResolver itemResolver) {
        this.itemResolver = itemResolver;
    }

    protected void unsetItemResolver(ItemResolver itemResolver) {
        this.itemResolver = null;
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }
}
