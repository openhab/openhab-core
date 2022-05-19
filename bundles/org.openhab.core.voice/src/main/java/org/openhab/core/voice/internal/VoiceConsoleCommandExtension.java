/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.voice.internal;

import static java.util.Comparator.comparing;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.voice.KSService;
import org.openhab.core.voice.STTService;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension for all voice features.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Wouter Born - Sort TTS voices
 * @author Laurent Garnier - Added sub-commands startdialog and stopdialog
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class VoiceConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_SAY = "say";
    private static final String SUBCMD_INTERPRET = "interpret";
    private static final String SUBCMD_VOICES = "voices";
    private static final String SUBCMD_START_DIALOG = "startdialog";
    private static final String SUBCMD_STOP_DIALOG = "stopdialog";
    private static final String SUBCMD_LISTEN_ANSWER = "listenandanswer";
    private static final String SUBCMD_INTERPRETERS = "interpreters";
    private static final String SUBCMD_KEYWORD_SPOTTERS = "keywordspotters";
    private static final String SUBCMD_STT_SERVICES = "sttservices";
    private static final String SUBCMD_TTS_SERVICES = "ttsservices";

    private final ItemRegistry itemRegistry;
    private final VoiceManager voiceManager;
    private final AudioManager audioManager;
    private final LocaleProvider localeProvider;

    @Activate
    public VoiceConsoleCommandExtension(final @Reference VoiceManager voiceManager,
            final @Reference AudioManager audioManager, final @Reference LocaleProvider localeProvider,
            final @Reference ItemRegistry itemRegistry) {
        super("voice", "Commands around voice enablement features.");
        this.voiceManager = voiceManager;
        this.audioManager = audioManager;
        this.localeProvider = localeProvider;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public List<String> getUsages() {
        return List.of(buildCommandUsage(SUBCMD_SAY + " <text>", "speaks a text"),
                buildCommandUsage(SUBCMD_INTERPRET + " <command>", "interprets a human language command"),
                buildCommandUsage(SUBCMD_VOICES, "lists available voices of the TTS services"),
                buildCommandUsage(
                        SUBCMD_START_DIALOG
                                + " [<source> [<sink> [<interpreters> [<tts> [<stt> [<ks> {<voice> [<keyword>]]]]]]]]",
                        "start a new dialog processing using the default services or the services identified with provided arguments"),
                buildCommandUsage(SUBCMD_STOP_DIALOG + " [<source>]",
                        "stop the dialog processing for the default audio source or the audio source identified with provided argument"),
                buildCommandUsage(
                        SUBCMD_LISTEN_ANSWER + " [<source> [<sink> [<interpreters> [<tts> [<stt> [<voice]]]]]]",
                        "Execute a simple dialog sequence without keyword spotting using the default services or the services identified with provided arguments"),
                buildCommandUsage(SUBCMD_INTERPRETERS, "lists the interpreters"),
                buildCommandUsage(SUBCMD_KEYWORD_SPOTTERS, "lists the keyword spotters"),
                buildCommandUsage(SUBCMD_STT_SERVICES, "lists the Speech-to-Text services"),
                buildCommandUsage(SUBCMD_TTS_SERVICES, "lists the Text-to-Speech services"));
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_SAY:
                    if (args.length > 1) {
                        say(Arrays.copyOfRange(args, 1, args.length), console);
                    } else {
                        console.println("Specify text to say (e.g. 'say hello')");
                    }
                    return;
                case SUBCMD_INTERPRET:
                    if (args.length > 1) {
                        interpret(Arrays.copyOfRange(args, 1, args.length), console);
                    } else {
                        console.println("Specify text to interpret (e.g. 'interpret turn all lights off')");
                    }
                    return;
                case SUBCMD_VOICES:
                    Locale locale = localeProvider.getLocale();
                    Voice defaultVoice = getDefaultVoice();
                    for (Voice voice : voiceManager.getAllVoices()) {
                        TTSService ttsService = voiceManager.getTTS(voice.getUID().split(":")[0]);
                        if (ttsService != null) {
                            console.println(String.format("%s %s - %s - %s (%s)",
                                    voice.equals(defaultVoice) ? "*" : " ", ttsService.getLabel(locale),
                                    voice.getLocale().getDisplayName(locale), voice.getLabel(), voice.getUID()));
                        }
                    }
                    return;
                case SUBCMD_START_DIALOG:
                    try {
                        AudioSource source = args.length < 2 ? null : audioManager.getSource(args[1]);
                        AudioSink sink = args.length < 3 ? null : audioManager.getSink(args[2]);
                        List<HumanLanguageInterpreter> hlis = args.length < 4 ? List.of()
                                : voiceManager.getHLIsByIds(args[3]);
                        TTSService tts = args.length < 5 ? null : voiceManager.getTTS(args[4]);
                        STTService stt = args.length < 6 ? null : voiceManager.getSTT(args[5]);
                        KSService ks = args.length < 7 ? null : voiceManager.getKS(args[6]);
                        Voice voice = args.length < 8 ? null : getVoice(args[7]);
                        String keyword = args.length < 9 ? null : args[8];
                        voiceManager.startDialog(ks, stt, tts, voice, hlis, source, sink, null, keyword, null);
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while starting the dialog"));
                    }
                    break;
                case SUBCMD_STOP_DIALOG:
                    try {
                        voiceManager.stopDialog(args.length < 2 ? null : audioManager.getSource(args[1]));
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while stopping the dialog"));
                    }
                    break;
                case SUBCMD_LISTEN_ANSWER:
                    try {
                        AudioSource source = args.length < 2 ? null : audioManager.getSource(args[1]);
                        AudioSink sink = args.length < 3 ? null : audioManager.getSink(args[2]);
                        List<HumanLanguageInterpreter> hlis = args.length < 4 ? List.of()
                                : voiceManager.getHLIsByIds(args[3]);
                        TTSService tts = args.length < 5 ? null : voiceManager.getTTS(args[4]);
                        STTService stt = args.length < 6 ? null : voiceManager.getSTT(args[5]);
                        Voice voice = args.length < 7 ? null : getVoice(args[6]);
                        voiceManager.listenAndAnswer(stt, tts, voice, hlis, source, sink, null, null);
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while executing the simple dialog sequence"));
                    }
                    break;
                case SUBCMD_INTERPRETERS:
                    listInterpreters(console);
                    return;
                case SUBCMD_KEYWORD_SPOTTERS:
                    listKeywordSpotters(console);
                    return;
                case SUBCMD_STT_SERVICES:
                    listSTTs(console);
                    return;
                case SUBCMD_TTS_SERVICES:
                    listTTSs(console);
                    return;
                default:
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    private @Nullable Voice getDefaultVoice() {
        Voice defaultVoice = voiceManager.getDefaultVoice();
        if (defaultVoice == null) {
            TTSService tts = voiceManager.getTTS();
            if (tts != null) {
                return voiceManager.getPreferredVoice(tts.getAvailableVoices());
            }
        }
        return defaultVoice;
    }

    private void interpret(String[] args, Console console) {
        StringBuilder sb = new StringBuilder(args[0]);
        for (int i = 1; i < args.length; i++) {
            sb.append(" ");
            sb.append(args[i]);
        }
        String msg = sb.toString();
        try {
            String result = voiceManager.interpret(msg);
            if (result != null) {
                console.println(result);
            }
        } catch (InterpretationException ie) {
            console.println(Objects.requireNonNullElse(ie.getMessage(),
                    String.format("An error occurred while interpreting '%s'", msg)));
        }
    }

    private void say(String[] args, Console console) {
        StringBuilder msg = new StringBuilder();
        for (String word : args) {
            if (word.startsWith("%") && word.endsWith("%") && word.length() > 2) {
                String itemName = word.substring(1, word.length() - 1);
                try {
                    Item item = this.itemRegistry.getItemByPattern(itemName);
                    msg.append(item.getState().toString());
                } catch (ItemNotFoundException e) {
                    console.println("Error: Item '" + itemName + "' does not exist.");
                } catch (ItemNotUniqueException e) {
                    console.print("Error: Multiple items match this pattern: ");
                    for (Item item : e.getMatchingItems()) {
                        console.print(item.getName() + " ");
                    }
                }
            } else {
                msg.append(word);
            }
            msg.append(" ");
        }
        voiceManager.say(msg.toString());
    }

    private void listInterpreters(Console console) {
        Collection<HumanLanguageInterpreter> interpreters = voiceManager.getHLIs();
        if (!interpreters.isEmpty()) {
            HumanLanguageInterpreter defaultHLI = voiceManager.getHLI();
            Locale locale = localeProvider.getLocale();
            interpreters.stream().sorted(comparing(s -> s.getLabel(locale))).forEach(hli -> {
                console.println(String.format("%s %s (%s)", hli.equals(defaultHLI) ? "*" : " ", hli.getLabel(locale),
                        hli.getId()));
            });
        } else {
            console.println("No interpreters found.");
        }
    }

    private void listKeywordSpotters(Console console) {
        Collection<KSService> spotters = voiceManager.getKSs();
        if (!spotters.isEmpty()) {
            KSService defaultKS = voiceManager.getKS();
            Locale locale = localeProvider.getLocale();
            spotters.stream().sorted(comparing(s -> s.getLabel(locale))).forEach(ks -> {
                console.println(
                        String.format("%s %s (%s)", ks.equals(defaultKS) ? "*" : " ", ks.getLabel(locale), ks.getId()));
            });
        } else {
            console.println("No keyword spotters found.");
        }
    }

    private void listSTTs(Console console) {
        Collection<STTService> services = voiceManager.getSTTs();
        if (!services.isEmpty()) {
            STTService defaultSTT = voiceManager.getSTT();
            Locale locale = localeProvider.getLocale();
            services.stream().sorted(comparing(s -> s.getLabel(locale))).forEach(stt -> {
                console.println(String.format("%s %s (%s)", stt.equals(defaultSTT) ? "*" : " ", stt.getLabel(locale),
                        stt.getId()));
            });
        } else {
            console.println("No Speech-to-Text services found.");
        }
    }

    private void listTTSs(Console console) {
        Collection<TTSService> services = voiceManager.getTTSs();
        if (!services.isEmpty()) {
            TTSService defaultTTS = voiceManager.getTTS();
            Locale locale = localeProvider.getLocale();
            services.stream().sorted(comparing(s -> s.getLabel(locale))).forEach(tts -> {
                console.println(String.format("%s %s (%s)", tts.equals(defaultTTS) ? "*" : " ", tts.getLabel(locale),
                        tts.getId()));
            });
        } else {
            console.println("No Text-to-Speech services found.");
        }
    }

    private @Nullable Voice getVoice(String id) {
        return voiceManager.getAllVoices().stream().filter(voice -> voice.getUID().equals(id)).findAny().orElse(null);
    }
}
