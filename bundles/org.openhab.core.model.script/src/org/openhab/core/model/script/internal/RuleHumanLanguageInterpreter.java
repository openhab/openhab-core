/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.model.script.internal;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.types.StringType;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.osgi.framework.Constants;
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
 * @author Kai Kreuzer - Initial contribution
 */
@Component(immediate = true, service = HumanLanguageInterpreter.class, configurationPid = "org.openhab.rulehli", property = {
        Constants.SERVICE_PID + "=org.openhab.rulehli", ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=voice",
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=Rule Voice Interpreter",
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=" + RuleHumanLanguageInterpreter.CONFIG_URI })
public class RuleHumanLanguageInterpreter implements HumanLanguageInterpreter {

    private final Logger logger = LoggerFactory.getLogger(RuleHumanLanguageInterpreter.class);

    // constants for the configuration properties
    protected static final String CONFIG_URI = "voice:rulehli";

    private String itemName = "VoiceCommand";

    private final EventPublisher eventPublisher;

    @Activate
    public RuleHumanLanguageInterpreter(final @Reference EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

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

}
