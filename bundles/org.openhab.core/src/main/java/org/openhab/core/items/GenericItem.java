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
package org.openhab.core.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.service.CommandDescriptionService;
import org.openhab.core.service.StateDescriptionService;
import org.openhab.core.types.Command;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandDescriptionBuilder;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The abstract base class for all items. It provides all relevant logic
 * for the infrastructure, such as publishing updates to the event bus
 * or notifying listeners.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Andre Fuechsel - Added tags
 * @author Stefan Bußweiler - Migration to new ESH event concept
 */
@NonNullByDefault
public abstract class GenericItem implements ActiveItem {

    private final Logger logger = LoggerFactory.getLogger(GenericItem.class);

    private static final String ITEM_THREADPOOLNAME = "items";

    protected @Nullable EventPublisher eventPublisher;

    protected Set<StateChangeListener> listeners = new CopyOnWriteArraySet<>(
            Collections.newSetFromMap(new WeakHashMap<>()));

    protected List<String> groupNames = new ArrayList<>();

    protected Set<String> tags = new HashSet<>();

    protected final String name;

    protected final String type;

    protected State state = UnDefType.NULL;

    protected @Nullable String label;

    protected @Nullable String category;

    private @Nullable StateDescriptionService stateDescriptionService;

    private @Nullable CommandDescriptionService commandDescriptionService;

    protected @Nullable UnitProvider unitProvider;

    protected @Nullable ItemStateConverter itemStateConverter;

