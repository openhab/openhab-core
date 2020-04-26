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
package org.openhab.core.config.core.internal.validation;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.validation.ConfigDescriptionValidator;
import org.openhab.core.config.core.validation.ConfigValidationException;
import org.openhab.core.config.core.validation.ConfigValidationMessage;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Testing the {@link ConfigDescriptionValidator}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ConfigDescriptionValidatorTest {

    private static final int MIN_VIOLATED = 1;
    private static final int MAX_VIOLATED = 1234;

    private static final BigDecimal DECIMAL_MIN_VIOLATED = new BigDecimal("1");
    private static final BigDecimal DECIMAL_MAX_VIOLATED = new BigDecimal("3.5");

    private static final BigDecimal MIN = BigDecimal.valueOf(2);
    private static final BigDecimal MAX = BigDecimal.valueOf(3);

    private static final BigDecimal DECIMAL_MIN = new BigDecimal("1.3");
    private static final BigDecimal DECIMAL_MAX = new BigDecimal("3.3");

    private static final String PATTERN = "ab*c";

    private static final String UNKNOWN = "unknown";
    private static final Long INVALID = 0l;

    private static final String BOOL_PARAM_NAME = "bool-param";
    private static final String BOOL_REQUIRED_PARAM_NAME = "bool-required-papram";

    private static final String TXT_PARAM_NAME = "txt-param";
    private static final String TXT_REQUIRED_PARAM_NAME = "txt-required-papram";
    private static final String TXT_MIN_PARAM_NAME = "txt-min-name";
    private static final String TXT_MAX_PARAM_NAME = "txt-max-name";
    private static final String TXT_PATTERN_PARAM_NAME = "txt-pattern-name";
    private static final String TXT_MAX_PATTERN_PARAM_NAME = "txt-max-pattern-name";

    private static final String INT_PARAM_NAME = "int-param";
    private static final String INT_REQUIRED_PARAM_NAME = "int-required-papram";
    private static final String INT_MIN_PARAM_NAME = "int-min-name";
    private static final String INT_MAX_PARAM_NAME = "int-max-name";

    private static final String DECIMAL_PARAM_NAME = "decimal-param";
    private static final String DECIMAL_REQUIRED_PARAM_NAME = "decimal-required-papram";
    private static final String DECIMAL_MIN_PARAM_NAME = "decimal-min-name";
    private static final String DECIMAL_MAX_PARAM_NAME = "decimal-max-name";

    private static final ConfigDescriptionParameter BOOL_PARAM = ConfigDescriptionParameterBuilder
            .create(BOOL_PARAM_NAME, ConfigDescriptionParameter.Type.BOOLEAN).build();
    private static final ConfigDescriptionParameter BOOL_REQUIRED_PARAM = ConfigDescriptionParameterBuilder
            .create(BOOL_REQUIRED_PARAM_NAME, ConfigDescriptionParameter.Type.BOOLEAN).withRequired(true).build();

    private static final ConfigDescriptionParameter TXT_PARAM = ConfigDescriptionParameterBuilder
            .create(TXT_PARAM_NAME, ConfigDescriptionParameter.Type.TEXT).build();
    private static final ConfigDescriptionParameter TXT_REQUIRED_PARAM = ConfigDescriptionParameterBuilder
            .create(TXT_REQUIRED_PARAM_NAME, ConfigDescriptionParameter.Type.TEXT).withRequired(true).build();
    private static final ConfigDescriptionParameter TXT_MIN_PARAM = ConfigDescriptionParameterBuilder
            .create(TXT_MIN_PARAM_NAME, ConfigDescriptionParameter.Type.TEXT).withMinimum(MIN).build();
    private static final ConfigDescriptionParameter TXT_MAX_PARAM = ConfigDescriptionParameterBuilder
            .create(TXT_MAX_PARAM_NAME, ConfigDescriptionParameter.Type.TEXT).withMaximum(MAX).build();
    private static final ConfigDescriptionParameter TXT_PATTERN_PARAM = ConfigDescriptionParameterBuilder
            .create(TXT_PATTERN_PARAM_NAME, ConfigDescriptionParameter.Type.TEXT).withPattern(PATTERN).build();
    private static final ConfigDescriptionParameter TXT_MAX_PATTERN_PARAM = ConfigDescriptionParameterBuilder
            .create(TXT_MAX_PATTERN_PARAM_NAME, ConfigDescriptionParameter.Type.TEXT).withMaximum(MAX)
            .withPattern(PATTERN).build();

    private static final ConfigDescriptionParameter INT_PARAM = ConfigDescriptionParameterBuilder
            .create(INT_PARAM_NAME, ConfigDescriptionParameter.Type.INTEGER).build();
    private static final ConfigDescriptionParameter INT_REQUIRED_PARAM = ConfigDescriptionParameterBuilder
            .create(INT_REQUIRED_PARAM_NAME, ConfigDescriptionParameter.Type.INTEGER).withRequired(true).build();
    private static final ConfigDescriptionParameter INT_MIN_PARAM = ConfigDescriptionParameterBuilder
            .create(INT_MIN_PARAM_NAME, ConfigDescriptionParameter.Type.INTEGER).withMinimum(MIN).build();
    private static final ConfigDescriptionParameter INT_MAX_PARAM = ConfigDescriptionParameterBuilder
            .create(INT_MAX_PARAM_NAME, ConfigDescriptionParameter.Type.INTEGER).withMaximum(MAX).build();

    private static final ConfigDescriptionParameter DECIMAL_PARAM = ConfigDescriptionParameterBuilder
            .create(DECIMAL_PARAM_NAME, ConfigDescriptionParameter.Type.DECIMAL).build();
    private static final ConfigDescriptionParameter DECIMAL_REQUIRED_PARAM = ConfigDescriptionParameterBuilder
            .create(DECIMAL_REQUIRED_PARAM_NAME, ConfigDescriptionParameter.Type.DECIMAL).withRequired(true).build();
    private static final ConfigDescriptionParameter DECIMAL_MIN_PARAM = ConfigDescriptionParameterBuilder
            .create(DECIMAL_MIN_PARAM_NAME, ConfigDescriptionParameter.Type.DECIMAL).withMinimum(DECIMAL_MIN).build();
    private static final ConfigDescriptionParameter DECIMAL_MAX_PARAM = ConfigDescriptionParameterBuilder
            .create(DECIMAL_MAX_PARAM_NAME, ConfigDescriptionParameter.Type.DECIMAL).withMaximum(DECIMAL_MAX).build();

    private static final URI CONFIG_DESCRIPTION_URI = createURI("config:dummy");

    private static final URI createURI(String s) {
        try {
            return new URI(s);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to create URI: " + e.getMessage(), e);
        }
    }

    private static final ConfigDescription CONFIG_DESCRIPTION = new ConfigDescription(CONFIG_DESCRIPTION_URI,
            Stream.of(BOOL_PARAM, BOOL_REQUIRED_PARAM, TXT_PARAM, TXT_REQUIRED_PARAM, TXT_MIN_PARAM, TXT_MAX_PARAM,
                    TXT_PATTERN_PARAM, TXT_MAX_PATTERN_PARAM, INT_PARAM, INT_REQUIRED_PARAM, INT_MIN_PARAM,
                    INT_MAX_PARAM, DECIMAL_PARAM, DECIMAL_REQUIRED_PARAM, DECIMAL_MIN_PARAM, DECIMAL_MAX_PARAM)
                    .collect(toList()));

    private Map<String, Object> params;
    private ConfigDescriptionValidatorImpl configDescriptionValidator;

    @Before
    public void setUp() {
        ConfigDescriptionRegistry configDescriptionRegistry = mock(ConfigDescriptionRegistry.class);
        when(configDescriptionRegistry.getConfigDescription(any())).thenAnswer(new Answer<ConfigDescription>() {
            @Override
            public ConfigDescription answer(InvocationOnMock invocation) throws Throwable {
                URI uri = (URI) invocation.getArgument(0);
                return !CONFIG_DESCRIPTION_URI.equals(uri) ? null : CONFIG_DESCRIPTION;
            }
        });

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle()).thenReturn(mock(Bundle.class));

        configDescriptionValidator = new ConfigDescriptionValidatorImpl();
        configDescriptionValidator.setConfigDescriptionRegistry(configDescriptionRegistry);
        configDescriptionValidator.activate(bundleContext);

        params = new LinkedHashMap<>();
        params.put(BOOL_PARAM_NAME, null);
        params.put(BOOL_REQUIRED_PARAM_NAME, Boolean.FALSE);
        params.put(TXT_PARAM_NAME, null);
        params.put(TXT_REQUIRED_PARAM_NAME, "");
        params.put(TXT_MIN_PARAM_NAME, String.valueOf(MAX_VIOLATED));
        params.put(TXT_MAX_PARAM_NAME, String.valueOf(MIN_VIOLATED));
        params.put(TXT_PATTERN_PARAM_NAME, "abbbc");
        params.put(TXT_MAX_PATTERN_PARAM_NAME, "abc");
        params.put(INT_PARAM_NAME, null);
        params.put(INT_REQUIRED_PARAM_NAME, 0);
        params.put(INT_MIN_PARAM_NAME, MIN);
        params.put(INT_MAX_PARAM_NAME, MAX);
        params.put(DECIMAL_PARAM_NAME, null);
        params.put(DECIMAL_REQUIRED_PARAM_NAME, 0f);
        params.put(DECIMAL_MIN_PARAM_NAME, DECIMAL_MIN);
        params.put(DECIMAL_MAX_PARAM_NAME, DECIMAL_MAX);
    }

    @Test
    public void assertValidationThrowsNoExceptionForValidConfigParameters() {
        configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
    }

    // ===========================================================================
    // REQUIRED VALIDATIONS
    // ===========================================================================

    @Test
    public void assertValidationThrowsExceptionForMissingRequiredBooleanConfiParameter() {
        assertRequired(BOOL_REQUIRED_PARAM_NAME);
    }

    @Test
    public void assertValidationThrowsExceptionForMissingRequiredTxtConfigParameter() {
        assertRequired(TXT_REQUIRED_PARAM_NAME);
    }

    @Test
    public void assertValidationThrowsExceptionForMissingRequiredIntConfigParameter() {
        assertRequired(INT_REQUIRED_PARAM_NAME);
    }

    @Test
    public void assertValidationThrowsExceptionForMissingRequiredDecimalConfigParameter() {
        assertRequired(DECIMAL_REQUIRED_PARAM_NAME);
    }

    @Test
    public void assertValidationThrowsExceptionContainingMessagesForAllRequiredConfigParameters() {
        List<ConfigValidationMessage> expected = Stream.of(
                new ConfigValidationMessage(BOOL_REQUIRED_PARAM_NAME, MessageKey.PARAMETER_REQUIRED.defaultMessage,
                        MessageKey.PARAMETER_REQUIRED.key),
                new ConfigValidationMessage(TXT_REQUIRED_PARAM_NAME, MessageKey.PARAMETER_REQUIRED.defaultMessage,
                        MessageKey.PARAMETER_REQUIRED.key),
                new ConfigValidationMessage(INT_REQUIRED_PARAM_NAME, MessageKey.PARAMETER_REQUIRED.defaultMessage,
                        MessageKey.PARAMETER_REQUIRED.key),
                new ConfigValidationMessage(DECIMAL_REQUIRED_PARAM_NAME, MessageKey.PARAMETER_REQUIRED.defaultMessage,
                        MessageKey.PARAMETER_REQUIRED.key))
                .collect(toList());
        try {
            params.put(BOOL_PARAM_NAME, null);
            params.put(TXT_PARAM_NAME, null);
            params.put(INT_PARAM_NAME, null);
            params.put(DECIMAL_PARAM_NAME, null);
            params.put(BOOL_REQUIRED_PARAM_NAME, null);
            params.put(TXT_REQUIRED_PARAM_NAME, null);
            params.put(INT_REQUIRED_PARAM_NAME, null);
            params.put(DECIMAL_REQUIRED_PARAM_NAME, null);
            configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
            failBecauseOfMissingConfigValidationException();
        } catch (ConfigValidationException e) {
            assertThat(getConfigValidationMessages(e), is(expected));
        }
    }

    void assertRequired(String parameterName) {
        List<ConfigValidationMessage> expected = Collections.singletonList(new ConfigValidationMessage(parameterName,
                MessageKey.PARAMETER_REQUIRED.defaultMessage, MessageKey.PARAMETER_REQUIRED.key));

        try {
            params.put(parameterName, null);
            configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
            failBecauseOfMissingConfigValidationException();
        } catch (ConfigValidationException e) {
            assertThat(getConfigValidationMessages(e), is(expected));
        }
    }

    // ===========================================================================
    // MIN MAX VALIDATIONS
    // ===========================================================================

    @Test
    public void assertValidationThrowsExceptionForInvalidMinAttributeOfTxtConfigParameter() {
        assertMinMax(TXT_MIN_PARAM_NAME, String.valueOf(MIN_VIOLATED), MessageKey.MIN_VALUE_TXT_VIOLATED,
                MIN.toString());
    }

    @Test
    public void assertValidationThrowsExceptionForInvalidMaxAttributeOfTxtConfigParameter() {
        assertMinMax(TXT_MAX_PARAM_NAME, String.valueOf(MAX_VIOLATED), MessageKey.MAX_VALUE_TXT_VIOLATED,
                MAX.toString());
    }

    @Test
    public void assertValidationThrowsExceptionForInvalidMinAttributeOfIntConfigParameter() {
        assertMinMax(INT_MIN_PARAM_NAME, MIN_VIOLATED, MessageKey.MIN_VALUE_NUMERIC_VIOLATED, MIN.toString());
    }

    @Test
    public void assertValidationThrowsExceptionForInvalidMaxAttributeOfIntConfigParameter() {
        assertMinMax(INT_MAX_PARAM_NAME, MAX_VIOLATED, MessageKey.MAX_VALUE_NUMERIC_VIOLATED, MAX.toString());
    }

    @Test
    public void assertValidationThrowsExceptionForInvalidMinAttributeOfDecimalConfigParameter() {
        assertMinMax(DECIMAL_MIN_PARAM_NAME, DECIMAL_MIN_VIOLATED, MessageKey.MIN_VALUE_NUMERIC_VIOLATED,
                DECIMAL_MIN.toString());
    }

    @Test
    public void assertValidationThrowsExceptionForInvalidMaxAttributeOfDecimalConfigParameter() {
        assertMinMax(DECIMAL_MAX_PARAM_NAME, DECIMAL_MAX_VIOLATED, MessageKey.MAX_VALUE_NUMERIC_VIOLATED,
                DECIMAL_MAX.toString());
    }

    @Test
    public void assertValidationThrowsExceptionContainingMessagesForAllMinMaxConfigParameters() {
        List<ConfigValidationMessage> expected = Stream.of(
                new ConfigValidationMessage(TXT_MIN_PARAM_NAME, MessageKey.MIN_VALUE_TXT_VIOLATED.defaultMessage,
                        MessageKey.MIN_VALUE_TXT_VIOLATED.key, MIN.toString()),
                new ConfigValidationMessage(TXT_MAX_PARAM_NAME, MessageKey.MAX_VALUE_TXT_VIOLATED.defaultMessage,
                        MessageKey.MAX_VALUE_TXT_VIOLATED.key, MAX.toString()),
                new ConfigValidationMessage(INT_MIN_PARAM_NAME, MessageKey.MIN_VALUE_NUMERIC_VIOLATED.defaultMessage,
                        MessageKey.MIN_VALUE_NUMERIC_VIOLATED.key, MIN.toString()),
                new ConfigValidationMessage(INT_MAX_PARAM_NAME, MessageKey.MAX_VALUE_NUMERIC_VIOLATED.defaultMessage,
                        MessageKey.MAX_VALUE_NUMERIC_VIOLATED.key, MAX.toString()),
                new ConfigValidationMessage(DECIMAL_MIN_PARAM_NAME,
                        MessageKey.MIN_VALUE_NUMERIC_VIOLATED.defaultMessage, MessageKey.MIN_VALUE_NUMERIC_VIOLATED.key,
                        DECIMAL_MIN.toString()),
                new ConfigValidationMessage(DECIMAL_MAX_PARAM_NAME,
                        MessageKey.MAX_VALUE_NUMERIC_VIOLATED.defaultMessage, MessageKey.MAX_VALUE_NUMERIC_VIOLATED.key,
                        DECIMAL_MAX.toString()))
                .collect(toList());
        try {
            params.put(TXT_MIN_PARAM_NAME, String.valueOf(MIN_VIOLATED));
            params.put(TXT_MAX_PARAM_NAME, String.valueOf(MAX_VIOLATED));
            params.put(INT_MIN_PARAM_NAME, MIN_VIOLATED);
            params.put(INT_MAX_PARAM_NAME, MAX_VIOLATED);
            params.put(DECIMAL_MIN_PARAM_NAME, DECIMAL_MIN_VIOLATED);
            params.put(DECIMAL_MAX_PARAM_NAME, DECIMAL_MAX_VIOLATED);
            configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
            failBecauseOfMissingConfigValidationException();
        } catch (ConfigValidationException e) {
            assertThat(getConfigValidationMessages(e), is(expected));
        }
    }

    void assertMinMax(String parameterName, Object value, MessageKey msgKey, String minMax) {
        List<ConfigValidationMessage> expected = Collections
                .singletonList(new ConfigValidationMessage(parameterName, msgKey.defaultMessage, msgKey.key, minMax));
        try {
            params.put(parameterName, value);
            configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
            failBecauseOfMissingConfigValidationException();
        } catch (ConfigValidationException e) {
            assertThat(getConfigValidationMessages(e), is(expected));
        }
    }

    // ===========================================================================
    // TYPE VALIDATIONS
    // ===========================================================================

    @Test
    public void assertValidationThrowsExceptionForInvalidTypeForBooleanConfigParameter() {
        assertType(BOOL_PARAM_NAME, Type.BOOLEAN);
    }

    @Test
    public void assertValidationThrowsExceptionForInvalidTypeForTxtConfigParameter() {
        assertType(TXT_PARAM_NAME, Type.TEXT);
    }

    @Test
    public void assertValidationThrowsExceptionForInvalidTypeForIntConfigParameter() {
        assertType(INT_PARAM_NAME, Type.INTEGER);
    }

    @Test
    public void assertValidationThrowsExceptionForInvalidTypeForDecimalConfigParameter() {
        assertType(DECIMAL_PARAM_NAME, Type.DECIMAL);
    }

    @Test
    public void assertValidationThrowsExceptionContainingMessagesForMultipleInvalidTypedConfigParameters() {
        List<ConfigValidationMessage> expected = Stream.of(
                new ConfigValidationMessage(BOOL_PARAM_NAME, MessageKey.DATA_TYPE_VIOLATED.defaultMessage,
                        MessageKey.DATA_TYPE_VIOLATED.key, Type.BOOLEAN),
                new ConfigValidationMessage(TXT_PARAM_NAME, MessageKey.DATA_TYPE_VIOLATED.defaultMessage,
                        MessageKey.DATA_TYPE_VIOLATED.key, Type.TEXT),
                new ConfigValidationMessage(INT_PARAM_NAME, MessageKey.DATA_TYPE_VIOLATED.defaultMessage,
                        MessageKey.DATA_TYPE_VIOLATED.key, Type.INTEGER),
                new ConfigValidationMessage(DECIMAL_PARAM_NAME, MessageKey.DATA_TYPE_VIOLATED.defaultMessage,
                        MessageKey.DATA_TYPE_VIOLATED.key, Type.DECIMAL))
                .collect(toList());
        try {
            params.put(BOOL_PARAM_NAME, INVALID);
            params.put(TXT_PARAM_NAME, INVALID);
            params.put(INT_PARAM_NAME, INVALID);
            params.put(DECIMAL_PARAM_NAME, INVALID);
            configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
            failBecauseOfMissingConfigValidationException();
        } catch (ConfigValidationException e) {
            assertThat(getConfigValidationMessages(e), is(expected));
        }
    }

    void assertType(String parameterName, Type type) {
        List<ConfigValidationMessage> expected = Collections.singletonList(new ConfigValidationMessage(parameterName,
                MessageKey.DATA_TYPE_VIOLATED.defaultMessage, MessageKey.DATA_TYPE_VIOLATED.key, type));
        try {
            params.put(parameterName, INVALID);
            configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
            failBecauseOfMissingConfigValidationException();
        } catch (ConfigValidationException e) {
            assertThat(getConfigValidationMessages(e), is(expected));
        }
    }

    // ===========================================================================
    // PATTERN VALIDATIONS
    // ===========================================================================

    @Test
    public void assertValidationThrowsExceptionContainingMessagesForInvalidPatternForTxtConfigParameters() {
        List<ConfigValidationMessage> expected = Collections.singletonList(
                new ConfigValidationMessage(TXT_PATTERN_PARAM_NAME, MessageKey.PATTERN_VIOLATED.defaultMessage,
                        MessageKey.PATTERN_VIOLATED.key, String.valueOf(MAX_VIOLATED), PATTERN));
        try {
            params.put(TXT_PATTERN_PARAM_NAME, String.valueOf(MAX_VIOLATED));
            configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
            failBecauseOfMissingConfigValidationException();
        } catch (ConfigValidationException e) {
            assertThat(getConfigValidationMessages(e), is(expected));
        }
    }

    // ===========================================================================
    // MISC VALIDATIONS
    // ===========================================================================

    @Test
    public void assertValidationThrowsExceptionContainingMultipleVariousViolations() {
        List<ConfigValidationMessage> expected = Stream.of(
                new ConfigValidationMessage(BOOL_REQUIRED_PARAM_NAME, MessageKey.PARAMETER_REQUIRED.defaultMessage,
                        MessageKey.PARAMETER_REQUIRED.key),
                new ConfigValidationMessage(TXT_REQUIRED_PARAM_NAME, MessageKey.PARAMETER_REQUIRED.defaultMessage,
                        MessageKey.PARAMETER_REQUIRED.key),
                new ConfigValidationMessage(TXT_MAX_PARAM_NAME, MessageKey.MAX_VALUE_TXT_VIOLATED.defaultMessage,
                        MessageKey.MAX_VALUE_TXT_VIOLATED.key, MAX.toString()),
                new ConfigValidationMessage(TXT_PATTERN_PARAM_NAME, MessageKey.PATTERN_VIOLATED.defaultMessage,
                        MessageKey.PATTERN_VIOLATED.key, String.valueOf(MAX_VIOLATED), PATTERN),
                new ConfigValidationMessage(INT_MIN_PARAM_NAME, MessageKey.MIN_VALUE_NUMERIC_VIOLATED.defaultMessage,
                        MessageKey.MIN_VALUE_NUMERIC_VIOLATED.key, MIN.toString()),
                new ConfigValidationMessage(DECIMAL_PARAM_NAME, MessageKey.DATA_TYPE_VIOLATED.defaultMessage,
                        MessageKey.DATA_TYPE_VIOLATED.key, Type.DECIMAL),
                new ConfigValidationMessage(DECIMAL_MAX_PARAM_NAME,
                        MessageKey.MAX_VALUE_NUMERIC_VIOLATED.defaultMessage, MessageKey.MAX_VALUE_NUMERIC_VIOLATED.key,
                        DECIMAL_MAX.toString()))
                .collect(toList());
        try {
            params.put(BOOL_REQUIRED_PARAM_NAME, null);
            params.put(TXT_REQUIRED_PARAM_NAME, null);
            params.put(TXT_MAX_PARAM_NAME, String.valueOf(MAX_VIOLATED));
            params.put(TXT_PATTERN_PARAM_NAME, String.valueOf(MAX_VIOLATED));
            params.put(INT_MIN_PARAM_NAME, MIN_VIOLATED);
            params.put(DECIMAL_PARAM_NAME, INVALID);
            params.put(DECIMAL_MAX_PARAM_NAME, DECIMAL_MAX_VIOLATED);
            configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
            failBecauseOfMissingConfigValidationException();
        } catch (ConfigValidationException e) {
            assertThat(getConfigValidationMessages(e), is(expected));
        }
    }

    @Test
    public void assertValidationProvidesOnlyOneMessagePerParameterAlthoughMultipleViolationsOccur() {
        List<ConfigValidationMessage> expected = Collections.singletonList(new ConfigValidationMessage(
                TXT_MAX_PATTERN_PARAM_NAME, MessageKey.MAX_VALUE_TXT_VIOLATED.defaultMessage,
                MessageKey.MAX_VALUE_TXT_VIOLATED.key, MAX.toString()));
        try {
            params.put(TXT_MAX_PATTERN_PARAM_NAME, String.valueOf(MAX_VIOLATED));
            configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
            failBecauseOfMissingConfigValidationException();
        } catch (ConfigValidationException e) {
            assertThat(getConfigValidationMessages(e), is(expected));
        }
    }

    @Test
    public void assertValidationDoesNotCareAboutParameterThatIsNotSpecifiedInConfigDescription() {
        params.put(UNKNOWN, null);
        configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);

        params.put(UNKNOWN, MIN_VIOLATED);
        configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);

        params.put(UNKNOWN, MAX_VIOLATED);
        configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);

        params.put(UNKNOWN, INVALID);
        configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
    }

    @Test(expected = NullPointerException.class)
    public void assertValidateThrowsNPEforNullParamerters() {
        configDescriptionValidator.validate(null, CONFIG_DESCRIPTION_URI);
    }

    @Test(expected = NullPointerException.class)
    public void assertValidateThrowsNPEforNullConfigDescriptionUri() {
        configDescriptionValidator.validate(params, null);
    }

    @Test
    public void assertValidateCanHandleUnknownURIs() throws Exception {
        configDescriptionValidator.validate(params, new URI(UNKNOWN));
    }

    @Test
    public void assertValidateCanHandleSituationsWithoutConfigDescriptionRegistry() {
        configDescriptionValidator.setConfigDescriptionRegistry(null);
        configDescriptionValidator.validate(params, CONFIG_DESCRIPTION_URI);
    }

    private static List<ConfigValidationMessage> getConfigValidationMessages(ConfigValidationException cve) {
        try {
            Field field = cve.getClass().getDeclaredField("configValidationMessages");
            field.setAccessible(true);
            return (List<ConfigValidationMessage>) field.get(cve);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to get configValidationMessages: " + e.getMessage(), e);
        }
    }

    private void failBecauseOfMissingConfigValidationException() {
        fail("A config validation exception was expected but it was not thrown");
    }
}
