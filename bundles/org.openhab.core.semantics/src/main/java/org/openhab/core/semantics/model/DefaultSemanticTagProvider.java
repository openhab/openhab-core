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
        defaultTags.add(DefaultSemanticTags.LOCATION_APARTMENT);
        defaultTags.add(DefaultSemanticTags.LOCATION_BUILDING);
        defaultTags.add(DefaultSemanticTags.LOCATION_GARAGE);
        defaultTags.add(DefaultSemanticTags.LOCATION_HOUSE);
        defaultTags.add(DefaultSemanticTags.LOCATION_SHED);
        defaultTags.add(DefaultSemanticTags.LOCATION_SUMMERHOUSE);
        defaultTags.add(DefaultSemanticTags.LOCATION_FLOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_GROUNDFLOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_FIRSTFLOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_SECONDFLOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_THIRDFLOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_ATTIC);
        defaultTags.add(DefaultSemanticTags.LOCATION_BASEMENT);
        defaultTags.add(DefaultSemanticTags.LOCATION_CORRIDOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_ROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_BATHROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_BEDROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_BOILERROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_CELLAR);
        defaultTags.add(DefaultSemanticTags.LOCATION_DININGROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_ENTRY);
        defaultTags.add(DefaultSemanticTags.LOCATION_FAMILYROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_GUESTROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_KITCHEN);
        defaultTags.add(DefaultSemanticTags.LOCATION_LAUNDRYROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_LIVINGROOM);
        defaultTags.add(DefaultSemanticTags.LOCATION_OFFICE);
        defaultTags.add(DefaultSemanticTags.LOCATION_VERANDA);
        defaultTags.add(DefaultSemanticTags.LOCATION_OUTDOOR);
        defaultTags.add(DefaultSemanticTags.LOCATION_CARPORT);
        defaultTags.add(DefaultSemanticTags.LOCATION_DRIVEWAY);
        defaultTags.add(DefaultSemanticTags.LOCATION_GARDEN);
        defaultTags.add(DefaultSemanticTags.LOCATION_PATIO);
        defaultTags.add(DefaultSemanticTags.LOCATION_PORCH);
        defaultTags.add(DefaultSemanticTags.LOCATION_TERRACE);
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
        defaultTags.add(DefaultSemanticTags.POINT_SWITCH);
        defaultTags.add(DefaultSemanticTags.POINT_MEASUREMENT);
        defaultTags.add(DefaultSemanticTags.POINT_SETPOINT);
        defaultTags.add(DefaultSemanticTags.POINT_STATUS);
        defaultTags.add(DefaultSemanticTags.POINT_LOWBATTERY);
        defaultTags.add(DefaultSemanticTags.POINT_OPENLEVEL);
        defaultTags.add(DefaultSemanticTags.POINT_OPENSTATE);
        defaultTags.add(DefaultSemanticTags.POINT_TAMPERED);
        defaultTags.add(DefaultSemanticTags.POINT_TILT);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_ALARMSYSTEM);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_BATTERY);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_BLINDS);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_BOILER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_CAMERA);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_CAR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_CLEANINGROBOT);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_BACKDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_CELLARDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_FRONTDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_GARAGEDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_GATE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_INNERDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SIDEDOOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DOORBELL);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_FAN);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_CEILINGFAN);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_KITCHENHOOD);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_HVAC);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_INVERTER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_LAWNMOWER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_LIGHTBULB);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_LIGHTSTRIPE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_LOCK);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_NETWORKAPPLIANCE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_POWEROUTLET);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_PROJECTOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_PUMP);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_RADIATORCONTROL);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_RECEIVER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_REMOTECONTROL);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SCREEN);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_TELEVISION);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SENSOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_MOTIONDETECTOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SMOKEDETECTOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SIREN);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SMARTPHONE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_SPEAKER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_VALVE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_VOICEASSISTANT);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WALLSWITCH);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WEBSERVICE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WEATHERSERVICE);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WHITEGOOD);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DISHWASHER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_DRYER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_FREEZER);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_OVEN);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_REFRIGERATOR);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT_WASHINGMACHINE);
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
