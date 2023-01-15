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
package org.openhab.core.tools.i18n.plugin;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PropertiesToTranslationsConverter}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class PropertiesToTranslationsConverterTest {

    @Test
    public void readBindingInfo() {
        PropertiesToTranslationsConverter converter = new PropertiesToTranslationsConverter(new SystemStreamLog());
        Translations translations = converter
                .convert(Path.of("src/test/resources/acmeweather.bundle/OH-INF/i18n/acmeweather.properties"));

        assertThat(translations.hasTranslations(), is(true));
        assertThat(translations.sections.size(), is(8));
        assertThat(translations.keysStream().count(), is(44L));

        String lines = translations.linesStream().collect(Collectors.joining(System.lineSeparator()));
        assertThat(lines, containsString("# add-on"));
        assertThat(lines, containsString("addon.acmeweather.name = ACME Weather Binding"));
        assertThat(lines, containsString(
                "addon.acmeweather.description = ACME Weather - Current weather and forecasts in your city."));
        assertThat(lines, containsString(
                "channel-group-type.acmeweather.forecast.channel.minTemperature.description = Minimum forecasted temperature in degrees Celsius (metric) or Fahrenheit (imperial)."));
        assertThat(lines, containsString(
                "channel-type.acmeweather.time-stamp.state.pattern = %1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS"));
        assertThat(lines, containsString("CUSTOM_KEY = Provides various weather data from the ACME weather service"));
    }

    @Test
    public void readGenericBundleInfo() {
        PropertiesToTranslationsConverter converter = new PropertiesToTranslationsConverter(new SystemStreamLog());
        Translations translations = converter
                .convert(Path.of("src/test/resources/acmetts.bundle/OH-INF/i18n/acmetts.properties"));

        assertThat(translations.hasTranslations(), is(true));
        assertThat(translations.sections.size(), is(2));
        assertThat(translations.keysStream().count(), is(19L));

        String lines = translations.linesStream().collect(Collectors.joining(System.lineSeparator()));

        assertThat(lines, containsString("voice.config.acmetts.clientId.label = Client Id"));
        assertThat(lines,
                containsString("voice.config.acmetts.clientId.description = ACME Platform OAuth 2.0-Client Id."));
        assertThat(lines, containsString("voice.config.acmetts.pitch.label = Pitch"));
        assertThat(lines, containsString(
                "voice.config.acmetts.pitch.description = Customize the pitch of your selected voice, up to 20 semitones more or less than the default output."));
    }

    @Test
    public void readPathWithoutAnyInfo() {
        PropertiesToTranslationsConverter converter = new PropertiesToTranslationsConverter(new SystemStreamLog());
        Translations translations = converter
                .convert(Path.of("src/test/resources/infoless.bundle/OH-INF/i18n/nonexisting.properties"));

        assertThat(translations.hasTranslations(), is(false));
        assertThat(translations.keysStream().count(), is(0L));

        String lines = translations.linesStream().collect(Collectors.joining(System.lineSeparator()));
        assertThat(lines, is(emptyString()));
    }
}
