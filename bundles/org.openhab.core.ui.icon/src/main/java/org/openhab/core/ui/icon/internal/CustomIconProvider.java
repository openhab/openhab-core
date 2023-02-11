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
package org.openhab.core.ui.icon.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.ui.icon.AbstractResourceIconProvider;
import org.openhab.core.ui.icon.IconProvider;
import org.openhab.core.ui.icon.IconSet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The custom icon provider supports custom icons in the configurations/icons
 * folder. If a custom icon is found, it will be used over the standard system icon.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(immediate = true, service = { IconProvider.class })
@NonNullByDefault
public class CustomIconProvider extends AbstractResourceIconProvider {

    @Activate
    public CustomIconProvider(final @Reference TranslationProvider i18nProvider) {
        super(i18nProvider);
    }

    private @Nullable Path getIconFile(String filename, String iconSetId) {
        Path folder = Path.of(OpenHAB.getConfigFolder(), "icons", iconSetId);
        try (Stream<Path> stream = Files.walk(folder, FileVisitOption.FOLLOW_LINKS)) {
            return stream.filter(file -> !Files.isDirectory(file) && filename.equals(file.getFileName().toString()))
                    .findAny().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected @Nullable InputStream getResource(String iconSetId, String resourceName) {
        Path file = getIconFile(resourceName, iconSetId);
        if (file != null) {
            try {
                return Files.newInputStream(file);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    protected boolean hasResource(String iconSetId, String resourceName) {
        return getIconFile(resourceName, iconSetId) != null;
    }

    @Override
    public Set<IconSet> getIconSets(@Nullable Locale locale) {
        return Collections.emptySet();
    }

    @Override
    protected Integer getPriority() {
        return 5;
    }
}
