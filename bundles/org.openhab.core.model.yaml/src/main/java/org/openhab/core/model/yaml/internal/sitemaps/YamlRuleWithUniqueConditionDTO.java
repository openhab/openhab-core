/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.sitemaps;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a data transfer object that is used to serialize a sitemap rule with an unique condition.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlRuleWithUniqueConditionDTO extends YamlConditionDTO {

    public String value;

    public YamlRuleWithUniqueConditionDTO() {
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, item, operator, argument);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlRuleWithUniqueConditionDTO other = (YamlRuleWithUniqueConditionDTO) obj;
        return Objects.equals(value, other.value) && Objects.equals(item, other.item)
                && Objects.equals(operator, other.operator) && Objects.equals(argument, other.argument);
    }
}
