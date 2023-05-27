/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.semantics.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.semantics.Equipment;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.ManagedSemanticTagProvider;
import org.openhab.core.semantics.Point;
import org.openhab.core.semantics.Property;
import org.openhab.core.semantics.SemanticTagRegistry;
import org.openhab.core.semantics.Tag;
import org.openhab.core.semantics.model.DefaultSemanticTagProvider;

/**
 * @author Laurent Garnier - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class SemanticTagRegistryImplTest {

    private @Mock @NonNullByDefault({}) ManagedSemanticTagProvider managedSemanticTagProviderMock;
    private @NonNullByDefault({}) SemanticTagRegistry semanticTagRegistry;

    private @NonNullByDefault({}) Class<? extends Tag> roomTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> bathroomTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> cleaningRobotTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> measurementTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> temperatureTagClass;

    @BeforeEach
    public void setup() throws Exception {
        when(managedSemanticTagProviderMock.getAll()).thenReturn(List.of());
        semanticTagRegistry = new SemanticTagRegistryImpl(new DefaultSemanticTagProvider(),
                managedSemanticTagProviderMock);

        roomTagClass = semanticTagRegistry.getTagClassById("Location_Indoor_Room");
        bathroomTagClass = semanticTagRegistry.getTagClassById("Location_Indoor_Room_Bathroom");
        cleaningRobotTagClass = semanticTagRegistry.getTagClassById("Equipment_CleaningRobot");
        measurementTagClass = semanticTagRegistry.getTagClassById("Point_Measurement");
        temperatureTagClass = semanticTagRegistry.getTagClassById("Property_Temperature");
    }

    @Test
    public void testGetById() {
        assertEquals(Location.class, semanticTagRegistry.getTagClassById("Location"));
        assertEquals(roomTagClass, semanticTagRegistry.getTagClassById("Room"));
        assertEquals(roomTagClass, semanticTagRegistry.getTagClassById("Indoor_Room"));
        assertEquals(roomTagClass, semanticTagRegistry.getTagClassById("Location_Indoor_Room"));
        assertEquals(bathroomTagClass, semanticTagRegistry.getTagClassById("Bathroom"));
        assertEquals(bathroomTagClass, semanticTagRegistry.getTagClassById("Room_Bathroom"));
        assertEquals(bathroomTagClass, semanticTagRegistry.getTagClassById("Indoor_Room_Bathroom"));
        assertEquals(bathroomTagClass, semanticTagRegistry.getTagClassById("Location_Indoor_Room_Bathroom"));
    }

    @Test
    public void testBuildId() {
        assertEquals("Location", semanticTagRegistry.buildId(Location.class));
        assertEquals("Location_Indoor_Room", semanticTagRegistry.buildId(roomTagClass));
        assertEquals("Location_Indoor_Room_Bathroom", semanticTagRegistry.buildId(bathroomTagClass));
        assertEquals("Equipment", semanticTagRegistry.buildId(Equipment.class));
        assertEquals("Equipment_CleaningRobot", semanticTagRegistry.buildId(cleaningRobotTagClass));
        assertEquals("Point", semanticTagRegistry.buildId(Point.class));
        assertEquals("Point_Measurement", semanticTagRegistry.buildId(measurementTagClass));
        assertEquals("Property", semanticTagRegistry.buildId(Property.class));
        assertEquals("Property_Temperature", semanticTagRegistry.buildId(temperatureTagClass));
    }
}
