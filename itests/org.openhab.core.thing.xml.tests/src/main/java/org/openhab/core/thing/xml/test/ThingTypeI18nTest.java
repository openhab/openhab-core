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
package org.openhab.core.thing.xml.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeRegistry;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.xml.test.LoadedTestBundle.StuffAddition;

/**
 * @author Henning Treu - Initial contribution
 */
public class ThingTypeI18nTest extends JavaOSGiTest {

    private LoadedTestBundle loadedTestBundle() throws Exception {
        return new LoadedTestBundle("yahooweather.bundle", bundleContext, this::getService,
                new StuffAddition().thingTypes(2));
    }

    private ThingTypeProvider thingTypeProvider;
    private ChannelTypeRegistry channelTypeRegistry;
    private ChannelGroupTypeRegistry channelGroupTypeRegistry;

    @Before
    public void setUp() {
        thingTypeProvider = getService(ThingTypeProvider.class);
        assertNotNull(thingTypeProvider);

        channelTypeRegistry = getService(ChannelTypeRegistry.class);
        assertNotNull(channelTypeRegistry);

        channelGroupTypeRegistry = getService(ChannelGroupTypeRegistry.class);
        assertNotNull(channelGroupTypeRegistry);
    }

    @Test
    public void thingTypeShouldBeLocalized() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
            Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(Locale.GERMAN);

            ThingType weatherType = thingTypes.stream().filter(it -> it.toString().equals("yahooweather:weather"))
                    .findFirst().get();
            assertNotNull(weatherType);