    public GenericItem(String type, String name) {
        this.name = name;
        this.type = type;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public <T extends State> @Nullable T getStateAs(Class<T> typeClass) {
        return state.as(typeClass);
    }

    @Override
    public String getUID() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public List<String> getGroupNames() {
        return Collections.unmodifiableList(new ArrayList<>(groupNames));
    }

    /**
     * Adds a group name to the {@link GenericItem}.
     *
     * @param groupItemName group item name to add
     * @throws IllegalArgumentException if groupItemName is {@code null}
     */
    @Override
    public void addGroupName(String groupItemName) {
        if (groupItemName == null) {
            throw new IllegalArgumentException("Group item name must not be null!");
        }
        if (!groupNames.contains(groupItemName)) {
            groupNames.add(groupItemName);
        }
    }

    @Override
    public void addGroupNames(String... groupItemNames) {
        for (String groupItemName : groupItemNames) {
            addGroupName(groupItemName);
        }
    }

    @Override
    public void addGroupNames(List<String> groupItemNames) {
        for (String groupItemName : groupItemNames) {
            addGroupName(groupItemName);
        }
    }

    /**
     * Removes a group item name from the {@link GenericItem}.
     *
     * @param groupItemName group item name to remove
     * @throws IllegalArgumentException if groupItemName is {@code null}
     */
    @Override
    public void removeGroupName(String groupItemName) {
        if (groupItemName == null) {
            throw new IllegalArgumentException("Group item name must not be null!");
        }
        groupNames.remove(groupItemName);
    }

    /**
     * Disposes this item. Clears all injected services and unregisters all change listeners.
     * This does not remove this item from its groups. Removing from groups should be done externally to retain the
     * member order in case this item is exchanged in a group.
     */
    public void dispose() {
        this.listeners.clear();
        this.eventPublisher = null;
        this.stateDescriptionService = null;
        this.commandDescriptionService = null;
        this.unitProvider = null;
        this.itemStateConverter = null;
    }

    public void setEventPublisher(@Nullable EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void setStateDescriptionService(@Nullable StateDescriptionService stateDescriptionService) {
        this.stateDescriptionService = stateDescriptionService;
    }

    public void setCommandDescriptionService(@Nullable CommandDescriptionService commandDescriptionService) {
        this.commandDescriptionService = commandDescriptionService;
    }

    public void setUnitProvider(@Nullable UnitProvider unitProvider) {
        this.unitProvider = unitProvider;
    }

    public void setItemStateConverter(@Nullable ItemStateConverter itemStateConverter) {
        this.itemStateConverter = itemStateConverter;
    }

    protected void internalSend(Command command) {
        // try to send the command to the bus
        if (eventPublisher != null) {
            eventPublisher.post(ItemEventFactory.createCommandEvent(this.getName(), command));
        }
    }

    /**
     * Set a new state.
     *
     * Subclasses may override this method in order to do necessary conversions upfront. Afterwards,
     * {@link #applyState(State)} should be called by classes overriding this method.
     *
     * @param state new state of this item
     */
    public void setState(State state) {
        applyState(state);
    }

    /**
     * Sets new state, notifies listeners and sends events.
     *
     * Classes overriding the {@link #setState(State)} method should call this method in order to actually set the
     * state, inform listeners and send the event.
     *
     * @param state new state of this item
     */
    protected final void applyState(State state) {
        State oldState = this.state;
        this.state = state;
        notifyListeners(oldState, state);
        if (!oldState.equals(state)) {
            sendStateChangedEvent(state, oldState);
        }
    }

    private void sendStateChangedEvent(State newState, State oldState) {
        if (eventPublisher != null) {
            eventPublisher.post(ItemEventFactory.createStateChangedEvent(this.name, newState, oldState));
        }
    }

    public void send(RefreshType command) {
        internalSend(command);
    }

    protected void notifyListeners(final State oldState, final State newState) {
        // if nothing has changed, we send update notifications
        Set<StateChangeListener> clonedListeners = new CopyOnWriteArraySet<>(listeners);
        ExecutorService pool = ThreadPoolManager.getPool(ITEM_THREADPOOLNAME);
        try {
            final boolean stateChanged = newState != null && !newState.equals(oldState);
            clonedListeners.forEach(listener ->
                pool.execute(() -> {
                    try {
                        listener.stateUpdated(GenericItem.this, newState);
                        if (stateChanged) {
                            listener.stateChanged(GenericItem.this, oldState, newState);
                        }
                    } catch (Exception e) {
                        logger.warn("failed notifying listener '{}' about state update of item {}: {}", listener,
                                GenericItem.this.getName(), e.getMessage(), e);
                    }
                }));
        } catch (IllegalArgumentException e) {
            logger.warn("failed comparing oldState '{}' to newState '{}' for item {}: {}", oldState,
                    newState, GenericItem.this.getName(), e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(" (");
        sb.append("Type=");
        sb.append(getClass().getSimpleName());
        sb.append(", ");
        sb.append("State=");
        sb.append(getState());
        sb.append(", ");
        sb.append("Label=");
        sb.append(getLabel());
        sb.append(", ");
        sb.append("Category=");
        sb.append(getCategory());
        if (!getTags().isEmpty()) {
            sb.append(", ");
            sb.append("Tags=[");
            sb.append(getTags().stream().collect(Collectors.joining(", ")));
            sb.append("]");
        }
        if (!getGroupNames().isEmpty()) {
            sb.append(", ");
            sb.append("Groups=[");
            sb.append(getGroupNames().stream().collect(Collectors.joining(", ")));
            sb.append("]");
        }
        sb.append(")");
        return sb.toString();
    }

    public void addStateChangeListener(StateChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeStateChangeListener(StateChangeListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GenericItem other = (GenericItem) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public Set<String> getTags() {
        return Collections.unmodifiableSet(new HashSet<>(tags));
    }

    @Override
    public boolean hasTag(String tag) {
        return tags.stream().anyMatch(t -> t.equalsIgnoreCase(tag));
    }

    @Override
    public void addTag(String tag) {
        tags.add(tag);
    }

    @Override
    public void addTags(Collection<String> tags) {
        this.tags.addAll(tags);
    }

    @Override
    public void addTags(String... tags) {
        this.tags.addAll(Arrays.asList(tags));
    }

    @Override
    public void removeTag(String tag) {
        tags.remove(tags.stream().filter(t -> t.equalsIgnoreCase(tag)).findFirst().orElse(tag));
    }

    @Override
    public void removeAllTags() {
        tags.clear();
    }

    @Override
    public @Nullable String getLabel() {
        return this.label;
    }

    @Override
    public void setLabel(@Nullable String label) {
        this.label = label;
    }

    @Override
    public @Nullable String getCategory() {
        return category;
    }

    @Override
    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    @Override
    public @Nullable StateDescription getStateDescription() {
        return getStateDescription(null);
    }

    @Override
    public @Nullable StateDescription getStateDescription(@Nullable Locale locale) {
        if (stateDescriptionService != null) {
            return stateDescriptionService.getStateDescription(this.name, locale);
        }
        return null;
    }

    @Override
    public @Nullable CommandDescription getCommandDescription(@Nullable Locale locale) {
        if (commandDescriptionService != null) {
            CommandDescription commandDescription = commandDescriptionService.getCommandDescription(this.name, locale);
            if (commandDescription != null) {
                return commandDescription;
            }
        }

        StateDescription stateDescription = getStateDescription(locale);
        if (stateDescription != null && !stateDescription.getOptions().isEmpty()) {
            return stateOptions2CommandOptions(stateDescription);
        }

        return null;
    }

    /**
     * Tests if state is within acceptedDataTypes list or a subclass of one of them
     *
     * @param acceptedDataTypes list of datatypes this items accepts as a state
     * @param state to be tested
     * @return true if state is an acceptedDataType or subclass thereof
     */
    public boolean isAcceptedState(List<Class<? extends State>> acceptedDataTypes, State state) {
        return acceptedDataTypes.stream().map(clazz -> clazz.isAssignableFrom(state.getClass())).filter(found -> found)
                .findAny().isPresent();
    }

    protected void logSetTypeError(State state) {
        logger.error("Tried to set invalid state {} ({}) on item {} of type {}, ignoring it", state,
                state.getClass().getSimpleName(), getName(), getClass().getSimpleName());
    }

    private @Nullable CommandDescription stateOptions2CommandOptions(StateDescription stateDescription) {
        CommandDescriptionBuilder builder = CommandDescriptionBuilder.create();
        stateDescription.getOptions()
                .forEach(so -> builder.withCommandOption(new CommandOption(so.getValue(), so.getLabel())));
        return builder.build();
    }

}
