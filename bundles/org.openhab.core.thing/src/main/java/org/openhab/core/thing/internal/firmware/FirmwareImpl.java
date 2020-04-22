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
package org.openhab.core.thing.internal.firmware;

import static org.openhab.core.thing.Thing.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.binding.firmware.FirmwareRestriction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link Firmware}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - FirmwareUID is replaced by ThingTypeUID and firmware version
 */
@NonNullByDefault
public final class FirmwareImpl implements Firmware {

    /** The key for the requires a factory reset property. */
    public static final String PROPERTY_REQUIRES_FACTORY_RESET = "requiresFactoryReset";

    private final Logger logger = LoggerFactory.getLogger(FirmwareImpl.class);

    private final ThingTypeUID thingTypeUID;
    private final @Nullable String vendor;
    private final @Nullable String model;
    private final boolean modelRestricted;
    private final @Nullable String description;
    private final Version version;
    private final @Nullable Version prerequisiteVersion;
    private final FirmwareRestriction firmwareRestriction;
    private final @Nullable String changelog;
    private final @Nullable URL onlineChangelog;
    private final @Nullable transient InputStream inputStream;
    private final @Nullable String md5Hash;
    private final Map<String, String> properties;

    private transient byte @Nullable [] bytes;

    /**
     * Constructs new firmware by the given meta information.
     *
     * @param thingTypeUID thing type UID, that this firmware is associated with (not null)
     * @param vendor the vendor of the firmware (can be null)
     * @param model the model of the firmware (can be null)
     * @param modelRestricted whether the firmware is restricted to a particular model
     * @param description the description of the firmware (can be null)
     * @param version the version of the firmware (not null)
     * @param prerequisiteVersion the prerequisite version of the firmware (can be null)
     * @param firmwareRestriction {@link FirmwareRestriction} for applying an additional restriction on
     *            the firmware (can be null). If null, a default function will be used to return always true
     * @param changelog the changelog of the firmware (can be null)
     * @param onlineChangelog the URL the an online changelog of the firmware (can be null)
     * @param inputStream the input stream for the binary content of the firmware (can be null)
     * @param md5Hash the MD5 hash value of the firmware (can be null)
     * @param properties the immutable properties of the firmware (can be null)
     * @throws IllegalArgumentException if the ThingTypeUID or the firmware version are null
     */
    public FirmwareImpl(ThingTypeUID thingTypeUID, @Nullable String vendor, @Nullable String model,
            boolean modelRestricted, @Nullable String description, String version, @Nullable String prerequisiteVersion,
            @Nullable FirmwareRestriction firmwareRestriction, @Nullable String changelog,
            @Nullable URL onlineChangelog, @Nullable InputStream inputStream, @Nullable String md5Hash,
            @Nullable Map<String, String> properties) {
        ParameterChecks.checkNotNull(thingTypeUID, "ThingTypeUID");
        this.thingTypeUID = thingTypeUID;
        ParameterChecks.checkNotNullOrEmpty(version, "Firmware version");
        this.version = new Version(version);
        this.vendor = vendor;
        this.model = model;
        this.modelRestricted = modelRestricted;
        this.description = description;
        this.prerequisiteVersion = prerequisiteVersion != null ? new Version(prerequisiteVersion) : null;
        this.firmwareRestriction = firmwareRestriction != null ? firmwareRestriction : t -> true;
        this.changelog = changelog;
        this.onlineChangelog = onlineChangelog;
        this.inputStream = inputStream;
        this.md5Hash = md5Hash;
        this.properties = Collections.unmodifiableMap(properties != null ? properties : Collections.emptyMap());
    }

    @Override
    public ThingTypeUID getThingTypeUID() {
        return thingTypeUID;
    }

    @Override
    public @Nullable String getVendor() {
        return vendor;
    }

    @Override
    public @Nullable String getModel() {
        return model;
    }

    @Override
    public boolean isModelRestricted() {
        return modelRestricted;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version.toString();
    }

    @Override
    public @Nullable String getPrerequisiteVersion() {
        return (prerequisiteVersion != null) ? prerequisiteVersion.toString() : null;
    }

    @Override
    public FirmwareRestriction getFirmwareRestriction() {
        return firmwareRestriction;
    }

    @Override
    public @Nullable String getChangelog() {
        return changelog;
    }

    @Override
    public @Nullable URL getOnlineChangelog() {
        return onlineChangelog;
    }

    @Override
    public @Nullable InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public @Nullable String getMd5Hash() {
        return md5Hash;
    }

