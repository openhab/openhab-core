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

import java.io.InputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.voice.text.InterpretationException;
import org.openhab.nlp.ItemNamedAttribute.AttributeType;
import org.openhab.nlp.internal.IntentTrainer;

/**
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class LocaleSpecificIntentTrainer implements RegistryChangeListener<Item> {

    private @Nullable IntentTrainer intentTrainer = null;
    private @Nullable Locale currentLocale = null;
    private final String tokenizerId;

    public LocaleSpecificIntentTrainer(String tokenizerId) {
        this.tokenizerId = tokenizerId;

    }

    @Override
    public void added(Item element) {
        intentTrainer = null;
    }

    @Override
    public void removed(Item element) {
        intentTrainer = null;
    }

    @Override
    public void updated(Item oldElement, Item element) {
        intentTrainer = null;
    }

    public void resetTrainer() {
        intentTrainer = null;
    }

    public Locale getLocale() {
        Locale locale = currentLocale;
        if (locale == null) {
            throw new IllegalStateException();
        }
        return locale;
    }

    public IntentTrainer getIntentTrainer() {
        IntentTrainer trainer = intentTrainer;
        if (trainer == null) {
            throw new IllegalStateException();
        }
        return trainer;
    }

    public void changeLocale(Locale locale, ItemResolver itemResolver, Collection<Skill> skills)
            throws InterpretationException {

        if (locale.equals(currentLocale) && intentTrainer != null) {
            return;
        }

        try {
            itemResolver.setLocale(locale);
            intentTrainer = new IntentTrainer(locale.getLanguage(), skills.stream().sorted(new Comparator<Skill>() {

                @Override
                public int compare(Skill o1, Skill o2) {
                    if (o1.getIntentId().equals("get-status")) {
                        return -1;
                    }
                    if (o2.getIntentId().equals("get-status")) {
                        return 1;
                    }
                    return o1.getIntentId().compareTo(o2.getIntentId());
                }

            }).collect(Collectors.toList()), getNameSamples(itemResolver), tokenizerId);
            currentLocale = locale;
        } catch (Exception e) {
            InterpretationException fe = new InterpretationException(
                    "Error during trainer initialization: " + e.getMessage());
            fe.initCause(e);
            throw fe;
        }
    }

    /**
     * Get an {@link InputStream} of additional name samples to feed to
     * the {@link IntentTrainer} to improve the recognition.
     *
     * @return an OpenNLP compatible input stream with the tagged name samples on separate lines
     */
    protected InputStream getNameSamples(ItemResolver itemResolver) throws UnsupportedLanguageException {
        StringBuilder nameSamplesDoc = new StringBuilder();
        Map<Item, Set<ItemNamedAttribute>> itemAttributes = itemResolver.getAllItemNamedAttributes();

        Stream<ItemNamedAttribute> attributes = itemAttributes.values().stream().flatMap(a -> a.stream());

        attributes.forEach(attribute -> {
            if (attribute.getType() == AttributeType.LOCATION) {
                nameSamplesDoc.append(String.format("<START:location> %s <END>%n", attribute.getValue()));
            } else {
                nameSamplesDoc.append(String.format("<START:object> %s <END>%n", attribute.getValue()));
            }
        });

        return IOUtils.toInputStream(nameSamplesDoc.toString());
    }
}
