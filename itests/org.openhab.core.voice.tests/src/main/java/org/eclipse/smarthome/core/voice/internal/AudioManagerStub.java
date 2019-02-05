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
package org.eclipse.smarthome.core.voice.internal;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.core.audio.AudioException;
import org.eclipse.smarthome.core.audio.AudioManager;
import org.eclipse.smarthome.core.audio.AudioSink;
import org.eclipse.smarthome.core.audio.AudioSource;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.library.types.PercentType;

/**
 * Only the get source and get sink methods are used in the tests.
 *
 * @author Velin Yordanov - Initial contribution
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Added methods for getting all sinks and sources
 */
public class AudioManagerStub implements AudioManager {
    private SinkStub sink = new SinkStub();
    private AudioSourceStub source = new AudioSourceStub();

    @Override
    public AudioSource getSource() {
        return source;
    }

    @Override
    public Set<AudioSource> getAllSources() {
        return Collections.emptySet();
    }

    @Override
    public AudioSink getSink() {
        return sink;
    }

    @Override
    public Set<AudioSink> getAllSinks() {
        return Collections.emptySet();
    }

    @Override
    public void play(AudioStream audioStream) {

    }

    @Override
    public void play(AudioStream audioStream, String sinkId) {

    }

    @Override
    public void play(AudioStream audioStream, String sinkId, PercentType volume) {

    }

    @Override
    public void playFile(String fileName) throws AudioException {

    }

    @Override
    public void playFile(String fileName, PercentType volume) throws AudioException {

    }

    @Override
    public void playFile(String fileName, String sinkId) throws AudioException {

    }

    @Override
    public void playFile(String fileName, String sinkId, PercentType volume) throws AudioException {

    }

    @Override
    public void stream(String url) throws AudioException {

    }

    @Override
    public void stream(String url, String sinkId) throws AudioException {

    }

    @Override
    public PercentType getVolume(String sinkId) {
        return PercentType.ZERO;
    }

    @Override
    public void setVolume(PercentType volume, String sinkId) {

    }

    @Override
    public Set<String> getSourceIds(String pattern) {
        return Collections.emptySet();
    }

    @Override
    public AudioSink getSink(String sinkId) {
        return getSink();
    }

    @Override
    public Set<String> getSinkIds(String pattern) {
        return Collections.emptySet();
    }

}
