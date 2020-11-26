/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.semantics.model.equipment;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.model.Equipment;

/**
 * This class provides a stream of all defined equipments.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
public class Equipments {

    static final Set<Class<? extends Equipment>> EQUIPMENTS = new HashSet<>();

    static {
        EQUIPMENTS.add(Equipment.class);
        EQUIPMENTS.add(AlarmSystem.class);
        EQUIPMENTS.add(BackDoor.class);
        EQUIPMENTS.add(Battery.class);
        EQUIPMENTS.add(Blinds.class);
        EQUIPMENTS.add(Boiler.class);
        EQUIPMENTS.add(Camera.class);
        EQUIPMENTS.add(Car.class);
        EQUIPMENTS.add(CeilingFan.class);
        EQUIPMENTS.add(CellarDoor.class);
        EQUIPMENTS.add(CleaningRobot.class);
        EQUIPMENTS.add(Dishwasher.class);
        EQUIPMENTS.add(Door.class);
        EQUIPMENTS.add(Doorbell.class);
        EQUIPMENTS.add(Dryer.class);
        EQUIPMENTS.add(Fan.class);
        EQUIPMENTS.add(Freezer.class);
        EQUIPMENTS.add(FrontDoor.class);
        EQUIPMENTS.add(GarageDoor.class);
        EQUIPMENTS.add(Gate.class);
        EQUIPMENTS.add(HVAC.class);
        EQUIPMENTS.add(InnerDoor.class);
        EQUIPMENTS.add(Inverter.class);
        EQUIPMENTS.add(KitchenHood.class);
        EQUIPMENTS.add(LawnMower.class);
        EQUIPMENTS.add(LightStripe.class);
        EQUIPMENTS.add(Lightbulb.class);
        EQUIPMENTS.add(Lock.class);
        EQUIPMENTS.add(MotionDetector.class);
        EQUIPMENTS.add(NetworkAppliance.class);
        EQUIPMENTS.add(Oven.class);
        EQUIPMENTS.add(PowerOutlet.class);
        EQUIPMENTS.add(Projector.class);
        EQUIPMENTS.add(Pump.class);
        EQUIPMENTS.add(RadiatorControl.class);
        EQUIPMENTS.add(Receiver.class);
        EQUIPMENTS.add(Refrigerator.class);
        EQUIPMENTS.add(RemoteControl.class);
        EQUIPMENTS.add(Screen.class);
        EQUIPMENTS.add(Sensor.class);
        EQUIPMENTS.add(SideDoor.class);
        EQUIPMENTS.add(Siren.class);
        EQUIPMENTS.add(Smartphone.class);
        EQUIPMENTS.add(SmokeDetector.class);
        EQUIPMENTS.add(Speaker.class);
        EQUIPMENTS.add(Television.class);
        EQUIPMENTS.add(Valve.class);
        EQUIPMENTS.add(VoiceAssistant.class);
        EQUIPMENTS.add(WallSwitch.class);
        EQUIPMENTS.add(WashingMachine.class);
        EQUIPMENTS.add(WeatherService.class);
        EQUIPMENTS.add(WebService.class);
        EQUIPMENTS.add(WhiteGood.class);
        EQUIPMENTS.add(Window.class);
    }

    public static Stream<Class<? extends Equipment>> stream() {
        return EQUIPMENTS.stream();
    }
}
