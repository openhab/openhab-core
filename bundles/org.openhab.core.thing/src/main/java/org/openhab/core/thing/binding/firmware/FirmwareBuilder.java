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
package org.openhab.core.thing.binding.firmware;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.internal.firmware.FirmwareImpl;

/**
 * The builder to create a {@link Firmware}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Extracted as separate class for Firmware, introduced firmware restriction
 *         function
 */
@NonNullByDefault
public final class FirmwareBuilder {

    private final ThingTypeUID thingTypeUID;
    private final String version;
    private @Nullable String vendor;
    private @Nullable String model;
    private boolean modelRestricted;
    private @Nullable String description;
    private @Nullable String prerequisiteVersion;
    private @Nullable FirmwareRestriction firmwareRestriction;
    private @Nullable String changelog;
    private @Nullable URL onlineChangelog;
    private @Nullable transient InputStream inputStream;
    private @Nullable String md5Hash;
    private Map<String, String> properties;

    public static FirmwareBuilder create(ThingTypeUID thingTypeUID, String firmwareVersion) {
        return new FirmwareBuilder(thingTypeUID, firmwareVersion);
    }

    /**
     * Creates a new builder.
     *
     * @param thingTypeUID the thing type UID that is associated with this firmware (not null)
     * @param firmwareVersion the version of the firmware to be created (not null)
     * @throws IllegalArgumentException if given firmware version is null or empty; if the thing type UID is null
     */
    private FirmwareBuilder(ThingTypeUID thingTypeUID, String firmwareVersion) {
        checkNotNull(thingTypeUID, "ThingTypeUID");
        this.thingTypeUID = thingTypeUID;

        checkNotNullOrEmpty(firmwareVersion, "Firmware version");
        this.version = firmwareVersion;

        this.properties = new HashMap<>();
    }

    /**
     * Adds the vendor to the builder.
     *
     * @param vendor the vendor to be added to the builder (can be null)
     * @return the updated builder
     */
    public FirmwareBuilder withVendor(@Nullable String vendor) {
        this.vendor = vendor;
        return this;
    }

    /**
     * Adds the model to the builder.
     *
     * @param model the model to be added to the builder (can be null)
     * @return the updated builder
     */
    public FirmwareBuilder withModel(@Nullable String model) {
        this.model = model;
        return this;
    }

    /**
     * Sets the modelRestricted flag in the builder.
     *
     * @param modelRestricted the modelRestricted flag to be added to the builder
     * @return the updated builder
     */
    public FirmwareBuilder withModelRestricted(boolean modelRestricted) {
        this.modelRestricted = modelRestricted;
        return this;
    }

    /**
     * Adds the description to the builder.
     *
     * @param description the description to be added to the builder (can be null)
     * @return the updated builder
     */
    public FirmwareBuilder withDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds the prerequisite version to the builder.
     *
     * @param prerequisiteVersion the prerequisite version to be added to the builder (can be null)
     * @return the updated builder
     */
    public FirmwareBuilder withPrerequisiteVersion(@Nullable String prerequisiteVersion) {
        this.prerequisiteVersion = prerequisiteVersion;
        return this;
    }

    /**
     * Adds the changelog to the builder.
     *
     * @param changelog the changelog to be added to the builder (can be null)
     * @return the updated builder
     */
    public FirmwareBuilder withChangelog(@Nullable String changelog) {
        this.changelog = changelog;
        return this;
    }

    /**
     * Adds the online changelog to the builder.
     *
     * @param onlineChangelog the online changelog to be added to the builder (can be null)
     * @return the updated builder
     */
    public FirmwareBuilder withOnlineChangelog(@Nullable URL onlineChangelog) {
        this.onlineChangelog = onlineChangelog;
        return this;
    }

    /**
     * Adds the input stream for the binary content to the builder.
     *
     * @param inputStream the input stream for the binary content to be added to the builder (can be null)
     * @return the updated builder
     */
    public FirmwareBuilder withInputStream(@Nullable InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    /**
     * Adds the properties to the builder.
     *
     * @param properties the properties to be added to the builder (not null)
     * @return the updated builder
     * @throws IllegalArgumentException if the given properties are null
     */
    public FirmwareBuilder withProperties(Map<String, String> properties) {
        checkNotNull(properties, "Properties");
        this.properties = properties;
        return this;
    }

    /**
     * Adds the given md5 hash value to the builder.
     *
     * @param md5Hash the md5 hash value to be added to the builder (can be null)
     * @return the updated builder
     */
    public FirmwareBuilder withMd5Hash(@Nullable String md5Hash) {
        this.md5Hash = md5Hash;
        return this;
    }

    /**
     * An additional restriction can be applied on the firmware by providing a
     * {@link FirmwareRestriction} function.
     *
     * @param firmwareRestriction a {@link FirmwareRestriction} for applying an additional
     *            restriction function on the firmware (not null)
     * @return the updated builder
     * @throws IllegalArgumentException if the given function is null
     */
    public FirmwareBuilder withFirmwareRestriction(FirmwareRestriction firmwareRestriction) {
        checkNotNull(firmwareRestriction, "Firmware restriction function");
        this.firmwareRestriction = firmwareRestriction;
        return this;
    }

    private void checkNotNull(@Nullable Object object, String argumentName) {
        if (object == null) {
            throw new IllegalArgumentException(argumentName + " must not be null.");
        }
    }

    private void checkNotNullOrEmpty(@Nullable String string, String argumentName) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException(argumentName + " must not be null or empty.");
        }
    }

    /**
     * Builds the firmware.
     *
     * @return the firmware instance based on this builder
     * @throws IllegalArgumentException when the model restricted property ({@link #withModelRestricted(boolean)}) is
     *             set to true, but the model ({@link #withModel(String)}) is not set
     */
    public Firmware build() {
        if (modelRestricted && StringUtils.isEmpty(model)) {
            throw new IllegalArgumentException("Cannot create model restricted firmware without model");
        }

        return new FirmwareImpl(thingTypeUID, vendor, model, modelRestricted, description, version, prerequisiteVersion,
                firmwareRestriction, changelog, onlineChangelog, inputStream, md5Hash, properties);
    }
}
