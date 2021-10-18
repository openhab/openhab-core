/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
@Mojo(name = "generate-default-translations", threadSafe = true)
public class GenerateDefaultTranslationsMojo extends AbstractI18nMojo {

    /**
     * The directory where the properties files will be generated
     */
    @Parameter(property = "i18n.target.dir", defaultValue = "${project.basedir}/src/main/resources/OH-INF/i18n")
    private @NonNullByDefault({}) File targetDirectory;

    @Override
    public void execute() throws MojoFailureException {
        try {
            if (ohinfExists()) {
                readAddonInfo();
                String translationsString = generateDefaultTranslations();
                if (!translationsString.isBlank()) {
                    writeDefaultTranslations(translationsString);
                }
            }
        } catch (IOException e) {
            throw new MojoFailureException("Failed to read OH-INF XML files", e);
        }
    }

    protected String generateDefaultTranslations() {
        XmlToTranslationsConverter xmlConverter = new XmlToTranslationsConverter(getLog());
        Translations generatedTranslations = xmlConverter.convert(addonInfo);

        Path defaultTranslationsPath = ohinfDirectory.toPath()
                .resolve(Path.of("i18n", addonInfo.getBindingId() + ".properties"));

        PropertiesToTranslationsConverter propertiesConverter = new PropertiesToTranslationsConverter(getLog());
        Translations existingTranslations = propertiesConverter.convert(defaultTranslationsPath);

        TranslationsMerger translationsMerger = new TranslationsMerger(getLog());
        translationsMerger.merge(generatedTranslations, existingTranslations);

        return generatedTranslations.linesStream().collect(Collectors.joining(System.lineSeparator()));
    }

    private void writeDefaultTranslations(String translationsString) throws MojoFailureException {
        Path translationsPath = targetDirectory.toPath().resolve(addonInfo.getBindingId() + ".properties");

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
}
