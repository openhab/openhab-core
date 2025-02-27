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
        defaultTags.add(DefaultSemanticTags.INDOOR);
        defaultTags.add(DefaultSemanticTags.APARTMENT);
        defaultTags.add(DefaultSemanticTags.BUILDING);
        defaultTags.add(DefaultSemanticTags.GARAGE);
        defaultTags.add(DefaultSemanticTags.HOUSE);
        defaultTags.add(DefaultSemanticTags.SHED);
        defaultTags.add(DefaultSemanticTags.SUMMERHOUSE);
        defaultTags.add(DefaultSemanticTags.FLOOR);
        defaultTags.add(DefaultSemanticTags.GROUNDFLOOR);
        defaultTags.add(DefaultSemanticTags.FIRSTFLOOR);
        defaultTags.add(DefaultSemanticTags.SECONDFLOOR);
        defaultTags.add(DefaultSemanticTags.THIRDFLOOR);
        defaultTags.add(DefaultSemanticTags.ATTIC);
        defaultTags.add(DefaultSemanticTags.BASEMENT);
        defaultTags.add(DefaultSemanticTags.CORRIDOR);
        defaultTags.add(DefaultSemanticTags.ROOM);
        defaultTags.add(DefaultSemanticTags.BATHROOM);
        defaultTags.add(DefaultSemanticTags.BEDROOM);
        defaultTags.add(DefaultSemanticTags.BOILERROOM);
        defaultTags.add(DefaultSemanticTags.CELLAR);
        defaultTags.add(DefaultSemanticTags.DININGROOM);
        defaultTags.add(DefaultSemanticTags.ENTRY);
        defaultTags.add(DefaultSemanticTags.FAMILYROOM);
        defaultTags.add(DefaultSemanticTags.GUESTROOM);
        defaultTags.add(DefaultSemanticTags.KITCHEN);
        defaultTags.add(DefaultSemanticTags.LAUNDRYROOM);
        defaultTags.add(DefaultSemanticTags.LIVINGROOM);
        defaultTags.add(DefaultSemanticTags.OFFICE);
        defaultTags.add(DefaultSemanticTags.VERANDA);
        defaultTags.add(DefaultSemanticTags.OUTDOOR);
        defaultTags.add(DefaultSemanticTags.CARPORT);
        defaultTags.add(DefaultSemanticTags.DRIVEWAY);
        defaultTags.add(DefaultSemanticTags.GARDEN);
        defaultTags.add(DefaultSemanticTags.PATIO);
        defaultTags.add(DefaultSemanticTags.PORCH);
        defaultTags.add(DefaultSemanticTags.TERRACE);
        defaultTags.add(DefaultSemanticTags.TEMPERATURE);
        defaultTags.add(DefaultSemanticTags.LIGHT);
        defaultTags.add(DefaultSemanticTags.COLORTEMPERATURE);
        defaultTags.add(DefaultSemanticTags.HUMIDITY);
        defaultTags.add(DefaultSemanticTags.PRESENCE);
        defaultTags.add(DefaultSemanticTags.PRESSURE);
        defaultTags.add(DefaultSemanticTags.SMOKE);
        defaultTags.add(DefaultSemanticTags.NOISE);
        defaultTags.add(DefaultSemanticTags.RAIN);
        defaultTags.add(DefaultSemanticTags.WIND);
        defaultTags.add(DefaultSemanticTags.WATER);
        defaultTags.add(DefaultSemanticTags.CO2);
        defaultTags.add(DefaultSemanticTags.CO);
        defaultTags.add(DefaultSemanticTags.ENERGY);
        defaultTags.add(DefaultSemanticTags.POWER);
        defaultTags.add(DefaultSemanticTags.VOLTAGE);
        defaultTags.add(DefaultSemanticTags.CURRENT);
        defaultTags.add(DefaultSemanticTags.FREQUENCY);
        defaultTags.add(DefaultSemanticTags.GAS);
        defaultTags.add(DefaultSemanticTags.SOUNDVOLUME);
        defaultTags.add(DefaultSemanticTags.OIL);
        defaultTags.add(DefaultSemanticTags.DURATION);
        defaultTags.add(DefaultSemanticTags.LEVEL);
        defaultTags.add(DefaultSemanticTags.OPENING);
        defaultTags.add(DefaultSemanticTags.TIMESTAMP);
        defaultTags.add(DefaultSemanticTags.ULTRAVIOLET);
        defaultTags.add(DefaultSemanticTags.VIBRATION);
        defaultTags.add(DefaultSemanticTags.ALARM);
        defaultTags.add(DefaultSemanticTags.CONTROL);
        defaultTags.add(DefaultSemanticTags.SWITCH);
        defaultTags.add(DefaultSemanticTags.MEASUREMENT);
        defaultTags.add(DefaultSemanticTags.SETPOINT);
        defaultTags.add(DefaultSemanticTags.STATUS);
        defaultTags.add(DefaultSemanticTags.LOWBATTERY);
        defaultTags.add(DefaultSemanticTags.OPENLEVEL);
        defaultTags.add(DefaultSemanticTags.OPENSTATE);
        defaultTags.add(DefaultSemanticTags.TAMPERED);
        defaultTags.add(DefaultSemanticTags.TILT);
        defaultTags.add(DefaultSemanticTags.ALARMSYSTEM);
        defaultTags.add(DefaultSemanticTags.BATTERY);
        defaultTags.add(DefaultSemanticTags.BLINDS);
        defaultTags.add(DefaultSemanticTags.BOILER);
        defaultTags.add(DefaultSemanticTags.CAMERA);
        defaultTags.add(DefaultSemanticTags.CAR);
        defaultTags.add(DefaultSemanticTags.CLEANINGROBOT);
        defaultTags.add(DefaultSemanticTags.DOOR);
        defaultTags.add(DefaultSemanticTags.BACKDOOR);
        defaultTags.add(DefaultSemanticTags.CELLARDOOR);
        defaultTags.add(DefaultSemanticTags.FRONTDOOR);
        defaultTags.add(DefaultSemanticTags.GARAGEDOOR);
        defaultTags.add(DefaultSemanticTags.GATE);
        defaultTags.add(DefaultSemanticTags.INNERDOOR);
        defaultTags.add(DefaultSemanticTags.SIDEDOOR);
        defaultTags.add(DefaultSemanticTags.DOORBELL);
        defaultTags.add(DefaultSemanticTags.FAN);
        defaultTags.add(DefaultSemanticTags.CEILINGFAN);
        defaultTags.add(DefaultSemanticTags.KITCHENHOOD);
        defaultTags.add(DefaultSemanticTags.HVAC);
        defaultTags.add(DefaultSemanticTags.INVERTER);
        defaultTags.add(DefaultSemanticTags.LAWNMOWER);
        defaultTags.add(DefaultSemanticTags.LIGHTBULB);
        defaultTags.add(DefaultSemanticTags.LIGHTSTRIPE);
        defaultTags.add(DefaultSemanticTags.LOCK);
        defaultTags.add(DefaultSemanticTags.NETWORKAPPLIANCE);
        defaultTags.add(DefaultSemanticTags.POWEROUTLET);
        defaultTags.add(DefaultSemanticTags.PROJECTOR);
        defaultTags.add(DefaultSemanticTags.PUMP);
        defaultTags.add(DefaultSemanticTags.RADIATORCONTROL);
        defaultTags.add(DefaultSemanticTags.RECEIVER);
        defaultTags.add(DefaultSemanticTags.REMOTECONTROL);
        defaultTags.add(DefaultSemanticTags.SCREEN);
        defaultTags.add(DefaultSemanticTags.TELEVISION);
        defaultTags.add(DefaultSemanticTags.SENSOR);
        defaultTags.add(DefaultSemanticTags.MOTIONDETECTOR);
        defaultTags.add(DefaultSemanticTags.SMOKEDETECTOR);
        defaultTags.add(DefaultSemanticTags.SIREN);
        defaultTags.add(DefaultSemanticTags.SMARTPHONE);
        defaultTags.add(DefaultSemanticTags.SPEAKER);
        defaultTags.add(DefaultSemanticTags.VALVE);
        defaultTags.add(DefaultSemanticTags.VOICEASSISTANT);
        defaultTags.add(DefaultSemanticTags.WALLSWITCH);
        defaultTags.add(DefaultSemanticTags.WEBSERVICE);
        defaultTags.add(DefaultSemanticTags.WEATHERSERVICE);
        defaultTags.add(DefaultSemanticTags.WHITEGOOD);
        defaultTags.add(DefaultSemanticTags.DISHWASHER);
        defaultTags.add(DefaultSemanticTags.DRYER);
        defaultTags.add(DefaultSemanticTags.FREEZER);
        defaultTags.add(DefaultSemanticTags.OVEN);
        defaultTags.add(DefaultSemanticTags.REFRIGERATOR);
        defaultTags.add(DefaultSemanticTags.WASHINGMACHINE);
        defaultTags.add(DefaultSemanticTags.WINDOW);
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
