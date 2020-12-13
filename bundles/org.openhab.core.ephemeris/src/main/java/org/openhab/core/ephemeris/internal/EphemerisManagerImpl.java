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
package org.openhab.core.ephemeris.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.ephemeris.EphemerisManager;
import org.openhab.core.i18n.LocaleProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jollyday.Holiday;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameter;
import de.jollyday.ManagerParameters;
import de.jollyday.util.ResourceUtil;

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

    private static final String JOLLYDAY_COUNTRY_DESCRIPTIONS = "jollyday/descriptions/country_descriptions.properties";
    private static final String PROPERTY_COUNTRY_DESCRIPTION_PREFIX = "country.description.";
    private static final String PROPERTY_COUNTRY_DESCRIPTION_DELIMITER = "\\.";

    final List<ParameterOption> countries = new ArrayList<>();
    final Map<String, List<ParameterOption>> regions = new HashMap<>();
    final Map<String, List<ParameterOption>> cities = new HashMap<>();

    final Map<String, Set<DayOfWeek>> daysets = new HashMap<>();
    private final Map<Object, HolidayManager> holidayManagers = new HashMap<>();
    private final List<String> countryParameters = new ArrayList<>();
    /**
     * Utility for accessing resources.
     */
    private final ResourceUtil resourceUtil = new ResourceUtil();

    private final LocaleProvider localeProvider;

    private @NonNullByDefault({}) String country;
    private @Nullable String region;

    @Activate
    public EphemerisManagerImpl(final @Reference LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;

        final Bundle bundle = FrameworkUtil.getBundle(getClass());
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

    private void sortByLabel(List<ParameterOption> parameterOptions) {
        Collections.sort(parameterOptions,
                (ParameterOption po1, ParameterOption po2) -> po1.getLabel().compareTo(po2.getLabel()));
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        config.entrySet().stream().filter(e -> e.getKey().startsWith(CONFIG_DAYSET_PREFIX)).forEach(e -> {
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
                        logger.warn("Erroneous dayset definition {} : {}", e.getKey(), entry);
                    }
                } else if (entry instanceof Iterable) {
                    addDayset(setName, (Iterable<?>) entry);
                }
            } else {
                logger.warn("Erroneous dayset definition {}", e.getKey());
            }
        });

        if (config.containsKey(CONFIG_COUNTRY)) {
            country = config.get(CONFIG_COUNTRY).toString().toLowerCase();
        } else {
            country = localeProvider.getLocale().getCountry().toLowerCase();
            logger.debug("Using system default country '{}' ", country);
        }

        if (config.containsKey(CONFIG_REGION)) {
            String region = config.get(CONFIG_REGION).toString().toLowerCase();
            countryParameters.add(region);
            this.region = region;
        } else {
            this.region = null;
        }

        if (config.containsKey(CONFIG_CITY)) {
            countryParameters.add(config.get(CONFIG_CITY).toString());
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

    private URL getUrl(String filename) throws FileNotFoundException {
        if (Files.exists(Paths.get(filename))) {
            try {
                return new URL("file:" + filename);
            } catch (MalformedURLException e) {
                throw new FileNotFoundException(e.getMessage());
            }
        } else {
            throw new FileNotFoundException(filename);
        }
    }

    private HolidayManager getHolidayManager(Object managerKey) {
        HolidayManager holidayManager = holidayManagers.get(managerKey);
        if (holidayManager == null) {
            final ManagerParameter parameters = managerKey.getClass() == String.class
                    ? ManagerParameters.create((String) managerKey)
                    : ManagerParameters.create((URL) managerKey);
            holidayManager = HolidayManager.getInstance(parameters);
            holidayManagers.put(managerKey, holidayManager);
        }
        return holidayManager;
    }

    private List<Holiday> getHolidays(ZonedDateTime from, int span, HolidayManager holidayManager) {
        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = from.plusDays(span).toLocalDate();

        Set<Holiday> days = holidayManager.getHolidays(fromDate, toDate, countryParameters.toArray(new String[0]));
        List<Holiday> sortedHolidays = days.stream().sorted(Comparator.comparing(Holiday::getDate))
                .collect(Collectors.toList());
        return sortedHolidays;
    }

    @Override
    public long getDaysUntil(ZonedDateTime from, String searchedHoliday) {
        List<Holiday> sortedHolidays = getHolidays(from, 366, getHolidayManager(country));
        Optional<Holiday> result = sortedHolidays.stream()
                .filter(holiday -> searchedHoliday.equalsIgnoreCase(holiday.getPropertiesKey())).findFirst();
        return result.isPresent() ? from.toLocalDate().until(result.get().getDate(), ChronoUnit.DAYS) : -1;
    }

    @Override
    public long getDaysUntil(ZonedDateTime from, String searchedHoliday, URL resource) {
        List<Holiday> sortedHolidays = getHolidays(from, 366, getHolidayManager(resource));
        Optional<Holiday> result = sortedHolidays.stream()
                .filter(holiday -> searchedHoliday.equalsIgnoreCase(holiday.getPropertiesKey())).findFirst();
        return result.isPresent() ? from.toLocalDate().until(result.get().getDate(), ChronoUnit.DAYS) : -1;
    }

    @Override
    public long getDaysUntil(ZonedDateTime from, String searchedHoliday, String filename) throws FileNotFoundException {
        return getDaysUntil(from, searchedHoliday, getUrl(filename));
    }

    private @Nullable String getFirstBankHolidayKey(ZonedDateTime from, int span, HolidayManager holidayManager) {
        Optional<Holiday> holiday = getHolidays(from, span, holidayManager).stream().findFirst();
        return holiday.map(p -> p.getPropertiesKey()).orElse(null);
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
        if (daysets.containsKey(daysetName)) {
            DayOfWeek dow = date.getDayOfWeek();
            return daysets.get(daysetName).contains(dow);
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

    private void addDayset(String setName, Iterable<?> values) {
        Set<DayOfWeek> dayset = new HashSet<>();
        for (Object day : values) {
            dayset.add(DayOfWeek.valueOf(day.toString().trim().toUpperCase()));
        }
        daysets.put(setName, dayset);
    }

    /**
     * Parses each entry of a properties list loaded from 'jolliday/descriptions/country_descriptions.properties'. The
     * file has been copied from
     * https://github.com/svendiedrichsen/jollyday/blob/master/src/main/resources/descriptions/country_descriptions.properties
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
     * "ho" and "nh" are kess of cities or areas inside the region "tas"
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
                if (regions.containsKey(part)) {
                    regions.get(part).add(option);
                } else {
                    final List<ParameterOption> options = new ArrayList<>();
                    options.add(option);
                    regions.put(part, options);
                }
                break;
            case 3:
                part = getValidPart(parts[1]);
                option = new ParameterOption(getValidPart(parts[2]), name);
                if (cities.containsKey(part)) {
                    cities.get(part).add(option);
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
        return holiday != null ? resourceUtil.getHolidayDescription(localeProvider.getLocale(), holiday) : null;
    }
}
