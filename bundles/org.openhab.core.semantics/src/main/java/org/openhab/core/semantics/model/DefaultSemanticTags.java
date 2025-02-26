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

    public static final SemanticTag LOCATION_INDOOR = new SemanticTagImpl( //
            "Location_Indoor", //
            "Indoor", //
            "Anything that is inside a closed building", //
            "");
    public static final SemanticTag LOCATION_INDOOR_APARTMENT = new SemanticTagImpl( //
            "Location_Indoor_Apartment", //
            "Apartment", //
            "", //
            "Apartments");
    public static final SemanticTag LOCATION_INDOOR_BUILDING = new SemanticTagImpl( //
            "Location_Indoor_Building", //
            "Building", //
            "", //
            "Buildings");
    public static final SemanticTag LOCATION_INDOOR_BUILDING_GARAGE = new SemanticTagImpl( //
            "Location_Indoor_Building_Garage", //
            "Garage", //
            "", //
            "Garages");
    public static final SemanticTag LOCATION_INDOOR_BUILDING_HOUSE = new SemanticTagImpl( //
            "Location_Indoor_Building_House", //
            "House", //
            "", //
            "Houses");
    public static final SemanticTag LOCATION_INDOOR_BUILDING_SHED = new SemanticTagImpl( //
            "Location_Indoor_Building_Shed", //
            "Shed", //
            "", //
            "Sheds");
    public static final SemanticTag LOCATION_INDOOR_BUILDING_SUMMERHOUSE = new SemanticTagImpl( //
            "Location_Indoor_Building_SummerHouse", //
            "Summer House", //
            "", //
            "Summer Houses, Second Home, Second Homes");
    public static final SemanticTag LOCATION_INDOOR_FLOOR = new SemanticTagImpl( //
            "Location_Indoor_Floor", //
            "Floor", //
            "", //
            "Floors");
    public static final SemanticTag LOCATION_INDOOR_FLOOR_GROUNDFLOOR = new SemanticTagImpl( //
            "Location_Indoor_Floor_GroundFloor", //
            "Ground Floor", //
            "", //
            "Ground Floors, Downstairs");
    public static final SemanticTag LOCATION_INDOOR_FLOOR_FIRSTFLOOR = new SemanticTagImpl( //
            "Location_Indoor_Floor_FirstFloor", //
            "First Floor", //
            "", //
            "First Floors, Upstairs");
    public static final SemanticTag LOCATION_INDOOR_FLOOR_SECONDFLOOR = new SemanticTagImpl( //
            "Location_Indoor_Floor_SecondFloor", //
            "Second Floor", //
            "", //
            "Second Floors");
    public static final SemanticTag LOCATION_INDOOR_FLOOR_THIRDFLOOR = new SemanticTagImpl( //
            "Location_Indoor_Floor_ThirdFloor", //
            "Third Floor", //
            "", //
            "Third Floors");
    public static final SemanticTag LOCATION_INDOOR_FLOOR_ATTIC = new SemanticTagImpl( //
            "Location_Indoor_Floor_Attic", //
            "Attic", //
            "", //
            "Attics");
    public static final SemanticTag LOCATION_INDOOR_FLOOR_BASEMENT = new SemanticTagImpl( //
            "Location_Indoor_Floor_Basement", //
            "Basement", //
            "", //
            "Basements");
    public static final SemanticTag LOCATION_INDOOR_CORRIDOR = new SemanticTagImpl( //
            "Location_Indoor_Corridor", //
            "Corridor", //
            "", //
            "Corridors, Hallway, Hallways");
    public static final SemanticTag LOCATION_INDOOR_ROOM = new SemanticTagImpl( //
            "Location_Indoor_Room", //
            "Room", //
            "", //
            "Rooms");
    public static final SemanticTag LOCATION_INDOOR_ROOM_BATHROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_Bathroom", //
            "Bathroom", //
            "", //
            "Bathrooms, Bath, Baths, Powder Room, Powder Rooms");
    public static final SemanticTag LOCATION_INDOOR_ROOM_BEDROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_Bedroom", //
            "Bedroom", //
            "", //
            "Bedrooms");
    public static final SemanticTag LOCATION_INDOOR_ROOM_BOILERROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_BoilerRoom", //
            "Boiler Room", //
            "", //
            "Boiler Rooms");
    public static final SemanticTag LOCATION_INDOOR_ROOM_CELLAR = new SemanticTagImpl( //
            "Location_Indoor_Room_Cellar", //
            "Cellar", //
            "", //
            "Cellars");
    public static final SemanticTag LOCATION_INDOOR_ROOM_DININGROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_DiningRoom", //
            "Dining Room", //
            "", //
            "Dining Rooms");
    public static final SemanticTag LOCATION_INDOOR_ROOM_ENTRY = new SemanticTagImpl( //
            "Location_Indoor_Room_Entry", //
            "Entry", //
            "", //
            "Entries, Foyer, Foyers");
    public static final SemanticTag LOCATION_INDOOR_ROOM_FAMILYROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_FamilyRoom", //
            "Family Room", //
            "", //
            "Family Rooms");
    public static final SemanticTag LOCATION_INDOOR_ROOM_GUESTROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_GuestRoom", //
            "Guest Room", //
            "", //
            "Guest Rooms");
    public static final SemanticTag LOCATION_INDOOR_ROOM_KITCHEN = new SemanticTagImpl( //
            "Location_Indoor_Room_Kitchen", //
            "Kitchen", //
            "", //
            "Kitchens");
    public static final SemanticTag LOCATION_INDOOR_ROOM_LAUNDRYROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_LaundryRoom", //
            "Laundry Room", //
            "", //
            "Laundry Rooms");
    public static final SemanticTag LOCATION_INDOOR_ROOM_LIVINGROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_LivingRoom", //
            "Living Room", //
            "", //
            "Living Rooms");
    public static final SemanticTag LOCATION_INDOOR_ROOM_OFFICE = new SemanticTagImpl( //
            "Location_Indoor_Room_Office", //
            "Office", //
            "", //
            "Offices");
    public static final SemanticTag LOCATION_INDOOR_ROOM_VERANDA = new SemanticTagImpl( //
            "Location_Indoor_Room_Veranda", //
            "Veranda", //
            "", //
            "Verandas");
    public static final SemanticTag LOCATION_OUTDOOR = new SemanticTagImpl( //
            "Location_Outdoor", //
            "Outdoor", //
            "", //
            "");
    public static final SemanticTag LOCATION_OUTDOOR_CARPORT = new SemanticTagImpl( //
            "Location_Outdoor_Carport", //
            "Carport", //
            "", //
            "Carports");
    public static final SemanticTag LOCATION_OUTDOOR_DRIVEWAY = new SemanticTagImpl( //
            "Location_Outdoor_Driveway", //
            "Driveway", //
            "", //
            "Driveways");
    public static final SemanticTag LOCATION_OUTDOOR_GARDEN = new SemanticTagImpl( //
            "Location_Outdoor_Garden", //
            "Garden", //
            "", //
            "Gardens");
    public static final SemanticTag LOCATION_OUTDOOR_PATIO = new SemanticTagImpl( //
            "Location_Outdoor_Patio", //
            "Patio", //
            "", //
            "Patios");
    public static final SemanticTag LOCATION_OUTDOOR_PORCH = new SemanticTagImpl( //
            "Location_Outdoor_Porch", //
            "Porch", //
            "", //
            "Porches");
    public static final SemanticTag LOCATION_OUTDOOR_TERRACE = new SemanticTagImpl( //
            "Location_Outdoor_Terrace", //
            "Terrace", //
            "", //
            "Terraces, Deck, Decks");
    public static final SemanticTag PROPERTY_TEMPERATURE = new SemanticTagImpl( //
            "Property_Temperature", //
            "Temperature", //
            "", //
            "Temperatures");
    public static final SemanticTag PROPERTY_LIGHT = new SemanticTagImpl( //
            "Property_Light", //
            "Light", //
            "", //
            "Lights, Lighting");
    public static final SemanticTag PROPERTY_COLORTEMPERATURE = new SemanticTagImpl( //
            "Property_ColorTemperature", //
            "Color Temperature", //
            "", //
            "");
    public static final SemanticTag PROPERTY_HUMIDITY = new SemanticTagImpl( //
            "Property_Humidity", //
            "Humidity", //
            "", //
            "Moisture");
    public static final SemanticTag PROPERTY_PRESENCE = new SemanticTagImpl( //
            "Property_Presence", //
            "Presence", //
            "", //
            "");
    public static final SemanticTag PROPERTY_PRESSURE = new SemanticTagImpl( //
            "Property_Pressure", //
            "Pressure", //
            "", //
            "");
    public static final SemanticTag PROPERTY_SMOKE = new SemanticTagImpl( //
            "Property_Smoke", //
            "Smoke", //
            "", //
            "");
    public static final SemanticTag PROPERTY_NOISE = new SemanticTagImpl( //
            "Property_Noise", //
            "Noise", //
            "", //
            "");
    public static final SemanticTag PROPERTY_RAIN = new SemanticTagImpl( //
            "Property_Rain", //
            "Rain", //
            "", //
            "");
    public static final SemanticTag PROPERTY_WIND = new SemanticTagImpl( //
            "Property_Wind", //
            "Wind", //
            "", //
            "");
    public static final SemanticTag PROPERTY_WATER = new SemanticTagImpl( //
            "Property_Water", //
            "Water", //
            "", //
            "");
    public static final SemanticTag PROPERTY_CO2 = new SemanticTagImpl( //
            "Property_CO2", //
            "CO2", //
            "", //
            "Carbon Dioxide");
    public static final SemanticTag PROPERTY_CO = new SemanticTagImpl( //
            "Property_CO", //
            "CO", //
            "", //
            "Carbon Monoxide");
    public static final SemanticTag PROPERTY_ENERGY = new SemanticTagImpl( //
            "Property_Energy", //
            "Energy", //
            "", //
            "");
    public static final SemanticTag PROPERTY_POWER = new SemanticTagImpl( //
            "Property_Power", //
            "Power", //
            "", //
            "");
    public static final SemanticTag PROPERTY_VOLTAGE = new SemanticTagImpl( //
            "Property_Voltage", //
            "Voltage", //
            "", //
            "");
    public static final SemanticTag PROPERTY_CURRENT = new SemanticTagImpl( //
            "Property_Current", //
            "Current", //
            "", //
            "");
    public static final SemanticTag PROPERTY_FREQUENCY = new SemanticTagImpl( //
            "Property_Frequency", //
            "Frequency", //
            "", //
            "");
    public static final SemanticTag PROPERTY_GAS = new SemanticTagImpl( //
            "Property_Gas", //
            "Gas", //
            "", //
            "");
    public static final SemanticTag PROPERTY_SOUNDVOLUME = new SemanticTagImpl( //
            "Property_SoundVolume", //
            "Sound Volume", //
            "", //
            "");
    public static final SemanticTag PROPERTY_OIL = new SemanticTagImpl( //
            "Property_Oil", //
            "Oil", //
            "", //
            "");
    public static final SemanticTag PROPERTY_DURATION = new SemanticTagImpl( //
            "Property_Duration", //
            "Duration", //
            "", //
            "");
    public static final SemanticTag PROPERTY_LEVEL = new SemanticTagImpl( //
            "Property_Level", //
            "Level", //
            "", //
            "");
    public static final SemanticTag PROPERTY_OPENING = new SemanticTagImpl( //
            "Property_Opening", //
            "Opening", //
            "", //
            "");
    public static final SemanticTag PROPERTY_TIMESTAMP = new SemanticTagImpl( //
            "Property_Timestamp", //
            "Timestamp", //
            "", //
            "");
    public static final SemanticTag PROPERTY_ULTRAVIOLET = new SemanticTagImpl( //
            "Property_Ultraviolet", //
            "Ultraviolet", //
            "", //
            "UV");
    public static final SemanticTag PROPERTY_VIBRATION = new SemanticTagImpl( //
            "Property_Vibration", //
            "Vibration", //
            "", //
            "");
    public static final SemanticTag POINT_ALARM = new SemanticTagImpl( //
            "Point_Alarm", //
            "Alarm", //
            "", //
            "");
    public static final SemanticTag POINT_CONTROL = new SemanticTagImpl( //
            "Point_Control", //
            "Control", //
            "", //
            "");
    public static final SemanticTag POINT_CONTROL_SWITCH = new SemanticTagImpl( //
            "Point_Control_Switch", //
            "Switch", //
            "", //
            "");
    public static final SemanticTag POINT_MEASUREMENT = new SemanticTagImpl( //
            "Point_Measurement", //
            "Measurement", //
            "", //
            "");
    public static final SemanticTag POINT_SETPOINT = new SemanticTagImpl( //
            "Point_Setpoint", //
            "Setpoint", //
            "", //
            "");
    public static final SemanticTag POINT_STATUS = new SemanticTagImpl( //
            "Point_Status", //
            "Status", //
            "", //
            "");
    public static final SemanticTag POINT_STATUS_LOWBATTERY = new SemanticTagImpl( //
            "Point_Status_LowBattery", //
            "LowBattery", //
            "", //
            "");
    public static final SemanticTag POINT_STATUS_OPENLEVEL = new SemanticTagImpl( //
            "Point_Status_OpenLevel", //
            "OpenLevel", //
            "", //
            "");
    public static final SemanticTag POINT_STATUS_OPENSTATE = new SemanticTagImpl( //
            "Point_Status_OpenState", //
            "OpenState", //
            "", //
            "");
    public static final SemanticTag POINT_STATUS_TAMPERED = new SemanticTagImpl( //
            "Point_Status_Tampered", //
            "Tampered", //
            "", //
            "");
    public static final SemanticTag POINT_STATUS_TILT = new SemanticTagImpl( //
            "Point_Status_Tilt", //
            "Tilt", //
            "", //
            "");
    public static final SemanticTag EQUIPMENT_ALARMSYSTEM = new SemanticTagImpl( //
            "Equipment_AlarmSystem", //
            "Alarm System", //
            "", //
            "Alarm Systems");
    public static final SemanticTag EQUIPMENT_BATTERY = new SemanticTagImpl( //
            "Equipment_Battery", //
            "Battery", //
            "", //
            "Batteries");
    public static final SemanticTag EQUIPMENT_BLINDS = new SemanticTagImpl( //
            "Equipment_Blinds", //
            "Blinds", //
            "", //
            "Rollershutter, Rollershutters, Roller shutter, Roller shutters, Shutter, Shutters");
    public static final SemanticTag EQUIPMENT_BOILER = new SemanticTagImpl( //
            "Equipment_Boiler", //
            "Boiler", //
            "", //
            "Boilers");
    public static final SemanticTag EQUIPMENT_CAMERA = new SemanticTagImpl( //
            "Equipment_Camera", //
            "Camera", //
            "", //
            "Cameras");
    public static final SemanticTag EQUIPMENT_CAR = new SemanticTagImpl( //
            "Equipment_Car", //
            "Car", //
            "", //
            "Cars");
    public static final SemanticTag EQUIPMENT_CLEANINGROBOT = new SemanticTagImpl( //
            "Equipment_CleaningRobot", //
            "Cleaning Robot", //
            "", //
            "Cleaning Robots, Vacuum robot, Vacuum robots");
    public static final SemanticTag EQUIPMENT_DOOR = new SemanticTagImpl( //
            "Equipment_Door", //
            "Door", //
            "", //
            "Doors");
    public static final SemanticTag EQUIPMENT_DOOR_BACKDOOR = new SemanticTagImpl( //
            "Equipment_Door_BackDoor", //
            "Back Door", //
            "", //
            "Back Doors");
    public static final SemanticTag EQUIPMENT_DOOR_CELLARDOOR = new SemanticTagImpl( //
            "Equipment_Door_CellarDoor", //
            "Cellar Door", //
            "", //
            "Cellar Doors");
    public static final SemanticTag EQUIPMENT_DOOR_FRONTDOOR = new SemanticTagImpl( //
            "Equipment_Door_FrontDoor", //
            "Front Door", //
            "", //
            "Front Doors, Frontdoor, Frontdoors");
    public static final SemanticTag EQUIPMENT_DOOR_GARAGEDOOR = new SemanticTagImpl( //
            "Equipment_Door_GarageDoor", //
            "Garage Door", //
            "", //
            "Garage Doors");
    public static final SemanticTag EQUIPMENT_DOOR_GATE = new SemanticTagImpl( //
            "Equipment_Door_Gate", //
            "Gate", //
            "", //
            "Gates");
    public static final SemanticTag EQUIPMENT_DOOR_INNERDOOR = new SemanticTagImpl( //
            "Equipment_Door_InnerDoor", //
            "Inner Door", //
            "", //
            "Inner Doors");
    public static final SemanticTag EQUIPMENT_DOOR_SIDEDOOR = new SemanticTagImpl( //
            "Equipment_Door_SideDoor", //
            "Side Door", //
            "", //
            "Side Doors");
    public static final SemanticTag EQUIPMENT_DOORBELL = new SemanticTagImpl( //
            "Equipment_Doorbell", //
            "Doorbell", //
            "", //
            "Doorbells");
    public static final SemanticTag EQUIPMENT_FAN = new SemanticTagImpl( //
            "Equipment_Fan", //
            "Fan", //
            "", //
            "Fans");
    public static final SemanticTag EQUIPMENT_FAN_CEILINGFAN = new SemanticTagImpl( //
            "Equipment_Fan_CeilingFan", //
            "Ceiling Fan", //
            "", //
            "Ceiling Fans");
    public static final SemanticTag EQUIPMENT_FAN_KITCHENHOOD = new SemanticTagImpl( //
            "Equipment_Fan_KitchenHood", //
            "Kitchen Hood", //
            "", //
            "Kitchen Hoods");
    public static final SemanticTag EQUIPMENT_HVAC = new SemanticTagImpl( //
            "Equipment_HVAC", //
            "HVAC", //
            "", //
            "Heating, Ventilation, Air Conditioning, A/C, A/Cs, AC");
    public static final SemanticTag EQUIPMENT_INVERTER = new SemanticTagImpl( //
            "Equipment_Inverter", //
            "Inverter", //
            "", //
            "Inverters");
    public static final SemanticTag EQUIPMENT_LAWNMOWER = new SemanticTagImpl( //
            "Equipment_LawnMower", //
            "Lawn Mower", //
            "", //
            "Lawn Mowers");
    public static final SemanticTag EQUIPMENT_LIGHTBULB = new SemanticTagImpl( //
            "Equipment_Lightbulb", //
            "Lightbulb", //
            "", //
            "Lightbulbs, Bulb, Bulbs, Lamp, Lamps, Lights, Lighting");
    public static final SemanticTag EQUIPMENT_LIGHTBULB_LIGHTSTRIPE = new SemanticTagImpl( //
            "Equipment_Lightbulb_LightStripe", //
            "Light Stripe", //
            "", //
            "Light Stripes");
    public static final SemanticTag EQUIPMENT_LOCK = new SemanticTagImpl( //
            "Equipment_Lock", //
            "Lock", //
            "", //
            "Locks");
    public static final SemanticTag EQUIPMENT_NETWORKAPPLIANCE = new SemanticTagImpl( //
            "Equipment_NetworkAppliance", //
            "Network Appliance", //
            "", //
            "Network Appliances");
    public static final SemanticTag EQUIPMENT_POWEROUTLET = new SemanticTagImpl( //
            "Equipment_PowerOutlet", //
            "Power Outlet", //
            "", //
            "Power Outlets, Outlet, Outlets");
    public static final SemanticTag EQUIPMENT_PROJECTOR = new SemanticTagImpl( //
            "Equipment_Projector", //
            "Projector", //
            "", //
            "Projectors, Beamer, Beamers");
    public static final SemanticTag EQUIPMENT_PUMP = new SemanticTagImpl( //
            "Equipment_Pump", //
            "Pump", //
            "", //
            "Pumps");
    public static final SemanticTag EQUIPMENT_RADIATORCONTROL = new SemanticTagImpl( //
            "Equipment_RadiatorControl", //
            "Radiator Control", //
            "", //
            "Radiator Controls, Radiator, Radiators");
    public static final SemanticTag EQUIPMENT_RECEIVER = new SemanticTagImpl( //
            "Equipment_Receiver", //
            "Receiver", //
            "", //
            "Receivers, Audio Receiver, Audio Receivers, AV Receiver, AV Receivers");
    public static final SemanticTag EQUIPMENT_REMOTECONTROL = new SemanticTagImpl( //
            "Equipment_RemoteControl", //
            "Remote Control", //
            "", //
            "Remote Controls");
    public static final SemanticTag EQUIPMENT_SCREEN = new SemanticTagImpl( //
            "Equipment_Screen", //
            "Screen", //
            "", //
            "Screens");
    public static final SemanticTag EQUIPMENT_SCREEN_TELEVISION = new SemanticTagImpl( //
            "Equipment_Screen_Television", //
            "Television", //
            "", //
            "Televisions, TV, TVs");
    public static final SemanticTag EQUIPMENT_SENSOR = new SemanticTagImpl( //
            "Equipment_Sensor", //
            "Sensor", //
            "", //
            "Sensors");
    public static final SemanticTag EQUIPMENT_SENSOR_MOTIONDETECTOR = new SemanticTagImpl( //
            "Equipment_Sensor_MotionDetector", //
            "Motion Detector", //
            "", //
            "Motion Detectors, Motion sensor, Motion sensors");
    public static final SemanticTag EQUIPMENT_SENSOR_SMOKEDETECTOR = new SemanticTagImpl( //
            "Equipment_Sensor_SmokeDetector", //
            "Smoke Detector", //
            "", //
            "Smoke Detectors");
    public static final SemanticTag EQUIPMENT_SIREN = new SemanticTagImpl( //
            "Equipment_Siren", //
            "Siren", //
            "", //
            "Sirens");
    public static final SemanticTag EQUIPMENT_SMARTPHONE = new SemanticTagImpl( //
            "Equipment_Smartphone", //
            "Smartphone", //
            "", //
            "Smartphones, Phone, Phones");
    public static final SemanticTag EQUIPMENT_SPEAKER = new SemanticTagImpl( //
            "Equipment_Speaker", //
            "Speaker", //
            "", //
            "Speakers");
    public static final SemanticTag EQUIPMENT_VALVE = new SemanticTagImpl( //
            "Equipment_Valve", //
            "Valve", //
            "", //
            "Valves");
    public static final SemanticTag EQUIPMENT_VOICEASSISTANT = new SemanticTagImpl( //
            "Equipment_VoiceAssistant", //
            "Voice Assistant", //
            "", //
            "Voice Assistants");
    public static final SemanticTag EQUIPMENT_WALLSWITCH = new SemanticTagImpl( //
            "Equipment_WallSwitch", //
            "Wall Switch", //
            "", //
            "Wall Switches");
    public static final SemanticTag EQUIPMENT_WEBSERVICE = new SemanticTagImpl( //
            "Equipment_WebService", //
            "Web Service", //
            "", //
            "Web Services");
    public static final SemanticTag EQUIPMENT_WEBSERVICE_WEATHERSERVICE = new SemanticTagImpl( //
            "Equipment_WebService_WeatherService", //
            "Weather Service", //
            "", //
            "Weather Services");
    public static final SemanticTag EQUIPMENT_WHITEGOOD = new SemanticTagImpl( //
            "Equipment_WhiteGood", //
            "White Good", //
            "", //
            "White Goods");
    public static final SemanticTag EQUIPMENT_WHITEGOOD_DISHWASHER = new SemanticTagImpl( //
            "Equipment_WhiteGood_Dishwasher", //
            "Dishwasher", //
            "", //
            "Dishwashers");
    public static final SemanticTag EQUIPMENT_WHITEGOOD_DRYER = new SemanticTagImpl( //
            "Equipment_WhiteGood_Dryer", //
            "Dryer", //
            "", //
            "Dryers");
    public static final SemanticTag EQUIPMENT_WHITEGOOD_FREEZER = new SemanticTagImpl( //
            "Equipment_WhiteGood_Freezer", //
            "Freezer", //
            "", //
            "Freezers");
    public static final SemanticTag EQUIPMENT_WHITEGOOD_OVEN = new SemanticTagImpl( //
            "Equipment_WhiteGood_Oven", //
            "Oven", //
            "", //
            "Ovens");
    public static final SemanticTag EQUIPMENT_WHITEGOOD_REFRIGERATOR = new SemanticTagImpl( //
            "Equipment_WhiteGood_Refrigerator", //
            "Refrigerator", //
            "", //
            "Refrigerators");
    public static final SemanticTag EQUIPMENT_WHITEGOOD_WASHINGMACHINE = new SemanticTagImpl( //
            "Equipment_WhiteGood_WashingMachine", //
            "Washing Machine", //
            "", //
            "Washing Machines");
    public static final SemanticTag EQUIPMENT_WINDOW = new SemanticTagImpl( //
            "Equipment_Window", //
            "Window", //
            "", //
            "Windows");
}
