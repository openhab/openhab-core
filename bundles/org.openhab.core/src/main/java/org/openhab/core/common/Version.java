/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.common;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class holds version information in a standardized form, which can easily be compared, sorted or checked for
 * equality. It allows for consistent, system-wide treatment of versions. It also bridges with
 * {@link org.osgi.framework.Version} by having to and from conversion methods.
 * <p>
 * This class is immutable.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class Version implements Comparable<Version> {

    /** The {@link Pattern} used to parse version strings */
    public static final Pattern VERSION_PATTERN = Pattern.compile(
            "^\\s*(?<major>\\d+)(?:\\s*\\.\\s*(?<minor>\\d+)(?:\\s*\\.\\s*(?<micro>\\d+)(?:\\s*(?<lastSeparator>\\.|-|_)\\s*(?<qualifier>[a-zA-Z0-9_-]+))?)?)?\\s*$");
    protected static final Pattern QUALIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    protected static final Pattern RC_PATTERN = Pattern.compile("(?i)rc(\\d+)");
    protected static final Pattern MILESTONE_PATTERN = Pattern.compile("(?i)m(\\d+)");
    protected static final Pattern SNAPSHOT_PATTERN = Pattern.compile("(?i)snapshot");
    protected static final char SEPARATOR = '.';

    protected final int major;
    protected final int minor;
    protected final int micro;
    protected final String qualifier;
    protected final char lastSeparator;
    private transient volatile @Nullable String versionString;
    private transient volatile int hash;

    /**
     * The "empty" version, {@code 0.0.0}.
     */
    public static final Version EMPTY_VERSION = new Version(0, 0, 0);

    /**
     * Create a new instance by parsing the specified version string.
     *
     * @param version the version string to parse.
     * @throws IllegalArgumentException If a version syntax can't be parsed from {@code version}.
     */
    public Version(String version) {
        String s;
        Matcher m = VERSION_PATTERN.matcher(version);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid version format \"" + version + '"');
        }

        major = Integer.parseInt(m.group("major"));
        minor = (s = m.group("minor")) == null ? 0 : Integer.parseInt(s);
        micro = (s = m.group("micro")) == null ? 0 : Integer.parseInt(s);
        lastSeparator = (s = m.group("lastSeparator")) == null ? '.' : s.charAt(0);
        qualifier = (s = m.group("qualifier")) == null ? "" : s;
    }

    /**
     * Create a new instance with the specified major, minor and micro values.
     *
     * @param major the major version.
     * @param minor the minor version.
     * @param micro the micro version.
     * @throws IllegalArgumentException If {@code major}, {@code minor} or {@code micro} is negative.
     */
    public Version(int major, int minor, int micro) {
        this(major, minor, micro, null);
    }

    /**
     * Create a new instance with specified major, minor and micro values, and an optional qualifier value.
     *
     * @param major the major version.
     * @param minor the minor version.
     * @param micro the micro version.
     * @param qualifier the qualifier.
     * @throws IllegalArgumentException If {@code major}, {@code minor} or {@code micro} is negative, or if
     *             {@code qualifier} is invalid (not in {@code [a-zA-Z0-9_-]}).
     */
    public Version(int major, int minor, int micro, @Nullable String qualifier) {
        this(major, minor, micro, '.', qualifier);
    }

    /**
     * Create a new instance with specified major, minor and micro values, and an optional qualifier value.
     *
     * @param major the major version.
     * @param minor the minor version.
     * @param micro the micro version.
     * @param lastSeparator the last separator, which can differ from the others and be one of "{@code .}",
     *            "{@code -}" or "{@code _}".
     * @param qualifier the qualifier.
     * @throws IllegalArgumentException If {@code major}, {@code minor} or {@code micro} is negative, if
     *             {@code lastSeparator} is invalid (not in {@code [._-]}), or if {@code qualifier} is invalid (not in
     *             {@code [a-zA-Z0-9_-]}).
     */
    public Version(int major, int minor, int micro, char lastSeparator, @Nullable String qualifier) {
        if (major < 0) {
            throw new IllegalArgumentException("Major version cannot be negative: " + major);
        }
        if (minor < 0) {
            throw new IllegalArgumentException("Minor version cannot be negative:" + minor);
        }
        if (micro < 0) {
            throw new IllegalArgumentException("Micro version cannot be negative:" + micro);
        }
        if (lastSeparator != '.' && lastSeparator != '-' && lastSeparator != '_') {
            throw new IllegalArgumentException("Invalid last separator: \"" + lastSeparator + '"');
        }
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.lastSeparator = lastSeparator;
        if (qualifier == null || qualifier.isEmpty()) {
            this.qualifier = "";
        } else {
            if (!QUALIFIER_PATTERN.matcher(qualifier).find()) {
                throw new IllegalArgumentException("Invalid qualifier: \"" + qualifier + '"');
            }
            this.qualifier = qualifier;
        }
    }

    /**
     * @return The major version.
     */
    public int getMajor() {
        return major;
    }

    /**
     * @return The minor version.
     */
    public int getMinor() {
        return minor;
    }

    /**
     * @return The micro version.
     */
    public int getMicro() {
        return micro;
    }

    /**
     * @return The last separator, which can differ from the others and be one of "{@code .}", "{@code -}" or
     *         "{@code _}".
     */
    public char getLastSeparator() {
        return lastSeparator;
    }

    /**
     * @return The qualifier.
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Convert this {@link Version} to an {@link org.osgi.framework.Version} instance.
     *
     * @return The corresponding {@link org.osgi.framework.Version} instance.
     */
    public org.osgi.framework.Version toOSGiVersion() {
        return new org.osgi.framework.Version(major, minor, micro, qualifier);
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h != 0) {
            return h;
        }
        return hash = Objects.hash(major, micro, minor, lastSeparator, qualifier);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Version)) {
            return false;
        }
        Version other = (Version) obj;
        return lastSeparator == other.lastSeparator && major == other.major && micro == other.micro
                && minor == other.minor && Objects.equals(qualifier, other.qualifier);
    }

    @Override
    public String toString() {
        String s = versionString;
        if (s != null) {
            return s;
        }
        int qLen = qualifier.length();
        StringBuilder sb = new StringBuilder(20 + qLen);
        sb.append(major).append(SEPARATOR).append(minor).append(SEPARATOR).append(micro);
        if (qLen > 0) {
            sb.append(lastSeparator).append(qualifier);
        }
        return versionString = sb.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation is inconsistent with equals for {@code lastSeparator}, which isn't taken into
     *           account when comparing versions.
     */
    @Override
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public int compareTo(@Nullable Version other) {
        if (other == this) {
            return 0;
        }
        if (other == null) {
            return 1;
        }

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

        if (qualifier.equals(other.qualifier)) {
            // Includes if both are empty
            return 0;
        }
        if (qualifier.isEmpty()) {
            // Release is newer
            return 1;
        }
        if (other.qualifier.isEmpty()) {
            // Release is newer
            return -1;
        }

        Matcher rcMatcher = RC_PATTERN.matcher(qualifier);
        Matcher orcMatcher = RC_PATTERN.matcher(other.qualifier);
        Matcher msMatcher = MILESTONE_PATTERN.matcher(qualifier);
        Matcher omsMatcher = MILESTONE_PATTERN.matcher(other.qualifier);
        boolean rc = rcMatcher.matches();
        boolean orc = orcMatcher.matches();
        boolean ms = msMatcher.matches();
        boolean oms = omsMatcher.matches();
        if (rc && orc) {
            // Both are release candidates
            int n = Integer.valueOf(rcMatcher.group(1));
            int on = Integer.valueOf(orcMatcher.group(1));
            return Integer.compare(n, on);
        }
        if (ms && oms) {
            // Both are milestones
            int n = Integer.valueOf(msMatcher.group(1));
            int on = Integer.valueOf(omsMatcher.group(1));
            return Integer.compare(n, on);
        }

        if (rc && oms) {
            // Release candidate is newer than milestone
            return 1;
        }
        if (ms && orc) {
            // Milestone is older than release candidate
            return -1;
        }

        long ql, oql;
        try {
            ql = Long.parseLong(qualifier);
        } catch (NumberFormatException e) {
            ql = Long.MIN_VALUE;
        }
        try {
            oql = Long.parseLong(other.qualifier);
        } catch (NumberFormatException e) {
            oql = Long.MIN_VALUE;
        }

        if (ql >= 0 && oql >= 0) {
            // Both are positive integers, compare numerically
            return Long.compare(ql, oql);
        }

        boolean ss = SNAPSHOT_PATTERN.matcher(qualifier).matches();
        boolean oss = SNAPSHOT_PATTERN.matcher(other.qualifier).matches();
        if (ql < 0 && oql < 0) {
            // Both aren't positive integers, snapshots are newer, otherwise do a simple string comparison
            if (ss) {
                // Snapshots are newer
                return 1;
            }
            if (oss) {
                // Non-snapshots are older
                return -1;
            }

            // If both are snapshots but have different case, compare them to remain consistent with equals()
            return qualifier.compareTo(other.qualifier);
        }

        if (ss) {
            // Snapshots are newer than numbers
            return 1;
        }

        if (oss) {
            // Numbers are older than snapshots
            return -1;
        }

        // Numbers are newer than non-numbers
        return ql >= 0 ? 1 : -1;
    }

    /**
     * Compare this with a {@link org.osgi.framework.Version}.
     *
     * @param other the {@link org.osgi.framework.Version} to compare with.
     * @return A negative integer, zero, or a positive integer as this instance is less than, equal to, or greater than
     *         the specified {@link org.osgi.framework.Version}.
     */
    public int compareTo(org.osgi.framework.Version other) {
        int result = major - other.getMajor();
        if (result != 0) {
            return result;
        }

        result = minor - other.getMinor();
        if (result != 0) {
            return result;
        }

        result = micro - other.getMicro();
        if (result != 0) {
            return result;
        }

        String oq = other.getQualifier();

        if (qualifier.equals(oq)) {
            // Includes if both are empty
            return 0;
        }
        if (qualifier.isEmpty()) {
            // Release is newer
            return 1;
        }
        if (oq.isEmpty()) {
            // Release is newer
            return -1;
        }

        Matcher rcMatcher = RC_PATTERN.matcher(qualifier);
        Matcher orcMatcher = RC_PATTERN.matcher(oq);
        Matcher msMatcher = MILESTONE_PATTERN.matcher(qualifier);
        Matcher omsMatcher = MILESTONE_PATTERN.matcher(oq);
        boolean rc = rcMatcher.matches();
        boolean orc = orcMatcher.matches();
        boolean ms = msMatcher.matches();
        boolean oms = omsMatcher.matches();
        if (rc && orc) {
            // Both are release candidates
            int n = Integer.valueOf(rcMatcher.group(1));
            int on = Integer.valueOf(orcMatcher.group(1));
            return Integer.compare(n, on);
        }
        if (ms && oms) {
            // Both are milestones
            int n = Integer.valueOf(msMatcher.group(1));
            int on = Integer.valueOf(omsMatcher.group(1));
            return Integer.compare(n, on);
        }

        if (rc && oms) {
            // Release candidate is newer than milestone
            return 1;
        }
        if (ms && orc) {
            // Milestone is older than release candidate
            return -1;
        }

        long ql, oql;
        try {
            ql = Long.parseLong(qualifier);
        } catch (NumberFormatException e) {
            ql = Long.MIN_VALUE;
        }
        try {
            oql = Long.parseLong(oq);
        } catch (NumberFormatException e) {
            oql = Long.MIN_VALUE;
        }

        if (ql >= 0 && oql >= 0) {
            // Both are positive integers, compare numerically
            return Long.compare(ql, oql);
        }

        boolean ss = SNAPSHOT_PATTERN.matcher(qualifier).matches();
        boolean oss = SNAPSHOT_PATTERN.matcher(oq).matches();
        if (ql < 0 && oql < 0) {
            // Both aren't positive integers, snapshots are newer, otherwise do a simple string comparison
            if (ss) {
                // Snapshots are newer
                return 1;
            }
            if (oss) {
                // Non-snapshots are older
                return -1;
            }

            // If both are snapshots but have different case, compare them to remain consistent with equals()
            return qualifier.compareTo(oq);
        }

        if (ss) {
            // Snapshots are newer than numbers
            return 1;
        }

        if (oss) {
            // Numbers are older than snapshots
            return -1;
        }

        // Numbers are newer than non-numbers
        return ql >= 0 ? 1 : -1;
    }

    /**
     * Create a new {@link Version} instance by parsing the specified version string.
     *
     * @param version the version string to parse.
     * @return The new {@link Version} instance.
     * @throws IllegalArgumentException If a version syntax can't be parsed from {@code version}.
     */
    public static Version valueOf(@Nullable String version) {
        if (version == null) {
            return EMPTY_VERSION;
        }
        String v = version.trim();
        if (v.length() == 0) {
            return EMPTY_VERSION;
        }

        return new Version(v);
    }

    /**
     * Create a new {@link Version} instance from the specified {@link org.osgi.framework.Version}.
     *
     * @param version the {@link org.osgi.framework.Version} to get the version info from.
     * @return The new {@link Version} instance.
     */
    public static Version valueOf(org.osgi.framework.Version version) {
        return new Version(version.getMajor(), version.getMinor(), version.getMicro(), version.getQualifier());
    }
}
