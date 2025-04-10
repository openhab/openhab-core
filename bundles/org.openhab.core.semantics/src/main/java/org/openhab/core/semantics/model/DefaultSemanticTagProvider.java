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
        defaultTags.add(DefaultSemanticTags.LOCATION);
        defaultTags.add(DefaultSemanticTags.POINT);
        defaultTags.add(DefaultSemanticTags.PROPERTY);
        defaultTags.add(DefaultSemanticTags.EQUIPMENT);
        defaultTags.add(DefaultSemanticTags.Location.INDOOR);
        defaultTags.add(DefaultSemanticTags.Location.APARTMENT);
        defaultTags.add(DefaultSemanticTags.Location.BUILDING);
        defaultTags.add(DefaultSemanticTags.Location.GARAGE);
        defaultTags.add(DefaultSemanticTags.Location.HOUSE);
        defaultTags.add(DefaultSemanticTags.Location.SHED);
        defaultTags.add(DefaultSemanticTags.Location.SUMMER_HOUSE);
        defaultTags.add(DefaultSemanticTags.Location.CORRIDOR);
        defaultTags.add(DefaultSemanticTags.Location.FLOOR);
        defaultTags.add(DefaultSemanticTags.Location.ATTIC);
        defaultTags.add(DefaultSemanticTags.Location.BASEMENT);
        defaultTags.add(DefaultSemanticTags.Location.FIRST_FLOOR);
        defaultTags.add(DefaultSemanticTags.Location.GROUND_FLOOR);
        defaultTags.add(DefaultSemanticTags.Location.SECOND_FLOOR);
        defaultTags.add(DefaultSemanticTags.Location.THIRD_FLOOR);
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
        defaultTags.add(DefaultSemanticTags.Point.ALARM);
        defaultTags.add(DefaultSemanticTags.Point.CONTROL);
        defaultTags.add(DefaultSemanticTags.Point.SWITCH);
        defaultTags.add(DefaultSemanticTags.Point.FORECAST);
        defaultTags.add(DefaultSemanticTags.Point.MEASUREMENT);
        defaultTags.add(DefaultSemanticTags.Point.SETPOINT);
        defaultTags.add(DefaultSemanticTags.Point.STATUS);
        defaultTags.add(DefaultSemanticTags.Property.AIR_QUALITY);
        defaultTags.add(DefaultSemanticTags.Property.AQI);
        defaultTags.add(DefaultSemanticTags.Property.CO);
        defaultTags.add(DefaultSemanticTags.Property.CO2);
        defaultTags.add(DefaultSemanticTags.Property.OZONE);
        defaultTags.add(DefaultSemanticTags.Property.PARTICULATE_MATTER);
        defaultTags.add(DefaultSemanticTags.Property.POLLEN);
        defaultTags.add(DefaultSemanticTags.Property.RADON);
        defaultTags.add(DefaultSemanticTags.Property.VOC);
        defaultTags.add(DefaultSemanticTags.Property.AIRCONDITIONING);
        defaultTags.add(DefaultSemanticTags.Property.AIRFLOW);
        defaultTags.add(DefaultSemanticTags.Property.APP);
        defaultTags.add(DefaultSemanticTags.Property.BRIGHTNESS);
        defaultTags.add(DefaultSemanticTags.Property.CHANNEL);
        defaultTags.add(DefaultSemanticTags.Property.COLOR);
        defaultTags.add(DefaultSemanticTags.Property.COLOR_TEMPERATURE);
        defaultTags.add(DefaultSemanticTags.Property.CURRENT);
        defaultTags.add(DefaultSemanticTags.Property.DURATION);
        defaultTags.add(DefaultSemanticTags.Property.ENERGY);
        defaultTags.add(DefaultSemanticTags.Property.FREQUENCY);
        defaultTags.add(DefaultSemanticTags.Property.GAS);
        defaultTags.add(DefaultSemanticTags.Property.HEATING);
        defaultTags.add(DefaultSemanticTags.Property.HOME_AWAY);
        defaultTags.add(DefaultSemanticTags.Property.HUMIDITY);
        defaultTags.add(DefaultSemanticTags.Property.ILLUMINANCE);
        defaultTags.add(DefaultSemanticTags.Property.LEVEL);
        defaultTags.add(DefaultSemanticTags.Property.LIGHT);
        defaultTags.add(DefaultSemanticTags.Property.LOW_BATTERY);
        defaultTags.add(DefaultSemanticTags.Property.MEDIA_CONTROL);
        defaultTags.add(DefaultSemanticTags.Property.MODE);
        defaultTags.add(DefaultSemanticTags.Property.AUTO_MANUAL);
        defaultTags.add(DefaultSemanticTags.Property.ENABLED_DISABLED);
        defaultTags.add(DefaultSemanticTags.Property.NOISE);
        defaultTags.add(DefaultSemanticTags.Property.OIL);
        defaultTags.add(DefaultSemanticTags.Property.ON_OFF);
        defaultTags.add(DefaultSemanticTags.Property.OPENING);
        defaultTags.add(DefaultSemanticTags.Property.OPEN_LEVEL);
        defaultTags.add(DefaultSemanticTags.Property.OPEN_STATE);
        defaultTags.add(DefaultSemanticTags.Property.POSITION);
        defaultTags.add(DefaultSemanticTags.Property.GEO_LOCATION);
        defaultTags.add(DefaultSemanticTags.Property.POWER);
        defaultTags.add(DefaultSemanticTags.Property.PRESENCE);
        defaultTags.add(DefaultSemanticTags.Property.PRESSURE);
        defaultTags.add(DefaultSemanticTags.Property.QUALITY_OF_SERVICE);
        defaultTags.add(DefaultSemanticTags.Property.RAIN);
        defaultTags.add(DefaultSemanticTags.Property.SIGNAL_STRENGTH);
        defaultTags.add(DefaultSemanticTags.Property.RSSI);
        defaultTags.add(DefaultSemanticTags.Property.SMOKE);
        defaultTags.add(DefaultSemanticTags.Property.SOUND_VOLUME);
        defaultTags.add(DefaultSemanticTags.Property.SPEED);
        defaultTags.add(DefaultSemanticTags.Property.TAMPERED);
        defaultTags.add(DefaultSemanticTags.Property.TEMPERATURE);
        defaultTags.add(DefaultSemanticTags.Property.TILT);
        defaultTags.add(DefaultSemanticTags.Property.TIMESTAMP);
        defaultTags.add(DefaultSemanticTags.Property.ULTRAVIOLET);
        defaultTags.add(DefaultSemanticTags.Property.VENTILATION);
        defaultTags.add(DefaultSemanticTags.Property.VIBRATION);
        defaultTags.add(DefaultSemanticTags.Property.VOLTAGE);
        defaultTags.add(DefaultSemanticTags.Property.WATER);
        defaultTags.add(DefaultSemanticTags.Property.WIND);
        defaultTags.add(DefaultSemanticTags.Equipment.ALARM_DEVICE);
        defaultTags.add(DefaultSemanticTags.Equipment.ALARM_SYSTEM);
        defaultTags.add(DefaultSemanticTags.Equipment.AUDIO_VISUAL);
        defaultTags.add(DefaultSemanticTags.Equipment.MEDIA_PLAYER);
        defaultTags.add(DefaultSemanticTags.Equipment.RECEIVER);
        defaultTags.add(DefaultSemanticTags.Equipment.SCREEN);
        defaultTags.add(DefaultSemanticTags.Equipment.TELEVISION);
        defaultTags.add(DefaultSemanticTags.Equipment.BATTERY);
        defaultTags.add(DefaultSemanticTags.Equipment.BED);
        defaultTags.add(DefaultSemanticTags.Equipment.BLINDS);
        defaultTags.add(DefaultSemanticTags.Equipment.CAMERA);
        defaultTags.add(DefaultSemanticTags.Equipment.CLEANING_ROBOT);
        defaultTags.add(DefaultSemanticTags.Equipment.COMPUTER);
        defaultTags.add(DefaultSemanticTags.Equipment.CONTACT_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.CONTROL_DEVICE);
        defaultTags.add(DefaultSemanticTags.Equipment.BUTTON);
        defaultTags.add(DefaultSemanticTags.Equipment.DIAL);
        defaultTags.add(DefaultSemanticTags.Equipment.KEYPAD);
        defaultTags.add(DefaultSemanticTags.Equipment.SLIDER);
        defaultTags.add(DefaultSemanticTags.Equipment.WALL_SWITCH);
        defaultTags.add(DefaultSemanticTags.Equipment.DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.BACK_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.CELLAR_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.FRONT_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.GARAGE_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.GATE);
        defaultTags.add(DefaultSemanticTags.Equipment.INNER_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.SIDE_DOOR);
        defaultTags.add(DefaultSemanticTags.Equipment.DOORBELL);
        defaultTags.add(DefaultSemanticTags.Equipment.DRINKING_WATER);
        defaultTags.add(DefaultSemanticTags.Equipment.HOT_WATER_FAUCET);
        defaultTags.add(DefaultSemanticTags.Equipment.WATER_FILTER);
        defaultTags.add(DefaultSemanticTags.Equipment.WATER_SOFTENER);
        defaultTags.add(DefaultSemanticTags.Equipment.HVAC);
        defaultTags.add(DefaultSemanticTags.Equipment.AIR_CONDITIONER);
        defaultTags.add(DefaultSemanticTags.Equipment.AIR_FILTER);
        defaultTags.add(DefaultSemanticTags.Equipment.BOILER);
        defaultTags.add(DefaultSemanticTags.Equipment.DEHUMIDIFIER);
        defaultTags.add(DefaultSemanticTags.Equipment.FAN);
        defaultTags.add(DefaultSemanticTags.Equipment.CEILING_FAN);
        defaultTags.add(DefaultSemanticTags.Equipment.EXHAUST_FAN);
        defaultTags.add(DefaultSemanticTags.Equipment.KITCHEN_HOOD);
        defaultTags.add(DefaultSemanticTags.Equipment.FLOOR_HEATING);
        defaultTags.add(DefaultSemanticTags.Equipment.FURNACE);
        defaultTags.add(DefaultSemanticTags.Equipment.HEAT_PUMP);
        defaultTags.add(DefaultSemanticTags.Equipment.HEAT_RECOVERY);
        defaultTags.add(DefaultSemanticTags.Equipment.HUMIDIFIER);
        defaultTags.add(DefaultSemanticTags.Equipment.RADIATOR_CONTROL);
        defaultTags.add(DefaultSemanticTags.Equipment.SMART_VENT);
        defaultTags.add(DefaultSemanticTags.Equipment.THERMOSTAT);
        defaultTags.add(DefaultSemanticTags.Equipment.WATER_HEATER);
        defaultTags.add(DefaultSemanticTags.Equipment.HORTICULTURE);
        defaultTags.add(DefaultSemanticTags.Equipment.IRRIGATION);
        defaultTags.add(DefaultSemanticTags.Equipment.LAWN_MOWER);
        defaultTags.add(DefaultSemanticTags.Equipment.SOIL_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.LIGHT_SOURCE);
        defaultTags.add(DefaultSemanticTags.Equipment.ACCENT_LIGHT);
        defaultTags.add(DefaultSemanticTags.Equipment.CHANDELIER);
        defaultTags.add(DefaultSemanticTags.Equipment.DOWNLIGHT);
        defaultTags.add(DefaultSemanticTags.Equipment.FLOOD_LIGHT);
        defaultTags.add(DefaultSemanticTags.Equipment.LAMP);
        defaultTags.add(DefaultSemanticTags.Equipment.LIGHT_STRIP);
        defaultTags.add(DefaultSemanticTags.Equipment.LIGHT_STRIPE);
        defaultTags.add(DefaultSemanticTags.Equipment.LIGHTBULB);
        defaultTags.add(DefaultSemanticTags.Equipment.PENDANT);
        defaultTags.add(DefaultSemanticTags.Equipment.SCONCE);
        defaultTags.add(DefaultSemanticTags.Equipment.SPOT_LIGHT);
        defaultTags.add(DefaultSemanticTags.Equipment.TRACK_LIGHT);
        defaultTags.add(DefaultSemanticTags.Equipment.WALL_LIGHT);
        defaultTags.add(DefaultSemanticTags.Equipment.LOCK);
        defaultTags.add(DefaultSemanticTags.Equipment.NETWORK_APPLIANCE);
        defaultTags.add(DefaultSemanticTags.Equipment.FIREWALL);
        defaultTags.add(DefaultSemanticTags.Equipment.NETWORK_SWITCH);
        defaultTags.add(DefaultSemanticTags.Equipment.ROUTER);
        defaultTags.add(DefaultSemanticTags.Equipment.WIRELESS_ACCESS_POINT);
        defaultTags.add(DefaultSemanticTags.Equipment.PET_CARE);
        defaultTags.add(DefaultSemanticTags.Equipment.AQUARIUM);
        defaultTags.add(DefaultSemanticTags.Equipment.PET_FEEDER);
        defaultTags.add(DefaultSemanticTags.Equipment.PET_FLAP);
        defaultTags.add(DefaultSemanticTags.Equipment.POWER_OUTLET);
        defaultTags.add(DefaultSemanticTags.Equipment.POWER_SUPPLY);
        defaultTags.add(DefaultSemanticTags.Equipment.EVSE);
        defaultTags.add(DefaultSemanticTags.Equipment.GENERATOR);
        defaultTags.add(DefaultSemanticTags.Equipment.INVERTER);
        defaultTags.add(DefaultSemanticTags.Equipment.SOLAR_PANEL);
        defaultTags.add(DefaultSemanticTags.Equipment.TRANSFER_SWITCH);
        defaultTags.add(DefaultSemanticTags.Equipment.UPS);
        defaultTags.add(DefaultSemanticTags.Equipment.WIND_GENERATOR);
        defaultTags.add(DefaultSemanticTags.Equipment.PRINTER);
        defaultTags.add(DefaultSemanticTags.Equipment.PRINTER3D);
        defaultTags.add(DefaultSemanticTags.Equipment.PROJECTOR);
        defaultTags.add(DefaultSemanticTags.Equipment.PUMP);
        defaultTags.add(DefaultSemanticTags.Equipment.WATER_FEATURE);
        defaultTags.add(DefaultSemanticTags.Equipment.REMOTE_CONTROL);
        defaultTags.add(DefaultSemanticTags.Equipment.SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.AIR_QUALITY_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.CO2_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.CO_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.ELECTRIC_METER);
        defaultTags.add(DefaultSemanticTags.Equipment.FIRE_DETECTOR);
        defaultTags.add(DefaultSemanticTags.Equipment.HEAT_DETECTOR);
        defaultTags.add(DefaultSemanticTags.Equipment.SMOKE_DETECTOR);
        defaultTags.add(DefaultSemanticTags.Equipment.GAS_METER);
        defaultTags.add(DefaultSemanticTags.Equipment.GLASS_BREAK_DETECTOR);
        defaultTags.add(DefaultSemanticTags.Equipment.HUMIDITY_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.ILLUMINANCE_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.LEAK_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.OCCUPANCY_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.MOTION_DETECTOR);
        defaultTags.add(DefaultSemanticTags.Equipment.TEMPERATURE_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.VIBRATION_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.WATER_METER);
        defaultTags.add(DefaultSemanticTags.Equipment.WATER_QUALITY_SENSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.WEATHER_STATION);
        defaultTags.add(DefaultSemanticTags.Equipment.SIREN);
        defaultTags.add(DefaultSemanticTags.Equipment.SMARTPHONE);
        defaultTags.add(DefaultSemanticTags.Equipment.SPEAKER);
        defaultTags.add(DefaultSemanticTags.Equipment.TOOL);
        defaultTags.add(DefaultSemanticTags.Equipment.TRACKER);
        defaultTags.add(DefaultSemanticTags.Equipment.VALVE);
        defaultTags.add(DefaultSemanticTags.Equipment.VEHICLE);
        defaultTags.add(DefaultSemanticTags.Equipment.CAR);
        defaultTags.add(DefaultSemanticTags.Equipment.VOICE_ASSISTANT);
        defaultTags.add(DefaultSemanticTags.Equipment.WEB_SERVICE);
        defaultTags.add(DefaultSemanticTags.Equipment.WEATHER_SERVICE);
        defaultTags.add(DefaultSemanticTags.Equipment.WELLNESS);
        defaultTags.add(DefaultSemanticTags.Equipment.CHLORINATOR);
        defaultTags.add(DefaultSemanticTags.Equipment.JACUZZI);
        defaultTags.add(DefaultSemanticTags.Equipment.POOL_COVER);
        defaultTags.add(DefaultSemanticTags.Equipment.POOL_HEATER);
        defaultTags.add(DefaultSemanticTags.Equipment.SAUNA);
        defaultTags.add(DefaultSemanticTags.Equipment.SHOWER);
        defaultTags.add(DefaultSemanticTags.Equipment.SWIMMING_POOL);
        defaultTags.add(DefaultSemanticTags.Equipment.WHITE_GOOD);
        defaultTags.add(DefaultSemanticTags.Equipment.AIR_FRYER);
        defaultTags.add(DefaultSemanticTags.Equipment.COOKTOP);
        defaultTags.add(DefaultSemanticTags.Equipment.DISHWASHER);
        defaultTags.add(DefaultSemanticTags.Equipment.DRYER);
        defaultTags.add(DefaultSemanticTags.Equipment.FOOD_PROCESSOR);
        defaultTags.add(DefaultSemanticTags.Equipment.FREEZER);
        defaultTags.add(DefaultSemanticTags.Equipment.FRYER);
        defaultTags.add(DefaultSemanticTags.Equipment.ICE_MAKER);
        defaultTags.add(DefaultSemanticTags.Equipment.MICROWAVE);
        defaultTags.add(DefaultSemanticTags.Equipment.MIXER);
        defaultTags.add(DefaultSemanticTags.Equipment.OVEN);
        defaultTags.add(DefaultSemanticTags.Equipment.RANGE);
        defaultTags.add(DefaultSemanticTags.Equipment.REFRIGERATOR);
        defaultTags.add(DefaultSemanticTags.Equipment.TOASTER);
        defaultTags.add(DefaultSemanticTags.Equipment.WASHING_MACHINE);
        defaultTags.add(DefaultSemanticTags.Equipment.WINDOW);
        defaultTags.add(DefaultSemanticTags.Equipment.WINDOW_COVERING);
        defaultTags.add(DefaultSemanticTags.Equipment.DRAPES);
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
