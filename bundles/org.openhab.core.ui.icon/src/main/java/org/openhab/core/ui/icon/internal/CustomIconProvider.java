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
package org.openhab.core.ui.icon.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigConstants;
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

    private @Nullable File getIconFile(String filename, String iconSetId) {
        File folder = new File(
                ConfigConstants.getConfigFolder() + File.separator + "icons" + File.separator + iconSetId);
        File file = new File(folder, filename);
        return file.exists() ? file : null;
    }

    @Override
    protected @Nullable InputStream getResource(String iconSetId, String resourceName) {
        File file = getIconFile(resourceName, iconSetId);
        if (file != null) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
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
