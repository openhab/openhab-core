/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.script.internal;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.ItemUtil;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.voice.text.HumanLanguageInterpreter;
import org.eclipse.smarthome.core.voice.text.InterpretationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link HumanLanguageInterpreter} implementation which is mainly meant for backward-compatibility in the way
 * that it passes the texts to interpret as a command to a specified String item, on which DSL rules can then process
 * the string and trigger some actions.
 * The implicit agreement was an item called "VoiceCommand" to do exactly this; existing apps are using this and hence
 * this service will make this work.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@Component(service = HumanLanguageInterpreter.class, immediate = true, property = {
        "service.pid=org.eclipse.smarthome.rulehli", "service.config.description.uri=voice:rulehli",
        "service.config.label=Rule Voice Interpreter", "service.config.category=voice" })
public class RuleHumanLanguageInterpreter implements HumanLanguageInterpreter {

    private final Logger logger = LoggerFactory.getLogger(RuleHumanLanguageInterpreter.class);

    private String itemName = "VoiceCommand";
    private EventPublisher eventPublisher;

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        if (config != null) {
            String configItemName = (String) config.get("item");
            if (configItemName != null && ItemUtil.isValidItemName(configItemName)) {
                itemName = configItemName;
                logger.debug("Using item '{}' for passing voice commands.", itemName);
            }
        }
    }

    @Override
    public String getId() {
        return "rulehli";
    }

    @Override
    public String getLabel(Locale locale) {
        return "Rule-based Interpreter";
    }

    @Override
    public String interpret(Locale locale, String text) throws InterpretationException {
        Event event = ItemEventFactory.createCommandEvent(itemName, new StringType(text));
        eventPublisher.post(event);
        return null;
    }

    @Override
    public String getGrammar(Locale locale, String format) {
        return null;
    }

    @Override
    public Set<Locale> getSupportedLocales() {
        // we do not care about locales, so we return null here to indicate this
        return null;
    }

    @Override
    public Set<String> getSupportedGrammarFormats() {
        return Collections.emptySet();
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }
}
