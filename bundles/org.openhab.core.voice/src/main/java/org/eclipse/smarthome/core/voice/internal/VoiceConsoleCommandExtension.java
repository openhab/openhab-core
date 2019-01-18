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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemNotUniqueException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.voice.TTSService;
import org.eclipse.smarthome.core.voice.Voice;
import org.eclipse.smarthome.core.voice.VoiceManager;
import org.eclipse.smarthome.core.voice.text.InterpretationException;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension for all voice features.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Wouter Born - Sort TTS voices
 */
@Component(service = ConsoleCommandExtension.class)
public class VoiceConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_SAY = "say";
    private static final String SUBCMD_INTERPRET = "interpret";
    private static final String SUBCMD_VOICES = "voices";

    private ItemRegistry itemRegistry;
    private LocaleProvider localeProvider;
    private VoiceManager voiceManager;

    public VoiceConsoleCommandExtension() {
        super("voice", "Commands around voice enablement features.");
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
                        say((String[]) ArrayUtils.subarray(args, 1, args.length), console);
                    } else {
                        console.println("Specify text to say (e.g. 'say hello')");
                    }
                    return;
                case SUBCMD_INTERPRET:
                    if (args.length > 1) {
                        interpret((String[]) ArrayUtils.subarray(args, 1, args.length), console);
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

    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference
    protected void setLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    protected void unsetLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = null;
    }

    @Reference
    protected void setVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = voiceManager;
    }

    protected void unsetVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = null;
    }

}
