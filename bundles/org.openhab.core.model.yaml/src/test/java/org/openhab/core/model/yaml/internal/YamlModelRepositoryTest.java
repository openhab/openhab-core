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
package org.openhab.core.model.yaml.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.model.yaml.YamlDTO;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.test.FirstTypeDTO;
import org.openhab.core.model.yaml.test.SecondTypeDTO;
import org.openhab.core.service.WatchService;

/**
 * The {@link YamlModelRepositoryTest} contains tests for the {@link YamlModelRepository} class.
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlModelRepositoryTest {
    private static final Path BASE_PATH = Path.of("src/test/resources");

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull FirstTypeDTO> firstTypeListener;
    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull SecondTypeDTO> secondTypeListener1;
    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull SecondTypeDTO> secondTypeListener2;

    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<FirstTypeDTO>> firstTypeCaptor;
    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<SecondTypeDTO>> secondTypeCaptor1;
    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<SecondTypeDTO>> secondTypeCaptor2;

    @BeforeEach
    public void setup() {
        when(firstTypeListener.getTypeClass()).thenReturn(FirstTypeDTO.class);
        when(firstTypeListener.getTypeName()).thenReturn("firstType");
        when(secondTypeListener1.getTypeClass()).thenReturn(SecondTypeDTO.class);
        when(secondTypeListener1.getTypeName()).thenReturn("secondType");
        when(secondTypeListener2.getTypeClass()).thenReturn(SecondTypeDTO.class);
        when(secondTypeListener2.getTypeName()).thenReturn("secondType");
    }

    @Test
    public void testFileAddedAfterListeners() {
        YamlModelRepository modelRepository = new YamlModelRepository(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        String modelName = "modelName";

        File validInputFile = BASE_PATH.resolve("added.yaml").toFile();
        modelRepository.process(modelName, WatchService.Kind.CREATE, validInputFile);

        verify(firstTypeListener).addedModel(eq(modelName), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
        verify(secondTypeListener1).addedModel(eq(modelName), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());
        verify(secondTypeListener2).addedModel(eq(modelName), secondTypeCaptor2.capture());
        verify(secondTypeListener2, never()).updatedModel(any(), any());
        verify(secondTypeListener2, never()).removedModel(any(), any());

        Collection<? extends YamlDTO> firstTypeElements = firstTypeCaptor.getValue();
        Collection<? extends YamlDTO> secondTypeElements1 = secondTypeCaptor1.getValue();
        Collection<? extends YamlDTO> secondTypeElements2 = secondTypeCaptor2.getValue();

        assertThat(firstTypeElements, hasSize(2));
        assertThat(secondTypeElements1, hasSize(1));
        assertThat(secondTypeElements2, hasSize(1));
    }

    @Test
    public void testFileAddedBeforeListeners() {
        YamlModelRepository modelRepository = new YamlModelRepository(watchServiceMock);
        String modelName = "modelName";

        File addedFile = BASE_PATH.resolve("added.yaml").toFile();
        modelRepository.process(modelName, WatchService.Kind.CREATE, addedFile);

        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        verify(firstTypeListener).addedModel(eq(modelName), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
        verify(secondTypeListener1).addedModel(eq(modelName), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());
        verify(secondTypeListener2).addedModel(eq(modelName), secondTypeCaptor2.capture());
        verify(secondTypeListener2, never()).updatedModel(any(), any());
        verify(secondTypeListener2, never()).removedModel(any(), any());

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptor.getValue();
        Collection<SecondTypeDTO> secondTypeElements1 = secondTypeCaptor1.getValue();
        Collection<SecondTypeDTO> secondTypeElements2 = secondTypeCaptor2.getValue();

        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
        assertThat(secondTypeElements1, contains(new SecondTypeDTO("Second1", "Label1")));
        assertThat(secondTypeElements1, contains(new SecondTypeDTO("Second1", "Label1")));
    }

    @Test
    public void testFileUpdated() {
        YamlModelRepository modelRepository = new YamlModelRepository(watchServiceMock);
        String modelName = "modelName";
        modelRepository.addYamlModelListener(firstTypeListener);

        File preInputFile = BASE_PATH.resolve("pre-update.yaml").toFile();
        File postInputFile = BASE_PATH.resolve("post-update.yaml").toFile();
        modelRepository.process(modelName, WatchService.Kind.CREATE, preInputFile);
        verify(firstTypeListener).addedModel(eq(modelName), any());

        modelRepository.process(modelName, WatchService.Kind.MODIFY, postInputFile);
        verify(firstTypeListener, times(2)).addedModel(eq(modelName), firstTypeCaptor.capture());
        verify(firstTypeListener).updatedModel(eq(modelName), firstTypeCaptor.capture());
        verify(firstTypeListener).removedModel(eq(modelName), firstTypeCaptor.capture());

        List<Collection<FirstTypeDTO>> arguments = firstTypeCaptor.getAllValues();
        assertThat(arguments, hasSize(4));

        // added originally
        assertThat(arguments.get(0), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // added by update
        assertThat(arguments.get(1), contains(new FirstTypeDTO("Fourth", "Fourth original")));
        // updated by update
        assertThat(arguments.get(2), contains(new FirstTypeDTO("Second", "Second modified")));
        // removed by update
        assertThat(arguments.get(3), contains(new FirstTypeDTO("Third", "Third original")));
    }

    @Test
    public void testFileRemoved() {
        YamlModelRepository modelRepository = new YamlModelRepository(watchServiceMock);
        String modelName = "modelName";

        modelRepository.addYamlModelListener(firstTypeListener);

        File removeInputFile = BASE_PATH.resolve("pre-update.yaml").toFile();
        modelRepository.process(modelName, WatchService.Kind.CREATE, removeInputFile);
        modelRepository.process(modelName, WatchService.Kind.DELETE, removeInputFile);

        verify(firstTypeListener).addedModel(eq(modelName), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener).removedModel(eq(modelName), firstTypeCaptor.capture());

        List<Collection<FirstTypeDTO>> arguments = firstTypeCaptor.getAllValues();
        assertThat(arguments, hasSize(2));

        // all are added
        assertThat(arguments.get(0), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // all are removed
        assertThat(arguments.get(0), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
    }
}
