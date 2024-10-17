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
    public void testMapActionInputToConfigDescriptionParameterForBoolean() {
        Input input = new Input("BooleanParam", "java.lang.Boolean", "Label", "Description", null, false, null, null);
        ConfigDescriptionParameter param = helper.mapActionInputToConfigDescriptionParameter(input);
        assertThat(param.getName(), is("BooleanParam"));
        assertThat(param.getLabel(), is("Label"));
        assertThat(param.getDescription(), is("Description"));
        assertThat(param.getType(), is(ConfigDescriptionParameter.Type.BOOLEAN));
        assertThat(param.isReadOnly(), is(false));
        assertThat(param.isRequired(), is(false));
        assertNull(param.getDefault());
        assertNull(param.getContext());
        assertNull(param.getUnit());

        input = new Input("BooleanParam", "boolean", "Label", "Description", null, false, null, null);
        param = helper.mapActionInputToConfigDescriptionParameter(input);
        assertThat(param.getName(), is("BooleanParam"));
        assertThat(param.getLabel(), is("Label"));
        assertThat(param.getDescription(), is("Description"));
        assertThat(param.getType(), is(ConfigDescriptionParameter.Type.BOOLEAN));
        assertThat(param.isReadOnly(), is(false));
        assertThat(param.isRequired(), is(true));
        assertThat(param.getDefault(), is("false"));
        assertNull(param.getContext());
        assertNull(param.getUnit());
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterForInteger() {
        Input input = new Input("IntegerParam", "java.lang.Integer", "Label", "Description", null, false, null, null);
        ConfigDescriptionParameter param = helper.mapActionInputToConfigDescriptionParameter(input);
        assertThat(param.getName(), is("IntegerParam"));
        assertThat(param.getLabel(), is("Label"));
        assertThat(param.getDescription(), is("Description"));
        assertThat(param.getType(), is(ConfigDescriptionParameter.Type.INTEGER));
        assertThat(param.isReadOnly(), is(false));
        assertThat(param.isRequired(), is(false));
        assertNull(param.getDefault());
        assertNull(param.getContext());
        assertNull(param.getUnit());

        input = new Input("IntegerParam", "int", "Label", "Description", null, false, null, null);
        param = helper.mapActionInputToConfigDescriptionParameter(input);
        assertThat(param.getName(), is("IntegerParam"));
        assertThat(param.getLabel(), is("Label"));
        assertThat(param.getDescription(), is("Description"));
        assertThat(param.getType(), is(ConfigDescriptionParameter.Type.INTEGER));
        assertThat(param.isReadOnly(), is(false));
        assertThat(param.isRequired(), is(true));
        assertThat(param.getDefault(), is("0"));
        assertNull(param.getContext());
        assertNull(param.getUnit());
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterForDouble() {
        Input input = new Input("DoubleParam", "java.lang.Double", "Label", "Description", null, false, null, null);
        ConfigDescriptionParameter param = helper.mapActionInputToConfigDescriptionParameter(input);
        assertThat(param.getName(), is("DoubleParam"));
        assertThat(param.getLabel(), is("Label"));
        assertThat(param.getDescription(), is("Description"));
        assertThat(param.getType(), is(ConfigDescriptionParameter.Type.DECIMAL));
        assertThat(param.isReadOnly(), is(false));
        assertThat(param.isRequired(), is(false));
        assertNull(param.getDefault());
        assertNull(param.getContext());
        assertNull(param.getUnit());

        input = new Input("DoubleParam", "double", "Label", "Description", null, false, null, null);
        param = helper.mapActionInputToConfigDescriptionParameter(input);
        assertThat(param.getName(), is("DoubleParam"));
        assertThat(param.getLabel(), is("Label"));
        assertThat(param.getDescription(), is("Description"));
        assertThat(param.getType(), is(ConfigDescriptionParameter.Type.DECIMAL));
        assertThat(param.isReadOnly(), is(false));
        assertThat(param.isRequired(), is(true));
        assertThat(param.getDefault(), is("0"));
        assertNull(param.getContext());
        assertNull(param.getUnit());
    }

    @Test
    public void testMapActionInputToConfigDescriptionParameterForString() {
        Input input = new Input("StringParam", "java.lang.String", "Label", "Description", null, false, null, null);
        ConfigDescriptionParameter param = helper.mapActionInputToConfigDescriptionParameter(input);
        assertThat(param.getName(), is("StringParam"));
        assertThat(param.getLabel(), is("Label"));
        assertThat(param.getDescription(), is("Description"));
        assertThat(param.getType(), is(ConfigDescriptionParameter.Type.TEXT));
        assertThat(param.isReadOnly(), is(false));
        assertThat(param.isRequired(), is(false));
        assertNull(param.getDefault());
        assertNull(param.getContext());
        assertNull(param.getUnit());
    }

    @Test
    public void testMapActionInputsToConfigDescriptionParameters() {
        Input input1 = new Input("BooleanParam", "boolean", "Label", "Description", null, false, null, null);
        Input input2 = new Input("IntegerParam", "java.lang.Integer", "Label", "Description", null, false, null, null);
        Input input3 = new Input("DoubleParam", "double", "Label", "Description", null, false, null, null);
        Input input4 = new Input("StringParam", "java.lang.String", "Label", "Description", null, false, null, null);
        List<ConfigDescriptionParameter> params = helper
                .mapActionInputsToConfigDescriptionParameters(List.of(input1, input2, input3, input4));
        assertThat(params.size(), is(4));
        assertThat(params.get(0).getName(), is("BooleanParam"));
        assertThat(params.get(0).getType(), is(ConfigDescriptionParameter.Type.BOOLEAN));
        assertThat(params.get(0).isRequired(), is(true));
        assertThat(params.get(0).getDefault(), is("false"));
        assertThat(params.get(1).getName(), is("IntegerParam"));
        assertThat(params.get(1).getType(), is(ConfigDescriptionParameter.Type.INTEGER));
        assertThat(params.get(1).isRequired(), is(false));
        assertNull(params.get(1).getDefault());
        assertThat(params.get(2).getName(), is("DoubleParam"));
        assertThat(params.get(2).getType(), is(ConfigDescriptionParameter.Type.DECIMAL));
        assertThat(params.get(2).isRequired(), is(true));
        assertThat(params.get(2).getDefault(), is("0"));
        assertThat(params.get(3).getName(), is("StringParam"));
        assertThat(params.get(3).getType(), is(ConfigDescriptionParameter.Type.TEXT));
        assertThat(params.get(3).isRequired(), is(false));
        assertNull(params.get(3).getDefault());
    }

    @Test
    public void testMapSerializedInputToActionInputForBoolean() {
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
    public void testMapSerializedInputToActionInputForInteger() {
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
    public void testMapSerializedInputToActionInputForLong() {
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
    public void testMapSerializedInputToActionInputForFloat() {
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
    public void testMapSerializedInputToActionInputForDouble() {
        Input input = new Input("DoubleParam", "java.lang.Double");
        Double val = 456.789d;
        Double valAsDouble = Double.valueOf(val);
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(valAsDouble));
        assertThat(helper.mapSerializedInputToActionInput(input, valAsDouble), is(valAsDouble));
        assertThat(helper.mapSerializedInputToActionInput(input, "456.789"), is(valAsDouble));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputForDecimalType() {
        Input input = new Input("DecimalTypeParam", "org.openhab.core.library.types.DecimalType");
        DecimalType val = new DecimalType(23.45);
        assertThat(helper.mapSerializedInputToActionInput(input, val), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, val.doubleValue()), is(val));
        assertThat(helper.mapSerializedInputToActionInput(input, "23.45"), is(val));
        assertThrows(IllegalArgumentException.class, () -> helper.mapSerializedInputToActionInput(input, "wrong"));
    }

    @Test
    public void testMapSerializedInputToActionInputForQuantityType() {
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
