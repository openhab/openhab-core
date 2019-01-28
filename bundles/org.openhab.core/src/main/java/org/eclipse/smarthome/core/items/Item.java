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

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.common.registry.Identifiable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.UnDefType;

/**
 * <p>
 * This interface defines the core features of an Eclipse SmartHome item.
 *
 * <p>
 * Item instances are used for all stateful services and are especially important for the {@link ItemRegistry}.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault
public interface Item extends Identifiable<String> {

    /**
     * returns the current state of the item
     *
     * @return the current state
     */
    public State getState();

    /**
     * returns the current state of the item as a specific type
     *
     * @return the current state in the requested type or
     *         null, if state cannot be provided as the requested type
     */
    public <T extends State> @Nullable T getStateAs(Class<T> typeClass);

    /**
     * returns the name of the item
     *
     * @return the name of the item
     */
    public String getName();

    /**
     * returns the item type as defined by {@link ItemFactory}s
     *
     * @return the item type
     */
    public String getType();

    /**
     * <p>
     * This method provides a list of all data types that can be used to update the item state
     *
     * <p>
     * Imagine e.g. a dimmer device: It's status could be 0%, 10%, 50%, 100%, but also OFF or ON and maybe UNDEFINED. So
     * the accepted data types would be in this case {@link PercentType}, {@link OnOffType} and {@link UnDefType}
     *
     * <p>
     * The order of data types denotes the order of preference. So in case a state needs to be converted
     * in order to be accepted, it will be attempted to convert it to a type from top to bottom. Therefore
     * the type with the least information loss should be on top of the list - in the example above the
     * {@link PercentType} carries more information than the {@link OnOffType}, hence it is listed first.
     *
     * @return a list of data types that can be used to update the item state
     */
    public List<Class<? extends State>> getAcceptedDataTypes();

    /**
     * <p>
     * This method provides a list of all command types that can be used for this item
     *
     * <p>
     * Imagine e.g. a dimmer device: You could ask it to dim to 0%, 10%, 50%, 100%, but also to turn OFF or ON. So the
     * accepted command types would be in this case {@link PercentType}, {@link OnOffType}
     *
     *
     * @return a list of all command types that can be used for this item
     */
    public List<Class<? extends Command>> getAcceptedCommandTypes();

    /**
     * Returns a list of the names of the groups this item belongs to.
     *
     * @return list of item group names
     */
    public List<String> getGroupNames();

    /**
     * Returns a set of tags. If the item is not tagged, an empty set is returned.
     *
     * @return set of tags.
     */
    public Set<String> getTags();

    /**
     * Returns the label of the item or null if no label is set.
     *
     * @return item label or null
     */
    public @Nullable String getLabel();

    /**
     * Returns true if the item's tags contains the specific tag, otherwise false.
     *
     * @param tag a tag whose presence in the item's tags is to be tested.
     * @return true if the item's tags contains the specific tag, otherwise false.
     */
    public boolean hasTag(String tag);

    /**
     * Returns the category of the item or null if no category is set.
     *
     * @return category or null
     */
    public @Nullable String getCategory();

    /**
     * Returns the first provided state description (uses the default locale).
     * If options are defined on the channel, they are included in the returned state description.
     *
     * @return state description (can be null)
     */
    public @Nullable StateDescription getStateDescription();

    /**
     * Returns the first provided state description for a given locale.
     * If options are defined on the channel, they are included in the returned state description.
     *
     * @param locale locale (can be null)
     * @return state description (can be null)
     */
    public @Nullable StateDescription getStateDescription(@Nullable Locale locale);

}
