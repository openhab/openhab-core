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
package org.openhab.core.automation.module.provider.i18n;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.type.ModuleType;
import org.osgi.framework.Bundle;

/**
 * Interface for a service that offer i18n functionality
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public interface ModuleTypeI18nService {

    /**
     * Builds a {@link ModuleType} with the given {@link Locale}
     *
     * @param defModuleType the ModuleType as defined
     * @param locale a Locale into which the type should be translated
     * @param bundle the bundle containing the localization files
     * @return the localized ModuleType
     */
    @Nullable
    ModuleType getModuleTypePerLocale(@Nullable ModuleType defModuleType, @Nullable Locale locale, Bundle bundle);
}
