/*
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
package org.openhab.core.library;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemFactory;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.library.items.CallItem;
import org.openhab.core.library.items.ColorItem;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.ImageItem;
import org.openhab.core.library.items.LocationItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.PlayerItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link CoreItemFactory}-Implementation for the core ItemTypes
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Kai Kreuzer - Initial contribution
 * @author Alexander Kostadinov - Initial contribution
 */
@Component(immediate = true, service = { CoreItemFactory.class, ItemFactory.class })
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

    public static final Set<String> VALID_ITEM_TYPES = Set.of( //
            CALL, COLOR, CONTACT, DATETIME, DIMMER, IMAGE, LOCATION, NUMBER, PLAYER, ROLLERSHUTTER, STRING, SWITCH);

    private final UnitProvider unitProvider;

    @Activate
    public CoreItemFactory(final @Reference UnitProvider unitProvider) {
        this.unitProvider = unitProvider;
    }

    @Override
    public @Nullable GenericItem createItem(@Nullable String itemTypeName, String itemName) {
        if (itemTypeName == null) {
            return null;
        }

        String itemType = ItemUtil.getMainItemType(itemTypeName);
        return switch (itemType) {
            case CALL -> new CallItem(itemName);
            case COLOR -> new ColorItem(itemName);
            case CONTACT -> new ContactItem(itemName);
            case DATETIME -> new DateTimeItem(itemName);
            case DIMMER -> new DimmerItem(itemName);
            case IMAGE -> new ImageItem(itemName);
            case LOCATION -> new LocationItem(itemName);
            case NUMBER -> new NumberItem(itemTypeName, itemName, unitProvider);
            case PLAYER -> new PlayerItem(itemName);
            case ROLLERSHUTTER -> new RollershutterItem(itemName);
            case STRING -> new StringItem(itemName);
            case SWITCH -> new SwitchItem(itemName);
            default -> null;
        };
    }

    @Override
    public String[] getSupportedItemTypes() {
        return VALID_ITEM_TYPES.toArray(new String[0]);
    }
}
