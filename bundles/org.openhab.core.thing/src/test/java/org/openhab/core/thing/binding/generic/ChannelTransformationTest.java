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
package org.openhab.core.thing.binding.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;

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
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The {@link ChannelTransformationTest} contains tests for the {@link ChannelTransformation}
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ChannelTransformationTest {
    private static final String T1_NAME = "TRANSFORM1";
    private static final String T1_PATTERN = "T1Pattern";
    private static final String T1_INPUT = "T1Input";
    private static final String T1_RESULT = "T1Result";

    private static final String T2_NAME = "TRANSFORM2";
    private static final String T2_PATTERN = "T2Pattern";
    private static final String T2_INPUT = T1_RESULT;
    private static final String T2_RESULT = "T2Result";

    private static final String T3_NAME = T2_NAME;
    private static final String T3_PATTERN = "a()b()))";
    private static final String T3_INPUT = T2_RESULT;
    private static final String T3_RESULT = "T3Result";

    private @Mock @NonNullByDefault({}) TransformationService transformationService1Mock;
    private @Mock @NonNullByDefault({}) TransformationService transformationService2Mock;

    private @Mock @NonNullByDefault({}) BundleContext bundleContextMock;
    private @Mock @NonNullByDefault({}) ServiceReference<TransformationService> serviceRef1Mock;
    private @Mock @NonNullByDefault({}) ServiceReference<TransformationService> serviceRef2Mock;

    private @NonNullByDefault({}) TransformationHelper transformationHelper;

    @BeforeEach
    public void init() throws TransformationException {
        Mockito.when(transformationService1Mock.transform(eq(T1_PATTERN), eq(T1_INPUT)))
                .thenAnswer(answer -> T1_RESULT);
        Mockito.when(transformationService2Mock.transform(eq(T2_PATTERN), eq(T1_INPUT)))
                .thenAnswer(answer -> T2_RESULT);
        Mockito.when(transformationService2Mock.transform(eq(T2_PATTERN), eq(T2_INPUT)))
                .thenAnswer(answer -> T2_RESULT);
        Mockito.when(transformationService2Mock.transform(eq(T3_PATTERN), eq(T3_INPUT)))
                .thenAnswer(answer -> T3_RESULT);

        Mockito.when(serviceRef1Mock.getProperty(any())).thenReturn("TRANSFORM1");
        Mockito.when(serviceRef2Mock.getProperty(any())).thenReturn("TRANSFORM2");

        Mockito.when(bundleContextMock.getService(serviceRef1Mock)).thenReturn(transformationService1Mock);
        Mockito.when(bundleContextMock.getService(serviceRef2Mock)).thenReturn(transformationService2Mock);

        transformationHelper = new TransformationHelper(bundleContextMock);
        transformationHelper.setTransformationService(serviceRef1Mock);
        transformationHelper.setTransformationService(serviceRef2Mock);
    }

    @AfterEach
    public void tearDown() {
        transformationHelper.deactivate();
    }

    @Test
    public void testMissingTransformation() {
        String pattern = "TRANSFORM:pattern";

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertNull(result);
    }

    @Test
    public void testSingleTransformationWithColon() {
        String pattern = T1_NAME + ":" + T1_PATTERN;

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T1_RESULT, result);
    }

    @Test
    public void testSingleTransformationWithParens() {
        String pattern = T1_NAME + "(" + T1_PATTERN + ")";

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T1_RESULT, result);
    }

    @Test
    public void testParensTransformationWithNestedParensInPattern() {
        String pattern = T3_NAME + "(" + T3_PATTERN + ")";

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T3_INPUT).orElse(null);

        assertEquals(T3_RESULT, result);
    }

    @Test
    public void testInvalidFirstTransformation() {
        String pattern = T1_NAME + "X:" + T1_PATTERN + "∩" + T2_NAME + ":" + T2_PATTERN;

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertNull(result);
    }

    @Test
    public void testInvalidSecondTransformation() {
        String pattern = T1_NAME + ":" + T1_PATTERN + "∩" + T2_NAME + "X:" + T2_PATTERN;

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertNull(result);
    }

    @Test
    public void testColonDoubleTransformationWithoutSpaces() {
        String pattern = T1_NAME + ":" + T1_PATTERN + "∩" + T2_NAME + ":" + T2_PATTERN;

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }

    @Test
    public void testTransformationsInAList() {
        List<String> patterns = List.of(T1_NAME + ":" + T1_PATTERN, T2_NAME + ":" + T2_PATTERN);

        ChannelTransformation transformation = new ChannelTransformation(patterns);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }

    @Test
    public void testMixedTransformationsInAList1() {
        List<String> patterns = List.of(T1_NAME + ":" + T1_PATTERN + "∩" + T2_NAME + ":" + T2_PATTERN,
                T3_NAME + ":" + T3_PATTERN);

        ChannelTransformation transformation = new ChannelTransformation(patterns);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T3_RESULT, result);
    }

    @Test
    public void testMixedTransformationsInAList2() {
        List<String> patterns = List.of(T1_NAME + ":" + T1_PATTERN,
                T2_NAME + ":" + T2_PATTERN + "∩" + T3_NAME + ":" + T3_PATTERN);

        ChannelTransformation transformation = new ChannelTransformation(patterns);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T3_RESULT, result);
    }

    @Test
    public void testParensDoubleTransformationWithoutSpaces() {
        String pattern = T1_NAME + "(" + T1_PATTERN + ")∩" + T2_NAME + "(" + T2_PATTERN + ")";

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }

    @Test
    public void testMixedDoubleTransformationWithoutSpaces1() {
        String pattern = T1_NAME + ":" + T1_PATTERN + "∩" + T2_NAME + "(" + T2_PATTERN + ")";

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }

    @Test
    public void testMixedDoubleTransformationWithoutSpaces2() {
        String pattern = T1_NAME + "(" + T1_PATTERN + ")∩" + T2_NAME + ":" + T2_PATTERN;

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }

    @Test
    public void testColonDoubleTransformationWithSpaces() {
        String pattern = " " + T1_NAME + " : " + T1_PATTERN + " ∩ " + T2_NAME + " : " + T2_PATTERN + " ";

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }

    @Test
    public void testParensDoubleTransformationWithSpaces() {
        String pattern = " " + T1_NAME + " ( " + T1_PATTERN + " ) ∩ " + T2_NAME + " ( " + T2_PATTERN + " ) ";

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }

    @Test
    public void testMixedDoubleTransformationWithSpaces1() {
        String pattern = " " + T1_NAME + " : " + T1_PATTERN + " ∩ " + T2_NAME + " ( " + T2_PATTERN + " ) ";

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }

    @Test
    public void testMixedDoubleTransformationWithSpaces2() {
        String pattern = " " + T1_NAME + " ( " + T1_PATTERN + " ) ∩ " + T2_NAME + " : " + T2_PATTERN + " ";

        ChannelTransformation transformation = new ChannelTransformation(pattern);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }

    @Test
    public void testIsValidTransform() {
        // single with colon
        assertTrue(ChannelTransformation.isValidTransformation("FOO:BAR"));
        assertTrue(ChannelTransformation.isValidTransformation(" FOO : BAR "));

        // single with parens
        assertTrue(ChannelTransformation.isValidTransformation("FOO(BAR())"));
        assertTrue(ChannelTransformation.isValidTransformation(" FOO ( BAR) )")); // deliberate extra closing parens

        // chained with colon
        assertTrue(ChannelTransformation.isValidTransformation("FOO:BAR∩BAZ:QUX"));
        assertTrue(ChannelTransformation.isValidTransformation("FOO:BAR∩BAZ:QUX()"));
        assertTrue(ChannelTransformation.isValidTransformation(" FOO : BAR ∩ BAZ : QUX() "));

        // chained with parens
        assertTrue(ChannelTransformation.isValidTransformation("FOO(BAR)∩BAZ(QUX)"));
        assertTrue(ChannelTransformation.isValidTransformation("FOO(BAR)∩BAZ(QUX())"));
        assertTrue(ChannelTransformation.isValidTransformation("FOO(BAR)∩BAZ(QUX))")); // deliberate extra parens
        assertTrue(ChannelTransformation.isValidTransformation(" FOO ( BAR ) ∩ BAZ ( QUX )"));
        assertTrue(ChannelTransformation.isValidTransformation(" FOO ( BAR ) ∩ BAZ ( QUX() )"));

        // mixed chains
        assertTrue(ChannelTransformation.isValidTransformation("FOO:BAR∩BAZ(QUX)"));
        assertTrue(ChannelTransformation.isValidTransformation("FOO(BAR)∩BAZ:QUX"));
        assertTrue(ChannelTransformation.isValidTransformation("FOO:BAR()∩BAZ(QUX())"));
        assertTrue(ChannelTransformation.isValidTransformation(" FOO : BAR ∩ BAZ ( QUX ) "));
        assertTrue(ChannelTransformation.isValidTransformation(" FOO ( BAR ) ∩ BAZ : QUX "));

        // invalid syntaxes
        assertFalse(ChannelTransformation.isValidTransformation(null));
        assertFalse(ChannelTransformation.isValidTransformation(""));
        assertFalse(ChannelTransformation.isValidTransformation(" "));
        assertFalse(ChannelTransformation.isValidTransformation("FOOBAR"));
        assertFalse(ChannelTransformation.isValidTransformation("(FOO)BAR"));
        assertFalse(ChannelTransformation.isValidTransformation("FOO∩BAR"));
        assertFalse(ChannelTransformation.isValidTransformation("FOO:BAR∩BAZ"));
        assertFalse(ChannelTransformation.isValidTransformation("FOO(BAR)∩BAZ"));
        assertFalse(ChannelTransformation.isValidTransformation("FOO∩BAZ:BAR"));
        assertFalse(ChannelTransformation.isValidTransformation("FOO∩BAZ(BAR)"));
    }
}
