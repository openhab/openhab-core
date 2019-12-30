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
package org.openhab.core.thing.firmware;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;

import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.binding.firmware.FirmwareBuilder;

/**
 * @author Thomas Höfer - Initial contribution
 */
public class Constants {

    public static final String UNKNOWN = "unknown";
    public static final URI CONFIG_URI = newURI("simple:simple");
    public static final String BINDING_ID = "simpleBinding";
    public static final String THING_TYPE_ID1 = "simpleThingType1";
    public static final String THING_TYPE_ID2 = "simpleThingType2";
    public static final String THING_TYPE_ID3 = "simpleThingType3";
    public static final String THING_TYPE_ID_WITHOUT_FW = "simpleThingTypeWithoutFWs";
    public static final ThingTypeUID THING_TYPE_UID1 = new ThingTypeUID(BINDING_ID, THING_TYPE_ID1);
    public static final ThingTypeUID THING_TYPE_UID2 = new ThingTypeUID(BINDING_ID, THING_TYPE_ID2);
    public static final ThingTypeUID THING_TYPE_UID3 = new ThingTypeUID(BINDING_ID, THING_TYPE_ID3);
    public static final ThingTypeUID THING_TYPE_UID_WITHOUT_FW = new ThingTypeUID(BINDING_ID, THING_TYPE_ID_WITHOUT_FW);
    public static final ThingTypeUID UNKNOWN_THING_TYPE_UID = new ThingTypeUID(BINDING_ID, UNKNOWN);
    public static final String THING1_ID = "simpleThing1";
    public static final String THING2_ID = "simpleThing2";
    public static final String THING3_ID = "simpleThing3";
    public static final String THING4_ID = "simpleThing4";
    public static final String THING5_ID = "simpleThing5";
    public static final ThingUID THING1_UID = new ThingUID(THING_TYPE_UID1, THING1_ID);
    public static final ThingUID THING2_UID = new ThingUID(THING_TYPE_UID1, THING2_ID);
    public static final ThingUID THING3_UID = new ThingUID(THING_TYPE_UID2, THING3_ID);
    public static final ThingUID THING4_UID = new ThingUID(THING_TYPE_UID3, THING4_ID);
    public static final ThingUID UNKNOWN_THING_UID = new ThingUID(THING_TYPE_UID1, UNKNOWN);

    public static final String VENDOR1 = "vendor1";
    public static final String VENDOR2 = "vendor2";
    public static final String MODEL1 = "model1";
    public static final String MODEL2 = "model2";

    public static final String V009 = "0.0.9";
    public static final String V111 = "1.1.1";
    public static final String V111_FIX = "1.1.1-fix";
    public static final String V112 = "1.1.2";
    public static final String V113 = "1.1.3";
    public static final String V120 = "1.2.0";
    public static final String VALPHA = "alpha";
    public static final String VBETA = "beta";
    public static final String VGAMMA = "gamma";

    public static final String UNKNOWN_FIRMWARE_VERSION = "10.9.8.7";

    public static final Firmware FW009_EN = FirmwareBuilder.create(THING_TYPE_UID1, V009).withVendor(VENDOR1)
            .withModel(MODEL1).withDescription("English description v0.0.9").withChangelog("English changelog v0.0.9")
            .withOnlineChangelog(newURL("http://url/changelog-v0.0.9?lang=en")).build();
    public static final Firmware FW111_EN = FirmwareBuilder.create(THING_TYPE_UID1, V111).withVendor(VENDOR1)
            .withModel(MODEL1).withDescription("English description v1.1.1").withChangelog("English changelog v1.1.1")
            .withOnlineChangelog(newURL("http://url/changelog-v1.1.1?lang=en")).build();
    public static final Firmware FW111_FIX_EN = FirmwareBuilder.create(THING_TYPE_UID1, V111_FIX).withVendor(VENDOR1)
            .withModel(MODEL1).withDescription("English description v1.1.1-fix")
            .withChangelog("English changelog v1.1.1-fix")
            .withOnlineChangelog(newURL("http://url/changelog-v1.1.1-fix?lang=en")).build();
    public static final Firmware FW112_EN = FirmwareBuilder.create(THING_TYPE_UID1, V112).withVendor(VENDOR1)
            .withModel(MODEL1).withDescription("English description v1.1.2").withChangelog("English changelog v1.1.2")
            .withOnlineChangelog(newURL("http://url/changelog-v1.1.2?lang=en")).build();
    public static final Firmware FW113_EN = FirmwareBuilder.create(THING_TYPE_UID1, V113).withVendor(VENDOR1)
            .withModel(MODEL1).withDescription("English description v1.1.3").withChangelog("English changelog v1.1.3")
            .withOnlineChangelog(newURL("http://url/changelog-v1.1.3?lang=en"))
            .withPrerequisiteVersion(FW111_FIX_EN.getVersion()).build();

