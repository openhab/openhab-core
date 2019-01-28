/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.ui.icon.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.ui.icon.AbstractResourceIconProvider;
import org.eclipse.smarthome.ui.icon.IconProvider;
import org.eclipse.smarthome.ui.icon.IconSet;
import org.osgi.service.component.annotations.Component;

/**
 * The custom icon provider supports custom icons in the configurations/icons
 * folder. If a custom icon is found, it will be used over the standard system icon.
 *
 * @author Kai Kreuzer - Initial contribution
 *
 */
@Component(immediate = true, service = {IconProvider.class} )
public class CustomIconProvider extends AbstractResourceIconProvider {

    private File getIconFile(String filename, String iconSetId) {
        File folder = new File(
                ConfigConstants.getConfigFolder() + File.separator + "icons" + File.separator + iconSetId);
        File file = new File(folder, filename);
        if (file.exists()) {
            return file;
        } else {
            return null;
        }
    }

    @Override
    protected InputStream getResource(String iconSetId, String resourceName) {
        File file = getIconFile(resourceName, iconSetId);
        if (file != null) {
            try {
                FileInputStream is = new FileInputStream(file);
                return is;
            } catch (FileNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    protected boolean hasResource(String iconSetId, String resourceName) {
        File file = getIconFile(resourceName, iconSetId);
        return file != null;
    }

    @Override
    public Set<IconSet> getIconSets(Locale locale) {
        return Collections.emptySet();
    }

    @Override
    protected Integer getPriority() {
        return 5;
    }
}
