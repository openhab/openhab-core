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
package org.openhab.core.model.yaml.internal.uicomponents;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.internal.config.YamlConfigDescriptionDTO;
import org.openhab.core.model.yaml.internal.util.FlexibleDateDeserializer;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponent;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * The {@link AbstractYamlRootUIComponentDTO} is a data transfer object used to serialize UI components in a YAML
 * configuration file. It maps to a {@link RootUIComponent}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public abstract class AbstractYamlRootUIComponentDTO implements YamlElement, Cloneable {

    public String uid;
    public String component;
    public Map<String, Object> config;
    public Map<String, List<UIComponent>> slots;
    public Set<String> tags;
    public YamlConfigDescriptionDTO props;
    @JsonDeserialize(using = FlexibleDateDeserializer.class)
    public @Nullable Date timestamp;

    @Override
    public @NonNull String getId() {
        return uid == null ? "" : uid;
    }

    @Override
    public void setId(@NonNull String id) {
        uid = id;
    }

    /**
     * @return The type of UI component, e.g. {@code widget} or {@code page}.
     */
    public abstract String getUIComponentType();

    @Override
    public YamlElement cloneWithoutId() {
        AbstractYamlRootUIComponentDTO copy;
        try {
            copy = (AbstractYamlRootUIComponentDTO) super.clone();
            copy.uid = null;
            return copy;
        } catch (CloneNotSupportedException e) {
            // Will never happen
            throw new UnsupportedOperationException(getClass().getSimpleName() + " doesn't support clone()", e);
        }
    }

    @Override
    public boolean isValid(@Nullable List<@NonNull String> errors, @Nullable List<@NonNull String> warnings) {
        if (uid == null || uid.isBlank()) {
            addToList(errors, "invalid " + getUIComponentType() + ": uid is missing while mandatory");
            return false;
        }
        if (component == null || component.isBlank()) {
            addToList(errors, String.format(Locale.ROOT, "invalid %s \"%s\": component is missing while mandatory",
                    getUIComponentType(), uid));
            return false;
        }
        return true;
    }

    protected void addToList(@Nullable List<@NonNull String> list, String value) {
        if (list != null) {
            list.add(value);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, component, config, props, slots, tags, timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractYamlRootUIComponentDTO other = (AbstractYamlRootUIComponentDTO) obj;
        return Objects.equals(component, other.component) && Objects.equals(config, other.config)
                && Objects.equals(props, other.props) && Objects.equals(slots, other.slots)
                && Objects.equals(tags, other.tags) && Objects.equals(timestamp, other.timestamp)
                && Objects.equals(uid, other.uid);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
        if (uid != null) {
            builder.append("uid=").append(uid).append(", ");
        }
        if (component != null) {
            builder.append("component=").append(component).append(", ");
        }
        if (props != null) {
            builder.append("props=").append(props).append(", ");
        }
        if (config != null) {
            builder.append("config=").append(config).append(", ");
        }
        if (slots != null) {
            builder.append("slots=").append(slots).append(", ");
        }
        if (tags != null) {
            builder.append("tags=").append(tags).append(", ");
        }
        if (timestamp != null) {
            builder.append("timestamp=").append(timestamp);
        }
        builder.append("]");
        return builder.toString();
    }
}
