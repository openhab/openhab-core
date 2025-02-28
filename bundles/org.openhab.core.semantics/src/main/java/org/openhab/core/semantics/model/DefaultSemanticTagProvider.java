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
package org.openhab.core.semantics.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagProvider;
import org.osgi.service.component.annotations.Component;

/**
 * This class defines a provider of all default semantic tags.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { SemanticTagProvider.class, DefaultSemanticTagProvider.class })
public class DefaultSemanticTagProvider implements SemanticTagProvider {

    private List<SemanticTag> defaultTags;

    public DefaultSemanticTagProvider() {
        this.defaultTags = new ArrayList<>();
        defaultTags.add(DefaultSemanticTags.EQUIPMENT);
        defaultTags.add(DefaultSemanticTags.LOCATION);
        defaultTags.add(DefaultSemanticTags.POINT);
        defaultTags.add(DefaultSemanticTags.PROPERTY);
        defaultTags.add(DefaultSemanticTags.Location.INDOOR);
        defaultTags.add(DefaultSemanticTags.Location.APARTMENT);
        defaultTags.add(DefaultSemanticTags.Location.BUILDING);
        defaultTags.add(DefaultSemanticTags.Location.GARAGE);
        defaultTags.add(DefaultSemanticTags.Location.HOUSE);
        defaultTags.add(DefaultSemanticTags.Location.SHED);
        defaultTags.add(DefaultSemanticTags.Location.SUMMER_HOUSE);
        defaultTags.add(DefaultSemanticTags.Location.FLOOR);
        defaultTags.add(DefaultSemanticTags.Location.GROUND_FLOOR);
        defaultTags.add(DefaultSemanticTags.Location.FIRST_FLOOR);
        defaultTags.add(DefaultSemanticTags.Location.SECOND_FLOOR);
        defaultTags.add(DefaultSemanticTags.Location.THIRD_FLOOR);
        defaultTags.add(DefaultSemanticTags.Location.ATTIC);
        defaultTags.add(DefaultSemanticTags.Location.BASEMENT);
        defaultTags.add(DefaultSemanticTags.Location.CORRIDOR);
        defaultTags.add(DefaultSemanticTags.Location.ROOM);
        defaultTags.add(DefaultSemanticTags.Location.BATHROOM);
        defaultTags.add(DefaultSemanticTags.Location.BEDROOM);
        defaultTags.add(DefaultSemanticTags.Location.BOILER_ROOM);
        defaultTags.add(DefaultSemanticTags.Location.CELLAR);
        defaultTags.add(DefaultSemanticTags.Location.DINING_ROOM);
        defaultTags.add(DefaultSemanticTags.Location.ENTRY);
        defaultTags.add(DefaultSemanticTags.Location.FAMILY_ROOM);
        defaultTags.add(DefaultSemanticTags.Location.GUEST_ROOM);
        defaultTags.add(DefaultSemanticTags.Location.KITCHEN);
        defaultTags.add(DefaultSemanticTags.Location.LAUNDRY_ROOM);
        defaultTags.add(DefaultSemanticTags.Location.LIVING_ROOM);
        defaultTags.add(DefaultSemanticTags.Location.OFFICE);
        defaultTags.add(DefaultSemanticTags.Location.VERANDA);
        defaultTags.add(DefaultSemanticTags.Location.OUTDOOR);
        defaultTags.add(DefaultSemanticTags.Location.CARPORT);
        defaultTags.add(DefaultSemanticTags.Location.DRIVEWAY);
        defaultTags.add(DefaultSemanticTags.Location.GARDEN);
        defaultTags.add(DefaultSemanticTags.Location.PATIO);
        defaultTags.add(DefaultSemanticTags.Location.PORCH);
        defaultTags.add(DefaultSemanticTags.Location.TERRACE);
        defaultTags.add(DefaultSemanticTags.Property.TEMPERATURE);
        defaultTags.add(DefaultSemanticTags.Property.LIGHT);
        defaultTags.add(DefaultSemanticTags.Property.COLOR_TEMPERATURE);
        defaultTags.add(DefaultSemanticTags.Property.HUMIDITY);
        defaultTags.add(DefaultSemanticTags.Property.PRESENCE);
        defaultTags.add(DefaultSemanticTags.Property.PRESSURE);
        defaultTags.add(DefaultSemanticTags.Property.SMOKE);
        defaultTags.add(DefaultSemanticTags.Property.NOISE);
        defaultTags.add(DefaultSemanticTags.Property.RAIN);
        defaultTags.add(DefaultSemanticTags.Property.WIND);
        defaultTags.add(DefaultSemanticTags.Property.WATER);
        defaultTags.add(DefaultSemanticTags.Property.CO2);
        defaultTags.add(DefaultSemanticTags.Property.CO);
        defaultTags.add(DefaultSemanticTags.Property.ENERGY);
        defaultTags.add(DefaultSemanticTags.Property.POWER);
        defaultTags.add(DefaultSemanticTags.Property.VOLTAGE);
        defaultTags.add(DefaultSemanticTags.Property.CURRENT);
        defaultTags.add(DefaultSemanticTags.Property.FREQUENCY);
        defaultTags.add(DefaultSemanticTags.Property.GAS);
        defaultTags.add(DefaultSemanticTags.Property.SOUND_VOLUME);
        defaultTags.add(DefaultSemanticTags.Property.OIL);
        defaultTags.add(DefaultSemanticTags.Property.DURATION);
        defaultTags.add(DefaultSemanticTags.Property.LEVEL);
        defaultTags.add(DefaultSemanticTags.Property.OPENING);
        defaultTags.add(DefaultSemanticTags.Property.TIMESTAMP);
        defaultTags.add(DefaultSemanticTags.Property.ULTRAVIOLET);
        defaultTags.add(DefaultSemanticTags.Property.VIBRATION);
        defaultTags.add(DefaultSemanticTags.Point.ALARM);
        defaultTags.add(DefaultSemanticTags.Point.CONTROL);
        defaultTags.add(DefaultSemanticTags.Point.SWITCH);
        defaultTags.add(DefaultSemanticTags.Point.MEASUREMENT);
        defaultTags.add(DefaultSemanticTags.Point.SETPOINT);
        defaultTags.add(DefaultSemanticTags.Point.STATUS);
        defaultTags.add(DefaultSemanticTags.Point.LOW_BATTERY);
        defaultTags.add(DefaultSemanticTags.Point.OPEN_LEVEL);
        defaultTags.add(DefaultSemanticTags.Point.OPEN_STATE);
        defaultTags.add(DefaultSemanticTags.Point.TAMPERED);
        defaultTags.add(DefaultSemanticTags.Point.TILT);
        defaultTags.add(DefaultSemanticTags.Equipment.ALARM_SYSTEM);
        defaultTags.add(DefaultSemanticTags.Equipment.BATTERY);
        defaultTags.add(DefaultSemanticTags.Equipment.BLINDS);
        defaultTags.add(DefaultSemanticTags.Equipment.BOILER);
        defaultTags.add(DefaultSemanticTags.Equipment.CAMERA);
        defaultTags.add(DefaultSemanticTags.Equipment.CAR);
        defaultTags.add(DefaultSemanticTags.Equipment.CLEANING_ROBOT);
        defaultTags.add(DefaultSemanticTags.Equipment.DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.BACK_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.CELLAR_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.FRONT_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.GARAGE_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.GATE);
        defaultTags.add(DefaultSemanticTags.Equipment.INNER_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.SIDE_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.DOORBELL);
        defaultTags.add(DefaultSemanticTags.Equipment.FAN);
        defaultTags.add(DefaultSemanticTags.Equipment.CEILING_FAN);
        defaultTags.add(DefaultSemanticTags.Equipment.KITCHEN_HOOD);
        defaultTags.add(DefaultSemanticTags.Equipment.HVAC);
        defaultTags.add(DefaultSemanticTags.Equipment.INVERTER);
        defaultTags.add(DefaultSemanticTags.Equipment.LAWN_MOWER);
        defaultTags.add(DefaultSemanticTags.Equipment.LIGHTBULB);
        defaultTags.add(DefaultSemanticTags.Equipment.LIGHT_STRIPE);
        defaultTags.add(DefaultSemanticTags.Equipment.LOCK);
        defaultTags.add(DefaultSemanticTags.Equipment.NETWORK_APPLIANCE);
        defaultTags.add(DefaultSemanticTags.Equipment.POWER_OUTLET);
        defaultTags.add(DefaultSemanticTags.Equipment.PROJECTOR);
        defaultTags.add(DefaultSemanticTags.Equipment.PUMP);
        defaultTags.add(DefaultSemanticTags.Equipment.RADIATOR_CONTROL);
        defaultTags.add(DefaultSemanticTags.Equipment.RECEIVER);
        defaultTags.add(DefaultSemanticTags.Equipment.REMOTE_CONTROL);
        defaultTags.add(DefaultSemanticTags.Equipment.SCREEN);
        defaultTags.add(DefaultSemanticTags.Equipment.TELEVISION);
        defaultTags.add(DefaultSemanticTags.Equipment.SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.MOTION_DETECTOR);
        defaultTags.add(DefaultSemanticTags.Equipment.SMOKE_DETECTOR);
        defaultTags.add(DefaultSemanticTags.Equipment.SIREN);
        defaultTags.add(DefaultSemanticTags.Equipment.SMARTPHONE);
        defaultTags.add(DefaultSemanticTags.Equipment.SPEAKER);
        defaultTags.add(DefaultSemanticTags.Equipment.VALVE);
        defaultTags.add(DefaultSemanticTags.Equipment.VOICE_ASSISTANT);
        defaultTags.add(DefaultSemanticTags.Equipment.WALL_SWITCH);
        defaultTags.add(DefaultSemanticTags.Equipment.WEB_SERVICE);
        defaultTags.add(DefaultSemanticTags.Equipment.WEATHER_SERVICE);
        defaultTags.add(DefaultSemanticTags.Equipment.WHITE_GOOD);
        defaultTags.add(DefaultSemanticTags.Equipment.DISHWASHER);
        defaultTags.add(DefaultSemanticTags.Equipment.DRYER);
        defaultTags.add(DefaultSemanticTags.Equipment.FREEZER);
        defaultTags.add(DefaultSemanticTags.Equipment.OVEN);
        defaultTags.add(DefaultSemanticTags.Equipment.REFRIGERATOR);
        defaultTags.add(DefaultSemanticTags.Equipment.WASHING_MACHINE);
        defaultTags.add(DefaultSemanticTags.Equipment.WINDOW);
    }

    @Override
    public Collection<SemanticTag> getAll() {
        return defaultTags;
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<SemanticTag> listener) {
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<SemanticTag> listener) {
    }
}
