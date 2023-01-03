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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JsonToTranslationsConverter}.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class JsonToTranslationsConverterTest {

    @Test
    public void convertModuleTypeInfo() throws IOException {
        BundleInfoReader reader = new BundleInfoReader(new SystemStreamLog());
        BundleInfo bundleInfo = reader.readBundleInfo(Path.of("src/test/resources/acmeweather.bundle/OH-INF"));

        JsonToTranslationsConverter converter = new JsonToTranslationsConverter();
        Translations translations = converter.convert(bundleInfo);

        assertThat(translations.hasTranslations(), is(true));
        assertThat(translations.sections.size(), is(2));
        assertThat(translations.keysStream().count(), is(10L));

        String lines = translations.linesStream().collect(Collectors.joining(System.lineSeparator()));
        assertThat(lines, containsString("acme.WeatherBadTrigger.label = Weather Bad Trigger"));
        assertThat(lines, containsString(
                "module-type.acme.WeatherBadTrigger.config.minimumLevel.description = The minimum level that results in a trigger"));
        assertThat(lines, containsString("module-type.acme.WeatherBadTrigger.output.level.label = Level"));
    }
}
