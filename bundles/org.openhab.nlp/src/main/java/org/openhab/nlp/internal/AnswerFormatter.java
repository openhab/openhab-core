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
package org.openhab.nlp.internal;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * This class helps building natural language answers by selecting random answers from a resource bundle.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class AnswerFormatter {

    ResourceBundle answers;
    Locale locale;

    public AnswerFormatter(Locale locale) {
        this.locale = locale;
        this.answers = ResourceBundle.getBundle("answers", locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
    }

    public AnswerFormatter(String language) {
        this.locale = new Locale(language);
        this.answers = ResourceBundle.getBundle("answers", locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
    }

    public String getRandomAnswer(String key, Map<String, String> placeholderValues) {
        String alternativesStr = answers.getString(key);

        String[] alternatives = alternativesStr.split("\\|");

        Random random = new Random();
        String answer = alternatives[random.nextInt(alternatives.length)].trim();

        if (placeholderValues != null) {
            for (String placeholder : placeholderValues.keySet()) {
                answer = answer.replace("{" + placeholder + "}", placeholderValues.get(placeholder));
            }
        }

        return answer;
    }

    public String getRandomAnswer(String key) {
        return getRandomAnswer(key, null);
    }

    public String getStandardTagHint(Map<String, String> entities) {
        final String string = String.join(" & ",
                entities.entrySet().stream().filter(e -> e.getKey().equals("object") || e.getKey().equals("location"))
                        .map(e -> String.format("\"%s\"", e.getValue())).toArray(String[]::new));

        return getRandomAnswer("standard_hint",
                Collections.unmodifiableMap(Collections.singletonMap("attributes", string)));
    }
}
