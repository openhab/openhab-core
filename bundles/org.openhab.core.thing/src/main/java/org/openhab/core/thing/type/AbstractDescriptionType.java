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
package org.openhab.core.thing.type;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.thing.UID;

/**
 * The {@link AbstractDescriptionType} class is the base class for a {@link ThingType},
 * a {@link BridgeType} a {@link ChannelType} or a {@link ChannelGroupType}.
 * This class contains only properties and methods accessing them.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractDescriptionType implements Identifiable<UID> {

    private UID uid;
    private String label;
    private @Nullable String description;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param uid the unique identifier which identifies the according type within
     *            the overall system (must neither be null, nor empty)
     * @param label the human readable label for the according type
     *            (must neither be null nor empty)
     * @param description the human readable description for the according type
     *            (could be null or empty)
     * @throws IllegalArgumentException if the UID is null, or the label is null or empty
     */
    public AbstractDescriptionType(UID uid, String label, @Nullable String description)
            throws IllegalArgumentException {
        if ((label == null) || (label.isEmpty())) {
            throw new IllegalArgumentException("The label must neither be null nor empty!");
        }

        this.uid = uid;
        this.label = label;
        this.description = description;
    }

    /**
     * Returns the unique identifier which identifies the according type within the overall system.
     *
     * @return the unique identifier which identifies the according type within
     *         the overall system (neither null, nor empty)
     */
    @Override
    public UID getUID() {
        return this.uid;
    }

    /**
     * Returns the human readable label for the according type.
     *
     * @return the human readable label for the according type (neither null, nor empty)
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Returns the human readable description for the according type.
     *
     * @return the human readable description for the according type (could be null or empty)
     */
    public @Nullable String getDescription() {
        return this.description;
    }

}
