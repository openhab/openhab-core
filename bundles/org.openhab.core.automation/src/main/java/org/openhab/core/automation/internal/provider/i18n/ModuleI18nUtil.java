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
package org.openhab.core.automation.internal.provider.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;

/**
 * This class is used as utility for resolving the localized {@link Module}s. It automatically infers the key if the
 * default text is not a constant with the assistance of {@link TranslationProvider}.
 *
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
public class ModuleI18nUtil {

    private final TranslationProvider i18nProvider;

    public ModuleI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public <T extends Module> List<T> getLocalizedModules(List<T> modules, Bundle bundle, String uid, String prefix,
            @Nullable Locale locale) {
        List<T> lmodules = new ArrayList<>();
        for (T module : modules) {
            String label = getModuleLabel(bundle, uid, module.getId(), module.getLabel(), prefix, locale);
            String description = getModuleDescription(bundle, uid, module.getId(), module.getDescription(), prefix,
                    locale);
            @Nullable
            T lmodule = createLocalizedModule(module, label, description);
            lmodules.add(lmodule == null ? module : lmodule);
        }
        return lmodules;
    }

    @SuppressWarnings("unchecked")
    private <T extends Module> @Nullable T createLocalizedModule(T module, @Nullable String label,
            @Nullable String description) {
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

    private Trigger createLocalizedTrigger(Trigger module, @Nullable String label, @Nullable String description) {
        return ModuleBuilder.createTrigger(module).withLabel(label).withDescription(description).build();
    }

    private Condition createLocalizedCondition(Condition module, @Nullable String label, @Nullable String description) {
        return ModuleBuilder.createCondition(module).withLabel(label).withDescription(description).build();
    }

    private Action createLocalizedAction(Action module, @Nullable String label, @Nullable String description) {
        return ModuleBuilder.createAction(module).withLabel(label).withDescription(description).build();
    }

    private @Nullable String getModuleLabel(Bundle bundle, String uid, String moduleName, @Nullable String defaultLabel,
            String prefix, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferModuleKey(prefix, uid, moduleName, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    private @Nullable String getModuleDescription(Bundle bundle, String uid, String moduleName,
            @Nullable String defaultDescription, String prefix, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferModuleKey(prefix, uid, moduleName, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    private String inferModuleKey(String prefix, String uid, String moduleName, String lastSegment) {
        return prefix + uid + ".input." + moduleName + "." + lastSegment;
    }
}
