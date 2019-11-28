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
package org.openhab.core.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class GroupItem extends GenericItem implements StateChangeListener {

    private final Logger logger = LoggerFactory.getLogger(GroupItem.class);

    protected final GenericItem baseItem;

    protected final List<Item> members;

    protected GroupFunction function;

    public GroupItem(String name) {
        this(name, null);
    }

    public GroupItem(String name, GenericItem baseItem) {
        this(name, baseItem, new GroupFunction.Equality());
    }

    public GroupItem(String name, GenericItem baseItem, GroupFunction function) {
        super(name);
        members = new CopyOnWriteArrayList<>();
        this.function = function;
        this.baseItem = baseItem;
    }

    /**
     * Returns the base item of this {@link GroupItem}. This method is only
     * intended to allow instance checks of the underlying BaseItem. It must
     * not be changed in any way.
     *
     * @return the base item of this GroupItem
     */
    public GenericItem getBaseItem() {
        return baseItem;
    }

    /**
     * Returns the direct members of this {@link GroupItem} regardless if these
     * members are {@link GroupItem}s as well.
     *
     * @return the direct members of this {@link GroupItem}
     */
    public List<Item> getMembers() {
        return members;
    }

    /**
     * Returns the direct members of this {@link GroupItem} and recursively all
     * members of the potentially contained {@link GroupItem}s as well. The
     * {@link GroupItem}s itself aren't contained. The returned items are unique.
     *
     * @return all members of this and all contained {@link GroupItem}s
     */
    public List<Item> getAllMembers() {
        Set<Item> allMembers = new HashSet<>();
        collectMembers(allMembers, members);
        return new ArrayList<>(allMembers);
    }

    private void collectMembers(Set<Item> allMembers, List<Item> members) {
        for (Item member : members) {
            if (member instanceof GroupItem) {
                collectMembers(allMembers, ((GroupItem) member).members);
            } else {
                allMembers.add(member);
            }
        }
    }

    public void addMember(Item item) {
        members.add(item);
        if (item instanceof GenericItem) {
            GenericItem genericItem = (GenericItem) item;
            genericItem.addStateChangeListener(this);
        }
    }

    public void removeMember(Item item) {
        members.remove(item);
        if (item instanceof GenericItem) {
            GenericItem genericItem = (GenericItem) item;
            genericItem.removeStateChangeListener(this);
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
    public List<Class<? extends State>> getAcceptedDataTypes() {
        if (baseItem != null) {
            return baseItem.getAcceptedDataTypes();
        } else {
            List<Class<? extends State>> acceptedDataTypes = null;
            for (Item item : members) {
                if (acceptedDataTypes == null || acceptedDataTypes.isEmpty()) {
                    acceptedDataTypes = item.getAcceptedDataTypes();
                } else {
                    acceptedDataTypes = item.getAcceptedDataTypes().stream().distinct()
                            .filter(acceptedDataTypes::contains).collect(Collectors.toList());
                }
            }
            return acceptedDataTypes == null ? Collections.emptyList() : acceptedDataTypes;
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
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        if (baseItem != null) {
            return baseItem.getAcceptedCommandTypes();
        } else {
            List<Class<? extends Command>> acceptedCommandTypes = null;
            for (Item item : members) {
                if (acceptedCommandTypes == null || acceptedCommandTypes.isEmpty()) {
                    acceptedCommandTypes = item.getAcceptedCommandTypes();
                } else {
                    acceptedCommandTypes = item.getAcceptedCommandTypes().stream().distinct()
                            .filter(acceptedCommandTypes::contains).collect(Collectors.toList());
                }
            }
            return acceptedCommandTypes == null ? Collections.emptyList() : acceptedCommandTypes;
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
                eventPublisher.sendCommand(member.getName(), command);
            }
        }
    }

    @Override
    public State getStateAs(Class<? extends State> typeClass) {
        State newState = function.getStateAs(getAllMembers(), typeClass);
        if (newState == null && baseItem != null) {
            // we use the transformation method from the base item
            baseItem.setState(state);
            newState = baseItem.getStateAs(typeClass);
        }
        if (newState == null) {
            newState = super.getStateAs(typeClass);
        }
        return newState;
    }

    @Override
    public String toString() {
        return getName() + " (" + "Type=" + getClass().getSimpleName() + ", "
                + (baseItem != null ? "BaseType=" + baseItem.getClass().getSimpleName() + ", " : "") + "Members="
                + members.size() + ", " + "State=" + getState() + ")";
    }

    @Override
    public void stateChanged(Item item, State oldState, State newState) {
        setState(function.calculate(members));
    }

    @Override
    public void stateUpdated(Item item, State state) {
        setState(function.calculate(members));
    }
}
