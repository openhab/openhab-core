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
package org.openhab.core.addon.marketplace;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link BundleVersion} wraps a bundle version and provides a method to compare them
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class BundleVersion {
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<micro>\\d+)(\\.((?<rc>RC)|(?<milestone>M))?(?<qualifier>\\d+))?");
    public static final Pattern RANGE_PATTERN = Pattern.compile(
            "\\[(?<start>\\d+\\.\\d+(?<startmicro>\\.\\d+(\\.\\w+)?)?);(?<end>\\d+\\.\\d+(?<endmicro>\\.\\d+(\\.\\w+)?)?)(?<endtype>[)\\]])");

    private final String version;
    private final int major;
    private final int minor;
    private final int micro;
    private final @Nullable Long qualifier;

    public BundleVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.matches()) {
            this.version = version;
            this.major = Integer.parseInt(matcher.group("major"));
            this.minor = Integer.parseInt(matcher.group("minor"));
            this.micro = Integer.parseInt(matcher.group("micro"));
            String qualifier = matcher.group("qualifier");
            if (qualifier != null) {
                long intQualifier = Long.parseLong(qualifier);
                if (matcher.group("rc") != null) {
                    // we can safely assume that there are less than Integer.MAX_VALUE milestones
                    // so RCs are always newer than milestones
                    // since snapshot qualifiers are larger than 10*Integer.MAX_VALUE they are
                    // still considered newer
                    this.qualifier = intQualifier + Integer.MAX_VALUE;
                } else {
                    this.qualifier = intQualifier;
                }
            } else {
                this.qualifier = null;
            }
        } else {
            throw new IllegalArgumentException("Input does not match pattern");
        }
    }

    /**
     * Test if this version is within the provided range
     *
     * @param range a Maven like version range
     * @return {@code true} if this version is inside range, {@code false} otherwise
     * @throws IllegalArgumentException if {@code range} does not represent a valid range
     */
    public boolean inRange(@Nullable String range) throws IllegalArgumentException {
        if (range == null || range.isBlank()) {
            // if no range is given, we assume the range covers everything
            return true;
        }
        Matcher matcher = RANGE_PATTERN.matcher(range);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(range + "is not a valid version range");
        }
        String startString = matcher.group("startmicro") != null ? matcher.group("start")
                : matcher.group("start") + ".0";
        BundleVersion startVersion = new BundleVersion(startString);
        if (this.compareTo(startVersion) < 0) {
            return false;
        }

        String endString = matcher.group("endmicro") != null ? matcher.group("end") : matcher.group("stop") + ".0";
        boolean inclusive = "]".equals(matcher.group("endtype"));
        BundleVersion endVersion = new BundleVersion(endString);
        int comparison = this.compareTo(endVersion);
        return (inclusive && comparison == 0) || comparison < 0;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
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

        // both versions are milestones, we can compare them
        return Long.compare(qualifier, other.qualifier);
    }

    @Override
    public String toString() {
        return version;
    }
}
