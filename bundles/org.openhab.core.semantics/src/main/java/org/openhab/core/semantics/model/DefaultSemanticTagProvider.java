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
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_APARTMENT);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_BUILDING);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_BUILDING_GARAGE);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_BUILDING_HOUSE);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_BUILDING_SHED);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_BUILDING_SUMMERHOUSE);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_FLOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_FLOOR_GROUNDFLOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_FLOOR_FIRSTFLOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_FLOOR_SECONDFLOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_FLOOR_THIRDFLOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_FLOOR_ATTIC);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_FLOOR_BASEMENT);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_CORRIDOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_BATHROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_BEDROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_BOILERROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_CELLAR);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_DININGROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_ENTRY);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_FAMILYROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_GUESTROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_KITCHEN);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_LAUNDRYROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_LIVINGROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_OFFICE);
        defaultTags.add(DefaultSemanticTags.LOCATION_INDOOR_ROOM_VERANDA);
        defaultTags.add(DefaultSemanticTags.LOCATION_OUTDOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_OUTDOOR_CARPORT);
        defaultTags.add(DefaultSemanticTags.LOCATION_OUTDOOR_DRIVEWAY);
        defaultTags.add(DefaultSemanticTags.LOCATION_OUTDOOR_GARDEN);
        defaultTags.add(DefaultSemanticTags.LOCATION_OUTDOOR_PATIO);
        defaultTags.add(DefaultSemanticTags.LOCATION_OUTDOOR_PORCH);
        defaultTags.add(DefaultSemanticTags.LOCATION_OUTDOOR_TERRACE);
        defaultTags.add(DefaultSemanticTags.PROPERTY_TEMPERATURE);
        defaultTags.add(DefaultSemanticTags.PROPERTY_LIGHT);
        defaultTags.add(DefaultSemanticTags.PROPERTY_COLORTEMPERATURE);
        defaultTags.add(DefaultSemanticTags.PROPERTY_HUMIDITY);
        defaultTags.add(DefaultSemanticTags.PROPERTY_PRESENCE);
        defaultTags.add(DefaultSemanticTags.PROPERTY_PRESSURE);
        defaultTags.add(DefaultSemanticTags.PROPERTY_SMOKE);
        defaultTags.add(DefaultSemanticTags.PROPERTY_NOISE);
        defaultTags.add(DefaultSemanticTags.PROPERTY_RAIN);
        defaultTags.add(DefaultSemanticTags.PROPERTY_WIND);
        defaultTags.add(DefaultSemanticTags.PROPERTY_WATER);
        defaultTags.add(DefaultSemanticTags.PROPERTY_CO2);
        defaultTags.add(DefaultSemanticTags.PROPERTY_CO);
        defaultTags.add(DefaultSemanticTags.PROPERTY_ENERGY);
        defaultTags.add(DefaultSemanticTags.PROPERTY_POWER);
        defaultTags.add(DefaultSemanticTags.PROPERTY_VOLTAGE);
        defaultTags.add(DefaultSemanticTags.PROPERTY_CURRENT);
        defaultTags.add(DefaultSemanticTags.PROPERTY_FREQUENCY);
        defaultTags.add(DefaultSemanticTags.PROPERTY_GAS);
        defaultTags.add(DefaultSemanticTags.PROPERTY_SOUNDVOLUME);
        defaultTags.add(DefaultSemanticTags.PROPERTY_OIL);
        defaultTags.add(DefaultSemanticTags.PROPERTY_DURATION);
        defaultTags.add(DefaultSemanticTags.PROPERTY_LEVEL);
        defaultTags.add(DefaultSemanticTags.PROPERTY_OPENING);
        defaultTags.add(DefaultSemanticTags.PROPERTY_TIMESTAMP);
        defaultTags.add(DefaultSemanticTags.PROPERTY_ULTRAVIOLET);
        defaultTags.add(DefaultSemanticTags.PROPERTY_VIBRATION);
        defaultTags.add(DefaultSemanticTags.POINT_ALARM);
        defaultTags.add(DefaultSemanticTags.POINT_CONTROL);
        defaultTags.add(DefaultSemanticTags.POINT_CONTROL_SWITCH);
        defaultTags.add(DefaultSemanticTags.POINT_MEASUREMENT);
        defaultTags.add(DefaultSemanticTags.POINT_SETPOINT);
        defaultTags.add(DefaultSemanticTags.POINT_STATUS);
        defaultTags.add(DefaultSemanticTags.POINT_STATUS_LOWBATTERY);
        defaultTags.add(DefaultSemanticTags.POINT_STATUS_OPENLEVEL);
        defaultTags.add(DefaultSemanticTags.POINT_STATUS_OPENSTATE);
        defaultTags.add(DefaultSemanticTags.POINT_STATUS_TAMPERED);
        defaultTags.add(DefaultSemanticTags.POINT_STATUS_TILT);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_ALARMSYSTEM);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_BATTERY);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_BLINDS);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_BOILER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_CAMERA);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_CAR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_CLEANINGROBOT);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOOR_BACKDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOOR_CELLARDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOOR_FRONTDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOOR_GARAGEDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOOR_GATE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOOR_INNERDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOOR_SIDEDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOORBELL);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_FAN);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_FAN_CEILINGFAN);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_FAN_KITCHENHOOD);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_HVAC);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_INVERTER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_LAWNMOWER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_LIGHTBULB);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_LIGHTBULB_LIGHTSTRIPE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_LOCK);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_NETWORKAPPLIANCE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_POWEROUTLET);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_PROJECTOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_PUMP);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_RADIATORCONTROL);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_RECEIVER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_REMOTECONTROL);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SCREEN);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SCREEN_TELEVISION);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SENSOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SENSOR_MOTIONDETECTOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SENSOR_SMOKEDETECTOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SIREN);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SMARTPHONE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SPEAKER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_VALVE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_VOICEASSISTANT);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WALLSWITCH);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WEBSERVICE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WEBSERVICE_WEATHERSERVICE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WHITEGOOD);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WHITEGOOD_DISHWASHER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WHITEGOOD_DRYER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WHITEGOOD_FREEZER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WHITEGOOD_OVEN);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WHITEGOOD_REFRIGERATOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WHITEGOOD_WASHINGMACHINE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WINDOW);
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
