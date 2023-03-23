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
package org.openhab.core.voice;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;

/**
 * This service provides functionality around voice services and is the central service to be used directly by others.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Laurent Garnier - Updated methods startDialog and added method stopDialog
 * @author Miguel Álvarez - New dialog methods using DialogContext
 */
@NonNullByDefault
public interface VoiceManager {

    /**
     * Speaks the passed string using the default TTS service and default audio sink.
     *
     * @param text The text to say
     */
    void say(String text);

    /**
     * Speaks the passed string with the given volume using the default TTS service and default audio sink.
     *
     * @param text The text to say
     * @param volume The volume to be used or null if the default notification volume should be used
     */
    void say(String text, @Nullable PercentType volume);

    /**
     * Speaks the passed string using the provided voiceId and the default audio sink.
     * If the voiceId is fully qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     * voiceId is assumed to be available on the default TTS service.
     *
     * @param text The text to say
     * @param voiceId The id of the voice to use (either with or without prefix)
     */
    void say(String text, String voiceId);

    /**
     * Speaks the passed string with the given volume using the provided voiceId and the default audio sink.
     * If the voiceId is fully qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     * voiceId is assumed to be available on the default TTS service.
     *
     * @param text The text to say
     * @param voiceId The id of the voice to use (either with or without prefix) or null
     * @param volume The volume to be used or null if the default notification volume should be used
     */
    void say(String text, @Nullable String voiceId, @Nullable PercentType volume);

    /**
     * Speaks the passed string using the provided voiceId and the given audio sink.
     * If the voiceId is fully qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     * voiceId is assumed to be available on the default TTS service.
     *
     * @param text The text to say
     * @param voiceId The id of the voice to use (either with or without prefix) or null
     * @param sinkId The id of the audio sink to use or null
     */
    void say(String text, @Nullable String voiceId, @Nullable String sinkId);

    /**
     * Speaks the passed string with the given volume using the provided voiceId and the given audio sink.
     * If the voiceId is fully qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     * voiceId is assumed to be available on the default TTS service.
     *
     * @param text The text to say
     * @param voiceId The id of the voice to use (either with or without prefix) or null
     * @param sinkId The id of the audio sink to use or null
     * @param volume The volume to be used or null if the default notification volume should be used
     */
    void say(String text, @Nullable String voiceId, @Nullable String sinkId, @Nullable PercentType volume);

    /**
     * Interprets the passed string using the default services for HLI and locale.
     *
     * @param text The text to interpret
     * @throws InterpretationException
     * @return a human language response
     */
    String interpret(String text) throws InterpretationException;

    /**
     * Interprets the passed string using a particular HLI service and the default locale.
     *
     * @param text The text to interpret
     * @param hliIdList Comma separated list of HLI service ids to use or null
     * @throws InterpretationException
     * @return a human language response
     */
    String interpret(String text, @Nullable String hliIdList) throws InterpretationException;

    /**
     * Determines the preferred voice for the currently set locale
     *
     * @param voices a set of voices to chose from
     * @return the preferred voice for the current locale, or null if no voice can be found
     */
    @Nullable
    Voice getPreferredVoice(Set<Voice> voices);

    /**
     * Returns a dialog context builder with the default required services and configurations for dialog processing
     *
     * @throws IllegalStateException if some required service is not available
     */
    DialogContext.Builder getDialogContextBuilder();

    /**
     * Returns an object with the services and configurations last used for dialog processing if any.
     * The underling value is updated before an active dialog starts to interpret the transcription result.
     */
    @Nullable
    DialogContext getLastDialogContext();

    /**
     * Starts an infinite dialog sequence using all default services: keyword spotting on the default audio source,
     * audio source listening to retrieve a question or a command (default Speech to Text service), interpretation and
     * handling of the command, and finally playback of the answer on the default audio sink (default Text to Speech
     * service).
     *
     * Only one dialog can be started for the default audio source.
     *
     * @throws IllegalStateException if required services are not all available or the default locale is not supported
     *             by all these services or a dialog is already started for the default audio source
     */
    @Deprecated
    void startDialog() throws IllegalStateException;

    /**
     * Starts an infinite dialog sequence: keyword spotting on the audio source, audio source listening to retrieve
     * a question or a command (Speech to Text service), interpretation and handling of the command, and finally
     * playback of the answer on the audio sink (Text to Speech service).
     *
     * Only one dialog can be started for an audio source.
     *
     * @param ks the keyword spotting service to use or null to use the default service
     * @param stt the speech-to-text service to use or null to use the default service
     * @param tts the text-to-speech service to use or null to use the default service
     * @param hli the human language text interpreter to use or null to use the default service
     * @param source the audio source to use or null to use the default source
     * @param sink the audio sink to use or null to use the default sink
     * @param locale the locale to use or null to use the default locale
     * @param keyword the keyword to use during keyword spotting or null to use the default keyword
     * @param listeningItem the item to switch ON while listening to a question
     * @throws IllegalStateException if required services are not all available or the provided locale is not supported
     *             by all these services or a dialog is already started for this audio source
     */
    @Deprecated
    void startDialog(@Nullable KSService ks, @Nullable STTService stt, @Nullable TTSService tts,
            @Nullable HumanLanguageInterpreter hli, @Nullable AudioSource source, @Nullable AudioSink sink,
            @Nullable Locale locale, @Nullable String keyword, @Nullable String listeningItem)
            throws IllegalStateException;

