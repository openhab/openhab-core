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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.LocalizedKey;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.EventDescription;
import org.openhab.core.types.EventOption;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implementation providing default system wide channel types
 *
 * @author Ivan Iliev - Initial contribution
 * @author Chris Jackson - Added battery level
 * @author Dennis Nobel - Changed to {@link ChannelTypeProvider}
 * @author Markus Rathgeb - Make battery-low indication read-only
 * @author Moritz Kammerer - Added system trigger types
 * @author Christoph Weitkamp - Added support for translation
 * @author Stefan Triller - Added more system channels
 * @author Christoph Weitkamp - factored out common i18n aspects into ThingTypeI18nLocalizationService
 */
@NonNullByDefault
@Component
public class DefaultSystemChannelTypeProvider implements ChannelTypeProvider {

    static final String BINDING_ID = "system";

    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_SIGNAL_STRENGTH = new ChannelTypeUID(BINDING_ID,
            "signal-strength");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_LOW_BATTERY = new ChannelTypeUID(BINDING_ID,
            "low-battery");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_BATTERY_LEVEL = new ChannelTypeUID(BINDING_ID,
            "battery-level");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_TRIGGER = new ChannelTypeUID(BINDING_ID, "trigger");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_RAWBUTTON = new ChannelTypeUID(BINDING_ID, "rawbutton");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_BUTTON = new ChannelTypeUID(BINDING_ID, "button");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_RAWROCKER = new ChannelTypeUID(BINDING_ID, "rawrocker");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_POWER = new ChannelTypeUID(BINDING_ID, "power");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_LOCATION = new ChannelTypeUID(BINDING_ID, "location");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_MOTION = new ChannelTypeUID(BINDING_ID, "motion");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_BRIGHTNESS = new ChannelTypeUID(BINDING_ID,
            "brightness");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_COLOR = new ChannelTypeUID(BINDING_ID, "color");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE = new ChannelTypeUID(BINDING_ID,
            "color-temperature");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE_ABS = new ChannelTypeUID(BINDING_ID,
            "color-temperature-abs");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_VOLUME = new ChannelTypeUID(BINDING_ID, "volume");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_MUTE = new ChannelTypeUID(BINDING_ID, "mute");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_MEDIA_CONTROL = new ChannelTypeUID(BINDING_ID,
            "media-control");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_MEDIA_TITLE = new ChannelTypeUID(BINDING_ID,
            "media-title");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_MEDIA_ARTIST = new ChannelTypeUID(BINDING_ID,
            "media-artist");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_WIND_DIRECTION = new ChannelTypeUID(BINDING_ID,
            "wind-direction");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_WIND_SPEED = new ChannelTypeUID(BINDING_ID,
            "wind-speed");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_OUTDOOR_TEMPERATURE = new ChannelTypeUID(BINDING_ID,
            "outdoor-temperature");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_INDOOR_TEMPERATURE = new ChannelTypeUID(BINDING_ID,
            "indoor-temperature");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_ATMOSPHERIC_HUMIDITY = new ChannelTypeUID(BINDING_ID,
            "atmospheric-humidity");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_BAROMETRIC_PRESSURE = new ChannelTypeUID(BINDING_ID,
            "barometric-pressure");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_ELECTRIC_POWER = new ChannelTypeUID(BINDING_ID,
            "electric-power");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_ELECTRIC_CURRENT = new ChannelTypeUID(BINDING_ID,
            "electric-current");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_ELECTRIC_VOLTAGE = new ChannelTypeUID(BINDING_ID,
            "electric-voltage");
    public static final ChannelTypeUID SYSTEM_CHANNEL_TYPE_UID_ELECTRICAL_ENERGY = new ChannelTypeUID(BINDING_ID,
            "electrical-energy");

