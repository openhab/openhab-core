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

import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.service.BundleResolverImpl;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The {@link ItemUtil} class contains utility methods for {@link Item} objects.
 * <p>
 * This class cannot be instantiated, it only contains static methods.
 *
 * @author Michael Grammling - Initial contribution
 * @author Simon Kaufmann - added type conversion
 * @author Martin van Wingerden - when converting types convert null to UnDefType.NULL
 */
@NonNullByDefault
public class ItemUtil {

    public static final String EXTENSION_SEPARATOR = ":";

    /**
     * The constructor is private.
     * This class cannot be instantiated.
     */
    private ItemUtil() {
        // nothing to do
    }

    /**
     * Returns {@code true} if the specified name is a valid item name, otherwise {@code false}.
     * <p>
     * A valid item name must <i>only</i> only consists of the following characters:
     * <ul>
     * <li>a-z</li>
     * <li>A-Z</li>
     * <li>0..9</li>
     * <li>_ (underscore)</li>
     * </ul>
     *
     * @param itemName the name of the item to be checked (could be null or empty)
     * @return true if the specified name is a valid item name, otherwise false
     */
    public static boolean isValidItemName(final String itemName) {
        return StringUtils.isNotEmpty(itemName) && itemName.matches("[a-zA-Z0-9_]*");
    }

    /**
     * Ensures that the specified name of the item is valid.
     * <p>
     * If the name of the item is invalid an {@link IllegalArgumentException} is thrown, otherwise this method returns
     * silently.
     * <p>
     * A valid item name must <i>only</i> only consists of the following characters:
     * <ul>
     * <li>a-z</li>
     * <li>A-Z</li>
     * <li>0..9</li>
     * <li>_ (underscore)</li>
     * </ul>
     *
     * @param itemName the name of the item to be checked (could be null or empty)
     * @throws IllegalArgumentException if the name of the item is invalid
     */
    public static void assertValidItemName(String itemName) throws IllegalArgumentException {
        if (!isValidItemName(itemName)) {
            throw new IllegalArgumentException("The specified name of the item '" + itemName + "' is not valid!");
        }
    }

    /**
     * Get the main item type from an item type name. The name may consist of an extended item type where an extension
     * is separated by ":".
     *
     * @param itemTypeName the item type name, e.g. "Number:Temperature" or "Switch".
     * @return the main item type without the extension.
     */
    public static String getMainItemType(String itemTypeName) {
        Objects.requireNonNull(itemTypeName);

        if (itemTypeName.contains(EXTENSION_SEPARATOR)) {
            return itemTypeName.substring(0, itemTypeName.indexOf(EXTENSION_SEPARATOR));
        }

        return itemTypeName;
    }

    /**
     * Get the optional extension from an item type name.
     *
     * @param itemTypeName the item type name, e.g. "Number:Temperature" or "Switch".
     * @return the extension from the item type name, {@code null} in case no extension is defined.
     */
    public static @Nullable String getItemTypeExtension(@Nullable String itemTypeName) {
        if (itemTypeName == null) {
            return null;
        }
        if (itemTypeName.contains(ItemUtil.EXTENSION_SEPARATOR)) {
            return itemTypeName.substring(itemTypeName.indexOf(ItemUtil.EXTENSION_SEPARATOR) + 1);
        }

        return null;
    }

    /**
     * @deprecated use DS service {@link ItemStateConverter#convertToAcceptedState(State, Item)} instead.
     */
    @Deprecated
    public static @Nullable State convertToAcceptedState(@Nullable State state, Item item) {
        BundleContext bundleContext = new BundleResolverImpl().resolveBundle(ItemUtil.class).getBundleContext();
        ServiceReference<ItemStateConverter> service = bundleContext.getServiceReference(ItemStateConverter.class);
        if (service == null) {
            return null;
        }

        ItemStateConverter itemStateConverter = bundleContext.getService(service);
        if (itemStateConverter == null) {
            return null;
        }

        return itemStateConverter.convertToAcceptedState(state, item);
    }

}
