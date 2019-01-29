/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.binding.firmware;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Test;

/**
 * Testing the {@link Firmware} domain object.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Henning Sudbrock - Migrated from Groovy to Java
 * @author Dimitar Ivanov - replaced Firmware UID with thing UID and firmware version
 */
public class FirmwareTest extends JavaOSGiTest {

    private static final String FILE_NAME = "firmware.txt";

    private static final ThingTypeUID thingTypeUID = new ThingTypeUID("binding", "thingType");

    private static final Firmware valpha = FirmwareBuilder.create(thingTypeUID, "alpha").build();
    private static final Firmware valpha1 = FirmwareBuilder.create(thingTypeUID, "alpha1").build();
    private static final Firmware vbeta = FirmwareBuilder.create(thingTypeUID, "beta")
            .withPrerequisiteVersion(valpha1.getVersion()).build();
    private static final Firmware vbetafix = FirmwareBuilder.create(thingTypeUID, "beta-fix").build();
    private static final Firmware vgamma = FirmwareBuilder.create(thingTypeUID, "gamma")
            .withPrerequisiteVersion(vbetafix.getVersion()).build();
    private static final Firmware vdelta = FirmwareBuilder.create(thingTypeUID, "delta").build();

    private static final Firmware xyz = FirmwareBuilder.create(thingTypeUID, "xyz_1").build();
    private static final Firmware abc = FirmwareBuilder.create(thingTypeUID, "abc.2").build();

    private static final Firmware v0 = FirmwareBuilder.create(thingTypeUID, "0").build();
    private static final Firmware v0dot0dot9 = FirmwareBuilder.create(thingTypeUID, "0.0.9").build();
    private static final Firmware v1 = FirmwareBuilder.create(thingTypeUID, "1").build();
    private static final Firmware v1dot0dot0 = FirmwareBuilder.create(thingTypeUID, "1.0.0").build();
    private static final Firmware v1dot0dot1 = FirmwareBuilder.create(thingTypeUID, "1.0.1").build();
    private static final Firmware v1dot0dot2 = FirmwareBuilder.create(thingTypeUID, "1.0.2")
            .withPrerequisiteVersion(v1dot0dot1.getVersion()).build();
    private static final Firmware v1dot0dot2dashfix = FirmwareBuilder.create(thingTypeUID, "1.0.2-fix").build();
    private static final Firmware v1dot0dot3 = FirmwareBuilder.create(thingTypeUID, "1.0.3")
            .withPrerequisiteVersion(v1dot0dot2dashfix.getVersion()).build();
    private static final Firmware v1dash1 = FirmwareBuilder.create(thingTypeUID, "1-1").build();
    private static final Firmware v1dot1dot0 = FirmwareBuilder.create(thingTypeUID, "1.1.0")
            .withPrerequisiteVersion(v1dot0dot2dashfix.getVersion()).build();
    private static final Firmware v1dot2dot0 = FirmwareBuilder.create(thingTypeUID, "1.2.0").build();
    private static final Firmware v1dot10 = FirmwareBuilder.create(thingTypeUID, "1.10").build();
    private static final Firmware v1dot10dot0 = FirmwareBuilder.create(thingTypeUID, "1.10.0").build();
    private static final Firmware v1dash11dot2_1 = FirmwareBuilder.create(thingTypeUID, "1-11.2_1").build();
    private static final Firmware v1dot11_2dasha = FirmwareBuilder.create(thingTypeUID, "1.11_2-a").build();
    private static final Firmware v2dot0dot0 = FirmwareBuilder.create(thingTypeUID, "2.0.0")
            .withPrerequisiteVersion(v1dot11_2dasha.getVersion()).build();

