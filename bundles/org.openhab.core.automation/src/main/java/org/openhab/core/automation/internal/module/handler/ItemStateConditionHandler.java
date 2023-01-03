/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module.handler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseConditionModuleHandler;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.events.TopicPrefixEventFilter;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemAddedEvent;
import org.openhab.core.items.events.ItemRemovedEvent;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConditionHandler implementation to check item state
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 */
@NonNullByDefault
public class ItemStateConditionHandler extends BaseConditionModuleHandler implements EventSubscriber {

    /**
     * Constants for Config-Parameters corresponding to Definition in ItemModuleTypeDefinition.json
     */
    public static final String ITEM_NAME = "itemName";
    public static final String OPERATOR = "operator";
    public static final String STATE = "state";

    private final Logger logger = LoggerFactory.getLogger(ItemStateConditionHandler.class);

    public static final String ITEM_STATE_CONDITION = "core.ItemStateCondition";

    private final ItemRegistry itemRegistry;
    private final String ruleUID;
    private final String itemName;
    private final EventFilter eventFilter;
    private final BundleContext bundleContext;
    private final Set<String> types;
    private final ServiceRegistration<?> eventSubscriberRegistration;
    private final TimeZoneProvider timeZoneProvider;

    public ItemStateConditionHandler(Condition condition, String ruleUID, BundleContext bundleContext,
            ItemRegistry itemRegistry, TimeZoneProvider timeZoneProvider) {
        super(condition);
        this.itemRegistry = itemRegistry;
        this.bundleContext = bundleContext;
        this.timeZoneProvider = timeZoneProvider;
        this.itemName = (String) module.getConfiguration().get(ITEM_NAME);
        this.eventFilter = new TopicPrefixEventFilter("openhab/items/" + itemName + "/");
        this.types = Set.of(ItemAddedEvent.TYPE, ItemRemovedEvent.TYPE);
        this.ruleUID = ruleUID;

        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this, null);

