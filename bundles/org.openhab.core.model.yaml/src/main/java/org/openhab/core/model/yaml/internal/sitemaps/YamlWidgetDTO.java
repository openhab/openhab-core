/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;

/**
 * The {@link YamlWidgetDTO} is a data transfer object used to serialize a sitemap widget in a YAML configuration file.
 *
 * @author Mark Herwege - Initial contribution
 */
@YamlElementName("widget")
public class YamlWidgetDTO implements YamlElement, Cloneable {

    public String item;
    public String label;
    public String icon;
    public List<YamlRuleDTO> iconRules;
    public Boolean staticIcon;

    public String url;
    public Integer refresh;
    public String encoding;
    public String service;
    public String period;
    public Boolean legend;
    public Boolean forceAsItem;
    public String yAxisDecimalPattern;
    public String interpolation;
    public Integer height;
    public Boolean switchEnabled;
    public Boolean releaseOnly;
    public BigDecimal minValue;
    public BigDecimal maxValue;
    public BigDecimal step;
    public String inputHint;
    public Integer row;
    public Integer column;
    public Boolean stateless;
    public String cmd;
    public String releaseCmd;

    public List<YamlMappingDTO> mappings;

    public List<YamlRuleDTO> labelColor;
    public List<YamlRuleDTO> valueColor;
    public List<YamlRuleDTO> iconColor;
    public List<YamlRuleDTO> visibility;

    public List<Map.Entry<String, YamlWidgetDTO>> widgets;

    public YamlWidgetDTO() {
    }

    @Override
    public @NonNull String getId() {
        return "";
    }

    @Override
    public void setId(@NonNull String id) {
    }

    @Override
    public YamlElement cloneWithoutId() {
        YamlWidgetDTO copy;
        try {
            copy = (YamlWidgetDTO) super.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            // Will never happen
            return new YamlWidgetDTO();
        }
    }

    @Override
    public boolean isValid(@Nullable List<@NonNull String> errors, @Nullable List<@NonNull String> warnings) {
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, label, widgets);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlWidgetDTO other = (YamlWidgetDTO) obj;
        return Objects.equals(item, other.item) && Objects.equals(label, other.label)
                && Objects.equals(widgets, other.widgets);
    }
}
