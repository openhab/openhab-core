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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagImpl;

/**
 * This class defines all the default semantic tags.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
public class DefaultSemanticTags {

    public static final SemanticTag EQUIPMENT = new SemanticTagImpl("Equipment", "", "", "");
    public static final SemanticTag LOCATION = new SemanticTagImpl("Location", "", "", "");
    public static final SemanticTag POINT = new SemanticTagImpl("Point", "", "", "");
    public static final SemanticTag PROPERTY = new SemanticTagImpl("Property", "", "", "");

    public static class Location {
        public static final SemanticTag INDOOR = new SemanticTagImpl( //
                "Location_Indoor", //
                "Indoor", //
                "Anything that is inside a closed building", //
                "");
        public static final SemanticTag APARTMENT = new SemanticTagImpl( //
                "Location_Indoor_Apartment", //
                "Apartment", //
                "", //
                "Apartments");
        public static final SemanticTag BUILDING = new SemanticTagImpl( //
                "Location_Indoor_Building", //
                "Building", //
                "", //
                "Buildings");
        public static final SemanticTag GARAGE = new SemanticTagImpl( //
                "Location_Indoor_Building_Garage", //
                "Garage", //
                "", //
                "Garages");
        public static final SemanticTag HOUSE = new SemanticTagImpl( //
                "Location_Indoor_Building_House", //
                "House", //
                "", //
                "Houses");
        public static final SemanticTag SHED = new SemanticTagImpl( //
                "Location_Indoor_Building_Shed", //
                "Shed", //
                "", //
                "Sheds");
        public static final SemanticTag SUMMER_HOUSE = new SemanticTagImpl( //
                "Location_Indoor_Building_SummerHouse", //
                "Summer House", //
                "", //
                "Summer Houses, Second Home, Second Homes");
        public static final SemanticTag CORRIDOR = new SemanticTagImpl( //
                "Location_Indoor_Corridor", //
                "Corridor", //
                "", //
                "Corridors, Hallway, Hallways");
        public static final SemanticTag FLOOR = new SemanticTagImpl( //
                "Location_Indoor_Floor", //
                "Floor", //
                "", //
                "Floors");
        public static final SemanticTag ATTIC = new SemanticTagImpl( //
                "Location_Indoor_Floor_Attic", //
                "Attic", //
                "", //
                "Attics");
        public static final SemanticTag BASEMENT = new SemanticTagImpl( //
                "Location_Indoor_Floor_Basement", //
                "Basement", //
                "", //
                "Basements");
        public static final SemanticTag FIRST_FLOOR = new SemanticTagImpl( //
                "Location_Indoor_Floor_FirstFloor", //
                "First Floor", //
                "", //
                "First Floors, Upstairs");
        public static final SemanticTag GROUND_FLOOR = new SemanticTagImpl( //
                "Location_Indoor_Floor_GroundFloor", //
                "Ground Floor", //
                "", //
                "Ground Floors, Downstairs");
        public static final SemanticTag SECOND_FLOOR = new SemanticTagImpl( //
                "Location_Indoor_Floor_SecondFloor", //
                "Second Floor", //
                "", //
                "Second Floors");
        public static final SemanticTag THIRD_FLOOR = new SemanticTagImpl( //
                "Location_Indoor_Floor_ThirdFloor", //
                "Third Floor", //
                "", //
                "Third Floors");
        public static final SemanticTag ROOM = new SemanticTagImpl( //
                "Location_Indoor_Room", //
                "Room", //
                "", //
                "Rooms");
        public static final SemanticTag BATHROOM = new SemanticTagImpl( //
                "Location_Indoor_Room_Bathroom", //
                "Bathroom", //
                "", //
                "Bathrooms, Bath, Baths, Powder Room, Powder Rooms");
        public static final SemanticTag BEDROOM = new SemanticTagImpl( //
                "Location_Indoor_Room_Bedroom", //
                "Bedroom", //
                "", //
                "Bedrooms");
        public static final SemanticTag BOILER_ROOM = new SemanticTagImpl( //
                "Location_Indoor_Room_BoilerRoom", //
                "Boiler Room", //
                "", //
                "Boiler Rooms");
        public static final SemanticTag CELLAR = new SemanticTagImpl( //
                "Location_Indoor_Room_Cellar", //
                "Cellar", //
                "", //
                "Cellars");
        public static final SemanticTag DINING_ROOM = new SemanticTagImpl( //
                "Location_Indoor_Room_DiningRoom", //
                "Dining Room", //
                "", //
                "Dining Rooms");
        public static final SemanticTag ENTRY = new SemanticTagImpl( //
                "Location_Indoor_Room_Entry", //
                "Entry", //
                "", //
                "Entries, Foyer, Foyers");
        public static final SemanticTag FAMILY_ROOM = new SemanticTagImpl( //
                "Location_Indoor_Room_FamilyRoom", //
                "Family Room", //
                "", //
                "Family Rooms");
        public static final SemanticTag GUEST_ROOM = new SemanticTagImpl( //
                "Location_Indoor_Room_GuestRoom", //
                "Guest Room", //
                "", //
                "Guest Rooms");
        public static final SemanticTag KITCHEN = new SemanticTagImpl( //
                "Location_Indoor_Room_Kitchen", //
                "Kitchen", //
                "", //
                "Kitchens");
        public static final SemanticTag LAUNDRY_ROOM = new SemanticTagImpl( //
                "Location_Indoor_Room_LaundryRoom", //
                "Laundry Room", //
                "", //
                "Laundry Rooms");
        public static final SemanticTag LIVING_ROOM = new SemanticTagImpl( //
                "Location_Indoor_Room_LivingRoom", //
                "Living Room", //
                "", //
                "Living Rooms");
        public static final SemanticTag OFFICE = new SemanticTagImpl( //
                "Location_Indoor_Room_Office", //
                "Office", //
                "", //
                "Offices");
        public static final SemanticTag VERANDA = new SemanticTagImpl( //
                "Location_Indoor_Room_Veranda", //
                "Veranda", //
                "", //
                "Verandas");
        public static final SemanticTag OUTDOOR = new SemanticTagImpl( //
                "Location_Outdoor", //
                "Outdoor", //
                "", //
                "");
        public static final SemanticTag CARPORT = new SemanticTagImpl( //
                "Location_Outdoor_Carport", //
                "Carport", //
                "", //
                "Carports");
        public static final SemanticTag DRIVEWAY = new SemanticTagImpl( //
                "Location_Outdoor_Driveway", //
                "Driveway", //
                "", //
                "Driveways");
        public static final SemanticTag GARDEN = new SemanticTagImpl( //
                "Location_Outdoor_Garden", //
                "Garden", //
                "", //
                "Gardens");
        public static final SemanticTag PATIO = new SemanticTagImpl( //
                "Location_Outdoor_Patio", //
                "Patio", //
                "", //
                "Patios");
        public static final SemanticTag PORCH = new SemanticTagImpl( //
                "Location_Outdoor_Porch", //
                "Porch", //
                "", //
                "Porches");
        public static final SemanticTag TERRACE = new SemanticTagImpl( //
                "Location_Outdoor_Terrace", //
                "Terrace", //
                "", //
                "Terraces, Deck, Decks");
    }

    public static class Point {
        public static final SemanticTag ALARM = new SemanticTagImpl( //
                "Point_Alarm", //
                "Alarm", //
                "", //
                "");
        public static final SemanticTag CONTROL = new SemanticTagImpl( //
                "Point_Control", //
                "Control", //
                "", //
                "");
        public static final SemanticTag SWITCH = new SemanticTagImpl( //
                "Point_Control_Switch", //
                "Switch", //
                "", //
                "");
        public static final SemanticTag FORECAST = new SemanticTagImpl( //
                "Point_Forecast", //
                "Forecast", //
                "", //
                "");
        public static final SemanticTag MEASUREMENT = new SemanticTagImpl( //
                "Point_Measurement", //
                "Measurement", //
                "", //
                "");
        public static final SemanticTag SETPOINT = new SemanticTagImpl( //
                "Point_Setpoint", //
                "Setpoint", //
                "", //
                "");
        public static final SemanticTag STATUS = new SemanticTagImpl( //
                "Point_Status", //
                "Status", //
                "", //
                "");
    }

    public static class Property {
        public static final SemanticTag AIR_QUALITY = new SemanticTagImpl( //
                "Property_AirQuality", //
                "Air Quality", //
                "", //
                "");
        public static final SemanticTag AQI = new SemanticTagImpl( //
                "Property_AirQuality_AQI", //
                "AQI", //
                "", //
                "Air Quality Index");
        public static final SemanticTag CO = new SemanticTagImpl( //
                "Property_AirQuality_CO", //
                "CO", //
                "", //
                "Carbon Monoxide");
        public static final SemanticTag CO2 = new SemanticTagImpl( //
                "Property_AirQuality_CO2", //
                "CO2", //
                "", //
                "Carbon Dioxide");
        public static final SemanticTag OZONE = new SemanticTagImpl( //
                "Property_AirQuality_Ozone", //
                "Ozone", //
                "", //
                "");
        public static final SemanticTag PARTICULATE_MATTER = new SemanticTagImpl( //
                "Property_AirQuality_ParticulateMatter", //
                "Particulate Matter", //
                "", //
                "PM25");
        public static final SemanticTag POLLEN = new SemanticTagImpl( //
                "Property_AirQuality_Pollen", //
                "Pollen", //
                "", //
                "");
        public static final SemanticTag RADON = new SemanticTagImpl( //
                "Property_AirQuality_Radon", //
                "Radon", //
                "", //
                "");
        public static final SemanticTag VOC = new SemanticTagImpl( //
                "Property_AirQuality_VOC", //
                "VOC", //
                "", //
                "Volatile Organic Compounds");
        public static final SemanticTag AIRCONDITIONING = new SemanticTagImpl( //
                "Property_Airconditioning", //
                "Airconditioning", //
                "", //
                "");
        public static final SemanticTag AIRFLOW = new SemanticTagImpl( //
                "Property_Airflow", //
                "Airflow", //
                "", //
                "");
        public static final SemanticTag APP = new SemanticTagImpl( //
                "Property_App", //
                "App", //
                "Software program", //
                "Application");
        public static final SemanticTag BRIGHTNESS = new SemanticTagImpl( //
                "Property_Brightness", //
                "Brightness", //
                "", //
                "");
        public static final SemanticTag CHANNEL = new SemanticTagImpl( //
                "Property_Channel", //
                "Channel", //
                "", //
                "");
        public static final SemanticTag COLOR = new SemanticTagImpl( //
                "Property_Color", //
                "Color", //
                "", //
                "");
        public static final SemanticTag COLOR_TEMPERATURE = new SemanticTagImpl( //
                "Property_ColorTemperature", //
                "Color Temperature", //
                "", //
                "");
        public static final SemanticTag CURRENT = new SemanticTagImpl( //
                "Property_Current", //
                "Current", //
                "", //
                "");
        public static final SemanticTag DURATION = new SemanticTagImpl( //
                "Property_Duration", //
                "Duration", //
                "", //
                "");
        public static final SemanticTag ENABLED = new SemanticTagImpl( //
                "Property_Enabled", //
                "Enabled", //
                "", //
                "");
        public static final SemanticTag ENERGY = new SemanticTagImpl( //
                "Property_Energy", //
                "Energy", //
                "", //
                "");
        public static final SemanticTag FREQUENCY = new SemanticTagImpl( //
                "Property_Frequency", //
                "Frequency", //
                "", //
                "");
        public static final SemanticTag GAS = new SemanticTagImpl( //
                "Property_Gas", //
                "Gas", //
                "", //
                "");
        public static final SemanticTag HEATING = new SemanticTagImpl( //
                "Property_Heating", //
                "Heating", //
                "", //
                "");
        public static final SemanticTag HUMIDITY = new SemanticTagImpl( //
                "Property_Humidity", //
                "Humidity", //
                "", //
                "Moisture");
        public static final SemanticTag ILLUMINANCE = new SemanticTagImpl( //
                "Property_Illuminance", //
                "Illuminance", //
                "", //
                "");
        public static final SemanticTag LEVEL = new SemanticTagImpl( //
                "Property_Level", //
                "Level", //
                "", //
                "");
        public static final SemanticTag LIGHT = new SemanticTagImpl( //
                "Property_Light", //
                "Light", //
                "", //
                "Lights, Lighting");
        public static final SemanticTag LOW_BATTERY = new SemanticTagImpl( //
                "Property_LowBattery", //
                "Low Battery", //
                "", //
                "");
        public static final SemanticTag MEDIA_CONTROL = new SemanticTagImpl( //
                "Property_MediaControl", //
                "Media Control", //
                "", //
                "");
        public static final SemanticTag MODE = new SemanticTagImpl( //
                "Property_Mode", //
                "Mode", //
                "", //
                "");
        public static final SemanticTag MOISTURE = new SemanticTagImpl( //
                "Property_Moisture", //
                "Moisture", //
                "", //
                "");
        public static final SemanticTag MOTION = new SemanticTagImpl( //
                "Property_Motion", //
                "Motion", //
                "", //
                "");
        public static final SemanticTag NOISE = new SemanticTagImpl( //
                "Property_Noise", //
                "Noise", //
                "", //
                "");
        public static final SemanticTag OIL = new SemanticTagImpl( //
                "Property_Oil", //
                "Oil", //
                "", //
                "");
        public static final SemanticTag OPENING = new SemanticTagImpl( //
                "Property_Opening", //
                "Opening", //
                "", //
                "");
        public static final SemanticTag OPEN_LEVEL = new SemanticTagImpl( //
                "Property_Opening_OpenLevel", //
                "Open Level", //
                "", //
                "");
        public static final SemanticTag OPEN_STATE = new SemanticTagImpl( //
                "Property_Opening_OpenState", //
                "Open State", //
                "", //
                "Open Closed");
        public static final SemanticTag POSITION = new SemanticTagImpl( //
                "Property_Position", //
                "Position", //
                "", //
                "");
        public static final SemanticTag GEO_LOCATION = new SemanticTagImpl( //
                "Property_Position_GeoLocation", //
                "Geo Location", //
                "", //
                "");
        public static final SemanticTag POWER = new SemanticTagImpl( //
                "Property_Power", //
                "Power", //
                "", //
                "");
        public static final SemanticTag PRESENCE = new SemanticTagImpl( //
                "Property_Presence", //
                "Presence", //
                "", //
                "");
        public static final SemanticTag PRESSURE = new SemanticTagImpl( //
                "Property_Pressure", //
                "Pressure", //
                "", //
                "");
        public static final SemanticTag QUALITY_OF_SERVICE = new SemanticTagImpl( //
                "Property_QualityOfService", //
                "Quality of Service", //
                "", //
                "");
        public static final SemanticTag RAIN = new SemanticTagImpl( //
                "Property_Rain", //
                "Rain", //
                "", //
                "");
        public static final SemanticTag SIGNAL_STRENGTH = new SemanticTagImpl( //
                "Property_SignalStrength", //
                "Signal Strength", //
                "", //
                "");
        public static final SemanticTag RSSI = new SemanticTagImpl( //
                "Property_SignalStrength_RSSI", //
                "RSSI", //
                "", //
                "Received Signal Strength Indication");
        public static final SemanticTag SMOKE = new SemanticTagImpl( //
                "Property_Smoke", //
                "Smoke", //
                "", //
                "");
        public static final SemanticTag SOUND_VOLUME = new SemanticTagImpl( //
                "Property_SoundVolume", //
                "Sound Volume", //
                "", //
                "");
        public static final SemanticTag SPEED = new SemanticTagImpl( //
                "Property_Speed", //
                "Speed", //
                "", //
                "");
        public static final SemanticTag TAMPERED = new SemanticTagImpl( //
                "Property_Tampered", //
                "Tampered", //
                "", //
                "");
        public static final SemanticTag TEMPERATURE = new SemanticTagImpl( //
                "Property_Temperature", //
                "Temperature", //
                "", //
                "Temperatures");
        public static final SemanticTag TILT = new SemanticTagImpl( //
                "Property_Tilt", //
                "Tilt", //
                "", //
                "");
        public static final SemanticTag TIMESTAMP = new SemanticTagImpl( //
                "Property_Timestamp", //
                "Timestamp", //
                "", //
                "");
        public static final SemanticTag ULTRAVIOLET = new SemanticTagImpl( //
                "Property_Ultraviolet", //
                "Ultraviolet", //
                "", //
                "UV");
        public static final SemanticTag VENTILATION = new SemanticTagImpl( //
                "Property_Ventilation", //
                "Ventilation", //
                "", //
                "");
        public static final SemanticTag VIBRATION = new SemanticTagImpl( //
                "Property_Vibration", //
                "Vibration", //
                "", //
                "");
        public static final SemanticTag VOLTAGE = new SemanticTagImpl( //
                "Property_Voltage", //
                "Voltage", //
                "", //
                "");
        public static final SemanticTag WATER = new SemanticTagImpl( //
                "Property_Water", //
                "Water", //
                "", //
                "");
        public static final SemanticTag WIND = new SemanticTagImpl( //
                "Property_Wind", //
                "Wind", //
                "", //
                "");
    }

    public static class Equipment {
        public static final SemanticTag ALARM_DEVICE = new SemanticTagImpl( //
                "Equipment_AlarmDevice", //
                "Alarm Device", //
                "", //
                "");
        public static final SemanticTag ALARM_SYSTEM = new SemanticTagImpl( //
                "Equipment_AlarmSystem", //
                "Alarm System", //
                "", //
                "Alarm Systems");
        public static final SemanticTag AUDIO_VISUAL = new SemanticTagImpl( //
                "Equipment_AudioVisual", //
                "Audio Visual", //
                "", //
                "");
        public static final SemanticTag DISPLAY = new SemanticTagImpl( //
                "Equipment_AudioVisual_Display", //
                "Display", //
                "", //
                "");
        public static final SemanticTag PROJECTOR = new SemanticTagImpl( //
                "Equipment_AudioVisual_Display_Projector", //
                "Projector", //
                "", //
                "Projectors, Beamer, Beamers");
        public static final SemanticTag TELEVISION = new SemanticTagImpl( //
                "Equipment_AudioVisual_Display_Television", //
                "Television", //
                "", //
                "Televisions, TV, TVs");
        public static final SemanticTag MEDIA_PLAYER = new SemanticTagImpl( //
                "Equipment_AudioVisual_MediaPlayer", //
                "Media Player", //
                "", //
                "");
        public static final SemanticTag RECEIVER = new SemanticTagImpl( //
                "Equipment_AudioVisual_Receiver", //
                "Receiver", //
                "", //
                "Receivers, Audio Receiver, Audio Receivers, AV Receiver, AV Receivers");
        public static final SemanticTag SCREEN = new SemanticTagImpl( //
                "Equipment_AudioVisual_Screen", //
                "Screen", //
                "", //
                "Screens");
        public static final SemanticTag SPEAKER = new SemanticTagImpl( //
                "Equipment_AudioVisual_Speaker", //
                "Speaker", //
                "", //
                "Speakers");
        public static final SemanticTag BATTERY = new SemanticTagImpl( //
                "Equipment_Battery", //
                "Battery", //
                "", //
                "Batteries");
        public static final SemanticTag BED = new SemanticTagImpl( //
                "Equipment_Bed", //
                "Bed", //
                "", //
                "");
        public static final SemanticTag CAMERA = new SemanticTagImpl( //
                "Equipment_Camera", //
                "Camera", //
                "", //
                "Cameras");
        public static final SemanticTag CLEANING_ROBOT = new SemanticTagImpl( //
                "Equipment_CleaningRobot", //
                "Cleaning Robot", //
                "", //
                "Cleaning Robots, Vacuum robot, Vacuum robots");
        public static final SemanticTag COMPUTER = new SemanticTagImpl( //
                "Equipment_Computer", //
                "Computer", //
                "", //
                "");
        public static final SemanticTag CONTROL_DEVICE = new SemanticTagImpl( //
                "Equipment_ControlDevice", //
                "Control Device", //
                "", //
                "");
        public static final SemanticTag BUTTON = new SemanticTagImpl( //
                "Equipment_ControlDevice_Button", //
                "Button", //
                "", //
                "");
        public static final SemanticTag DIAL = new SemanticTagImpl( //
                "Equipment_ControlDevice_Dial", //
                "Dial", //
                "", //
                "Rotary Dial");
        public static final SemanticTag KEYPAD = new SemanticTagImpl( //
                "Equipment_ControlDevice_Keypad", //
                "Keypad", //
                "", //
                "");
        public static final SemanticTag SLIDER = new SemanticTagImpl( //
                "Equipment_ControlDevice_Slider", //
                "Slider", //
                "", //
                "");
        public static final SemanticTag WALL_SWITCH = new SemanticTagImpl( //
                "Equipment_ControlDevice_WallSwitch", //
                "Wall Switch", //
                "", //
                "Wall Switches");
        public static final SemanticTag DOOR = new SemanticTagImpl( //
                "Equipment_Door", //
                "Door", //
                "", //
                "Doors");
        public static final SemanticTag BACK_DOOR = new SemanticTagImpl( //
                "Equipment_Door_BackDoor", //
                "Back Door", //
                "", //
                "Back Doors");
        public static final SemanticTag CELLAR_DOOR = new SemanticTagImpl( //
                "Equipment_Door_CellarDoor", //
                "Cellar Door", //
                "", //
                "Cellar Doors");
        public static final SemanticTag FRONT_DOOR = new SemanticTagImpl( //
                "Equipment_Door_FrontDoor", //
                "Front Door", //
                "", //
                "Front Doors, Frontdoor, Frontdoors");
        public static final SemanticTag GARAGE_DOOR = new SemanticTagImpl( //
                "Equipment_Door_GarageDoor", //
                "Garage Door", //
                "", //
                "Garage Doors");
        public static final SemanticTag GATE = new SemanticTagImpl( //
                "Equipment_Door_Gate", //
                "Gate", //
                "", //
                "Gates");
        public static final SemanticTag INNER_DOOR = new SemanticTagImpl( //
                "Equipment_Door_InnerDoor", //
                "Inner Door", //
                "", //
                "Inner Doors");
        public static final SemanticTag SIDE_DOOR = new SemanticTagImpl( //
                "Equipment_Door_SideDoor", //
                "Side Door", //
                "", //
                "Side Doors");
        public static final SemanticTag DOORBELL = new SemanticTagImpl( //
                "Equipment_Doorbell", //
                "Doorbell", //
                "", //
                "Doorbells");
        public static final SemanticTag DRINKING_WATER = new SemanticTagImpl( //
                "Equipment_DrinkingWater", //
                "Drinking Water", //
                "", //
                "Potable Water");
        public static final SemanticTag HOT_WATER_FAUCET = new SemanticTagImpl( //
                "Equipment_DrinkingWater_HotWaterFaucet", //
                "Hot Water Faucet", //
                "", //
                "Hot Water Tap, Boiling Water Tap, Boiling Water Faucet");
        public static final SemanticTag WATER_FILTER = new SemanticTagImpl( //
                "Equipment_DrinkingWater_WaterFilter", //
                "Water Filter", //
                "", //
                "");
        public static final SemanticTag WATER_SOFTENER = new SemanticTagImpl( //
                "Equipment_DrinkingWater_WaterSoftener", //
                "Water Softener", //
                "", //
                "");
        public static final SemanticTag HVAC = new SemanticTagImpl( //
                "Equipment_HVAC", //
                "HVAC", //
                "", //
                "Heating, Ventilation, Air Conditioning, A/C, A/Cs, AC");
        public static final SemanticTag AIR_CONDITIONER = new SemanticTagImpl( //
                "Equipment_HVAC_AirConditioner", //
                "Air Conditioner", //
                "", //
                "");
        public static final SemanticTag AIR_FILTER = new SemanticTagImpl( //
                "Equipment_HVAC_AirFilter", //
                "Air Filter", //
                "", //
                "");
        public static final SemanticTag BOILER = new SemanticTagImpl( //
                "Equipment_HVAC_Boiler", //
                "Boiler", //
                "", //
                "Boilers");
        public static final SemanticTag DEHUMIDIFIER = new SemanticTagImpl( //
                "Equipment_HVAC_Dehumidifier", //
                "Dehumidifier", //
                "", //
                "");
        public static final SemanticTag FAN = new SemanticTagImpl( //
                "Equipment_HVAC_Fan", //
                "Fan", //
                "", //
                "Fans");
        public static final SemanticTag CEILING_FAN = new SemanticTagImpl( //
                "Equipment_HVAC_Fan_CeilingFan", //
                "Ceiling Fan", //
                "", //
                "Ceiling Fans");
        public static final SemanticTag EXHAUST_FAN = new SemanticTagImpl( //
                "Equipment_HVAC_Fan_ExhaustFan", //
                "Exhaust Fan", //
                "", //
                "Extract Fan, Toilet Fan");
        public static final SemanticTag KITCHEN_HOOD = new SemanticTagImpl( //
                "Equipment_HVAC_Fan_KitchenHood", //
                "Kitchen Hood", //
                "", //
                "Kitchen Hoods");
        public static final SemanticTag FLOOR_HEATING = new SemanticTagImpl( //
                "Equipment_HVAC_FloorHeating", //
                "Floor Heating", //
                "", //
                "Underfloor Heating, Radiant Floor Heating");
        public static final SemanticTag FURNACE = new SemanticTagImpl( //
                "Equipment_HVAC_Furnace", //
                "Furnace", //
                "", //
                "Wood Burner, Wood Heater");
        public static final SemanticTag HEAT_PUMP = new SemanticTagImpl( //
                "Equipment_HVAC_HeatPump", //
                "Heat Pump", //
                "", //
                "");
        public static final SemanticTag HEAT_RECOVERY = new SemanticTagImpl( //
                "Equipment_HVAC_HeatRecovery", //
                "Heat Recovery", //
                "", //
                "Energy Recovery");
        public static final SemanticTag HUMIDIFIER = new SemanticTagImpl( //
                "Equipment_HVAC_Humidifier", //
                "Humidifier", //
                "", //
                "");
        public static final SemanticTag RADIATOR_CONTROL = new SemanticTagImpl( //
                "Equipment_HVAC_RadiatorControl", //
                "Radiator Control", //
                "", //
                "Radiator Controls, Radiator, Radiators");
        public static final SemanticTag SMART_VENT = new SemanticTagImpl( //
                "Equipment_HVAC_SmartVent", //
                "Smart Vent", //
                "", //
                "");
        public static final SemanticTag THERMOSTAT = new SemanticTagImpl( //
                "Equipment_HVAC_Thermostat", //
                "Thermostat", //
                "", //
                "");
        public static final SemanticTag WATER_HEATER = new SemanticTagImpl( //
                "Equipment_HVAC_WaterHeater", //
                "Water Heater", //
                "", //
                "Water Boiler");
        public static final SemanticTag HORTICULTURE = new SemanticTagImpl( //
                "Equipment_Horticulture", //
                "Horticulture", //
                "", //
                "");
        public static final SemanticTag IRRIGATION = new SemanticTagImpl( //
                "Equipment_Horticulture_Irrigation", //
                "Irrigation", //
                "", //
                "Sprinkler, Drip System");
        public static final SemanticTag LAWN_MOWER = new SemanticTagImpl( //
                "Equipment_Horticulture_LawnMower", //
                "Lawn Mower", //
                "", //
                "Lawn Mowers");
        public static final SemanticTag SOIL_SENSOR = new SemanticTagImpl( //
                "Equipment_Horticulture_SoilSensor", //
                "Soil Sensor", //
                "", //
                "Moisture Sensor");
        public static final SemanticTag LIGHT_SOURCE = new SemanticTagImpl( //
                "Equipment_LightSource", //
                "Light Source", //
                "", //
                "Lights, Lighting");
        public static final SemanticTag ACCENT_LIGHT = new SemanticTagImpl( //
                "Equipment_LightSource_AccentLight", //
                "Accent Light", //
                "", //
                "");
        public static final SemanticTag CHANDELIER = new SemanticTagImpl( //
                "Equipment_LightSource_Chandelier", //
                "Chandelier", //
                "", //
                "");
        public static final SemanticTag DOWNLIGHT = new SemanticTagImpl( //
                "Equipment_LightSource_Downlight", //
                "Downlight", //
                "", //
                "Can Light, Pot Light");
        public static final SemanticTag FLOOD_LIGHT = new SemanticTagImpl( //
                "Equipment_LightSource_FloodLight", //
                "Flood Light", //
                "", //
                "");
        public static final SemanticTag LAMP = new SemanticTagImpl( //
                "Equipment_LightSource_Lamp", //
                "Lamp", //
                "", //
                "");
        public static final SemanticTag LIGHT_STRIP = new SemanticTagImpl( //
                "Equipment_LightSource_LightStrip", //
                "Light Strip", //
                "", //
                "LED Strip");
        public static final SemanticTag LIGHT_STRIPE = new SemanticTagImpl( //
                "Equipment_LightSource_LightStripe", //
                "Light Stripe", //
                "", //
                "Light Stripes");
        public static final SemanticTag LIGHTBULB = new SemanticTagImpl( //
                "Equipment_LightSource_Lightbulb", //
                "Light Bulb", //
                "", //
                "Lightbulbs, Bulb, Bulbs, Lamp, Lamps");
        public static final SemanticTag PENDANT = new SemanticTagImpl( //
                "Equipment_LightSource_Pendant", //
                "Pendant", //
                "", //
                "");
        public static final SemanticTag SCONCE = new SemanticTagImpl( //
                "Equipment_LightSource_Sconce", //
                "Sconce", //
                "", //
                "");
        public static final SemanticTag SPOT_LIGHT = new SemanticTagImpl( //
                "Equipment_LightSource_SpotLight", //
                "Spot Light", //
                "", //
                "");
        public static final SemanticTag TRACK_LIGHT = new SemanticTagImpl( //
                "Equipment_LightSource_TrackLight", //
                "Track Light", //
                "", //
                "");
        public static final SemanticTag WALL_LIGHT = new SemanticTagImpl( //
                "Equipment_LightSource_WallLight", //
                "Wall Light", //
                "", //
                "");
        public static final SemanticTag LOCK = new SemanticTagImpl( //
                "Equipment_Lock", //
                "Lock", //
                "", //
                "Locks");
        public static final SemanticTag NETWORK_APPLIANCE = new SemanticTagImpl( //
                "Equipment_NetworkAppliance", //
                "Network Appliance", //
                "", //
                "Network Appliances");
        public static final SemanticTag FIREWALL = new SemanticTagImpl( //
                "Equipment_NetworkAppliance_Firewall", //
                "Firewall", //
                "", //
                "");
        public static final SemanticTag NETWORK_SWITCH = new SemanticTagImpl( //
                "Equipment_NetworkAppliance_NetworkSwitch", //
                "Network Switch", //
                "", //
                "");
        public static final SemanticTag ROUTER = new SemanticTagImpl( //
                "Equipment_NetworkAppliance_Router", //
                "Router", //
                "", //
                "");
        public static final SemanticTag WIRELESS_ACCESS_POINT = new SemanticTagImpl( //
                "Equipment_NetworkAppliance_WirelessAccessPoint", //
                "Wireless Access Point", //
                "", //
                "Access Point, WAP, WiFi, WiFi Access Point");
        public static final SemanticTag PET_CARE = new SemanticTagImpl( //
                "Equipment_PetCare", //
                "Pet Care", //
                "", //
                "");
        public static final SemanticTag AQUARIUM = new SemanticTagImpl( //
                "Equipment_PetCare_Aquarium", //
                "Aquarium", //
                "", //
                "Fish Tank");
        public static final SemanticTag PET_FEEDER = new SemanticTagImpl( //
                "Equipment_PetCare_PetFeeder", //
                "Pet Feeder", //
                "", //
                "");
        public static final SemanticTag PET_FLAP = new SemanticTagImpl( //
                "Equipment_PetCare_PetFlap", //
                "Pet Flap", //
                "", //
                "Cat Flap, Dog Flap");
        public static final SemanticTag POWER_OUTLET = new SemanticTagImpl( //
                "Equipment_PowerOutlet", //
                "Power Outlet", //
                "", //
                "Power Outlets, Outlet, Outlets, Smart Plug, Smart Plugs");
        public static final SemanticTag POWER_SUPPLY = new SemanticTagImpl( //
                "Equipment_PowerSupply", //
                "Power Supply", //
                "", //
                "");
        public static final SemanticTag EVSE = new SemanticTagImpl( //
                "Equipment_PowerSupply_EVSE", //
                "Electric Vehicle Supply Equipment", //
                "", //
                "EV Charger, Car Charger");
        public static final SemanticTag GENERATOR = new SemanticTagImpl( //
                "Equipment_PowerSupply_Generator", //
                "Generator", //
                "", //
                "Emergency Generator");
        public static final SemanticTag INVERTER = new SemanticTagImpl( //
                "Equipment_PowerSupply_Inverter", //
                "Inverter", //
                "", //
                "Inverters");
        public static final SemanticTag SOLAR_PANEL = new SemanticTagImpl( //
                "Equipment_PowerSupply_SolarPanel", //
                "Solar Panel", //
                "", //
                "");
        public static final SemanticTag TRANSFER_SWITCH = new SemanticTagImpl( //
                "Equipment_PowerSupply_TransferSwitch", //
                "Transfer Switch", //
                "", //
                "");
        public static final SemanticTag UPS = new SemanticTagImpl( //
                "Equipment_PowerSupply_UPS", //
                "UPS", //
                "", //
                "Uninterruptible Power Supply");
        public static final SemanticTag WIND_GENERATOR = new SemanticTagImpl( //
                "Equipment_PowerSupply_WindGenerator", //
                "Wind Generator", //
                "", //
                "Wind Turbine");
        public static final SemanticTag PRINTER = new SemanticTagImpl( //
                "Equipment_Printer", //
                "Printer", //
                "", //
                "");
        public static final SemanticTag PRINTER3D = new SemanticTagImpl( //
                "Equipment_Printer_Printer3D", //
                "3D Printer", //
                "", //
                "");
        public static final SemanticTag PUMP = new SemanticTagImpl( //
                "Equipment_Pump", //
                "Pump", //
                "", //
                "Pumps");
        public static final SemanticTag WATER_FEATURE = new SemanticTagImpl( //
                "Equipment_Pump_WaterFeature", //
                "Water Feature", //
                "", //
                "Waterfall, Pond Pump");
        public static final SemanticTag REMOTE_CONTROL = new SemanticTagImpl( //
                "Equipment_RemoteControl", //
                "Remote Control", //
                "", //
                "Remote Controls");
        public static final SemanticTag SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor", //
                "Sensor", //
                "", //
                "Sensors");
        public static final SemanticTag AIR_QUALITY_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_AirQualitySensor", //
                "Air Quality Sensor", //
                "", //
                "");
        public static final SemanticTag CO2_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_CO2Sensor", //
                "CO2 Sensor", //
                "", //
                "");
        public static final SemanticTag CO_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_COSensor", //
                "CO Sensor", //
                "", //
                "");
        public static final SemanticTag CONTACT_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_ContactSensor", //
                "Contact Sensor", //
                "", //
                "");
        public static final SemanticTag ELECTRIC_METER = new SemanticTagImpl( //
                "Equipment_Sensor_ElectricMeter", //
                "Electric Meter", //
                "", //
                "");
        public static final SemanticTag FIRE_DETECTOR = new SemanticTagImpl( //
                "Equipment_Sensor_FireDetector", //
                "Fire Detector", //
                "", //
                "");
        public static final SemanticTag FLAME_DETECTOR = new SemanticTagImpl( //
                "Equipment_Sensor_FireDetector_FlameDetector", //
                "Flame Detector", //
                "", //
                "");
        public static final SemanticTag HEAT_DETECTOR = new SemanticTagImpl( //
                "Equipment_Sensor_FireDetector_HeatDetector", //
                "Heat Detector", //
                "", //
                "");
        public static final SemanticTag SMOKE_DETECTOR = new SemanticTagImpl( //
                "Equipment_Sensor_FireDetector_SmokeDetector", //
                "Smoke Detector", //
                "", //
                "Smoke Detectors");
        public static final SemanticTag GAS_METER = new SemanticTagImpl( //
                "Equipment_Sensor_GasMeter", //
                "Gas Meter", //
                "", //
                "");
        public static final SemanticTag GLASS_BREAK_DETECTOR = new SemanticTagImpl( //
                "Equipment_Sensor_GlassBreakDetector", //
                "Glass Break Detector", //
                "", //
                "");
        public static final SemanticTag HUMIDITY_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_HumiditySensor", //
                "Humidity Sensor", //
                "", //
                "Hygrometer");
        public static final SemanticTag ILLUMINANCE_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_IlluminanceSensor", //
                "Illuminance Sensor", //
                "", //
                "");
        public static final SemanticTag LEAK_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_LeakSensor", //
                "Leak Sensor", //
                "", //
                "");
        public static final SemanticTag OCCUPANCY_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_OccupancySensor", //
                "Occupancy Sensor", //
                "", //
                "");
        public static final SemanticTag MOTION_DETECTOR = new SemanticTagImpl( //
                "Equipment_Sensor_OccupancySensor_MotionDetector", //
                "Motion Detector", //
                "", //
                "Motion Detectors, Motion Sensor, Motion Sensors");
        public static final SemanticTag TEMPERATURE_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_TemperatureSensor", //
                "Temperature Sensor", //
                "", //
                "");
        public static final SemanticTag VIBRATION_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_VibrationSensor", //
                "Vibration Sensor", //
                "", //
                "");
        public static final SemanticTag WATER_METER = new SemanticTagImpl( //
                "Equipment_Sensor_WaterMeter", //
                "Water Meter", //
                "", //
                "");
        public static final SemanticTag WATER_QUALITY_SENSOR = new SemanticTagImpl( //
                "Equipment_Sensor_WaterQualitySensor", //
                "Water Quality Sensor", //
                "", //
                "");
        public static final SemanticTag WEATHER_STATION = new SemanticTagImpl( //
                "Equipment_Sensor_WeatherStation", //
                "Weather Station", //
                "", //
                "");
        public static final SemanticTag SIREN = new SemanticTagImpl( //
                "Equipment_Siren", //
                "Siren", //
                "", //
                "Sirens");
        public static final SemanticTag SMARTPHONE = new SemanticTagImpl( //
                "Equipment_Smartphone", //
                "Smartphone", //
                "", //
                "Smartphones, Phone, Phones");
        public static final SemanticTag TOOL = new SemanticTagImpl( //
                "Equipment_Tool", //
                "Tool", //
                "", //
                "");
        public static final SemanticTag TRACKER = new SemanticTagImpl( //
                "Equipment_Tracker", //
                "Tracker", //
                "", //
                "");
        public static final SemanticTag VALVE = new SemanticTagImpl( //
                "Equipment_Valve", //
                "Valve", //
                "", //
                "Valves");
        public static final SemanticTag VEHICLE = new SemanticTagImpl( //
                "Equipment_Vehicle", //
                "Vehicle", //
                "", //
                "");
        public static final SemanticTag CAR = new SemanticTagImpl( //
                "Equipment_Vehicle_Car", //
                "Car", //
                "", //
                "");
        public static final SemanticTag VOICE_ASSISTANT = new SemanticTagImpl( //
                "Equipment_VoiceAssistant", //
                "Voice Assistant", //
                "", //
                "Voice Assistants");
        public static final SemanticTag WEB_SERVICE = new SemanticTagImpl( //
                "Equipment_WebService", //
                "Web Service", //
                "", //
                "Web Services");
        public static final SemanticTag WEATHER_SERVICE = new SemanticTagImpl( //
                "Equipment_WebService_WeatherService", //
                "Weather Service", //
                "", //
                "Weather Services");
        public static final SemanticTag WELLNESS = new SemanticTagImpl( //
                "Equipment_Wellness", //
                "Wellness", //
                "", //
                "");
        public static final SemanticTag CHLORINATOR = new SemanticTagImpl( //
                "Equipment_Wellness_Chlorinator", //
                "Chlorinator", //
                "", //
                "");
        public static final SemanticTag JACUZZI = new SemanticTagImpl( //
                "Equipment_Wellness_Jacuzzi", //
                "Jacuzzi", //
                "", //
                "Spa, Hot Tub, Whirlpool");
        public static final SemanticTag POOL_COVER = new SemanticTagImpl( //
                "Equipment_Wellness_PoolCover", //
                "Pool Cover", //
                "", //
                "");
        public static final SemanticTag POOL_HEATER = new SemanticTagImpl( //
                "Equipment_Wellness_PoolHeater", //
                "Pool Heater", //
                "", //
                "");
        public static final SemanticTag SAUNA = new SemanticTagImpl( //
                "Equipment_Wellness_Sauna", //
                "Sauna", //
                "", //
                "Steam Room");
        public static final SemanticTag SHOWER = new SemanticTagImpl( //
                "Equipment_Wellness_Shower", //
                "Shower", //
                "", //
                "");
        public static final SemanticTag SWIMMING_POOL = new SemanticTagImpl( //
                "Equipment_Wellness_SwimmingPool", //
                "Swimming Pool", //
                "", //
                "Swimming Pool, Pool");
        public static final SemanticTag WHITE_GOOD = new SemanticTagImpl( //
                "Equipment_WhiteGood", //
                "White Good", //
                "", //
                "White Goods");
        public static final SemanticTag AIR_FRYER = new SemanticTagImpl( //
                "Equipment_WhiteGood_AirFryer", //
                "Air Fryer", //
                "", //
                "");
        public static final SemanticTag COFFEE_MAKER = new SemanticTagImpl( //
                "Equipment_WhiteGood_CoffeeMaker", //
                "Coffee Maker", //
                "", //
                "Coffee Makers, Coffee Machine, Coffee Machines");
        public static final SemanticTag COOKTOP = new SemanticTagImpl( //
                "Equipment_WhiteGood_Cooktop", //
                "Cooktop", //
                "", //
                "Hob");
        public static final SemanticTag DISHWASHER = new SemanticTagImpl( //
                "Equipment_WhiteGood_Dishwasher", //
                "Dishwasher", //
                "", //
                "Dishwashers");
        public static final SemanticTag DRYER = new SemanticTagImpl( //
                "Equipment_WhiteGood_Dryer", //
                "Dryer", //
                "", //
                "Dryers, Tumble Dryer, Tumble Dryers");
        public static final SemanticTag FOOD_PROCESSOR = new SemanticTagImpl( //
                "Equipment_WhiteGood_FoodProcessor", //
                "Food Processor", //
                "", //
                "");
        public static final SemanticTag FREEZER = new SemanticTagImpl( //
                "Equipment_WhiteGood_Freezer", //
                "Freezer", //
                "", //
                "Freezers");
        public static final SemanticTag FRYER = new SemanticTagImpl( //
                "Equipment_WhiteGood_Fryer", //
                "Fryer", //
                "", //
                "");
        public static final SemanticTag ICE_MAKER = new SemanticTagImpl( //
                "Equipment_WhiteGood_IceMaker", //
                "Ice Maker", //
                "", //
                "");
        public static final SemanticTag MICROWAVE = new SemanticTagImpl( //
                "Equipment_WhiteGood_Microwave", //
                "Microwave", //
                "", //
                "");
        public static final SemanticTag MIXER = new SemanticTagImpl( //
                "Equipment_WhiteGood_Mixer", //
                "Mixer", //
                "", //
                "");
        public static final SemanticTag OVEN = new SemanticTagImpl( //
                "Equipment_WhiteGood_Oven", //
                "Oven", //
                "", //
                "Ovens");
        public static final SemanticTag RANGE = new SemanticTagImpl( //
                "Equipment_WhiteGood_Range", //
                "Range", //
                "", //
                "");
        public static final SemanticTag REFRIGERATOR = new SemanticTagImpl( //
                "Equipment_WhiteGood_Refrigerator", //
                "Refrigerator", //
                "", //
                "Refrigerators");
        public static final SemanticTag TOASTER = new SemanticTagImpl( //
                "Equipment_WhiteGood_Toaster", //
                "Toaster", //
                "", //
                "Toaster Oven");
        public static final SemanticTag WASHING_MACHINE = new SemanticTagImpl( //
                "Equipment_WhiteGood_WashingMachine", //
                "Washing Machine", //
                "", //
                "Washing Machines");
        public static final SemanticTag WINDOW = new SemanticTagImpl( //
                "Equipment_Window", //
                "Window", //
                "", //
                "Windows");
        public static final SemanticTag WINDOW_COVERING = new SemanticTagImpl( //
                "Equipment_WindowCovering", //
                "Window Covering", //
                "", //
                "");
        public static final SemanticTag BLINDS = new SemanticTagImpl( //
                "Equipment_WindowCovering_Blinds", //
                "Blinds", //
                "", //
                "Rollershutter, Rollershutters, Roller shutter, Roller shutters, Shutter, Shutters");
        public static final SemanticTag DRAPES = new SemanticTagImpl( //
                "Equipment_WindowCovering_Drapes", //
                "Drapes", //
                "", //
                "Curtains");
    }
}
