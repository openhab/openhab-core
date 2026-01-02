package org.openhab.core.config.core;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;

/**
 * Unit tests for {@link ConfigUtil}.
 *
 * These tests validate:
 * - Utility class design (private constructor enforcement)
 * - Correct type normalization behavior
 * - Defensive handling of invalid inputs
 * - Preservation of existing functionality after perfective maintenance
 */
class ConfigUtilTest {

    /* ---------- Utility class constructor ---------- */

    @Test
    void testUtilityClassCannotBeInstantiated() throws Exception {
        Constructor<ConfigUtil> constructor = ConfigUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        Exception exception = assertThrows(Exception.class, constructor::newInstance);

        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }

    /* ---------- getNumberOfDecimalPlaces ---------- */

    @Test
    void testGetNumberOfDecimalPlaces() {
        assertEquals(0, ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("10")));
        assertEquals(1, ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("10.50")));
        assertEquals(3, ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("0.125")));
    }

    /* ---------- getDefaultValueAsCorrectType ---------- */

    @Test
    void testGetDefaultValueAsCorrectTypeInteger() {
        Object result = ConfigUtil.getDefaultValueAsCorrectType("testParam", Type.INTEGER, "42");

        assertTrue(result instanceof BigDecimal);
        assertEquals(new BigDecimal("42"), result);
    }

    @Test
    void testGetDefaultValueAsCorrectTypeIntegerWithDecimal() {
        Object result = ConfigUtil.getDefaultValueAsCorrectType("testParam", Type.INTEGER, "42.9");

        assertEquals(new BigDecimal("42"), result);
    }

    @Test
    void testGetDefaultValueAsCorrectTypeInvalidNumber() {
        Object result = ConfigUtil.getDefaultValueAsCorrectType("testParam", Type.DECIMAL, "abc");

        assertNull(result);
    }

    /* ---------- normalizeType ---------- */

    @Test
    void testNormalizePrimitiveTypes() {
        assertEquals(true, ConfigUtil.normalizeType(true));
        assertEquals("text", ConfigUtil.normalizeType("text"));
        assertEquals(new BigDecimal("5"), ConfigUtil.normalizeType(5));
        assertEquals(new BigDecimal("5.5"), ConfigUtil.normalizeType(5.5));
    }

    @Test
    void testNormalizeCollectionHomogeneous() {
        List<Integer> input = List.of(1, 2, 3);
        Object result = ConfigUtil.normalizeType(input);

        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertTrue(list.get(0) instanceof BigDecimal);
    }

    @Test
    void testNormalizeCollectionHeterogeneousThrowsException() {
        List<Object> input = List.of(1, "text");

        assertThrows(IllegalArgumentException.class, () -> ConfigUtil.normalizeType(input));
    }

    @Test
    void testNormalizeInvalidTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ConfigUtil.normalizeType(new Object()));
    }

    /* ---------- normalizeTypes(Map) ---------- */

    @Test
    void testNormalizeTypesIgnoresOSGiParameters() {
        Map<String, Object> input = Map.of("validKey", 10, org.osgi.framework.Constants.OBJECTCLASS, "ignored");

        Map<String, Object> result = ConfigUtil.normalizeTypes(input);

        assertTrue(result.containsKey("validKey"));
        assertFalse(result.containsKey(org.osgi.framework.Constants.OBJECTCLASS));
        assertEquals(new BigDecimal("10"), result.get("validKey"));
    }

    /* ---------- normalizeTypes(Map, List) ---------- */

    @Test
    void testNormalizeTypesWithEmptyConfigDescriptionsThrowsException() {
        Map<String, Object> config = Map.of("key", "value");

        assertThrows(IllegalArgumentException.class, () -> ConfigUtil.normalizeTypes(config, List.of()));
    }
}