    /**
     * Starts an infinite dialog sequence: keyword spotting on the audio source, audio source listening to retrieve
     * a question or a command (Speech to Text service), interpretation and handling of the command, and finally
     * playback of the answer on the audio sink (Text to Speech service).
     *
     * Only one dialog can be started for an audio source.
     *
     * @param ks the keyword spotting service to use or null to use the default service
     * @param stt the speech-to-text service to use or null to use the default service
     * @param tts the text-to-speech service to use or null to use the default service
     * @param voice the voice to use or null to use the default voice or any voice provided by the text-to-speech
     *            service matching the locale
     * @param hlis list of human language text interpreters to use, they are executed in order until the first
     *            successful response, or empty to use the default service
     * @param source the audio source to use or null to use the default source
     * @param sink the audio sink to use or null to use the default sink
     * @param locale the locale to use or null to use the default locale
     * @param keyword the keyword to use during keyword spotting or null to use the default keyword
     * @param listeningItem the item to switch ON while listening to a question
     * @throws IllegalStateException if required services are not all available or the provided locale is not supported
     *             by all these services or a dialog is already started for this audio source
     */
    @Deprecated
    void startDialog(@Nullable KSService ks, @Nullable STTService stt, @Nullable TTSService tts, @Nullable Voice voice,
            List<HumanLanguageInterpreter> hlis, @Nullable AudioSource source, @Nullable AudioSink sink,
            @Nullable Locale locale, @Nullable String keyword, @Nullable String listeningItem)
            throws IllegalStateException;

    /**
     * Starts an infinite dialog sequence: keyword spotting on the audio source, audio source listening to retrieve
     * a question or a command (Speech to Text service), interpretation and handling of the command, and finally
     * playback of the answer on the audio sink (Text to Speech service).
     *
     * Only one dialog can be started for an audio source.
     *
     * @param context with the configured services and options for the dialog
     * @throws IllegalStateException if required services are not compatible or the provided locale is not supported
     *             by all these services or a dialog is already started for this audio source
     */
    void startDialog(DialogContext context) throws IllegalStateException;

    /**
     * Stop the dialog associated to an audio source
     *
     * @param source the audio source or null to consider the default audio source
     * @throws IllegalStateException if no dialog is started for the audio source
     */
    void stopDialog(@Nullable AudioSource source) throws IllegalStateException;

    /**
     * Stop the dialog associated to the audio source
     *
     * @param context with the configured services and options for the dialog
     * @throws IllegalStateException if no dialog is started for the audio source
     */
    void stopDialog(DialogContext context) throws IllegalStateException;

    /**
     * Executes a simple dialog sequence without keyword spotting using all default services: default audio source
     * listening to retrieve a question or a command (default Speech to Text service), interpretation and handling of
     * the command, and finally playback of the answer on the default audio sink (default Text to Speech service).
     *
     * Only possible if no dialog processor is already started for the default audio source.
     *
     * @throws IllegalStateException if required services are not all available or the provided default locale is not
     *             supported by all these services or a dialog is already started for the default audio source
     */
    @Deprecated
    void listenAndAnswer() throws IllegalStateException;

    /**
     * Executes a simple dialog sequence without keyword spotting: audio source listening to retrieve a question or a
     * command (Speech to Text service), interpretation and handling of the command, and finally playback of the
     * answer on the audio sink (Text to Speech service).
     *
     * Only possible if no dialog processor is already started for the audio source.
     *
     * @param stt the speech-to-text service to use or null to use the default service
     * @param tts the text-to-speech service to use or null to use the default service
     * @param voice the voice to use or null to use the default voice or any voice provided by the text-to-speech
     *            service matching the locale
     * @param hlis list of human language text interpreters to use, they are executed in order until the first
     *            successful response, or empty to use the default service
     * @param source the audio source to use or null to use the default source
     * @param sink the audio sink to use or null to use the default sink
     * @param locale the locale to use or null to use the default locale
     * @param listeningItem the item to switch ON while listening to a question
     * @throws IllegalStateException if required services are not all available or the provided locale is not supported
     *             by all these services or a dialog is already started for this audio source
     */
    @Deprecated
    void listenAndAnswer(@Nullable STTService stt, @Nullable TTSService tts, @Nullable Voice voice,
            List<HumanLanguageInterpreter> hlis, @Nullable AudioSource source, @Nullable AudioSink sink,
            @Nullable Locale locale, @Nullable String listeningItem) throws IllegalStateException;

    /**
     * Executes a simple dialog sequence without keyword spotting: audio source listening to retrieve a question or a
     * command (Speech to Text service), interpretation and handling of the command, and finally playback of the
     * answer on the audio sink (Text to Speech service).
     *
     * Only possible if no dialog processor is already started for the audio source.
     *
     * @param context with the configured services and options for the dialog
     * @throws IllegalStateException the provided locale is not supported by all these services or a dialog is already
     *             started for this audio source
     */
    void listenAndAnswer(DialogContext context) throws IllegalStateException;

