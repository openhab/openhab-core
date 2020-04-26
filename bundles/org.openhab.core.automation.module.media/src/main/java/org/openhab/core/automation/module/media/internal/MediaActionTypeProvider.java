/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.ConfigConstants;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ParameterOption;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class dynamically provides the Play action type.
 * This is necessary since there is no other way to provide dynamic config param options for module types.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Kaufmann - added "say" action
 */
@NonNullByDefault
@Component(immediate = true)
public class MediaActionTypeProvider implements ModuleTypeProvider {

    private @NonNullByDefault({}) AudioManager audioManager;

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable ModuleType getModuleType(String UID, @Nullable Locale locale) {
        if (PlayActionHandler.TYPE_ID.equals(UID)) {
            return getPlayActionType(locale);
        } else if (SayActionHandler.TYPE_ID.equals(UID)) {
            return getSayActionType(locale);
        } else {
            return null;
        }
    }

    @Override
    public Collection<ModuleType> getModuleTypes(@Nullable Locale locale) {
        return Stream.of(getPlayActionType(locale), getSayActionType(locale)).collect(Collectors.toList());
    }

    private ModuleType getPlayActionType(@Nullable Locale locale) {
        return new ActionType(PlayActionHandler.TYPE_ID, getConfigPlayDesc(locale), "play a sound",
                "Plays a sound file.", null, Visibility.VISIBLE, new ArrayList<>(), new ArrayList<>());
    }

    private ModuleType getSayActionType(@Nullable Locale locale) {
        return new ActionType(SayActionHandler.TYPE_ID, getConfigSayDesc(locale), "say something",
                "Speaks a given text through a natural voice.", null, Visibility.VISIBLE, new ArrayList<>(),
                new ArrayList<>());
    }

    private List<ConfigDescriptionParameter> getConfigPlayDesc(@Nullable Locale locale) {
        ConfigDescriptionParameter param1 = ConfigDescriptionParameterBuilder
                .create(PlayActionHandler.PARAM_SOUND, Type.TEXT).withRequired(true).withLabel("Sound")
                .withDescription("the sound to play").withOptions(getSoundOptions()).withLimitToOptions(true).build();
        return Stream.of(param1, getAudioSinkConfigDescParam(locale)).collect(Collectors.toList());
    }

    private List<ConfigDescriptionParameter> getConfigSayDesc(@Nullable Locale locale) {
        ConfigDescriptionParameter param1 = ConfigDescriptionParameterBuilder
                .create(SayActionHandler.PARAM_TEXT, Type.TEXT).withRequired(true).withLabel("Text")
                .withDescription("the text to speak").build();
        return Stream.of(param1, getAudioSinkConfigDescParam(locale)).collect(Collectors.toList());
    }

    private ConfigDescriptionParameter getAudioSinkConfigDescParam(@Nullable Locale locale) {
        ConfigDescriptionParameter param2 = ConfigDescriptionParameterBuilder
                .create(SayActionHandler.PARAM_SINK, Type.TEXT).withRequired(false).withLabel("Sink")
                .withDescription("the audio sink id").withOptions(getSinkOptions(locale)).withLimitToOptions(true)
                .build();
        return param2;
    }

    /**
     * This method creates one option for every file that is found in the sounds directory.
     * As a label, the file extension is removed and the string is capitalized.
     *
     * @return a list of parameter options representing the sound files
     */
    private List<ParameterOption> getSoundOptions() {
        List<ParameterOption> options = new ArrayList<>();
        File soundsDir = Paths.get(ConfigConstants.getConfigFolder(), AudioManager.SOUND_DIR).toFile();
        if (soundsDir.isDirectory()) {
            for (String fileName : soundsDir.list()) {
                if (fileName.contains(".") && !fileName.startsWith(".")) {
                    String soundName = fileName.substring(0, fileName.lastIndexOf("."));
                    String capitalizedSoundName = soundName.substring(0, 1).toUpperCase()
                            + soundName.substring(1).toLowerCase();
                    options.add(new ParameterOption(fileName, capitalizedSoundName));
                }
            }
        }
        return options;
    }

    /**
     * This method creates one option for every sink that is found in the system.
     *
     * @return a list of parameter options representing the audio sinks
     */
    private List<ParameterOption> getSinkOptions(@Nullable Locale locale) {
        List<ParameterOption> options = new ArrayList<>();

        for (AudioSink sink : audioManager.getAllSinks()) {
            options.add(new ParameterOption(sink.getId(), sink.getLabel(locale)));
        }
        return options;
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

    @Reference
    protected void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    protected void unsetAudioManager(AudioManager audioManager) {
        this.audioManager = null;
    }
}
