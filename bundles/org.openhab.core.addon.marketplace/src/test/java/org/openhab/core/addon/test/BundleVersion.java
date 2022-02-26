/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.addon.test;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link BundleVersion} wraps a bundle version
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class BundleVersion {
    private static final Pattern VERSION_PATTERN = Pattern
            .compile("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<micro>\\d+)(\\.((?<snapshot>\\d+)|M(?<milestone>\\d)))?");
    private final int major;
    private final int minor;
    private final int micro;
    private final @Nullable String qualifier;

    public BundleVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.matches()) {
            this.major = Integer.parseInt(matcher.group("major"));
            this.minor = Integer.parseInt(matcher.group("minor"));
            this.micro = Integer.parseInt(matcher.group("micro"));
            String milestone = matcher.group("milestone");
            qualifier = milestone != null ? milestone : matcher.group("snapshot") != null ? "SNAPSHOT" : null;
        } else {
            throw new IllegalArgumentException("Input does not match pattern");
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BundleVersion version = (BundleVersion) o;
        return major == version.major && minor == version.minor && micro == version.micro
                && Objects.equals(qualifier, version.qualifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, micro, qualifier);
    }

    /**
     * Compares two bundle versions
     *
     * @param other the other bundle version
     * @return a positive integer if this version is newer than the other version, a negative number if this version is
     *         older than the other version and 0 if the versions are equal
     */
    public int compareTo(BundleVersion other) {
        int result = major - other.major;
        if (result != 0) {
            return result;
        }

        result = minor - other.minor;
        if (result != 0) {
            return result;
        }

        result = micro - other.micro;
        if (result != 0) {
            return result;
        }

        if (Objects.equals(qualifier, other.qualifier)) {
            return 0;
        }

        // the release is always newer than a milestone or snapshot
        if (qualifier == null) { // we are the release
            return 1;
        }
        if (other.qualifier == null) { // the other is the release
            return -1;
        }

        // we assume a snapshot is always latest, so newer than a milestone
        if ("SNAPSHOT".equals(qualifier)) { // we are the snapshot
            return 1;
        }
        if ("SNAPSHOT".equals(other.qualifier)) { // the other is the snapshot
            return -1;
        }

        // both versions are milestones, we can compare them
        return Integer.compare(Integer.parseInt(Objects.requireNonNull(qualifier)),
                Integer.parseInt(Objects.requireNonNull(other.qualifier)));
    }
}
