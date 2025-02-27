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
    public static final SemanticTag SUMMERHOUSE = new SemanticTagImpl( //
            "Location_Indoor_Building_SummerHouse", //
            "Summer House", //
            "", //
            "Summer Houses, Second Home, Second Homes");
    public static final SemanticTag FLOOR = new SemanticTagImpl( //
            "Location_Indoor_Floor", //
            "Floor", //
            "", //
            "Floors");
    public static final SemanticTag GROUNDFLOOR = new SemanticTagImpl( //
            "Location_Indoor_Floor_GroundFloor", //
            "Ground Floor", //
            "", //
            "Ground Floors, Downstairs");
    public static final SemanticTag FIRSTFLOOR = new SemanticTagImpl( //
            "Location_Indoor_Floor_FirstFloor", //
            "First Floor", //
            "", //
            "First Floors, Upstairs");
    public static final SemanticTag SECONDFLOOR = new SemanticTagImpl( //
            "Location_Indoor_Floor_SecondFloor", //
            "Second Floor", //
            "", //
            "Second Floors");
    public static final SemanticTag THIRDFLOOR = new SemanticTagImpl( //
            "Location_Indoor_Floor_ThirdFloor", //
            "Third Floor", //
            "", //
            "Third Floors");
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
    public static final SemanticTag CORRIDOR = new SemanticTagImpl( //
            "Location_Indoor_Corridor", //
            "Corridor", //
            "", //
            "Corridors, Hallway, Hallways");
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
    public static final SemanticTag BOILERROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_BoilerRoom", //
            "Boiler Room", //
            "", //
            "Boiler Rooms");
    public static final SemanticTag CELLAR = new SemanticTagImpl( //
            "Location_Indoor_Room_Cellar", //
            "Cellar", //
            "", //
            "Cellars");
    public static final SemanticTag DININGROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_DiningRoom", //
            "Dining Room", //
            "", //
            "Dining Rooms");
    public static final SemanticTag ENTRY = new SemanticTagImpl( //
            "Location_Indoor_Room_Entry", //
            "Entry", //
            "", //
            "Entries, Foyer, Foyers");
    public static final SemanticTag FAMILYROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_FamilyRoom", //
            "Family Room", //
            "", //
            "Family Rooms");
    public static final SemanticTag GUESTROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_GuestRoom", //
            "Guest Room", //
            "", //
            "Guest Rooms");
    public static final SemanticTag KITCHEN = new SemanticTagImpl( //
            "Location_Indoor_Room_Kitchen", //
            "Kitchen", //
            "", //
            "Kitchens");
    public static final SemanticTag LAUNDRYROOM = new SemanticTagImpl( //
            "Location_Indoor_Room_LaundryRoom", //
            "Laundry Room", //
            "", //
            "Laundry Rooms");
    public static final SemanticTag LIVINGROOM = new SemanticTagImpl( //
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
    public static final SemanticTag TEMPERATURE = new SemanticTagImpl( //
            "Property_Temperature", //
            "Temperature", //
            "", //
            "Temperatures");
    public static final SemanticTag LIGHT = new SemanticTagImpl( //
            "Property_Light", //
            "Light", //
            "", //
            "Lights, Lighting");
    public static final SemanticTag COLORTEMPERATURE = new SemanticTagImpl( //
            "Property_ColorTemperature", //
            "Color Temperature", //
            "", //
            "");
    public static final SemanticTag HUMIDITY = new SemanticTagImpl( //
            "Property_Humidity", //
            "Humidity", //
            "", //
            "Moisture");
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
    public static final SemanticTag SMOKE = new SemanticTagImpl( //
            "Property_Smoke", //
            "Smoke", //
            "", //
            "");
    public static final SemanticTag NOISE = new SemanticTagImpl( //
            "Property_Noise", //
            "Noise", //
            "", //
            "");
    public static final SemanticTag RAIN = new SemanticTagImpl( //
            "Property_Rain", //
            "Rain", //
            "", //
            "");
    public static final SemanticTag WIND = new SemanticTagImpl( //
            "Property_Wind", //
            "Wind", //
            "", //
            "");
    public static final SemanticTag WATER = new SemanticTagImpl( //
            "Property_Water", //
            "Water", //
            "", //
            "");
    public static final SemanticTag CO2 = new SemanticTagImpl( //
            "Property_CO2", //
            "CO2", //
            "", //
            "Carbon Dioxide");
    public static final SemanticTag CO = new SemanticTagImpl( //
            "Property_CO", //
            "CO", //
            "", //
            "Carbon Monoxide");
    public static final SemanticTag ENERGY = new SemanticTagImpl( //
            "Property_Energy", //
            "Energy", //
            "", //
            "");
    public static final SemanticTag POWER = new SemanticTagImpl( //
            "Property_Power", //
            "Power", //
            "", //
            "");
    public static final SemanticTag VOLTAGE = new SemanticTagImpl( //
            "Property_Voltage", //
            "Voltage", //
            "", //
            "");
    public static final SemanticTag CURRENT = new SemanticTagImpl( //
            "Property_Current", //
            "Current", //
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
    public static final SemanticTag SOUNDVOLUME = new SemanticTagImpl( //
            "Property_SoundVolume", //
            "Sound Volume", //
            "", //
            "");
    public static final SemanticTag OIL = new SemanticTagImpl( //
            "Property_Oil", //
            "Oil", //
            "", //
            "");
    public static final SemanticTag DURATION = new SemanticTagImpl( //
            "Property_Duration", //
            "Duration", //
            "", //
            "");
    public static final SemanticTag LEVEL = new SemanticTagImpl( //
            "Property_Level", //
            "Level", //
            "", //
            "");
    public static final SemanticTag OPENING = new SemanticTagImpl( //
            "Property_Opening", //
            "Opening", //
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
    public static final SemanticTag VIBRATION = new SemanticTagImpl( //
            "Property_Vibration", //
            "Vibration", //
            "", //
            "");
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
    public static final SemanticTag LOWBATTERY = new SemanticTagImpl( //
            "Point_Status_LowBattery", //
            "LowBattery", //
            "", //
            "");
    public static final SemanticTag OPENLEVEL = new SemanticTagImpl( //
            "Point_Status_OpenLevel", //
            "OpenLevel", //
            "", //
            "");
    public static final SemanticTag OPENSTATE = new SemanticTagImpl( //
            "Point_Status_OpenState", //
            "OpenState", //
            "", //
            "");
    public static final SemanticTag TAMPERED = new SemanticTagImpl( //
            "Point_Status_Tampered", //
            "Tampered", //
            "", //
            "");
    public static final SemanticTag TILT = new SemanticTagImpl( //
            "Point_Status_Tilt", //
            "Tilt", //
            "", //
            "");
    public static final SemanticTag ALARMSYSTEM = new SemanticTagImpl( //
            "Equipment_AlarmSystem", //
            "Alarm System", //
            "", //
            "Alarm Systems");
    public static final SemanticTag BATTERY = new SemanticTagImpl( //
            "Equipment_Battery", //
            "Battery", //
            "", //
            "Batteries");
    public static final SemanticTag BLINDS = new SemanticTagImpl( //
            "Equipment_Blinds", //
            "Blinds", //
            "", //
            "Rollershutter, Rollershutters, Roller shutter, Roller shutters, Shutter, Shutters");
    public static final SemanticTag BOILER = new SemanticTagImpl( //
            "Equipment_Boiler", //
            "Boiler", //
            "", //
            "Boilers");
    public static final SemanticTag CAMERA = new SemanticTagImpl( //
            "Equipment_Camera", //
            "Camera", //
            "", //
            "Cameras");
    public static final SemanticTag CAR = new SemanticTagImpl( //
            "Equipment_Car", //
            "Car", //
            "", //
            "Cars");
    public static final SemanticTag CLEANINGROBOT = new SemanticTagImpl( //
            "Equipment_CleaningRobot", //
            "Cleaning Robot", //
            "", //
            "Cleaning Robots, Vacuum robot, Vacuum robots");
    public static final SemanticTag DOOR = new SemanticTagImpl( //
            "Equipment_Door", //
            "Door", //
            "", //
            "Doors");
    public static final SemanticTag BACKDOOR = new SemanticTagImpl( //
            "Equipment_Door_BackDoor", //
            "Back Door", //
            "", //
            "Back Doors");
    public static final SemanticTag CELLARDOOR = new SemanticTagImpl( //
            "Equipment_Door_CellarDoor", //
            "Cellar Door", //
            "", //
            "Cellar Doors");
    public static final SemanticTag FRONTDOOR = new SemanticTagImpl( //
            "Equipment_Door_FrontDoor", //
            "Front Door", //
            "", //
            "Front Doors, Frontdoor, Frontdoors");
    public static final SemanticTag GARAGEDOOR = new SemanticTagImpl( //
            "Equipment_Door_GarageDoor", //
            "Garage Door", //
            "", //
            "Garage Doors");
    public static final SemanticTag GATE = new SemanticTagImpl( //
            "Equipment_Door_Gate", //
            "Gate", //
            "", //
            "Gates");
    public static final SemanticTag INNERDOOR = new SemanticTagImpl( //
            "Equipment_Door_InnerDoor", //
            "Inner Door", //
            "", //
            "Inner Doors");
    public static final SemanticTag SIDEDOOR = new SemanticTagImpl( //
            "Equipment_Door_SideDoor", //
            "Side Door", //
            "", //
            "Side Doors");
    public static final SemanticTag DOORBELL = new SemanticTagImpl( //
            "Equipment_Doorbell", //
            "Doorbell", //
            "", //
            "Doorbells");
    public static final SemanticTag FAN = new SemanticTagImpl( //
            "Equipment_Fan", //
            "Fan", //
            "", //
            "Fans");
    public static final SemanticTag CEILINGFAN = new SemanticTagImpl( //
            "Equipment_Fan_CeilingFan", //
            "Ceiling Fan", //
            "", //
            "Ceiling Fans");
    public static final SemanticTag KITCHENHOOD = new SemanticTagImpl( //
            "Equipment_Fan_KitchenHood", //
            "Kitchen Hood", //
            "", //
            "Kitchen Hoods");
    public static final SemanticTag HVAC = new SemanticTagImpl( //
            "Equipment_HVAC", //
            "HVAC", //
            "", //
            "Heating, Ventilation, Air Conditioning, A/C, A/Cs, AC");
    public static final SemanticTag INVERTER = new SemanticTagImpl( //
            "Equipment_Inverter", //
            "Inverter", //
            "", //
            "Inverters");
    public static final SemanticTag LAWNMOWER = new SemanticTagImpl( //
            "Equipment_LawnMower", //
            "Lawn Mower", //
            "", //
            "Lawn Mowers");
    public static final SemanticTag LIGHTBULB = new SemanticTagImpl( //
            "Equipment_Lightbulb", //
            "Lightbulb", //
            "", //
            "Lightbulbs, Bulb, Bulbs, Lamp, Lamps, Lights, Lighting");
    public static final SemanticTag LIGHTSTRIPE = new SemanticTagImpl( //
            "Equipment_Lightbulb_LightStripe", //
            "Light Stripe", //
            "", //
            "Light Stripes");
    public static final SemanticTag LOCK = new SemanticTagImpl( //
            "Equipment_Lock", //
            "Lock", //
            "", //
            "Locks");
    public static final SemanticTag NETWORKAPPLIANCE = new SemanticTagImpl( //
            "Equipment_NetworkAppliance", //
            "Network Appliance", //
            "", //
            "Network Appliances");
    public static final SemanticTag POWEROUTLET = new SemanticTagImpl( //
            "Equipment_PowerOutlet", //
            "Power Outlet", //
            "", //
            "Power Outlets, Outlet, Outlets");
    public static final SemanticTag PROJECTOR = new SemanticTagImpl( //
            "Equipment_Projector", //
            "Projector", //
            "", //
            "Projectors, Beamer, Beamers");
    public static final SemanticTag PUMP = new SemanticTagImpl( //
            "Equipment_Pump", //
            "Pump", //
            "", //
            "Pumps");
    public static final SemanticTag RADIATORCONTROL = new SemanticTagImpl( //
            "Equipment_RadiatorControl", //
            "Radiator Control", //
            "", //
            "Radiator Controls, Radiator, Radiators");
    public static final SemanticTag RECEIVER = new SemanticTagImpl( //
            "Equipment_Receiver", //
            "Receiver", //
            "", //
            "Receivers, Audio Receiver, Audio Receivers, AV Receiver, AV Receivers");
    public static final SemanticTag REMOTECONTROL = new SemanticTagImpl( //
            "Equipment_RemoteControl", //
            "Remote Control", //
            "", //
            "Remote Controls");
    public static final SemanticTag SCREEN = new SemanticTagImpl( //
            "Equipment_Screen", //
            "Screen", //
            "", //
            "Screens");
    public static final SemanticTag TELEVISION = new SemanticTagImpl( //
            "Equipment_Screen_Television", //
            "Television", //
            "", //
            "Televisions, TV, TVs");
    public static final SemanticTag SENSOR = new SemanticTagImpl( //
            "Equipment_Sensor", //
            "Sensor", //
            "", //
            "Sensors");
    public static final SemanticTag MOTIONDETECTOR = new SemanticTagImpl( //
            "Equipment_Sensor_MotionDetector", //
            "Motion Detector", //
            "", //
            "Motion Detectors, Motion sensor, Motion sensors");
    public static final SemanticTag SMOKEDETECTOR = new SemanticTagImpl( //
            "Equipment_Sensor_SmokeDetector", //
            "Smoke Detector", //
            "", //
            "Smoke Detectors");
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
    public static final SemanticTag SPEAKER = new SemanticTagImpl( //
            "Equipment_Speaker", //
            "Speaker", //
            "", //
            "Speakers");
    public static final SemanticTag VALVE = new SemanticTagImpl( //
            "Equipment_Valve", //
            "Valve", //
            "", //
            "Valves");
    public static final SemanticTag VOICEASSISTANT = new SemanticTagImpl( //
            "Equipment_VoiceAssistant", //
            "Voice Assistant", //
            "", //
            "Voice Assistants");
    public static final SemanticTag WALLSWITCH = new SemanticTagImpl( //
            "Equipment_WallSwitch", //
            "Wall Switch", //
            "", //
            "Wall Switches");
    public static final SemanticTag WEBSERVICE = new SemanticTagImpl( //
            "Equipment_WebService", //
            "Web Service", //
            "", //
            "Web Services");
    public static final SemanticTag WEATHERSERVICE = new SemanticTagImpl( //
            "Equipment_WebService_WeatherService", //
            "Weather Service", //
            "", //
            "Weather Services");
    public static final SemanticTag WHITEGOOD = new SemanticTagImpl( //
            "Equipment_WhiteGood", //
            "White Good", //
            "", //
            "White Goods");
    public static final SemanticTag DISHWASHER = new SemanticTagImpl( //
            "Equipment_WhiteGood_Dishwasher", //
            "Dishwasher", //
            "", //
            "Dishwashers");
    public static final SemanticTag DRYER = new SemanticTagImpl( //
            "Equipment_WhiteGood_Dryer", //
            "Dryer", //
            "", //
            "Dryers");
    public static final SemanticTag FREEZER = new SemanticTagImpl( //
            "Equipment_WhiteGood_Freezer", //
            "Freezer", //
            "", //
            "Freezers");
    public static final SemanticTag OVEN = new SemanticTagImpl( //
            "Equipment_WhiteGood_Oven", //
            "Oven", //
            "", //
            "Ovens");
    public static final SemanticTag REFRIGERATOR = new SemanticTagImpl( //
            "Equipment_WhiteGood_Refrigerator", //
            "Refrigerator", //
            "", //
            "Refrigerators");
    public static final SemanticTag WASHINGMACHINE = new SemanticTagImpl( //
            "Equipment_WhiteGood_WashingMachine", //
            "Washing Machine", //
            "", //
            "Washing Machines");
    public static final SemanticTag WINDOW = new SemanticTagImpl( //
            "Equipment_Window", //
            "Window", //
            "", //
            "Windows");
}
