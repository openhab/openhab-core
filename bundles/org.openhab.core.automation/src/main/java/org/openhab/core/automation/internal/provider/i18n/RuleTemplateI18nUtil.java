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

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;

/**
 * This class is used as utility for resolving the localized {@link RuleTemplate}s. It automatically infers the key if
 * the default text is not a constant with the assistance of {@link TranslationProvider}.
 *
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
public class RuleTemplateI18nUtil {

    public static final String RULE_TEMPLATE = "rule-template";

    private final TranslationProvider i18nProvider;

    public RuleTemplateI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public @Nullable String getLocalizedRuleTemplateLabel(Bundle bundle, String ruleTemplateUID,
            @Nullable String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferRuleTemplateKey(ruleTemplateUID, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    public @Nullable String getLocalizedRuleTemplateDescription(Bundle bundle, String ruleTemplateUID,
            @Nullable String defaultDescription, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferRuleTemplateKey(ruleTemplateUID, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    private String inferRuleTemplateKey(String ruleTemplateUID, String lastSegment) {
        return RULE_TEMPLATE + "." + ruleTemplateUID + "." + lastSegment;
    }
}
