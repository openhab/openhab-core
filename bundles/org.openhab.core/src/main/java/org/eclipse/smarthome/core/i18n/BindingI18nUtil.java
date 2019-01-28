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
package org.eclipse.smarthome.core.i18n;

import java.util.Locale;

import org.osgi.framework.Bundle;

/**
 * The {@link BindingI18nUtil} uses the {@link TranslationProvider} to resolve the
 * localized texts. It automatically infers the key if the default text is not a
 * constant.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class BindingI18nUtil {

    private final TranslationProvider i18nProvider;

    public BindingI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public String getDescription(Bundle bundle, String bindingId, String defaultDescription, Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription, () -> inferKey(bindingId, "description"));

        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    public String getName(Bundle bundle, String bindingId, String defaultLabel, Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferKey(bindingId, "name"));

        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    private String inferKey(String bindingId, String lastSegment) {
        return "binding." + bindingId + "." + lastSegment;
    }

}
