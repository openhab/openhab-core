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

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;

/**
 * The {@link YamlRuleDTO} is a data transfer object used to serialize a sitemap widget rule in a YAML configuration
 * file.
 *
 * @author Mark Herwege - Initial contribution
 */
@YamlElementName("rule")
public class YamlRuleDTO implements YamlElement, Cloneable {

    public String uid;
    public List<Condition> conditions;
    public String argument;

    public YamlRuleDTO() {
    }

    @Override
    public @NonNull String getId() {
        return uid == null ? "" : uid;
    }

    @Override
    public void setId(@NonNull String id) {
        uid = id;
    }

    @Override
    public YamlElement cloneWithoutId() {
        YamlRuleDTO copy;
        try {
            copy = (YamlRuleDTO) super.clone();
            copy.uid = null;
            return copy;
        } catch (CloneNotSupportedException e) {
            // Will never happen
            return new YamlRuleDTO();
        }
    }

    @Override
    public boolean isValid(@Nullable List<@NonNull String> errors, @Nullable List<@NonNull String> warnings) {
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, conditions);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlRuleDTO other = (YamlRuleDTO) obj;
        return Objects.equals(uid, other.uid) && Objects.equals(conditions, other.conditions);
    }

    public class Condition {
        public String item;
        public String condition;
        public String value;
    }
}
