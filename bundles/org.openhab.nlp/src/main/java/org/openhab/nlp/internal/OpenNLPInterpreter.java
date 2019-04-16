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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.voice.text.HumanLanguageInterpreter;
import org.eclipse.smarthome.core.voice.text.InterpretationException;
import org.openhab.nlp.ChatReply;
import org.openhab.nlp.ComputeReply;
import org.openhab.nlp.ItemResolver;
import org.openhab.nlp.LocaleSpecificIntentTrainer;
import org.openhab.nlp.Skill;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The OpenNLP-based interpreter
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(service = HumanLanguageInterpreter.class, immediate = true, name = "org.openhab.opennlphli", property = {
        "service.config.description.uri=voice:opennlphli", "service.config.label=OpenNLP Interpreter",
        "service.config.category=voice" })
@NonNullByDefault
public class OpenNLPInterpreter implements HumanLanguageInterpreter {

    public static final Set<Locale> SUPPORTED_LOCALES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN)));

    private @NonNullByDefault({}) LocaleSpecificIntentTrainer localeIntentTrainer;
    private ComputeReply computeReply = new ComputeReply();

    @Reference
    private @NonNullByDefault({}) ItemResolver itemResolver;
    @Reference
    private @NonNullByDefault({}) EventPublisher eventPublisher;

    private Map<String, Skill> skills = new HashMap<String, Skill>();

    @Override
    public String getId() {
        return "opennlp";
    }

    @NonNullByDefault({})
    @Override
    public String getLabel(Locale locale) {
        return "OpenNLP Interpreter";
    }

    @NonNullByDefault({})
    @Override
    public @Nullable String interpret(Locale locale, String text) throws InterpretationException {
        return getReply(locale, text).answer;
    }

    public ChatReply getReply(Locale locale, String text) throws InterpretationException {
        localeIntentTrainer.changeLocale(locale, itemResolver, skills.values());
        ChatReply reply = computeReply.reply(localeIntentTrainer, text, itemResolver, skills);
        return reply;
    }

    @NonNullByDefault({})
    @Override
    public String getGrammar(Locale locale, String format) {
        throw new UnsupportedOperationException();
    }

    @NonNullByDefault({})
    @Override
    public Set<Locale> getSupportedLocales() {
        return SUPPORTED_LOCALES;
    }

    @NonNullByDefault({})
    @Override
    public Set<String> getSupportedGrammarFormats() {
        return Collections.emptySet();
    }

    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        itemRegistry.addRegistryChangeListener(localeIntentTrainer);
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        itemRegistry.removeRegistryChangeListener(localeIntentTrainer);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSkill(Skill skill) {
        this.skills.put(skill.getIntentId(), skill);
        localeIntentTrainer.resetTrainer();
    }

    protected void removeSkill(Skill skill) {
        this.skills.remove(skill.getIntentId());
        localeIntentTrainer.resetTrainer();
    }

    @Activate
    protected void activate(Map<String, Object> configProps) {
        modified(configProps);
    }

    @Modified
    protected void modified(Map<String, Object> configProps) {
        String tokenizerId;
        if (configProps.containsKey("tokenizer")) {
            tokenizerId = configProps.get("tokenizer").toString();
        } else {
            tokenizerId = "";
        }

        localeIntentTrainer = new LocaleSpecificIntentTrainer(tokenizerId);
    }
}