        if (itemRegistry.get(itemName) == null) {
            logger.warn("Item '{}' needed for rule '{}' is missing. Condition '{}' will not work.", itemName, ruleUID,
                    module.getId());
        }
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return eventFilter;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemAddedEvent) {
            if (itemName.equals(((ItemAddedEvent) event).getItem().name)) {
                logger.info("Item '{}' needed for rule '{}' added. Condition '{}' will now work.", itemName, ruleUID,
                        module.getId());
                return;
            }
        } else if (event instanceof ItemRemovedEvent) {
            if (itemName.equals(((ItemRemovedEvent) event).getItem().name)) {
                logger.warn("Item '{}' needed for rule '{}' removed. Condition '{}' will no longer work.", itemName,
                        ruleUID, module.getId());
                return;
            }
        }
    }

    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {
        String state = (String) module.getConfiguration().get(STATE);
        String operator = (String) module.getConfiguration().get(OPERATOR);
        if (operator == null || state == null || itemName == null) {
            logger.error("Module is not well configured: itemName={}  operator={}  state = {}", itemName, operator,
                    state);
            return false;
        }
        try {
            logger.debug("ItemStateCondition '{}' checking if {} {} {}", module.getId(), itemName, operator, state);
            switch (operator) {
                case "=":
                    return equalsToItemState(itemName, state);
                case "!=":
                    return !equalsToItemState(itemName, state);
                case "<":
                    return !greaterThanOrEqualsToItemState(itemName, state);
                case "<=":
                case "=<":
                    return lessThanOrEqualsToItemState(itemName, state);
                case ">":
                    return !lessThanOrEqualsToItemState(itemName, state);
                case ">=":
                case "=>":
                    return greaterThanOrEqualsToItemState(itemName, state);
            }
        } catch (ItemNotFoundException e) {
            logger.error("Item with name {} not found in ItemRegistry.", itemName);
        }
        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked", "null" })
    private boolean lessThanOrEqualsToItemState(String itemName, String state) throws ItemNotFoundException {
        Item item = itemRegistry.getItem(itemName);
        State compareState = TypeParser.parseState(item.getAcceptedDataTypes(), state);
        State itemState = item.getState();
        if (itemState instanceof DateTimeType) {
            ZonedDateTime itemTime = ((DateTimeType) itemState).getZonedDateTime();
            ZonedDateTime compareTime = getCompareTime(state);
            return itemTime.compareTo(compareTime) <= 0;
        } else if (itemState instanceof QuantityType) {
            QuantityType qtState = (QuantityType) itemState;
            if (compareState instanceof DecimalType) {
                // allow compareState without unit -> implicitly assume its the same as the one from the
                // state, but warn the user
                if (!Units.ONE.equals(qtState.getUnit())) {
                    logger.warn(
                            "Received a QuantityType state '{}' with unit for item {}, but the condition is defined as a plain number without unit ({}), please consider adding a unit to the condition.",
                            qtState, itemName, state);
                }
                return qtState.compareTo(
                        new QuantityType<>(((DecimalType) compareState).toBigDecimal(), qtState.getUnit())) <= 0;
            } else if (compareState instanceof QuantityType) {
                return qtState.compareTo((QuantityType) compareState) <= 0;
            }
        } else if (itemState instanceof PercentType && null != compareState) {
            // we need to handle PercentType first, otherwise the comparison will fail
            PercentType percentState = compareState.as(PercentType.class);
            if (null != percentState) {
                return ((PercentType) itemState).compareTo(percentState) <= 0;
            }
        } else if (itemState instanceof DecimalType && null != compareState) {
            DecimalType decimalState = compareState.as(DecimalType.class);
            if (null != decimalState) {
                return ((DecimalType) itemState).compareTo(decimalState) <= 0;
            }
        }
        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked", "null" })
    private boolean greaterThanOrEqualsToItemState(String itemName, String state) throws ItemNotFoundException {
        Item item = itemRegistry.getItem(itemName);
        State compareState = TypeParser.parseState(item.getAcceptedDataTypes(), state);
        State itemState = item.getState();
        if (itemState instanceof DateTimeType) {
            ZonedDateTime itemTime = ((DateTimeType) itemState).getZonedDateTime();
            ZonedDateTime compareTime = getCompareTime(state);
            return itemTime.compareTo(compareTime) >= 0;
        } else if (itemState instanceof QuantityType) {
            QuantityType qtState = (QuantityType) itemState;
            if (compareState instanceof DecimalType) {
                // allow compareState without unit -> implicitly assume its the same as the one from the
                // state, but warn the user
                if (!Units.ONE.equals(qtState.getUnit())) {
                    logger.warn(
                            "Received a QuantityType state '{}' with unit for item {}, but the condition is defined as a plain number without unit ({}), please consider adding a unit to the condition.",
                            qtState, itemName, state);
                }
                return qtState.compareTo(
                        new QuantityType<>(((DecimalType) compareState).toBigDecimal(), qtState.getUnit())) >= 0;
            } else if (compareState instanceof QuantityType) {
                return qtState.compareTo((QuantityType) compareState) >= 0;
            }
        } else if (itemState instanceof PercentType && null != compareState) {
            // we need to handle PercentType first, otherwise the comparison will fail
            PercentType percentState = compareState.as(PercentType.class);
            if (null != percentState) {
                return ((PercentType) itemState).compareTo(percentState) >= 0;
            }
        } else if (itemState instanceof DecimalType && null != compareState) {
            DecimalType decimalState = compareState.as(DecimalType.class);
            if (null != decimalState) {
                return ((DecimalType) itemState).compareTo(decimalState) >= 0;
            }
        }
        return false;
    }

    private boolean equalsToItemState(String itemName, String state) throws ItemNotFoundException {
        Item item = itemRegistry.getItem(itemName);
        State compareState = TypeParser.parseState(item.getAcceptedDataTypes(), state);
        State itemState = item.getState();
        if (itemState instanceof QuantityType && compareState instanceof DecimalType) {
            QuantityType<?> qtState = (QuantityType<?>) itemState;
            if (Units.ONE.equals(qtState.getUnit())) {
                // allow compareStates without unit if the unit of the state equals to ONE
                return itemState
                        .equals(new QuantityType<>(((DecimalType) compareState).toBigDecimal(), qtState.getUnit()));
            } else {
                // log a warning if the unit of the state differs from ONE
                logger.warn(
                        "Received a QuantityType state '{}' with unit for item {}, but the condition is defined as a plain number without unit ({}), comparison will fail unless a unit is added to the condition.",
                        itemState, itemName, state);
                return false;
            }
        }
        return itemState.equals(compareState);
    }

    @Override
    public void dispose() {
        super.dispose();
        eventSubscriberRegistration.unregister();
    }

    private ZonedDateTime getCompareTime(String input) {
        if (input.isBlank()) {
            // no parameter given, use now
            return ZonedDateTime.now();
        }
        try {
            return ZonedDateTime.parse(input);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(input, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(timeZoneProvider.getTimeZone());
        } catch (DateTimeParseException ignored) {
        }
        try {
            int dayPosition = input.indexOf("D");
            if (dayPosition == -1) {
                // no date in string, add period symbol and time separator
                return ZonedDateTime.now().plus(Duration.parse("PT" + input));
            } else if (dayPosition == input.length() - 1) {
                // day is the last symbol, only add the period symbol
                return ZonedDateTime.now().plus(Duration.parse("P" + input));
            } else {
                // add period symbol and time separator
                return ZonedDateTime.now().plus(Duration
                        .parse("P" + input.substring(0, dayPosition + 1) + "T" + input.substring(dayPosition + 1)));
            }
        } catch (DateTimeParseException e) {
            logger.warn("Couldn't get a comparable time from '{}', using now", input);
        }
        return ZonedDateTime.now();
    }
}
