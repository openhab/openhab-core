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
package org.openhab.core.ui.internal.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.sitemap.Condition;
import org.openhab.core.sitemap.registry.SitemapFactory;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.openhab.core.ui.components.UIComponent;

/**
 * @author Mark Herwege - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
class UIComponentSitemapProviderTest {

    private @Mock @NonNullByDefault({}) SitemapRegistry sitemapRegistry;
    private @Mock @NonNullByDefault({}) SitemapFactory sitemapFactory;
    private @Mock @NonNullByDefault({}) UIComponent component;

    private @InjectMocks @NonNullByDefault({}) UIComponentSitemapProvider uiComponentSitemapProvider;

    private Condition mockCondition() {
        Condition condition = mock(Condition.class);
        when(sitemapFactory.createCondition()).thenReturn(condition);
        return condition;
    }

    @Test
    void testValueOnly() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("value"), component, "key");
        assertEquals(1, result.size());
        verify(condition).setItem(null);
        verify(condition).setCondition(null);
        verify(condition).setValue("value");
    }

    @Test
    void testItemAndValue() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("item value"), component, "key");
        assertEquals(1, result.size());
        verify(condition).setItem("item");
        verify(condition).setCondition(null);
        verify(condition).setValue("value");
    }

    @Test
    void testItemConditionValue() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("item >= 42"), component, "key");
        assertEquals(1, result.size());
        verify(condition).setItem("item");
        verify(condition).setCondition(">=");
        verify(condition).setValue("42");
    }

    @Test
    void testItemConditionValueNoSpaces() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("item>=42"), component, "key");
        assertEquals(1, result.size());
        verify(condition).setItem("item");
        verify(condition).setCondition(">=");
        verify(condition).setValue("42");
    }

    @Test
    void testConditionAndValueOnly() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of(">= 42"), component, "key");
        assertEquals(1, result.size());
        verify(condition).setItem(null);
        verify(condition).setCondition(">=");
        verify(condition).setValue("42");
    }

    @Test
    void testQuotedValue() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("item \"value string\""), component,
                "key");
        assertEquals(1, result.size());
        verify(condition).setItem("item");
        verify(condition).setCondition(null);
        verify(condition).setValue("value string");
    }

    @Test
    void testItemConditionQuotedValue() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("item == \"value string\""),
                component, "key");
        assertEquals(1, result.size());
        verify(condition).setItem("item");
        verify(condition).setCondition("==");
        verify(condition).setValue("value string");
    }

    @Test
    void testItemConditionQuotedValueNoSpaces() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("item==\"value string\""), component,
                "key");
        assertEquals(1, result.size());
        verify(condition).setItem("item");
        verify(condition).setCondition("==");
        verify(condition).setValue("value string");
    }

    @Test
    void testQuotedValueOnly() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("\"value string\""), component,
                "key");
        assertEquals(1, result.size());
        verify(condition).setItem(null);
        verify(condition).setCondition(null);
        verify(condition).setValue("value string");
    }

    @Test
    void testQuotedValueWithConditionOperator() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("\"value == string\""), component,
                "key");
        assertEquals(1, result.size());
        verify(condition).setItem(null);
        verify(condition).setCondition(null);
        verify(condition).setValue("value == string");
    }

    @Test
    void testUnderscorePrefixedItemConditionValueNoSpaces() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("_temp>=42"), component, "key");
        assertEquals(1, result.size());
        verify(condition).setItem("_temp");
        verify(condition).setCondition(">=");
        verify(condition).setValue("42");
    }

    @Test
    void testSingleCharacterItemConditionQuotedValueNoSpaces() {
        Condition condition = mockCondition();
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("T==\"ON\""), component, "key");
        assertEquals(1, result.size());
        verify(condition).setItem("T");
        verify(condition).setCondition("==");
        verify(condition).setValue("ON");
    }

    @Test
    void testInvalidInput() {
        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of(""), component, "key");
        assertEquals(0, result.size());
    }

    @Test
    void testMultipleConditions() {
        Condition condition1 = mock(Condition.class);
        Condition condition2 = mock(Condition.class);
        when(sitemapFactory.createCondition()).thenReturn(condition1).thenReturn(condition2);

        List<Condition> result = uiComponentSitemapProvider.getConditions(List.of("item1 >= 42", "item2 == 10"),
                component, "key");
        assertEquals(2, result.size());
        verify(condition1).setItem("item1");
        verify(condition1).setCondition(">=");
        verify(condition1).setValue("42");
        verify(condition2).setItem("item2");
        verify(condition2).setCondition("==");
        verify(condition2).setValue("10");
    }
}
