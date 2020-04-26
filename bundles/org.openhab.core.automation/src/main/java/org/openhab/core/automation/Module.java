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
package org.openhab.core.automation;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.Output;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.Configuration;

/**
 * This interface represents automation {@code Modules} which are building components of the {@link Rule}s.
 * <p>
 * Each module is identified by id, which is unique in scope of the {@link Rule}.
 * <p>
 * Each module has a {@link ModuleType} which provides meta data of the module. The meta data defines {@link Input}s,
 * {@link Output}s and {@link ConfigDescriptionParameter}s which are the building elements of the {@link Module}.
 * <br>
 * Setters of the module don't have immediate effect on the Rule. To apply the changes, the Module should be set on the
 * {@link Rule} and the Rule has to be updated in {@link RuleRegistry} by invoking {@code update} method.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface Module {

    /**
     * Gets the {@link Module}'s unique identifier in the scope of the rule in which this module belongs. The identifier
     * of the {@link Module} is used to identify it when other rule's module refers it as input.
     *
     * @return the {@link Module}'s unique identifier in the scope of the rule in which this module belongs.
     */
    String getId();

    /**
     * Gets the module type unique identifier which is a reference to the corresponding {@link ModuleType} that
     * describes this module. The {@link ModuleType} contains {@link Input}s, {@link Output}s and
     * {@link ConfigDescriptionParameter}s of this module.
     *
     * @return the {@link ModuleType} unique identifier.
     */
    String getTypeUID();

    /**
     * Gets the label of the {@link Module}. The label is user understandable name of the Module.
     *
     * @return the label of the module or {@code null} if not specified.
     */
    @Nullable
    String getLabel();

    /**
     * Gets the description of the {@link Module}. The description is a detailed, human understandable description of
     * the Module.
     *
     * @return the detailed description of the module or {@code null} if not specified.
     */
    @Nullable
    String getDescription();

    /**
     * Gets the configuration values of the {@link Module}.
     *
     * @return the current configuration values of the {@link Module}.
     */
    Configuration getConfiguration();
}
