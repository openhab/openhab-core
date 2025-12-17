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
package org.openhab.core.ephemeris.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.ephemeris.EphemerisManager;
import org.openhab.core.i18n.LocaleProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.focus_shift.jollyday.core.Holiday;
import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameter;
import de.focus_shift.jollyday.core.ManagerParameters;
import de.focus_shift.jollyday.core.parameter.CalendarPartManagerParameter;
import de.focus_shift.jollyday.core.util.ResourceUtil;

/**
 * This service provides functionality around ephemeris services and is the central service to be used directly by
 * others.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@Component(name = "org.openhab.ephemeris", property = Constants.SERVICE_PID + "=org.openhab.ephemeris")
@ConfigurableService(category = "system", label = "Ephemeris", description_uri = EphemerisManagerImpl.CONFIG_URI)
@NonNullByDefault
public class EphemerisManagerImpl implements EphemerisManager, ConfigOptionProvider {

    private final Logger logger = LoggerFactory.getLogger(EphemerisManagerImpl.class);

    // constants for the configuration properties
    protected static final String CONFIG_URI = "system:ephemeris";
    public static final String CONFIG_DAYSET_PREFIX = "dayset-";
    public static final String CONFIG_DAYSET_WEEKEND = "weekend";
    public static final String CONFIG_COUNTRY = "country";
    public static final String CONFIG_REGION = "region";
    public static final String CONFIG_CITY = "city";

    private static final String RESOURCES_ROOT = "jollyday/";
    private static final String JOLLYDAY_COUNTRY_DESCRIPTIONS = RESOURCES_ROOT
            + "descriptions/country_descriptions.properties";
    private static final String PROPERTY_COUNTRY_DESCRIPTION_PREFIX = "country.description.";
    private static final String PROPERTY_COUNTRY_DESCRIPTION_DELIMITER = "\\.";

    final List<ParameterOption> countries = new ArrayList<>();
    final Map<String, List<ParameterOption>> regions = new HashMap<>();
    final Map<String, List<ParameterOption>> cities = new HashMap<>();

    final Map<String, Set<DayOfWeek>> daysets = new HashMap<>();
    private final Map<Object, HolidayManager> holidayManagers = new HashMap<>();
    private final List<String> countryParameters = new ArrayList<>();

    private final LocaleProvider localeProvider;
    private final Bundle bundle;

    private @NonNullByDefault({}) String country;
    private @Nullable String region;

    /**
     * Constructs and activates the EphemerisManagerImpl.
     * This constructor is called by the OSGi framework during component activation.
     * It initializes the ephemeris manager with locale support, loads country/region/city descriptions
     * from the Jollyday library resources, and sets up the default weekend dayset.
     *
     * @param localeProvider the locale provider service for internationalization support
     * @param bundleContext the OSGi bundle context used to access bundle resources
     */
    @Activate
    public EphemerisManagerImpl(final @Reference LocaleProvider localeProvider, final BundleContext bundleContext) {
        this.localeProvider = localeProvider;
        bundle = bundleContext.getBundle();

        // Default weekend dayset
        addDayset(CONFIG_DAYSET_WEEKEND, List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

        try (InputStream stream = bundle.getResource(JOLLYDAY_COUNTRY_DESCRIPTIONS).openStream()) {
            final Properties properties = new Properties();
            properties.load(stream);
            properties.forEach(this::parseProperty);

            sortByLabel(countries);
            regions.values().forEach(this::sortByLabel);
            cities.values().forEach(this::sortByLabel);
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            logger.warn("The resource '{}' could not be loaded properly! ConfigDescription options are not available.",
                    JOLLYDAY_COUNTRY_DESCRIPTIONS, e);
        }
    }

    /**
     * Sorts a list of parameter options alphabetically by their label.
     * This ensures that country, region, and city options are presented in alphabetical order
     * in the user interface configuration dropdowns.
     *
     * @param parameterOptions the list of parameter options to sort in-place
     */
    private void sortByLabel(List<ParameterOption> parameterOptions) {
        parameterOptions.sort(Comparator.comparing(ParameterOption::getLabel));
    }

    /**
     * Activates the EphemerisManagerImpl component.
     * This method is called by the OSGi framework when the component is activated.
     * It delegates to the {@link #modified(Map)} method to process the initial configuration.
     *
     * @param config the initial configuration properties
     */
    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    /**
     * Processes configuration updates for the EphemerisManagerImpl.
     * This method is called by the OSGi framework when the configuration is modified.
     * It processes custom dayset definitions (e.g., custom weekend days), and updates
     * the country, region, and city parameters for holiday calculations.
     *
     * @param config the updated configuration properties
     */
    @Modified
    protected void modified(Map<String, Object> config) {
        config.entrySet().stream().filter(e -> e.getKey().startsWith(CONFIG_DAYSET_PREFIX)).forEach(e -> {
            try {
                String[] setNameParts = e.getKey().split("-");
                if (setNameParts.length > 1) {
                    String setName = setNameParts[1];
                    Object entry = e.getValue();
                    if (entry instanceof String) {
                        String value = entry.toString();
                        while (value.startsWith("[")) {
                            value = value.substring(1);
                        }
                        while (value.endsWith("]")) {
                            value = value.substring(0, value.length() - 1);
                        }
                        String[] setDefinition = value.split(",");
                        if (setDefinition.length > 0) {
                            addDayset(setName, List.of(setDefinition));
                        } else {
                            logger.warn("Erroneous day set definition {} : {}", e.getKey(), entry);
                        }
                    } else if (entry instanceof Iterable iterable) {
                        addDayset(setName, iterable);
                    }
                } else {
                    logger.warn("Erroneous day set definition {}", e.getKey());
                }
            } catch (IllegalArgumentException ex) {
                logger.warn("Erroneous day set definition {}: {}", e.getKey(), ex.getMessage());
            }
        });

        Object configValue = config.get(CONFIG_COUNTRY);
        if (configValue != null) {
            country = configValue.toString().toLowerCase();
        } else {
            country = localeProvider.getLocale().getCountry().toLowerCase();
            logger.debug("Using system default country '{}' ", country);
        }

        configValue = config.get(CONFIG_REGION);
        if (configValue != null) {
            String region = configValue.toString().toLowerCase();
            countryParameters.add(region);
            this.region = region;
        } else {
            this.region = null;
        }

        configValue = config.get(CONFIG_CITY);
        if (configValue != null) {
            countryParameters.add(configValue.toString());
        }
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (CONFIG_URI.equals(uri.toString())) {
            switch (param) {
                case CONFIG_COUNTRY:
                    return countries;
                case CONFIG_REGION:
                    if (regions.containsKey(country)) {
                        return regions.get(country);
                    }
                case CONFIG_CITY:
                    if (region != null && cities.containsKey(region)) {
                        return cities.get(region);
                    }
                default:
                    if (param.startsWith(CONFIG_DAYSET_PREFIX)) {
                        Locale nullSafeLocale = locale == null ? localeProvider.getLocale() : locale;
                        final List<ParameterOption> options = new ArrayList<>();
                        for (DayOfWeek day : DayOfWeek.values()) {
                            ParameterOption option = new ParameterOption(day.name(),
                                    day.getDisplayName(TextStyle.FULL, nullSafeLocale));
                            options.add(option);
                        }
                        return options;
                    }
                    break;
            }
        }
        return null;
    }

    /**
     * Converts a file path to a URL.
     * This method checks if the file exists on the local filesystem and converts it to a file:// URL.
     * Used to load custom holiday definition files from the filesystem.
     *
     * @param filename the absolute or relative path to the file
     * @return the URL representing the file
     * @throws FileNotFoundException if the file does not exist or the URL cannot be formed
     */
    private URL getUrl(String filename) throws FileNotFoundException {
        if (Files.exists(Path.of(filename))) {
            try {
                return (new URI("file:" + filename)).toURL();
            } catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
                throw new FileNotFoundException(e.getMessage());
            }
        } else {
            throw new FileNotFoundException(filename);
        }
    }

    /**
     * Retrieves or creates a HolidayManager for the specified key.
     * This method caches HolidayManager instances to avoid recreating them for repeated requests.
     * The manager key can be either a country code (String) or a URL to a custom holiday definition file.
     *
     * @param managerKey either a country code string or a URL to a holiday configuration file
     * @return the HolidayManager instance for the specified key
     */
    private HolidayManager getHolidayManager(Object managerKey) {
        HolidayManager holidayManager = holidayManagers.get(managerKey);
        if (holidayManager == null) {
            final ManagerParameter parameters;
            if (managerKey instanceof String stringKey) {
                URL urlOverride = bundle
                        .getResource(RESOURCES_ROOT + CalendarPartManagerParameter.getConfigurationFileName(stringKey));
                parameters = urlOverride != null //
                        ? ManagerParameters.create(urlOverride)
                        : ManagerParameters.create(stringKey);
            } else {
                parameters = ManagerParameters.create((URL) managerKey);
            }
            holidayManager = HolidayManager.getInstance(parameters);
            holidayManagers.put(managerKey, holidayManager);
        }
        return holidayManager;
    }

    /**
     * Retrieves a list of holidays within a specified time window.
     * This method queries the HolidayManager for all holidays between the start date and
     * the end date (start date + span), considering the configured country/region/city parameters.
     *
     * @param from the start date of the time window
     * @param span the number of days to look ahead from the start date
     * @param holidayManager the HolidayManager instance to query
     * @return a sorted list of holidays within the time window
     */
    private List<Holiday> getHolidays(ZonedDateTime from, int span, HolidayManager holidayManager) {
        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = from.plusDays(span).toLocalDate();

        Set<Holiday> days = holidayManager.getHolidays(fromDate, toDate, countryParameters.toArray(new String[0]));
        return days.stream().sorted(Comparator.comparing(Holiday::getDate)).toList();
    }

    @Override
    public long getDaysUntil(ZonedDateTime from, String searchedHoliday) {
        return getDaysUntil(from, searchedHoliday, getHolidayManager(country));
    }

    @Override
    public long getDaysUntil(ZonedDateTime from, String searchedHoliday, URL resource) {
        return getDaysUntil(from, searchedHoliday, getHolidayManager(resource));
    }

    @Override
    public long getDaysUntil(ZonedDateTime from, String searchedHoliday, String filename) throws FileNotFoundException {
        return getDaysUntil(from, searchedHoliday, getUrl(filename));
    }

    /**
     * Calculates the number of days until a specified holiday.
     * This method searches for the holiday within a 1-year time window starting from the given date.
     * The search is case-insensitive and matches against the holiday's property key.
     *
     * @param from the start date to calculate from
     * @param searchedHoliday the name/key of the holiday to search for
     * @param holidayManager the HolidayManager instance to query
     * @return the number of days until the holiday, or -1 if the holiday is not found within the year
     */
    private long getDaysUntil(ZonedDateTime from, String searchedHoliday, HolidayManager holidayManager) {
        List<Holiday> sortedHolidays = getHolidays(from, 366, holidayManager);
        Optional<Holiday> result = sortedHolidays.stream()
                .filter(holiday -> searchedHoliday.equalsIgnoreCase(holiday.getPropertiesKey())).findFirst();
        return result.map(holiday -> from.toLocalDate().until(holiday.getDate(), ChronoUnit.DAYS)).orElse(-1L);
    }

    /**
     * Retrieves the key of the first bank holiday within a specified time window.
     * This method searches for the earliest occurring holiday starting from the given date.
     *
     * @param from the start date of the time window
     * @param span the number of days to search ahead
     * @param holidayManager the HolidayManager instance to query
     * @return the property key of the first holiday found, or null if no holiday exists in the time window
     */
    private @Nullable String getFirstBankHolidayKey(ZonedDateTime from, int span, HolidayManager holidayManager) {
        Optional<Holiday> holiday = getHolidays(from, span, holidayManager).stream().findFirst();
        return holiday.map(Holiday::getPropertiesKey).orElse(null);
    }

    @Override
    public boolean isBankHoliday(ZonedDateTime date) {
        return !getHolidays(date, 0, getHolidayManager(country)).isEmpty();
    }

    @Override
    public boolean isBankHoliday(ZonedDateTime date, URL resource) {
        return !getHolidays(date, 0, getHolidayManager(resource)).isEmpty();
    }

    @Override
    public boolean isBankHoliday(ZonedDateTime date, String filename) throws FileNotFoundException {
        return isBankHoliday(date, getUrl(filename));
    }

    @Override
    public boolean isWeekend(ZonedDateTime date) {
        return isInDayset(CONFIG_DAYSET_WEEKEND, date);
    }

    @Override
    public boolean isInDayset(String daysetName, ZonedDateTime date) {
        Set<DayOfWeek> dayset = daysets.get(daysetName);
        if (dayset != null) {
            DayOfWeek dow = date.getDayOfWeek();
            return dayset.contains(dow);
        } else {
            logger.warn("This dayset is not configured : {}", daysetName);
            return false;
        }
    }

    @Override
    public @Nullable String getBankHolidayName(ZonedDateTime date) {
        return getFirstBankHolidayKey(date, 0, getHolidayManager(country));
    }

    @Override
    public @Nullable String getBankHolidayName(ZonedDateTime date, URL resource) {
        return getFirstBankHolidayKey(date, 0, getHolidayManager(resource));
    }

    @Override
    public @Nullable String getBankHolidayName(ZonedDateTime date, String filename) throws FileNotFoundException {
        return getBankHolidayName(date, getUrl(filename));
    }

    @Override
    public @Nullable String getNextBankHoliday(ZonedDateTime from) {
        return getFirstBankHolidayKey(from, 365, getHolidayManager(country));
    }

    @Override
    public @Nullable String getNextBankHoliday(ZonedDateTime from, URL resource) {
        return getFirstBankHolidayKey(from, 365, getHolidayManager(resource));
    }

    @Override
    public @Nullable String getNextBankHoliday(ZonedDateTime from, String filename) throws FileNotFoundException {
        return getNextBankHoliday(from, getUrl(filename));
    }

    /**
     * Adds or updates a dayset definition.
     * A dayset is a collection of days of the week (e.g., weekend = Saturday + Sunday).
     * This method parses the provided values, strips non-alphabetic characters, converts to uppercase,
     * and creates a set of DayOfWeek values.
     *
     * @param setName the name of the dayset (e.g., "weekend", "workdays")
     * @param values an iterable collection of day names (e.g., "SATURDAY", "SUNDAY")
     * @throws IllegalArgumentException if a day name cannot be parsed as a valid DayOfWeek
     */
    private void addDayset(String setName, Iterable<?> values) {
        Set<DayOfWeek> dayset = new HashSet<>();
        for (Object day : values) {
            // fix illegal entries by stripping all non A-Z characters
            String dayString = day.toString().toUpperCase().replaceAll("[^A-Z]", "");
            dayset.add(DayOfWeek.valueOf(dayString));
        }
        daysets.put(setName, dayset);
    }

    /**
     * Parses each entry of a properties list loaded from 'jolliday/descriptions/country_descriptions.properties'. The
     * file has been copied from
     * <a href=
     * "https://github.com/svendiedrichsen/jollyday/blob/master/src/main/resources/descriptions/country_descriptions.properties">https://github.com/svendiedrichsen/jollyday/blob/master/src/main/resources/descriptions/country_descriptions.properties</a>
     * and contains values in the following format - some examples:
     *
     * country.description.at = Austria
     * country.description.au = Australia
     * country.description.au.sa = South Australia
     * country.description.au.tas = Tasmania
     * country.description.au.tas.ho = Hobard Area
     * country.description.au.tas.nh = Non-Hobard Area
     *
     * "at" and "au" are keys of countries
     * "sa" and "tas" are keys of regions inside the country "au"
     * "ho" and "nh" are keys of cities or areas inside the region "tas"
     *
     * @param key key of the property will be parsed
     * @param value value of the property will be used as name
     * @throws IllegalArgumentException if the property could not be parsed properly
     */
    void parseProperty(Object key, Object value) throws IllegalArgumentException {
        final String property = key.toString().replace(PROPERTY_COUNTRY_DESCRIPTION_PREFIX, "");
        final String[] parts = property.split(PROPERTY_COUNTRY_DESCRIPTION_DELIMITER);
        final String name = value.toString();
        final String part;
        final ParameterOption option;
        switch (parts.length) {
            case 1:
                countries.add(new ParameterOption(getValidPart(parts[0]), name));
                break;
            case 2:
                part = getValidPart(parts[0]);
                option = new ParameterOption(getValidPart(parts[1]), name);
                List<ParameterOption> regionsPart = regions.get(part);
                if (regionsPart != null) {
                    regionsPart.add(option);
                } else {
                    final List<ParameterOption> options = new ArrayList<>();
                    options.add(option);
                    regions.put(part, options);
                }
                break;
            case 3:
                part = getValidPart(parts[1]);
                option = new ParameterOption(getValidPart(parts[2]), name);
                List<ParameterOption> citiesPart = cities.get(part);
                if (citiesPart != null) {
                    citiesPart.add(option);
                } else {
                    final List<ParameterOption> options = new ArrayList<>();
                    options.add(option);
                    cities.put(part, options);
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("Unable to parse property '%s = %s'.", key, value));
        }
    }

    /**
     * Validates and normalizes a property part.
     * This method trims whitespace and converts the part to lowercase.
     * Used during parsing of country/region/city property keys.
     *
     * @param part the property part to validate
     * @return the trimmed and lowercased property part
     * @throws IllegalArgumentException if the part is empty after trimming
     */
    private static String getValidPart(String part) {
        final String subject = part.trim();
        if (!subject.isEmpty()) {
            return subject.toLowerCase();
        } else {
            throw new IllegalArgumentException("Unable to parse property - token is empty.");
        }
    }

    @Override
    public @Nullable String getHolidayDescription(@Nullable String holiday) {
        return holiday != null ? ResourceUtil.getHolidayDescription(localeProvider.getLocale(), holiday) : null;
    }
}
