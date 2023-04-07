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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.openhab.core.tools.i18n.plugin.DefaultTranslationsGenerationMode.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link GenerateDefaultTranslationsMojo}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class GenerateDefaultTranslationsMojoTest {

    public @NonNullByDefault({}) Path tempPath;
    public @NonNullByDefault({}) Path tempI18nPath;

    public static final Path TTS_RESOURCES_PATH = Path.of("src/test/resources/acmetts.bundle");
    public static final Path TTS_I18N_RESOURCES_PATH = TTS_RESOURCES_PATH.resolve("OH-INF/i18n");
    public static final Path TTS_ALL_PROPERTIES_PATH = TTS_I18N_RESOURCES_PATH.resolve("acmetts.properties");
    public static final Path TTS_ALL_DE_PROPERTIES_PATH = TTS_I18N_RESOURCES_PATH.resolve("acmetts_de.properties");
    public static final Path TTS_GENERATED_PROPERTIES_PATH = TTS_I18N_RESOURCES_PATH
            .resolve("acmetts.generated.properties");
    public static final Path TTS_PARTIAL_PROPERTIES_PATH = TTS_I18N_RESOURCES_PATH
            .resolve("acmetts.partial.properties");

    public static final Path WEATHER_RESOURCES_PATH = Path.of("src/test/resources/acmeweather.bundle");
    public static final Path WEATHER_I18N_RESOURCES_PATH = WEATHER_RESOURCES_PATH.resolve("OH-INF/i18n");
    public static final Path WEATHER_ALL_PROPERTIES_PATH = WEATHER_I18N_RESOURCES_PATH
            .resolve("acmeweather.properties");
    public static final Path WEATHER_ALL_DE_PROPERTIES_PATH = WEATHER_I18N_RESOURCES_PATH
            .resolve("acmeweather_de.properties");
    public static final Path WEATHER_GENERATED_PROPERTIES_PATH = WEATHER_I18N_RESOURCES_PATH
            .resolve("acmeweather.generated.properties");
    public static final Path WEATHER_PARTIAL_PROPERTIES_PATH = WEATHER_I18N_RESOURCES_PATH
            .resolve("acmeweather.partial.properties");

    public static final Path INFOLESS_RESOURCES_PATH = Path.of("src/test/resources/infoless.bundle");

    private @NonNullByDefault({}) GenerateDefaultTranslationsMojo mojo;

    private void copyPath(Path src, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
        }
    }

    private void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    private void deleteTempI18nPath() throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(tempI18nPath)) {
            for (Path entry : entries) {
                Files.delete(entry);
            }
        }

        Files.delete(tempI18nPath);
    }

    @BeforeEach
    public void before() throws IOException {
        tempPath = Files.createTempDirectory("i18n-");
        tempI18nPath = tempPath.resolve("OH-INF/i18n");

        mojo = new GenerateDefaultTranslationsMojo();
        mojo.setLog(new SystemStreamLog());
        mojo.setOhinfDirectory(tempPath.resolve("OH-INF").toFile());
        mojo.setTargetDirectory(tempI18nPath.toFile());
    }

    @AfterEach
    public void afterEach() throws IOException {
        Files.walk(tempPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    private void assertSameProperties(Path expectedPath, Path actualPath) throws IOException {
        String expected = Files.readAllLines(expectedPath).stream().collect(Collectors.joining(System.lineSeparator()));
        String actual = Files.readAllLines(actualPath).stream().collect(Collectors.joining(System.lineSeparator()));
        assertThat(expected, equalTo(actual));
    }

    @Test
    public void addMissingAddonTranslationsWithoutI18nPath() throws IOException, MojoFailureException {
        copyPath(WEATHER_RESOURCES_PATH, tempPath);
        deleteTempI18nPath();

        mojo.setGenerationMode(ADD_MISSING_TRANSLATIONS);
        mojo.execute();

        assertSameProperties(WEATHER_GENERATED_PROPERTIES_PATH, tempI18nPath.resolve("acmeweather.properties"));
    }

    @Test
    public void addMissingAddonTranslationsNoChanges() throws IOException, MojoFailureException {
        copyPath(WEATHER_RESOURCES_PATH, tempPath);

        mojo.setGenerationMode(ADD_MISSING_TRANSLATIONS);
        mojo.execute();

        assertSameProperties(WEATHER_ALL_PROPERTIES_PATH, tempI18nPath.resolve("acmeweather.properties"));
        assertSameProperties(WEATHER_ALL_DE_PROPERTIES_PATH, tempI18nPath.resolve("acmeweather_de.properties"));
    }

    @Test
    public void addMissingBindingTranslationsToPartialTranslation() throws IOException, MojoFailureException {
        copyPath(WEATHER_RESOURCES_PATH, tempPath);
        Files.move(tempI18nPath.resolve("acmeweather.partial.properties"),
                tempI18nPath.resolve("acmeweather.properties"), REPLACE_EXISTING);

        mojo.setGenerationMode(ADD_MISSING_TRANSLATIONS);
        mojo.execute();

        assertSameProperties(WEATHER_ALL_PROPERTIES_PATH, tempI18nPath.resolve("acmeweather.properties"));
        assertSameProperties(WEATHER_ALL_DE_PROPERTIES_PATH, tempI18nPath.resolve("acmeweather_de.properties"));
    }

    @Test
    public void skipTranslationsBindingTranslationsGeneratingWithExistingTranslations()
            throws IOException, MojoFailureException {
        copyPath(WEATHER_RESOURCES_PATH, tempPath);
        Files.move(tempI18nPath.resolve("acmeweather.partial.properties"),
                tempI18nPath.resolve("acmeweather.properties"), REPLACE_EXISTING);

        mojo.setGenerationMode(ADD_MISSING_FILES);
        mojo.execute();

        assertSameProperties(WEATHER_PARTIAL_PROPERTIES_PATH, tempI18nPath.resolve("acmeweather.properties"));
        assertSameProperties(WEATHER_ALL_DE_PROPERTIES_PATH, tempI18nPath.resolve("acmeweather_de.properties"));
    }

    @Test
    public void regenerateBindingTranslations() throws IOException, MojoFailureException {
        copyPath(WEATHER_RESOURCES_PATH, tempPath);

        mojo.setGenerationMode(REGENERATE_FILES);
        mojo.execute();

        assertSameProperties(WEATHER_GENERATED_PROPERTIES_PATH, tempI18nPath.resolve("acmeweather.properties"));
        assertSameProperties(WEATHER_ALL_DE_PROPERTIES_PATH, tempI18nPath.resolve("acmeweather_de.properties"));
    }

    @Test
    public void addMissingGenericBundleTranslationsWithoutI18nPath() throws IOException, MojoFailureException {
        copyPath(TTS_RESOURCES_PATH, tempPath);
        deleteTempI18nPath();

        mojo.setGenerationMode(ADD_MISSING_TRANSLATIONS);
        mojo.execute();

        assertSameProperties(TTS_GENERATED_PROPERTIES_PATH, tempI18nPath.resolve("acmetts.properties"));
    }

    @Test
    public void addMissingGenericBundleTranslationsNoChanges() throws IOException, MojoFailureException {
        copyPath(TTS_RESOURCES_PATH, tempPath);

        mojo.setGenerationMode(ADD_MISSING_TRANSLATIONS);
        mojo.execute();

        assertSameProperties(TTS_ALL_PROPERTIES_PATH, tempI18nPath.resolve("acmetts.properties"));
        assertSameProperties(TTS_ALL_DE_PROPERTIES_PATH, tempI18nPath.resolve("acmetts_de.properties"));
    }

    @Test
    public void addMissingGenericBundleTranslationsToPartialTranslation() throws IOException, MojoFailureException {
        copyPath(TTS_RESOURCES_PATH, tempPath);
        Files.move(tempI18nPath.resolve("acmetts.partial.properties"), tempI18nPath.resolve("acmetts.properties"),
                REPLACE_EXISTING);

        mojo.setGenerationMode(ADD_MISSING_TRANSLATIONS);
        mojo.execute();

        assertSameProperties(TTS_ALL_PROPERTIES_PATH, tempI18nPath.resolve("acmetts.properties"));
        assertSameProperties(TTS_ALL_DE_PROPERTIES_PATH, tempI18nPath.resolve("acmetts_de.properties"));
    }

    @Test
    public void skipTranslationsGenericBundleTranslationsGeneratingWithExistingTranslations()
            throws IOException, MojoFailureException {
        copyPath(TTS_RESOURCES_PATH, tempPath);
        Files.move(tempI18nPath.resolve("acmetts.partial.properties"), tempI18nPath.resolve("acmetts.properties"),
                REPLACE_EXISTING);

        mojo.setGenerationMode(ADD_MISSING_FILES);
        mojo.execute();

        assertSameProperties(TTS_PARTIAL_PROPERTIES_PATH, tempI18nPath.resolve("acmetts.properties"));
        assertSameProperties(TTS_ALL_DE_PROPERTIES_PATH, tempI18nPath.resolve("acmetts_de.properties"));
    }

    @Test
    public void regenerateGenericBundleTranslations() throws IOException, MojoFailureException {
        copyPath(TTS_RESOURCES_PATH, tempPath);

        mojo.setGenerationMode(REGENERATE_FILES);
        mojo.execute();

        assertSameProperties(TTS_GENERATED_PROPERTIES_PATH, tempI18nPath.resolve("acmetts.properties"));
        assertSameProperties(TTS_ALL_DE_PROPERTIES_PATH, tempI18nPath.resolve("acmetts_de.properties"));
    }

    @Test
    public void addMissingTranslationsWithoutOhInfPath() throws IOException, MojoFailureException {
        copyPath(INFOLESS_RESOURCES_PATH, tempPath);

        mojo.setGenerationMode(ADD_MISSING_TRANSLATIONS);
        mojo.execute();
    }
}
