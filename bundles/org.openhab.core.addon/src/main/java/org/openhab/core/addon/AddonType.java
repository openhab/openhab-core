/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.addon;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class defines an add-on type.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class AddonType {

    public static final AddonType AUTOMATION = new AddonType("automation", "Automation");
    public static final AddonType BINDING = new AddonType("binding", "Bindings");
    public static final AddonType MISC = new AddonType("misc", "Misc");
    public static final AddonType PERSISTENCE = new AddonType("persistence", "Persistence");
    public static final AddonType TRANSFORMATION = new AddonType("transformation", "Transformations");
    public static final AddonType UI = new AddonType("ui", "User Interfaces");
    public static final AddonType VOICE = new AddonType("voice", "Voice");

    public static final List<AddonType> DEFAULT_TYPES = List.of(AUTOMATION, BINDING, MISC, PERSISTENCE, TRANSFORMATION,
            UI, VOICE);

    private final String id;
    private final String label;

    /**
     * Creates a new type instance with the given id and label
     *
     * @param id
     * @param label
     */
    public AddonType(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * The id of the type
     */
    public String getId() {
        return id;
    }

    /**
     * The label of the type to be used for headers (likely to be plural form)
     */
    public String getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        return id.equals(((AddonType) obj).id);
    }
}
