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
package org.openhab.core.voice.internal.text.interpreter.llm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;

/**
 * Test class for {@link FindItemsLLMTool}.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public class FindItemsLLMToolTest {
    private final ItemRegistry itemRegistry = mock(ItemRegistry.class);
    private final ItemPermissionResolver itemPermissionResolver = mock(ItemPermissionResolver.class);
    private final MetadataRegistry metadataRegistry = mock(MetadataRegistry.class);
    private final TimeZoneProvider timeZoneProvider = mock(TimeZoneProvider.class);
    private final List<Item> items = new ArrayList<>();

    private @NonNullByDefault({}) FindItemsLLMTool tool;

    @BeforeEach
    public void setUp() {
        when(itemRegistry.getItems()).thenReturn(items);
        when(itemRegistry.get(any())).thenAnswer(invocation -> items.stream()
                .filter(item -> item.getName().equals(invocation.getArgument(0))).findFirst().orElse(null));
        when(itemPermissionResolver.getPermission(any())).thenReturn(ItemPermission.READ_WRITE);
        when(timeZoneProvider.getTimeZone()).thenReturn(ZoneId.systemDefault());

        tool = new FindItemsLLMTool(itemRegistry, itemPermissionResolver, metadataRegistry, timeZoneProvider);
    }

    @Test
    public void getUIDReturnsCorrectId() {
        assertEquals(FindItemsLLMTool.ID, tool.getUID());
    }

    @Test
    public void itemsMatchingAllTermsComeFirst() throws LLMToolException {
        add(new GroupItem("Kitchen"), "Kitchen");
        add(new GroupItem("Office"), "Office");
        add(new SwitchItem("Office_Light"), "Light", "Office");
        add(new SwitchItem("Kitchen_Fan"), "Fan", "Kitchen");
        add(new SwitchItem("Kitchen_Light"), "Light", "Kitchen");

        String result = tool.call(Map.of("query", "kitchen light"), Locale.ENGLISH);

        assertTrue(result.startsWith("- Kitchen_Light (Switch) \"Light\" in Kitchen"), result);
    }

    @Test
    public void semanticParentsAreSearchedAndShown() throws LLMToolException {
        add(new GroupItem("Garage"), "Garage");
        add(new GroupItem("Garage_Door"), "Door");
        ContactItem contact = add(new ContactItem("Garage_Door_Contact"), "Contact");
        contact.setState(OpenClosedType.OPEN);
        stubSemanticParent("Garage_Door_Contact", SemanticTags.REL_IS_POINT_OF, "Garage_Door");
        stubSemanticParent("Garage_Door", SemanticTags.REL_HAS_LOCATION, "Garage");

        String result = tool.call(Map.of("query", "garage door"), Locale.ENGLISH);

        assertTrue(result.contains("- Garage_Door_Contact (Contact) \"Contact\" in Door, Garage: OPEN"), result);
    }

    @Test
    public void itemsWithoutAccessAreNotReturned() throws LLMToolException {
        add(new SwitchItem("Alarm_Arm"), "Alarm");
        when(itemPermissionResolver.getPermission(items.getFirst())).thenReturn(ItemPermission.NO_ACCESS);

        String result = tool.call(Map.of("query", "alarm"), Locale.ENGLISH);

        assertEquals("No Items found matching 'alarm'.", result);
    }

    @Test
    public void parentsWithoutAccessAreNotShownOrSearched() throws LLMToolException {
        GroupItem room = add(new GroupItem("Secret_Room"), "Secret Room");
        add(new SwitchItem("Desk_Lamp"), "Lamp", "Secret_Room");
        when(itemPermissionResolver.getPermission(room)).thenReturn(ItemPermission.NO_ACCESS);

        String result = tool.call(Map.of("query", "lamp"), Locale.ENGLISH);

        assertTrue(result.startsWith("- Desk_Lamp (Switch) \"Lamp\":"), result);
        assertFalse(result.contains("Secret"), result);
        assertEquals("No Items found matching 'secret'.", tool.call(Map.of("query", "secret"), Locale.ENGLISH));
    }

    @Test
    public void readOnlyItemsAreMarked() throws LLMToolException {
        SwitchItem pump = add(new SwitchItem("Pool_Pump"), "Pool Pump");
        pump.setState(OnOffType.ON);
        when(itemPermissionResolver.getPermission(pump)).thenReturn(ItemPermission.READ_ONLY);

        String result = tool.call(Map.of("query", "pool"), Locale.ENGLISH);

        assertEquals("- Pool_Pump (Switch, read-only) \"Pool Pump\": ON", result);
    }

    @Test
    public void limitCapsResults() throws LLMToolException {
        for (int i = 1; i <= 4; i++) {
            add(new SwitchItem("Lamp_" + i), "Lamp " + i);
        }

        String result = tool.call(Map.of("query", "lamp", "limit", 2), Locale.ENGLISH);

        assertEquals(3, result.lines().count(), result);
        assertTrue(result.endsWith("(2 more, refine the query to narrow down)"), result);
    }

    @Test
    public void missingQueryThrows() {
        assertThrows(LLMToolException.class, () -> tool.call(Map.of(), Locale.ENGLISH));
        assertThrows(LLMToolException.class, () -> tool.call(Map.of("query", " ? "), Locale.ENGLISH));
    }

    private <T extends GenericItem> T add(T item, String label, String... groups) {
        item.setLabel(label);
        for (String group : groups) {
            item.addGroupName(group);
        }
        items.add(item);
        return item;
    }

    private void stubSemanticParent(String itemName, String relation, String parent) {
        MetadataKey key = new MetadataKey(SemanticTags.METADATA_NAMESPACE, itemName);
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "Point", Map.of(relation, parent)));
    }
}
