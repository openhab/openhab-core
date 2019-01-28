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
package org.eclipse.smarthome.core.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A non specific base class for unique identifiers within the SmartHome framework.
 *
 * @author Markus Rathgeb - Splitted from the Thing's UID class
 */
@NonNullByDefault
public abstract class AbstractUID {

    public static final String SEGMENT_PATTERN = "[A-Za-z0-9_-]*";
    public static final String SEPARATOR = ":";
    private final List<String> segments;

    /**
     * Constructor must be public, otherwise it can not be called by subclasses from another package.
     */
    public AbstractUID() {
        this.segments = Collections.emptyList();
    }

    /**
     * Parses a UID for a given string. The UID must be in the format
     * 'bindingId:segment:segment:...'.
     *
     * @param uid uid in form a string (must not be null)
     */
    public AbstractUID(String uid) {
        this(splitToSegments(uid));
    }

    /**
     * Creates a AbstractUID for a list of segments.
     *
     * @param segments the id segments
     */
    public AbstractUID(final String... segments) {
        this(Arrays.asList(segments));
    }

    /**
     * Creates a UID for list of segments.
     *
     * @param segments segments (must not be null)
     */
    public AbstractUID(List<String> segments) {
        int minNumberOfSegments = getMinimalNumberOfSegments();
        int numberOfSegments = segments.size();
        if (numberOfSegments < minNumberOfSegments) {
            throw new IllegalArgumentException(
                    String.format("UID must have at least %d segments.", minNumberOfSegments));
        }
        for (int i = 0; i < numberOfSegments; i++) {
            String segment = segments.get(i);
            validateSegment(segment, i, numberOfSegments);
        }
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
    }

    /**
     * Specifies how many segments the UID has to have at least.
     *
     * @return
     */
    protected abstract int getMinimalNumberOfSegments();

    protected List<String> getAllSegments() {
        return segments;
    }

    protected String getSegment(int segment) {
        return segments.get(segment);
    }

    protected void validateSegment(String segment, int index, int length) {
        if (!segment.matches(SEGMENT_PATTERN)) {
            throw new IllegalArgumentException(String.format(
                    "ID segment '%s' contains invalid characters. Each segment of the ID must match the pattern %s.",
                    segment, SEGMENT_PATTERN));
        }
    }

    @Override
    public String toString() {
        return getAsString();
    }

    public String getAsString() {
        return String.join(SEPARATOR, segments);
    }

    private static List<String> splitToSegments(final String id) {
        return Arrays.asList(id.split(SEPARATOR));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + segments.hashCode();
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
        AbstractUID other = (AbstractUID) obj;
        if (!segments.equals(other.segments)) {
            return false;
        }
        return true;
    }

}
