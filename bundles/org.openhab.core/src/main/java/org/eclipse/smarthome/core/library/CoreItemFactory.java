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
package org.eclipse.smarthome.core.library;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.ItemFactory;
import org.eclipse.smarthome.core.items.ItemUtil;
import org.eclipse.smarthome.core.library.items.CallItem;
import org.eclipse.smarthome.core.library.items.ColorItem;
import org.eclipse.smarthome.core.library.items.ContactItem;
import org.eclipse.smarthome.core.library.items.DateTimeItem;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.ImageItem;
import org.eclipse.smarthome.core.library.items.LocationItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.PlayerItem;
import org.eclipse.smarthome.core.library.items.RollershutterItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.osgi.service.component.annotations.Component;

/**
 * {@link CoreItemFactory}-Implementation for the core ItemTypes
 *
 * @author Thomas.Eichstaedt-Engelen
 * @author Kai Kreuzer
 * @author Alexander Kostadinov
 */
@Component(immediate = true)
@NonNullByDefault
public class CoreItemFactory implements ItemFactory {

    public static final String CALL = "Call";
    public static final String COLOR = "Color";
    public static final String CONTACT = "Contact";
    public static final String DATETIME = "DateTime";
    public static final String DIMMER = "Dimmer";
    public static final String IMAGE = "Image";
    public static final String LOCATION = "Location";
    public static final String NUMBER = "Number";
    public static final String PLAYER = "Player";
    public static final String ROLLERSHUTTER = "Rollershutter";
    public static final String STRING = "String";
    public static final String SWITCH = "Switch";

    @Override
    public @Nullable GenericItem createItem(@Nullable String itemTypeName, String itemName) {
        if (itemTypeName == null) {
            return null;
        }

        String itemType = ItemUtil.getMainItemType(itemTypeName);
        switch (itemType) {
            case CALL:
                return new CallItem(itemName);
            case COLOR:
                return new ColorItem(itemName);
            case CONTACT:
                return new ContactItem(itemName);
            case DATETIME:
                return new DateTimeItem(itemName);
            case DIMMER:
                return new DimmerItem(itemName);
            case IMAGE:
                return new ImageItem(itemName);
            case LOCATION:
                return new LocationItem(itemName);
            case NUMBER:
                return new NumberItem(itemTypeName, itemName);
            case PLAYER:
                return new PlayerItem(itemName);
            case ROLLERSHUTTER:
                return new RollershutterItem(itemName);
            case STRING:
                return new StringItem(itemName);
            case SWITCH:
                return new SwitchItem(itemName);
            default:
                return null;
        }
    }

    @Override
    public String[] getSupportedItemTypes() {
        return new String[] { SWITCH, ROLLERSHUTTER, CONTACT, STRING, NUMBER, DIMMER, DATETIME, COLOR, IMAGE, PLAYER,
                LOCATION, CALL };
    }

}
