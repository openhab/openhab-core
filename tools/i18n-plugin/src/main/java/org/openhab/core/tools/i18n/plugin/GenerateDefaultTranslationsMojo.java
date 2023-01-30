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

import static java.nio.file.StandardOpenOption.*;
import static org.openhab.core.tools.i18n.plugin.DefaultTranslationsGenerationMode.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescription;

/**
 * Generates the default translations properties file for a bundle based on the XML files in the <code>OH-INF</code>
 * directory.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
@Mojo(name = "generate-default-translations", threadSafe = true)
public class GenerateDefaultTranslationsMojo extends AbstractI18nMojo {

    private static final Set<String> ADDON_TYPES = Set.of("automation", "binding", "io", "persistence", "transform",
            "voice");

    /**
     * The directory where the properties files will be generated
     */
    @Parameter(property = "i18n.target.dir", defaultValue = "${project.basedir}/src/main/resources/OH-INF/i18n")
    private @NonNullByDefault({}) File targetDirectory;

    @Parameter(property = "i18n.generation.mode", defaultValue = "ADD_MISSING_TRANSLATIONS")
    private DefaultTranslationsGenerationMode generationMode = ADD_MISSING_TRANSLATIONS;

    @Override
    public void execute() throws MojoFailureException {
        try {
            if (ohinfExists()) {
                readAddonInfo();

                Path defaultTranslationsPath = ohinfDirectory.toPath()
                        .resolve(Path.of("i18n", propertiesFileName(bundleInfo)));

                if (Files.exists(defaultTranslationsPath)) {
                    if (generationMode == ADD_MISSING_FILES) {
                        getLog().info("Skipped: " + defaultTranslationsPath);
                        return;
                    } else if (generationMode == REGENERATE_FILES) {
                        try {
                            Files.delete(defaultTranslationsPath);
                            getLog().info("Deleted: " + defaultTranslationsPath);
                        } catch (IOException e) {
                            throw new MojoFailureException(
                                    "Failed to delete existing default translations: " + defaultTranslationsPath, e);
                        }
                    }
                }

                String translationsString = generateDefaultTranslations(defaultTranslationsPath);
                if (!translationsString.isBlank()) {
                    writeDefaultTranslations(translationsString);
                }
            }
        } catch (IOException e) {
            throw new MojoFailureException("Failed to read OH-INF XML files", e);
        }
    }

    private String propertiesFileName(BundleInfo bundleInfo) {
        String name = bundleInfo.getAddonId();
        if (name.isEmpty()) {
            Optional<ConfigDescription> optional = bundleInfo.getConfigDescriptions().stream().findFirst();
            if (optional.isPresent()) {
                ConfigDescription configDescription = optional.get();
                String[] uid = configDescription.getUID().toString().split(":");
                if (uid.length > 2 && ADDON_TYPES.contains(uid[1])) {
                    name = uid[2].toLowerCase();
                } else {
                    name = uid[1].toLowerCase();
                }
            }
        }

        if (name.isBlank()) {
            name = "unknown";
        }

        return name + ".properties";
    }

    protected String generateDefaultTranslations(Path defaultTranslationsPath) {
        XmlToTranslationsConverter xmlConverter = new XmlToTranslationsConverter();
        Translations generatedTranslations = xmlConverter.convert(bundleInfo);

        JsonToTranslationsConverter jsonConverter = new JsonToTranslationsConverter();
        jsonConverter.convert(bundleInfo).sections.forEach(generatedTranslations::addSection);

        PropertiesToTranslationsConverter propertiesConverter = new PropertiesToTranslationsConverter(getLog());
        Translations existingTranslations = propertiesConverter.convert(defaultTranslationsPath);

        TranslationsMerger translationsMerger = new TranslationsMerger();
        translationsMerger.merge(generatedTranslations, existingTranslations);

        return generatedTranslations.linesStream().collect(Collectors.joining(System.lineSeparator()));
    }

    private void writeDefaultTranslations(String translationsString) throws MojoFailureException {
        Path translationsPath = targetDirectory.toPath().resolve(propertiesFileName(bundleInfo));

        try {
            Files.createDirectories(translationsPath.getParent());
        } catch (IOException e) {
            throw new MojoFailureException(
                    "Failed to create translations target directory: " + translationsPath.getParent(), e);
        }

        try {
            getLog().info("Writing: " + translationsPath);
            Files.writeString(translationsPath, translationsString, CREATE, TRUNCATE_EXISTING, WRITE);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write generated translations to: " + translationsPath, e);
        }
    }

    void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    void setGenerationMode(DefaultTranslationsGenerationMode generationMode) {
        this.generationMode = generationMode;
    }
}
