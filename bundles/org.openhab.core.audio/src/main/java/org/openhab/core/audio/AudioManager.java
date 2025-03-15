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
package org.openhab.core.audio;

import java.io.IOException;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.PercentType;

import io.reactivex.annotations.NonNull;

/**
 * This service provides functionality around audio services and is the central service to be used directly by others.
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - removed unwanted dependencies
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Added methods for getting all sinks and sources
 * @author Miguel Álvarez - Add record method
 * @author Karel Goderis - Add multisink support
 */
@NonNullByDefault
public interface AudioManager {

    /**
     * Name of the sub-directory of the config folder, holding sound files.
     */
    String SOUND_DIR = "sounds";

    /**
     * Plays the passed audio stream using the default audio sink.
     *
     * @param audioStream The audio stream to play or null if streaming should be stopped
     */
    void play(@Nullable AudioStream audioStream);

    /**
     * Plays the passed audio stream on the given sink.
     *
     * @param audioStream The audio stream to play or null if streaming should be stopped
     * @param sinkId The id of the audio sink to use or null for the default
     */
    void play(@Nullable AudioStream audioStream, @Nullable String sinkId);

    /**
     * Plays the passed audio stream on the given set of sinks.
     *
     * @param audioStream The audio stream to play or null if streaming should be stopped
     * @param sinkIds The set of the audio sink ids to use
     */
    void play(@Nullable AudioStream audioStream, @NonNull Set<String> sinkIds);

    /**
     * Plays the passed audio stream on the given sink.
     *
     * @param audioStream The audio stream to play or null if streaming should be stopped
     * @param sinkId The id of the audio sink to use or null for the default
     * @param volume The volume to be used or null if the default notification volume should be used
     */
    void play(@Nullable AudioStream audioStream, @Nullable String sinkId, @Nullable PercentType volume);

    /**
     * Plays the passed audio stream on the given set of sinks.
     *
     * @param audioStream The audio stream to play or null if streaming should be stopped
     * @param sinkIds The set of the audio sink ids to use
     * @param volume The volume to be used or null if the default notification volume should be used
     */
    void play(@Nullable AudioStream audioStream, Set<String> sinkIds, @Nullable PercentType volume);

    /**
     * Plays an audio file from the "sounds" folder using the default audio sink.
     *
     * @param fileName The file from the "sounds" folder
     * @throws AudioException in case the file does not exist or cannot be opened
     */
    void playFile(String fileName) throws AudioException;

    /**
     * Plays an audio file with the given volume from the "sounds" folder using the default audio sink.
     *
     * @param fileName The file from the "sounds" folder
     * @param volume The volume to be used or null if the default notification volume should be used
     * @throws AudioException in case the file does not exist or cannot be opened
     */
    void playFile(String fileName, @Nullable PercentType volume) throws AudioException;

    /**
     * Plays an audio file from the "sounds" folder using the given audio sink.
     *
     * @param fileName The file from the "sounds" folder
     * @param sinkId The id of the audio sink to use or null for the default
     * @throws AudioException in case the file does not exist or cannot be opened
     */
    void playFile(String fileName, @Nullable String sinkId) throws AudioException;

    /**
     * Plays an audio file from the "sounds" folder using the given audio sink.
     *
     * @param fileName The file from the "sounds" folder
     * @param sinkIds The set of the audio sink ids to use
     * @throws AudioException in case the file does not exist or cannot be opened
     */
    void playFile(String fileName, @NonNull Set<String> sinkIds) throws AudioException;

    /**
     * Plays an audio file with the given volume from the "sounds" folder using the given audio sink.
     *
     * @param fileName The file from the "sounds" folder
     * @param sinkId The id of the audio sink to use or null for the default
     * @param volume The volume to be used or null if the default notification volume should be used
     * @throws AudioException in case the file does not exist or cannot be opened
     */
    void playFile(String fileName, @Nullable String sinkId, @Nullable PercentType volume) throws AudioException;

