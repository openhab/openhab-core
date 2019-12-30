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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;

/**
 * Testing the {@link Firmware} domain object.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Henning Sudbrock - Migrated from Groovy to Java
 * @author Dimitar Ivanov - replaced Firmware UID with thing UID and firmware version
 */
public class FirmwareTest extends JavaOSGiTest {

    private static final String FILE_NAME = "firmware.txt";

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding", "thingType");

    private static final Firmware VALPHA = FirmwareBuilder.create(THING_TYPE_UID, "alpha").build();
    private static final Firmware VALPHA1 = FirmwareBuilder.create(THING_TYPE_UID, "alpha1").build();
    private static final Firmware VBETA = FirmwareBuilder.create(THING_TYPE_UID, "beta")
            .withPrerequisiteVersion(VALPHA1.getVersion()).build();
    private static final Firmware VBETAFIX = FirmwareBuilder.create(THING_TYPE_UID, "beta-fix").build();
    private static final Firmware VGAMMA = FirmwareBuilder.create(THING_TYPE_UID, "gamma")
            .withPrerequisiteVersion(VBETAFIX.getVersion()).build();
    private static final Firmware VDELTA = FirmwareBuilder.create(THING_TYPE_UID, "delta").build();

    private static final Firmware XYX = FirmwareBuilder.create(THING_TYPE_UID, "xyz_1").build();
    private static final Firmware ABC = FirmwareBuilder.create(THING_TYPE_UID, "abc.2").build();

    private static final Firmware V0 = FirmwareBuilder.create(THING_TYPE_UID, "0").build();
    private static final Firmware V0_DOT_0_DOT_9 = FirmwareBuilder.create(THING_TYPE_UID, "0.0.9").build();
    private static final Firmware V1 = FirmwareBuilder.create(THING_TYPE_UID, "1").build();
    private static final Firmware V1_DOT_0_DOT_0 = FirmwareBuilder.create(THING_TYPE_UID, "1.0.0").build();
    private static final Firmware V1_DOT_0_DOT_1 = FirmwareBuilder.create(THING_TYPE_UID, "1.0.1").build();
    private static final Firmware V1_DOT_0_DOT_2 = FirmwareBuilder.create(THING_TYPE_UID, "1.0.2")
            .withPrerequisiteVersion(V1_DOT_0_DOT_1.getVersion()).build();
    private static final Firmware V1_DOT_0_DOT_2_DASH_FIX = FirmwareBuilder.create(THING_TYPE_UID, "1.0.2-fix").build();
    private static final Firmware V1_DOT_0_DOT_3 = FirmwareBuilder.create(THING_TYPE_UID, "1.0.3")
            .withPrerequisiteVersion(V1_DOT_0_DOT_2_DASH_FIX.getVersion()).build();
    private static final Firmware V1_DASH_1 = FirmwareBuilder.create(THING_TYPE_UID, "1-1").build();
    private static final Firmware V1_DOT_1_DOT_0 = FirmwareBuilder.create(THING_TYPE_UID, "1.1.0")
            .withPrerequisiteVersion(V1_DOT_0_DOT_2_DASH_FIX.getVersion()).build();
    private static final Firmware V1_DOT_2_DOT_0 = FirmwareBuilder.create(THING_TYPE_UID, "1.2.0").build();
    private static final Firmware V1_DOT_10 = FirmwareBuilder.create(THING_TYPE_UID, "1.10").build();
    private static final Firmware V1_DOT_10_DOT_0 = FirmwareBuilder.create(THING_TYPE_UID, "1.10.0").build();
    private static final Firmware V1_DASH_11_DOT_2_1 = FirmwareBuilder.create(THING_TYPE_UID, "1-11.2_1").build();
    private static final Firmware V1_DOT_11_2_DASH_A = FirmwareBuilder.create(THING_TYPE_UID, "1.11_2-a").build();
    private static final Firmware V2_DOT_0_DOT_0 = FirmwareBuilder.create(THING_TYPE_UID, "2.0.0")
            .withPrerequisiteVersion(V1_DOT_11_2_DASH_A.getVersion()).build();

