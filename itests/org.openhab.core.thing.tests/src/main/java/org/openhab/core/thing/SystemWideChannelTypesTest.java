/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.*;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;

/**
 * Test cases for the {@link DefaultSystemChannelTypeProvider} class.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class SystemWideChannelTypesTest extends JavaOSGiTest {

    private static final int NUMBER_OF_SYSTEM_WIDE_CHANNEL_TYPES = 29;

    private @NonNullByDefault({}) ChannelTypeProvider systemChannelTypeProvider;

    @BeforeEach
    public void setUp() {
        ChannelTypeProvider provider = getService(ChannelTypeProvider.class, DefaultSystemChannelTypeProvider.class);
        assertTrue(provider instanceof DefaultSystemChannelTypeProvider);
        systemChannelTypeProvider = provider;
    }

    @Test
    public void systemChannelTypesShouldBeAvailable() {
        Collection<ChannelType> sytemChannelTypes = systemChannelTypeProvider.getChannelTypes(null);
        assertEquals(NUMBER_OF_SYSTEM_WIDE_CHANNEL_TYPES, sytemChannelTypes.size());

        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_SIGNAL_STRENGTH, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_LOW_BATTERY, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_BATTERY_LEVEL, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_TRIGGER, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_RAWBUTTON, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_BUTTON, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_RAWROCKER, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_POWER, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_LOCATION, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_MOTION, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_BRIGHTNESS, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_COLOR, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE_ABS, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_VOLUME, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_MUTE, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_MEDIA_CONTROL, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_MEDIA_TITLE, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_MEDIA_ARTIST, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_WIND_DIRECTION, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_WIND_SPEED, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_OUTDOOR_TEMPERATURE, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_INDOOR_TEMPERATURE, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_ATMOSPHERIC_HUMIDITY, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_BAROMETRIC_PRESSURE, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_ELECTRIC_POWER, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_ELECTRIC_CURRENT, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_ELECTRIC_VOLTAGE, null));
        assertNotNull(systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_ELECTRICAL_ENERGY, null));
    }

    @Test
    public void systemChannelTypesShouldBeTranslatedProperly() {
        Collection<ChannelType> localizedChannelTypes = systemChannelTypeProvider.getChannelTypes(Locale.GERMAN);
        assertEquals(NUMBER_OF_SYSTEM_WIDE_CHANNEL_TYPES, localizedChannelTypes.size());

        ChannelType signalStrengthChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_SIGNAL_STRENGTH, Locale.GERMAN);
        assertNotNull(signalStrengthChannelType);
        assertEquals("Signalstärke", signalStrengthChannelType.getLabel());
        assertNotNull(signalStrengthChannelType.getDescription());

        StateDescription stateDescription = signalStrengthChannelType.getState();
        if (stateDescription != null) {
            List<StateOption> signalStrengthChannelTypeOptions = stateDescription.getOptions();
            assertEquals(5, signalStrengthChannelTypeOptions.size());

            StateOption noSignalOption = signalStrengthChannelTypeOptions.stream()
                    .filter(it -> "0".equals(it.getValue())).findFirst().get();
            assertNotNull(noSignalOption);
            assertEquals("Kein Signal", noSignalOption.getLabel());
            StateOption weakOption = signalStrengthChannelTypeOptions.stream().filter(it -> "1".equals(it.getValue()))
                    .findFirst().get();
            assertNotNull(weakOption);
            assertEquals("Schwach", weakOption.getLabel());
            StateOption averageOption = signalStrengthChannelTypeOptions.stream()
                    .filter(it -> "2".equals(it.getValue())).findFirst().get();
            assertNotNull(averageOption);
            assertEquals("Durchschnittlich", averageOption.getLabel());
            StateOption goodOption = signalStrengthChannelTypeOptions.stream().filter(it -> "3".equals(it.getValue()))
                    .findFirst().get();
            assertNotNull(goodOption);
            assertEquals("Gut", goodOption.getLabel());
            StateOption excellentOption = signalStrengthChannelTypeOptions.stream()
                    .filter(it -> "4".equals(it.getValue())).findFirst().get();
            assertNotNull(excellentOption);
            assertEquals("Ausgezeichnet", excellentOption.getLabel());
        }

        ChannelType lowBatteryChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_LOW_BATTERY, Locale.GERMAN);
        assertNotNull(lowBatteryChannelType);
        assertEquals("Niedriger Batteriestatus", lowBatteryChannelType.getLabel());
        assertNotNull(lowBatteryChannelType.getDescription());

        ChannelType batteryLevelChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_BATTERY_LEVEL, Locale.GERMAN);
        assertNotNull(batteryLevelChannelType);
        assertEquals("Batterieladung", batteryLevelChannelType.getLabel());
        assertNotNull(batteryLevelChannelType.getDescription());

        ChannelType powerChannelType = systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_POWER,
                Locale.GERMAN);
        assertNotNull(powerChannelType);
        assertEquals("Betrieb", powerChannelType.getLabel());
        assertEquals("Steuert die Betriebsbereitschaft. Bei ON ist das Gerät betriebsbereit, bei OFF nicht.",
                powerChannelType.getDescription());

        ChannelType locationChannelType = systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_LOCATION,
                Locale.GERMAN);
        assertNotNull(locationChannelType);
        assertEquals("Ort", locationChannelType.getLabel());
        assertEquals("Zeigt einen Ort in geographischen Koordinaten (Breitengrad/Längengrad/Höhe) an.",
                locationChannelType.getDescription());

        ChannelType motionChannelType = systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_MOTION,
                Locale.GERMAN);
        assertNotNull(motionChannelType);
        assertEquals("Bewegung", motionChannelType.getLabel());
        assertEquals("Zeigt eine erkannte Bewegung an.", motionChannelType.getDescription());

        ChannelType brightnessChannelType = systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_BRIGHTNESS,
                Locale.GERMAN);
        assertNotNull(brightnessChannelType);
        assertEquals("Helligkeit", brightnessChannelType.getLabel());
        assertEquals("Steuert die Helligkeit und schaltet das Licht ein und aus.",
                brightnessChannelType.getDescription());

        ChannelType colorChannelType = systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_COLOR,
                Locale.GERMAN);
        assertNotNull(colorChannelType);
        assertEquals("Farbe", colorChannelType.getLabel());
        assertEquals("Steuert die Farbe, die Helligkeit und schaltet das Licht ein und aus.",
                colorChannelType.getDescription());

        ChannelType colorTemperatureChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE, Locale.GERMAN);
        assertNotNull(colorTemperatureChannelType);
        assertEquals("Farbtemperatur", colorTemperatureChannelType.getLabel());
        assertEquals("Steuert die Farbtemperatur des Lichts von 0 (kalt) bis 100 (warm).",
                colorTemperatureChannelType.getDescription());

        ChannelType colorTemperatureAbsChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE_ABS, Locale.GERMAN);
        assertNotNull(colorTemperatureAbsChannelType);
        assertEquals("Farbtemperatur", colorTemperatureAbsChannelType.getLabel());
        assertEquals("Steuert die Farbtemperatur des Lichts (in Kelvin).",
                colorTemperatureAbsChannelType.getDescription());

        ChannelType volumeChannelType = systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_VOLUME,
                Locale.GERMAN);
        assertNotNull(volumeChannelType);
        assertEquals("Lautstärke", volumeChannelType.getLabel());
        assertEquals("Steuert die Lautstärke eines Gerätes.", volumeChannelType.getDescription());

        ChannelType muteChannelType = systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_MUTE,
                Locale.GERMAN);
        assertNotNull(muteChannelType);
        assertEquals("Stumm schalten", muteChannelType.getLabel());
        assertEquals("Schaltet ein Gerät stumm.", muteChannelType.getDescription());

        ChannelType mediaControlChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_MEDIA_CONTROL, Locale.GERMAN);
        assertNotNull(mediaControlChannelType);
        assertEquals("Fernbedienung", mediaControlChannelType.getLabel());
        assertNull(mediaControlChannelType.getDescription());

        ChannelType mediaTitleChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_MEDIA_TITLE, Locale.GERMAN);
        assertNotNull(mediaTitleChannelType);
        assertEquals("Titel", mediaTitleChannelType.getLabel());
        assertEquals("Zeigt den Titel der (aktuell abgespielten) Video- oder Audiodatei an.",
                mediaTitleChannelType.getDescription());

        ChannelType mediaArtistChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_MEDIA_ARTIST, Locale.GERMAN);
        assertNotNull(mediaArtistChannelType);
        assertEquals("Künstler", mediaArtistChannelType.getLabel());
        assertEquals("Zeigt den Künstler der (aktuell abgespielten) Video- oder Audiodatei an.",
                mediaArtistChannelType.getDescription());

        ChannelType windDirectionChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_WIND_DIRECTION, Locale.GERMAN);
        assertNotNull(windDirectionChannelType);
        assertEquals("Windrichtung", windDirectionChannelType.getLabel());
        assertEquals("Zeigt die aktuelle Windrichtung an (als Winkel).", windDirectionChannelType.getDescription());

        ChannelType windSpeedChannelType = systemChannelTypeProvider.getChannelType(SYSTEM_CHANNEL_TYPE_UID_WIND_SPEED,
                Locale.GERMAN);
        assertNotNull(windSpeedChannelType);
        assertEquals("Windgeschwindigkeit", windSpeedChannelType.getLabel());
        assertEquals("Zeigt die aktuelle Windgeschwindigkeit an.", windSpeedChannelType.getDescription());

        ChannelType outdoorTemperatureChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_OUTDOOR_TEMPERATURE, Locale.GERMAN);
        assertNotNull(outdoorTemperatureChannelType);
        assertEquals("Außentemperatur", outdoorTemperatureChannelType.getLabel());
        assertEquals("Zeigt die aktuelle Außentemperatur an.", outdoorTemperatureChannelType.getDescription());

        ChannelType atmosphericHumidityChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_ATMOSPHERIC_HUMIDITY, Locale.GERMAN);
        assertNotNull(atmosphericHumidityChannelType);
        assertEquals("Luftfeuchtigkeit", atmosphericHumidityChannelType.getLabel());
        assertEquals("Zeigt die aktuelle atmosphärische relative Luftfeuchtigkeit an.",
                atmosphericHumidityChannelType.getDescription());

        ChannelType barometricPressureChannelType = systemChannelTypeProvider
                .getChannelType(SYSTEM_CHANNEL_TYPE_UID_BAROMETRIC_PRESSURE, Locale.GERMAN);
        assertNotNull(barometricPressureChannelType);
        assertEquals("Luftdruck", barometricPressureChannelType.getLabel());
        assertEquals("Zeigt den aktuellen Luftdruck an.", barometricPressureChannelType.getDescription());
    }
}