    /**
     * Plays an audio file with the given volume from the "sounds" folder using the given audio sink.
     *
     * @param fileName The file from the "sounds" folder
     * @param sinkIds The set of the audio sink ids to use
     * @param volume The volume to be used or null if the default notification volume should be used
     * @throws AudioException in case the file does not exist or cannot be opened
     */
    void playFile(String fileName, @NonNull Set<String> sinkIds, @Nullable PercentType volume) throws AudioException;

    /**
     * Stream audio from the passed url using the default audio sink.
     *
     * @param url The url to stream from or null if streaming should be stopped
     * @throws AudioException in case the url stream cannot be opened
     */
    void stream(@Nullable String url) throws AudioException;

    /**
     * Stream audio from the passed url to the given sink
     *
     * @param url The url to stream from or null if streaming should be stopped
     * @param sinkId The id of the audio sink to use or null for the default
     * @throws AudioException in case the url stream cannot be opened
     */
    void stream(@Nullable String url, @Nullable String sinkId) throws AudioException;

    /**
     * Stream audio from the passed url to the given set of sinks
     *
     * @param url The url to stream from or null if streaming should be stopped
     * @param sinkIds The set of the audio sink ids to use
     * @throws AudioException in case the url stream cannot be opened
     */
    void stream(@Nullable String url, @NonNull Set<String> sinkIds) throws AudioException;

    /**
     * Parse and synthesize a melody and play it into the default sink.
     *
     * The melody should be a spaced separated list of note names or silences (character 0 or O).
     * You can optionally add the character "'" to increase the note one octave.
     * You can optionally add ":ms" where ms is an int value to customize the note/silence milliseconds duration
     * (defaults to 200ms).
     *
     * @param melody The url to stream from or null if streaming should be stopped.
     */
    void playMelody(String melody);

    /**
     * Parse and synthesize a melody and play it into the given sink.
     *
     * The melody should be a spaced separated list of note names or silences (character 0 or O).
     * You can optionally add the character "'" to increase the note one octave.
     * You can optionally add ":ms" where ms is an int value to customize the note/silence milliseconds duration
     * (defaults to 200ms).
     *
     * @param melody The url to stream from or null if streaming should be stopped.
     * @param sinkId The id of the audio sink to use or null for the default.
     */
    void playMelody(String melody, @Nullable String sinkId);

    /**
     * Parse and synthesize a melody and play it into the given sink.
     *
     * The melody should be a spaced separated list of note names or silences (character 0 or O).
     * You can optionally add the character "'" to increase the note one octave.
     * You can optionally add ":ms" where ms is an int value to customize the note/silence milliseconds duration
     * (defaults to 200ms).
     *
     * @param melody The url to stream from or null if streaming should be stopped.
     * @param sinkIds The set of the audio sink ids to use
     */
    void playMelody(String melody, @NonNull Set<String> sinkIds);

    /**
     * Parse and synthesize a melody and play it into the given sink at the desired volume.
     *
     * The melody should be a spaced separated list of note names or silences (character 0 or O).
     * You can optionally add the character "'" to increase the note one octave.
     * You can optionally add ":ms" where ms is an int value to customize the note/silence milliseconds duration
     * (defaults to 200ms).
     *
     * @param melody The url to stream from or null if streaming should be stopped.
     * @param sinkId The id of the audio sink to use or null for the default.
     * @param volume The volume to be used or null if the default notification volume should be used
     */
    void playMelody(String melody, @Nullable String sinkId, @Nullable PercentType volume);

    /**
     * Parse and synthesize a melody and play it into the given sink at the desired volume.
     *
     * The melody should be a spaced separated list of note names or silences (character 0 or O).
     * You can optionally add the character "'" to increase the note one octave.
     * You can optionally add ":ms" where ms is an int value to customize the note/silence milliseconds duration
     * (defaults to 200ms).
     *
     * @param melody The url to stream from or null if streaming should be stopped.
     * @param sinkIds The set of the audio sink ids to use
     * @param volume The volume to be used or null if the default notification volume should be used
     */
    void playMelody(String melody, @NonNull Set<String> sinkIds, @Nullable PercentType volume);

