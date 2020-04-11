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
package org.openhab.core.library.types;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.ComplexType;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 * This type can be used for items that are dealing with GPS or
 * location awareness functionality.
 *
 * @author Gaël L'hopital - Initial contribution
 * @author John Cocula - Initial contribution
 */
@NonNullByDefault
public class PointType implements ComplexType, Command, State {

    // external format patterns for output
    public static final String LOCATION_PATTERN = "%2$s°N %3$s°E %1$sm";

    public static final double EARTH_GRAVITATIONAL_CONSTANT = 3.986004418e14;
    public static final double WGS84_A = 6378137; // The equatorial radius of WGS84 ellipsoid (6378137 m).

    // constants for the constituents
    public static final String KEY_LATITUDE = "lat";
    public static final String KEY_LONGITUDE = "long";
    public static final String KEY_ALTITUDE = "alt";

    private static final BigDecimal CIRCLE = new BigDecimal(360);
    private static final BigDecimal FLAT = new BigDecimal(180);
    private static final BigDecimal RIGHT = new BigDecimal(90);

    private BigDecimal latitude = BigDecimal.ZERO; // in decimal degrees
    private BigDecimal longitude = BigDecimal.ZERO; // in decimal degrees
    private BigDecimal altitude = BigDecimal.ZERO; // in decimal meters

    /**
     * Default constructor creates a point at sea level where the equator
     * (0° latitude) and the prime meridian (0° longitude) intersect.
     * A nullary constructor is needed by
     * {@link org.openhab.core.internal.items.ItemUpdater#receiveUpdate})
     */
    public PointType() {
    }

    public PointType(DecimalType latitude, DecimalType longitude) {
        canonicalize(latitude, longitude);
    }

    public PointType(DecimalType latitude, DecimalType longitude, DecimalType altitude) {
        this(latitude, longitude);
        setAltitude(altitude);
    }

    public PointType(StringType latitude, StringType longitude) {
        this(new DecimalType(latitude.toString()), new DecimalType(longitude.toString()));
    }

    public PointType(StringType latitude, StringType longitude, StringType altitude) {
        this(new DecimalType(latitude.toString()), new DecimalType(longitude.toString()),
                new DecimalType(altitude.toString()));
    }

    public PointType(String value) {
        if (!value.isEmpty()) {
            List<String> elements = Arrays.stream(value.split(",")).map(in -> in.trim()).collect(Collectors.toList());
            if (elements.size() >= 2) {
                canonicalize(new DecimalType(elements.get(0)), new DecimalType(elements.get(1)));
                if (elements.size() == 3) {
                    setAltitude(new DecimalType(elements.get(2)));
                } else if (elements.size() > 3) {
                    throw new IllegalArgumentException(value
                            + " is not a valid PointType syntax. The syntax must not consist of more than 3 elements.");
                }
            } else {
                throw new IllegalArgumentException(value + " is not a valid PointType syntax");
            }
        } else {
            throw new IllegalArgumentException("Constructor argument must not be blank");
        }
    }

    public DecimalType getLatitude() {
        return new DecimalType(latitude);
    }

    public DecimalType getLongitude() {
        return new DecimalType(longitude);
    }

    public DecimalType getAltitude() {
        return new DecimalType(altitude);
    }

    public void setAltitude(DecimalType altitude) {
        this.altitude = altitude.toBigDecimal();
    }

    public DecimalType getGravity() {
        double latRad = Math.toRadians(latitude.doubleValue());
        double deltaG = -2000.0 * (altitude.doubleValue() / 1000) * EARTH_GRAVITATIONAL_CONSTANT
                / (Math.pow(WGS84_A, 3.0));
        double sin2lat = Math.sin(latRad) * Math.sin(latRad);
        double sin22lat = Math.sin(2.0 * latRad) * Math.sin(2.0 * latRad);
        double result = (9.780327 * (1.0 + 5.3024e-3 * sin2lat - 5.8e-6 * sin22lat) + deltaG);
        return new DecimalType(result);
    }

