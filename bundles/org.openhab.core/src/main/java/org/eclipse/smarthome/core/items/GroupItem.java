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
package org.eclipse.smarthome.core.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.UnitProvider;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class GroupItem extends GenericItem implements StateChangeListener {

    public static final String TYPE = "Group";

    private final Logger logger = LoggerFactory.getLogger(GroupItem.class);

    protected @Nullable final Item baseItem;

    protected final CopyOnWriteArrayList<Item> members;

    protected @Nullable GroupFunction function;

    /**
     * Creates a plain GroupItem
     *
     * @param name name of the group
     */
    public GroupItem(String name) {
        this(name, null, null);
    }

    public GroupItem(String name, @Nullable Item baseItem) {
        // only baseItem but no function set -> use Equality
        this(name, baseItem, new GroupFunction.Equality());
    }

    /**
     * Creates a GroupItem with function
     *
     * @param name name of the group
     * @param baseItem type of items in the group
     * @param function function to calculate group status out of member status
     */
    public GroupItem(String name, @Nullable Item baseItem, @Nullable GroupFunction function) {
        super(TYPE, name);

        // we only allow GroupItem with BOTH, baseItem AND function set, or NONE of them set
        if (baseItem == null || function == null) {
            this.baseItem = null;
            this.function = null;
        } else {
            this.function = function;
            this.baseItem = baseItem;
        }

        members = new CopyOnWriteArrayList<Item>();
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Item member : getMembers()) {
            unregisterStateListener(member);
        }
        members.clear();
    }

    /**
     * Returns the base item of this {@link GroupItem}. This method is only
     * intended to allow instance checks of the underlying BaseItem. It must
     * not be changed in any way.
     *
     * @return the base item of this GroupItem
     */
    public @Nullable Item getBaseItem() {
        return baseItem;
    }

    /**
     * Returns the function of this {@link GroupItem}.
     *
     * @return the function of this GroupItem
     */
    public @Nullable GroupFunction getFunction() {
        return function;
    }

    /**
     * Returns the direct members of this {@link GroupItem} regardless if these
     * members are {@link GroupItem}s as well.
     *
     * @return the direct members of this {@link GroupItem}
     */
    public Set<Item> getMembers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(members));
    }

    /**
     * Returns the direct members of this {@link GroupItem} and recursively all
     * members of the potentially contained {@link GroupItem}s as well. The {@link GroupItem}s itself aren't contained.
     * The returned items are unique.
     *
     * @return all members of this and all contained {@link GroupItem}s
     */
    public Set<Item> getAllMembers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(getMembers((Item i) -> !(i instanceof GroupItem))));
    }

    private void collectMembers(Collection<Item> allMembers, Collection<Item> members) {
        for (Item member : members) {
            if (allMembers.contains(member)) {
                continue;
            }
            allMembers.add(member);
            if (member instanceof GroupItem) {
                collectMembers(allMembers, ((GroupItem) member).members);
            }
        }
    }

    /**
     * Retrieves ALL members of this group and filters it with the given Predicate
     *
     * @param filterItem Predicate with settings to filter member list
     * @return Set of member items filtered by filterItem
     */
    public Set<Item> getMembers(Predicate<Item> filterItem) {
        Set<Item> allMembers = new LinkedHashSet<Item>();
        collectMembers(allMembers, members);
        return allMembers.stream().filter(filterItem).collect(Collectors.toSet());
    }

    /**
     * Adds the given item to the members of this group item.
     *
     * @param item the item to be added (must not be null)
     * @throws IllegalArgumentException if the given item is null
     */
    public void addMember(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null!");
        }

        boolean added = members.addIfAbsent(item);

        // in case membership is constructed programmatically this sanitises
        // the group names on the item:
        if (added && item instanceof GenericItem) {
            ((GenericItem) item).addGroupName(this.getName());
        }
        registerStateListener(item);
    }

    private void registerStateListener(Item item) {
        if (item instanceof GenericItem) {
            GenericItem genericItem = (GenericItem) item;
            genericItem.addStateChangeListener(this);
        }
    }

    private void unregisterStateListener(Item old) {
        if (old instanceof GenericItem) {
            GenericItem genericItem = (GenericItem) old;
            genericItem.removeStateChangeListener(this);
        }
    }

    public void replaceMember(Item oldItem, Item newItem) {
        if (oldItem == null || newItem == null) {
            throw new IllegalArgumentException("Items must not be null!");
        }
        int index = members.indexOf(oldItem);
        if (index > -1) {
            Item old = members.set(index, newItem);
            unregisterStateListener(old);
        }
        registerStateListener(newItem);
    }

    /**
     * Removes the given item from the members of this group item.
     *
     * @param item the item to be removed (must not be null)
     * @throws IllegalArgumentException if the given item is null
     */
    public void removeMember(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null!");
        }
        members.remove(item);
        unregisterStateListener(item);
    }

    @Override
    public void setUnitProvider(@Nullable UnitProvider unitProvider) {
        super.setUnitProvider(unitProvider);
        if (baseItem != null && baseItem instanceof GenericItem) {
            ((GenericItem) baseItem).setUnitProvider(unitProvider);
        }
    }

    /**
     * The accepted data types of a group item is the same as of the underlying base item.
     * If none is defined, the intersection of all sets of accepted data types of all group
     * members is used instead.
     *
     * @return the accepted data types of this group item
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Class<? extends State>> getAcceptedDataTypes() {
        if (baseItem != null) {
            return baseItem.getAcceptedDataTypes();
        } else {
            List<Class<? extends State>> acceptedDataTypes = null;

            for (Item item : members) {
                if (acceptedDataTypes == null) {
                    acceptedDataTypes = new ArrayList<>(item.getAcceptedDataTypes());
                } else {
                    acceptedDataTypes.retainAll(item.getAcceptedDataTypes());
                }
            }
            return acceptedDataTypes == null ? Collections.unmodifiableList(Collections.EMPTY_LIST)
                    : Collections.unmodifiableList(acceptedDataTypes);
        }
    }

    /**
     * The accepted command types of a group item is the same as of the underlying base item.
     * If none is defined, the intersection of all sets of accepted command types of all group
     * members is used instead.
     *
     * @return the accepted command types of this group item
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        if (baseItem != null) {
            return baseItem.getAcceptedCommandTypes();
        } else {
            List<Class<? extends Command>> acceptedCommandTypes = null;

            for (Item item : members) {
                if (acceptedCommandTypes == null) {
                    acceptedCommandTypes = new ArrayList<>(item.getAcceptedCommandTypes());
                } else {
                    acceptedCommandTypes.retainAll(item.getAcceptedCommandTypes());
                }
            }
            return acceptedCommandTypes == null ? Collections.unmodifiableList(Collections.EMPTY_LIST)
                    : Collections.unmodifiableList(acceptedCommandTypes);
        }
    }

    public void send(Command command) {
        if (getAcceptedCommandTypes().contains(command.getClass())) {
            internalSend(command);
        } else {
            logger.warn("Command '{}' has been ignored for group '{}' as it is not accepted.", command.toString(),
                    getName());
        }
    }

    @Override
    protected void internalSend(Command command) {
        if (eventPublisher != null) {
            for (Item member : members) {
                // try to send the command to the bus
                eventPublisher.post(ItemEventFactory.createCommandEvent(member.getName(), command));
            }
        }
    }

    @Override
    public <T extends State> @Nullable T getStateAs(Class<T> typeClass) {
        // if a group does not have a function it cannot have a state
        @Nullable
        T newState = null;
        if (function != null) {
            newState = function.getStateAs(getStateMembers(getMembers()), typeClass);
        }

        if (newState == null && baseItem != null && baseItem instanceof GenericItem) {
            // we use the transformation method from the base item
            ((GenericItem) baseItem).setState(state);
            newState = baseItem.getStateAs(typeClass);
        }
        if (newState == null) {
            newState = super.getStateAs(typeClass);
        }
        return newState;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(" (");
        sb.append("Type=");
        sb.append(getClass().getSimpleName());
        sb.append(", ");
        if (getBaseItem() != null) {
            sb.append("BaseType=");
            sb.append(baseItem.getClass().getSimpleName());
            sb.append(", ");
        }
        sb.append("Members=");
        sb.append(members.size());
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

    @Override
    public void stateChanged(Item item, State oldState, State newState) {
    }

    @Override
    public void stateUpdated(Item item, State state) {
        State oldState = this.state;
        if (function != null && baseItem != null) {
            State calculatedState = function.calculate(getStateMembers(getMembers()));
            calculatedState = itemStateConverter.convertToAcceptedState(calculatedState, baseItem);
            setState(calculatedState);
        }
        if (!oldState.equals(this.state)) {
            sendGroupStateChangedEvent(item.getName(), this.state, oldState);
        }
    }

    @Override
    public void setState(State state) {
        State oldState = this.state;
        if (baseItem != null && baseItem instanceof GenericItem) {
            ((GenericItem) baseItem).setState(state);
            this.state = baseItem.getState();
        } else {
            this.state = state;
        }
        notifyListeners(oldState, state);
    }

    private void sendGroupStateChangedEvent(String memberName, State newState, State oldState) {
        if (eventPublisher != null) {
            eventPublisher.post(
                    ItemEventFactory.createGroupStateChangedEvent(this.getName(), memberName, newState, oldState));
        }
    }

    private Set<Item> getStateMembers(Set<Item> items) {
        Set<Item> result = new HashSet<>();
        collectStateMembers(result, items);

        // filter out group items w/o state. we had those in to detect cyclic membership.
        return result.stream().filter(i -> !isGroupItem(i) || hasOwnState((GroupItem) i)).collect(Collectors.toSet());
    }

    private void collectStateMembers(Set<Item> result, Set<Item> items) {
        for (Item item : items) {
            if (result.contains(item)) {
                continue;
            }
            if (!isGroupItem(item) || (isGroupItem(item) && hasOwnState((GroupItem) item))) {
                result.add(item);
            } else {
                result.add(item); // also add group items w/o state to detect cyclic membership.
                collectStateMembers(result, ((GroupItem) item).getMembers());
            }
        }
    }

    private boolean isGroupItem(Item item) {
        return item instanceof GroupItem;
    }

    private boolean hasOwnState(GroupItem item) {
        return item.getFunction() != null && item.getBaseItem() != null;
    }

}
