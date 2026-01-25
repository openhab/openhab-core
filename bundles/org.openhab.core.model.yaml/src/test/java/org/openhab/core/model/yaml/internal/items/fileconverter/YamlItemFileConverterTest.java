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
public class YamlItemFileConverterTest {

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

    @Test
    public void testStateDescriptionMetadataConvertedToShortForm() {
        Metadata expireMetadata = new Metadata(new MetadataKey("stateDescription", "item_name"), "", Map.of("pattern", "%d"));
        YamlItemDTO dto = convertWithMetadata(expireMetadata, "Number");
        assertEquals("%d", dto.format);
        assertNull(dto.metadata);
    }

    @Test
    public void testStateDescriptionMetadataWithOtherConfigStaysInMetadata() {
        Metadata expireMetadata = new Metadata(new MetadataKey("stateDescription", "item_name"), "",
                Map.of("pattern", "%d", "min", 0, "max", 100));
        YamlItemDTO dto = convertWithMetadata(expireMetadata, "Number");
        assertNull(dto.format);
        assertNotNull(dto.metadata);
        YamlMetadataDTO stateDescDto = dto.metadata.get("stateDescription");
        assertNotNull(stateDescDto);
        assertEquals("", stateDescDto.getValue());
        assertEquals("%d", stateDescDto.config.get("pattern"));
        assertEquals(0, stateDescDto.config.get("min"));
        assertEquals(100, stateDescDto.config.get("max"));
    }

    private YamlItemDTO convertWithMetadata(Metadata metadata, String itemType) {
        CapturingYamlModelRepository repository = new CapturingYamlModelRepository();
        YamlItemFileConverter converter = new YamlItemFileConverter(repository, mock(YamlItemProvider.class),
                mock(YamlMetadataProvider.class), mock(YamlChannelLinkProvider.class),
                mock(ConfigDescriptionRegistry.class));

        Item item = mock(Item.class);
        when(item.getName()).thenReturn(metadata.getUID().getItemName());
        when(item.getLabel()).thenReturn(null);
        when(item.getType()).thenReturn(itemType);
        when(item.getCategory()).thenReturn(null);
        when(item.getGroupNames()).thenReturn(List.of());
        when(item.getTags()).thenReturn(Set.of());

        converter.setItemsToBeGenerated("id", List.of(item), List.of(metadata), Map.of(), false);

        List<YamlElement> elements = repository.getElements();
        assertEquals(1, elements.size());
        assertInstanceOf(YamlItemDTO.class, elements.get(0));
        return (YamlItemDTO) elements.get(0);
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
