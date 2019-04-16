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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.voice.text.InterpretationException;

/**
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class ComputeReply {

    /**
     * This variant of interpret() returns a more complete interpretation result.
     *
     * @param locale the locale of the query
     * @param text the query text
     * @return the interpretation result as a {@link ChatReply} object
     * @throws InterpretationException
     */
    @SuppressWarnings("null")
    public ChatReply reply(LocaleSpecificIntentTrainer trainer, String text, ItemResolver itemResolver,
            Map<String, Skill> skills) {

        Intent intent;

        // Shortcut: if there are any items whose named attributes match the query (case insensitive), consider
        // it a "get-status" intent with this attribute as the corresponding entity.
        // This allows the user to query a named attribute quickly by simply stating it - and avoid a
        // misinterpretation by the categorizer.
        if (itemResolver.getMatchingItems(text, null).findAny().isPresent()) {
            intent = new Intent("get-status");
            intent.setEntities(new HashMap<String, String>());
            intent.getEntities().put("object", text.toLowerCase());
        } else if (itemResolver.getMatchingItems(null, text).findAny().isPresent()) {
            intent = new Intent("get-status");
            intent.setEntities(new HashMap<String, String>());
            intent.getEntities().put("location", text.toLowerCase());
        } else {
            // Else, run it through the IntentTrainer
            intent = trainer.getIntentTrainer().interpret(text);
        }

        Skill skill = skills.get(intent.getName());
        IntentInterpretation intentInterpretation = null;

        if (skill != null) {
            intentInterpretation = skill.interpret(intent, trainer.getLocale().getLanguage());
        }

        if (intentInterpretation == null) {
            return new ChatReply(trainer.getLocale(), text, intent, null, null, null);
        }

        String[] matchedItems = null;
        if (intentInterpretation.getMatchedItems() != null) {
            matchedItems = intentInterpretation.getMatchedItems().stream().map(i -> i.getName())
                    .collect(Collectors.toList()).toArray(new String[0]);
        }

        return new ChatReply(trainer.getLocale(), text, intent, intentInterpretation.getAnswer(),
                intentInterpretation.getHint(), matchedItems);
    }
}