    /**
     * Return the distance in meters from otherPoint, ignoring altitude. This algorithm also
     * ignores the oblate spheroid shape of Earth and assumes a perfect sphere, so results
     * are inexact.
     *
     * @param otherPoint
     * @return distance in meters
     * @see <a href="https://en.wikipedia.org/wiki/Haversine_formula">Haversine formula</a>
     */
    public DecimalType distanceFrom(PointType otherPoint) {
        double dLat = Math.toRadians(otherPoint.latitude.doubleValue() - this.latitude.doubleValue());
        double dLong = Math.toRadians(otherPoint.longitude.doubleValue() - this.longitude.doubleValue());
        double a = Math.pow(Math.sin(dLat / 2D), 2D) + Math.cos(Math.toRadians(this.latitude.doubleValue()))
                * Math.cos(Math.toRadians(otherPoint.latitude.doubleValue())) * Math.pow(Math.sin(dLong / 2D), 2D);
        double c = 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
        return new DecimalType(WGS84_A * c);
    }

    /**
     * Formats the value of this type according to a pattern (see {@link Formatter}). One single value of this type can
     * be referenced by the pattern using an index. The item order is defined by the natural (alphabetical) order of
     * their keys.
     *
     * @param pattern the pattern to use containing indexes to reference the single elements of this type
     * @return the formatted string
     */
    @Override
    public String format(@Nullable String pattern) {
        String formatPattern = pattern;

        if (formatPattern == null || "%s".equals(formatPattern)) {
            formatPattern = LOCATION_PATTERN;
        }

        return String.format(formatPattern, getConstituents().values().toArray());
    }

    public static PointType valueOf(String value) {
        return new PointType(value);
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        StringBuilder sb = new StringBuilder(latitude.toPlainString());
        sb.append(',');
        sb.append(longitude.toPlainString());
        if (!BigDecimal.ZERO.equals(altitude)) {
            sb.append(',');
            sb.append(altitude.toPlainString());
        }

        return sb.toString();
    }

    @Override
    public SortedMap<String, PrimitiveType> getConstituents() {
        SortedMap<String, PrimitiveType> result = new TreeMap<>();
        result.put(KEY_LATITUDE, getLatitude());
        result.put(KEY_LONGITUDE, getLongitude());
        result.put(KEY_ALTITUDE, getAltitude());
        return result;
    }

    /**
     * Canonicalize the current latitude and longitude values such that:
     *
     * <pre>
     * -90 &lt;= latitude &lt;= +90 - 180 &lt; longitude &lt;= +180
     * </pre>
     */
    private void canonicalize(DecimalType aLat, DecimalType aLon) {
        latitude = FLAT.add(aLat.toBigDecimal()).remainder(CIRCLE);
        longitude = aLon.toBigDecimal();
        if (latitude.compareTo(BigDecimal.ZERO) == -1) {
            latitude = latitude.add(CIRCLE);
        }

        latitude = latitude.subtract(FLAT);
        if (latitude.compareTo(RIGHT) == 1) {
            latitude = FLAT.subtract(latitude);
            longitude = longitude.add(FLAT);
        } else if (latitude.compareTo(RIGHT.negate()) == -1) {
            latitude = FLAT.negate().subtract(latitude);
            longitude = longitude.add(FLAT);
        }

        longitude = FLAT.add(longitude).remainder(CIRCLE);
        if (longitude.compareTo(BigDecimal.ZERO) <= 0) {
            longitude = longitude.add(CIRCLE);
        }
        longitude = longitude.subtract(FLAT);

    }

    @Override
    public int hashCode() {
        int tmp = 10000 * getLatitude().hashCode();
        tmp += 100 * getLongitude().hashCode();
        tmp += getAltitude().hashCode();
        return tmp;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PointType)) {
            return false;
        }
        PointType other = (PointType) obj;
        if (!getLatitude().equals(other.getLatitude()) || !getLongitude().equals(other.getLongitude())
                || !getAltitude().equals(other.getAltitude())) {
            return false;
        }
        return true;
    }

}
