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
package org.openhab.core.automation.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.Output;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.Configuration;

/**
 * Modules are building components of the {@link Rule}s. Each ModuleImpl is
 * identified by id, which is unique in scope of the {@link Rule}. It also has a {@link ModuleType} which provides
 * meta
 * data of the module. The meta data
 * defines {@link Input}s, {@link Output}s and {@link ConfigDescriptionParameter}s parameters of the {@link ModuleImpl}.
 * <br>
 * Setters of the module don't have immediate effect on the Rule. To apply the
 * changes, they should be set on the {@link Rule} and the Rule has to be
 * updated by RuleManager
 *
 * @author Yordan Mihaylov - Initial contribution
 */
@NonNullByDefault
public abstract class ModuleImpl implements Module {

    /**
     * Id of the ModuleImpl. It is mandatory and unique identifier in scope of the {@link Rule}. The id of the
     * {@link ModuleImpl} is used to identify the module
     * in the {@link Rule}.
     */
    private String id;

    /**
     * The label is a short, user friendly name of the {@link ModuleImpl} defined by
     * this descriptor.
     */
    private @Nullable String label;

    /**
     * The description is a long, user friendly description of the {@link ModuleImpl} defined by this descriptor.
     */
    private @Nullable String description;

    /**
     * Configuration values of the ModuleImpl.
     *
     * @see {@link ConfigDescriptionParameter}.
     */
    private Configuration configuration;

    /**
     * Unique type id of this module.
     */
    private String typeUID;

    /**
     * Constructor of the module.
     *
     * @param id the module id.
     * @param typeUID unique id of the module type.
     * @param configuration configuration values of the module.
     * @param label the label
     * @param description the description
     */
    public ModuleImpl(String id, String typeUID, @Nullable Configuration configuration, @Nullable String label,
            @Nullable String description) {
        this.id = id;
        this.typeUID = typeUID;
        this.configuration = new Configuration(configuration);
        this.label = label;
        this.description = description;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * This method is used for setting the id of the ModuleImpl.
     *
     * @param id of the module.
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTypeUID() {
        return typeUID;
    }

    /**
     * This method is used for setting the typeUID of the ModuleImpl.
     *
     * @param typeUID of the module.
     */
    public void setTypeUID(String typeUID) {
        this.typeUID = typeUID;
    }

    @Override
    public @Nullable String getLabel() {
        return label;
    }

    /**
     * This method is used for setting the label of the ModuleImpl.
     *
     * @param label of the module.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * This method is used for setting the description of the ModuleImpl.
     *
     * @param description of the module.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * This method is used for setting the configuration of the {@link ModuleImpl}.
     *
     * @param configuration new configuration values.
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = new Configuration(configuration);
    }
}