    public static final Firmware FW111_DE = FirmwareBuilder.create(THING_TYPE_UID1, V111).withVendor(VENDOR1)
            .withModel(MODEL1).withDescription("German description v1.1.1").withChangelog("German changelog v1.1.1")
            .withOnlineChangelog(newURL("http://url/changelog-v1.1.1?lang=de")).build();
    public static final Firmware FW111_FIX_DE = FirmwareBuilder.create(THING_TYPE_UID1, V111_FIX).withVendor(VENDOR1)
            .withModel(MODEL1).withDescription("German description v1.1.1-fix")
            .withChangelog("German changelog v1.1.1-fix")
            .withOnlineChangelog(newURL("http://url/changelog-v1.1.1-fix?lang=de")).build();
    public static final Firmware FW112_DE = FirmwareBuilder.create(THING_TYPE_UID1, V112).withVendor(VENDOR1)
            .withModel(MODEL1).withDescription("German description v1.1.2").withChangelog("German changelog v1.1.2")
            .withOnlineChangelog(newURL("http://url/changelog-v1.1.2?lang=de")).build();

    public static final Firmware FWALPHA_EN = FirmwareBuilder.create(THING_TYPE_UID2, VALPHA).withVendor(VENDOR2)
            .withModel(MODEL2).withDescription("English description valpha").withChangelog("English changelog valpha")
            .withOnlineChangelog(newURL("http://url/changelog-valpha?lang=en")).build();

    public static final Firmware FWBETA_EN = FirmwareBuilder.create(THING_TYPE_UID2, VBETA).withVendor(VENDOR2)
            .withModel(MODEL2).withDescription("English description vbeta").withChangelog("English changelog vbeta")
            .withOnlineChangelog(newURL("http://url/changelog-vbeta?lang=en"))
            .withProperties(Collections.singletonMap(Firmware.PROPERTY_REQUIRES_FACTORY_RESET, "true")).build();
    public static final Firmware FWGAMMA_EN = FirmwareBuilder.create(THING_TYPE_UID2, VGAMMA).withVendor(VENDOR2)
            .withModel(MODEL2).withDescription("English description vgamma").withChangelog("English changelog vgamma")
            .withOnlineChangelog(newURL("http://url/changelog-vgamma?lang=en"))
            .withProperties(new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
                {
                    put("prop1", "a");
                    put("prop2", "b");
                }
            }).build();

    public static final Firmware FWALPHA_DE = FirmwareBuilder.create(THING_TYPE_UID2, VALPHA).withVendor(VENDOR2)
            .withModel(MODEL2).withDescription("German description valpha").withChangelog("German changelog valpha")
            .withOnlineChangelog(newURL("http://url/changelog-valpha?lang=de")).build();
    public static final Firmware FWBETA_DE = FirmwareBuilder.create(THING_TYPE_UID2, VBETA).withVendor(VENDOR2)
            .withModel(MODEL2).withDescription("German description vbeta").withChangelog("German changelog vbeta")
            .withOnlineChangelog(newURL("http://url/changelog-vbeta?lang=de"))
            .withProperties(Collections.singletonMap(Firmware.PROPERTY_REQUIRES_FACTORY_RESET, "true")).build();
    public static final Firmware FWGAMMA_DE = FirmwareBuilder.create(THING_TYPE_UID2, VGAMMA).withVendor(VENDOR2)
            .withModel(MODEL2).withDescription("German description vgamma").withChangelog("German changelog vgamma")
            .withOnlineChangelog(newURL("http://url/changelog-vgamma?lang=de"))
            .withProperties(new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
                {
                    put("prop1", "a");
                    put("prop2", "b");
                }
            }).build();

    public static final Firmware FW120_EN = FirmwareBuilder.create(THING_TYPE_UID3, V120).withVendor(VENDOR1)
            .withModel(MODEL1).withDescription("English description v1.2.0").withChangelog("English changelog v1.2.0")
            .withOnlineChangelog(newURL("http://url/changelog-v1.2.0?lang=en")).build();

    public static final Firmware FWALPHA_RESTRICTED_TO_MODEL2 = FirmwareBuilder.create(THING_TYPE_UID1, VALPHA)
            .withVendor(VENDOR2).withModel(MODEL2).withDescription("English description valpha")
            .withChangelog("English changelog valpha")
            .withOnlineChangelog(newURL("http://url/changelog-valpha?lang=en")).withModelRestricted(true).build();

    private static URL newURL(String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static URI newURI(String urlString) {
        try {
            return new URI(urlString);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
