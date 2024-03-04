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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.test.FirstTypeDTO;
import org.openhab.core.model.yaml.test.SecondTypeDTO;
import org.openhab.core.service.WatchService;

/**
 * The {@link YamlModelRepositoryImplTest} contains tests for the {@link YamlModelRepositoryImpl} class.
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlModelRepositoryImplTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources");
    private static final String MODEL_NAME = "model";
    private static final Path MODEL_PATH = Path.of(MODEL_NAME + ".yaml");

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @TempDir @NonNullByDefault({}) Path watchPath;
    private @NonNullByDefault({}) Path fullModelPath;

    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull FirstTypeDTO> firstTypeListener;
    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull SecondTypeDTO> secondTypeListener1;
    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull SecondTypeDTO> secondTypeListener2;

    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<FirstTypeDTO>> firstTypeCaptor;
    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<SecondTypeDTO>> secondTypeCaptor1;
    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<SecondTypeDTO>> secondTypeCaptor2;

    @BeforeEach
    public void setup() {
        fullModelPath = watchPath.resolve(MODEL_PATH);
        when(watchServiceMock.getWatchPath()).thenReturn(watchPath);

        when(firstTypeListener.getElementClass()).thenReturn(FirstTypeDTO.class);
        when(secondTypeListener1.getElementClass()).thenReturn(SecondTypeDTO.class);
        when(secondTypeListener2.getElementClass()).thenReturn(SecondTypeDTO.class);
    }

    @Test
    public void testFileAddedAfterListeners() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());
        verify(secondTypeListener2).addedModel(eq(MODEL_NAME), secondTypeCaptor2.capture());
        verify(secondTypeListener2, never()).updatedModel(any(), any());
        verify(secondTypeListener2, never()).removedModel(any(), any());

        Collection<? extends YamlElement> firstTypeElements = firstTypeCaptor.getValue();
        Collection<? extends YamlElement> secondTypeElements1 = secondTypeCaptor1.getValue();
        Collection<? extends YamlElement> secondTypeElements2 = secondTypeCaptor2.getValue();

        assertThat(firstTypeElements, hasSize(2));
        assertThat(secondTypeElements1, hasSize(1));
        assertThat(secondTypeElements2, hasSize(1));
    }

    @Test
    public void testFileAddedBeforeListeners() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());
        verify(secondTypeListener2).addedModel(eq(MODEL_NAME), secondTypeCaptor2.capture());
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
    public void testFileUpdated() throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);
        verify(firstTypeListener).addedModel(eq(MODEL_NAME), any());

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePre.yaml"), fullModelPath, StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, MODEL_PATH);
        verify(firstTypeListener, times(2)).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener).updatedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), firstTypeCaptor.capture());

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
    public void testFileRemoved() throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        modelRepository.addYamlModelListener(firstTypeListener);

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);
        modelRepository.processWatchEvent(WatchService.Kind.DELETE, MODEL_PATH);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), firstTypeCaptor.capture());

        List<Collection<FirstTypeDTO>> arguments = firstTypeCaptor.getAllValues();
        assertThat(arguments, hasSize(2));

        // all are added
        assertThat(arguments.get(0), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // all are removed
        assertThat(arguments.get(0), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
    }

    @Test
    public void testAddElementToModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        FirstTypeDTO added = new FirstTypeDTO("element3", "description3");
        modelRepository.addElementToModel(MODEL_NAME, added);

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("addToModelExpectedContent.yaml"));

        assertThat(actualFileContent, is(expectedFileContent.replaceAll("\r\n", "\n")));
    }

    @Test
    public void testUpdateElementInModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        FirstTypeDTO updated = new FirstTypeDTO("element1", "newDescription1");
        modelRepository.updateElementInModel(MODEL_NAME, updated);

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("updateInModelExpectedContent.yaml"));

        assertThat(actualFileContent, is(expectedFileContent.replaceAll("\r\n", "\n")));
    }

    @Test
    public void testRemoveElementFromModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        FirstTypeDTO removed = new FirstTypeDTO("element1", "description1");
        modelRepository.removeElementFromModel(MODEL_NAME, removed);

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("removeFromModelExpectedContent.yaml"));

        assertThat(actualFileContent, is(expectedFileContent.replaceAll("\r\n", "\n")));
    }

    @Test
    public void testReadOnlyModelNotUpdated() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        FirstTypeDTO added = new FirstTypeDTO("element3", "description3");
        modelRepository.addElementToModel(MODEL_NAME, added);

        FirstTypeDTO removed = new FirstTypeDTO("element1", "description1");
        modelRepository.removeElementFromModel(MODEL_NAME, removed);

        FirstTypeDTO updated = new FirstTypeDTO("element2", "newDescription2");
        modelRepository.updateElementInModel(MODEL_NAME, updated);

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"));

        assertThat(actualFileContent, is(expectedFileContent));
    }
}
