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
package org.eclipse.smarthome.model.script.actions;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.voice.text.InterpretationException;
import org.eclipse.smarthome.model.script.engine.action.ActionDoc;
import org.eclipse.smarthome.model.script.engine.action.ParamDoc;
import org.eclipse.smarthome.model.script.internal.engine.action.VoiceActionService;

/**
 * The static methods of this class are made available as functions in the scripts.
 * This allows a script to use voice features.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 */
public class Voice {

    /**
     * Says the given text.
     *
     * This method uses the default voice and the default audio sink to play the audio.
     *
     * @param text The text to speak
     */
    @ActionDoc(text = "says a given text with the default voice")
    public static void say(@ParamDoc(name = "text") Object text) {
        say(text, null, null, null);
    }

    /**
     * Says the given text with the given volume.
     *
     * This method uses the default voice and the default audio sink to play the audio.
     *
     * @param text The text to speak
     * @param volume The volume to be used
     */
    @ActionDoc(text = "says a given text with the default voice and the given volume")
    public static void say(@ParamDoc(name = "text") Object text,
            @ParamDoc(name = "volume", text = "the volume to be used") PercentType volume) {
        say(text, null, null, volume);
    }

    /**
     * Says the given text with a given voice.
     *
     * This method uses the default audio sink to play the audio.
     *
     * @param text The text to speak
     * @param voice The name of the voice to use or null, if the default voice should be used. If the voiceId is fully
     *            qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     *            voiceId is assumed to be available on the default TTS service.
     */
    @ActionDoc(text = "says a given text with a given voice")
    public static void say(@ParamDoc(name = "text") Object text, @ParamDoc(name = "voice") String voice) {
        say(text, voice, null, null);
    }

    /**
     * Says the given text with a given voice and the given volume.
     *
     * This method uses the default audio sink to play the audio.
     *
     * @param text The text to speak
     * @param voice The name of the voice to use or null, if the default voice should be used. If the voiceId is fully
     *            qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     *            voiceId is assumed to be available on the default TTS service.
     * @param volume The volume to be used
     */
    @ActionDoc(text = "says a given text with a given voice and the given volume")
    public static void say(@ParamDoc(name = "text") Object text, @ParamDoc(name = "voice") String voice,
            @ParamDoc(name = "volume", text = "the volume to be used") PercentType volume) {
        say(text, voice, null, volume);
    }

    /**
     * Says the given text with a given voice through the given sink.
     *
     * @param text The text to speak
     * @param voice The name of the voice to use or null, if the default voice should be used. If the voiceId is fully
     *            qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     *            voiceId is assumed to be available on the default TTS service.
     * @param sink The name of audio sink to be used to play the audio or null, if the default sink should
     *            be used
     */
    @ActionDoc(text = "says a given text with a given voice through the given sink")
    public static void say(@ParamDoc(name = "text") Object text, @ParamDoc(name = "voice") String voice,
            @ParamDoc(name = "sink") String sink) {
        say(text, voice, sink, null);
    }

    /**
     * Says the given text with a given voice and the given volume through the given sink.
     *
     * @param text The text to speak
     * @param voice The name of the voice to use or null, if the default voice should be used. If the voiceId is fully
     *            qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     *            voiceId is assumed to be available on the default TTS service.
     * @param sink The name of audio sink to be used to play the audio or null, if the default sink should
     *            be used
     * @param volume The volume to be used
     */
    @ActionDoc(text = "says a given text with a given voice and the given volume through the given sink")
    public static void say(@ParamDoc(name = "text") Object text, @ParamDoc(name = "voice") String voice,
            @ParamDoc(name = "sink") String sink,
            @ParamDoc(name = "volume", text = "the volume to be used") PercentType volume) {
        if (StringUtils.isNotBlank(text.toString())) {
            VoiceActionService.voiceManager.say(text.toString(), voice, sink, volume);
        }
    }

    /**
     * Interprets the given text.
     *
     * This method uses the default Human Language Interpreter and passes the text to it.
     * In case of interpretation error, the error message is played using the default audio sink.
     *
     * @param text The text to interpret
     */
    @ActionDoc(text = "interprets a given text by the default human language interpreter", returns = "human language response")
    public static String interpret(@ParamDoc(name = "text") Object text) {
        return interpret(text, null);
    }

    /**
     * Interprets the given text with a given Human Language Interpreter.
     *
     * In case of interpretation error, the error message is played using the default audio sink.
     *
     * @param text The text to interpret
     * @param interpreter The Human Language Interpreter to be used
     */
    @ActionDoc(text = "interprets a given text by a given human language interpreter", returns = "human language response")
    public static String interpret(@ParamDoc(name = "text") Object text,
            @ParamDoc(name = "interpreter") String interpreter) {
        String response;
        try {
            response = VoiceActionService.voiceManager.interpret(text.toString(), interpreter);
        } catch (InterpretationException e) {
            say(e.getMessage());
            response = e.getMessage();
        }
        return response;
    }

    /**
     * Interprets the given text with a given Human Language Interpreter.
     *
     * In case of interpretation error, the error message is played using the given audio sink.
     * If sink parameter is null, the error message is simply not played.
     *
     * @param text The text to interpret
     * @param interpreter The Human Language Interpreter to be used
     * @param sink The name of audio sink to be used to play the error message
     */
    @ActionDoc(text = "interprets a given text by a given human language interpreter", returns = "human language response")
    public static String interpret(@ParamDoc(name = "text") Object text,
            @ParamDoc(name = "interpreter") String interpreter, @ParamDoc(name = "sink") String sink) {
        String response;
        try {
            response = VoiceActionService.voiceManager.interpret(text.toString(), interpreter);
        } catch (InterpretationException e) {
            if (sink != null) {
                say(e.getMessage(), null, sink);
            }
            response = e.getMessage();
        }
        return response;
    }

}