    private static final Firmware COMBINED1 = FirmwareBuilder.create(THING_TYPE_UID, "1.2.3-2.3.4").build();
    private static final Firmware COMBINED2 = FirmwareBuilder.create(THING_TYPE_UID, "1.2.3-2.4.1").build();
    private static final Firmware COMBINED3 = FirmwareBuilder.create(THING_TYPE_UID, "1.3.1-2.3.4").build();
    private static final Firmware COMBINED4 = FirmwareBuilder.create(THING_TYPE_UID, "1.3.1-2.4.1").build();

    @Test
    public void testFirmwareBuilder() throws MalformedURLException {
        ThingTypeUID sampleThingTypeUID = new ThingTypeUID("binding", "sampleThingType");
        String version = "1.0.0";
        String changelog = "changelog";
        InputStream inputStream = new BufferedInputStream(System.in);
        String description = "description";
        String model = "model";
        boolean modelRestricted = true;
        URL onlineChangelog = new URL("http://online.changelog");
        String prerequisiteVersion = "0.0.9";
        String md5hash = "123abc";
        String vendor = "vendor";
        FirmwareRestriction restrictionFunction = t -> true;

        Firmware firmware = FirmwareBuilder.create(sampleThingTypeUID, version).withModel(model)
                .withModelRestricted(modelRestricted).withChangelog(changelog).withInputStream(inputStream)
                .withDescription(description).withOnlineChangelog(onlineChangelog)
                .withPrerequisiteVersion(prerequisiteVersion).withFirmwareRestriction(restrictionFunction)
                .withVendor(vendor).withMd5Hash(md5hash).build();

        assertThat(firmware, is(notNullValue()));
        assertThat(firmware.getThingTypeUID(), is(sampleThingTypeUID));
        assertThat(firmware.getChangelog(), is(changelog));
        assertThat(firmware.getInputStream(), is(inputStream));
        assertThat(firmware.getDescription(), is(description));
        assertThat(firmware.getModel(), is(model));
        assertThat(firmware.isModelRestricted(), is(modelRestricted));
        assertThat(firmware.getOnlineChangelog(), is(onlineChangelog));
        assertThat(firmware.getVersion(), is(version));
        assertThat(firmware.getPrerequisiteVersion(), is(prerequisiteVersion));
        assertThat(firmware.getVendor(), is(vendor));
        assertThat(firmware.getMd5Hash(), is(md5hash));
        assertThat(firmware.getFirmwareRestriction(), is(restrictionFunction));
    }

    @Test(expected = IllegalArgumentException.class)
    public void modelRestrictedWithoutModel() {
        ThingTypeUID sampleThingTypeUID = new ThingTypeUID("binding", "sampleThingType");
        String version = "1.0.0";
        FirmwareBuilder.create(sampleThingTypeUID, version).withModelRestricted(true).build();
    }

