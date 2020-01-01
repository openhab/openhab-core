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
package org.openhab.core.thing;

import static org.junit.Assert.*;
import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.BINDING_ID;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.StateOption;

/**
 * Test cases for the {@link DefaultSystemChannelTypeProvider} class.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class SystemWideChannelTypesTest extends JavaOSGiTest {

    private static final ChannelTypeUID SIGNAL_STRENGTH_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID,
            "signal-strength");
    private static final ChannelTypeUID LOW_BATTERY_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "low-battery");
    private static final ChannelTypeUID BATTERY_LEVEL_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID,
            "battery-level");
    private static final ChannelTypeUID TRIGGER_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "trigger");
    private static final ChannelTypeUID RAWBUTTON_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "rawbutton");
    private static final ChannelTypeUID BUTTON_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "button");
    private static final ChannelTypeUID RAWROCKER_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "rawrocker");
    private static final ChannelTypeUID POWER_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "power");
    private static final ChannelTypeUID LOCATION_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "location");
    private static final ChannelTypeUID MOTION_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "motion");
    private static final ChannelTypeUID BRIGHTNESS_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "brightness");
    private static final ChannelTypeUID COLOR_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "color");
    private static final ChannelTypeUID COLOR_TEMPERATURE_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID,
            "color-temperature");
    private static final ChannelTypeUID VOLUME_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "volume");
    private static final ChannelTypeUID MUTE_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "mute");
    private static final ChannelTypeUID MEDIA_CONTROL_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID,
            "media-control");
    private static final ChannelTypeUID MEDIA_TITLE_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "media-title");
    private static final ChannelTypeUID MEDIA_ARTIST_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "media-artist");
    private static final ChannelTypeUID WIND_DIRECTION_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID,
            "wind-direction");
    private static final ChannelTypeUID WIND_SPEED_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "wind-speed");
    private static final ChannelTypeUID OUTDOOR_TEMPERATURE_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID,
            "outdoor-temperature");
    private static final ChannelTypeUID ATMOSPHERIC_HUMIDITY_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID,
            "atmospheric-humidity");
    private static final ChannelTypeUID BAROMETRIC_PRESSURE_CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID,
            "barometric-pressure");

    private ChannelTypeProvider systemChannelTypeProvider;

    @Before
    public void setUp() {
        ChannelTypeProvider provider = getService(ChannelTypeProvider.class, DefaultSystemChannelTypeProvider.class);
        assertTrue(provider instanceof DefaultSystemChannelTypeProvider);
        systemChannelTypeProvider = provider;
    }

    @Test
    public void systemChannelTypesShouldBeAvailable() {
        Collection<ChannelType> sytemChannelTypes = systemChannelTypeProvider.getChannelTypes(null);
        assertEquals(23, sytemChannelTypes.size());

        assertNotNull(systemChannelTypeProvider.getChannelType(SIGNAL_STRENGTH_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(LOW_BATTERY_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(BATTERY_LEVEL_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(TRIGGER_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(RAWBUTTON_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(BUTTON_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(RAWROCKER_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(POWER_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(LOCATION_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(MOTION_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(BRIGHTNESS_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(COLOR_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(COLOR_TEMPERATURE_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(VOLUME_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(MUTE_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(MEDIA_CONTROL_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(MEDIA_TITLE_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(MEDIA_ARTIST_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(WIND_DIRECTION_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(WIND_SPEED_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(OUTDOOR_TEMPERATURE_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(ATMOSPHERIC_HUMIDITY_CHANNEL_TYPE_UID, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(BAROMETRIC_PRESSURE_CHANNEL_TYPE_UID, null));
    }

    @Test
    public void systemChannelTypesShouldBeTranslatedProperly() {
        Collection<ChannelType> localizedChannelTypes = systemChannelTypeProvider.getChannelTypes(Locale.GERMAN);
        assertEquals(23, localizedChannelTypes.size());

        ChannelType signalStrengthChannelType = systemChannelTypeProvider
                .getChannelType(SIGNAL_STRENGTH_CHANNEL_TYPE_UID, Locale.GERMAN);
        assertNotNull(signalStrengthChannelType);
        assertEquals("Signalstärke", signalStrengthChannelType.getLabel());
        assertNull(signalStrengthChannelType.getDescription());

        List<StateOption> signalStrengthChannelTypeOptions = signalStrengthChannelType.getState().getOptions();
        assertEquals(5, signalStrengthChannelTypeOptions.size());

        StateOption noSignalOption = signalStrengthChannelTypeOptions.stream().filter(it -> "0".equals(it.getValue()))
                .findFirst().get();
        assertNotNull(noSignalOption);
        assertEquals("Kein Signal", noSignalOption.getLabel());
        StateOption weakOption = signalStrengthChannelTypeOptions.stream().filter(it -> "1".equals(it.getValue()))
                .findFirst().get();
        assertNotNull(weakOption);
        assertEquals("Schwach", weakOption.getLabel());
        StateOption averageOption = signalStrengthChannelTypeOptions.stream().filter(it -> "2".equals(it.getValue()))
                .findFirst().get();
        assertNotNull(averageOption);
        assertEquals("Durchschnittlich", averageOption.getLabel());
        StateOption goodOption = signalStrengthChannelTypeOptions.stream().filter(it -> "3".equals(it.getValue()))
                .findFirst().get();
        assertNotNull(goodOption);
        assertEquals("Gut", goodOption.getLabel());
        StateOption excellentOption = signalStrengthChannelTypeOptions.stream().filter(it -> "4".equals(it.getValue()))
                .findFirst().get();
        assertNotNull(excellentOption);
        assertEquals("Ausgezeichnet", excellentOption.getLabel());

        ChannelType lowBatteryChannelType = systemChannelTypeProvider.getChannelType(LOW_BATTERY_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(lowBatteryChannelType);
        assertEquals("Niedriger Batteriestatus", lowBatteryChannelType.getLabel());
        assertNull(lowBatteryChannelType.getDescription());

        ChannelType batteryLevelChannelType = systemChannelTypeProvider.getChannelType(BATTERY_LEVEL_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(batteryLevelChannelType);
        assertEquals("Batterieladung", batteryLevelChannelType.getLabel());
        assertNull(batteryLevelChannelType.getDescription());

        ChannelType powerChannelType = systemChannelTypeProvider.getChannelType(POWER_CHANNEL_TYPE_UID, Locale.GERMAN);
        assertNotNull(powerChannelType);
        assertEquals("Betrieb", powerChannelType.getLabel());
        assertEquals(
                "Ermöglicht die Steuerung der Betriebsbereitschaft. Das Gerät ist betriebsbereit, wenn \"Betrieb\" den Status ON hat.",
                powerChannelType.getDescription());

        ChannelType locationChannelType = systemChannelTypeProvider.getChannelType(LOCATION_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(locationChannelType);
        assertEquals("Ort", locationChannelType.getLabel());
        assertEquals("Ort in geographischen Koordinaten (Breitengrad/Längengrad/Höhe).",
                locationChannelType.getDescription());

        ChannelType motionChannelType = systemChannelTypeProvider.getChannelType(MOTION_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(motionChannelType);
        assertEquals("Bewegung", motionChannelType.getLabel());
        assertEquals("Zeigt eine erkannte Bewegung an.", motionChannelType.getDescription());

        ChannelType brightnessChannelType = systemChannelTypeProvider.getChannelType(BRIGHTNESS_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(brightnessChannelType);
        assertEquals("Helligkeit", brightnessChannelType.getLabel());
        assertEquals("Steuert die Helligkeit und schaltet das Licht ein und aus.",
                brightnessChannelType.getDescription());

        ChannelType colorChannelType = systemChannelTypeProvider.getChannelType(COLOR_CHANNEL_TYPE_UID, Locale.GERMAN);
        assertNotNull(colorChannelType);
        assertEquals("Farbe", colorChannelType.getLabel());
        assertEquals("Steuert die Lichtfarbe.", colorChannelType.getDescription());

        ChannelType colorTemperatureChannelType = systemChannelTypeProvider
                .getChannelType(COLOR_TEMPERATURE_CHANNEL_TYPE_UID, Locale.GERMAN);
        assertNotNull(colorTemperatureChannelType);
        assertEquals("Farbtemperatur", colorTemperatureChannelType.getLabel());
        assertEquals("Steuert die Farbtemperatur des Lichts.", colorTemperatureChannelType.getDescription());

        ChannelType volumeChannelType = systemChannelTypeProvider.getChannelType(VOLUME_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(volumeChannelType);
        assertEquals("Lautstärke", volumeChannelType.getLabel());
        assertEquals("Ermöglicht die Steuerung der Lautstärke.", volumeChannelType.getDescription());

        ChannelType muteChannelType = systemChannelTypeProvider.getChannelType(MUTE_CHANNEL_TYPE_UID, Locale.GERMAN);
        assertNotNull(muteChannelType);
        assertEquals("Stumm schalten", muteChannelType.getLabel());
        assertEquals("Ermöglicht die Lautstärke auf stumm zu schalten.", muteChannelType.getDescription());

        ChannelType mediaControlChannelType = systemChannelTypeProvider.getChannelType(MEDIA_CONTROL_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(mediaControlChannelType);
        assertEquals("Fernbedienung", mediaControlChannelType.getLabel());
        assertNull(mediaControlChannelType.getDescription());

        ChannelType mediaTitleChannelType = systemChannelTypeProvider.getChannelType(MEDIA_TITLE_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(mediaTitleChannelType);
        assertEquals("Titel", mediaTitleChannelType.getLabel());
        assertEquals("Zeigt den Titel der (aktuell abgespielten) Video- oder Audiodatei an.",
                mediaTitleChannelType.getDescription());

        ChannelType mediaArtistChannelType = systemChannelTypeProvider.getChannelType(MEDIA_ARTIST_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(mediaArtistChannelType);
        assertEquals("Künstler", mediaArtistChannelType.getLabel());
        assertEquals("Zeigt den Künstler der (aktuell abgespielten) Video- oder Audiodatei an.",
                mediaArtistChannelType.getDescription());

        ChannelType windDirectionChannelType = systemChannelTypeProvider.getChannelType(WIND_DIRECTION_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(windDirectionChannelType);
        assertEquals("Windrichtung", windDirectionChannelType.getLabel());
        assertEquals("Aktuelle Windrichtung ausgedrückt als Winkel.", windDirectionChannelType.getDescription());

        ChannelType windSpeedChannelType = systemChannelTypeProvider.getChannelType(WIND_SPEED_CHANNEL_TYPE_UID,
                Locale.GERMAN);
        assertNotNull(windSpeedChannelType);
        assertEquals("Windgeschwindigkeit", windSpeedChannelType.getLabel());
        assertEquals("Aktuelle Windgeschwindigkeit.", windSpeedChannelType.getDescription());

        ChannelType outdoorTemperatureChannelType = systemChannelTypeProvider
                .getChannelType(OUTDOOR_TEMPERATURE_CHANNEL_TYPE_UID, Locale.GERMAN);
        assertNotNull(outdoorTemperatureChannelType);
        assertEquals("Außentemperatur", outdoorTemperatureChannelType.getLabel());
        assertEquals("Aktuelle Außentemperatur.", outdoorTemperatureChannelType.getDescription());

        ChannelType atmosphericHumidityChannelType = systemChannelTypeProvider
                .getChannelType(ATMOSPHERIC_HUMIDITY_CHANNEL_TYPE_UID, Locale.GERMAN);
        assertNotNull(atmosphericHumidityChannelType);
        assertEquals("Luftfeuchtigkeit", atmosphericHumidityChannelType.getLabel());
        assertEquals("Aktuelle atmosphärische relative Luftfeuchtigkeit.",
                atmosphericHumidityChannelType.getDescription());

        ChannelType barometricPressureChannelType = systemChannelTypeProvider
                .getChannelType(BAROMETRIC_PRESSURE_CHANNEL_TYPE_UID, Locale.GERMAN);
        assertNotNull(barometricPressureChannelType);
        assertEquals("Luftdruck", barometricPressureChannelType.getLabel());
        assertEquals("Aktueller Luftdruck.", barometricPressureChannelType.getDescription());
    }
}
