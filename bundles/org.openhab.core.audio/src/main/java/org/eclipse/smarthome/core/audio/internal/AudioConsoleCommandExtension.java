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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.smarthome.core.audio.AudioException;
import org.eclipse.smarthome.core.audio.AudioManager;
import org.eclipse.smarthome.core.audio.AudioSink;
import org.eclipse.smarthome.core.audio.AudioSource;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension for all audio features.
 *
 * @author Karel Goderis - Initial contribution and API
 * @author Kai Kreuzer - refactored to match AudioManager implementation
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Sort audio sink and source options
 */
@Component(service = ConsoleCommandExtension.class)
public class AudioConsoleCommandExtension extends AbstractConsoleCommandExtension {

    static final String SUBCMD_PLAY = "play";
    static final String SUBCMD_STREAM = "stream";
    static final String SUBCMD_SOURCES = "sources";
    static final String SUBCMD_SINKS = "sinks";

    private AudioManager audioManager;
    private LocaleProvider localeProvider;

    public AudioConsoleCommandExtension() {
        super("audio", "Commands around audio enablement features.");
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] {
                buildCommandUsage(SUBCMD_PLAY + " [<sink>] <filename>",
                        "plays a sound file from the sounds folder through the optionally specified audio sink(s)"),
                buildCommandUsage(SUBCMD_PLAY + " <sink> <filename> <volume>",
                        "plays a sound file from the sounds folder through the specified audio sink(s) with the specified volume"),
                buildCommandUsage(SUBCMD_STREAM + " [<sink>] <url>",
                        "streams the sound from the url through the optionally specified audio sink(s)"),
                buildCommandUsage(SUBCMD_SOURCES, "lists the audio sources"),
                buildCommandUsage(SUBCMD_SINKS, "lists the audio sinks") });

    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_PLAY:
                    if (args.length > 1) {
                        play((String[]) ArrayUtils.subarray(args, 1, args.length), console);
                    } else {
                        console.println(
                                "Specify file to play, and optionally the sink(s) to use (e.g. 'play javasound hello.mp3')");
                    }
                    return;
                case SUBCMD_STREAM:
                    if (args.length > 1) {
                        stream((String[]) ArrayUtils.subarray(args, 1, args.length), console);
                    } else {
                        console.println("Specify url to stream from, and optionally the sink(s) to use");
                    }
                    return;
                case SUBCMD_SOURCES:
                    listSources(console);
                    return;
                case SUBCMD_SINKS:
                    listSinks(console);
                    return;
                default:
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    private void listSources(Console console) {
        Set<AudioSource> sources = audioManager.getAllSources();
        if (sources.size() > 0) {
            AudioSource defaultSource = audioManager.getSource();
            Locale locale = localeProvider.getLocale();
            sources.stream().sorted(comparing(s -> s.getLabel(locale))).forEach(source -> {
                console.println(String.format("%s %s (%s)", source.equals(defaultSource) ? "*" : " ",
                        source.getLabel(locale), source.getId()));
            });
        } else {
            console.println("No audio sources found.");
        }
    }

    private void listSinks(Console console) {
        Set<AudioSink> sinks = audioManager.getAllSinks();
        if (sinks.size() > 0) {
            AudioSink defaultSink = audioManager.getSink();
            Locale locale = localeProvider.getLocale();
            sinks.stream().sorted(comparing(s -> s.getLabel(locale))).forEach(sink -> {
                console.println(String.format("%s %s (%s)", sink.equals(defaultSink) ? "*" : " ", sink.getLabel(locale),
                        sink.getId()));
            });
        } else {
            console.println("No audio sinks found.");
        }
    }

    private void play(String[] args, Console console) {
        switch (args.length) {
            case 1:
                playOnSink(null, args[0], null, console);
                break;
            case 2:
                playOnSinks(args[0], args[1], null, console);
                break;
            case 3:
                PercentType volume = null;
                try {
                    volume = PercentType.valueOf(args[2]);
                } catch (Exception e) {
                    console.println(e.getMessage());
                    break;
                }
                playOnSinks(args[0], args[1], volume, console);
                break;
            default:
                break;
        }

    }

    private void playOnSinks(String pattern, String fileName, PercentType volume, Console console) {
        for (String sinkId : audioManager.getSinkIds(pattern)) {
            playOnSink(sinkId, fileName, volume, console);
        }
    }

    private void playOnSink(String sinkId, String fileName, PercentType volume, Console console) {
        try {
            audioManager.playFile(fileName, sinkId, volume);
        } catch (AudioException e) {
            console.println(e.getMessage());
        }
    }

    private void stream(String[] args, Console console) {
        switch (args.length) {
            case 1:
                streamOnSink(null, args[0], console);
                break;
            case 2:
                streamOnSinks(args[0], args[1], console);
                break;
            default:
                break;
        }
    }

    private void streamOnSinks(String pattern, String url, Console console) {
        for (String sinkId : audioManager.getSinkIds(pattern)) {
            streamOnSink(sinkId, url, console);
        }
    }

    private void streamOnSink(String sinkId, String url, Console console) {
        try {
            audioManager.stream(url, sinkId);
        } catch (AudioException e) {
            console.println(e.getMessage());
        }
    }

    @Reference
    protected void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    protected void unsetAudioManager(AudioManager audioManager) {
        this.audioManager = null;
    }

    @Reference
    protected void setLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    protected void unsetLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = null;
    }

}