            assertThat(weatherType.getLabel(), is("Wetterinformation"));
            assertThat(weatherType.getDescription(),
                    is("Stellt verschiedene Wetterdaten vom Yahoo Wetterdienst bereit"));
        }
    }

    @Test
    public void channelGroupTypeShouldBeLocalized() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
            Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(Locale.GERMAN);

            ThingType weatherGroupType = thingTypes.stream()
                    .filter(it -> it.toString().equals("yahooweather:weather-with-group")).findFirst().get();
            assertNotNull(weatherGroupType);

            ChannelGroupType channelGroupType = channelGroupTypeRegistry.getChannelGroupType(
                    weatherGroupType.getChannelGroupDefinitions().get(0).getTypeUID(), Locale.GERMAN);
            assertNotNull(channelGroupType);

            assertThat(channelGroupType.getLabel(), is("Wetterinformation mit Gruppe"));
            assertThat(channelGroupType.getDescription(), is("Wetterinformation mit Gruppe Beschreibung."));
        }
    }

    @Test
    public void channelGroupsShouldBeLocalized() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
            Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(Locale.GERMAN);

            ThingType weatherGroupType = thingTypes.stream()
                    .filter(it -> it.toString().equals("yahooweather:weather-with-group")).findFirst().get();
            assertNotNull(weatherGroupType);
            assertThat(weatherGroupType.getChannelGroupDefinitions().size(), is(2));

            ChannelGroupDefinition forecastTodayChannelGroupDefinition = weatherGroupType.getChannelGroupDefinitions()
                    .stream().filter(it -> it.getId().equals("forecastToday")).findFirst().get();
            assertNotNull(forecastTodayChannelGroupDefinition);

            assertThat(forecastTodayChannelGroupDefinition.getLabel(), is("Wettervorhersage heute"));
            assertThat(forecastTodayChannelGroupDefinition.getDescription(),
                    is("Wettervorhersage für den heutigen Tag."));

            ChannelGroupDefinition forecastTomorrowChannelGroupDefinition = weatherGroupType
                    .getChannelGroupDefinitions().stream().filter(it -> it.getId().equals("forecastTomorrow"))
                    .findFirst().get();
            assertNotNull(forecastTomorrowChannelGroupDefinition);

            assertThat(forecastTomorrowChannelGroupDefinition.getLabel(), is("Wettervorhersage morgen"));
            assertThat(forecastTomorrowChannelGroupDefinition.getDescription(),
                    is("Wettervorhersage für den morgigen Tag."));
        }
    }

    @Test
    public void channelsInGroupTypeShouldBeLocalized() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
            Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(Locale.GERMAN);

            ThingType weatherGroupType = thingTypes.stream()
                    .filter(it -> it.toString().equals("yahooweather:weather-with-group")).findFirst().get();
            assertNotNull(weatherGroupType);
            assertEquals(2, weatherGroupType.getChannelGroupDefinitions().size());

            ChannelGroupDefinition forecastTodayChannelGroupDefinition = weatherGroupType.getChannelGroupDefinitions()
                    .stream().filter(it -> it.getId().equals("forecastToday")).findFirst().get();
            assertNotNull(forecastTodayChannelGroupDefinition);

            ChannelGroupType forecastTodayChannelGroupType = channelGroupTypeRegistry
                    .getChannelGroupType(forecastTodayChannelGroupDefinition.getTypeUID(), Locale.GERMAN);
            assertNotNull(forecastTodayChannelGroupType);
            assertEquals(3, forecastTodayChannelGroupType.getChannelDefinitions().size());

            ChannelDefinition temperatureChannelDefinition = forecastTodayChannelGroupType.getChannelDefinitions()
                    .stream().filter(it -> it.getId().equals("temperature")).findFirst().get();
            assertNotNull(temperatureChannelDefinition);

            assertEquals("Temperatur", temperatureChannelDefinition.getLabel());
            assertEquals("Temperatur in Grad Celsius (Metrisch) oder Fahrenheit (Imperial).",
                    temperatureChannelDefinition.getDescription());

            ChannelDefinition minTemperatureChannelDefinition = forecastTodayChannelGroupType.getChannelDefinitions()
                    .stream().filter(it -> it.getId().equals("minTemperature")).findFirst().get();
            assertNotNull(minTemperatureChannelDefinition);

            assertEquals("Min. Temperatur", minTemperatureChannelDefinition.getLabel());
            assertEquals("Minimale vorhergesagte Temperatur in Grad Celsius (Metrisch) oder Fahrenheit (Imperial).",
                    minTemperatureChannelDefinition.getDescription());

            ChannelDefinition maxTemperatureChannelDefinition = forecastTodayChannelGroupType.getChannelDefinitions()
                    .stream().filter(it -> it.getId().equals("maxTemperature")).findFirst().get();
            assertNotNull(maxTemperatureChannelDefinition);

            assertEquals("Max. Temperatur", maxTemperatureChannelDefinition.getLabel());
            assertEquals("Maximale vorhergesagte Temperatur in Grad Celsius (Metrisch) oder Fahrenheit (Imperial).",
                    maxTemperatureChannelDefinition.getDescription());
        }
    }

    @Test
    public void channelTypeShouldBeLocalized() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
            Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(Locale.GERMAN);

            ThingType weatherType = thingTypes.stream().filter(it -> it.toString().equals("yahooweather:weather"))
                    .findFirst().get();
            assertNotNull(weatherType);
            assertThat(weatherType.getChannelDefinitions().size(), is(2));

            ChannelType temperatureChannelType = channelTypeRegistry.getChannelType(weatherType.getChannelDefinitions()
                    .stream().filter(it -> it.getId().equals("temperature")).findFirst().get().getChannelTypeUID(),
                    Locale.GERMAN);
            assertNotNull(temperatureChannelType);

            assertThat(temperatureChannelType.getLabel(), is("Temperatur"));
            assertThat(temperatureChannelType.getDescription(),
                    is("Temperatur in Grad Celsius (Metrisch) oder Fahrenheit (Imperial)."));
            assertThat(temperatureChannelType.getState().getPattern(), is("%d Grad Celsius"));
            assertThat(temperatureChannelType.getState().getOptions().get(0).getLabel(), is("Mein String"));
        }
    }

    @Test
    public void channelsShouldBeLocalized() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
            Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(Locale.GERMAN);

            ThingType weatherType = thingTypes.stream().filter(it -> it.toString().equals("yahooweather:weather"))
                    .findFirst().get();
            assertNotNull(weatherType);
            assertThat(weatherType.getChannelDefinitions().size(), is(2));

            ChannelDefinition temperatureChannelDefinition = weatherType.getChannelDefinitions().stream()
                    .filter(it -> it.getId().equals("temperature")).findFirst().get();
            assertNotNull(temperatureChannelDefinition);

            assertThat(temperatureChannelDefinition.getLabel(), is("Temperatur"));
            assertThat(temperatureChannelDefinition.getDescription(),
                    is("Temperatur in Grad Celsius (Metrisch) oder Fahrenheit (Imperial)."));

            ChannelDefinition minTemperatureChannelDefinition = weatherType.getChannelDefinitions().stream()
                    .filter(it -> it.getId().equals("minTemperature")).findFirst().get();
            assertNotNull(minTemperatureChannelDefinition);

            assertThat(minTemperatureChannelDefinition.getLabel(), is("Min. Temperatur"));
            assertThat(minTemperatureChannelDefinition.getDescription(),
                    is("Minimale Temperatur in Grad Celsius (Metrisch) oder Fahrenheit (Imperial)."));
        }
    }
}
