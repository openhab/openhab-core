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
package org.openhab.core.automation.module.provider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.Output;

/**
 * Wrapper class to collect information about actions modules to be created
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class ModuleInformation {
    private final Method method;
    private final Object actionProvider;
    private final String uid;
    private @Nullable String label;
    private @Nullable String description;
    private @Nullable Visibility visibility;
    private @Nullable Set<String> tags;
    private @Nullable List<Input> inputs;
    private @Nullable List<Output> outputs;
    private @Nullable String configName;
    private @Nullable String thingUID;

    public ModuleInformation(String uid, Object actionProvider, Method m) {
        this.uid = uid;
        this.actionProvider = actionProvider;
        this.method = m;
    }

    public String getUID() {
        return uid;
    }

    public Method getMethod() {
        return method;
    }

    public Object getActionProvider() {
        return actionProvider;
    }

    public @Nullable String getLabel() {
        return label;
    }

    public void setLabel(@Nullable String label) {
        this.label = label;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public @Nullable Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(@Nullable Visibility visibility) {
        this.visibility = visibility;
    }

    public @Nullable List<Input> getInputs() {
        return inputs;
    }

    public void setInputs(@Nullable List<Input> inputs) {
        this.inputs = inputs;
    }

    public @Nullable List<Output> getOutputs() {
        return outputs;
    }

    public void setOutputs(@Nullable List<Output> outputs) {
        this.outputs = outputs;
    }

    public @Nullable String getConfigName() {
        return configName;
    }

    public @Nullable Set<String> getTags() {
        return tags;
    }

    public void setTags(@Nullable Set<String> tags) {
        this.tags = tags;
    }

    public void setConfigName(@Nullable String configName) {
        this.configName = configName;
    }

    public @Nullable String getThingUID() {
        return thingUID;
    }

    public void setThingUID(@Nullable String thingUID) {
        this.thingUID = thingUID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((actionProvider == null) ? 0 : actionProvider.hashCode());
        result = prime * result + ((configName == null) ? 0 : configName.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        result = prime * result + ((thingUID == null) ? 0 : thingUID.hashCode());
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ModuleInformation other = (ModuleInformation) obj;
        if (actionProvider == null) {
            if (other.actionProvider != null) {
                return false;
            }
        } else if (!actionProvider.equals(other.actionProvider)) {
            return false;
        }
        if (configName == null) {
            if (other.configName != null) {
                return false;
            }
        } else if (!configName.equals(other.configName)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        if (method == null) {
            if (other.method != null) {
                return false;
            }
        } else if (!method.equals(other.method)) {
            return false;
        }
        if (thingUID == null) {
            if (other.thingUID != null) {
                return false;
            }
        } else if (!thingUID.equals(other.thingUID)) {
            return false;
        }
        if (uid == null) {
            if (other.uid != null) {
                return false;
            }
        } else if (!uid.equals(other.uid)) {
            return false;
        }
        if (visibility != other.visibility) {
            return false;
        }
        return true;
    }
}