    private static final Firmware combined1 = FirmwareBuilder.create(thingTypeUID, "1.2.3-2.3.4").build();
    private static final Firmware combined2 = FirmwareBuilder.create(thingTypeUID, "1.2.3-2.4.1").build();
    private static final Firmware combined3 = FirmwareBuilder.create(thingTypeUID, "1.3.1-2.3.4").build();
    private static final Firmware combined4 = FirmwareBuilder.create(thingTypeUID, "1.3.1-2.4.1").build();

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
        assertThat(v2dot0dot0.isSuccessorVersion(v1dot11_2dasha.getVersion()), is(true));
        assertThat(v1dot11_2dasha.isSuccessorVersion(v2dot0dot0.getVersion()), is(false));
        assertThat(v1dot11_2dasha.isSuccessorVersion(v1dot11_2dasha.getVersion()), is(false));

        assertThat(v1dot11_2dasha.isSuccessorVersion(v1dash11dot2_1.getVersion()), is(true));
        assertThat(v1dash11dot2_1.isSuccessorVersion(v1dot10dot0.getVersion()), is(true));
        assertThat(v1dot10dot0.isSuccessorVersion(v1dot10.getVersion()), is(true));
        assertThat(v1dot10.isSuccessorVersion(v1dot2dot0.getVersion()), is(true));
        assertThat(v1dot2dot0.isSuccessorVersion(v1dot1dot0.getVersion()), is(true));
        assertThat(v1dot1dot0.isSuccessorVersion(v1dash1.getVersion()), is(true));
        assertThat(v1dash1.isSuccessorVersion(v1dot0dot3.getVersion()), is(true));
        assertThat(v1dot0dot3.isSuccessorVersion(v1dot0dot2dashfix.getVersion()), is(true));
        assertThat(v1dot0dot2dashfix.isSuccessorVersion(v1dot0dot2.getVersion()), is(true));
        assertThat(v1dot0dot2.isSuccessorVersion(v1dot0dot1.getVersion()), is(true));
        assertThat(v1dot0dot1.isSuccessorVersion(v1dot0dot0.getVersion()), is(true));
        assertThat(v1dot0dot1.isSuccessorVersion(v1.getVersion()), is(true));
        assertThat(v1.isSuccessorVersion(v0dot0dot9.getVersion()), is(true));
        assertThat(v0dot0dot9.isSuccessorVersion(v0.getVersion()), is(true));

        assertThat(vgamma.isSuccessorVersion(vbetafix.getVersion()), is(true));
        assertThat(vbetafix.isSuccessorVersion(vbeta.getVersion()), is(true));
        assertThat(vbeta.isSuccessorVersion(valpha1.getVersion()), is(true));
        assertThat(valpha1.isSuccessorVersion(valpha.getVersion()), is(true));

        assertThat(xyz.isSuccessorVersion(abc.getVersion()), is(true));
        assertThat(abc.isSuccessorVersion(v2dot0dot0.getVersion()), is(true));
        assertThat(abc.isSuccessorVersion(xyz.getVersion()), is(false));

        assertThat(vdelta.isSuccessorVersion(v0dot0dot9.getVersion()), is(true));
        assertThat(v0dot0dot9.isSuccessorVersion(vdelta.getVersion()), is(false));
        assertThat(vdelta.isSuccessorVersion(vgamma.getVersion()), is(false));

        assertThat(vdelta.isSuccessorVersion(null), is(false));

        assertThat(combined4.isSuccessorVersion(combined3.getVersion()), is(true));
        assertThat(combined3.isSuccessorVersion(combined4.getVersion()), is(false));

        assertThat(combined3.isSuccessorVersion(combined2.getVersion()), is(true));
        assertThat(combined2.isSuccessorVersion(combined3.getVersion()), is(false));

        assertThat(combined2.isSuccessorVersion(combined1.getVersion()), is(true));
        assertThat(combined1.isSuccessorVersion(combined2.getVersion()), is(false));
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
        Firmware firmware = FirmwareBuilder.create(thingTypeUID, "1.2.3")
                .withFirmwareRestriction(thing -> thing.getLabel().equals(label)).build();