    @Test
    public void testFirmwareSuccessorVersion() {
        assertThat(V2_DOT_0_DOT_0.isSuccessorVersion(V1_DOT_11_2_DASH_A.getVersion()), is(true));
        assertThat(V1_DOT_11_2_DASH_A.isSuccessorVersion(V2_DOT_0_DOT_0.getVersion()), is(false));
        assertThat(V1_DOT_11_2_DASH_A.isSuccessorVersion(V1_DOT_11_2_DASH_A.getVersion()), is(false));

        assertThat(V1_DOT_11_2_DASH_A.isSuccessorVersion(V1_DASH_11_DOT_2_1.getVersion()), is(true));
        assertThat(V1_DASH_11_DOT_2_1.isSuccessorVersion(V1_DOT_10_DOT_0.getVersion()), is(true));
        assertThat(V1_DOT_10_DOT_0.isSuccessorVersion(V1_DOT_10.getVersion()), is(true));
        assertThat(V1_DOT_10.isSuccessorVersion(V1_DOT_2_DOT_0.getVersion()), is(true));
        assertThat(V1_DOT_2_DOT_0.isSuccessorVersion(V1_DOT_1_DOT_0.getVersion()), is(true));
        assertThat(V1_DOT_1_DOT_0.isSuccessorVersion(V1_DASH_1.getVersion()), is(true));
        assertThat(V1_DASH_1.isSuccessorVersion(V1_DOT_0_DOT_3.getVersion()), is(true));
        assertThat(V1_DOT_0_DOT_3.isSuccessorVersion(V1_DOT_0_DOT_2_DASH_FIX.getVersion()), is(true));
        assertThat(V1_DOT_0_DOT_2_DASH_FIX.isSuccessorVersion(V1_DOT_0_DOT_2.getVersion()), is(true));
        assertThat(V1_DOT_0_DOT_2.isSuccessorVersion(V1_DOT_0_DOT_1.getVersion()), is(true));
        assertThat(V1_DOT_0_DOT_1.isSuccessorVersion(V1_DOT_0_DOT_0.getVersion()), is(true));
        assertThat(V1_DOT_0_DOT_1.isSuccessorVersion(V1.getVersion()), is(true));
        assertThat(V1.isSuccessorVersion(V0_DOT_0_DOT_9.getVersion()), is(true));
        assertThat(V0_DOT_0_DOT_9.isSuccessorVersion(V0.getVersion()), is(true));

        assertThat(VGAMMA.isSuccessorVersion(VBETAFIX.getVersion()), is(true));
        assertThat(VBETAFIX.isSuccessorVersion(VBETA.getVersion()), is(true));
        assertThat(VBETA.isSuccessorVersion(VALPHA1.getVersion()), is(true));
        assertThat(VALPHA1.isSuccessorVersion(VALPHA.getVersion()), is(true));

        assertThat(XYX.isSuccessorVersion(ABC.getVersion()), is(true));
        assertThat(ABC.isSuccessorVersion(V2_DOT_0_DOT_0.getVersion()), is(true));
        assertThat(ABC.isSuccessorVersion(XYX.getVersion()), is(false));

        assertThat(VDELTA.isSuccessorVersion(V0_DOT_0_DOT_9.getVersion()), is(true));
        assertThat(V0_DOT_0_DOT_9.isSuccessorVersion(VDELTA.getVersion()), is(false));
        assertThat(VDELTA.isSuccessorVersion(VGAMMA.getVersion()), is(false));

        assertThat(VDELTA.isSuccessorVersion(null), is(false));

        assertThat(COMBINED4.isSuccessorVersion(COMBINED3.getVersion()), is(true));
        assertThat(COMBINED3.isSuccessorVersion(COMBINED4.getVersion()), is(false));

        assertThat(COMBINED3.isSuccessorVersion(COMBINED2.getVersion()), is(true));
        assertThat(COMBINED2.isSuccessorVersion(COMBINED3.getVersion()), is(false));

        assertThat(COMBINED2.isSuccessorVersion(COMBINED1.getVersion()), is(true));
        assertThat(COMBINED1.isSuccessorVersion(COMBINED2.getVersion()), is(false));
    }

    @Test
    public void testFirmwareSuitabilityNoPrerequisiteVersion() {
        Firmware firmware = firmwareWithVersion("2.0.0");

        assertTrue(firmware.isSuitableFor(thingWithFirmwareVersion(null)));
        assertTrue(firmware.isSuitableFor(thingWithFirmwareVersion("1.0.0")));
        assertTrue(firmware.isSuitableFor(thingWithFirmwareVersion("2.0.0")));
        assertTrue(firmware.isSuitableFor(thingWithFirmwareVersion("3.0.0")));
    }

    @Test
    public void testFirmwareSuitabilityWithPrerequisiteVersion() {
        Firmware firmware = firmwareWithVersionAndPrerequisiteVersion("3.0.0", "2.0.0");

        assertFalse(firmware.isSuitableFor(thingWithFirmwareVersion(null)));
        assertFalse(firmware.isSuitableFor(thingWithFirmwareVersion("1.0.0")));
        assertTrue(firmware.isSuitableFor(thingWithFirmwareVersion("2.0.0")));
        assertTrue(firmware.isSuitableFor(thingWithFirmwareVersion("3.0.0")));
        assertTrue(firmware.isSuitableFor(thingWithFirmwareVersion("4.0.0")));
    }

    @Test
    public void testFirmwareSuitabilityWithHigherPrerequisiteVersion() {
        Firmware firmware = firmwareWithVersionAndPrerequisiteVersion("1.0.0", "2.0.0");

        assertFalse(firmware.isSuitableFor(thingWithFirmwareVersion(null)));
        assertFalse(firmware.isSuitableFor(thingWithFirmwareVersion("1.0.0")));
        assertTrue(firmware.isSuitableFor(thingWithFirmwareVersion("2.0.0")));
        assertTrue(firmware.isSuitableFor(thingWithFirmwareVersion("3.0.0")));
    }

