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
package org.eclipse.smarthome.automation.module.core.provider.i18n;

import java.util.Locale;

import org.eclipse.smarthome.automation.type.ModuleType;
import org.osgi.framework.Bundle;

/**
 * Interface for a service that offer i18n functionality
 *
 * @author Stefan Triller - initial contribution
 *
 */
public interface ModuleTypeI18nService {

    /**
     * Builds a {@link ModuleType} with the given {@link Locale}
     *
     * @param defModuleType - the ModuleType as defined
     * @param locale - a Locale into which the type should be translated
     * @param bundle - the bundle containing the localization files
     * @return the localized ModuleType
     */
    ModuleType getModuleTypePerLocale(ModuleType defModuleType, Locale locale, Bundle bundle);

}
