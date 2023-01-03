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
package org.openhab.core.automation.module.media.internal;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ParameterOption;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class dynamically provides the Play, Say and Synthesize action types.
 * This is necessary since there is no other way to provide dynamic config param options for module types.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Kaufmann - added "say" action
 * @author Christoph Weitkamp - Added parameter volume
 */
@NonNullByDefault
@Component(service = ModuleTypeProvider.class)
public class MediaActionTypeProvider implements ModuleTypeProvider {

    private final AudioManager audioManager;

    @Activate
    public MediaActionTypeProvider(final @Reference AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable ModuleType getModuleType(String UID, @Nullable Locale locale) {
        switch (UID) {
            case PlayActionHandler.TYPE_ID:
                return getPlayActionType(locale);
            case SayActionHandler.TYPE_ID:
                return getSayActionType(locale);
            case SynthesizeActionHandler.TYPE_ID:
                return getSynthesizeActionType(locale);
            default:
                return null;
        }
    }

    @Override
    public Collection<ModuleType> getModuleTypes(@Nullable Locale locale) {
        return List.of(getPlayActionType(locale), getSayActionType(locale));
    }

    private ModuleType getPlayActionType(@Nullable Locale locale) {
        return new ActionType(PlayActionHandler.TYPE_ID, getConfigPlayDesc(locale), "play a sound",
                "Plays a sound file. Optionally sets the volume.", null, Visibility.VISIBLE, null, null);
    }

    private ModuleType getSayActionType(@Nullable Locale locale) {
        return new ActionType(SayActionHandler.TYPE_ID, getConfigSayDesc(locale), "say something",
                "Speaks a given text through a natural voice. Optionally sets the volume.", null, Visibility.VISIBLE,
                null, null);
    }

    private ModuleType getSynthesizeActionType(@Nullable Locale locale) {
        return new ActionType(SynthesizeActionHandler.TYPE_ID, getConfigSynthesizeDesc(locale),
                "synthesize a tone melody", "Synthesize the given melody text and play it. Optionally sets the volume.",
                null, Visibility.VISIBLE, null, null);
    }

    private List<ConfigDescriptionParameter> getConfigPlayDesc(@Nullable Locale locale) {
        return List.of(
                ConfigDescriptionParameterBuilder.create(PlayActionHandler.PARAM_SOUND, Type.TEXT).withRequired(true)
                        .withLabel("Sound").withDescription("the sound to play").withOptions(getSoundOptions())
                        .withLimitToOptions(true).build(),
                getAudioSinkConfigDescParam(locale), getVolumeConfigDescParam(locale));
    }

    private List<ConfigDescriptionParameter> getConfigSayDesc(@Nullable Locale locale) {
        return List.of(
                ConfigDescriptionParameterBuilder.create(SayActionHandler.PARAM_TEXT, Type.TEXT).withRequired(true)
                        .withLabel("Text").withDescription("the text to speak").build(),
                getAudioSinkConfigDescParam(locale), getVolumeConfigDescParam(locale));
    }

    private List<ConfigDescriptionParameter> getConfigSynthesizeDesc(@Nullable Locale locale) {
        return List.of(
                ConfigDescriptionParameterBuilder.create(SynthesizeActionHandler.PARAM_MELODY, Type.TEXT)
                        .withRequired(true).withLabel("Melody")
                        .withDescription("the melody as spaced separated note names").build(),
                getAudioSinkConfigDescParam(locale), getVolumeConfigDescParam(locale));
    }

    private ConfigDescriptionParameter getAudioSinkConfigDescParam(@Nullable Locale locale) {
        ConfigDescriptionParameter param2 = ConfigDescriptionParameterBuilder
                .create(SayActionHandler.PARAM_SINK, Type.TEXT).withRequired(false).withLabel("Sink")
                .withDescription("the audio sink id").withOptions(getSinkOptions(locale)).withLimitToOptions(true)
                .build();
        return param2;
    }

    private ConfigDescriptionParameter getVolumeConfigDescParam(@Nullable Locale locale) {
        ConfigDescriptionParameter param3 = ConfigDescriptionParameterBuilder
                .create(SayActionHandler.PARAM_VOLUME, Type.INTEGER).withLabel("Volume")
                .withDescription("the volume to use").withMinimum(BigDecimal.ZERO).withMaximum(BigDecimal.valueOf(100))
                .withStepSize(BigDecimal.ONE).build();
        return param3;
    }

    /**
     * This method creates one option for every file that is found in the sounds directory.
     * As a label, the file extension is removed and the string is capitalized.
     *
     * @return a list of parameter options representing the sound files
     */
    private List<ParameterOption> getSoundOptions() {
        List<ParameterOption> options = new ArrayList<>();
        File soundsDir = Paths.get(OpenHAB.getConfigFolder(), AudioManager.SOUND_DIR).toFile();
        if (soundsDir.isDirectory()) {
            for (String fileName : soundsDir.list()) {
                if (fileName.contains(".") && !fileName.startsWith(".")) {
                    String soundName = fileName.substring(0, fileName.lastIndexOf("."));
                    String capitalizedSoundName = soundName.substring(0, 1).toUpperCase()
                            + soundName.substring(1).toLowerCase();
                    options.add(new ParameterOption(fileName, capitalizedSoundName));
                }
            }
            options.sort(comparing(o -> o.getLabel()));
        }
        return options;
    }

    /**
     * This method creates one option for every sink that is found in the system.
     *
     * @return a list of parameter options representing the audio sinks
     */
    private List<ParameterOption> getSinkOptions(@Nullable Locale locale) {
        final Locale safeLocale = locale != null ? locale : Locale.getDefault();
        return audioManager.getAllSinks().stream().sorted(comparing(s -> s.getLabel(safeLocale)))
                .map(s -> new ParameterOption(s.getId(), s.getLabel(safeLocale))).collect(toList());
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        // does nothing because this provider does not change
    }

    @Override
    public Collection<ModuleType> getAll() {
        return getModuleTypes(null);
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        // does nothing because this provider does not change
    }
}