    @Test
    public void testFirmwareIsNotSuitableForThingWithDifferentThingType() {
        Firmware firmware = FirmwareBuilder.create(new ThingTypeUID("binding:thingTypeA"), "version").build();
        Thing thing = ThingBuilder.create(new ThingTypeUID("binding:thingTypeB"), "thing").build();

        assertThat(firmware.isSuitableFor(thing), is(false));
    }

    @Test
    public void testNotModelRestrictedFirmwareIsSuitableForThingWithSameThingType() {
        Firmware firmware = FirmwareBuilder.create(new ThingTypeUID("binding:thingTypeA"), "version").build();
        Thing thing = ThingBuilder.create(new ThingTypeUID("binding:thingTypeA"), "thing").build();

        assertThat(firmware.isSuitableFor(thing), is(true));
    }

    @Test
    public void testModelRestrictedFirmwareIsSuitableForThingWithSameThingTypeAndSameModel() {
        Firmware firmware = FirmwareBuilder.create(new ThingTypeUID("binding:thingTypeA"), "version")
                .withModelRestricted(true).withModel("someModel").build();
        Thing thing = ThingBuilder.create(new ThingTypeUID("binding:thingTypeA"), "thing").build();
        thing.setProperty(Thing.PROPERTY_MODEL_ID, "someModel");

        assertThat(firmware.isSuitableFor(thing), is(true));
    }

    @Test
    public void testModelRestrictedFirmwareIsNotSuitableForThingWithSameThingTypeAndAnotherModel() {
        Firmware firmware = FirmwareBuilder.create(new ThingTypeUID("binding:thingTypeA"), "version")
                .withModelRestricted(true).withModel("someModel").build();
        Thing thing = ThingBuilder.create(new ThingTypeUID("binding:thingTypeA"), "thing").build();
        thing.setProperty(Thing.PROPERTY_MODEL_ID, "someOtherModel");

        assertThat(firmware.isSuitableFor(thing), is(false));
    }

    @Test
    public void testFirmwareIsSuitableForThingWithSameModelAndVendor() {
        Firmware firmware = FirmwareBuilder.create(new ThingTypeUID("binding:thingTypeA"), "version")
                .withVendor("bindingVendor").build();
        Thing thing = ThingBuilder.create(new ThingTypeUID("binding:thingTypeA"), "thing").build();

        assertThat(firmware.isSuitableFor(thing), is(true));
    }

    @Test
    public void firmwareRestrictedFirmwareIsSuitable() {
        String label = "label";
        Firmware firmware = FirmwareBuilder.create(THING_TYPE_UID, "1.2.3")
                .withFirmwareRestriction(thing -> thing.getLabel().equals(label)).build();

        Thing thing = ThingBuilder.create(THING_TYPE_UID, "thing").withLabel(label).build();

        assertThat(firmware.isSuitableFor(thing), is(true));
    }

    @Test
    public void firmwareRestrictedFirmwareIsNotSuitable() {
        Firmware firmware = FirmwareBuilder.create(THING_TYPE_UID, "1.2.3")
                .withFirmwareRestriction(thing -> thing.getLabel().equals("label")).build();

        Thing thing = ThingBuilder.create(THING_TYPE_UID, "thing").withLabel("invalid_label").build();

        assertThat(firmware.isSuitableFor(thing), is(false));
    }

    @Test
    public void assertThatColonCanBeUsedAsPartOfTheFirmwareVersion() {
        FirmwareBuilder.create(THING_TYPE_UID, "1.2:3");
    }

    @Test
    public void assertThatFirmwareWithValidMD5HashValueDoesNotThrowExceptionForGetBytes() throws IOException {
        Firmware firmware = FirmwareBuilder.create(THING_TYPE_UID, "1")
                .withInputStream(bundleContext.getBundle().getResource(FILE_NAME).openStream())
                .withMd5Hash("78805a221a988e79ef3f42d7c5bfd418").build();

        byte[] bytes = firmware.getBytes();
        assertThat(bytes, is(notNullValue()));
    }

    @Test(expected = IllegalStateException.class)
    public void assertThatFirmwareWithInvalidMD5HashValueThrowsExceptionForGetBytes() throws IOException {
        Firmware firmware = FirmwareBuilder.create(THING_TYPE_UID, "1")
                .withInputStream(bundleContext.getBundle().getResource(FILE_NAME).openStream())
                .withMd5Hash("78805a221a988e79ef3f42d7c5bfd419").build();
        firmware.getBytes();
    }

