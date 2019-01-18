/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.module.core.provider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.automation.type.Input;
import org.eclipse.smarthome.automation.type.Output;

/**
 * Wrapper class to collect information about actions modules to be created
 *
 * @author Stefan Triller - initial contribution
 *
 */
public class ModuleInformation {
    private final Method method;
    private final Object actionProvider;
    private final String uid;
    private String label;
    private String description;
    private Visibility visibility;
    private Set<String> tags;
    private List<Input> inputs;
    private List<Output> outputs;
    private String configName;
    private String thingUID;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public List<Input> getInputs() {
        return inputs;
    }

    public void setInputs(List<Input> inputs) {
        this.inputs = inputs;
    }

    public List<Output> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Output> outputs) {
        this.outputs = outputs;
    }

    public String getConfigName() {
        return configName;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getThingUID() {
        return thingUID;
    }

    public void setThingUID(String thingUID) {
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
    public boolean equals(Object obj) {
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
