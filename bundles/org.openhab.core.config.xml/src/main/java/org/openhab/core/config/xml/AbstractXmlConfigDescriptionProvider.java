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
package org.openhab.core.config.xml;

import java.net.URI;
import java.util.Collection;
import java.util.Locale;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.i18n.ConfigI18nLocalizationService;
import org.osgi.framework.Bundle;

/**
 * The {@link AbstractXmlConfigDescriptionProvider} is a concrete implementation of the
 * {@link ConfigDescriptionProvider}
 * service interface.
 * <p>
 * This implementation manages any {@link ConfigDescription} objects associated to specific modules. If a specific
 * module disappears, any registered {@link ConfigDescription} objects associated with that module are released.
 *
 * @author Michael Grammling - Initial contribution
 * @author Dennis Nobel - Added locale support
 * @author Alex Tugarev - Extended for pattern and options
 * @author Chris Jackson - Modify to use config parameter builder
 * @author Thomas Höfer - Extended for unit
 * @author Markus Rathgeb - Use ConfigI18nLocalizerService
 * @author Simon Kaufmann - factored out common aspects into {@link AbstractXmlBasedProvider}
 */
public abstract class AbstractXmlConfigDescriptionProvider extends AbstractXmlBasedProvider<URI, ConfigDescription>
        implements ConfigDescriptionProvider {

    @Override
    public synchronized Collection<ConfigDescription> getConfigDescriptions(@Nullable Locale locale) {
        return getAll(locale);
    }

    @Override
    public synchronized @Nullable ConfigDescription getConfigDescription(URI uri, @Nullable Locale locale) {
        return get(uri, locale);
    }

    @Override
    protected @Nullable ConfigDescription localize(Bundle bundle, ConfigDescription configDescription,
            @Nullable Locale locale) {
        ConfigI18nLocalizationService configI18nLocalizerService = getConfigI18nLocalizerService();
        return configI18nLocalizerService.getLocalizedConfigDescription(bundle, configDescription, locale);
    }

    protected abstract ConfigI18nLocalizationService getConfigI18nLocalizerService();

}
