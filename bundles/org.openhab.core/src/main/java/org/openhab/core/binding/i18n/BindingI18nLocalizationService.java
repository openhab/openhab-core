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
package org.openhab.core.binding.i18n;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.binding.BindingInfo;
import org.openhab.core.binding.internal.i18n.BindingI18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This OSGi service could be used to localize the binding info using the I18N mechanism of the openHAB
 * framework.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@Component(immediate = true, service = { BindingI18nLocalizationService.class })
@NonNullByDefault
public class BindingI18nLocalizationService {

    private final BindingI18nUtil bindingI18nUtil;

    @Activate
    public BindingI18nLocalizationService(final @Reference TranslationProvider i18nProvider) {
        this.bindingI18nUtil = new BindingI18nUtil(i18nProvider);
    }

    /**
     * Localizes a binding info.
     *
     * @param bundle the bundle the i18n resources are located
     * @param bindingInfo the binding info that should be localized
     * @param locale the locale it should be localized to
     * @return a localized binding info on success, a non-localized one on error (e.g. no translation is found).
     */
    public BindingInfo createLocalizedBindingInfo(Bundle bundle, BindingInfo bindingInfo, @Nullable Locale locale) {
        String bindingInfoUID = bindingInfo.getUID();
        String name = bindingI18nUtil.getName(bundle, bindingInfoUID, bindingInfo.getName(), locale);
        String description = bindingI18nUtil.getDescription(bundle, bindingInfoUID, bindingInfo.getDescription(),
                locale);

        return new BindingInfo(bindingInfoUID, name == null ? bindingInfo.getName() : name,
                description == null ? bindingInfo.getDescription() : description, bindingInfo.getAuthor(),
                bindingInfo.getServiceId(), bindingInfo.getConfigDescriptionURI());
    }
}
