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
package org.openhab.core.model.yaml.internal.items.fileconverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.items.YamlChannelLinkProvider;
import org.openhab.core.model.yaml.internal.items.YamlItemDTO;
import org.openhab.core.model.yaml.internal.items.YamlItemProvider;
import org.openhab.core.model.yaml.internal.items.YamlMetadataDTO;
import org.openhab.core.model.yaml.internal.items.YamlMetadataProvider;

@NonNullByDefault
public class YamlItemConverterTest {

    @Test
    public void testExpireMetadataConvertedToShortForm() {
        Metadata expireMetadata = new Metadata(new MetadataKey("expire", "item_name"), "10m", Map.of());
        YamlItemDTO dto = convertWithMetadata(expireMetadata, "String");
        assertEquals("10m", dto.expire);
        assertNull(dto.metadata);
    }

    @Test
    public void testExpireMetadataEmptyStringStaysInShortForm() {
        Metadata expireMetadata = new Metadata(new MetadataKey("expire", "item_name"), "", Map.of());
        YamlItemDTO dto = convertWithMetadata(expireMetadata, "String");
        assertEquals("", dto.expire);
        assertNull(dto.metadata);
    }

    @Test
    public void testExpireMetadataWithConfigStaysMetadata() {
        Metadata expireMetadata = new Metadata(new MetadataKey("expire", "item_name"), "10m", Map.of("command", "OFF"));
        YamlItemDTO dto = convertWithMetadata(expireMetadata, "String");
        assertNull(dto.expire);
        assertNotNull(dto.metadata);
        assertTrue(dto.metadata.containsKey("expire"));
        assertEquals("10m", dto.metadata.get("expire").getValue());
        assertEquals("OFF", dto.metadata.get("expire").config.get("command"));
    }

    @Test
    public void testAutoupdateMetadataSetsField() {
        Metadata autoupdate = new Metadata(new MetadataKey("autoupdate", "item_name"), "true", Map.of());
        YamlItemDTO dto = convertWithMetadata(autoupdate, "String");
        assertEquals(Boolean.TRUE, dto.autoupdate);
        assertNull(dto.metadata);
    }

    @Test
    public void testAutoupdateMetadataEmptyStringIsTreatedAsNotSet() {
        Metadata autoupdate = new Metadata(new MetadataKey("autoupdate", "item_name"), "", Map.of());
        YamlItemDTO dto = convertWithMetadata(autoupdate, "String");
        assertNull(dto.autoupdate);
        assertNull(dto.metadata);
    }

    @Test
    public void testUnitMetadataSetsField() {
        Metadata unit = new Metadata(new MetadataKey("unit", "item_name"), "kWh", Map.of());
        YamlItemDTO dto = convertWithMetadata(unit, "Number");
        assertEquals("kWh", dto.unit);
        assertNull(dto.metadata);
    }

    @Test
    public void testUnitMetadataEmptyStringStaysInShortForm() {
        Metadata unit = new Metadata(new MetadataKey("unit", "item_name"), "", Map.of());
        YamlItemDTO dto = convertWithMetadata(unit, "Number");
        assertEquals("", dto.unit);
        assertNull(dto.metadata);
    }

    @Nested
    class StateDescriptionAndFormatTests {

        @Test
        public void testStateDescriptionMetadataConvertedToShortForm() {
            Metadata stateDescriptionMetadata = new Metadata(new MetadataKey("stateDescription", "item_name"), "",
                    Map.of("pattern", "%d"));
            YamlItemDTO dto = convertWithMetadata(stateDescriptionMetadata, "Number");
            assertEquals("%d", dto.format);
            assertNull(dto.metadata);
        }

        @Test
        public void testStateDescriptionMetadataWithOtherConfigStaysInMetadata() {
            Metadata stateDescriptionMetadata = new Metadata(new MetadataKey("stateDescription", "item_name"), "",
                    Map.of("pattern", "%d", "min", 0, "max", 100));
            YamlItemDTO dto = convertWithMetadata(stateDescriptionMetadata, "Number");
            assertNull(dto.format);
            assertNotNull(dto.metadata);
            YamlMetadataDTO stateDescDto = dto.metadata.get("stateDescription");
            assertNotNull(stateDescDto);
            assertEquals("", stateDescDto.getValue());
            assertEquals("%d", stateDescDto.config.get("pattern"));
            assertEquals(0, stateDescDto.config.get("min"));
            assertEquals(100, stateDescDto.config.get("max"));
        }

        @Test
        public void testStandaloneStateFormatterPreservedWithoutStateDescription() {
            // Test standalone stateFormatter (e.g., extracted from item label or file-format API)
            // when no stateDescription metadata exists.
            YamlItemDTO dto = convertWithStateFormatterAndMetadata("Number", "%.2f %s", List.of());
            assertEquals("%.2f %s", dto.format);
            assertNull(dto.metadata);
        }

