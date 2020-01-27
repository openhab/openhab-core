/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.text.InterpretationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension for all voice features.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Wouter Born - Sort TTS voices
 */
@Component(service = ConsoleCommandExtension.class)
public class VoiceConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_SAY = "say";
    private static final String SUBCMD_INTERPRET = "interpret";
    private static final String SUBCMD_VOICES = "voices";

    private final ItemRegistry itemRegistry;
    private final VoiceManager voiceManager;
    private final LocaleProvider localeProvider;

    @Activate
    public VoiceConsoleCommandExtension(final @Reference VoiceManager voiceManager,
            final @Reference LocaleProvider localeProvider, final @Reference ItemRegistry itemRegistry) {
        super("voice", "Commands around voice enablement features.");
        this.voiceManager = voiceManager;
        this.localeProvider = localeProvider;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] { buildCommandUsage(SUBCMD_SAY + " <text>", "speaks a text"),
                buildCommandUsage(SUBCMD_INTERPRET + " <command>", "interprets a human language command"),
                buildCommandUsage(SUBCMD_VOICES, "lists available voices of the TTS services") });
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
                defaultVoice = voiceManager.getPreferredVoice(tts.getAvailableVoices());
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
            console.println(ie.getMessage());
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

}