    /**
     * Signal strength default system wide {@link ChannelType}. Represents signal strength of a device as a number
     * with values 0, 1, 2, 3 or 4, 0 being worst strength and 4 being best strength.
     */
    public static final ChannelType SYSTEM_CHANNEL_SIGNAL_STRENGTH = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_SIGNAL_STRENGTH, "Signal Strength", CoreItemFactory.NUMBER)
            .withDescription("Signal strength as with values 0 (worst), 1, 2, 3 or 4 (best)")
            .withCategory("QualityOfService")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(4)).withStep(BigDecimal.ONE).withReadOnly(Boolean.TRUE)
                    .withOptions(List.of(new StateOption("0", "no signal"), new StateOption("1", "weak"),
                            new StateOption("2", "average"), new StateOption("3", "good"),
                            new StateOption("4", "excellent")))
                    .build())
            .withTags(List.of("Measurement", "Level")).build();

    /**
     * Low battery default system wide {@link ChannelType}. Represents a low battery warning with possible values
     * on (low battery) and off (battery ok).
     */
    public static final ChannelType SYSTEM_CHANNEL_LOW_BATTERY = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_LOW_BATTERY, "Low Battery", CoreItemFactory.SWITCH)
            .withDescription("Low battery warning with possible values on (low battery) and off (battery ok)")
            .withCategory("LowBattery")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withReadOnly(true).build())
            .withTags(List.of("LowBattery", "Energy")).build();

    /**
     * Battery level default system wide {@link ChannelType}. Represents the battery level as a percentage.
     */
    public static final ChannelType SYSTEM_CHANNEL_BATTERY_LEVEL = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_BATTERY_LEVEL, "Battery Level", CoreItemFactory.NUMBER)
            .withDescription("Battery level as a percentage (0-100%)").withCategory("Battery")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(100)).withStep(BigDecimal.ONE).withReadOnly(true).withPattern("%.0f %%")
                    .build())
            .withTags(List.of("Measurement", "Energy")).build();

    /**
     * System wide trigger {@link ChannelType} without event options.
     */
    public static final ChannelType SYSTEM_TRIGGER = ChannelTypeBuilder
            .trigger(SYSTEM_CHANNEL_TYPE_UID_TRIGGER, "Trigger").build();

    /**
     * System wide trigger {@link ChannelType} which triggers "PRESSED" and "RELEASED" events.
     */
    public static final ChannelType SYSTEM_RAWBUTTON = ChannelTypeBuilder
            .trigger(SYSTEM_CHANNEL_TYPE_UID_RAWBUTTON, "Raw Button")
            .withEventDescription(new EventDescription(List.of(new EventOption(CommonTriggerEvents.PRESSED, null),
                    new EventOption(CommonTriggerEvents.RELEASED, null))))
            .build();

    /**
     * System wide trigger {@link ChannelType} which triggers "SHORT_PRESSED", "DOUBLE_PRESSED" and "LONG_PRESSED"
     * events.
     */
    public static final ChannelType SYSTEM_BUTTON = ChannelTypeBuilder.trigger(SYSTEM_CHANNEL_TYPE_UID_BUTTON, "Button")
            .withEventDescription(new EventDescription(List.of(new EventOption(CommonTriggerEvents.SHORT_PRESSED, null),
                    new EventOption(CommonTriggerEvents.DOUBLE_PRESSED, null),
                    new EventOption(CommonTriggerEvents.LONG_PRESSED, null))))
            .build();

    /**
     * System wide trigger {@link ChannelType} which triggers "DIR1_PRESSED", "DIR1_RELEASED", "DIR2_PRESSED" and
     * "DIR2_RELEASED" events.
     */
    public static final ChannelType SYSTEM_RAWROCKER = ChannelTypeBuilder
            .trigger(SYSTEM_CHANNEL_TYPE_UID_RAWROCKER, "Raw Rocker Button")
            .withEventDescription(new EventDescription(List.of(new EventOption(CommonTriggerEvents.DIR1_PRESSED, null),
                    new EventOption(CommonTriggerEvents.DIR1_RELEASED, null),
                    new EventOption(CommonTriggerEvents.DIR2_PRESSED, null),
                    new EventOption(CommonTriggerEvents.DIR2_RELEASED, null))))
            .build();

    /**
     * Power: default system wide {@link ChannelType} which allows turning off (potentially on) a device
     */
    public static final ChannelType SYSTEM_POWER = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_POWER, "Power", CoreItemFactory.SWITCH)
            .withDescription("Device is operable when channel has state ON").withCategory("Switch")
            .withTags(List.of("Switch", "Power")).build();

    /**
     * Location: default system wide {@link ChannelType} which displays a location
     */
    public static final ChannelType SYSTEM_LOCATION = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_LOCATION, "Location", CoreItemFactory.LOCATION)
            .withDescription("Location in lat./lon./height coordinates")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withReadOnly(true)
                    .withPattern("%2$s°N %3$s°E %1$sm").build())
            .withTag("Measurement").build();

    /**
     * Motion: default system wide {@link ChannelType} which indications whether motion was detected (state ON)
     */
    public static final ChannelType SYSTEM_MOTION = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_MOTION, "Motion", CoreItemFactory.SWITCH)
            .withDescription("Motion detected by the device").withCategory("Motion")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withReadOnly(true).build())
            .withTags(List.of("Status", "Presence")).build();

    /**
     * Brightness: default system wide {@link ChannelType} which allows changing the brightness from 0-100%
     */
    public static final ChannelType SYSTEM_BRIGHTNESS = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_BRIGHTNESS, "Brightness", CoreItemFactory.DIMMER)
            .withDescription("Controls the brightness and switches the light on and off").withCategory("Light")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(100)).withPattern("%d %%").build())
            .withTags(List.of("Control", "Light")).build();

    /**
     * Color: default system wide {@link ChannelType} which allows changing the color
     */
    public static final ChannelType SYSTEM_COLOR = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_COLOR, "Color", CoreItemFactory.COLOR)
            .withDescription("Controls the color of the light").withCategory("ColorLight")
            .withTags(List.of("Control", "Light")).build();

    /**
     * Color-temperature: default system wide {@link ChannelType} which allows changing the color temperature in percent
     */
    public static final ChannelType SYSTEM_COLOR_TEMPERATURE = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE, "Color Temperature", CoreItemFactory.DIMMER)
            .withDescription("Controls the color temperature of the light from 0 (cold) to 100 (warm)")
            .withCategory("ColorLight")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(100)).withPattern("%.0f").build())
            .withTags(List.of("Control", "ColorTemperature")).build();

    /**
     * Color-temperature: default system wide {@link ChannelType} which allows changing the color temperature in Kelvin
     */
    public static final ChannelType SYSTEM_COLOR_TEMPERATURE_ABS = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE_ABS, "Color Temperature", "Number:Temperature")
            .withDescription("Controls the color temperature of the light in Kelvin").withCategory("ColorLight")
            .isAdvanced(true)
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(new BigDecimal(1000))
                    .withMaximum(new BigDecimal(10000)).withPattern("%.0f K").build())
            .withTags(List.of("Control", "ColorTemperature")).build();

    // media channels

    /**
     * Volume: default system wide {@link ChannelType} which allows changing the audio volume from 0-100%
     */
    public static final ChannelType SYSTEM_VOLUME = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_VOLUME, "Volume", CoreItemFactory.DIMMER)
            .withDescription("Change the sound volume of a device")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(100)).withPattern("%d %%").build())
            .withCategory("SoundVolume").withTags(List.of("Control", "SoundVolume")).build();

    /**
     * Mute: default system wide {@link ChannelType} which allows muting and un-muting audio
     */
    public static final ChannelType SYSTEM_MUTE = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_MUTE, "Mute", CoreItemFactory.SWITCH)
            .withDescription("Mute audio of the device").withCategory("SoundVolume")
            .withTags(List.of("Switch", "SoundVolume")).build();

    /**
     * Media-control: system wide {@link ChannelType} which controls a media player
     */
    public static final ChannelType SYSTEM_MEDIA_CONTROL = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_MEDIA_CONTROL, "Media Control", CoreItemFactory.PLAYER)
            .withCategory("MediaControl").withTag("Control").build();

    /**
     * Media-title: default system wide {@link ChannelType} which displays the title of a (played) song
     */
    public static final ChannelType SYSTEM_MEDIA_TITLE = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_MEDIA_TITLE, "Media Title", CoreItemFactory.STRING)
            .withDescription("Title of a (played) media file")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withReadOnly(true).build())
            .withTag("Status").build();

    /**
     * Media-artist: default system wide {@link ChannelType} which displays the artist of a (played) song
     */
    public static final ChannelType SYSTEM_MEDIA_ARTIST = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_MEDIA_ARTIST, "Media Artist", CoreItemFactory.STRING)
            .withDescription("Artist of a (played) media file")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withReadOnly(true).build())
            .withTag("Status").build();

    // weather channels

    /**
     * Wind-direction: system wide {@link ChannelType} which shows the wind direction in degrees 0-360
     */
    public static final ChannelType SYSTEM_WIND_DIRECTION = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_WIND_DIRECTION, "Wind Direction", "Number:Angle")
            .withDescription("Current wind direction expressed as an angle").withCategory("Wind")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(360)).withReadOnly(true).withPattern("%.0f %unit%").build())
            .withTags(List.of("Measurement", "Wind")).build();

    /**
     * Wind-speed: system wide {@link ChannelType} which shows the wind speed
     */
    public static final ChannelType SYSTEM_WIND_SPEED = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_WIND_SPEED, "Wind Speed", "Number:Speed")
            .withDescription("Current wind speed").withCategory("Wind")
            .withStateDescriptionFragment(
                    StateDescriptionFragmentBuilder.create().withReadOnly(true).withPattern("%.1f %unit%").build())
            .withTags(List.of("Measurement", "Wind")).build();

    /**
     * Outdoor-temperature: system wide {@link ChannelType} which shows the outdoor temperature
     */
    public static final ChannelType SYSTEM_OUTDOOR_TEMPERATURE = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_OUTDOOR_TEMPERATURE, "Outdoor Temperature", "Number:Temperature")
            .withDescription("Current outdoor temperature").withCategory("Temperature")
            .withStateDescriptionFragment(
                    StateDescriptionFragmentBuilder.create().withReadOnly(true).withPattern("%.1f %unit%").build())
            .withTags(List.of("Measurement", "Temperature")).build();

    /**
     * Indoor-temperature: system wide {@link ChannelType} which shows the indoor temperature
     */
    public static final ChannelType SYSTEM_INDOOR_TEMPERATURE = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_INDOOR_TEMPERATURE, "Indoor Temperature", "Number:Temperature")
            .withDescription("Current indoor temperature").withCategory("Temperature")
            .withStateDescriptionFragment(
                    StateDescriptionFragmentBuilder.create().withReadOnly(true).withPattern("%.1f %unit%").build())
            .withTags(List.of("Measurement", "Temperature")).build();

    /**
     * Atmospheric-humidity: system wide {@link ChannelType} which shows the atmospheric humidity
     */
    public static final ChannelType SYSTEM_ATMOSPHERIC_HUMIDITY = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_ATMOSPHERIC_HUMIDITY, "Atmospheric Humidity", "Number:Dimensionless")
            .withDescription("Current atmospheric relative humidity").withCategory("Humidity")
            .withStateDescriptionFragment(
                    StateDescriptionFragmentBuilder.create().withReadOnly(true).withPattern("%.0f %%").build())
            .withTags(List.of("Measurement", "Humidity")).build();

    /**
     * Barometric-pressure: system wide {@link ChannelType} which shows the barometric pressure
     */
    public static final ChannelType SYSTEM_BAROMETRIC_PRESSURE = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_BAROMETRIC_PRESSURE, "Barometric Pressure", "Number:Pressure")
            .withDescription("Current barometric pressure").withCategory("Pressure")
            .withStateDescriptionFragment(
                    StateDescriptionFragmentBuilder.create().withReadOnly(true).withPattern("%.3f %unit%").build())
            .withTags(List.of("Measurement", "Pressure")).build();

    // Energy

    /**
     * Electric-power: system wide {@link ChannelType} which shows the electric power
     */
    public static final ChannelType SYSTEM_ELECTRIC_POWER = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_ELECTRIC_POWER, "Electric Power", "Number:Power")
            .withDescription("Current electric power").withCategory("Energy")
            .withStateDescriptionFragment(
                    StateDescriptionFragmentBuilder.create().withReadOnly(true).withPattern("%.1f %unit%").build())
            .withTags(List.of("Measurement", "Power")).build();

    /**
     * Electric-current: system wide {@link ChannelType} which shows the electric current
     */
    public static final ChannelType SYSTEM_ELECTRIC_CURRENT = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_ELECTRIC_CURRENT, "Electric Current", "Number:ElectricCurrent")
            .withDescription("Current electric current").withCategory("Energy")
            .withStateDescriptionFragment(
                    StateDescriptionFragmentBuilder.create().withReadOnly(true).withPattern("%.1f %unit%").build())
            .withTags(List.of("Measurement", "Current")).build();

    /**
     * Electric-voltage: system wide {@link ChannelType} which shows the electric voltage
     */
    public static final ChannelType SYSTEM_ELECTRIC_VOLTAGE = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_ELECTRIC_VOLTAGE, "Electric Voltage", "Number:ElectricPotential")
            .withDescription("Current electric voltage").withCategory("Energy")
            .withStateDescriptionFragment(
                    StateDescriptionFragmentBuilder.create().withReadOnly(true).withPattern("%.1f %unit%").build())
            .withTags(List.of("Measurement", "Voltage")).build();

    /**
     * Electrical-energy: system wide {@link ChannelType} which shows the electrical energy
     */
    public static final ChannelType SYSTEM_ELECTRICAL_ENERGY = ChannelTypeBuilder
            .state(SYSTEM_CHANNEL_TYPE_UID_ELECTRICAL_ENERGY, "Electrical Energy", "Number:Energy")
            .withDescription("Current electrical energy").withCategory("Energy")
            .withStateDescriptionFragment(
                    StateDescriptionFragmentBuilder.create().withReadOnly(true).withPattern("%.1f %unit%").build())
            .withTags(List.of("Measurement", "Energy")).build();

    private static final Collection<ChannelType> CHANNEL_TYPES = List.of(SYSTEM_CHANNEL_SIGNAL_STRENGTH,
            SYSTEM_CHANNEL_LOW_BATTERY, SYSTEM_CHANNEL_BATTERY_LEVEL, SYSTEM_TRIGGER, SYSTEM_RAWBUTTON, SYSTEM_BUTTON,
            SYSTEM_RAWROCKER, SYSTEM_POWER, SYSTEM_LOCATION, SYSTEM_MOTION, SYSTEM_BRIGHTNESS, SYSTEM_COLOR,
            SYSTEM_COLOR_TEMPERATURE, SYSTEM_COLOR_TEMPERATURE_ABS, SYSTEM_VOLUME, SYSTEM_MUTE, SYSTEM_MEDIA_CONTROL,
            SYSTEM_MEDIA_TITLE, SYSTEM_MEDIA_ARTIST, SYSTEM_WIND_DIRECTION, SYSTEM_WIND_SPEED,
            SYSTEM_OUTDOOR_TEMPERATURE, SYSTEM_INDOOR_TEMPERATURE, SYSTEM_ATMOSPHERIC_HUMIDITY,
            SYSTEM_BAROMETRIC_PRESSURE, SYSTEM_ELECTRIC_POWER, SYSTEM_ELECTRIC_CURRENT, SYSTEM_ELECTRIC_VOLTAGE,
            SYSTEM_ELECTRICAL_ENERGY);

    private final Map<LocalizedKey, ChannelType> localizedChannelTypeCache = new ConcurrentHashMap<>();

    private final ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService;
    private final BundleResolver bundleResolver;

    @Activate
    public DefaultSystemChannelTypeProvider(
            final @Reference ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService,
            final @Reference BundleResolver bundleResolver) {
        this.channelTypeI18nLocalizationService = channelTypeI18nLocalizationService;
        this.bundleResolver = bundleResolver;
    }

    @Override
    public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        final List<ChannelType> allChannelTypes = new ArrayList<>();
        final Bundle bundle = bundleResolver.resolveBundle(DefaultSystemChannelTypeProvider.class);

        for (final ChannelType channelType : CHANNEL_TYPES) {
            allChannelTypes.add(createLocalizedChannelType(bundle, channelType, locale));
        }
        return allChannelTypes;
    }

    @Override
    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        final Bundle bundle = bundleResolver.resolveBundle(DefaultSystemChannelTypeProvider.class);

        for (final ChannelType channelType : CHANNEL_TYPES) {
            if (channelTypeUID.equals(channelType.getUID())) {
                return createLocalizedChannelType(bundle, channelType, locale);
            }
        }
        return null;
    }

    private ChannelType createLocalizedChannelType(Bundle bundle, ChannelType channelType, @Nullable Locale locale) {
        LocalizedKey localizedKey = new LocalizedKey(channelType.getUID(),
                locale != null ? locale.toLanguageTag() : null);

        ChannelType cachedEntry = localizedChannelTypeCache.get(localizedKey);
        if (cachedEntry != null) {
            return cachedEntry;
        }

        ChannelType localizedChannelType = channelTypeI18nLocalizationService.createLocalizedChannelType(bundle,
                channelType, locale);
        localizedChannelTypeCache.put(localizedKey, localizedChannelType);
        return localizedChannelType;
    }
}
