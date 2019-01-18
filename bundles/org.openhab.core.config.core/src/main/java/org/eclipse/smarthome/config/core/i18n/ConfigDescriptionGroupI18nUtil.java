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
package org.eclipse.smarthome.config.core.i18n;

import java.net.URI;
import java.util.Locale;

import org.eclipse.smarthome.core.i18n.I18nUtil;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;

/**
 * The {@link ConfigDescriptionGroupI18nUtil} uses the {@link TranslationProvider} to
 * resolve the localized texts in the configuration parameter groups. It automatically infers the key if the default
 * text is not a constant.
 *
 * @author Chris Jackson - Initial contribution
 */
public class ConfigDescriptionGroupI18nUtil {

    private final TranslationProvider i18nProvider;

    public ConfigDescriptionGroupI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public String getGroupDescription(Bundle bundle, URI configDescriptionURI, String groupName,
            String defaultDescription, Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferKey(configDescriptionURI, groupName, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    public String getGroupLabel(Bundle bundle, URI configDescriptionURI, String groupName, String defaultLabel,
            Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferKey(configDescriptionURI, groupName, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    private String inferKey(URI configDescriptionURI, String groupName, String lastSegment) {
        String uri = configDescriptionURI.getSchemeSpecificPart().replace(":", ".");
        return configDescriptionURI.getScheme() + ".config." + uri + ".group." + groupName + "." + lastSegment;
    }
}
