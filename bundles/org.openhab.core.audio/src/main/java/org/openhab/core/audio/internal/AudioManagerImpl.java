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
package org.openhab.core.audio.internal;

import static java.util.Comparator.comparing;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FileAudioStream;
import org.openhab.core.audio.URLAudioStream;
import org.openhab.core.audio.utils.ToneSynthesizer;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.library.types.PercentType;
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
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - removed unwanted dependencies
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Sort audio sink and source options
 */
@NonNullByDefault
@Component(immediate = true, configurationPid = "org.openhab.audio", //
        property = Constants.SERVICE_PID + "=org.openhab.audio")
@ConfigurableService(category = "system", label = "Audio", description_uri = AudioManagerImpl.CONFIG_URI)
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
    private @Nullable String defaultSource;
    private @Nullable String defaultSink;

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Deactivate
    protected void deactivate() {
    }

    @Modified
    void modified(@Nullable Map<String, Object> config) {
        if (config != null) {
            this.defaultSource = config.containsKey(CONFIG_DEFAULT_SOURCE)
                    ? config.get(CONFIG_DEFAULT_SOURCE).toString()
                    : null;
            this.defaultSink = config.containsKey(CONFIG_DEFAULT_SINK) ? config.get(CONFIG_DEFAULT_SINK).toString()
                    : null;
        }
    }

    @Override
    public void play(@Nullable AudioStream audioStream) {
        play(audioStream, null);
    }

    @Override
    public void play(@Nullable AudioStream audioStream, @Nullable String sinkId) {
        play(audioStream, sinkId, null);
    }

    @Override
    public void play(@Nullable AudioStream audioStream, @Nullable String sinkId, @Nullable PercentType volume) {
        AudioSink sink = getSink(sinkId);
        if (sink != null) {
            Runnable restoreVolume = handleVolumeCommand(volume, sink);
            sink.processAndComplete(audioStream).exceptionally(exception -> {
                logger.warn("Error playing '{}': {}", audioStream, exception.getMessage(), exception);
                return null;
            }).thenRun(restoreVolume);
        } else {
            logger.warn("Failed playing audio stream '{}' as no audio sink was found.", audioStream);
        }
    }

    @Override
    public void playFile(String fileName) throws AudioException {
        playFile(fileName, null, null);
    }

    @Override
    public void playFile(String fileName, @Nullable PercentType volume) throws AudioException {
        playFile(fileName, null, volume);
    }

    @Override
    public void playFile(String fileName, @Nullable String sinkId) throws AudioException {
        playFile(fileName, sinkId, null);
    }

    @Override
    public void playFile(String fileName, @Nullable String sinkId, @Nullable PercentType volume) throws AudioException {
        Objects.requireNonNull(fileName, "File cannot be played as fileName is null.");

        File file = new File(OpenHAB.getConfigFolder() + File.separator + SOUND_DIR + File.separator + fileName);
        FileAudioStream is = new FileAudioStream(file);
        play(is, sinkId, volume);
    }

    @Override
    public void stream(@Nullable String url) throws AudioException {
        stream(url, null);
    }

    @Override
    public void stream(@Nullable String url, @Nullable String sinkId) throws AudioException {
        AudioStream audioStream = url != null ? new URLAudioStream(url) : null;
        play(audioStream, sinkId, null);
    }

    @Override
    public void playMelody(String melody) {
        playMelody(melody, null);
    }

    @Override
    public void playMelody(String melody, @Nullable String sinkId) {
        playMelody(melody, sinkId, null);
    }

    @Override
    public void playMelody(String melody, @Nullable String sinkId, @Nullable PercentType volume) {
        AudioSink sink = getSink(sinkId);
        if (sink == null) {
            logger.warn("Failed playing melody as no audio sink {} was found.", sinkId);
            return;
        }
        var synthesizerFormat = AudioFormat.getBestMatch(ToneSynthesizer.getSupportedFormats(),
                sink.getSupportedFormats());
        if (synthesizerFormat == null) {
            logger.warn("Failed playing melody as sink {} does not support wav.", sinkId);
            return;
        }
        try {
            var audioStream = new ToneSynthesizer(synthesizerFormat).getStream(ToneSynthesizer.parseMelody(melody));
            play(audioStream, sinkId, volume);
        } catch (IOException | ParseException e) {
            logger.warn("Failed playing melody: {}", e.getMessage());
        }
    }

    @Override
    public PercentType getVolume(@Nullable String sinkId) throws IOException {
        AudioSink sink = getSink(sinkId);

        if (sink != null) {
            return sink.getVolume();
        }
        return PercentType.ZERO;
    }

    @Override
    public void setVolume(PercentType volume, @Nullable String sinkId) throws IOException {
        AudioSink sink = getSink(sinkId);

        if (sink != null) {
            sink.setVolume(volume);
        }
    }

    @Override
    public @Nullable String getSourceId() {
        return defaultSource;
    }

    @Override
    public @Nullable AudioSource getSource() {
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
    public @Nullable AudioSource getSource(@Nullable String sourceId) {
        return (sourceId == null) ? getSource() : audioSources.get(sourceId);
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
    public @Nullable String getSinkId() {
        return defaultSink;
    }

    @Override
    public @Nullable AudioSink getSink() {
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
    public @Nullable AudioSink getSink(@Nullable String sinkId) {
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
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (CONFIG_URI.equals(uri.toString())) {
            final Locale safeLocale = locale != null ? locale : Locale.getDefault();
            if (CONFIG_DEFAULT_SOURCE.equals(param)) {
                return audioSources.values().stream().sorted(comparing(s -> s.getLabel(safeLocale)))
                        .map(s -> new ParameterOption(s.getId(), s.getLabel(safeLocale))).toList();
            } else if (CONFIG_DEFAULT_SINK.equals(param)) {
                return audioSinks.values().stream().sorted(comparing(s -> s.getLabel(safeLocale)))
                        .map(s -> new ParameterOption(s.getId(), s.getLabel(safeLocale))).toList();
            }
        }
        return null;
    }

    @Override
    public Runnable handleVolumeCommand(@Nullable PercentType volume, AudioSink sink) {
        boolean volumeChanged = false;
        PercentType oldVolume = null;

        Runnable toRunWhenProcessFinished = () -> {
        };

        if (volume == null) {
            return toRunWhenProcessFinished;
        }

        // set notification sound volume
        try {
            // get current volume
            oldVolume = sink.getVolume();
        } catch (IOException | UnsupportedOperationException e) {
            logger.debug("An exception occurred while getting the volume of sink '{}' : {}", sink.getId(),
                    e.getMessage(), e);
        }

        if (!volume.equals(oldVolume) || oldVolume == null) {
            try {
                sink.setVolume(volume);
                volumeChanged = true;
            } catch (IOException | UnsupportedOperationException e) {
                logger.debug("An exception occurred while setting the volume of sink '{}' : {}", sink.getId(),
                        e.getMessage(), e);
            }
        }

        final PercentType oldVolumeFinal = oldVolume;
        // restore volume only if it was set before
        if (volumeChanged && oldVolumeFinal != null) {
            toRunWhenProcessFinished = () -> {
                try {
                    sink.setVolume(oldVolumeFinal);
                } catch (IOException | UnsupportedOperationException e) {
                    logger.debug("An exception occurred while setting the volume of sink '{}' : {}", sink.getId(),
                            e.getMessage(), e);
                }
            };
        }

        return toRunWhenProcessFinished;
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
