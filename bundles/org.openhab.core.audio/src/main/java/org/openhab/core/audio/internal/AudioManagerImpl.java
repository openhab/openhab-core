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
package org.openhab.core.audio.internal;

import static java.util.Comparator.comparing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

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
import org.openhab.core.audio.utils.AudioWaveUtils;
import org.openhab.core.audio.utils.ToneSynthesizer;
import org.openhab.core.common.ThreadPoolManager;
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

import io.reactivex.annotations.NonNull;

/**
 * This service provides functionality around audio services and is the central service to be used directly by others.
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - removed unwanted dependencies
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Sort audio sink and source options
 * @author Miguel Álvarez - Add record from source
 * @author Karel Goderis - Add multisink support
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
    static final String AUDIO_THREADPOOL_NAME = "audio";

    private final Logger logger = LoggerFactory.getLogger(AudioManagerImpl.class);

    // service maps
    private final Map<String, AudioSource> audioSources = new ConcurrentHashMap<>();
    private final Map<String, AudioSink> audioSinks = new ConcurrentHashMap<>();

    private final ExecutorService pool = ThreadPoolManager.getPool(AUDIO_THREADPOOL_NAME);

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
            this.defaultSource = config.get(CONFIG_DEFAULT_SOURCE) instanceof Object source ? source.toString() : null;
            this.defaultSink = config.get(CONFIG_DEFAULT_SINK) instanceof Object sink ? sink.toString() : null;
        }
    }

    @Override
    public void play(@Nullable AudioStream audioStream) {
        playSingleSink(audioStream, null, null);
    }

    @Override
    public void play(@Nullable AudioStream audioStream, @Nullable String sinkId) {
        playSingleSink(audioStream, sinkId, null);
    }

    @Override
    public void play(@Nullable AudioStream audioStream, @NonNull Set<String> sinkIds) {
        playMultiSink(audioStream, sinkIds, null);
    }

    @Override
    public void play(@Nullable AudioStream audioStream, @Nullable String sinkId, @Nullable PercentType volume) {
        playSingleSink(audioStream, sinkId, volume);
    }

    @Override
    public void play(@Nullable AudioStream audioStream, Set<String> sinkIds, @Nullable PercentType volume) {
        playMultiSink(audioStream, sinkIds, volume);
    }

    protected void playSingleSink(@Nullable AudioStream audioStream, @Nullable String sinkId,
            @Nullable PercentType volume) {
        AudioSink sink = getSink(sinkId);
        if (sink == null) {
            logger.warn("No audio sink provided for playback.");
            return;
        }

        // Handle volume adjustment for the current sink
        Runnable restoreVolume = handleVolumeCommand(volume, sink);

        try {
            // Process and complete playback asynchronously
            sink.processAndComplete(audioStream).exceptionally(exception -> {
                logger.error("Error playing audio stream '{}' on sink '{}': {}", audioStream, sinkId,
                        exception.getMessage(), exception);
                return null; // Handle the exception gracefully
            }).thenRun(() -> {
                restoreVolume.run(); // Ensure volume is restored after playback completes
                logger.info("Audio stream '{}' has been successfully played on sink '{}'.", audioStream, sinkId);
            });
        } catch (Exception e) {
            logger.error("Unexpected error while processing audio stream '{}' on sink '{}': {}", audioStream, sinkId,
                    e.getMessage(), e);
        }
    }

    protected void playMultiSink(@Nullable AudioStream audioStream, Set<String> sinkIds, @Nullable PercentType volume) {
        if (sinkIds.isEmpty()) {
            logger.warn("No audio sinks provided for playback.");
            return;
        }

        // Create a list of CompletableFutures for parallel execution
        List<CompletableFuture<Object>> futures = sinkIds.stream().map(sinkId -> CompletableFuture.supplyAsync(() -> {
            AudioSink sink = getSink(sinkId);
            if (sink == null) {
                logger.warn("Sink '{}' not found. Skipping.", sinkId);
                return null; // Return null for missing sinks
            }

            // Handle volume adjustment for the current sink
            Runnable restoreVolume = handleVolumeCommand(volume, sink);

            try {
                // Play the audio stream synchronously on this sink
                sink.processAndComplete(audioStream);
                logger.debug("Audio stream '{}' has been played on sink '{}'.", audioStream, sinkId);
            } catch (Exception e) {
                logger.error("Error playing '{}' on sink '{}': {}", audioStream, sinkId, e.getMessage(), e);
            } finally {
                restoreVolume.run(); // Ensure volume is restored after playback completes
            }
            return null;
        }, pool)).collect(Collectors.toList());

        // Wait for all sinks to complete playback
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logger.info("Audio stream '{}' has been played on all sinks.", audioStream))
                .exceptionally(exception -> {
                    logger.error("Error completing playback on all sinks: {}", exception.getMessage(), exception);
                    return null;
                });
    }

    @Override
    public void playFile(String fileName) throws AudioException {
        playFileSingleSink(fileName, null, null);
    }

    @Override
    public void playFile(String fileName, @Nullable PercentType volume) throws AudioException {
        playFileSingleSink(fileName, null, volume);
    }

    @Override
    public void playFile(String fileName, @Nullable String sinkId) throws AudioException {
        playFileSingleSink(fileName, sinkId, null);
    }

    @Override
    public void playFile(String fileName, @NonNull Set<String> sinkIds) throws AudioException {
        playFileMultipleSink(fileName, sinkIds, null);
    }

    @Override
    public void playFile(String fileName, @Nullable String sinkId, @Nullable PercentType volume) throws AudioException {
        playFileSingleSink(fileName, sinkId, volume);
    }

    @Override
    public void playFile(String fileName, @NonNull Set<String> sinkIds, @Nullable PercentType volume)
            throws AudioException {
        playFileMultipleSink(fileName, sinkIds, volume);
    }

    protected void playFileSingleSink(String fileName, @Nullable String sinkId, @Nullable PercentType volume)
            throws AudioException {
        Objects.requireNonNull(fileName, "File cannot be played as fileName is null.");
        File file = Path.of(OpenHAB.getConfigFolder(), SOUND_DIR, fileName).toFile();
        FileAudioStream is = new FileAudioStream(file);
        play(is, sinkId, volume);
    }

    protected void playFileMultipleSink(String fileName, @NonNull Set<String> sinkIds, @Nullable PercentType volume)
            throws AudioException {
        Objects.requireNonNull(fileName, "File cannot be played as fileName is null.");
        File file = Path.of(OpenHAB.getConfigFolder(), SOUND_DIR, fileName).toFile();
        FileAudioStream is = new FileAudioStream(file);
        play(is, sinkIds, volume);
    }

    @Override
    public void stream(@Nullable String url) throws AudioException {
        streamSingleSink(url, null);
    }

    @Override
    public void stream(@Nullable String url, @Nullable String sinkId) throws AudioException {
        streamSingleSink(url, sinkId);
    }

    @Override
    public void stream(@Nullable String url, @NonNull Set<String> sinkIds) throws AudioException {
        streamMultipleSink(url, sinkIds);
    }

    protected void streamSingleSink(@Nullable String url, @Nullable String sinkId) throws AudioException {
        AudioStream audioStream = url != null ? new URLAudioStream(url) : null;
        play(audioStream, sinkId, null);
    }

    protected void streamMultipleSink(@Nullable String url, @NonNull Set<String> sinkIds) throws AudioException {
        AudioStream audioStream = url != null ? new URLAudioStream(url) : null;
        play(audioStream, sinkIds, null);
    }

    @Override
    public void playMelody(String melody) {
        playMelodySingleSink(melody, null, null);
    }

    @Override
    public void playMelody(String melody, @Nullable String sinkId) {
        playMelodySingleSink(melody, sinkId, null);
    }

    @Override
    public void playMelody(String melody, @NonNull Set<String> sinkIds) {
        playMelodyMultiSink(melody, sinkIds, null);
    }

    @Override
    public void playMelody(String melody, @Nullable String sinkId, @Nullable PercentType volume) {
        playMelodySingleSink(melody, sinkId, volume);
    }

    @Override
    public void playMelody(String melody, @NonNull Set<String> sinkIds, @Nullable PercentType volume) {
        playMelodyMultiSink(melody, sinkIds, volume);
    }

    protected void playMelodySingleSink(String melody, @Nullable String sinkId, @Nullable PercentType volume) {
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

    protected void playMelodyMultiSink(String melody, @NonNull Set<String> sinkIds, @Nullable PercentType volume) {

        if (sinkIds.isEmpty()) {
            logger.warn("Failed playing melody as no audio sinks were provided.");
            return;
        }

        // Create a list of CompletableFutures for parallel execution
        List<CompletableFuture<Object>> futures = sinkIds.stream().map(sinkId -> CompletableFuture.supplyAsync(() -> {
            AudioSink sink = getSink(sinkId);
            if (sink == null) {
                logger.warn("Sink '{}' not found. Skipping.", sinkId);
                return null;
            }

            var synthesizerFormat = AudioFormat.getBestMatch(ToneSynthesizer.getSupportedFormats(),
                    sink.getSupportedFormats());
            if (synthesizerFormat == null) {
                logger.warn("Sink '{}' does not support the required audio format. Skipping.", sinkId);
                return null;
            }

            try {

                // Generate the audio stream for the melody
                var audioStream = new ToneSynthesizer(synthesizerFormat).getStream(ToneSynthesizer.parseMelody(melody));

                // Play the melody on this sink asynchronously
                play(audioStream, sinkId, volume);
                logger.debug("Melody '{}' has been played on sink '{}'.", melody, sinkId);
            } catch (Exception e) {
                logger.warn("Error playing melody on sink '{}': {}", sinkId, e.getMessage(), e);
            }
            return null;
        }, pool)).collect(Collectors.toList());

        // Wait for all sinks to complete playback
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logger.info("Melody '{}' has been played on all sinks.", melody))
                .exceptionally(exception -> {
                    logger.error("Error completing playback on all sinks: {}", exception.getMessage(), exception);
                    return null;
                });
    }

    @Override
    public void record(int seconds, String filename, @Nullable String sourceId) throws AudioException {
        var audioSource = sourceId != null ? getSource(sourceId) : getSource();
        if (audioSource == null) {
            throw new AudioException("Audio source '" + (sourceId != null ? sourceId : "default") + "' not available");
        }
        var audioFormat = AudioFormat.getBestMatch(audioSource.getSupportedFormats(),
                Set.of(AudioFormat.PCM_SIGNED, AudioFormat.WAV));
        if (audioFormat == null) {
            throw new AudioException("Unable to find valid audio format");
        }
        javax.sound.sampled.AudioFormat jAudioFormat = new javax.sound.sampled.AudioFormat(
                Objects.requireNonNull(audioFormat.getFrequency()), Objects.requireNonNull(audioFormat.getBitDepth()),
                Objects.requireNonNull(audioFormat.getChannels()), true, false);
        int secondByteLength = ((int) jAudioFormat.getSampleRate() * jAudioFormat.getFrameSize());
        int targetByteLength = secondByteLength * seconds;
        ByteBuffer recordBuffer = ByteBuffer.allocate(targetByteLength);
        try (var audioStream = audioSource.getInputStream(audioFormat)) {
            if (audioFormat.isCompatible(AudioFormat.WAV)) {
                AudioWaveUtils.removeFMT(audioStream);
            }
            while (true) {
                try {
                    var bytes = audioStream.readNBytes(secondByteLength);
                    if (bytes.length == 0) {
                        logger.debug("End of input audio stream reached");
                        break;
                    }
                    if (recordBuffer.position() + bytes.length > recordBuffer.limit()) {
                        logger.debug("Recording limit reached");
                        break;
                    }
                    recordBuffer.put(bytes);
                } catch (IOException e) {
                    logger.warn("Reading audio data failed");
                }
            }
        } catch (IOException e) {
            logger.warn("IOException while reading audioStream: {}", e.getMessage());
        }
        String recordFilename = filename.endsWith(".wav") ? filename : filename + ".wav";
        logger.info("Saving record file: {}", recordFilename);
        byte[] audioBytes = new byte[recordBuffer.position()];
        logger.info("Saving bytes: {}", audioBytes.length);
        recordBuffer.rewind();
        recordBuffer.get(audioBytes);
        File recordFile = new File(
                OpenHAB.getConfigFolder() + File.separator + SOUND_DIR + File.separator + recordFilename);
        try (FileOutputStream fileOutputStream = new FileOutputStream(recordFile)) {
            AudioSystem.write(
                    new AudioInputStream(new ByteArrayInputStream(audioBytes), jAudioFormat,
                            (long) Math.ceil(((double) audioBytes.length) / jAudioFormat.getFrameSize())), //
                    AudioFileFormat.Type.WAVE, //
                    fileOutputStream //
            );
            fileOutputStream.flush();
        } catch (IOException e) {
            logger.warn("IOException while saving record file: {}", e.getMessage());
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

        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be empty.");
        }

        Set<String> matchedSinkIds = new HashSet<>();

        for (String segment : pattern.split(",")) {
            segment = segment.trim();

            if (segment.contains("*") || segment.contains("?")) {
                String regex = segment.replace("?", ".?").replace("*", ".*?");

                for (String sinkId : audioSinks.keySet()) {
                    if (sinkId.matches(regex)) {
                        matchedSinkIds.add(sinkId);
                    }
                }
            } else {
                matchedSinkIds.add(segment);
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
