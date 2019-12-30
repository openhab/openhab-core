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
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.Output;
import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;

/**
 * This class is used as utility for resolving the localized {@link ModuleTypes}s. It automatically infers the key if
 * the default text is not a constant with the assistance of {@link TranslationProvider}.
 *
 * @author Ana Dimova - Initial contribution
 * @author Yordan Mihaylov - updates related to api changes
 */
@NonNullByDefault
public class ModuleTypeI18nUtil {

    public static final String MODULE_TYPE = "module-type";

    private final TranslationProvider i18nProvider;

    public ModuleTypeI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public @Nullable String getLocalizedModuleTypeLabel(Bundle bundle, String moduleTypeUID,
            @Nullable String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferModuleTypeKey(moduleTypeUID, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    public @Nullable String getLocalizedModuleTypeDescription(Bundle bundle, String moduleTypeUID,
            @Nullable String defaultDescription, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferModuleTypeKey(moduleTypeUID, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    public List<Input> getLocalizedInputs(@Nullable List<Input> inputs, Bundle bundle, String uid,
            @Nullable Locale locale) {
        List<Input> linputs = new ArrayList<>();
        if (inputs != null) {
            for (Input input : inputs) {
                String inputName = input.getName();
                String ilabel = getInputLabel(bundle, uid, inputName, input.getLabel(), locale);
                String idescription = getInputDescription(bundle, uid, inputName, input.getDescription(), locale);
                linputs.add(new Input(inputName, input.getType(), ilabel, idescription, input.getTags(),
                        input.isRequired(), input.getReference(), input.getDefaultValue()));
            }
        }
        return linputs;
    }

    public List<Output> getLocalizedOutputs(@Nullable List<Output> outputs, Bundle bundle, String uid,
            @Nullable Locale locale) {
        List<Output> loutputs = new ArrayList<>();
        if (outputs != null) {
            for (Output output : outputs) {
                String outputName = output.getName();
                String olabel = getOutputLabel(bundle, uid, outputName, output.getLabel(), locale);
                String odescription = getOutputDescription(bundle, uid, outputName, output.getDescription(), locale);
                loutputs.add(new Output(outputName, output.getType(), olabel, odescription, output.getTags(),
                        output.getReference(), output.getDefaultValue()));
            }
        }
        return loutputs;
    }

    private @Nullable String getInputLabel(Bundle bundle, String moduleTypeUID, String inputName,
            @Nullable String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferInputKey(moduleTypeUID, inputName, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    private @Nullable String getInputDescription(Bundle bundle, String moduleTypeUID, String inputName,
            @Nullable String defaultDescription, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferInputKey(moduleTypeUID, inputName, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    private @Nullable String getOutputLabel(Bundle bundle, String ruleTemplateUID, String outputName,
            String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferOutputKey(ruleTemplateUID, outputName, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    private @Nullable String getOutputDescription(Bundle bundle, String moduleTypeUID, String outputName,
            String defaultDescription, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferOutputKey(moduleTypeUID, outputName, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    private String inferModuleTypeKey(String moduleTypeUID, String lastSegment) {
        return MODULE_TYPE + "." + moduleTypeUID + "." + lastSegment;
    }

    private String inferInputKey(String moduleTypeUID, String inputName, String lastSegment) {
        return MODULE_TYPE + ".input." + moduleTypeUID + ".name." + inputName + "." + lastSegment;
    }

    private String inferOutputKey(String moduleTypeUID, String outputName, String lastSegment) {
        return MODULE_TYPE + ".output." + moduleTypeUID + ".name." + outputName + "." + lastSegment;
    }
}