    @Test
    public void assertThatFirmwareWithoutMD5HashValueDoesNotThrowExceptionForGetBytes() throws IOException {
        Firmware firmware = FirmwareBuilder.create(THING_TYPE_UID, "1")
                .withInputStream(bundleContext.getBundle().getResource(FILE_NAME).openStream()).build();

        byte[] bytes = firmware.getBytes();
        assertThat(bytes, is(notNullValue()));
    }

    @Test
    public void assertDuplicateFirmwaresWithEquals() throws IOException {
        String changelog = "changelog";
        String description = "description";
        String model = "model";
        boolean modelRestricted = true;
        URL onlineChangelog = new URL("https://secure.com/changelog");
        String prerequisiteVersion = "0.1";
        String vendor = "vendor";
        Map<String, String> properties = new HashMap<>();
        properties.put("prop1", "val1");
        properties.put("prop2", "val2");
        InputStream openStream = bundleContext.getBundle().getResource(FILE_NAME).openStream();

        Firmware firmware1 = FirmwareBuilder.create(THING_TYPE_UID, "1").withInputStream(openStream)
                .withChangelog(changelog).withDescription(description).withModel(model)
                .withModelRestricted(modelRestricted).withOnlineChangelog(onlineChangelog)
                .withPrerequisiteVersion(prerequisiteVersion).withVendor(vendor).withProperties(properties).build();

        Firmware firmware2 = FirmwareBuilder.create(THING_TYPE_UID, "1").withInputStream(openStream)
                .withChangelog(changelog).withDescription(description).withModel(model)
                .withModelRestricted(modelRestricted).withOnlineChangelog(onlineChangelog)
                .withPrerequisiteVersion(prerequisiteVersion).withVendor(vendor).withProperties(properties).build();

        assertThat(firmware1.equals(firmware2), is(true));
        assertThat(firmware2.equals(firmware1), is(true));
    }

    @Test
    public void duplicateFirmwaresHashWithoutRestrictions() throws IOException {
        String changelog = "changelog";
        String description = "description";
        String model = "model";
        boolean modelRestricted = true;
        URL onlineChangelog = new URL("https://secure.com/changelog");
        String prerequisiteVersion = "0.1";
        String vendor = "vendor";
        Map<String, String> properties = new HashMap<>();
        properties.put("prop1", "val1");
        properties.put("prop2", "val2");
        InputStream openStream = bundleContext.getBundle().getResource(FILE_NAME).openStream();

        Firmware firmware1 = FirmwareBuilder.create(THING_TYPE_UID, "1").withInputStream(openStream)
                .withChangelog(changelog).withDescription(description).withModel(model)
                .withModelRestricted(modelRestricted).withOnlineChangelog(onlineChangelog)
                .withPrerequisiteVersion(prerequisiteVersion).withVendor(vendor).withProperties(properties).build();

        Firmware firmware2 = FirmwareBuilder.create(THING_TYPE_UID, "1").withInputStream(openStream)
                .withChangelog(changelog).withDescription(description).withModel(model)
                .withModelRestricted(modelRestricted).withOnlineChangelog(onlineChangelog)
                .withPrerequisiteVersion(prerequisiteVersion).withVendor(vendor).withProperties(properties).build();

        assertThat(firmware1.hashCode(), is(firmware2.hashCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullThingTypeOnCreation() {
        FirmwareBuilder.create(giveNull(), "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFirmwareVersionOnCreation() {
        FirmwareBuilder.create(THING_TYPE_UID, giveNull());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyVersionOnCreation() {
        FirmwareBuilder.create(THING_TYPE_UID, "");
    }

    private Firmware firmwareWithVersion(String version) {
        return FirmwareBuilder.create(THING_TYPE_UID, version).build();
    }

    private Firmware firmwareWithVersionAndPrerequisiteVersion(String version, String prerequisiteVersion) {
        return FirmwareBuilder.create(THING_TYPE_UID, version).withPrerequisiteVersion(prerequisiteVersion).build();
    }

    private Thing thingWithFirmwareVersion(String version) {
        Map<String, String> properties = new HashMap<>();
        properties.put(Thing.PROPERTY_FIRMWARE_VERSION, version);
        return ThingBuilder.create(THING_TYPE_UID, "testThing").withProperties(properties).build();
    }
}
