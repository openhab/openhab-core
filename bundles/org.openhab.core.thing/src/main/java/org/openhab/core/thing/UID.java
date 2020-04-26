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
package org.openhab.core.thing;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.AbstractUID;

/**
 * Base class for binding related unique identifiers within the SmartHome framework.
 * <p>
 * A UID must always start with a binding ID.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Oliver Libutzki - Added possibility to define UIDs with variable amount of segments
 * @author Jochen Hiller - Bugfix 455434: added default constructor, object is now mutable
 */
@NonNullByDefault
public abstract class UID extends AbstractUID {

    /**
     * For reflection only.
     * Constructor must be public, otherwise it cannot be called by subclasses from another package.
     */
    public UID() {
        super();
    }

    /**
     * Parses a UID for a given string. The UID must be in the format
     * 'bindingId:segment:segment:...'.
     *
     * @param uid uid in form a string (must not be null)
     */
    public UID(String uid) {
        super(uid);
    }

    /**
     * Creates a UID for list of segments.
     *
     * @param segments segments (must not be null)
     */
    public UID(String... segments) {
        super(segments);
    }

    /**
     * Creates a UID for list of segments.
     *
     * @param segments segments (must not be null)
     */
    protected UID(List<String> segments) {
        super(segments);
    }

    /**
     * Returns the binding id.
     *
     * @return binding id
     */
    public String getBindingId() {
        return getSegment(0);
    }

    /**
     * @deprecated use {@link #getAllSegments()} instead
     */
    @Deprecated
    protected String[] getSegments() {
        final List<String> segments = super.getAllSegments();
        return segments.toArray(new String[segments.size()]);
    }

    @Override
    // Avoid subclasses to require importing the o.e.sh.core.common package
    protected List<String> getAllSegments() {
        return super.getAllSegments();
    }

    @Override
    // Avoid bindings to require importing the o.e.sh.core.common package
    public String toString() {
        return super.toString();
    }

    @Override
    // Avoid bindings to require importing the o.e.sh.core.common package
    public String getAsString() {
        return super.getAsString();
    }

    @Override
    // Avoid bindings to require importing the o.e.sh.core.common package
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    // Avoid bindings to require importing the o.e.sh.core.common package
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }
}