    /**
     * Register a dialog, so it will be persisted and started any time the required services are available.
     *
     * Only one registration can be done for an audio source.
     *
     * @param registration with the desired services ids and options for the dialog
     *
     * @throws IllegalStateException if there is another registration for the same source
     */
    void registerDialog(DialogRegistration registration) throws IllegalStateException;

    /**
     * Removes a dialog registration and stops the associate dialog.
     *
     * @param registration with the desired services ids and options for the dialog
     */
    void unregisterDialog(DialogRegistration registration);

    /**
     * Removes a dialog registration and stops the associate dialog.
     *
     * @param sourceId the registration audio source id.
     */
    void unregisterDialog(String sourceId);

    /**
     * List current dialog registrations
     *
     * @return a list of {@link DialogRegistration}
     */
    List<DialogRegistration> getDialogRegistrations();

    /**
     * Retrieves a TTS service.
     * If a default name is configured and the service available, this is returned. Otherwise, the first available
     * service is returned.
     *
     * @return a TTS service or null, if no service is available or if a default is configured, but no according service
     *         is found
     */
    @Nullable
    TTSService getTTS();

    /**
     * Retrieves a TTS service with the given id.
     *
     * @param id the id of the TTS service
     * @return a TTS service or null, if no service with this id exists
     */
    @Nullable
    TTSService getTTS(@Nullable String id);

    /**
     * Retrieves all TTS services.
     *
     * @return a collection of TTS services
     */
    Collection<TTSService> getTTSs();

    /**
     * Retrieves a STT service.
     * If a default name is configured and the service available, this is returned. Otherwise, the first available
     * service is returned.
     *
     * @return a STT service or null, if no service is available or if a default is configured, but no according service
     *         is found
     */
    @Nullable
    STTService getSTT();

    /**
     * Retrieves a STT service with the given id.
     *
     * @param id the id of the STT service
     * @return a STT service or null, if no service with this id exists
     */
    @Nullable
    STTService getSTT(@Nullable String id);

    /**
     * Retrieves all STT services.
     *
     * @return a collection of STT services
     */
    Collection<STTService> getSTTs();

    /**
     * Retrieves a KS service.
     * If a default name is configured and the service available, this is returned. Otherwise, the first available
     * service is returned.
     *
     * @return a KS service or null, if no service is available or if a default is configured, but no according service
     *         is found
     */
    @Nullable
    KSService getKS();

    /**
     * Retrieves a KS service with the given id.
     *
     * @param id the id of the KS service
     * @return a KS service or null, if no service with this id exists
     */
    @Nullable
    KSService getKS(@Nullable String id);

    /**
     * Retrieves all KS services.
     *
     * @return a collection of KS services
     */
    Collection<KSService> getKSs();

    /**
     * Retrieves a HumanLanguageInterpreter collection.
     * If no services are available returns an empty list.
     *
     * @param ids Comma separated list of HLI service ids to use
     * @return a List<HumanLanguageInterpreter> or empty, if none of the services is available
     */
    List<HumanLanguageInterpreter> getHLIsByIds(@Nullable String ids);

    /**
     * Retrieves a HumanLanguageInterpreter collection.
     * If no services are available returns an empty list.
     *
     * @param ids List of HLI service ids to use or null
     * @return a List<HumanLanguageInterpreter> or empty, if none of the services is available
     */
    List<HumanLanguageInterpreter> getHLIsByIds(List<String> ids);

    /**
     * Retrieves a HumanLanguageInterpreter.
     * If a default name is configured and the service available, this is returned. Otherwise, the first available
     * service is returned.
     *
     * @return a HumanLanguageInterpreter or null, if no service is available or if a default is configured, but no
     *         according service is found
     */
    @Nullable
    HumanLanguageInterpreter getHLI();

    /**
     * Retrieves a HumanLanguageInterpreter with the given id.
     *
     * @param id the id of the HumanLanguageInterpreter
     * @return a HumanLanguageInterpreter or null, if no interpreter with this id exists
     */
    @Nullable
    HumanLanguageInterpreter getHLI(@Nullable String id);

    /**
     * Retrieves all HumanLanguageInterpreters.
     *
     * @return a collection of HumanLanguageInterpreters
     */
    Collection<HumanLanguageInterpreter> getHLIs();

    /**
     * Returns a sorted set of all available voices in the system from all TTS services.
     *
     * The voices in the set are sorted by:
     * <ol>
     * <li>Voice TTSService label (localized with the default locale)
     * <li>Voice locale display name (localized with the default locale)
     * <li>Voice label
     * </ol>
     *
     * @return a sorted set of available voices
     */
    Set<Voice> getAllVoices();

    /**
     * Returns the default voice used for TTS.
     *
     * @return the default voice or null, if no default voice is configured or if a default is configured, but no
     *         according service is found
     */
    @Nullable
    Voice getDefaultVoice();
}
