/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;

/**
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ActionInputHelplerTest {

    private @Mock @NonNullByDefault({}) UnitProvider unitProviderMock;

    private ActionInputsHelper helper = new ActionInputsHelper(unitProviderMock);

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenBoolean() {
        Input input = buildInput("Boolean", "java.lang.Boolean");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "BooleanParam", "Boolean Param",
                "Boolean Parameter", ConfigDescriptionParameter.Type.BOOLEAN, false, null, null, null);

        input = buildInput("Boolean", "boolean");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "BooleanParam", "Boolean Param",
                "Boolean Parameter", ConfigDescriptionParameter.Type.BOOLEAN, true, "false", null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenByte() {
        Input input = buildInput("Byte", "java.lang.Byte");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "ByteParam", "Byte Param",
                "Byte Parameter", ConfigDescriptionParameter.Type.INTEGER, false, null, null, null);

        input = buildInput("Byte", "byte");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "ByteParam", "Byte Param",
                "Byte Parameter", ConfigDescriptionParameter.Type.INTEGER, true, "0", null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenShort() {
        Input input = buildInput("Short", "java.lang.Short");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "ShortParam", "Short Param",
                "Short Parameter", ConfigDescriptionParameter.Type.INTEGER, false, null, null, null);

        input = buildInput("Short", "short");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "ShortParam", "Short Param",
                "Short Parameter", ConfigDescriptionParameter.Type.INTEGER, true, "0", null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenInteger() {
        Input input = buildInput("Integer", "java.lang.Integer");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "IntegerParam", "Integer Param",
                "Integer Parameter", ConfigDescriptionParameter.Type.INTEGER, false, null, null, null);

        input = buildInput("Integer", "int");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "IntegerParam", "Integer Param",
                "Integer Parameter", ConfigDescriptionParameter.Type.INTEGER, true, "0", null, null);

        input = new Input("IntegerParam", "int", "Integer Param", "Integer Parameter", null, false, null, "-1");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "IntegerParam", "Integer Param",
                "Integer Parameter", ConfigDescriptionParameter.Type.INTEGER, true, "-1", null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenLong() {
        Input input = buildInput("Long", "java.lang.Long");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "LongParam", "Long Param",
                "Long Parameter", ConfigDescriptionParameter.Type.INTEGER, false, null, null, null);

        input = buildInput("Long", "long");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "LongParam", "Long Param",
                "Long Parameter", ConfigDescriptionParameter.Type.INTEGER, true, "0", null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenFloat() {
        Input input = buildInput("Float", "java.lang.Float");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "FloatParam", "Float Param",
                "Float Parameter", ConfigDescriptionParameter.Type.DECIMAL, false, null, null, null);

        input = buildInput("Float", "float");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "FloatParam", "Float Param",
                "Float Parameter", ConfigDescriptionParameter.Type.DECIMAL, true, "0", null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenDouble() {
        Input input = buildInput("Double", "java.lang.Double");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "DoubleParam", "Double Param",
                "Double Parameter", ConfigDescriptionParameter.Type.DECIMAL, false, null, null, null);

        input = buildInput("Double", "double");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "DoubleParam", "Double Param",
                "Double Parameter", ConfigDescriptionParameter.Type.DECIMAL, true, "0", null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenNumber() {
        Input input = buildInput("Number", "java.lang.Number");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "NumberParam", "Number Param",
                "Number Parameter", ConfigDescriptionParameter.Type.DECIMAL, false, null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenDecimalType() {
        Input input = buildInput("DecimalType", "org.openhab.core.library.types.DecimalType");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "DecimalTypeParam", "DecimalType Param",
                "DecimalType Parameter", ConfigDescriptionParameter.Type.DECIMAL, false, null, null, null);
    }

    @Disabled
    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenQuantityType() {
        ActionInputsHelper helperLocal = new ActionInputsHelper(unitProviderMock);
        when(unitProviderMock.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);
        Input input = buildInput("Temp", "QuantityType<Temperature>");
        checkParamter(helperLocal.mapActionInputToConfigDescriptionParameter(input), "TempParam", "Temp Param",
                "Temp Parameter", ConfigDescriptionParameter.Type.DECIMAL, false, null, null, "°C");
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenString() {
        Input input = buildInput("String", "java.lang.String");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "StringParam", "String Param",
                "String Parameter", ConfigDescriptionParameter.Type.TEXT, false, null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenLocalDate() {
        Input input = buildInput("LocalDate", "java.time.LocalDate");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "LocalDateParam", "LocalDate Param",
                "LocalDate Parameter", ConfigDescriptionParameter.Type.TEXT, false, null, "date", null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenLocalTime() {
        Input input = buildInput("LocalTime", "java.time.LocalTime");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "LocalTimeParam", "LocalTime Param",
                "LocalTime Parameter", ConfigDescriptionParameter.Type.TEXT, false, null, "time", null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenLocalDateTime() {
        Input input = buildInput("LocalDateTime", "java.time.LocalDateTime");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "LocalDateTimeParam",
                "LocalDateTime Param", "LocalDateTime Parameter", ConfigDescriptionParameter.Type.TEXT, false, null,
                "datetime", null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenDate() {
        Input input = buildInput("Date", "java.util.Date");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "DateParam", "Date Param",
                "Date Parameter", ConfigDescriptionParameter.Type.TEXT, false, null, "datetime", null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenZonedDateTime() {
        Input input = buildInput("ZonedDateTime", "java.time.ZonedDateTime");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "ZonedDateTimeParam",
                "ZonedDateTime Param", "ZonedDateTime Parameter", ConfigDescriptionParameter.Type.TEXT, false, null,
                null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenInstant() {
        Input input = buildInput("Instant", "java.time.Instant");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "InstantParam", "Instant Param",
                "Instant Parameter", ConfigDescriptionParameter.Type.TEXT, false, null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenDuration() {
        Input input = buildInput("Duration", "java.time.Duration");
        checkParamter(helper.mapActionInputToConfigDescriptionParameter(input), "DurationParam", "Duration Param",
                "Duration Parameter", ConfigDescriptionParameter.Type.TEXT, false, null, null, null);
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterWhenUnsupportedType() {
        Input input = buildInput("List", "List<String>");
        assertThrows(IllegalArgumentException.class, () -> helper.mapActionInputToConfigDescriptionParameter(input));
    }

    @Test
    public void testMapActionInputsToConfigDescriptionParametersWhenOk() {
        Input input1 = buildInput("Boolean", "boolean");
        Input input2 = buildInput("String", "java.lang.String");
        List<ConfigDescriptionParameter> params = helper
                .mapActionInputsToConfigDescriptionParameters(List.of(input1, input2));
        assertThat(params.size(), is(2));
        checkParamter(params.get(0), "BooleanParam", "Boolean Param", "Boolean Parameter",
                ConfigDescriptionParameter.Type.BOOLEAN, true, "false", null, null);
        checkParamter(params.get(1), "StringParam", "String Param", "String Parameter",
                ConfigDescriptionParameter.Type.TEXT, false, null, null, null);
    }

    @Test
    public void testMapActionInputsToConfigDescriptionParametersWhenUnsupportedType() {
        Input input1 = buildInput("Boolean", "boolean");
        Input input2 = buildInput("List", "List<String>");
        assertThrows(IllegalArgumentException.class,
                () -> helper.mapActionInputsToConfigDescriptionParameters(List.of(input1, input2)));
    }

    private Input buildInput(String baseName, String type) {
        return new Input(baseName + "Param", type, baseName + " Param", baseName + " Parameter", null, false, null,
                null);
    }

    private void checkParamter(ConfigDescriptionParameter param, String name, String label, String description,
            ConfigDescriptionParameter.Type type, boolean required, @Nullable String defaultValue,
            @Nullable String context, @Nullable String unit) {
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
            assertThat(param.getUnit(), is(unit));
        }
    }

    @Test
    public void testMapSerializedInputToActionInputWhenBoolean() {
        Input input = new Input("BooleanParam", "java.lang.Boolean");
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
    }

    @Test
    public void testMapSerializedInputToActionInputWhenInteger() {
        Input input = new Input("IntegerParam", "java.lang.Integer");
        int val = 123;
        Integer valAsInteger = Integer.valueOf(val);
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsInteger));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsInteger), is(valAsInteger));
        assertThat(helper.mapSerializedInputToActionInput(input, Double.valueOf(val)), is(valAsInteger));
        assertThat(helper.mapSerializedInputToActionInput(input, "123"), is(valAsInteger));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenLong() {
        Input input = new Input("LongParam", "java.lang.Long");
        long val = 123456789;
        Long valAsLong = Long.valueOf(val);
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsLong));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsLong), is(valAsLong));
        assertThat(helper.mapSerializedInputToActionInput(input, Double.valueOf(val)), is(valAsLong));
        assertThat(helper.mapSerializedInputToActionInput(input, "123456789"), is(valAsLong));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenFloat() {
        Input input = new Input("FloatParam", "java.lang.Float");
        Float val = 456.789f;
        Float valAsFloat = Float.valueOf(val);
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsFloat));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsFloat), is(valAsFloat));
        assertThat(helper.mapSerializedInputToActionInput(input, Double.valueOf(val)), is(valAsFloat));
        assertThat(helper.mapSerializedInputToActionInput(input, "456.789"), is(valAsFloat));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenDouble() {
        Input input = new Input("DoubleParam", "java.lang.Double");
        Double val = 456.789d;
        Double valAsDouble = Double.valueOf(val);
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsDouble));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsDouble), is(valAsDouble));
        assertThat(helper.mapSerializedInputToActionInput(input, "456.789"), is(valAsDouble));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenDecimalType() {
        Input input = new Input("DecimalTypeParam", "org.openhab.core.library.types.DecimalType");
        DecimalType val = new DecimalType(23.45);
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, val.doubleValue()), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, "23.45"), is(val));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputWhenQuantityType() {
        ActionInputsHelper helperLocal = new ActionInputsHelper(unitProviderMock);
        when(unitProviderMock.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);
        Input input = new Input("QuantityTypeParam", "QuantityType<Temperature>");
        QuantityType<Temperature> val = new QuantityType<>(19.7, SIUnits.CELSIUS);
        assertThat(helperLocal.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helperLocal.mapSerializedInputToActionInput(input, 19.7d), is(val));
        assertThat(helperLocal.mapSerializedInputToActionInput(input, "19.7"), is(val));
        assertThat(helperLocal.mapSerializedInputToActionInput(input, "19.7 °C"), is(val));
        assertThrows(IllegalArgumentException.class,
                () -> helperLocal.mapSerializedInputToActionInput(input, "19.7 XXX"));
    }

    @Test
    public void testMapSerializedInputsToActionInputs() {
        Input input1 = new Input("BooleanParam", "java.lang.Boolean");
        Input input2 = new Input("IntegerParam", "java.lang.Integer");
        Input input3 = new Input("DoubleParam", "java.lang.Double");
        Input input4 = new Input("StringParam", "java.lang.String");
        ActionType action = new ActionType("action", null, List.of(input1, input2, input3, input4));

        Map<String, Object> result = helper.mapSerializedInputsToActionInputs(action,
                Map.of("BooleanParam", true, "StringParam", "test", "DoubleParam", 456.789, "IntegerParam", 123));
        assertThat(result.size(), is(4));
        assertThat(result.get("BooleanParam"), is(Boolean.TRUE));
        assertThat(result.get("IntegerParam"), is(Integer.valueOf(123)));
        assertThat(result.get("DoubleParam"), is(Double.valueOf(456.789)));
        assertThat(result.get("StringParam"), is("test"));

        result = helper.mapSerializedInputsToActionInputs(action,
                Map.of("BooleanParam", true, "StringParam", "test", "DoubleParam", "invalid", "OtherParam", "other"));
        assertThat(result.size(), is(2));
        assertThat(result.get("BooleanParam"), is(Boolean.TRUE));
        assertNull(result.get("IntegerParam"));
        assertNull(result.get("DoubleParam"));
        assertThat(result.get("StringParam"), is("test"));
    }
}
