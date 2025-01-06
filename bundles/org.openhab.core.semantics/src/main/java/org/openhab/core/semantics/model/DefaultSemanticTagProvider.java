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
import org.openhab.core.semantics.SemanticTagImpl;
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
        defaultTags.add(new SemanticTagImpl("Equipment", "", "", ""));
        defaultTags.add(new SemanticTagImpl("Location", "", "", ""));
        defaultTags.add(new SemanticTagImpl("Point", "", "", ""));
        defaultTags.add(new SemanticTagImpl("Property", "", "", ""));
        defaultTags.add(new SemanticTagImpl("Location_Indoor", //
                "Indoor", "Anything that is inside a closed building", ""));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Apartment", //
                "Apartment", "", "Apartments"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Building", //
                "Building", "", "Buildings"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Building_Garage", //
                "Garage", "", "Garages"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Building_House", //
                "House", "", "Houses"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Building_Shed", //
                "Shed", "", "Sheds"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Building_SummerHouse", //
                "Summer House", "", "Summer Houses, Second Home, Second Homes"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Floor", //
                "Floor", "", "Floors"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Floor_GroundFloor", //
                "Ground Floor", "", "Ground Floors, Downstairs"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Floor_FirstFloor", //
                "First Floor", "", "First Floors, Upstairs"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Floor_SecondFloor", //
                "Second Floor", "", "Second Floors"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Floor_ThirdFloor", //
                "Third Floor", "", "Third Floors"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Floor_Attic", //
                "Attic", "", "Attics"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Floor_Basement", //
                "Basement", "", "Basements"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Corridor", //
                "Corridor", "", "Corridors, Hallway, Hallways"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room", //
                "Room", "", "Rooms"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_Bathroom", //
                "Bathroom", "", "Bathrooms, Bath, Baths, Powder Room, Powder Rooms"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_Bedroom", //
                "Bedroom", "", "Bedrooms"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_BoilerRoom", //
                "Boiler Room", "", "Boiler Rooms"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_Cellar", //
                "Cellar", "", "Cellars"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_DiningRoom", //
                "Dining Room", "", "Dining Rooms"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_Entry", //
                "Entry", "", "Entries, Foyer, Foyers"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_FamilyRoom", //
                "Family Room", "", "Family Rooms"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_GuestRoom", //
                "Guest Room", "", "Guest Rooms"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_Kitchen", //
                "Kitchen", "", "Kitchens"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_LaundryRoom", //
                "Laundry Room", "", "Laundry Rooms"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_LivingRoom", //
                "Living Room", "", "Living Rooms"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_Office", //
                "Office", "", "Offices"));
        defaultTags.add(new SemanticTagImpl("Location_Indoor_Room_Veranda", //
                "Veranda", "", "Verandas"));
        defaultTags.add(new SemanticTagImpl("Location_Outdoor", //
                "Outdoor", "", ""));
        defaultTags.add(new SemanticTagImpl("Location_Outdoor_Carport", //
                "Carport", "", "Carports"));
        defaultTags.add(new SemanticTagImpl("Location_Outdoor_Driveway", //
                "Driveway", "", "Driveways"));
        defaultTags.add(new SemanticTagImpl("Location_Outdoor_Garden", //
                "Garden", "", "Gardens"));
        defaultTags.add(new SemanticTagImpl("Location_Outdoor_Patio", //
                "Patio", "", "Patios"));
        defaultTags.add(new SemanticTagImpl("Location_Outdoor_Porch", //
                "Porch", "", "Porches"));
        defaultTags.add(new SemanticTagImpl("Location_Outdoor_Terrace", //
                "Terrace", "", "Terraces, Deck, Decks"));
        defaultTags.add(new SemanticTagImpl("Property_Temperature", //
                "Temperature", "", "Temperatures"));
        defaultTags.add(new SemanticTagImpl("Property_Light", //
                "Light", "", "Lights, Lighting"));
        defaultTags.add(new SemanticTagImpl("Property_ColorTemperature", //
                "Color Temperature", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Humidity", //
                "Humidity", "", "Moisture"));
        defaultTags.add(new SemanticTagImpl("Property_Presence", //
                "Presence", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Pressure", //
                "Pressure", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Smoke", //
                "Smoke", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Noise", //
                "Noise", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Rain", //
                "Rain", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Wind", //
                "Wind", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Water", //
                "Water", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_CO2", //
                "CO2", "", "Carbon Dioxide"));
        defaultTags.add(new SemanticTagImpl("Property_CO", //
                "CO", "", "Carbon Monoxide"));
        defaultTags.add(new SemanticTagImpl("Property_Energy", //
                "Energy", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Power", //
                "Power", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Voltage", //
                "Voltage", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Current", //
                "Current", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Frequency", //
                "Frequency", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Gas", //
                "Gas", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_SoundVolume", //
                "Sound Volume", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Oil", //
                "Oil", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Duration", //
                "Duration", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Level", //
                "Level", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Opening", //
                "Opening", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Timestamp", //
                "Timestamp", "", ""));
        defaultTags.add(new SemanticTagImpl("Property_Ultraviolet", //
                "Ultraviolet", "", "UV"));
        defaultTags.add(new SemanticTagImpl("Property_Vibration", //
                "Vibration", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Alarm", //
                "Alarm", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Control", //
                "Control", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Control_Switch", //
                "Switch", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Measurement", //
                "Measurement", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Setpoint", //
                "Setpoint", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Status", //
                "Status", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Status_LowBattery", //
                "LowBattery", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Status_OpenLevel", //
                "OpenLevel", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Status_OpenState", //
                "OpenState", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Status_Tampered", //
                "Tampered", "", ""));
        defaultTags.add(new SemanticTagImpl("Point_Status_Tilt", //
                "Tilt", "", ""));
        defaultTags.add(new SemanticTagImpl("Equipment_AlarmSystem", //
                "Alarm System", "", "Alarm Systems"));
        defaultTags.add(new SemanticTagImpl("Equipment_Battery", //
                "Battery", "", "Batteries"));
        defaultTags.add(new SemanticTagImpl("Equipment_Blinds", //
                "Blinds", "", "Rollershutter, Rollershutters, Roller shutter, Roller shutters, Shutter, Shutters"));
        defaultTags.add(new SemanticTagImpl("Equipment_Boiler", //
                "Boiler", "", "Boilers"));
        defaultTags.add(new SemanticTagImpl("Equipment_Camera", //
                "Camera", "", "Cameras"));
        defaultTags.add(new SemanticTagImpl("Equipment_Car", //
                "Car", "", "Cars"));
        defaultTags.add(new SemanticTagImpl("Equipment_CleaningRobot", //
                "Cleaning Robot", "", "Cleaning Robots, Vacuum robot, Vacuum robots"));
        defaultTags.add(new SemanticTagImpl("Equipment_Door", //
                "Door", "", "Doors"));
        defaultTags.add(new SemanticTagImpl("Equipment_Door_BackDoor", //
                "Back Door", "", "Back Doors"));
        defaultTags.add(new SemanticTagImpl("Equipment_Door_CellarDoor", //
                "Cellar Door", "", "Cellar Doors"));
        defaultTags.add(new SemanticTagImpl("Equipment_Door_FrontDoor", //
                "Front Door", "", "Front Doors, Frontdoor, Frontdoors"));
        defaultTags.add(new SemanticTagImpl("Equipment_Door_GarageDoor", //
                "Garage Door", "", "Garage Doors"));
        defaultTags.add(new SemanticTagImpl("Equipment_Door_Gate", //
                "Gate", "", "Gates"));
        defaultTags.add(new SemanticTagImpl("Equipment_Door_InnerDoor", //
                "Inner Door", "", "Inner Doors"));
        defaultTags.add(new SemanticTagImpl("Equipment_Door_SideDoor", //
                "Side Door", "", "Side Doors"));
        defaultTags.add(new SemanticTagImpl("Equipment_Doorbell", //
                "Doorbell", "", "Doorbells"));
        defaultTags.add(new SemanticTagImpl("Equipment_Fan", //
                "Fan", "", "Fans"));
        defaultTags.add(new SemanticTagImpl("Equipment_Fan_CeilingFan", //
                "Ceiling Fan", "", "Ceiling Fans"));
        defaultTags.add(new SemanticTagImpl("Equipment_Fan_KitchenHood", //
                "Kitchen Hood", "", "Kitchen Hoods"));
        defaultTags.add(new SemanticTagImpl("Equipment_HVAC", //
                "HVAC", "", "Heating, Ventilation, Air Conditioning, A/C, A/Cs, AC"));
        defaultTags.add(new SemanticTagImpl("Equipment_Inverter", //
                "Inverter", "", "Inverters"));
        defaultTags.add(new SemanticTagImpl("Equipment_LawnMower", //
                "Lawn Mower", "", "Lawn Mowers"));
        defaultTags.add(new SemanticTagImpl("Equipment_Lightbulb", //
                "Lightbulb", "", "Lightbulbs, Bulb, Bulbs, Lamp, Lamps, Lights, Lighting"));
        defaultTags.add(new SemanticTagImpl("Equipment_Lightbulb_LightStripe", //
                "Light Stripe", "", "Light Stripes"));
        defaultTags.add(new SemanticTagImpl("Equipment_Lock", //
                "Lock", "", "Locks"));
        defaultTags.add(new SemanticTagImpl("Equipment_NetworkAppliance", //
                "Network Appliance", "", "Network Appliances"));
        defaultTags.add(new SemanticTagImpl("Equipment_PowerOutlet", //
                "Power Outlet", "", "Power Outlets, Outlet, Outlets"));
        defaultTags.add(new SemanticTagImpl("Equipment_Projector", //
                "Projector", "", "Projectors, Beamer, Beamers"));
        defaultTags.add(new SemanticTagImpl("Equipment_Pump", //
                "Pump", "", "Pumps"));
        defaultTags.add(new SemanticTagImpl("Equipment_RadiatorControl", //
                "Radiator Control", "", "Radiator Controls, Radiator, Radiators"));
        defaultTags.add(new SemanticTagImpl("Equipment_Receiver", //
                "Receiver", "", "Receivers, Audio Receiver, Audio Receivers, AV Receiver, AV Receivers"));
        defaultTags.add(new SemanticTagImpl("Equipment_RemoteControl", //
                "Remote Control", "", "Remote Controls"));
        defaultTags.add(new SemanticTagImpl("Equipment_Screen", //
                "Screen", "", "Screens"));
        defaultTags.add(new SemanticTagImpl("Equipment_Screen_Television", //
                "Television", "", "Televisions, TV, TVs"));
        defaultTags.add(new SemanticTagImpl("Equipment_Sensor", //
                "Sensor", "", "Sensors"));
        defaultTags.add(new SemanticTagImpl("Equipment_Sensor_MotionDetector", //
                "Motion Detector", "", "Motion Detectors, Motion sensor, Motion sensors"));
        defaultTags.add(new SemanticTagImpl("Equipment_Sensor_SmokeDetector", //
                "Smoke Detector", "", "Smoke Detectors"));
        defaultTags.add(new SemanticTagImpl("Equipment_Siren", //
                "Siren", "", "Sirens"));
        defaultTags.add(new SemanticTagImpl("Equipment_Smartphone", //
                "Smartphone", "", "Smartphones, Phone, Phones"));
        defaultTags.add(new SemanticTagImpl("Equipment_Speaker", //
                "Speaker", "", "Speakers"));
        defaultTags.add(new SemanticTagImpl("Equipment_Valve", //
                "Valve", "", "Valves"));
        defaultTags.add(new SemanticTagImpl("Equipment_VoiceAssistant", //
                "Voice Assistant", "", "Voice Assistants"));
        defaultTags.add(new SemanticTagImpl("Equipment_WallSwitch", //
                "Wall Switch", "", "Wall Switches"));
        defaultTags.add(new SemanticTagImpl("Equipment_WebService", //
                "Web Service", "", "Web Services"));
        defaultTags.add(new SemanticTagImpl("Equipment_WebService_WeatherService", //
                "Weather Service", "", "Weather Services"));
        defaultTags.add(new SemanticTagImpl("Equipment_WhiteGood", //
                "White Good", "", "White Goods"));
        defaultTags.add(new SemanticTagImpl("Equipment_WhiteGood_Dishwasher", //
                "Dishwasher", "", "Dishwashers"));
        defaultTags.add(new SemanticTagImpl("Equipment_WhiteGood_Dryer", //
                "Dryer", "", "Dryers"));
        defaultTags.add(new SemanticTagImpl("Equipment_WhiteGood_Freezer", //
                "Freezer", "", "Freezers"));
        defaultTags.add(new SemanticTagImpl("Equipment_WhiteGood_Oven", //
                "Oven", "", "Ovens"));
        defaultTags.add(new SemanticTagImpl("Equipment_WhiteGood_Refrigerator", //
                "Refrigerator", "", "Refrigerators"));
        defaultTags.add(new SemanticTagImpl("Equipment_WhiteGood_WashingMachine", //
                "Washing Machine", "", "Washing Machines"));
        defaultTags.add(new SemanticTagImpl("Equipment_Window", //
                "Window", "", "Windows"));
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
