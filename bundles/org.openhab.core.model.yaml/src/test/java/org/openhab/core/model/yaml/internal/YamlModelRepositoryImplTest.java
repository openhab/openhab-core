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
package org.openhab.core.model.yaml.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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
import org.openhab.core.model.yaml.internal.items.YamlItemDTO;
import org.openhab.core.model.yaml.test.FirstTypeDTO;
import org.openhab.core.model.yaml.test.SecondTypeDTO;
import org.openhab.core.service.WatchService;
import org.yaml.snakeyaml.Yaml;

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

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());
        verify(secondTypeListener2).addedModel(eq(MODEL_NAME), secondTypeCaptor2.capture());
        verify(secondTypeListener2, never()).updatedModel(any(), any());
        verify(secondTypeListener2, never()).removedModel(any(), any());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(1));
        List<Collection<SecondTypeDTO>> secondTypeCaptor1Values = secondTypeCaptor1.getAllValues();
        assertThat(secondTypeCaptor1Values, hasSize(1));
        List<Collection<SecondTypeDTO>> secondTypeCaptor2Values = secondTypeCaptor2.getAllValues();
        assertThat(secondTypeCaptor2Values, hasSize(1));

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
    }

    @Test
    public void testFileAddedBeforeListeners() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

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

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(1));
        List<Collection<SecondTypeDTO>> secondTypeCaptor1Values = secondTypeCaptor1.getAllValues();
        assertThat(secondTypeCaptor1Values, hasSize(1));
        List<Collection<SecondTypeDTO>> secondTypeCaptor2Values = secondTypeCaptor2.getAllValues();
        assertThat(secondTypeCaptor2Values, hasSize(1));

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
    }

    @Test
    public void testFileUpdated() throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);
        verify(firstTypeListener).addedModel(eq(MODEL_NAME), any());

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePre.yaml"), fullModelPath, StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, fullModelPath);
        verify(firstTypeListener, times(2)).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener).updatedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), firstTypeCaptor.capture());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(4));

        // added originally
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(3));
        assertThat(firstTypeCaptorValues.getFirst(), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // added by update
        assertThat(firstTypeCaptorValues.get(1), hasSize(1));
        assertThat(firstTypeCaptorValues.get(1), contains(new FirstTypeDTO("Fourth", "Fourth original")));
        // updated by update
        assertThat(firstTypeCaptorValues.get(2), hasSize(1));
        assertThat(firstTypeCaptorValues.get(2), contains(new FirstTypeDTO("Second", "Second modified")));
        // removed by update
        assertThat(firstTypeCaptorValues.get(3), hasSize(1));
        assertThat(firstTypeCaptorValues.get(3), contains(new FirstTypeDTO("Third", "Third original")));
    }

    @ParameterizedTest
    @CsvSource({ //
            "modelFileUpdateRemovedElements.yaml", "modelFileUpdateRenamedElements.yaml",
            "modelFileUpdateRemovedVersion.yaml" //
    })
    public void testFileRemovedElements(String file) throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);
        verify(firstTypeListener).addedModel(eq(MODEL_NAME), any());

        Files.copy(SOURCE_PATH.resolve(file), fullModelPath, StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, fullModelPath);
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), firstTypeCaptor.capture());

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptor.getAllValues().getFirst();

        // Check that the elements were removed
        assertThat(firstTypeElements, hasSize(3));
        assertThat(firstTypeElements, containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
    }

    @Test
    public void testFileRemoved() throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        modelRepository.addYamlModelListener(firstTypeListener);

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.DELETE, fullModelPath);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), firstTypeCaptor.capture());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(2));

        // all are added
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(3));
        assertThat(firstTypeCaptorValues.getFirst(), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // all are removed
        assertThat(firstTypeCaptorValues.get(1), hasSize(3));
        assertThat(firstTypeCaptorValues.get(1), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
    }

    @Test
    public void testAddElementToModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        FirstTypeDTO added = new FirstTypeDTO("element3", "description3");
        modelRepository.addElementToModel(MODEL_NAME, added);
        SecondTypeDTO added2 = new SecondTypeDTO("elt1", "My label");
        modelRepository.addElementToModel(MODEL_NAME, added2);

        verify(firstTypeListener, times(2)).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("addToModelExpectedContent.yaml"));
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(2));
        assertThat(firstTypeCaptorValues.get(1), hasSize(1));
        assertThat(firstTypeCaptorValues.get(1), contains(new FirstTypeDTO("element3", "description3")));

        List<Collection<SecondTypeDTO>> secondTypeCaptor1Values = secondTypeCaptor1.getAllValues();
        assertThat(secondTypeCaptor1Values, hasSize(1));
        assertThat(secondTypeCaptor1Values.getFirst(), hasSize(1));
        assertThat(secondTypeCaptor1Values.getFirst(), contains(new SecondTypeDTO("elt1", "My label")));
    }

    @Test
    public void testUpdateElementInModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        FirstTypeDTO updated = new FirstTypeDTO("element1", "newDescription1");
        modelRepository.updateElementInModel(MODEL_NAME, updated);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), any());
        verify(firstTypeListener).updatedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).removedModel(any(), any());

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("updateInModelExpectedContent.yaml"));
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(1));
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(1));
        assertThat(firstTypeCaptorValues.getFirst(), contains(new FirstTypeDTO("element1", "newDescription1")));
    }

    @Test
    public void testRemoveElementFromModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        FirstTypeDTO removed = new FirstTypeDTO("element1", "description1");
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.removeElementFromModel(MODEL_NAME, removed);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), any());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), firstTypeCaptor.capture());

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("removeFromModelExpectedContent.yaml"));
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(1));
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(1));
        assertThat(firstTypeCaptorValues.getFirst(), contains(new FirstTypeDTO("element1", "description1")));
    }

    @Test
    public void testReadOnlyModelNotUpdated() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

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
    }

    @Test
    public void testExistingProviderForVersion() throws IOException {
        // Provider supports version 1
        when(firstTypeListener.isVersionSupported(eq(1))).thenReturn(true);
        when(firstTypeListener.isDeprecated()).thenReturn(false);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptor.getValue();
        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
    }

    @Test
    public void testExistingDeprecatedProviderForVersion() throws IOException {
        // Provider supports version 1 as deprecated
        when(firstTypeListener.isVersionSupported(eq(1))).thenReturn(true);
        when(firstTypeListener.isDeprecated()).thenReturn(true);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptor.getValue();
        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
    }

    @Test
    public void testNoProviderForVersion() throws IOException {
        // Provider does not support version 1
        when(firstTypeListener.isVersionSupported(eq(1))).thenReturn(false);
        when(firstTypeListener.isDeprecated()).thenReturn(false);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        verify(firstTypeListener, never()).addedModel(any(), any());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
    }

    @Test
    public void testDifferentProvidersDependingOnVersion() throws IOException {
        // secondTypeListener1 supports version 1
        when(secondTypeListener1.isVersionSupported(eq(1))).thenReturn(true);
        when(secondTypeListener1.isDeprecated()).thenReturn(false);
        // secondTypeListener2 does not supports version 1
        when(secondTypeListener2.isVersionSupported(eq(1))).thenReturn(false);
        when(secondTypeListener2.isDeprecated()).thenReturn(false);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());

        verify(secondTypeListener2, never()).addedModel(any(), any());
        verify(secondTypeListener2, never()).updatedModel(any(), any());
        verify(secondTypeListener2, never()).removedModel(any(), any());

        Collection<SecondTypeDTO> secondTypeElements = secondTypeCaptor1.getValue();
        assertThat(secondTypeElements, hasSize(1));
        assertThat(secondTypeElements, contains(new SecondTypeDTO("Second1", "Label1")));
    }

    @Test
    public void testObjectFormMetadataLoadingAndGeneration() throws IOException {
        Files.copy(SOURCE_PATH.resolve("itemWithObjectFormMetadata.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        @SuppressWarnings("unchecked")
        YamlModelListener<YamlItemDTO> itemListener = mock(YamlModelListener.class);
        when(itemListener.getElementClass()).thenReturn(YamlItemDTO.class);
        when(itemListener.isVersionSupported(anyInt())).thenReturn(true);
        when(itemListener.isDeprecated()).thenReturn(false);

        modelRepository.addYamlModelListener(itemListener);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        // Verify the listener was called
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<YamlItemDTO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(itemListener).addedModel(eq(MODEL_NAME), captor.capture());

        // Verify that the valid item with object-form metadata was loaded
        Collection<YamlItemDTO> items = captor.getValue();
        assertThat(items, hasSize(1));

        YamlItemDTO item = items.iterator().next();
        assertThat(item.name, is("ValidItem"));
        assertThat(item.metadata, is(notNullValue()));
        assertThat(item.metadata.keySet(), containsInAnyOrder("alexa", "homekit"));
        assertThat(item.metadata.get("alexa").value, is("Switchable"));
        assertThat(item.metadata.get("alexa").config, is(Map.of("setting1", "value1")));
        assertThat(item.metadata.get("homekit").value, is("Lighting"));
        assertThat(item.metadata.get("homekit").config, is(nullValue()));

        // Verify YAML output contains object form metadata
        modelRepository.addElementsToBeGenerated("items", List.copyOf(items));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        modelRepository.generateFileFormat("items", out);
        String yaml = out.toString();
        // Should contain object form metadata for 'alexa'
        assertThat(yaml, containsString("alexa:"));
        assertThat(yaml, containsString("value: Switchable"));
        assertThat(yaml, containsString("config:"));
        assertThat(yaml, containsString("setting1: value1"));
    }

    @Test
    public void testShortFormMetadataLoadingAndGeneration() throws IOException {
        Files.copy(SOURCE_PATH.resolve("itemWithShortFormMetadata.yaml"), fullModelPath);
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        @SuppressWarnings("unchecked")
        YamlModelListener<YamlItemDTO> itemListener = mock(YamlModelListener.class);
        when(itemListener.getElementClass()).thenReturn(YamlItemDTO.class);
        when(itemListener.isVersionSupported(anyInt())).thenReturn(true);
        when(itemListener.isDeprecated()).thenReturn(false);

        modelRepository.addYamlModelListener(itemListener);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        // Verify the listener was called
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<YamlItemDTO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(itemListener).addedModel(eq(MODEL_NAME), captor.capture());
        // Verify items were loaded with short-form metadata
        Collection<YamlItemDTO> items = captor.getValue();
        assertThat(items, hasSize(1));

        YamlItemDTO item = items.iterator().next();
        assertThat(item.metadata, is(notNullValue()));
        assertThat(item.metadata.keySet(), containsInAnyOrder("alexa", "homekit"));
        // deserializer converts short-form to value field
        assertThat(item.metadata.get("alexa").value, is("Switchable"));
        assertThat(item.metadata.get("alexa").config, is(nullValue()));
        assertThat(item.metadata.get("homekit").value, is("Lighting"));
        assertThat(item.metadata.get("homekit").config, is(nullValue()));

        // Verify YAML output contains short form metadata
        modelRepository.addElementsToBeGenerated("items", List.copyOf(items));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        modelRepository.generateFileFormat("items", out);
        String yaml = out.toString();
        assertThat(yaml, containsString("alexa: Switchable"));
        assertThat(yaml, containsString("homekit: Lighting"));
        // Should not contain object form keys
        assertThat(yaml, not(containsString("value:")));
        assertThat(yaml, not(containsString("config:")));
    }

    @Test
    public void testMixedFormMetadataLoadingAndGeneration() throws IOException {
        Files.copy(SOURCE_PATH.resolve("itemWithMixedFormMetadata.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        @SuppressWarnings("unchecked")
        YamlModelListener<YamlItemDTO> itemListener = mock(YamlModelListener.class);
        when(itemListener.getElementClass()).thenReturn(YamlItemDTO.class);
        when(itemListener.isVersionSupported(anyInt())).thenReturn(true);
        when(itemListener.isDeprecated()).thenReturn(false);

        modelRepository.addYamlModelListener(itemListener);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        // Verify the listener was called
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<YamlItemDTO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(itemListener).addedModel(eq(MODEL_NAME), captor.capture());

        // Verify that the valid item with mixed-form metadata was loaded
        Collection<YamlItemDTO> items = captor.getValue();
        assertThat(items, hasSize(1));

        YamlItemDTO item = items.iterator().next();
        assertThat(item.name, is("ValidItem"));
        assertThat(item.metadata, is(notNullValue()));
        assertThat(item.metadata.keySet(), containsInAnyOrder("alexa", "homekit"));
        assertThat(item.metadata.get("alexa").value, is("Switchable"));
        assertThat(item.metadata.get("alexa").config, is(Map.of("setting1", "value1")));
        assertThat(item.metadata.get("homekit").value, is("Lighting"));
        assertThat(item.metadata.get("homekit").config, is(nullValue()));

        // Verify YAML output contains metadata in both forms
        modelRepository.addElementsToBeGenerated("items", List.copyOf(items));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        modelRepository.generateFileFormat("items", out);
        String yaml = out.toString();
        // Should contain object form metadata for 'alexa'
        assertThat(yaml, containsString("alexa:"));
        assertThat(yaml, containsString("value: Switchable"));
        assertThat(yaml, containsOnlyOnce("config:"));
        assertThat(yaml, containsString("setting1: value1"));
        // Should contain short form metadata for 'homekit'
        assertThat(yaml, containsString("homekit: Lighting"));
    }

    private static Matcher<String> containsOnlyOnce(String substring) {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return StringUtils.countMatches(item, substring) == 1;
            }

            @Override
            @NonNullByDefault({})
            public void describeTo(Description description) {
                description.appendText("string appearing exactly once: ").appendValue(substring);
            }

            @Override
            @NonNullByDefault({})
            protected void describeMismatchSafely(String item, Description mismatchDescription) {
                int count = StringUtils.countMatches(item, substring);
                mismatchDescription.appendText("was found ").appendValue(count).appendText(" times");
            }
        };
    }
}
