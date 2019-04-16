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
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openhab.nlp.AbstractItemIntentInterpreter;
import org.openhab.nlp.Intent;
import org.openhab.nlp.IntentInterpretation;
import org.openhab.nlp.ItemResolver;
import org.openhab.nlp.Skill;
import org.osgi.service.component.annotations.Reference;

import jersey.repackaged.com.google.common.collect.ImmutableMap;

/**
 * This {@link Skill} sets the matching item(s) to the specified numerical value (for dimmers, thermostats etc.) or
 * color (for Color items).
 *
 * @author Yannick Schaus - Initial contribution
 */
@org.osgi.service.component.annotations.Component(service = Skill.class)
public class SetValueSkill extends AbstractItemIntentInterpreter {

    private EventPublisher eventPublisher;

    @Override
    public String getIntentId() {
        return "set-value";
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

            if (intent.getEntities().containsKey("color")) {
                interpretSetColor(intent, language, interpretation, matchedItems);
            } else if (intent.getEntities().containsKey("value")) {
                interpretSetValue(intent, language, interpretation, matchedItems);
            } else {
                interpretation.setAnswer(answerFormatter.getRandomAnswer("value_misunderstood"));
            }
        }

        return interpretation;
    }

    private void interpretSetColor(Intent intent, String language, IntentInterpretation interpretation,
            Set<Item> matchedItems) {
        String colorString = intent.getEntities().get("color");

        // filter out the items which can't receive an HSB command
        List<Item> filteredItems = matchedItems.stream()
                .filter(i -> !(i instanceof GroupItem) && i.getAcceptedCommandTypes().contains(HSBType.class))
                .collect(Collectors.toList());

        String hsbValue;
        try {
            ResourceBundle colors = ResourceBundle.getBundle("colors", new Locale(language));
            hsbValue = colors.getString("color_" + colorString);
        } catch (MissingResourceException e) {
            interpretation.setAnswer(
                    answerFormatter.getRandomAnswer("set_color_unknown", ImmutableMap.of("color", colorString)));
            return;
        }

        if (filteredItems.isEmpty()) {
            interpretation.setAnswer(
                    answerFormatter.getRandomAnswer("set_color_no_item", ImmutableMap.of("color", colorString)));
            interpretation.setHint(answerFormatter.getStandardTagHint(intent.getEntities()));
        } else if (filteredItems.size() == 1) {

            interpretation.setIntentData(intent, filteredItems);
            eventPublisher
                    .post(ItemEventFactory.createCommandEvent(filteredItems.get(0).getName(), new HSBType(hsbValue)));
            interpretation.setAnswer(
                    answerFormatter.getRandomAnswer("set_color_single", ImmutableMap.of("color", colorString)));
        } else {
            interpretation.setIntentData(intent, filteredItems);
            for (Item item : filteredItems) {
                eventPublisher.post(ItemEventFactory.createCommandEvent(item.getName(), new HSBType(hsbValue)));
            }
            interpretation.setAnswer(answerFormatter.getRandomAnswer("set_color_multiple",
                    ImmutableMap.of("count", String.valueOf(filteredItems.size()), "color", colorString)));
        }
    }

    private void interpretSetValue(Intent intent, String language, IntentInterpretation interpretation,
            Set<Item> matchedItems) {
        String rawValue = intent.getEntities().get("value");

        // Set a color
        String cleanedValue = rawValue.replaceAll("[^0-9\\.,]", "");

        // only consider items which can receive an DecimalType command - includes PercentType, HSBType
        List<Item> filteredItems = matchedItems.stream()
                .filter(i -> !(i instanceof GroupItem) && i.getAcceptedCommandTypes().contains(DecimalType.class)
                        || i.getAcceptedCommandTypes().contains(PercentType.class))
                .collect(Collectors.toList());

        if (filteredItems.isEmpty()) {
            interpretation.setAnswer(
                    answerFormatter.getRandomAnswer("nothing_set_no_item", ImmutableMap.of("value", rawValue)));
            interpretation.setHint(answerFormatter.getStandardTagHint(intent.getEntities()));
        } else if (filteredItems.size() == 1) {
            DecimalType value = (filteredItems.get(0).getAcceptedCommandTypes().contains(DecimalType.class))
                    ? DecimalType.valueOf(cleanedValue)
                    : PercentType.valueOf(cleanedValue);
            interpretation.setIntentData(intent, filteredItems);
            eventPublisher.post(ItemEventFactory.createCommandEvent(filteredItems.get(0).getName(), value));
            interpretation
                    .setAnswer(answerFormatter.getRandomAnswer("set_value_single", ImmutableMap.of("value", rawValue)));
        } else {
            interpretation.setIntentData(intent, filteredItems);
            for (Item item : filteredItems) {
                DecimalType value = (item.getAcceptedCommandTypes().contains(DecimalType.class))
                        ? DecimalType.valueOf(cleanedValue)
                        : PercentType.valueOf(cleanedValue);
                eventPublisher.post(ItemEventFactory.createCommandEvent(item.getName(), value));
            }

            Map<String, String> map = new TreeMap<>();
            map.put("count", String.valueOf(filteredItems.size()));
            map.put("value", rawValue);
            interpretation
                    .setAnswer(answerFormatter.getRandomAnswer("set_value_multiple", Collections.unmodifiableMap(map)));
        }
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
