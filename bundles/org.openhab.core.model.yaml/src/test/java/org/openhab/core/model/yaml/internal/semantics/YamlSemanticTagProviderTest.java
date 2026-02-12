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
package org.openhab.core.model.yaml.internal.semantics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.internal.YamlModelRepositoryImpl;
import org.openhab.core.service.WatchService;

/**
 * The {@link YamlSemanticTagProviderTest} contains tests for the {@link YamlSemanticTagProvider} class.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlSemanticTagProviderTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/model/semantics");
    private static final String MODEL_NAME = "model.yaml";
    private static final Path MODEL_PATH = Path.of(MODEL_NAME);

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;

    private @NonNullByDefault({}) YamlModelRepositoryImpl modelRepository;

    private @TempDir @NonNullByDefault({}) Path watchPath;
    private @NonNullByDefault({}) Path fullModelPath;
    private @NonNullByDefault({}) YamlModelListener<YamlSemanticTagDTO> tagListener;

    @BeforeEach
    @SuppressWarnings({ "null", "unchecked" })
    public void setup() {
        fullModelPath = watchPath.resolve(MODEL_PATH);
        when(watchServiceMock.getWatchPath()).thenReturn(watchPath);

        modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        tagListener = mock(YamlModelListener.class);
        when(tagListener.getElementClass()).thenReturn(YamlSemanticTagDTO.class);
        when(tagListener.isVersionSupported(anyInt())).thenReturn(true);
        when(tagListener.isDeprecated()).thenReturn(false);

        modelRepository.addYamlModelListener(tagListener);
    }

    @Test
    public void testShortFormTagsLoadingAndGeneration() throws IOException {
        processYamlResource("tagWithShortFormSyntax.yaml");
        Collection<YamlSemanticTagDTO> tags = captureAddedTags();

        assertThat(tags, hasSize(1));
        YamlSemanticTagDTO tag = tags.iterator().next();
        assertThat(tag.getId(), is("Tag_uid"));
        assertThat(tag.label, is("TagLabel"));
        assertThat(tag.description, is(nullValue()));

        // Verify YAML output contains short form
        String outYaml = generateYamlFromTags(tags);
        assertThat(outYaml, containsString("Tag_uid: TagLabel"));
    }

    @Test
    public void testMapFormTagsLoadingAndGeneration() throws IOException {
        processYamlResource("tagWithMapForm.yaml");
        Collection<YamlSemanticTagDTO> tags = captureAddedTags();

        assertThat(tags, hasSize(1));
        YamlSemanticTagDTO tag = tags.iterator().next();
        assertThat(tag.getId(), is("Tag_uid"));
        assertThat(tag.label, is("TagLabel"));
        assertThat(tag.description, is("Some description"));
        assertThat(tag.synonyms, contains("syn1", "syn2"));

        // Verify YAML output contains map form
        String outYaml = generateYamlFromTags(tags);
        assertThat(outYaml, containsString("label: TagLabel"));
        assertThat(outYaml, containsString("description: Some description"));
        assertThat(outYaml, containsString("syn1"));
        assertThat(outYaml, containsString("syn2"));
    }

    @Test
    public void testShortFormTagWithNullLabelDeserializesAndSerializes() throws IOException {
        processYamlResource("tagWithShortFormSyntaxWithNullLabel.yaml");
        Collection<YamlSemanticTagDTO> tags = captureAddedTags();

        assertThat(tags, hasSize(1));
        YamlSemanticTagDTO tag = tags.iterator().next();
        assertThat(tag.getId(), is("Tag_null"));
        // DTO currently represents an explicit null label as null
        assertThat(tag.label, is(nullValue()));
        assertThat(tag.description, is(nullValue()));
        String outYaml = generateYamlFromTags(tags);

        // Generation should contain the tag key but should not emit label/description/synonyms
        assertThat(outYaml, containsString("Tag_null:"));
        assertThat(outYaml, not(containsString("label:")));
        assertThat(outYaml, not(containsString("description:")));
        assertThat(outYaml, not(containsString("synonyms:")));
    }

    @SuppressWarnings("unchecked")
    private Collection<YamlSemanticTagDTO> captureAddedTags() {
        ArgumentCaptor<Collection<YamlSemanticTagDTO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(tagListener).addedModel(eq(MODEL_NAME), captor.capture());
        return captor.getValue();
    }

    private void processYamlResource(String resourceName) throws IOException {
        Files.copy(SOURCE_PATH.resolve(resourceName), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);
    }

    private String generateYamlFromTags(Collection<YamlSemanticTagDTO> tags) {
        modelRepository.addElementsToBeGenerated("tags", List.copyOf(tags));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        modelRepository.generateFileFormat("tags", out);
        return out.toString();
    }
}