    /**
     * Record audio as a WAV file of the specified length to the sounds folder.
     *
     * @param seconds seconds to record.
     * @param filename record filename.
     * @param sourceId The id of the audio source to use or null for the default.
     */
    void record(int seconds, String filename, @Nullable String sourceId) throws AudioException;

    /**
     * Retrieves the current volume of a sink
     *
     * @param sinkId the sink to get the volume for or null for the default
     * @return the volume as a value between 0 and 100
     * @throws IOException if the sink is not able to determine the volume
     */
    PercentType getVolume(@Nullable String sinkId) throws IOException;

    /**
     * Sets the volume for a sink.
     *
     * @param volume the volume to set as a value between 0 and 100
     * @param sinkId the sink to set the volume for or null for the default
     * @throws IOException if the sink is not able to set the volume
     */
    void setVolume(PercentType volume, @Nullable String sinkId) throws IOException;

    /**
     * Retrieves the default AudioSource id if any.
     */
    @Nullable
    String getSourceId();

    /**
     * Retrieves an AudioSource.
     * If a default name is configured and the service available, this is returned. If no default name is configured,
     * the first available service is returned, if one exists. If no service with the default name is found, null is
     * returned.
     *
     * @return an AudioSource or null, if no service is available or if a default is configured, but no according
     *         service is found
     */
    @Nullable
    AudioSource getSource();

    /**
     * Retrieves all audio sources
     *
     * @return all audio sources
     */
    Set<AudioSource> getAllSources();

    /**
     * Retrieves the audio sources for a given id
     *
     * @param sourceId the id of the audio sources or null for the default
     * @return the audio sources for the id or the default audio sources
     */
    @Nullable
    AudioSource getSource(@Nullable String sourceId);

    /**
     * Get a list of source ids that match a given pattern
     *
     * @param pattern pattern to search, can include {@code *} and {@code ?} placeholders
     * @return ids of matching sources
     */
    Set<String> getSourceIds(String pattern);

    /**
     * Retrieves the default AudioSink id if any.
     */
    @Nullable
    String getSinkId();

    /**
     * Retrieves an AudioSink.
     * If a default name is configured and the service available, this is returned. If no default name is configured,
     * the first available service is returned, if one exists. If no service with the default name is found, null is
     * returned.
     *
     * @return an AudioSink or null, if no service is available or if a default is configured, but no according service
     *         is found
     */
    @Nullable
    AudioSink getSink();

    /**
     * Retrieves all audio sinks
     *
     * @return all audio sinks
     */
    Set<AudioSink> getAllSinks();

    /**
     * Retrieves the sink for a given id
     *
     * @param sinkId the id of the sink or null for the default
     * @return the sink instance for the id or the default sink
     */
    @Nullable
    AudioSink getSink(@Nullable String sinkId);

    /**
     * Get a list of sink ids that match a given set of patterns
     *
     * @param pattern patterns to search; patterns is a comma-separated list, whereby each can include {@code *} and
     *            {@code ?} placeholders, or can be literal string to designate a single sink
     * @return ids of matching sinks
     */
    Set<String> getSinkIds(String pattern);

    /**
     * Handles a volume command change and returns a Runnable to restore it.
     * Returning a Runnable allows us to have a no-op Runnable if changing volume back is not needed, and conveniently
     * keeping it as one liner usable in a chain for the caller.
     *
     * @param volume The volume to set
     * @param sink The sink to set the volume to
     * @return A runnable to restore the volume to its previous value, or no-operation if no change is required.
     */
    Runnable handleVolumeCommand(@Nullable PercentType volume, AudioSink sink);
}