    @Override
    public synchronized byte @Nullable [] getBytes() {
        if (inputStream == null) {
            return null;
        }

        if (bytes == null) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");

                try (DigestInputStream dis = new DigestInputStream(inputStream, md)) {
                    bytes = dis.readAllBytes();
                } catch (IOException ioEx) {
                    logger.error("Cannot read firmware {}.", this, ioEx);
                    return null;
                }

                byte[] digest = md.digest();

                if (md5Hash != null && digest != null) {
                    StringBuilder digestString = new StringBuilder();
                    for (byte b : digest) {
                        digestString.append(String.format("%02x", b));
                    }

                    if (!md5Hash.equals(digestString.toString())) {
                        bytes = null;
                        throw new IllegalStateException(
                                String.format("Invalid MD5 checksum. Expected %s, but was %s.", md5Hash, digestString));
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                logger.error("Cannot calculate MD5 checksum.", e);
                bytes = null;
                return null;
            }
        }

        return bytes;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public boolean isSuccessorVersion(@Nullable String firmwareVersion) {
        if (firmwareVersion == null) {
            return false;
        }
        return version.compare(new Version(firmwareVersion)) > 0;
    }

    @Override
    public boolean isSuitableFor(Thing thing) {
        return hasSameThingType(thing) && hasRequiredModel(thing) && firmwareOnThingIsHighEnough(thing)
                && firmwareRestriction.apply(thing);
    }

    @Override
    public int compareTo(Firmware firmware) {
        return -version.compare(new Version(firmware.getVersion()));
    }

    private boolean hasSameThingType(Thing thing) {
        return Objects.equals(this.getThingTypeUID(), thing.getThingTypeUID());
    }

    private boolean hasRequiredModel(Thing thing) {
        if (isModelRestricted()) {
            return Objects.equals(this.getModel(), thing.getProperties().get(PROPERTY_MODEL_ID));
        } else {
            return true;
        }
    }

    private boolean firmwareOnThingIsHighEnough(Thing thing) {
        if (prerequisiteVersion == null) {
            return true;
        } else {
            String firmwareOnThing = thing.getProperties().get(PROPERTY_FIRMWARE_VERSION);
            return firmwareOnThing != null && new Version(firmwareOnThing).compare(prerequisiteVersion) >= 0;
        }
    }

    private static class Version {

        private static final int NO_INT = -1;

        private final String versionString;
        private final String[] parts;

        private Version(String versionString) {
            this.versionString = versionString;
            this.parts = versionString.split("-|_|\\.");
        }

        private int compare(@Nullable Version theVersion) {
            if (theVersion == null) {
                return 1;
            }

            int max = Math.max(parts.length, theVersion.parts.length);

            for (int i = 0; i < max; i++) {
                String partA = i < parts.length ? parts[i] : null;
                String partB = i < theVersion.parts.length ? theVersion.parts[i] : null;

                Integer intA = partA != null && isInt(partA) ? Integer.parseInt(partA) : NO_INT;
                Integer intB = partB != null && isInt(partB) ? Integer.parseInt(partB) : NO_INT;

                if (intA != NO_INT && intB != NO_INT) {
                    if (intA < intB) {
                        return -1;
                    }
                    if (intA > intB) {
                        return 1;
                    }
                } else if (partA == null || partB == null) {
                    if (partA == null) {
                        return -1;
                    }
                    if (partB == null) {
                        return 1;
                    }
                } else {
                    int result = partA.compareTo(partB);
                    if (result != 0) {
                        return result;
                    }
                }
            }

            return 0;
        }

        private boolean isInt(String s) {
            return s.matches("^-?\\d+$");
        }

        @Override
        public String toString() {
            return versionString;
        }

        @Override
        public int hashCode() {
            return versionString.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (other == null) {
                return false;
            } else if (!(other instanceof Version)) {
                return false;
            } else {
                return Objects.equals(this.versionString, ((Version) other).versionString);
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changelog == null) ? 0 : changelog.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((md5Hash == null) ? 0 : md5Hash.hashCode());
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        result = prime * result + Boolean.hashCode(modelRestricted);
        result = prime * result + ((onlineChangelog == null) ? 0 : onlineChangelog.hashCode());
        result = prime * result + ((prerequisiteVersion == null) ? 0 : prerequisiteVersion.hashCode());
        result = prime * result + ((thingTypeUID == null) ? 0 : thingTypeUID.hashCode());
        result = prime * result + ((vendor == null) ? 0 : vendor.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
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
        FirmwareImpl other = (FirmwareImpl) obj;
        if (changelog == null) {
            if (other.changelog != null) {
                return false;
            }
        } else if (!changelog.equals(other.changelog)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (md5Hash == null) {
            if (other.md5Hash != null) {
                return false;
            }
        } else if (!md5Hash.equals(other.md5Hash)) {
            return false;
        }
        if (model == null) {
            if (other.model != null) {
                return false;
            }
        } else if (!model.equals(other.model)) {
            return false;
        }
        if (modelRestricted != other.modelRestricted) {
            return false;
        }
        if (onlineChangelog == null) {
            if (other.onlineChangelog != null) {
                return false;
            }
        } else if (!onlineChangelog.equals(other.onlineChangelog)) {
            return false;
        }
        if (prerequisiteVersion == null) {
            if (other.prerequisiteVersion != null) {
                return false;
            }
        } else if (!prerequisiteVersion.equals(other.prerequisiteVersion)) {
            return false;
        }
        if (thingTypeUID == null) {
            if (other.thingTypeUID != null) {
                return false;
            }
        } else if (!thingTypeUID.equals(other.thingTypeUID)) {
            return false;
        }
        if (vendor == null) {
            if (other.vendor != null) {
                return false;
            }
        } else if (!vendor.equals(other.vendor)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        if (properties == null) {
            if (other.properties != null) {
                return false;
            }
        } else if (!properties.equals(other.properties)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FirmwareImpl [thingTypeUID=" + thingTypeUID + ", vendor=" + vendor + ", model=" + model
                + ", modelRestricted=" + modelRestricted + ", description=" + description + ", version=" + version
                + ", prerequisiteVersion=" + prerequisiteVersion + ", changelog=" + changelog + ", onlineChangelog="
                + onlineChangelog + ", md5Hash=" + md5Hash + ", properties=" + properties + "]";
    }
}
