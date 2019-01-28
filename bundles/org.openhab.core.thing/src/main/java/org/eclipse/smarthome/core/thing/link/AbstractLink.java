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
package org.eclipse.smarthome.core.thing.link;

import org.eclipse.smarthome.core.common.registry.Identifiable;
import org.eclipse.smarthome.core.items.ItemUtil;
import org.eclipse.smarthome.core.thing.UID;

/**
 * {@link AbstractLink} is the abstract base class of all links.
 *
 * @author Dennis Nobel - Initial contribution
 */
public abstract class AbstractLink implements Identifiable<String> {

    /**
     * Returns the link ID for a given item name and UID
     *
     * @param itemName item name
     * @param uid UID
     * @return the item channel link ID
     */
    public static String getIDFor(String itemName, UID uid) {
        return itemName + " -> " + uid.toString();
    }

    private final String itemName;

    /**
     * Constructor.
     *
     * @param itemName the item name for the link
     * @throws IllegalArgumentException if the item name is invalid
     */
    public AbstractLink(String itemName) {
        ItemUtil.assertValidItemName(itemName);
        this.itemName = itemName;
    }

    AbstractLink() {
        this.itemName = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractLink) {
            AbstractLink link = (AbstractLink) obj;
            return this.getUID().equals(link.getUID());
        }
        return false;
    }

    /**
     * Returns the ID for the link.
     *
     * @return id (can not be null)
     */
    @Override
    public String getUID() {
        return getIDFor(getItemName(), getLinkedUID());
    }

    /**
     * Returns the item that is linked to the object.
     *
     * @return item name (can not be null)
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Returns the UID of the object, which is linked to the item.
     *
     * @return UID (can not be null)
     */
    public abstract UID getLinkedUID();

    @Override
    public int hashCode() {
        return this.itemName.hashCode() * this.getLinkedUID().hashCode();
    }

    @Override
    public String toString() {
        return getUID();
    }
}
