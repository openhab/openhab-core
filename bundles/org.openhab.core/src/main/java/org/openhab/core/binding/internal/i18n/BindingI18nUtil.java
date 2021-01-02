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
package org.openhab.core.binding.internal.i18n;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;

/**
 * The {@link BindingI18nUtil} uses the {@link TranslationProvider} to resolve the
 * localized texts. It automatically infers the key if the default text is not a
 * constant.
 *
 * @author Dennis Nobel - Initial contribution
 */
@NonNullByDefault
public class BindingI18nUtil {

    private final TranslationProvider i18nProvider;

    public BindingI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public @Nullable String getDescription(Bundle bundle, String bindingId, @Nullable String defaultDescription,
            @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription, () -> inferKey(bindingId, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    public @Nullable String getName(Bundle bundle, String bindingId, String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferKey(bindingId, "name"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    private String inferKey(String bindingId, String lastSegment) {
        return "binding." + bindingId + "." + lastSegment;
    }
}
