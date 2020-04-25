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
package org.openhab.core.automation.template;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Visibility;
import org.openhab.core.common.registry.Identifiable;

/**
 * The templates define types of shared, ready to use definitions of automation objects, which
 * can be instantiated and configured to produce automation instances. Each Template has a unique identifier (UID).
 * <p>
 * The {@link Template}s can be used by any creator of automation objects, but they can be modified only by its owner.
 * <p>
 * Templates can have {@code tags} - non-hierarchical keywords or terms for describing them.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 */
@NonNullByDefault
public interface Template extends Identifiable<String> {

    /**
     * Gets the unique identifier (UID) of a Template.
     *
     * @return the identifier of the Template.
     */
    @Override
    public String getUID();

    /**
     * Gets the assigned tags to a Template. The templates can have {@code tags} - non-hierarchical keywords or terms
     * for describing them. The tags are used to filter the templates.
     *
     * @return the tags assigned to the template.
     */
    public Set<String> getTags();

    /**
     * Gets the label of a Template. The label is a short, human-readable description of the Template defined by its
     * creator.
     *
     * @return the label of the Template.
     */
    public @Nullable String getLabel();

    /**
     * Gets the description of a Template. The description is a detailed, human-understandable description of the
     * purpose of a Template, defined by its creator.
     *
     * @return the description of the Template.
     */
    public @Nullable String getDescription();

    /**
     * Shows the visibility of a Template.
     *
     * @return the visibility of the Template.
     */
    public Visibility getVisibility();
}
