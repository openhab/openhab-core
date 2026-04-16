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
package org.openhab.core.model.yaml.internal.pages;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.dto.ConfigDescriptionDTO;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.ui.components.UIComponent;

/**
 * The {@link YamlPageDTO} is a data transfer object used to serialize a UI page in a YAML configuration file.
 * It maps to a {@link org.openhab.core.ui.components.RootUIComponent} in the {@code ui:page} namespace.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@YamlElementName("pages")
public class YamlPageDTO implements YamlElement, Cloneable {

    public String uid;
    public String component;
    public Map<String, Object> config;
    public Map<String, List<UIComponent>> slots;
    public Set<String> tags;
    public ConfigDescriptionDTO props;

    public YamlPageDTO() {
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
        YamlPageDTO copy;
        try {
            copy = (YamlPageDTO) super.clone();
            copy.uid = null;
            return copy;
        } catch (CloneNotSupportedException e) {
            // Will never happen
            return new YamlPageDTO();
        }
    }

    @Override
    public boolean isValid(@Nullable List<@NonNull String> errors, @Nullable List<@NonNull String> warnings) {
        if (uid == null || uid.isBlank()) {
            addToList(errors, "invalid page: uid is missing while mandatory");
            return false;
        }
        if (component == null || component.isBlank()) {
            addToList(errors, "invalid page \"%s\": component is missing while mandatory".formatted(uid));
            return false;
        }
        return true;
    }

    private void addToList(@Nullable List<@NonNull String> list, String value) {
        if (list != null) {
            list.add(value);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, component, config, slots, tags, props);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlPageDTO other = (YamlPageDTO) obj;
        return Objects.equals(uid, other.uid) && Objects.equals(component, other.component)
                && Objects.equals(config, other.config) && Objects.equals(slots, other.slots)
                && Objects.equals(tags, other.tags) && Objects.equals(props, other.props);
    }
}
