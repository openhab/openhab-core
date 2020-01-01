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
package org.openhab.core.io.bin2json;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * Unit tests for {@link Bin2Json}.
 *
 * @author Pauli Anttila - Initial contribution
 */
public class Bin2JsonTest {

    @Test(expected = ConversionException.class)
    public void testParserRuleError() throws ConversionException {
        new Bin2Json("byte a byte b ubyte c;").convert(new byte[] { 3, 34, (byte) 255 });
    }

    @Test
    public void testHexStringData() throws ConversionException {
        JsonObject json = new Bin2Json("byte a; byte b; ubyte c;").convert("03FAFF");
        assertEquals("{\"a\":3,\"b\":-6,\"c\":255}", json.toString());
    }

    @Test(expected = ConversionException.class)
    public void testHexStringDataError() throws ConversionException {
        new Bin2Json("byte a; byte b; ubyte c;").convert("0322F");
    }

    @Test
    public void testByteArrayData() throws ConversionException {
        JsonObject json = new Bin2Json("ubyte length; ubyte[length] data;")
                .convert(new byte[] { 4, 8, 33, 1, 2, 3, 4 });
        assertEquals("{\"length\":4,\"data\":[8,33,1,2]}", json.toString());
    }

    @Test(expected = ConversionException.class)
    public void testByteArrayDataError() throws ConversionException {
        new Bin2Json("byte a; byte b; ubyte c;").convert(new byte[] { 3, 34 });
    }

    @Test
    public void testInputStreamData() throws ConversionException, IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] { 4, 8, 33, 1, 2, 3, 4 });
        JsonObject json = new Bin2Json("ubyte length; ubyte[length] data;").convert(inputStream);
        assertEquals("{\"length\":4,\"data\":[8,33,1,2]}", json.toString());
    }

    @Test(expected = ConversionException.class)
    public void testInputStreamDataError() throws ConversionException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] { 4, 8, 33 });
        new Bin2Json("ubyte length; ubyte[length] data;").convert(inputStream);
    }
}
