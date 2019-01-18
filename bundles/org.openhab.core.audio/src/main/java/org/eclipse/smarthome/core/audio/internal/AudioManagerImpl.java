/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.audio.internal;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.config.core.ConfigOptionProvider;
import org.eclipse.smarthome.config.core.ConfigurableService;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.core.audio.AudioException;
import org.eclipse.smarthome.core.audio.AudioManager;
import org.eclipse.smarthome.core.audio.AudioSink;
import org.eclipse.smarthome.core.audio.AudioSource;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.audio.FileAudioStream;
import org.eclipse.smarthome.core.audio.URLAudioStream;
import org.eclipse.smarthome.core.audio.UnsupportedAudioFormatException;
import org.eclipse.smarthome.core.audio.UnsupportedAudioStreamException;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service provides functionality around audio services and is the central service to be used directly by others.
 *
 * @author Karel Goderis - Initial contribution and API
 * @author Kai Kreuzer - removed unwanted dependencies
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Sort audio sink and source options
 */
@Component(immediate = true, configurationPid = "org.eclipse.smarthome.audio", property = { //
        Constants.SERVICE_PID + "=org.eclipse.smarthome.audio", //
        ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=system", //
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=" + AudioManagerImpl.CONFIG_URI, //
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=Audio" //
})
public class AudioManagerImpl implements AudioManager, ConfigOptionProvider {

    // constants for the configuration properties
    static final String CONFIG_URI = "system:audio";
    static final String CONFIG_DEFAULT_SINK = "defaultSink";
    static final String CONFIG_DEFAULT_SOURCE = "defaultSource";

    private final Logger logger = LoggerFactory.getLogger(AudioManagerImpl.class);

    // service maps
    private final Map<String, AudioSource> audioSources = new ConcurrentHashMap<>();
    private final Map<String, AudioSink> audioSinks = new ConcurrentHashMap<>();

    /**
     * default settings filled through the service configuration
     */
    private String defaultSource;
    private String defaultSink;

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Deactivate
    protected void deactivate() {
    }

    @Modified
    void modified(Map<String, Object> config) {
        if (config != null) {
            this.defaultSource = config.containsKey(CONFIG_DEFAULT_SOURCE)
                    ? config.get(CONFIG_DEFAULT_SOURCE).toString()
                    : null;
            this.defaultSink = config.containsKey(CONFIG_DEFAULT_SINK) ? config.get(CONFIG_DEFAULT_SINK).toString()
                    : null;
        }
    }

    @Override
    public void play(AudioStream audioStream) {
        play(audioStream, null);
    }

    @Override
    public void play(AudioStream audioStream, String sinkId) {
        play(audioStream, sinkId, null);
    }

    @Override
    public void play(AudioStream audioStream, String sinkId, PercentType volume) {
        AudioSink sink = getSink(sinkId);
        if (sink != null) {
            PercentType oldVolume = null;
            try {
                // get current volume
                oldVolume = getVolume(sinkId);
            } catch (IOException e) {
                logger.debug("An exception occurred while getting the volume of sink '{}' : {}", sink.getId(),
                        e.getMessage(), e);
            }
            // set notification sound volume
            if (volume != null) {
                try {
                    setVolume(volume, sinkId);
                } catch (IOException e) {
                    logger.debug("An exception occurred while setting the volume of sink '{}' : {}", sink.getId(),
                            e.getMessage(), e);
                }
            }
            try {
                sink.process(audioStream);
            } catch (UnsupportedAudioFormatException | UnsupportedAudioStreamException e) {
                logger.warn("Error playing '{}': {}", audioStream, e.getMessage(), e);
            } finally {
                if (volume != null && oldVolume != null) {
                    // restore volume only if it was set before
                    try {
                        setVolume(oldVolume, sinkId);
                    } catch (IOException e) {
                        logger.debug("An exception occurred while setting the volume of sink '{}' : {}", sink.getId(),
                                e.getMessage(), e);
                    }
                }
            }
        } else {
            logger.warn("Failed playing audio stream '{}' as no audio sink was found.", audioStream);
        }
    }

    @Override
    public void playFile(String fileName) throws AudioException {
        playFile(fileName, null, null);
    }

    @Override
    public void playFile(String fileName, PercentType volume) throws AudioException {
        playFile(fileName, null, volume);
    }

    @Override
    public void playFile(String fileName, String sinkId) throws AudioException {
        playFile(fileName, sinkId, null);
    }

