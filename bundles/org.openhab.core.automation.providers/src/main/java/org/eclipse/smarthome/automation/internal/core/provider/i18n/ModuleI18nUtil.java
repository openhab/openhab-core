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
package org.eclipse.smarthome.automation.internal.core.provider.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.core.i18n.I18nUtil;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;

/**
 * This class is used as utility for resolving the localized {@link Module}s. It automatically infers the key if the
 * default text is not a constant with the assistance of {@link TranslationProvider}.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class ModuleI18nUtil {

    public static <T extends Module> List<T> getLocalizedModules(TranslationProvider i18nProvider, List<T> modules,
            Bundle bundle, String uid, String prefix, Locale locale) {
        List<T> lmodules = new ArrayList<T>();
        for (T module : modules) {
            String label = getModuleLabel(i18nProvider, bundle, uid, module.getId(), module.getLabel(), prefix, locale);
            String description = getModuleDescription(i18nProvider, bundle, uid, prefix, module.getId(),
                    module.getDescription(), locale);
            lmodules.add(createLocalizedModule(module, label, description));
        }
        return lmodules;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Module> T createLocalizedModule(T module, String label, String description) {
        if (module instanceof Action) {
            return (T) createLocalizedAction((Action) module, label, description);
        }
        if (module instanceof Condition) {
            return (T) createLocalizedCondition((Condition) module, label, description);
        }
        if (module instanceof Trigger) {
            return (T) createLocalizedTrigger((Trigger) module, label, description);
        }
        return null;
    }

    private static Trigger createLocalizedTrigger(Trigger module, String label, String description) {
        return ModuleBuilder.createTrigger(module).withLabel(label).withDescription(description).build();
    }

    private static Condition createLocalizedCondition(Condition module, String label, String description) {
        return ModuleBuilder.createCondition(module).withLabel(label).withDescription(description).build();
    }

    private static Action createLocalizedAction(Action module, String label, String description) {
        return ModuleBuilder.createAction(module).withLabel(label).withDescription(description).build();
    }

    private static String getModuleLabel(TranslationProvider i18nProvider, Bundle bundle, String uid, String moduleName,
            String defaultLabel, String prefix, Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferModuleKey(prefix, uid, moduleName, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    private static String getModuleDescription(TranslationProvider i18nProvider, Bundle bundle, String uid,
            String moduleName, String defaultDescription, String prefix, Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferModuleKey(prefix, uid, moduleName, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    private static String inferModuleKey(String prefix, String uid, String moduleName, String lastSegment) {
        return prefix + uid + ".input." + moduleName + "." + lastSegment;
    }
}
