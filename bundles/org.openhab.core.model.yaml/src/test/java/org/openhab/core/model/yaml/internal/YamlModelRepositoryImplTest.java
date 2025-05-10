/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.test.FirstTypeDTO;
import org.openhab.core.model.yaml.test.SecondTypeDTO;
import org.openhab.core.service.WatchService;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link YamlModelRepositoryImplTest} contains tests for the {@link YamlModelRepositoryImpl} class.
 *
 * @author Jan N. Klug - Initial contribution
 * @author Laurent Garnier - Extended tests to cover version 2
 * @author Laurent Garnier - Added one test for version management
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlModelRepositoryImplTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/model");
    private static final String MODEL_NAME = "model.yaml";
    private static final Path MODEL_PATH = Path.of(MODEL_NAME);
    private static final String MODEL2_NAME = "model2.yaml";
    private static final Path MODEL2_PATH = Path.of(MODEL2_NAME);

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @TempDir @NonNullByDefault({}) Path watchPath;
    private @NonNullByDefault({}) Path fullModelPath;
    private @NonNullByDefault({}) Path fullModel2Path;

    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull FirstTypeDTO> firstTypeListener;
    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull SecondTypeDTO> secondTypeListener1;
    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull SecondTypeDTO> secondTypeListener2;

    private @Captor @NonNullByDefault({}) ArgumentCaptor<ObjectMapper> mapperCaptor;
    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<FirstTypeDTO>> firstTypeCaptor;
    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<SecondTypeDTO>> secondTypeCaptor1;
    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<SecondTypeDTO>> secondTypeCaptor2;

    @BeforeEach
    public void setup() {
        fullModelPath = watchPath.resolve(MODEL_PATH);
        fullModel2Path = watchPath.resolve(MODEL2_PATH);
        when(watchServiceMock.getWatchPath()).thenReturn(watchPath);

        when(firstTypeListener.getElementClass()).thenReturn(FirstTypeDTO.class);
        when(firstTypeListener.isVersionSupported(anyInt())).thenReturn(true);
        when(firstTypeListener.isDeprecated()).thenReturn(false);
        when(secondTypeListener1.getElementClass()).thenReturn(SecondTypeDTO.class);
        when(secondTypeListener1.isVersionSupported(anyInt())).thenReturn(true);
        when(secondTypeListener1.isDeprecated()).thenReturn(false);
        when(secondTypeListener2.getElementClass()).thenReturn(SecondTypeDTO.class);
        when(secondTypeListener2.isVersionSupported(anyInt())).thenReturn(true);
        when(secondTypeListener2.isDeprecated()).thenReturn(false);
    }

    @Test
    public void testFileAddedAfterListeners() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);
        Files.copy(SOURCE_PATH.resolve("modelV2FileAddedOrRemoved.yaml"), fullModel2Path);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL2_PATH);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener, never()).removedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), secondTypeCaptor1.capture());
        verify(secondTypeListener1).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener1, never()).removedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener2).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), secondTypeCaptor2.capture());
        verify(secondTypeListener2).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), secondTypeCaptor2.capture());
        verify(secondTypeListener2, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener2, never()).removedModel(any(), mapperCaptor.capture(), any());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(2));
        List<Collection<SecondTypeDTO>> secondTypeCaptor1Values = secondTypeCaptor1.getAllValues();
        assertThat(secondTypeCaptor1Values, hasSize(2));
        List<Collection<SecondTypeDTO>> secondTypeCaptor2Values = secondTypeCaptor2.getAllValues();
        assertThat(secondTypeCaptor2Values, hasSize(2));

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptorValues.getFirst();
        Collection<SecondTypeDTO> secondTypeElements1 = secondTypeCaptor1Values.getFirst();
        Collection<SecondTypeDTO> secondTypeElements2 = secondTypeCaptor2Values.getFirst();

        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
        assertThat(secondTypeElements1, hasSize(1));
        assertThat(secondTypeElements1, contains(new SecondTypeDTO("Second1", "Label1")));
        assertThat(secondTypeElements2, hasSize(1));
        assertThat(secondTypeElements2, contains(new SecondTypeDTO("Second1", "Label1")));

        firstTypeElements = firstTypeCaptorValues.get(1);
        secondTypeElements1 = secondTypeCaptor1Values.get(1);
        secondTypeElements2 = secondTypeCaptor2Values.get(1);

        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
        assertThat(secondTypeElements1, hasSize(1));
        assertThat(secondTypeElements1, contains(new SecondTypeDTO("Second1", "Label1")));
        assertThat(secondTypeElements2, hasSize(1));
        assertThat(secondTypeElements2, contains(new SecondTypeDTO("Second1", "Label1")));
    }

    @Test
    public void testFileAddedBeforeListeners() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);
        Files.copy(SOURCE_PATH.resolve("modelV2FileAddedOrRemoved.yaml"), fullModel2Path);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL2_PATH);

        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener, never()).removedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), secondTypeCaptor1.capture());
        verify(secondTypeListener1).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener1, never()).removedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener2).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), secondTypeCaptor2.capture());
        verify(secondTypeListener2).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), secondTypeCaptor2.capture());
        verify(secondTypeListener2, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener2, never()).removedModel(any(), mapperCaptor.capture(), any());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(2));
        List<Collection<SecondTypeDTO>> secondTypeCaptor1Values = secondTypeCaptor1.getAllValues();
        assertThat(secondTypeCaptor1Values, hasSize(2));
        List<Collection<SecondTypeDTO>> secondTypeCaptor2Values = secondTypeCaptor2.getAllValues();
        assertThat(secondTypeCaptor2Values, hasSize(2));

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptorValues.getFirst();
        Collection<SecondTypeDTO> secondTypeElements1 = secondTypeCaptor1Values.getFirst();
        Collection<SecondTypeDTO> secondTypeElements2 = secondTypeCaptor2Values.getFirst();

        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
        assertThat(secondTypeElements1, hasSize(1));
        assertThat(secondTypeElements1, contains(new SecondTypeDTO("Second1", "Label1")));
        assertThat(secondTypeElements2, hasSize(1));
        assertThat(secondTypeElements2, contains(new SecondTypeDTO("Second1", "Label1")));

        firstTypeElements = firstTypeCaptorValues.get(1);
        secondTypeElements1 = secondTypeCaptor1Values.get(1);
        secondTypeElements2 = secondTypeCaptor2Values.get(1);

        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
        assertThat(secondTypeElements1, hasSize(1));
        assertThat(secondTypeElements1, contains(new SecondTypeDTO("Second1", "Label1")));
        assertThat(secondTypeElements2, hasSize(1));
        assertThat(secondTypeElements2, contains(new SecondTypeDTO("Second1", "Label1")));
    }

    @Test
    public void testFileUpdated() throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        // File in v1 format
        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);
        verify(firstTypeListener).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), any());

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePre.yaml"), fullModelPath, StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, MODEL_PATH);
        verify(firstTypeListener, times(2)).addedModel(eq(MODEL_NAME), mapperCaptor.capture(),
                firstTypeCaptor.capture());
        verify(firstTypeListener).updatedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());

        // File in v2 format
        Files.copy(SOURCE_PATH.resolve("modelV2FileUpdatePost.yaml"), fullModel2Path);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL2_PATH);
        verify(firstTypeListener).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), any());

        Files.copy(SOURCE_PATH.resolve("modelV2FileUpdatePre.yaml"), fullModel2Path,
                StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, MODEL2_PATH);
        verify(firstTypeListener, times(2)).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(),
                firstTypeCaptor.capture());
        verify(firstTypeListener).updatedModel(eq(MODEL2_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener).removedModel(eq(MODEL2_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(8));

        // added originally - first file
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(3));
        assertThat(firstTypeCaptorValues.getFirst(), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // added by update - first file
        assertThat(firstTypeCaptorValues.get(1), hasSize(1));
        assertThat(firstTypeCaptorValues.get(1), contains(new FirstTypeDTO("Fourth", "Fourth original")));
        // updated by update - first file
        assertThat(firstTypeCaptorValues.get(2), hasSize(1));
        assertThat(firstTypeCaptorValues.get(2), contains(new FirstTypeDTO("Second", "Second modified")));
        // removed by update - first file
        assertThat(firstTypeCaptorValues.get(3), hasSize(1));
        assertThat(firstTypeCaptorValues.get(3), contains(new FirstTypeDTO("Third", "Third original")));

        // added originally -second file
        assertThat(firstTypeCaptorValues.get(4), hasSize(3));
        assertThat(firstTypeCaptorValues.get(4), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // added by update -second file
        assertThat(firstTypeCaptorValues.get(5), hasSize(1));
        assertThat(firstTypeCaptorValues.get(5), contains(new FirstTypeDTO("Fourth", "Fourth original")));
        // updated by update -second file
        assertThat(firstTypeCaptorValues.get(6), hasSize(1));
        assertThat(firstTypeCaptorValues.get(6), contains(new FirstTypeDTO("Second", "Second modified")));
        // removed by update -second file
        assertThat(firstTypeCaptorValues.get(7), hasSize(1));
        assertThat(firstTypeCaptorValues.get(7), contains(new FirstTypeDTO("Third", "Third original")));
    }

    @ParameterizedTest
    @CsvSource({ //
            "modelFileUpdateRemovedElements.yaml,modelV2FileUpdateRemovedElements.yaml",
            "modelFileUpdateRenamedElements.yaml,modelV2FileUpdateRenamedElements.yaml",
            "modelFileUpdateRemovedVersion.yaml,modelV2FileUpdateRemovedVersion.yaml" //
    })
    public void testFileRemovedElements(String v1File, String v2File) throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        // File in v1 format
        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);
        verify(firstTypeListener).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), any());

        Files.copy(SOURCE_PATH.resolve(v1File), fullModelPath, StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, MODEL_PATH);
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptor.getAllValues().getFirst();

        // Check that the elements were removed
        assertThat(firstTypeElements, hasSize(3));
        assertThat(firstTypeElements, containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));

        // File in v2 format
        Files.copy(SOURCE_PATH.resolve("modelV2FileUpdatePost.yaml"), fullModel2Path);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL2_PATH);
        verify(firstTypeListener).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), any());

        Files.copy(SOURCE_PATH.resolve(v2File), fullModel2Path, StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, MODEL2_PATH);
        verify(firstTypeListener).removedModel(eq(MODEL2_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());

        assertThat(firstTypeCaptor.getAllValues(), hasSize(2));
        firstTypeElements = firstTypeCaptor.getAllValues().getLast();

        // Check that the elements were removed
        assertThat(firstTypeElements, hasSize(3));
        assertThat(firstTypeElements, containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
    }

    @Test
    public void testFileRemoved() throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        modelRepository.addYamlModelListener(firstTypeListener);

        // File in v1 format
        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);
        modelRepository.processWatchEvent(WatchService.Kind.DELETE, MODEL_PATH);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());

        // File in v2 format
        Files.copy(SOURCE_PATH.resolve("modelV2FileUpdatePost.yaml"), fullModel2Path);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL2_PATH);
        modelRepository.processWatchEvent(WatchService.Kind.DELETE, MODEL2_PATH);

        verify(firstTypeListener).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener).removedModel(eq(MODEL2_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(4));

        // all are added - first file
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(3));
        assertThat(firstTypeCaptorValues.getFirst(), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // all are removed - first file
        assertThat(firstTypeCaptorValues.get(1), hasSize(3));
        assertThat(firstTypeCaptorValues.get(1), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));

        // all are added - second file
        assertThat(firstTypeCaptorValues.get(2), hasSize(3));
        assertThat(firstTypeCaptorValues.get(2), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // all are removed - second file
        assertThat(firstTypeCaptorValues.get(3), hasSize(3));
        assertThat(firstTypeCaptorValues.get(3), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
    }

    @Test
    public void testAddElementToModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        FirstTypeDTO added = new FirstTypeDTO("element3", "description3");
        modelRepository.addElementToModel(MODEL_NAME, added);
        SecondTypeDTO added2 = new SecondTypeDTO("elt1", "My label");
        modelRepository.addElementToModel(MODEL_NAME, added2);

        verify(firstTypeListener, times(2)).addedModel(eq(MODEL_NAME), mapperCaptor.capture(),
                firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener, never()).removedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener1, never()).removedModel(any(), mapperCaptor.capture(), any());

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("addToModelExpectedContent.yaml"));
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        Files.copy(SOURCE_PATH.resolve("modifyModelV2InitialContent.yaml"), fullModel2Path);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL2_PATH);
        modelRepository.addElementToModel(MODEL2_NAME, added);
        modelRepository.addElementToModel(MODEL2_NAME, added2);
        verify(firstTypeListener, times(2)).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(),
                firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener, never()).removedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener1, never()).removedModel(any(), mapperCaptor.capture(), any());

        actualFileContent = Files.readString(fullModel2Path);
        expectedFileContent = Files.readString(SOURCE_PATH.resolve("addToModelV2ExpectedContent.yaml"));

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(4));
        assertThat(firstTypeCaptorValues.get(1), hasSize(1));
        assertThat(firstTypeCaptorValues.get(1), contains(new FirstTypeDTO("element3", "description3")));
        assertThat(firstTypeCaptorValues.get(3), hasSize(1));
        assertThat(firstTypeCaptorValues.get(3), contains(new FirstTypeDTO("element3", "description3")));

        List<Collection<SecondTypeDTO>> secondTypeCaptor1Values = secondTypeCaptor1.getAllValues();
        assertThat(secondTypeCaptor1Values, hasSize(2));
        assertThat(secondTypeCaptor1Values.getFirst(), hasSize(1));
        assertThat(secondTypeCaptor1Values.getFirst(), contains(new SecondTypeDTO("elt1", "My label")));
        assertThat(secondTypeCaptor1Values.get(1), hasSize(1));
        assertThat(secondTypeCaptor1Values.get(1), contains(new SecondTypeDTO("elt1", "My label")));
    }

    @Test
    public void testUpdateElementInModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        FirstTypeDTO updated = new FirstTypeDTO("element1", "newDescription1");
        modelRepository.updateElementInModel(MODEL_NAME, updated);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), any());
        verify(firstTypeListener).updatedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).removedModel(any(), mapperCaptor.capture(), any());

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("updateInModelExpectedContent.yaml"));
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        Files.copy(SOURCE_PATH.resolve("modifyModelV2InitialContent.yaml"), fullModel2Path);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL2_PATH);
        modelRepository.updateElementInModel(MODEL2_NAME, updated);
        verify(firstTypeListener).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), any());
        verify(firstTypeListener).updatedModel(eq(MODEL2_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).removedModel(any(), mapperCaptor.capture(), any());

        actualFileContent = Files.readString(fullModel2Path);
        expectedFileContent = Files.readString(SOURCE_PATH.resolve("updateInModelV2ExpectedContent.yaml"));

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(2));
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(1));
        assertThat(firstTypeCaptorValues.getFirst(), contains(new FirstTypeDTO("element1", "newDescription1")));
        assertThat(firstTypeCaptorValues.get(1), hasSize(1));
        assertThat(firstTypeCaptorValues.get(1), contains(new FirstTypeDTO("element1", "newDescription1")));
    }

    @Test
    public void testRemoveElementFromModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        FirstTypeDTO removed = new FirstTypeDTO("element1", "description1");
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.removeElementFromModel(MODEL_NAME, removed);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), any());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("removeFromModelExpectedContent.yaml"));
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        Files.copy(SOURCE_PATH.resolve("modifyModelV2InitialContent.yaml"), fullModel2Path);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL2_PATH);
        modelRepository.removeElementFromModel(MODEL2_NAME, removed);
        verify(firstTypeListener).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), any());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener).removedModel(eq(MODEL2_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());

        actualFileContent = Files.readString(fullModel2Path);
        expectedFileContent = Files.readString(SOURCE_PATH.resolve("removeFromModelV2ExpectedContent.yaml"));

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(2));
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(1));
        assertThat(firstTypeCaptorValues.getFirst(), contains(new FirstTypeDTO("element1", "description1")));
        assertThat(firstTypeCaptorValues.get(1), hasSize(1));
        assertThat(firstTypeCaptorValues.get(1), contains(new FirstTypeDTO("element1", "description1")));
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
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        Files.copy(SOURCE_PATH.resolve("modelV2FileAddedOrRemoved.yaml"), fullModel2Path);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL2_PATH);
        modelRepository.addElementToModel(MODEL2_NAME, added);
        modelRepository.removeElementFromModel(MODEL2_NAME, removed);
        modelRepository.updateElementInModel(MODEL2_NAME, updated);

        actualFileContent = Files.readString(fullModel2Path);
        expectedFileContent = Files.readString(SOURCE_PATH.resolve("modelV2FileAddedOrRemoved.yaml"));

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));
    }

    @Test
    public void testExistingProviderForVersion() throws IOException {
        // Provider supporting only version 1
        when(firstTypeListener.isVersionSupported(eq(1))).thenReturn(true);
        when(firstTypeListener.isVersionSupported(eq(2))).thenReturn(false);
        when(firstTypeListener.isDeprecated()).thenReturn(false);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener, never()).removedModel(any(), mapperCaptor.capture(), any());

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptor.getValue();
        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
    }

    @Test
    public void testExistingDeprecatedProviderForVersion() throws IOException {
        // Provider supporting only version 1 and deprecated
        when(firstTypeListener.isVersionSupported(eq(1))).thenReturn(true);
        when(firstTypeListener.isVersionSupported(eq(2))).thenReturn(false);
        when(firstTypeListener.isDeprecated()).thenReturn(true);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener, never()).removedModel(any(), mapperCaptor.capture(), any());

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptor.getValue();
        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
    }

    @Test
    public void testNoProviderForVersion() throws IOException {
        // Provider supporting only version 2
        when(firstTypeListener.isVersionSupported(eq(1))).thenReturn(false);
        when(firstTypeListener.isVersionSupported(eq(2))).thenReturn(true);
        when(firstTypeListener.isDeprecated()).thenReturn(false);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);

        verify(firstTypeListener, never()).addedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(firstTypeListener, never()).removedModel(any(), mapperCaptor.capture(), any());
    }

    @Test
    public void testDifferentProvidersDependingOnVersion() throws IOException {
        // secondTypeListener1 supports version 1 only
        when(secondTypeListener1.isVersionSupported(eq(1))).thenReturn(true);
        when(secondTypeListener1.isVersionSupported(eq(2))).thenReturn(false);
        when(secondTypeListener1.isDeprecated()).thenReturn(false);
        // secondTypeListener2 supports version 2 only
        when(secondTypeListener2.isVersionSupported(eq(1))).thenReturn(false);
        when(secondTypeListener2.isVersionSupported(eq(2))).thenReturn(true);
        when(secondTypeListener2.isDeprecated()).thenReturn(false);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);
        Files.copy(SOURCE_PATH.resolve("modelV2FileAddedOrRemoved.yaml"), fullModel2Path);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL_PATH);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, MODEL2_PATH);

        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), any());
        verify(secondTypeListener1, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener1, never()).removedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener2).addedModel(eq(MODEL2_NAME), mapperCaptor.capture(), secondTypeCaptor2.capture());
        verify(secondTypeListener2, never()).addedModel(eq(MODEL_NAME), mapperCaptor.capture(), any());
        verify(secondTypeListener2, never()).updatedModel(any(), mapperCaptor.capture(), any());
        verify(secondTypeListener2, never()).removedModel(any(), mapperCaptor.capture(), any());

        Collection<SecondTypeDTO> secondTypeElements = secondTypeCaptor1.getValue();
        assertThat(secondTypeElements, hasSize(1));
        assertThat(secondTypeElements, contains(new SecondTypeDTO("Second1", "Label1")));

        secondTypeElements = secondTypeCaptor2.getValue();
        assertThat(secondTypeElements, hasSize(1));
        assertThat(secondTypeElements, contains(new SecondTypeDTO("Second1", "Label1")));
    }
}
