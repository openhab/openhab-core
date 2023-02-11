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

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link XmlToTranslationsConverter}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class XmlToTranslationsConverterTest {

    @Test
    public void convertBindingInfo() throws IOException {
        BundleInfoReader reader = new BundleInfoReader(new SystemStreamLog());
        BundleInfo bundleInfo = reader.readBundleInfo(Path.of("src/test/resources/acmeweather.bundle/OH-INF"));

        XmlToTranslationsConverter converter = new XmlToTranslationsConverter();
        Translations translations = converter.convert(bundleInfo);

        assertThat(translations.hasTranslations(), is(true));
        assertThat(translations.sections.size(), is(7));
        assertThat(translations.keysStream().count(), is(34L));

        String lines = translations.linesStream().collect(Collectors.joining(System.lineSeparator()));
        assertThat(lines, containsString("# add-on"));
        assertThat(lines, containsString("addon.acmeweather.name = ACME Weather Binding"));
        assertThat(lines, containsString(
                "addon.acmeweather.description = ACME Weather - Current weather and forecasts in your city."));
        assertThat(lines, containsString(
                "channel-group-type.acmeweather.forecast.channel.minTemperature.description = Minimum forecasted temperature in degrees Celsius (metric) or Fahrenheit (imperial)."));
        assertThat(lines, containsString(
                "channel-type.acmeweather.time-stamp.state.pattern = %1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS"));
        assertThat(lines,
                containsString("thing-type.config.acmeweather.weather.language.option.de\\ DE = German (Germany)"));
        assertThat(lines, containsString("channel-type.acmeweather.temperature.state.option.VALUE\\ 1 = My label 1"));
        assertThat(lines, containsString("channel-type.acmeweather.temperature.state.option.VALUE\\:2 = My label 2"));
        assertThat(lines, containsString("channel-type.acmeweather.temperature.state.option.VALUE\\=3 = My label 3"));
    }

    @Test
    public void convertGenericBundleInfo() throws IOException {
        BundleInfoReader reader = new BundleInfoReader(new SystemStreamLog());
        BundleInfo bundleInfo = reader.readBundleInfo(Path.of("src/test/resources/acmetts.bundle/OH-INF"));

        XmlToTranslationsConverter converter = new XmlToTranslationsConverter();
        Translations translations = converter.convert(bundleInfo);

        assertThat(translations.hasTranslations(), is(true));
        assertThat(translations.sections.size(), is(1));
        assertThat(translations.keysStream().count(), is(18L));

        String lines = translations.linesStream().collect(Collectors.joining(System.lineSeparator()));
        assertThat(lines, containsString("voice.config.acmetts.clientId.label = Client Id"));
        assertThat(lines,
                containsString("voice.config.acmetts.clientId.description = ACME Platform OAuth 2.0-Client Id."));
        assertThat(lines, containsString("voice.config.acmetts.pitch.label = Pitch"));
        assertThat(lines, containsString(
                "voice.config.acmetts.pitch.description = Customize the pitch of your selected voice, up to 20 semitones more or less than the default output."));
    }

    @Test
    public void convertPathWithoutAnyInfo() throws IOException {
        BundleInfoReader reader = new BundleInfoReader(new SystemStreamLog());
        BundleInfo bundleInfo = reader.readBundleInfo(Path.of("src/test/resources/infoless.bundle/OH-INF"));

        XmlToTranslationsConverter converter = new XmlToTranslationsConverter();
        Translations translations = converter.convert(bundleInfo);

        assertThat(translations.hasTranslations(), is(false));
        assertThat(translations.keysStream().count(), is(0L));

        String lines = translations.linesStream().collect(Collectors.joining(System.lineSeparator()));
        assertThat(lines, is(emptyString()));
    }
}
