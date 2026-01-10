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
package org.openhab.core.io.rest.sse.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The {@link SseItemStatesEventBuilderTest} contains tests for the method getDisplayState from
 * {@link SseItemStatesEventBuilder}
 *
 * @author Laurent Garnier - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class SseItemStatesEventBuilderTest {
    private static final String ITEM_NAME = "test";
    private static final String ITEM_STATE_VALUE = "value";
    private static final String ITEM_STATE_VALUE2 = "other";
    private static final int ITEM_STATE_VALUE3 = 16;
    private static final String ITEM_STATE_OPTION_LABEL = "The value";

    private static final String PATTERN = "__ %s __";
    private static final String PATTERN2 = "__ %d __";

    private static final String TRANSFORM_NAME = "TRANSFORM";
    private static final String TRANSFORM_PATTERN = "Pattern";
    private static final String TRANSFORM_FORMAT = "%s-1";
    private static final String TRANSFORM_INPUT = String.format(TRANSFORM_FORMAT, ITEM_STATE_VALUE);
    private static final String TRANSFORM_INPUT2 = String.format(TRANSFORM_FORMAT, ITEM_STATE_VALUE2);
    private static final String TRANSFORM_RESULT = "Result with string";
    private static final String TRANSFORM_FORMAT_NUMBER = "_%d_";
    private static final String TRANSFORM_INPUT3 = String.format(TRANSFORM_FORMAT_NUMBER, ITEM_STATE_VALUE3);
    private static final String TRANSFORM_RESULT_NUMBER = "Result with number";
    private static final String TRANSFORM_RESULT_NULL = "State is NULL";
    private static final String TRANSFORM_RESULT_UNDEF = "State is UNDEF";

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) LocaleService localeServiceMock;
    private @Mock @NonNullByDefault({}) TimeZoneProvider timeZoneProviderMock;
    private @Mock @NonNullByDefault({}) StartLevelService startLevelServiceMock;

    private @Mock @NonNullByDefault({}) Item itemMock;

    private @Mock @NonNullByDefault({}) TransformationService transformationServiceMock;

    private @Mock @NonNullByDefault({}) BundleContext bundleContextMock;
    private @Mock @NonNullByDefault({}) ServiceReference<TransformationService> serviceRefMock;

    private @NonNullByDefault({}) TransformationHelper transformationHelper;

    private @NonNullByDefault({}) SseItemStatesEventBuilder sseItemStatesEventBuilder;

    @BeforeEach
    public void init() throws TransformationException {
        Mockito.when(transformationServiceMock.transform(eq(TRANSFORM_PATTERN), eq(TRANSFORM_INPUT)))
                .thenAnswer(answer -> TRANSFORM_RESULT);
        Mockito.when(transformationServiceMock.transform(eq(TRANSFORM_PATTERN), eq(TRANSFORM_INPUT2)))
                .thenAnswer(answer -> null);
        Mockito.when(transformationServiceMock.transform(eq(TRANSFORM_PATTERN), eq(TRANSFORM_INPUT3)))
                .thenAnswer(answer -> TRANSFORM_RESULT_NUMBER);
        Mockito.when(transformationServiceMock.transform(eq(TRANSFORM_PATTERN), eq("NULL")))
                .thenAnswer(answer -> TRANSFORM_RESULT_NULL);
        Mockito.when(transformationServiceMock.transform(eq(TRANSFORM_PATTERN), eq("UNDEF")))
                .thenAnswer(answer -> TRANSFORM_RESULT_UNDEF);

        Mockito.when(serviceRefMock.getProperty(any())).thenReturn(TRANSFORM_NAME);

        Mockito.when(bundleContextMock.getService(serviceRefMock)).thenReturn(transformationServiceMock);

        transformationHelper = new TransformationHelper(bundleContextMock);
        transformationHelper.setTransformationService(serviceRefMock);

        Mockito.when(itemMock.getName()).thenReturn(ITEM_NAME);

        sseItemStatesEventBuilder = new SseItemStatesEventBuilder(itemRegistryMock, localeServiceMock,
                timeZoneProviderMock, startLevelServiceMock);
    }

    @AfterEach
    public void tearDown() {
        transformationHelper.deactivate();
    }

    @Test
    public void getDisplayStateWhenMatchingStateOptionAndNoPattern() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create()
                .withOption(new StateOption(ITEM_STATE_VALUE, ITEM_STATE_OPTION_LABEL)).build().toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);

        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(ITEM_STATE_OPTION_LABEL, result);
    }

    @Test
    public void getDisplayStateWhenNoMatchingStateOptionAndNoPattern() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create()
                .withOption(new StateOption(ITEM_STATE_VALUE, ITEM_STATE_OPTION_LABEL)).build().toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);

        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE2));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(ITEM_STATE_VALUE2, result);
    }

    @Test
    public void getDisplayStateWhenMatchingStateOptionAndPattern() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern(PATTERN)
                .withOption(new StateOption(ITEM_STATE_VALUE, ITEM_STATE_OPTION_LABEL)).build().toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);

        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(String.format(PATTERN, ITEM_STATE_OPTION_LABEL), result);
    }

    @Test
    public void getDisplayStateWhenMatchingStateOptionAndWrongPattern() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern(PATTERN2)
                .withOption(new StateOption(ITEM_STATE_VALUE, ITEM_STATE_OPTION_LABEL)).build().toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);

        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(ITEM_STATE_OPTION_LABEL, result);
    }

    @Test
    public void getDisplayStateWhenNoMatchingStateOptionAndPattern() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern(PATTERN)
                .withOption(new StateOption(ITEM_STATE_VALUE, ITEM_STATE_OPTION_LABEL)).build().toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);

        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE2));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(String.format(PATTERN, ITEM_STATE_VALUE2), result);
    }

    @Test
    public void getDisplayStateWhenTransformAndNoStateOption() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create()
                .withPattern(TRANSFORM_NAME + "(" + TRANSFORM_PATTERN + "):" + TRANSFORM_FORMAT).build()
                .toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);
        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(TRANSFORM_RESULT, result);

        StateDescription stateDescription2 = StateDescriptionFragmentBuilder.create()
                .withPattern(TRANSFORM_NAME + "(" + TRANSFORM_PATTERN + "):" + TRANSFORM_FORMAT_NUMBER).build()
                .toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription2);
        Mockito.when(itemMock.getState()).thenReturn(new DecimalType(ITEM_STATE_VALUE3));
        result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(TRANSFORM_RESULT_NUMBER, result);
    }

    @Test
    public void getDisplayStateWhenTransformAndMatchingStateOption() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create()
                .withPattern(TRANSFORM_NAME + "(" + TRANSFORM_PATTERN + "):" + TRANSFORM_FORMAT)
                .withOption(new StateOption(ITEM_STATE_VALUE, ITEM_STATE_OPTION_LABEL)).build().toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);
        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(TRANSFORM_RESULT, result);

        StateDescription stateDescription2 = StateDescriptionFragmentBuilder.create()
                .withPattern(TRANSFORM_NAME + "(" + TRANSFORM_PATTERN + "):" + TRANSFORM_FORMAT_NUMBER).build()
                .toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription2);
        Mockito.when(itemMock.getState()).thenReturn(new DecimalType(ITEM_STATE_VALUE3));
        result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(TRANSFORM_RESULT_NUMBER, result);
    }

    @Test
    public void getDisplayStateWhenTransformReturningNull() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create()
                .withPattern(TRANSFORM_NAME + "(" + TRANSFORM_PATTERN + "):" + TRANSFORM_FORMAT).build()
                .toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);
        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE2));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(ITEM_STATE_VALUE2, result);
    }

    @Test
    public void getDisplayStateWhenStateUndef() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern(PATTERN)
                .withOption(new StateOption(ITEM_STATE_VALUE, ITEM_STATE_OPTION_LABEL)).build().toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);
        Mockito.when(itemMock.getState()).thenReturn(UnDefType.UNDEF);
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals("UNDEF", result);
    }

    @Test
    public void getDisplayStateWhenStateNull() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern(PATTERN)
                .withOption(new StateOption(ITEM_STATE_VALUE, ITEM_STATE_OPTION_LABEL)).build().toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);
        Mockito.when(itemMock.getState()).thenReturn(UnDefType.NULL);
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals("NULL", result);
    }

    @Test
    public void getDisplayStateWhenTransformAndStateUndef() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern(PATTERN)
                .withPattern(TRANSFORM_NAME + "(" + TRANSFORM_PATTERN + "):" + TRANSFORM_FORMAT_NUMBER).build()
                .toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);
        Mockito.when(itemMock.getState()).thenReturn(UnDefType.UNDEF);
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(TRANSFORM_RESULT_UNDEF, result);
    }

    @Test
    public void getDisplayStateWhenTransformAndStateNull() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern(PATTERN)
                .withPattern(TRANSFORM_NAME + "(" + TRANSFORM_PATTERN + "):" + TRANSFORM_FORMAT_NUMBER).build()
                .toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);
        Mockito.when(itemMock.getState()).thenReturn(UnDefType.NULL);
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(TRANSFORM_RESULT_NULL, result);
    }

    @Test
    public void getDisplayStateWhenPatternProvided() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern(PATTERN).build()
                .toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);
        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(String.format(PATTERN, ITEM_STATE_VALUE), result);

        StateDescription stateDescription2 = StateDescriptionFragmentBuilder.create().withPattern(PATTERN2).build()
                .toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription2);
        Mockito.when(itemMock.getState()).thenReturn(new DecimalType(ITEM_STATE_VALUE3));
        result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(String.format(PATTERN2, ITEM_STATE_VALUE3), result);
    }

    @Test
    public void getDisplayStateWhenWrongPatternProvided() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().withPattern(PATTERN2).build()
                .toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);
        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(ITEM_STATE_VALUE, result);
    }

    @Test
    public void getDisplayStateWhenNoPatternProvided() {
        StateDescription stateDescription = StateDescriptionFragmentBuilder.create().build().toStateDescription();
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(stateDescription);
        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(ITEM_STATE_VALUE, result);

        Mockito.when(itemMock.getState()).thenReturn(new DecimalType(ITEM_STATE_VALUE3));
        result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(String.format("%d", ITEM_STATE_VALUE3), result);
    }

    @Test
    public void getDisplayStateWhenNoStateDescription() {
        Mockito.when(itemMock.getStateDescription(eq(Locale.ENGLISH))).thenReturn(null);
        Mockito.when(itemMock.getState()).thenReturn(new StringType(ITEM_STATE_VALUE));
        String result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(ITEM_STATE_VALUE, result);

        Mockito.when(itemMock.getState()).thenReturn(new DecimalType(ITEM_STATE_VALUE3));
        result = sseItemStatesEventBuilder.getDisplayState(itemMock, Locale.ENGLISH);
        assertEquals(String.format("%d", ITEM_STATE_VALUE3), result);
    }
}
