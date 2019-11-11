/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.internal.profiles;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileContext;
import org.eclipse.smarthome.core.types.Type;
import org.junit.Test;

/**
 * Tests for the system:round profile
 *
 * @author Arne Seime - Initial contribution
 */
public class SystemRoundProfileTest {

    @Test
    public void testDecimalType() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile offsetProfile = createProfile(callback, "1");

        Type cmd = new DecimalType(1.23);
        Type roundedValue = offsetProfile.applyRounding(cmd);

        DecimalType decResult = (DecimalType) roundedValue;
        assertEquals(1.2, decResult.doubleValue(), 0);
    }

    @Test
    public void testDecimalToInt() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile offsetProfile = createProfile(callback, "0");

        Type cmd = new DecimalType(123.4);
        Type roundedValue = offsetProfile.applyRounding(cmd);

        DecimalType decResult = (DecimalType) roundedValue;
        assertEquals(123, decResult.doubleValue(), 0);
    }

    @Test
    public void testDecimalToTen() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile offsetProfile = createProfile(callback, "-1");

        Type cmd = new DecimalType(123.4);
        Type roundedValue = offsetProfile.applyRounding(cmd);

        DecimalType decResult = (DecimalType) roundedValue;
        assertEquals(120, decResult.doubleValue(), 0);
    }

    @Test
    public void testDecimalToHundred() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile offsetProfile = createProfile(callback, "-2");

        Type cmd = new DecimalType(951);
        Type roundedValue = offsetProfile.applyRounding(cmd);

        DecimalType decResult = (DecimalType) roundedValue;
        assertEquals(1000, decResult.doubleValue(), 0);
    }

    @Test
    public void testDecimalToHundredDown() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile offsetProfile = createProfile(callback, "-2");

        Type cmd = new DecimalType(949);
        Type roundedValue = offsetProfile.applyRounding(cmd);

        DecimalType decResult = (DecimalType) roundedValue;
        assertEquals(900, decResult.doubleValue(), 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQuantityType() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile offsetProfile = createProfile(callback, "1");

        Type cmd = new QuantityType<>("1.55°C");
        Type roundedValue = offsetProfile.applyRounding(cmd);

        QuantityType<Temperature> decResult = (QuantityType<Temperature>) roundedValue;
        assertEquals(1.6, decResult.doubleValue(), 0);
        assertEquals(SIUnits.CELSIUS, decResult.getUnit());

    }

    private SystemRoundProfile createProfile(ProfileCallback callback, String numDecimals) {
        ProfileContext context = mock(ProfileContext.class);
        Configuration config = new Configuration();
        config.put("decimals", numDecimals);
        when(context.getConfiguration()).thenReturn(config);

        return new SystemRoundProfile(callback, context);
    }
}