        Thing thing = ThingBuilder.create(thingTypeUID, "thing").withLabel(label).build();

        assertThat(firmware.isSuitableFor(thing), is(true));
    }

    @Test
    public void firmwareRestrictedFirmwareIsNotSuitable() {
        Firmware firmware = FirmwareBuilder.create(thingTypeUID, "1.2.3")
                .withFirmwareRestriction(thing -> thing.getLabel().equals("label")).build();

        Thing thing = ThingBuilder.create(thingTypeUID, "thing").withLabel("invalid_label").build();

        assertThat(firmware.isSuitableFor(thing), is(false));
    }

    @Test
    public void assertThatColonCanBeUsedAsPartOfTheFirmwareVersion() {
        FirmwareBuilder.create(thingTypeUID, "1.2:3");
    }

    @Test
    public void assertThatFirmwareWithValidMD5HashValueDoesNotThrowExceptionForGetBytes() throws IOException {
        Firmware firmware = FirmwareBuilder.create(thingTypeUID, "1")
                .withInputStream(bundleContext.getBundle().getResource(FILE_NAME).openStream())
                .withMd5Hash("78805a221a988e79ef3f42d7c5bfd418").build();

        byte[] bytes = firmware.getBytes();
        assertThat(bytes, is(notNullValue()));
    }

    @Test(expected = IllegalStateException.class)
    public void assertThatFirmwareWithInvalidMD5HashValueThrowsExceptionForGetBytes() throws IOException {
        Firmware firmware = FirmwareBuilder.create(thingTypeUID, "1")
                .withInputStream(bundleContext.getBundle().getResource(FILE_NAME).openStream())
                .withMd5Hash("78805a221a988e79ef3f42d7c5bfd419").build();
        firmware.getBytes();
    }

    @Test
    public void assertThatFirmwareWithoutMD5HashValueDoesNotThrowExceptionForGetBytes() throws IOException {
        Firmware firmware = FirmwareBuilder.create(thingTypeUID, "1")
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

        Firmware firmware1 = FirmwareBuilder.create(thingTypeUID, "1").withInputStream(openStream)
                .withChangelog(changelog).withDescription(description).withModel(model)
                .withModelRestricted(modelRestricted).withOnlineChangelog(onlineChangelog)
                .withPrerequisiteVersion(prerequisiteVersion).withVendor(vendor).withProperties(properties).build();

        Firmware firmware2 = FirmwareBuilder.create(thingTypeUID, "1").withInputStream(openStream)
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

        Firmware firmware1 = FirmwareBuilder.create(thingTypeUID, "1").withInputStream(openStream)
                .withChangelog(changelog).withDescription(description).withModel(model)
                .withModelRestricted(modelRestricted).withOnlineChangelog(onlineChangelog)
                .withPrerequisiteVersion(prerequisiteVersion).withVendor(vendor).withProperties(properties).build();

        Firmware firmware2 = FirmwareBuilder.create(thingTypeUID, "1").withInputStream(openStream)
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
        FirmwareBuilder.create(thingTypeUID, giveNull());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyVersionOnCreation() {
        FirmwareBuilder.create(thingTypeUID, "");
    }

    private Firmware firmwareWithVersion(String version) {
        return FirmwareBuilder.create(thingTypeUID, version).build();
    }

    private Firmware firmwareWithVersionAndPrerequisiteVersion(String version, String prerequisiteVersion) {
        return FirmwareBuilder.create(thingTypeUID, version).withPrerequisiteVersion(prerequisiteVersion).build();
    }

    private Thing thingWithFirmwareVersion(String version) {
        Map<String, String> properties = new HashMap<>();
        properties.put(Thing.PROPERTY_FIRMWARE_VERSION, version);
        return ThingBuilder.create(thingTypeUID, "testThing").withProperties(properties).build();
    }
}
