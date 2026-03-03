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
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.internal.YamlModelRepositoryImpl;
import org.openhab.core.semantics.SemanticTag;
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
    private @NonNullByDefault({}) YamlSemanticTagProvider semanticTagProvider;
    private @NonNullByDefault({}) TestSemanticTagChangeListener semanticTagListener;

    private @TempDir @NonNullByDefault({}) Path watchPath;
    private @NonNullByDefault({}) Path fullModelPath;

    @BeforeEach
    @SuppressWarnings({ "null", "unchecked" })
    public void setup() {
        fullModelPath = watchPath.resolve(MODEL_PATH);
        when(watchServiceMock.getWatchPath()).thenReturn(watchPath);

        semanticTagListener = new TestSemanticTagChangeListener();

        semanticTagProvider = new YamlSemanticTagProvider();
        semanticTagProvider.addProviderChangeListener(semanticTagListener);

        modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(semanticTagProvider);
    }

    @Test
    public void testShortFormTagsLoadingAndGeneration() throws IOException {
        processYamlResource("tagWithShortFormSyntax.yaml");

        assertThat(semanticTagListener.semanticTags, is(aMapWithSize(1)));
        assertThat(semanticTagListener.semanticTags, hasKey("Tag_uid"));
        Collection<SemanticTag> tags = semanticTagProvider.getAll();
        assertThat(tags, hasSize(1));
        SemanticTag tag = tags.iterator().next();
        assertThat(tag.getUID(), is("Tag_uid"));
        assertThat(tag.getName(), is("uid"));
        assertThat(tag.getParentUID(), is("Tag"));
        assertThat(tag.getLabel(), is("TagLabel"));
        assertThat(tag.getDescription(), is(""));
        assertThat(tag.getSynonyms(), hasSize(0));

        // Verify YAML output contains short form
        String outYaml = generateYamlFromTags(tags);
        assertThat(outYaml, containsString("Tag_uid: TagLabel"));
    }

    @Test
    public void testMapFormTagsLoadingAndGeneration() throws IOException {
        processYamlResource("tagWithMapForm.yaml");

        assertThat(semanticTagListener.semanticTags, is(aMapWithSize(1)));
        assertThat(semanticTagListener.semanticTags, hasKey("Tag_uid"));
        Collection<SemanticTag> tags = semanticTagProvider.getAll();
        assertThat(tags, hasSize(1));
        SemanticTag tag = tags.iterator().next();
        assertThat(tag.getUID(), is("Tag_uid"));
        assertThat(tag.getName(), is("uid"));
        assertThat(tag.getParentUID(), is("Tag"));
        assertThat(tag.getLabel(), is("TagLabel"));
        assertThat(tag.getDescription(), is("Some description"));
        assertThat(tag.getSynonyms(), contains("syn1", "syn2"));

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

        assertThat(semanticTagListener.semanticTags, is(aMapWithSize(1)));
        assertThat(semanticTagListener.semanticTags, hasKey("Tag_null"));
        Collection<SemanticTag> tags = semanticTagProvider.getAll();
        assertThat(tags, hasSize(1));
        SemanticTag tag = tags.iterator().next();
        // DTO currently represents an explicit null label as null
        assertThat(tag.getUID(), is("Tag_null"));
        assertThat(tag.getName(), is("null"));
        assertThat(tag.getParentUID(), is("Tag"));
        assertThat(tag.getLabel(), is(""));
        assertThat(tag.getDescription(), is(""));
        assertThat(tag.getSynonyms(), hasSize(0));

        // Generation should contain the tag key but should not emit label/description/synonyms
        String outYaml = generateYamlFromTags(tags);
        assertThat(outYaml, containsString("Tag_null:"));
        assertThat(outYaml, not(containsString("label:")));
        assertThat(outYaml, not(containsString("description:")));
        assertThat(outYaml, not(containsString("synonyms:")));
    }

    private void processYamlResource(String resourceName) throws IOException {
        Files.copy(SOURCE_PATH.resolve(resourceName), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);
    }

    private String generateYamlFromTags(Collection<SemanticTag> tags) {
        List<YamlElement> elts = new ArrayList<>();
        tags.forEach(tag -> {
            YamlSemanticTagDTO dto = new YamlSemanticTagDTO();
            dto.uid = tag.getUID();
            dto.label = tag.getLabel();
            dto.description = tag.getDescription();
            dto.synonyms = tag.getSynonyms();
            elts.add(dto);
        });
        modelRepository.addElementsToBeGenerated("tags", elts);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        modelRepository.generateFileFormat("tags", out);
        return out.toString();
    }

    private static class TestSemanticTagChangeListener implements ProviderChangeListener<SemanticTag> {

        public final Map<String, SemanticTag> semanticTags = new HashMap<>();

        @Override
        public void added(Provider<SemanticTag> provider, SemanticTag element) {
            semanticTags.put(element.getUID(), element);
        }

        @Override
        public void removed(Provider<SemanticTag> provider, SemanticTag element) {
            semanticTags.remove(element.getUID());
        }

        @Override
        public void updated(Provider<SemanticTag> provider, SemanticTag oldelement, SemanticTag element) {
            semanticTags.put(element.getUID(), element);
        }
    }
}
