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
package org.openhab.core.automation.internal.provider;

import java.util.StringTokenizer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class is designed to serves as a holder of most significant information for a bundle that provides resources
 * for automation objects - bundle ID and bundle version. These two features of the bundle, define it uniquely and
 * determine if the bundle was updated, which needs to be checked after the system has been restarted.
 *
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
public class Vendor {

    private static final String DELIMITER = ";";

    /**
     * This field provides a bundle symbolic name of a bundle that provides resources for automation objects.
     */
    private String vendorSymbolicName;

    /**
     * This field provides a bundle symbolic name of a bundle that provides resources for automation objects, but "." is
     * replaced with "_".
     */
    private String vendorID;

    /**
     * This field provides a bundle version of a bundle that provides resources for automation objects.
     */
    private String vendorVersion;

    /**
     * This field keeps the count of the rules provided from this vendor.
     */
    private int rulesCount = 0;

    public Vendor(String nameversion) {
        int index = nameversion.indexOf(DELIMITER);
        vendorSymbolicName = nameversion.substring(0, index);
        vendorVersion = nameversion.substring(++index);
        vendorID = parseSymbolicName();
    }

    /**
     * This constructor initialize the {@link vendorId} and the {@link vendorVersion} of the vendor with corresponding
     * bundle ID and bundle version of a bundle that provides resources for the automation objects.
     *
     * @param name a bundle symbolic name of a bundle that providing resources for automation objects.
     * @param version a bundle version of a bundle that provides resources for the automation objects.
     */
    public Vendor(String name, String version) {
        vendorSymbolicName = name;
        vendorVersion = version;
        vendorID = parseSymbolicName();
    }

    /**
     * Getter of {@link vendorSymbolicName}.
     *
     * @return a bundle symbolic name of a bundle that provides resources for the automation objects.
     */
    public String getVendorSymbolicName() {
        return vendorSymbolicName;
    }

    /**
     * Getter of {@link vendorId}.
     *
     * @return a bundle symbolic name of a bundle that provides resources for the automation objects.
     */
    public String getVendorID() {
        return vendorID;
    }

    /**
     * Getter of {@link vendorVersion}.
     *
     * @return a bundle version of a bundle that provides resources for the automation objects.
     */
    public String getVendorVersion() {
        return vendorVersion;
    }

    /**
     * Setter of {@link vendorVersion}.
     *
     * @param version a bundle version of a bundle that provides resources for the automation objects.
     */
    public void setVendorVersion(String version) {
        vendorVersion = version;
    }

    /**
     * This method increases the rules count and returns the current count.
     *
     * @return the current count of the rules provided by this vendor.
     */
    public int count() {
        return rulesCount++;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Vendor) {
            Vendor other = (Vendor) obj;
            return vendorSymbolicName.equals(other.vendorSymbolicName) && vendorVersion.equals(other.vendorVersion);
        }
        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return vendorSymbolicName.hashCode() + vendorVersion.hashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return vendorSymbolicName + DELIMITER + vendorVersion;
    }

    private String parseSymbolicName() {
        String res = "";
        StringTokenizer tokenizer = new StringTokenizer(vendorSymbolicName, ".");
        while (tokenizer.hasMoreTokens()) {
            res = res + tokenizer.nextToken() + "_";
        }
        return res;
    }
}
