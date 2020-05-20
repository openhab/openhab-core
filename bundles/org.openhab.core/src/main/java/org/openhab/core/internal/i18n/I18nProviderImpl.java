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
package org.openhab.core.internal.i18n;

import static org.openhab.core.library.unit.MetricPrefix.HECTO;

import java.text.MessageFormat;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.LocationProvider;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.library.dimension.Intensity;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.SmartHomeUnits;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link I18nProviderImpl} is a concrete implementation of the {@link TranslationProvider}, {@link LocaleProvider},
 * and {@link LocationProvider} service interfaces.
 *
 * <p>
 * This implementation uses the i18n mechanism of Java ({@link ResourceBundle}) to translate a given key into text. The
 * resources must be placed under the specific directory {@link LanguageResourceBundleManager#RESOURCE_DIRECTORY} within
 * the certain modules. Each module is tracked in the platform by using the {@link ResourceBundleTracker} and managed by
 * using one certain {@link LanguageResourceBundleManager} which is responsible for the translation.
 * <p>
 * <p>
 * It reads a user defined configuration to set a locale and a location for this installation.
 *
 * @author Michael Grammling - Initial contribution
 * @author Thomas Höfer - Added getText operation with arguments
 * @author Markus Rathgeb - Initial contribution
 * @author Stefan Triller - Initial contribution
 * @author Erdoan Hadzhiyusein - Added time zone
 */
@Component(immediate = true, configurationPid = I18nProviderImpl.CONFIGURATION_PID, property = {
        Constants.SERVICE_PID + "=org.openhab.i18n", //
        "service.config.label=Regional Settings", //
        "service.config.category=system", //
        "service.config.description.uri=system:i18n" })
@NonNullByDefault
public class I18nProviderImpl
        implements TranslationProvider, LocaleProvider, LocationProvider, TimeZoneProvider, UnitProvider {

    private final Logger logger = LoggerFactory.getLogger(I18nProviderImpl.class);

    public static final String CONFIGURATION_PID = "org.openhab.i18n";

    // LocaleProvider
    public static final String LANGUAGE = "language";
    public static final String SCRIPT = "script";
    public static final String REGION = "region";
    public static final String VARIANT = "variant";
    private @Nullable Locale locale;

    // TranslationProvider
    private final ResourceBundleTracker resourceBundleTracker;

    // LocationProvider
    static final String LOCATION = "location";
    private @Nullable PointType location;

    // TimeZoneProvider
    static final String TIMEZONE = "timezone";
    private @Nullable ZoneId timeZone;

    // UnitProvider
    private static final String MEASUREMENT_SYSTEM = "measurementSystem";
    private @Nullable SystemOfUnits measurementSystem;
    private final Map<Class<? extends Quantity<?>>, Map<SystemOfUnits, Unit<? extends Quantity<?>>>> dimensionMap = new HashMap<>();

    @Activate
    @SuppressWarnings("unchecked")
    public I18nProviderImpl(ComponentContext componentContext) {
        initDimensionMap();
        modified((Map<String, Object>) componentContext.getProperties());

        this.resourceBundleTracker = new ResourceBundleTracker(componentContext.getBundleContext(), this);
        this.resourceBundleTracker.open();
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        this.resourceBundleTracker.close();
    }

    @Modified
    protected synchronized void modified(Map<String, Object> config) {
        final String language = toStringOrNull(config.get(LANGUAGE));
        final String script = toStringOrNull(config.get(SCRIPT));
        final String region = toStringOrNull(config.get(REGION));
        final String variant = toStringOrNull(config.get(VARIANT));
        final String location = toStringOrNull(config.get(LOCATION));
        final String zoneId = toStringOrNull(config.get(TIMEZONE));
        final String measurementSystem = toStringOrNull(config.get(MEASUREMENT_SYSTEM));

        setTimeZone(zoneId);
        setLocation(location);
        setLocale(language, script, region, variant);
        setMeasurementSystem(measurementSystem);
    }

    private void setMeasurementSystem(@Nullable String measurementSystem) {
        SystemOfUnits oldMeasurementSystem = this.measurementSystem;

        final String ms;
        if (measurementSystem == null || measurementSystem.isEmpty()) {
            ms = "";
        } else {
            ms = measurementSystem;
        }

        final SystemOfUnits newMeasurementSystem;
        switch (ms) {
            case "SI":
                newMeasurementSystem = SIUnits.getInstance();
                break;
            case "US":
                newMeasurementSystem = ImperialUnits.getInstance();
                break;
            default:
                logger.debug("Error setting measurement system for value '{}'.", measurementSystem);
                newMeasurementSystem = null;
                break;
        }
        this.measurementSystem = newMeasurementSystem;

        if (oldMeasurementSystem != null && newMeasurementSystem == null) {
            logger.info("Measurement system is not set, falling back to locale based system.");
        } else if (newMeasurementSystem != null && !newMeasurementSystem.equals(oldMeasurementSystem)) {
            logger.info("Measurement system set to '{}'.", newMeasurementSystem.getName());
        }
    }

    private void setLocale(@Nullable String language, @Nullable String script, @Nullable String region,
            @Nullable String variant) {
        Locale oldLocale = this.locale;
        if (language == null || language.isEmpty()) {
            // at least the language must be defined otherwise the system default locale is used
            logger.debug("No language set, setting locale to 'null'.");
            locale = null;
            if (oldLocale != null) {
                logger.info("Locale is not set, falling back to the default locale");
            }
            return;
        }

        final Locale.Builder builder = new Locale.Builder();
        try {
            builder.setLanguage(language);
        } catch (final RuntimeException ex) {
            logger.warn("Language ({}) is invalid. Cannot create locale, keep old one.", language, ex);
            return;
        }

        try {
            builder.setScript(script);
        } catch (final RuntimeException ex) {
            logger.warn("Script ({}) is invalid. Skip it.", script, ex);
            return;
        }

        try {
            builder.setRegion(region);
        } catch (final RuntimeException ex) {
            logger.warn("Region ({}) is invalid. Skip it.", region, ex);
            return;
        }

        try {
            builder.setVariant(variant);
        } catch (final RuntimeException ex) {
            logger.warn("Variant ({}) is invalid. Skip it.", variant, ex);
            return;
        }
        final Locale newLocale = builder.build();
        locale = newLocale;

        if (!newLocale.equals(oldLocale)) {
            logger.info("Locale set to '{}'.", newLocale);
        }
    }

    private @Nullable String toStringOrNull(@Nullable Object value) {
        return value == null ? null : value.toString();
    }

    private void setLocation(final @Nullable String location) {
        PointType oldLocation = this.location;
        PointType newLocation;
        if (location == null || location.isEmpty()) {
            newLocation = null;
        } else {
            try {
                newLocation = PointType.valueOf(location);
            } catch (IllegalArgumentException e) {
                newLocation = oldLocation;
                // preserve old location or null if none was set before
                logger.warn("Could not set new location: {}, keeping old one, error message: {}", location,
                        e.getMessage());
            }
        }
        if (!Objects.equals(newLocation, oldLocation)) {
            this.location = newLocation;
            logger.info("Location set to '{}'.", newLocation);
        }
    }

    private void setTimeZone(final @Nullable String zoneId) {
        ZoneId oldTimeZone = this.timeZone;
        if (zoneId == null || zoneId.isBlank()) {
            timeZone = null;
        } else {
            try {
                timeZone = ZoneId.of(zoneId);
            } catch (DateTimeException e) {
                logger.warn("Error setting time zone '{}', falling back to the default time zone: {}", zoneId,
                        e.getMessage());
                timeZone = null;
            }
        }

        if (oldTimeZone != null && this.timeZone == null) {
            logger.info("Time zone is not set, falling back to the default time zone.");
        } else if (this.timeZone != null && !this.timeZone.equals(oldTimeZone)) {
            logger.info("Time zone set to '{}'.", this.timeZone);
        }
    }

    @Override
    public @Nullable PointType getLocation() {
        return location;
    }

    @Override
    public ZoneId getTimeZone() {
        final ZoneId timeZone = this.timeZone;
        if (timeZone == null) {
            return TimeZone.getDefault().toZoneId();
        }
        return timeZone;
    }

    @Override
    public Locale getLocale() {
        final Locale locale = this.locale;
        if (locale == null) {
            return Locale.getDefault();
        }
        return locale;
    }

    @Override
    public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
            @Nullable Locale locale) {
        LanguageResourceBundleManager languageResource = this.resourceBundleTracker.getLanguageResource(bundle);

        if (languageResource != null) {
            String text = languageResource.getText(key, locale);
            if (text != null) {
                return text;
            }
        }

        return defaultText;
    }

    @Override
    public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
            @Nullable Locale locale, @Nullable Object @Nullable... arguments) {
        String text = getText(bundle, key, defaultText, locale);

        if (text != null) {
            return MessageFormat.format(text, arguments);
        }

        return text;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Quantity<T>> @Nullable Unit<T> getUnit(@Nullable Class<T> dimension) {
        Map<SystemOfUnits, Unit<? extends Quantity<?>>> map = dimensionMap.get(dimension);
        if (map == null) {
            return null;
        }
        return (Unit<T>) map.get(getMeasurementSystem());
    }

    @Override
    public SystemOfUnits getMeasurementSystem() {
        final SystemOfUnits measurementSystem = this.measurementSystem;
        if (measurementSystem != null) {
            return measurementSystem;
        }

        // Only US and Liberia use the Imperial System.
        if (Locale.US.equals(locale) || Locale.forLanguageTag("en-LR").equals(locale)) {
            return ImperialUnits.getInstance();
        }
        return SIUnits.getInstance();
    }

    private void initDimensionMap() {
        Map<SystemOfUnits, Unit<? extends Quantity<?>>> temperatureMap = new HashMap<>();
        temperatureMap.put(SIUnits.getInstance(), SIUnits.CELSIUS);
        temperatureMap.put(ImperialUnits.getInstance(), ImperialUnits.FAHRENHEIT);
        dimensionMap.put(Temperature.class, temperatureMap);

        Map<SystemOfUnits, Unit<? extends Quantity<?>>> pressureMap = new HashMap<>();
        pressureMap.put(SIUnits.getInstance(), HECTO(SIUnits.PASCAL));
        pressureMap.put(ImperialUnits.getInstance(), ImperialUnits.INCH_OF_MERCURY);
        dimensionMap.put(Pressure.class, pressureMap);

        Map<SystemOfUnits, Unit<? extends Quantity<?>>> speedMap = new HashMap<>();
        speedMap.put(SIUnits.getInstance(), SIUnits.KILOMETRE_PER_HOUR);
        speedMap.put(ImperialUnits.getInstance(), ImperialUnits.MILES_PER_HOUR);
        dimensionMap.put(Speed.class, speedMap);

        Map<SystemOfUnits, Unit<? extends Quantity<?>>> lengthMap = new HashMap<>();
        lengthMap.put(SIUnits.getInstance(), SIUnits.METRE);
        lengthMap.put(ImperialUnits.getInstance(), ImperialUnits.INCH);
        dimensionMap.put(Length.class, lengthMap);

        Map<SystemOfUnits, Unit<? extends Quantity<?>>> intensityMap = new HashMap<>();
        intensityMap.put(SIUnits.getInstance(), SmartHomeUnits.IRRADIANCE);
        intensityMap.put(ImperialUnits.getInstance(), SmartHomeUnits.IRRADIANCE);
        dimensionMap.put(Intensity.class, intensityMap);

        Map<SystemOfUnits, Unit<? extends Quantity<?>>> percentMap = new HashMap<>();
        percentMap.put(SIUnits.getInstance(), SmartHomeUnits.ONE);
        percentMap.put(ImperialUnits.getInstance(), SmartHomeUnits.ONE);
        dimensionMap.put(Dimensionless.class, percentMap);

        Map<SystemOfUnits, Unit<? extends Quantity<?>>> angleMap = new HashMap<>();
        angleMap.put(SIUnits.getInstance(), SmartHomeUnits.DEGREE_ANGLE);
        angleMap.put(ImperialUnits.getInstance(), SmartHomeUnits.DEGREE_ANGLE);
        dimensionMap.put(Angle.class, angleMap);
    }
}
