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
package org.openhab.core.thing.binding;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.internal.types.StateDescriptionFragmentImpl;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.types.CommandDescriptionBuilder;
import org.openhab.core.types.StateDescriptionFragmentBuilder;

/**
 * The {@link AbstractStorageBasedTypeProviderTest} contains tests for the static mapping-methods
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class AbstractStorageBasedTypeProviderTest {

    @Test
    public void testStateChannelTypeProperlyMappedToEntityAndBack() {
        ChannelTypeUID channelTypeUID = new ChannelTypeUID("TestBinding:testQuantityChannelType");

        ChannelType expected = ChannelTypeBuilder.state(channelTypeUID, "testLabel", "Number:Length")
                .withDescription("testDescription").withCategory("testCategory")
                .withConfigDescriptionURI(URI.create("testBinding:testConfig"))
                .withAutoUpdatePolicy(AutoUpdatePolicy.VETO).isAdvanced(true).withTag("testTag")
                .withCommandDescription(CommandDescriptionBuilder.create().build())
                .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().build()).withUnitHint("km")
                .build();
        AbstractStorageBasedTypeProvider.ChannelTypeEntity entity = AbstractStorageBasedTypeProvider
                .mapToEntity(expected);
        ChannelType actual = AbstractStorageBasedTypeProvider.mapFromEntity(entity);

        assertThat(actual.getUID(), is(expected.getUID()));
        assertThat(actual.getKind(), is(expected.getKind()));
        assertThat(actual.getLabel(), is(expected.getLabel()));
        assertThat(actual.getDescription(), is(expected.getDescription()));
        assertThat(actual.getConfigDescriptionURI(), is(expected.getConfigDescriptionURI()));
        assertThat(actual.isAdvanced(), is(expected.isAdvanced()));
        assertThat(actual.getAutoUpdatePolicy(), is(expected.getAutoUpdatePolicy()));
        assertThat(actual.getCategory(), is(expected.getCategory()));
        assertThat(actual.getEvent(), is(expected.getEvent()));
        assertThat(actual.getCommandDescription(), is(expected.getCommandDescription()));
        assertThat(actual.getState(), is(expected.getState()));
        assertThat(actual.getItemType(), is(expected.getItemType()));
        assertThat(actual.getTags(), hasItems(expected.getTags().toArray(String[]::new)));
        assertThat(actual.getUnitHint(), is(expected.getUnitHint()));
    }

    @Test
    public void testChannelGroupTypeProperlyMappedToEntityAndBack() {
        ChannelGroupTypeUID groupTypeUID = new ChannelGroupTypeUID("testBinding:testGroupType");

        ChannelDefinition channelDefinition = new ChannelDefinitionBuilder("channelName",
                new ChannelTypeUID("system:color")).withLabel("label").withDescription("description")
                .withProperties(Map.of("key", "value")).withAutoUpdatePolicy(AutoUpdatePolicy.VETO).build();
        ChannelGroupType expected = ChannelGroupTypeBuilder.instance(groupTypeUID, "testLabel")
                .withDescription("testDescription").withCategory("testCategory")
                .withChannelDefinitions(List.of(channelDefinition)).build();

        AbstractStorageBasedTypeProvider.ChannelGroupTypeEntity entity = AbstractStorageBasedTypeProvider
                .mapToEntity(expected);
        ChannelGroupType actual = AbstractStorageBasedTypeProvider.mapFromEntity(entity);

        assertThat(actual.getUID(), is(expected.getUID()));
        assertThat(actual.getLabel(), is(expected.getLabel()));
        assertThat(actual.getDescription(), is(expected.getDescription()));
        assertThat(actual.getCategory(), is(expected.getCategory()));
        List<ChannelDefinition> expectedChannelDefinitions = expected.getChannelDefinitions();
        List<ChannelDefinition> actualChannelDefinitions = actual.getChannelDefinitions();
        assertThat(actualChannelDefinitions.size(), is(expectedChannelDefinitions.size()));
        for (ChannelDefinition expectedChannelDefinition : expectedChannelDefinitions) {
            ChannelDefinition actualChannelDefinition = Objects.requireNonNull(actualChannelDefinitions.stream()
                    .filter(d -> d.getId().equals(expectedChannelDefinition.getId())).findFirst().orElse(null));
            assertChannelDefinition(actualChannelDefinition, expectedChannelDefinition);
        }
    }

    @Test
    public void testThingTypeProperlyMappedToEntityAndBack() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("testBinding:testThingType");

        ChannelDefinition channelDefinition = new ChannelDefinitionBuilder("channelName",
                new ChannelTypeUID("system:color")).withLabel("label").withDescription("description")
                .withProperties(Map.of("key", "value")).withAutoUpdatePolicy(AutoUpdatePolicy.VETO).build();
        ChannelGroupDefinition channelGroupDefinition = new ChannelGroupDefinition("groupName",
                new ChannelGroupTypeUID("testBinding:channelGroupType"), "label", "description");
        ThingType expected = ThingTypeBuilder.instance(thingTypeUID, "testLabel").withDescription("description")
                .withCategory("category").withExtensibleChannelTypeIds(List.of("ch1", "ch2"))
                .withConfigDescriptionURI(URI.create("testBinding:testConfig"))
                .withChannelDefinitions(List.of(channelDefinition))
                .withChannelGroupDefinitions(List.of(channelGroupDefinition)).isListed(true)
                .withProperties(Map.of("key", "value")).withSupportedBridgeTypeUIDs(List.of("bridge1", "bridge2"))
                .build();

        AbstractStorageBasedTypeProvider.ThingTypeEntity entity = AbstractStorageBasedTypeProvider
                .mapToEntity(expected);
        ThingType actual = AbstractStorageBasedTypeProvider.mapFromEntity(entity);

        assertThat(actual.getUID(), is(expected.getUID()));
        assertThat(actual.getLabel(), is(expected.getLabel()));
        assertThat(actual.getDescription(), is(expected.getDescription()));
        assertThat(actual.getExtensibleChannelTypeIds(),
                containsInAnyOrder(expected.getExtensibleChannelTypeIds().toArray(String[]::new)));
        assertThat(actual.getSupportedBridgeTypeUIDs(),
                containsInAnyOrder(expected.getSupportedBridgeTypeUIDs().toArray(String[]::new)));
        assertThat(actual.getCategory(), is(expected.getCategory()));
        assertThat(actual.isListed(), is(expected.isListed()));
        assertThat(actual.getRepresentationProperty(), is(expected.getRepresentationProperty()));
        assertThat(actual.getConfigDescriptionURI(), is(expected.getConfigDescriptionURI()));
        assertMap(actual.getProperties(), expected.getProperties());
        List<ChannelDefinition> expectedChannelDefinitions = expected.getChannelDefinitions();
        List<ChannelDefinition> actualChannelDefinitions = actual.getChannelDefinitions();
        assertThat(actualChannelDefinitions.size(), is(expectedChannelDefinitions.size()));
        for (ChannelDefinition expectedChannelDefinition : expectedChannelDefinitions) {
            ChannelDefinition actualChannelDefinition = Objects.requireNonNull(actualChannelDefinitions.stream()
                    .filter(d -> d.getId().equals(expectedChannelDefinition.getId())).findFirst().orElse(null));
            assertChannelDefinition(actualChannelDefinition, expectedChannelDefinition);
        }
        List<ChannelGroupDefinition> expectedChannelGroupDefinitions = expected.getChannelGroupDefinitions();
        List<ChannelGroupDefinition> actualChannelGroupDefinitions = actual.getChannelGroupDefinitions();
        assertThat(actualChannelGroupDefinitions.size(), is(expectedChannelGroupDefinitions.size()));
        for (ChannelGroupDefinition expectedChannelGroupDefinition : expectedChannelGroupDefinitions) {
            ChannelGroupDefinition actualChannelGroupDefinition = Objects.requireNonNull(actualChannelGroupDefinitions
                    .stream().filter(d -> d.getId().equals(expectedChannelGroupDefinition.getId())).findFirst()
                    .orElse(null));
            assertChannelGroupDefinition(actualChannelGroupDefinition, expectedChannelGroupDefinition);
        }
    }

    @MethodSource
    @ParameterizedTest
    public void testMapToEntityIsComplete(Class<?> originalType, Class<?> mappedType, int allowedDelta) {
        Class<?> clazz = originalType;
        int originalTypeFieldCount = 0;
        while (clazz != Object.class) {
            originalTypeFieldCount += clazz.getDeclaredFields().length;
            clazz = clazz.getSuperclass();
        }

        int mappedEntityFieldCount = mappedType.getDeclaredFields().length;

        assertThat(originalType.getName() + " not properly mapped", mappedEntityFieldCount,
                is(originalTypeFieldCount + allowedDelta));
    }

    private static Stream<Arguments> testMapToEntityIsComplete() {
        return Stream.of( //
                // isBridge is an extra field for storage and not present in ThingType
                Arguments.of(ThingType.class, AbstractStorageBasedTypeProvider.ThingTypeEntity.class, 1),
                Arguments.of(ChannelType.class, AbstractStorageBasedTypeProvider.ChannelTypeEntity.class, 0),
                Arguments.of(ChannelDefinition.class, AbstractStorageBasedTypeProvider.ChannelDefinitionEntity.class,
                        0),
                // configDescriptionURI is not available for ChannelGroupType
                Arguments.of(ChannelGroupType.class, AbstractStorageBasedTypeProvider.ChannelGroupTypeEntity.class, -1),
                Arguments.of(ChannelGroupDefinition.class,
                        AbstractStorageBasedTypeProvider.ChannelGroupDefinitionEntity.class, 0),
                Arguments.of(StateDescriptionFragmentImpl.class,
                        AbstractStorageBasedTypeProvider.StateDescriptionFragmentEntity.class, 0));
    }

    private void assertChannelDefinition(ChannelDefinition actual, ChannelDefinition expected) {
        assertThat(actual.getId(), is(expected.getId()));
        assertThat(actual.getChannelTypeUID(), is(expected.getChannelTypeUID()));
        assertThat(actual.getLabel(), is(expected.getLabel()));
        assertThat(actual.getDescription(), is(expected.getDescription()));
        assertThat(actual.getAutoUpdatePolicy(), is(expected.getAutoUpdatePolicy()));
        assertMap(actual.getProperties(), expected.getProperties());
    }

    private void assertChannelGroupDefinition(ChannelGroupDefinition actual, ChannelGroupDefinition expected) {
        assertThat(actual.getId(), is(expected.getId()));
        assertThat(actual.getTypeUID(), is(expected.getTypeUID()));
        assertThat(actual.getLabel(), is(expected.getLabel()));
        assertThat(actual.getDescription(), is(expected.getDescription()));
    }

    private void assertMap(Map<String, String> actual, Map<String, String> expected) {
        assertThat(actual.size(), is(expected.size()));
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            assertThat(actual, hasEntry(entry.getKey(), entry.getValue()));
        }
    }
}
