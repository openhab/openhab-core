/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.internal.items;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateDescriptionFragmentProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link StateDescriptionFragment} provider providing a default state pattern for items of type String,
 * DateTime and Number (with or without dimension).
 *
 * @author Laurent Garnier - initial contribution
 *
 */
@NonNullByDefault
@Component(service = { StateDescriptionFragmentProvider.class,
        DefaultStateDescriptionFragmentProvider.class }, immediate = true, property = { "service.ranking:Integer=-2" })
public class DefaultStateDescriptionFragmentProvider implements StateDescriptionFragmentProvider {

    private static final StateDescriptionFragment DEFAULT_STRING = StateDescriptionFragmentBuilder.create()
            .withPattern("%s").build();
    private static final StateDescriptionFragment DEFAULT_DATETIME = StateDescriptionFragmentBuilder.create()
            .withPattern("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS").build();
    private static final StateDescriptionFragment DEFAULT_NUMBER = StateDescriptionFragmentBuilder.create()
            .withPattern("%.0f").build();
    private static final StateDescriptionFragment DEFAULT_NUMBER_WITH_DIMENSION = StateDescriptionFragmentBuilder
            .create().withPattern("%.0f %unit%").build();

    private final Logger logger = LoggerFactory.getLogger(DefaultStateDescriptionFragmentProvider.class);

    private final Map<String, StateDescriptionFragment> stateDescriptionFragments = new ConcurrentHashMap<>();

    private Integer rank = -2; // takes less precedence than all other providers

    @Activate
    public DefaultStateDescriptionFragmentProvider(Map<String, Object> properties) {
        Object serviceRanking = properties.get(Constants.SERVICE_RANKING);
        if (serviceRanking instanceof Integer rankValue) {
            rank = rankValue;
        }
    }

    @Deactivate
    protected void deactivate() {
        stateDescriptionFragments.clear();
    }

    public void onItemAdded(Item item) {
        logger.trace("onItemAdded {} {}", item.getName(), item.getType());
        if (item instanceof GroupItem group) {
            Item baseItem = group.getBaseItem();
            if (baseItem != null) {
                onItemAdded(baseItem);
            }
        } else if (item.getType().startsWith(CoreItemFactory.NUMBER + ":")) {
            stateDescriptionFragments.put(item.getName(), DEFAULT_NUMBER_WITH_DIMENSION);
        } else {
            switch (item.getType()) {
                case CoreItemFactory.STRING:
                    stateDescriptionFragments.put(item.getName(), DEFAULT_STRING);
                    break;
                case CoreItemFactory.DATETIME:
                    stateDescriptionFragments.put(item.getName(), DEFAULT_DATETIME);
                    break;
                case CoreItemFactory.NUMBER:
                    stateDescriptionFragments.put(item.getName(), DEFAULT_NUMBER);
                    break;
                default:
                    stateDescriptionFragments.remove(item.getName());
            }
        }
    }

    public void onItemRemoved(Item item) {
        logger.trace("onItemRemoved {}", item.getName());
        stateDescriptionFragments.remove(item.getName());
    }

    @Override
    public @Nullable StateDescriptionFragment getStateDescriptionFragment(String itemName, @Nullable Locale locale) {
        return stateDescriptionFragments.get(itemName);
    }

    @Override
    public Integer getRank() {
        return rank;
    }
}