        @Test
        public void testStandaloneStateFormatterWithStateDescriptionWithoutPattern() {
            // Test when standalone stateFormatter is provided along with stateDescription metadata
            // that has min/max config but NO pattern.
            Metadata stateDescMeta = new Metadata(new MetadataKey("stateDescription", "item_name"), "",
                    Map.of("min", 0, "max", 100));

            YamlItemDTO dto = convertWithStateFormatterAndMetadata("Number", "%.1f °C", List.of(stateDescMeta));

            // When stateDescription metadata exists with other params, dto.format should be cleared
            // because stateDescription takes precedence, but stateDescription config will adopt or keep format
            // parameters.
            assertNull(dto.format);
            assertNotNull(dto.metadata);
            YamlMetadataDTO stateDescDto = dto.metadata.get("stateDescription");
            assertNotNull(stateDescDto);
            assertEquals("", stateDescDto.getValue());
            assertEquals(0, stateDescDto.config.get("min"));
            assertEquals(100, stateDescDto.config.get("max"));
            // Verify adopted or preserved pattern in stateDescription config
            assertEquals("%.1f °C", stateDescDto.config.get("pattern"));
        }

        @Test
        public void testStateDescriptionPatternOverridesStandaloneStateFormatter() {
            // Test when stateDescription metadata HAS a pattern and standalone stateFormatter is also provided.
            // The stateDescription pattern should override/take precedence.
            Metadata stateDescMeta = new Metadata(new MetadataKey("stateDescription", "item_name"), "",
                    Map.of("pattern", "%d kWh"));

            YamlItemDTO dto = convertWithStateFormatterAndMetadata("Number", "%.2f", List.of(stateDescMeta));

            // Short-form format should reflect the stateDescription pattern (%d kWh),
            // not the standalone stateFormatter (%.2f)
            assertEquals("%d kWh", dto.format);
            assertNull(dto.metadata);
        }

        @Test
        public void testStateFormatterDslToYamlRoundTripInteraction() {
            // Simulate DSL -> YAML conversion flow with a standalone stateFormatter and additional metadata
            Metadata expireMeta = new Metadata(new MetadataKey("expire", "item_name"), "5m", Map.of());
            Metadata unitMeta = new Metadata(new MetadataKey("unit", "item_name"), "°C", Map.of());

            YamlItemDTO dto = convertWithStateFormatterAndMetadata("Number:Temperature", "%.1f %unit%",
                    List.of(expireMeta, unitMeta));

            // Standalone stateFormatter is converted to short-form format
            assertEquals("%.1f %unit%", dto.format);
            assertEquals("5m", dto.expire);
            assertEquals("°C", dto.unit);
            assertNull(dto.metadata);
        }
    }

    private YamlItemDTO convertWithMetadata(Metadata metadata, String itemType) {
        CapturingYamlModelRepository repository = new CapturingYamlModelRepository();
        YamlItemConverter converter = new YamlItemConverter(repository, mock(YamlItemProvider.class),
                mock(YamlMetadataProvider.class), mock(YamlChannelLinkProvider.class),
                mock(ConfigDescriptionRegistry.class));

        Item item = mock(Item.class);
        when(item.getName()).thenReturn(metadata.getUID().getItemName());
        when(item.getLabel()).thenReturn(null);
        when(item.getType()).thenReturn(itemType);
        when(item.getCategory()).thenReturn(null);
        when(item.getGroupNames()).thenReturn(List.of());
        when(item.getTags()).thenReturn(Set.of());

        converter.setItemsToBeSerialized("id", List.of(item), List.of(metadata), Map.of(), false);

        List<YamlElement> elements = repository.getElements();
        assertEquals(1, elements.size());
        assertInstanceOf(YamlItemDTO.class, elements.getFirst());
        return (YamlItemDTO) elements.getFirst();
    }

    private YamlItemDTO convertWithStateFormatterAndMetadata(String itemType, @Nullable String stateFormatter,
            List<Metadata> metadataList) {
        CapturingYamlModelRepository repository = new CapturingYamlModelRepository();
        YamlItemConverter converter = new YamlItemConverter(repository, mock(YamlItemProvider.class),
                mock(YamlMetadataProvider.class), mock(YamlChannelLinkProvider.class),
                mock(ConfigDescriptionRegistry.class));

        Item item = mock(Item.class);
        when(item.getName()).thenReturn("item_name");
        when(item.getLabel()).thenReturn(null);
        when(item.getType()).thenReturn(itemType);
        when(item.getCategory()).thenReturn(null);
        when(item.getGroupNames()).thenReturn(List.of());
        when(item.getTags()).thenReturn(Set.of());

        Map<String, String> stateFormatters = stateFormatter != null ? Map.of("item_name", stateFormatter) : Map.of();

        converter.setItemsToBeSerialized("id", List.of(item), metadataList, stateFormatters, false);

        List<YamlElement> elements = repository.getElements();
        assertEquals(1, elements.size());
        assertInstanceOf(YamlItemDTO.class, elements.getFirst());
        return (YamlItemDTO) elements.getFirst();
    }

    private static class CapturingYamlModelRepository implements YamlModelRepository {

        private List<YamlElement> elements = new ArrayList<>();

        @Override
        public void addElementToModel(String modelName, YamlElement element) {
            // no-op
        }

        @Override
        public void removeElementFromModel(String modelName, YamlElement element) {
            // no-op
        }

        @Override
        public void updateElementInModel(String modelName, YamlElement element) {
            // no-op
        }

        @Override
        public void addElementsToBeGenerated(String id, List<YamlElement> elements) {
            this.elements = elements;
        }

        @Override
        public void generateFileFormat(String id, OutputStream out) {
            // no-op
        }

        @Override
        public @Nullable String createIsolatedModel(InputStream inputStream, List<String> errors,
                List<String> warnings) {
            return null;
        }

        @Override
        public void removeIsolatedModel(String modelName) {
            // no-op
        }

        public List<YamlElement> getElements() {
            return elements;
        }
    }
}