    @Override
    public void playFile(String fileName, String sinkId, PercentType volume) throws AudioException {
        Objects.requireNonNull(fileName, "File cannot be played as fileName is null.");

        File file = new File(
                ConfigConstants.getConfigFolder() + File.separator + SOUND_DIR + File.separator + fileName);
        FileAudioStream is = new FileAudioStream(file);
        play(is, sinkId, volume);
    }

    @Override
    public void stream(String url) throws AudioException {
        stream(url, null);
    }

    @Override
    public void stream(String url, String sinkId) throws AudioException {
        AudioStream audioStream = url != null ? new URLAudioStream(url) : null;
        play(audioStream, sinkId, null);
    }

    @Override
    public PercentType getVolume(String sinkId) throws IOException {
        AudioSink sink = getSink(sinkId);

        if (sink != null) {
            return sink.getVolume();
        }
        return PercentType.ZERO;
    }

    @Override
    public void setVolume(PercentType volume, String sinkId) throws IOException {
        AudioSink sink = getSink(sinkId);

        if (sink != null) {
            sink.setVolume(volume);
        }
    }

    @Override
    public AudioSource getSource() {
        AudioSource source = null;
        if (defaultSource != null) {
            source = audioSources.get(defaultSource);
            if (source == null) {
                logger.warn("Default AudioSource service '{}' not available!", defaultSource);
            }
        } else if (!audioSources.isEmpty()) {
            source = audioSources.values().iterator().next();
        } else {
            logger.debug("No AudioSource service available!");
        }
        return source;
    }

    @Override
    public Set<AudioSource> getAllSources() {
        return new HashSet<>(audioSources.values());
    }

    @Override
    public AudioSink getSink() {
        AudioSink sink = null;
        if (defaultSink != null) {
            sink = audioSinks.get(defaultSink);
            if (sink == null) {
                logger.warn("Default AudioSink service '{}' not available!", defaultSink);
            }
        } else if (!audioSinks.isEmpty()) {
            sink = audioSinks.values().iterator().next();
        } else {
            logger.debug("No AudioSink service available!");
        }
        return sink;
    }

    @Override
    public Set<AudioSink> getAllSinks() {
        return new HashSet<>(audioSinks.values());
    }

    @Override
    public Set<String> getSourceIds(String pattern) {
        String regex = pattern.replace("?", ".?").replace("*", ".*?");
        Set<String> matchedSources = new HashSet<>();

        for (String aSource : audioSources.keySet()) {
            if (aSource.matches(regex)) {
                matchedSources.add(aSource);
            }
        }

        return matchedSources;
    }

    @Override
    public AudioSink getSink(String sinkId) {
        return (sinkId == null) ? getSink() : audioSinks.get(sinkId);
    }

    @Override
    public Set<String> getSinkIds(String pattern) {
        String regex = pattern.replace("?", ".?").replace("*", ".*?");
        Set<String> matchedSinkIds = new HashSet<>();

        for (String sinkId : audioSinks.keySet()) {
            if (sinkId.matches(regex)) {
                matchedSinkIds.add(sinkId);
            }
        }

        return matchedSinkIds;
    }

    @Override
    public Collection<ParameterOption> getParameterOptions(URI uri, String param, Locale locale) {
        if (uri.toString().equals(CONFIG_URI)) {
            final Locale safeLocale = locale != null ? locale : Locale.getDefault();
            if (CONFIG_DEFAULT_SOURCE.equals(param)) {
                return audioSources.values().stream().sorted(comparing(s -> s.getLabel(safeLocale)))
                        .map(s -> new ParameterOption(s.getId(), s.getLabel(safeLocale))).collect(toList());
            } else if (CONFIG_DEFAULT_SINK.equals(param)) {
                return audioSinks.values().stream().sorted(comparing(s -> s.getLabel(safeLocale)))
                        .map(s -> new ParameterOption(s.getId(), s.getLabel(safeLocale))).collect(toList());
            }
        }
        return null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addAudioSource(AudioSource audioSource) {
        this.audioSources.put(audioSource.getId(), audioSource);
    }

    protected void removeAudioSource(AudioSource audioSource) {
        this.audioSources.remove(audioSource.getId());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addAudioSink(AudioSink audioSink) {
        this.audioSinks.put(audioSink.getId(), audioSink);
    }

    protected void removeAudioSink(AudioSink audioSink) {
        this.audioSinks.remove(audioSink.getId());
    }

}
