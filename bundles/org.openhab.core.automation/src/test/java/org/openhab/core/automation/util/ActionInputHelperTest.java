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
package org.openhab.core.automation.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Temperature;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.internal.i18n.I18nProviderImpl;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;

/**
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class ActionInputHelperTest {
    private static final String PARAM_NAME = "Param";
    private static final String PARAM_LABEL = "Label Parameter";
    private static final String PARAM_DESCRIPTION = "Description parameter";

    private TimeZoneProvider timeZoneProvider = new TestTimeZoneProvider();
    private UnitProvider unitProvider = new TestUnitProvider();
    private ActionInputsHelper helper = new ActionInputsHelper(timeZoneProvider, unitProvider);

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenBoolean() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.lang.Boolean")),
                ConfigDescriptionParameter.Type.BOOLEAN, false, null, null, null, null);
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("boolean")),
                ConfigDescriptionParameter.Type.BOOLEAN, true, "false", null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenByte() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.lang.Byte")),
                ConfigDescriptionParameter.Type.INTEGER, false, null, null, null, null);
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("byte")),
                ConfigDescriptionParameter.Type.INTEGER, true, "0", null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenShort() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.lang.Short")),
                ConfigDescriptionParameter.Type.INTEGER, false, null, null, null, null);
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("short")),
                ConfigDescriptionParameter.Type.INTEGER, true, "0", null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenInteger() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.lang.Integer")),
                ConfigDescriptionParameter.Type.INTEGER, false, null, null, null, null);
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("int")),
                ConfigDescriptionParameter.Type.INTEGER, true, "0", null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenLong() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.lang.Long")),
                ConfigDescriptionParameter.Type.INTEGER, false, null, null, null, null);
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("long")),
                ConfigDescriptionParameter.Type.INTEGER, true, "0", null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenFloat() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.lang.Float")),
                ConfigDescriptionParameter.Type.DECIMAL, false, null, null, null, BigDecimal.ZERO);
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("float")),
                ConfigDescriptionParameter.Type.DECIMAL, true, "0", null, null, BigDecimal.ZERO);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenDouble() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.lang.Double")),
                ConfigDescriptionParameter.Type.DECIMAL, false, null, null, null, BigDecimal.ZERO);
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("double")),
                ConfigDescriptionParameter.Type.DECIMAL, true, "0", null, null, BigDecimal.ZERO);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenNumber() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.lang.Number")),
                ConfigDescriptionParameter.Type.DECIMAL, false, null, null, null, BigDecimal.ZERO);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenDecimalType() {
        checkParameter(
                helper.mapActionInputToConfigDescriptionParameter(
                        buildInput("org.openhab.core.library.types.DecimalType")),
                ConfigDescriptionParameter.Type.DECIMAL, false, null, null, null, BigDecimal.ZERO);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenQuantityType() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("QuantityType<Temperature>")),
                ConfigDescriptionParameter.Type.DECIMAL, false, null, null, "°C", BigDecimal.ZERO);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenString() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.lang.String")),
                ConfigDescriptionParameter.Type.TEXT, false, null, null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenLocalDate() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.time.LocalDate")),
                ConfigDescriptionParameter.Type.TEXT, false, null, "date", null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenLocalTime() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.time.LocalTime")),
                ConfigDescriptionParameter.Type.TEXT, false, null, "time", null, BigDecimal.ONE);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenLocalDateTime() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.time.LocalDateTime")),
                ConfigDescriptionParameter.Type.TEXT, false, null, "datetime", null, BigDecimal.ONE);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenDate() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.util.Date")),
                ConfigDescriptionParameter.Type.TEXT, false, null, "datetime", null, BigDecimal.ONE);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenZonedDateTime() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.time.ZonedDateTime")),
                ConfigDescriptionParameter.Type.TEXT, false, null, "datetime", null, BigDecimal.ONE);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenInstant() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.time.Instant")),
                ConfigDescriptionParameter.Type.TEXT, false, null, "datetime", null, BigDecimal.ONE);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenDuration() {
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(buildInput("java.time.Duration")),
                ConfigDescriptionParameter.Type.TEXT, false, null, null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenDefaultValue() {
        Input input = new Input(PARAM_NAME, "int", PARAM_LABEL, PARAM_DESCRIPTION, null, false, null, "-1");
        checkParameter(helper.mapActionInputToConfigDescriptionParameter(input),
                ConfigDescriptionParameter.Type.INTEGER, true, "-1", null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenUnsupportedType() {
        assertThrows(IllegalArgumentException.class,
                () -> helper.mapActionInputToConfigDescriptionParameter(buildInput("List<String>")));
    }

    @Test
    public void testMapActionInputsToConfigDescriptionParametersWhenOk() {
        Input input1 = buildInput("Boolean", "boolean");
        Input input2 = buildInput("String", "java.lang.String");
        List<ConfigDescriptionParameter> params = helper
                .mapActionInputsToConfigDescriptionParameters(List.of(input1, input2));
        assertThat(params.size(), is(2));
        checkParameter(params.getFirst(), "Boolean", ConfigDescriptionParameter.Type.BOOLEAN, PARAM_LABEL,
                PARAM_DESCRIPTION, true, "false", null, null, null);
        checkParameter(params.get(1), "String", ConfigDescriptionParameter.Type.TEXT, PARAM_LABEL, PARAM_DESCRIPTION,
                false, null, null, null, null);
    }

    @Test
    public void testMapActionInputsToConfigDescriptionParametersWhenUnsupportedType() {
        Input input1 = buildInput("Boolean", "boolean");
        Input input2 = buildInput("List", "List<String>");
        assertThrows(IllegalArgumentException.class,
                () -> helper.mapActionInputsToConfigDescriptionParameters(List.of(input1, input2)));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenBoolean() {
        Input input = buildInput("java.lang.Boolean");
        assertThat(helper.mapSerializedInputToActionInput(input, true), is(Boolean.TRUE));
        assertThat(helper.mapSerializedInputToActionInput(input, false), is(Boolean.FALSE));
        assertThat(helper.mapSerializedInputToActionInput(input, Boolean.TRUE), is(Boolean.TRUE));
        assertThat(helper.mapSerializedInputToActionInput(input, Boolean.FALSE), is(Boolean.FALSE));
        assertThat(helper.mapSerializedInputToActionInput(input, "true"), is(Boolean.TRUE));
        assertThat(helper.mapSerializedInputToActionInput(input, "True"), is(Boolean.TRUE));
        assertThat(helper.mapSerializedInputToActionInput(input, "TRUE"), is(Boolean.TRUE));
        assertThat(helper.mapSerializedInputToActionInput(input, "false"), is(Boolean.FALSE));
        assertThat(helper.mapSerializedInputToActionInput(input, "False"), is(Boolean.FALSE));
        assertThat(helper.mapSerializedInputToActionInput(input, "FALSE"), is(Boolean.FALSE));
        assertThat(helper.mapSerializedInputToActionInput(input, "other"), is(Boolean.FALSE));

        Input input2 = buildInput("boolean");
        assertThat(helper.mapSerializedInputToActionInput(input2, true), is(Boolean.TRUE));
        assertThat(helper.mapSerializedInputToActionInput(input2, false), is(Boolean.FALSE));
        assertThat(helper.mapSerializedInputToActionInput(input2, Boolean.TRUE), is(Boolean.TRUE));
        assertThat(helper.mapSerializedInputToActionInput(input2, Boolean.FALSE), is(Boolean.FALSE));
        assertThat(helper.mapSerializedInputToActionInput(input2, "true"), is(Boolean.TRUE));
        assertThat(helper.mapSerializedInputToActionInput(input2, "True"), is(Boolean.TRUE));
        assertThat(helper.mapSerializedInputToActionInput(input2, "TRUE"), is(Boolean.TRUE));
        assertThat(helper.mapSerializedInputToActionInput(input2, "false"), is(Boolean.FALSE));
        assertThat(helper.mapSerializedInputToActionInput(input2, "False"), is(Boolean.FALSE));
        assertThat(helper.mapSerializedInputToActionInput(input2, "FALSE"), is(Boolean.FALSE));
        assertThat(helper.mapSerializedInputToActionInput(input2, "other"), is(Boolean.FALSE));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenByte() {
        byte val = 127;
        Byte valAsByte = Byte.valueOf(val);

        Input input = buildInput("java.lang.Byte");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsByte));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsByte), is(valAsByte));
        assertThat(helper.mapSerializedInputToActionInput(input, Double.valueOf(val)), is(valAsByte));
        assertThat(helper.mapSerializedInputToActionInput(input, "127"), is(valAsByte));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "128"));

        Input input2 = buildInput("byte");
        assertThat(helper.mapSerializedInputToActionInput(input2, val), is(valAsByte));
        assertThat(helper.mapSerializedInputToActionInput(input2, valAsByte), is(valAsByte));
        assertThat(helper.mapSerializedInputToActionInput(input2, Double.valueOf(val)), is(valAsByte));
        assertThat(helper.mapSerializedInputToActionInput(input2, "127"), is(valAsByte));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input2, "128"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenShort() {
        short val = 32767;
        Short valAsShort = Short.valueOf(val);

        Input input = buildInput("java.lang.Short");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsShort));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsShort), is(valAsShort));
        assertThat(helper.mapSerializedInputToActionInput(input, Double.valueOf(val)), is(valAsShort));
        assertThat(helper.mapSerializedInputToActionInput(input, "32767"), is(valAsShort));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "32768"));

        Input input2 = buildInput("short");
        assertThat(helper.mapSerializedInputToActionInput(input2, val), is(valAsShort));
        assertThat(helper.mapSerializedInputToActionInput(input2, valAsShort), is(valAsShort));
        assertThat(helper.mapSerializedInputToActionInput(input2, Double.valueOf(val)), is(valAsShort));
        assertThat(helper.mapSerializedInputToActionInput(input2, "32767"), is(valAsShort));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input2, "32768"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenInteger() {
        int val = 123456789;
        Integer valAsInteger = Integer.valueOf(val);

        Input input = buildInput("java.lang.Integer");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsInteger));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsInteger), is(valAsInteger));
        assertThat(helper.mapSerializedInputToActionInput(input, Double.valueOf(val)), is(valAsInteger));
        assertThat(helper.mapSerializedInputToActionInput(input, "123456789"), is(valAsInteger));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));

        Input input2 = buildInput("int");
        assertThat(helper.mapSerializedInputToActionInput(input2, val), is(valAsInteger));
        assertThat(helper.mapSerializedInputToActionInput(input2, valAsInteger), is(valAsInteger));
        assertThat(helper.mapSerializedInputToActionInput(input2, Double.valueOf(val)), is(valAsInteger));
        assertThat(helper.mapSerializedInputToActionInput(input2, "123456789"), is(valAsInteger));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input2, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenLong() {
        long val = 123456789;
        Long valAsLong = Long.valueOf(val);

        Input input = buildInput("java.lang.Long");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsLong));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsLong), is(valAsLong));
        assertThat(helper.mapSerializedInputToActionInput(input, Double.valueOf(val)), is(valAsLong));
        assertThat(helper.mapSerializedInputToActionInput(input, "123456789"), is(valAsLong));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));

        Input input2 = buildInput("long");
        assertThat(helper.mapSerializedInputToActionInput(input2, val), is(valAsLong));
        assertThat(helper.mapSerializedInputToActionInput(input2, valAsLong), is(valAsLong));
        assertThat(helper.mapSerializedInputToActionInput(input2, Double.valueOf(val)), is(valAsLong));
        assertThat(helper.mapSerializedInputToActionInput(input2, "123456789"), is(valAsLong));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input2, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenFloat() {
        Float val = 456.789f;
        Float valAsFloat = Float.valueOf(val);

        Input input = buildInput("java.lang.Float");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsFloat));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsFloat), is(valAsFloat));
        assertThat(helper.mapSerializedInputToActionInput(input, Double.valueOf(val)), is(valAsFloat));
        assertThat(helper.mapSerializedInputToActionInput(input, "456.789"), is(valAsFloat));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));

        Input input2 = buildInput("float");
        assertThat(helper.mapSerializedInputToActionInput(input2, val), is(valAsFloat));
        assertThat(helper.mapSerializedInputToActionInput(input2, valAsFloat), is(valAsFloat));
        assertThat(helper.mapSerializedInputToActionInput(input2, Double.valueOf(val)), is(valAsFloat));
        assertThat(helper.mapSerializedInputToActionInput(input2, "456.789"), is(valAsFloat));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input2, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenDouble() {
        Double val = 456.789d;
        Double valAsDouble = Double.valueOf(val);

        Input input = buildInput("java.lang.Double");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsDouble));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsDouble), is(valAsDouble));
        assertThat(helper.mapSerializedInputToActionInput(input, "456.789"), is(valAsDouble));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));

        Input input2 = buildInput("double");
        assertThat(helper.mapSerializedInputToActionInput(input2, val), is(valAsDouble));
        assertThat(helper.mapSerializedInputToActionInput(input2, valAsDouble), is(valAsDouble));
        assertThat(helper.mapSerializedInputToActionInput(input2, "456.789"), is(valAsDouble));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input2, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenDecimalType() {
        DecimalType val = new DecimalType(23.45);
        Input input = buildInput("org.openhab.core.library.types.DecimalType");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, val.doubleValue()), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, "23.45"), is(val));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenQuantityType() {
        QuantityType<Temperature> val = new QuantityType<>(19.7, SIUnits.CELSIUS);
        Input input = buildInput("QuantityType<Temperature>");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, 19.7d), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, "19.7"), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, "19.7 °C"), is(val));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "19.7 XXX"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenLocalDate() {
        String valAsString = "2024-08-31";
        LocalDate val = LocalDate.parse(valAsString);
        Input input = buildInput("java.time.LocalDate");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsString), is(val));
        assertThrows(IllegalArgumentException.class,
                () -> helper.mapSerializedInputToActionInput(input, valAsString.replaceAll("-", " ")));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenLocalTime() {
        String valAsString = "08:30:55";
        LocalTime val = LocalTime.parse(valAsString);
        Input input = buildInput("java.time.LocalTime");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsString), is(val));
        assertThrows(IllegalArgumentException.class,
                () -> helper.mapSerializedInputToActionInput(input, valAsString.replaceAll(":", " ")));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenLocalDateTime() {
        String valAsString = "2024-07-01T20:30:45";
        LocalDateTime val = LocalDateTime.parse(valAsString);
        Input input = buildInput("java.time.LocalDateTime");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsString), is(val));
        assertThrows(IllegalArgumentException.class,
                () -> helper.mapSerializedInputToActionInput(input, valAsString.replaceAll("T", " ")));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenZonedDateTime() {
        String valAsString = "2007-12-03T10:15:30";
        ZonedDateTime val = LocalDateTime.parse(valAsString).atZone(timeZoneProvider.getTimeZone());
        Input input = buildInput("java.time.ZonedDateTime");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsString), is(val));
        String s1 = valAsString.replaceAll("T", " ");
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, s1));

        valAsString = "2007-12-03T10:15:30+04:00";
        val = ZonedDateTime.parse(valAsString, DateTimeFormatter.ISO_DATE_TIME);
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsString), is(val));
        assertThat(val.getOffset(), is(ZoneOffset.of("+04:00")));

        valAsString = "2007-12-03T10:15:30+04:00[Europe/Kyiv]";
        val = ZonedDateTime.parse(valAsString, DateTimeFormatter.ISO_DATE_TIME);
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsString), is(val));
        assertThat(val.getOffset(), is(ZoneOffset.of("+02:00")));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenDate() {
        String valAsString = "2024-11-05T09:45:12";
        Date val;
        try {
            val = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(valAsString);
        } catch (IllegalArgumentException | ParseException e) {
            val = null;
        }
        assertNotNull(val);
        Input input = buildInput("java.util.Date");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsString), is(val));
        assertThrows(IllegalArgumentException.class,
                () -> helper.mapSerializedInputToActionInput(input, valAsString.replaceAll("T", " ")));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenInstant() {
        String valAsString = "2017-12-09T20:15:30.00";
        Instant val = LocalDateTime.parse(valAsString).atZone(timeZoneProvider.getTimeZone()).toInstant();
        Input input = buildInput("java.time.Instant");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsString), is(val));
        assertThrows(IllegalArgumentException.class,
                () -> helper.mapSerializedInputToActionInput(input, valAsString.replaceAll("T", " ")));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenDuration() {
        String valAsString = "P2DT17H25M30.5S";
        Duration val = Duration.parse(valAsString);
        Input input = buildInput("java.time.Duration");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsString), is(val));
        assertThrows(IllegalArgumentException.class,
                () -> helper.mapSerializedInputToActionInput(input, valAsString.replaceAll("T", " ")));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenAnyOtherType() {
        List<String> val = List.of("Value 1", "Value 2");
        Input input = buildInput("List<String>");
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
    }

    @Test
    public void testMapSerializedInputsToActionInputsAppliesDefaults() {
        Input inputBoolean = buildInput("BooleanParam", "boolean");
        Input inputByte = buildInput("ByteParam", "byte");
        Input inputShort = buildInput("ShortParam", "short");
        Input inputInteger = buildInput("IntegerParam", "int");
        Input inputLong = buildInput("LongParam", "long");
        Input inputFloat = buildInput("FloatParam", "float");
        Input inputDouble = buildInput("DoubleParam", "double");
        ActionType action = new ActionType("action", null,
                List.of(inputBoolean, inputByte, inputShort, inputInteger, inputLong, inputFloat, inputDouble));

        Map<String, Object> result = helper.mapSerializedInputsToActionInputs(action, Map.of());
        assertThat(result.size(), is(7));
        assertThat(result.get("BooleanParam"), is(Boolean.FALSE));
        assertThat(result.get("ByteParam"), is((byte) 0));
        assertThat(result.get("ShortParam"), is((short) 0));
        assertThat(result.get("IntegerParam"), is(0));
        assertThat(result.get("LongParam"), is(0L));
        assertThat(result.get("FloatParam"), is(0.0f));
        assertThat(result.get("DoubleParam"), is(0.0));
    }

    @Test
    public void testMapSerializedInputsToActionInputs() {
        Input input1 = buildInput("BooleanParam", "java.lang.Boolean");
        Input input2 = buildInput("DoubleParam", "java.lang.Double");
        Input input3 = buildInput("StringParam", "java.lang.String");
        ActionType action = new ActionType("action", null, List.of(input1, input2, input3));

        Map<String, Object> result = helper.mapSerializedInputsToActionInputs(action,
                Map.of("BooleanParam", true, "OtherParam", "other", "DoubleParam", "invalid", "StringParam", "test"));
        assertThat(result.size(), is(2));
        assertThat(result.get("BooleanParam"), is(Boolean.TRUE));
        assertNull(result.get("DoubleParam"));
        assertThat(result.get("StringParam"), is("test"));
    }

    private Input buildInput(String type) {
        return buildInput(PARAM_NAME, type);
    }

    private Input buildInput(String name, String type) {
        return new Input(name, type, PARAM_LABEL, PARAM_DESCRIPTION, null, false, null, null);
    }

    private void checkParameter(ConfigDescriptionParameter param, ConfigDescriptionParameter.Type type,
            boolean required, @Nullable String defaultValue, @Nullable String context, @Nullable String unit,
            @Nullable BigDecimal step) {
        checkParameter(param, PARAM_NAME, type, PARAM_LABEL, PARAM_DESCRIPTION, required, defaultValue, context, unit,
                step);
    }

    private void checkParameter(ConfigDescriptionParameter param, String name, ConfigDescriptionParameter.Type type,
            String label, String description, boolean required, @Nullable String defaultValue, @Nullable String context,
            @Nullable String unit, @Nullable BigDecimal step) {
        assertThat(param.getName(), is(name));
        assertThat(param.getLabel(), is(label));
        assertThat(param.getDescription(), is(description));
        assertThat(param.getType(), is(type));
        assertThat(param.isReadOnly(), is(false));
        assertThat(param.isRequired(), is(required));
        if (defaultValue == null) {
            assertNull(param.getDefault());
        } else {
            assertThat(param.getDefault(), is(defaultValue));
        }
        if (context == null) {
            assertNull(param.getContext());
        } else {
            assertThat(param.getContext(), is(context));
        }
        if (unit == null) {
            assertNull(param.getUnit());
        } else {
            assertNotNull(param.getUnit());
        }
        if (step == null) {
            assertNull(param.getStepSize());
        } else {
            assertThat(param.getStepSize(), is(step));
        }
    }

    public class TestTimeZoneProvider implements TimeZoneProvider {

        @Override
        public ZoneId getTimeZone() {
            return ZoneId.of("Europe/Paris");
        }
    }

    public class TestUnitProvider implements UnitProvider {

        private final Map<Class<? extends Quantity<?>>, Map<SystemOfUnits, Unit<? extends Quantity<?>>>> dimensionMap = I18nProviderImpl
                .getDimensionMap();

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Quantity<T>> Unit<T> getUnit(Class<T> dimension) {
            return Objects.requireNonNull(
                    (Unit<T>) dimensionMap.getOrDefault(dimension, Map.of()).get(SIUnits.getInstance()));
        }

        @Override
        public SystemOfUnits getMeasurementSystem() {
            return SIUnits.getInstance();
        }

        @Override
        public Collection<Class<? extends Quantity<?>>> getAllDimensions() {
            return Set.of();
        }
    }
}
