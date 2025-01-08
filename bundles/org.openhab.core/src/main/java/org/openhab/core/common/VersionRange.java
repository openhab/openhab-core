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
 * This class represents a version range, where it can be evaluated whether any {@link Version} is included in the
 * range. It allows for consistent, system-wide parsing and evaluation of version ranges. The supported string syntax
 * expresses a version range using mathematical interval notation.
 * <p>
 * A valid range string requires an opening bracket or parenthesis, a mandatory lower bound, a range separator
 * ("{@code ;}", "{@code ,}", or "{@code ..}"), an optional upper bound, and a closing bracket or parenthesis. Brackets,
 * "{@code [}" and "{@code ]}", designate inclusive endpoints, while parentheses, "{@code (}" and "{@code )}",designate
 * exclusive endpoints.
 * <p>
 * <h3>Formal Syntax Grammar (ABNF)</h3>
 *
 * <pre>{@code
 * version-range   = interval
 *
 * interval        = ( "[" / "(" ) version separator [ version ] ( "]" / ")" )
 *
 * separator       = ";" / "," / ".."
 *
 * version         = major [ "." minor [ "." micro [ "." qualifier ] ] ]
 * major           = 1*DIGIT
 * minor           = 1*DIGIT
 * micro           = 1*DIGIT
 * qualifier       = 1*alphanumeric
 *
 * alphanumeric    = ALPHA / DIGIT / "-" / "_"
 * }</pre>
 *
 * <h3>Examples</h3>
 * <ul>
 * <li>{@code [5.1.0;5.2.0]} - Inclusive range from 5.1.0 to 5.2.0</li>
 * <li>{@code [5.0.0..)} - Open upper bound (version 5.0.0 or any later version)</li>
 * <li>{@code [5.2.0;5.2.0]} - Exact match for version 5.2.0</li>
 * <li>{@code [4.2.3,6)} - Range including 4.2.3 and anything before 6.0.0</li>
 * </ul>
 * <p>
 * This class is immutable.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class VersionRange {

    public static final VersionRange ANY = new VersionRange(true, Version.valueOf("0.0.0"), null, true);

    /** The left endpoint is exclusive ({@code '('}) */
    public static final char LEFT_EXCLUSIVE = '(';

    /** The left endpoint is inclusive ({@code '['}) */
    public static final char LEFT_INCLUSIVE = '[';

    /** The right endpoint is exclusive ({@code ')'}) */
    public static final char RIGHT_EXCLUSIVE = ')';

    /** The right endpoint is inclusive ({@code ']'}) */
    public static final char RIGHT_INCLUSIVE = ']';

    /** The {@link Pattern} used to parse version range strings */
    public static final Pattern RANGE_PATTERN = Pattern.compile(
            "\\s*(?<leftType>[\\[\\(])(?<left>\\d+(\\.\\d+(\\.\\d+(\\.[^\\)\\]]+)?)?)?)(?:,|;|\\.\\.)(?<right>\\d+(\\.\\d+(\\.\\d+(\\.[^\\)\\]]+)?)?)?)?(?<rightType>[\\]\\)])\\s*");

    protected static final Pattern WHITESPACE = Pattern.compile("\\s+");
    protected static final Pattern SEPARATORS = Pattern.compile(":|;|\\.\\.");

    protected final boolean leftInclusive;
    protected final Version left;
    protected final @Nullable Version right;
    protected final boolean rightInclusive;
    private transient volatile @Nullable String versionRangeString;
    private transient volatile int hash;

    /**
     * Create a new instance by parsing the specified version range string. See {@link VersionRange} for syntax
     * description.
     *
     * @param range the version range to parse.
     * @throws IllegalArgumentException If a valid version range can't be parsed from {@code range}.
     */
    public VersionRange(String range) {
        Objects.requireNonNull(range, "range cannot be null");
        if (range.isBlank()) {
            throw new IllegalArgumentException("range cannot be blank");
        }
        String r = range;
        Matcher matcher = WHITESPACE.matcher(r);
        if (matcher.find()) {
            r = matcher.replaceAll("");
        }
        matcher = SEPARATORS.matcher(r);
        if (matcher.find()) {
            r = matcher.replaceAll(",");
        }
        matcher = RANGE_PATTERN.matcher(r);
        if (!matcher.find() || matcher.group("left").isBlank()) {
            throw new IllegalArgumentException("Invalid range \"" + range + '"');
        }
        String right = matcher.group("right");
        this.leftInclusive = "[".equals(matcher.group("leftType"));
        this.left = Version.valueOf(matcher.group("left"));
        this.right = right == null ? null : Version.valueOf(right);
        this.rightInclusive = "]".equals(matcher.group("rightType"));
    }

    /**
     * Create a new instance using the specified parameters.
     *
     * @param leftType the left/opening bracket or parenthesis.
     * @param leftEndpoint the "from" version.
     * @param rightEndpoint the "to" version.
     * @param rightType the right/closing bracket or parenthesis.
     * @throws IllegalArgumentException If {@code leftType} or {@code rightType} isn't one of the valid characters.
     */
    public VersionRange(char leftType, Version leftEndpoint, @Nullable Version rightEndpoint, char rightType) {
        if ((leftType != LEFT_INCLUSIVE) && (leftType != LEFT_EXCLUSIVE)) {
            throw new IllegalArgumentException("Invalid leftType \"" + leftType + "\"");
        }
        if ((rightType != RIGHT_EXCLUSIVE) && (rightType != RIGHT_INCLUSIVE)) {
            throw new IllegalArgumentException("Invalid rightType \"" + rightType + "\"");
        }
        this.leftInclusive = leftType == LEFT_INCLUSIVE;
        this.left = leftEndpoint;
        this.right = rightEndpoint;
        this.rightInclusive = rightType == RIGHT_INCLUSIVE;
    }

    /**
     * Create a new instance using the specified parameters.
     *
     * @param leftInclusive whether the left/opening version is inclusive.
     * @param leftEndpoint the "from" version.
     * @param rightEndpoint the "to" version.
     * @param rightInclusive whether the right/closing version is inclusive.
     */
    public VersionRange(boolean leftInclusive, Version leftEndpoint, @Nullable Version rightEndpoint,
            boolean rightInclusive) {
        this.leftInclusive = leftInclusive;
        this.left = leftEndpoint;
        this.right = rightEndpoint;
        this.rightInclusive = rightInclusive;
    }

    /**
     * Check if the specified version is in this version range.
     *
     * @param version the version to evaluate.
     * @return {@code true} if it {@code version} is in the version range, {@code false} otherwise.
     */
    public boolean includes(org.osgi.framework.Version version) {
        return includes(Version.valueOf(version));
    }

    /**
     * Check if the specified version is in this version range.
     *
     * @param version the version to evaluate.
     * @return {@code true} if it {@code version} is in the version range, {@code false} otherwise.
     */
    public boolean includes(Version version) {
        Version v = version;
        Version right = this.right;
        if (left.compareTo(v) >= (leftInclusive ? 1 : 0)) {
            return false;
        }
        if (!v.getQualifier().isEmpty() && !this.rightInclusive && right != null && right.getQualifier().isEmpty()) {
            // This is a special case where, although technically correct, we don't want e.g 5.0.0.RC1 to be included in
            // [x.x.x,5.0.0)
            v = new Version(version.getMajor(), version.getMinor(), version.getMicro());
        }
        if (right == null) {
            return true;
        }
        return right.compareTo(v) >= (rightInclusive ? 0 : 1);
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h != 0) {
            return h;
        }
        return h = Objects.hash(leftInclusive, left, right, rightInclusive);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VersionRange)) {
            return false;
        }
        VersionRange other = (VersionRange) obj;
        return Objects.equals(left, other.left) && leftInclusive == other.leftInclusive
                && Objects.equals(right, other.right) && rightInclusive == other.rightInclusive;
    }

    @Override
    public String toString() {
        String s = versionRangeString;
        if (s != null) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(leftInclusive ? LEFT_INCLUSIVE : LEFT_EXCLUSIVE).append(left.toString()).append(',');
        Version r;
        if ((r = right) == null) {
            sb.append(RIGHT_INCLUSIVE);
        } else {
            sb.append(r.toString()).append(rightInclusive ? RIGHT_INCLUSIVE : RIGHT_EXCLUSIVE);
        }
        return versionRangeString = sb.toString();
    }

    /**
     * Create a new {@link VersionRange} instance by parsing the specified version range string. See
     * {@link VersionRange} for syntax definition.
     *
     * @param range the version range string to parse.
     * @return The new {@link VersionRange} instance.
     * @throws IllegalArgumentException If a valid version range can't be parsed from {@code range}.
     */
    public static VersionRange valueOf(@Nullable String range) {
        if (range == null || range.isBlank()) {
            return ANY;
        }
        return new VersionRange(range);
    }
}
