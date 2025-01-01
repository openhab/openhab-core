/**
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
package org.openhab.core.audio.internal.javasound;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSinkAsync;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.PipedAudioStream;
import org.openhab.core.audio.URLAudioStream;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.library.types.PercentType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an audio sink that is registered as a service, which can play wave files to the hosts outputs (e.g. speaker,
 * line-out).
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 * @author Miguel Álvarez Díez - Added piped audio stream support
 *
 */
@NonNullByDefault
@Component(service = AudioSink.class, immediate = true)
public class JavaSoundAudioSink extends AudioSinkAsync {

    private final Logger logger = LoggerFactory.getLogger(JavaSoundAudioSink.class);

    private boolean isMac = false;
    private @Nullable PercentType macVolumeValue = null;
    private @Nullable static Player streamPlayer = null;

    private NamedThreadFactory threadFactory = new NamedThreadFactory("audio");

    private static final Set<AudioFormat> SUPPORTED_AUDIO_FORMATS = Set.of(AudioFormat.MP3, AudioFormat.WAV,
            AudioFormat.PCM_SIGNED);

    // we accept any stream
    private static final Set<Class<? extends AudioStream>> SUPPORTED_AUDIO_STREAMS = Set.of(AudioStream.class);

    @Activate
    protected void activate(BundleContext context) {
        String os = context.getProperty(Constants.FRAMEWORK_OS_NAME);
        if (os != null && os.toLowerCase().startsWith("macos")) {
            isMac = true;
        }
    }

    @Override
    public synchronized void processAsynchronously(final @Nullable AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException {
        if (audioStream instanceof PipedAudioStream pipedAudioStream
                && AudioFormat.PCM_SIGNED.isCompatible(pipedAudioStream.getFormat())) {
            pipedAudioStream.onClose(() -> playbackFinished(pipedAudioStream));
            AudioPlayer audioPlayer = new AudioPlayer(pipedAudioStream);
            audioPlayer.start();
            try {
                audioPlayer.join();
            } catch (InterruptedException e) {
                logger.debug("Audio stream has been interrupted.");
            }
        } else if (audioStream != null && !AudioFormat.CODEC_MP3.equals(audioStream.getFormat().getCodec())) {
            AudioPlayer audioPlayer = new AudioPlayer(audioStream);
            audioPlayer.start();
            try {
                audioPlayer.join();
                playbackFinished(audioStream);
            } catch (InterruptedException e) {
                logger.error("Playing audio has been interrupted.");
            }
        } else {
            if (audioStream == null || audioStream instanceof URLAudioStream) {
                // we are dealing with an infinite stream here
                if (streamPlayer instanceof Player player) {
                    // if we are already playing a stream, stop it first
                    player.close();
                    streamPlayer = null;
                }
                if (audioStream == null) {
                    // the call was only for stopping the currently playing stream
                    return;
                } else {
                    try {
                        // we start a new continuous stream and store its handle
                        playInThread(audioStream, true);
                    } catch (JavaLayerException e) {
                        logger.error("An exception occurred while playing url audio stream : '{}'", e.getMessage());
                    }
                    return;
                }
            } else {
                // we are playing some normal file (no url stream)
                try {
                    playInThread(audioStream, false);
                } catch (JavaLayerException e) {
                    logger.error("An exception occurred while playing audio : '{}'", e.getMessage());
                }
            }
        }
    }

    private void playInThread(final AudioStream audioStream, boolean store) throws JavaLayerException {
        // run in new thread
        Player streamPlayerFinal = new Player(audioStream);
        if (store) { // we store its handle in case we want to interrupt it.
            streamPlayer = streamPlayerFinal;
        }
        threadFactory.newThread(() -> {
            try {
                streamPlayerFinal.play();
            } catch (Exception e) {
                logger.error("An exception occurred while playing audio : '{}'", e.getMessage());
            } finally {
                streamPlayerFinal.close();
                playbackFinished(audioStream);
            }
        }).start();
    }

    protected synchronized void deactivate() {
        if (streamPlayer instanceof Player player) {
            // stop playing streams on shutdown
            player.close();
            streamPlayer = null;
        }
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return SUPPORTED_AUDIO_FORMATS;
    }

    @Override
    public Set<Class<? extends AudioStream>> getSupportedStreams() {
        return SUPPORTED_AUDIO_STREAMS;
    }

    @Override
    public String getId() {
        return "enhancedjavasound";
    }

    @Override
    public @Nullable String getLabel(@Nullable Locale locale) {
        return "System Speaker";
    }

    @Override
    public PercentType getVolume() throws IOException {
        if (!isMac) {
            final Float[] volumes = new Float[1];
            runVolumeCommand((FloatControl input) -> {
                FloatControl volumeControl = input;
                volumes[0] = volumeControl.getValue();
                return true;
            });
            if (volumes[0] != null) {
                return new PercentType(Math.round(volumes[0] * 100f));
            } else {
                logger.warn("Cannot determine master volume level - assuming 100%");
                return PercentType.HUNDRED;
            }
        } else {
            // we use a cache of the value as the script execution is pretty slow
            PercentType cachedVolume = macVolumeValue;
            if (cachedVolume == null) {
                Process p = Runtime.getRuntime()
                        .exec(new String[] { "osascript", "-e", "output volume of (get volume settings)" });
                String value;
                try (Scanner scanner = new Scanner(p.getInputStream(), StandardCharsets.UTF_8.name())) {
                    value = scanner.useDelimiter("\\A").next().strip();
                }
                try {
                    cachedVolume = new PercentType(value);
                    macVolumeValue = cachedVolume;
                } catch (NumberFormatException e) {
                    logger.warn("Cannot determine master volume level, received response '{}' - assuming 100%", value);
                    return PercentType.HUNDRED;
                }
            }
            return cachedVolume;
        }
    }

    @Override
    public void setVolume(final PercentType volume) throws IOException {
        if (volume.intValue() < 0 || volume.intValue() > 100) {
            throw new IllegalArgumentException("Volume value must be in the range [0,100]!");
        }
        if (!isMac) {
            runVolumeCommand((FloatControl input) -> {
                input.setValue(volume.floatValue() / 100f);
                return true;
            });
        } else {
            Runtime.getRuntime()
                    .exec(new String[] { "osascript", "-e", "set volume output volume " + volume.intValue() });
            macVolumeValue = volume;
        }
    }

    private void runVolumeCommand(Function<FloatControl, Boolean> closure) {
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : infos) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.isLineSupported(Port.Info.SPEAKER)) {
                Port port;
                try {
                    port = (Port) mixer.getLine(Port.Info.SPEAKER);
                    port.open();
                    if (port.isControlSupported(FloatControl.Type.VOLUME)) {
                        FloatControl volume = (FloatControl) port.getControl(FloatControl.Type.VOLUME);
                        closure.apply(volume);
                    }
                    port.close();
                } catch (LineUnavailableException e) {
                    logger.error("Cannot access master volume control", e);
                }
            }
        }
    }
}
