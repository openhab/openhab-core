/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

import org.eclipse.smarthome.core.i18n.I18nUtil;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.openhab.core.automation.template.RuleTemplate;
import org.osgi.framework.Bundle;

/**
 * This class is used as utility for resolving the localized {@link RuleTemplate}s. It automatically infers the key if
 * the default text is not a constant with the assistance of {@link TranslationProvider}.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class RuleTemplateI18nUtil {

    public static final String RULE_TEMPLATE = "rule-template";

    public static String getLocalizedRuleTemplateLabel(TranslationProvider i18nProvider, Bundle bundle,
            String ruleTemplateUID, String defaultLabel, Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferRuleTemplateKey(ruleTemplateUID, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    public static String getLocalizedRuleTemplateDescription(TranslationProvider i18nProvider, Bundle bundle,
            String ruleTemplateUID, String defaultDescription, Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferRuleTemplateKey(ruleTemplateUID, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    private static String inferRuleTemplateKey(String ruleTemplateUID, String lastSegment) {
        return RULE_TEMPLATE + "." + ruleTemplateUID + "." + lastSegment;
    }

}
