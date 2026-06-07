/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.FileAudioStream;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.voice.DialogContext;
import org.openhab.core.voice.DialogRegistration;
import org.openhab.core.voice.KSService;
import org.openhab.core.voice.STTService;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationArguments;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationManager;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension for all voice features.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Wouter Born - Sort TTS voices
 * @author Laurent Garnier - Added sub-commands startdialog and stopdialog
 * @author Miguel Álvarez Díez - Add transcribe command
 * @author Miguel Álvarez Díez - Add conversation command
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class VoiceConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_SAY = "say";
    private static final String SUBCMD_TRANSCRIBE = "transcribe";
    private static final String SUBCMD_INTERPRET = "interpret";
    private static final String SUBCMD_VOICES = "voices";
    private static final String SUBCMD_START_DIALOG = "startdialog";
    private static final String SUBCMD_STOP_DIALOG = "stopdialog";
    private static final String SUBCMD_REGISTER_DIALOG = "registerdialog";
    private static final String SUBCMD_UNREGISTER_DIALOG = "unregisterdialog";
    private static final String SUBCMD_LISTEN_ANSWER = "listenandanswer";
    private static final String SUBCMD_DIALOGS = "dialogs";
    private static final String SUBCMD_DIALOG_REGS = "dialogregs";
    private static final String SUBCMD_INTERPRETERS = "interpreters";
    private static final String SUBCMD_KEYWORD_SPOTTERS = "keywordspotters";
    private static final String SUBCMD_STT_SERVICES = "sttservices";
    private static final String SUBCMD_TTS_SERVICES = "ttsservices";
    private static final String SUBCMD_LLM_TOOLS = "llmtools";
    private static final String SUBCMD_ITEMS = "items";
    private static final String SUBCMD_CONVERSATION = "conversation";
    private static final String SUBCMD_CONVERSATION_REMOVE = "conversationremove";

    private final ItemRegistry itemRegistry;
    private final ConversationManager conversationManager;
    private final VoiceManager voiceManager;
    private final AudioManager audioManager;
    private final LocaleProvider localeProvider;
    private final LLMToolRegistry llmToolRegistry;
    private final ItemPermissionResolver itemPermissionResolver;

    @Activate
    public VoiceConsoleCommandExtension(final @Reference ConversationManager conversationManager,
            final @Reference VoiceManager voiceManager, final @Reference AudioManager audioManager,
            final @Reference LocaleProvider localeProvider, final @Reference ItemRegistry itemRegistry,
            final @Reference LLMToolRegistry llmToolRegistry,
            final @Reference ItemPermissionResolver itemPermissionResolver) {
        super("voice", "Commands around voice enablement features.");
        this.conversationManager = conversationManager;
        this.voiceManager = voiceManager;
        this.audioManager = audioManager;
        this.localeProvider = localeProvider;
        this.itemRegistry = itemRegistry;
        this.llmToolRegistry = llmToolRegistry;
        this.itemPermissionResolver = itemPermissionResolver;
    }

    @Override
    public List<String> getUsages() {
        return List.of(buildCommandUsage(SUBCMD_SAY + " <text>", "speaks a text"), buildCommandUsage(
                SUBCMD_TRANSCRIBE + " [--source <source>]|[--file <file>] [--stt <stt>] [--locale <locale>]",
                "transcribe audio from default source, optionally specify a different source/file, speech-to-text service or locale"),
                buildCommandUsage(SUBCMD_INTERPRET
                        + " [--hli <comma,separated,interpreterIds>] [--conversation <conversationId>] [--llm-tools <comma,separated,llmToolIds>] [--location <locationId>] <command>",
                        "interprets a human language command"),
                buildCommandUsage(SUBCMD_VOICES, "lists available voices of the TTS services"),
                buildCommandUsage(SUBCMD_DIALOGS, "lists the running dialog and their audio/voice services"),
                buildCommandUsage(SUBCMD_DIALOG_REGS,
                        "lists the existing dialog registrations and their selected audio/voice services"),
                buildCommandUsage(SUBCMD_REGISTER_DIALOG
                        + " [--source <source>] [--sink <sink>] [--hli <comma,separated,interpreterIds>] [--tts <tts> [--voice <voice>]] [--stt <stt>] [--ks ks [--keyword <ks>]] [--listening-item <listeningItem>] [--location-item <locationItem>] [--dialog-group <dialogGroup>]",
                        "register a new dialog processing using the default services or the services identified with provided arguments, it will be persisted and keep running whenever is possible."),
                buildCommandUsage(SUBCMD_UNREGISTER_DIALOG + " [source]",
                        "unregister the dialog processing for the default audio source or the audio source identified with provided argument, stopping it if started"),
                buildCommandUsage(SUBCMD_START_DIALOG
                        + " [--source <source>] [--sink <sink>] [--hli <comma,separated,interpreterIds>] [--tts <tts> [--voice <voice>]] [--stt <stt>] [--ks ks [--keyword <ks>]] [--listening-item <listeningItem>] [--location-item <locationItem>] [--dialog-group <dialogGroup>]",
                        "start a new dialog processing using the default services or the services identified with provided arguments"),
                buildCommandUsage(SUBCMD_STOP_DIALOG + " [<source>]",
                        "stop the dialog processing for the default audio source or the audio source identified with provided argument"),
                buildCommandUsage(SUBCMD_LISTEN_ANSWER
                        + " [--source <source>] [--sink <sink>] [--hli <comma,separated,interpreterIds>] [--tts <tts> [--voice <voice>]] [--stt <stt>] [--listening-item <listeningItem>] [--location-item <locationItem>] [--dialog-group <dialogGroup>]",
                        "Execute a simple dialog sequence without keyword spotting using the default services or the services identified with provided arguments"),
                buildCommandUsage(SUBCMD_INTERPRETERS, "lists the interpreters"),
                buildCommandUsage(SUBCMD_KEYWORD_SPOTTERS, "lists the keyword spotters"),
                buildCommandUsage(SUBCMD_STT_SERVICES, "lists the Speech-to-Text services"),
                buildCommandUsage(SUBCMD_TTS_SERVICES, "lists the Text-to-Speech services"),
                buildCommandUsage(SUBCMD_LLM_TOOLS, "lists the LLM tools"),
                buildCommandUsage(SUBCMD_ITEMS + " [--all]",
                        "lists the Items that the voice system has access to, optionally list all Items"),
                buildCommandUsage(SUBCMD_CONVERSATION + " [--uid] <conversationId>", "Displays conversation messages"),
                buildCommandUsage(SUBCMD_CONVERSATION_REMOVE + " [--message-id <message-id>] <conversationId>",
                        "Remove Conversation"));
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_SAY -> {
                    if (args.length > 1) {
                        say(Arrays.copyOfRange(args, 1, args.length), console);
                    } else {
                        console.println("Specify text to say (e.g. 'say hello')");
                    }
                    return;
                }
                case SUBCMD_TRANSCRIBE -> {
                    transcribe(args, console);
                    return;
                }
                case SUBCMD_INTERPRET -> {
                    if (args.length > 1) {
                        interpret(Arrays.copyOfRange(args, 1, args.length), console);
                    } else {
                        console.println("Specify text to interpret (e.g. 'interpret turn all lights off')");
                    }
                    return;
                }
                case SUBCMD_VOICES -> {
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
                }
                case SUBCMD_REGISTER_DIALOG -> {
                    DialogRegistration dialogRegistration;
                    try {
                        dialogRegistration = parseDialogRegistration(Arrays.copyOfRange(args, 1, args.length));
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while parsing the dialog options"));
                        break;
                    }
                    try {
                        voiceManager.registerDialog(dialogRegistration);
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while registering the dialog"));
                    }
                    return;
                }
                case SUBCMD_UNREGISTER_DIALOG -> {
                    try {
                        var sourceId = args.length < 2 ? audioManager.getSourceId() : args[1];
                        if (sourceId == null) {
                            console.println("No source provided nor default source available");
                            break;
                        }
                        voiceManager.unregisterDialog(sourceId);
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while stopping the dialog"));
                    }
                    return;
                }
                case SUBCMD_START_DIALOG -> {
                    DialogContext.Builder dialogContextBuilder;
                    try {
                        dialogContextBuilder = parseDialogContext(Arrays.copyOfRange(args, 1, args.length));
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while parsing the dialog options"));
                        break;
                    }
                    try {
                        voiceManager.startDialog(dialogContextBuilder.build());
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while starting the dialog"));
                    }
                    return;
                }
                case SUBCMD_STOP_DIALOG -> {
                    try {
                        voiceManager.stopDialog(args.length < 2 ? null : audioManager.getSource(args[1]));
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while stopping the dialog"));
                    }
                    return;
                }
                case SUBCMD_LISTEN_ANSWER -> {
                    DialogContext.Builder dialogContextBuilder;
                    try {
                        dialogContextBuilder = parseDialogContext(Arrays.copyOfRange(args, 1, args.length));
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while parsing the dialog options"));
                        break;
                    }
                    try {
                        voiceManager.listenAndAnswer(dialogContextBuilder.build());
                    } catch (IllegalStateException e) {
                        console.println(Objects.requireNonNullElse(e.getMessage(),
                                "An error occurred while executing the simple dialog sequence"));
                    }
                    return;
                }
                case SUBCMD_DIALOGS -> {
                    listDialogs(console);
                    return;
                }
                case SUBCMD_DIALOG_REGS -> {
                    listDialogRegistrations(console);
                    return;
                }
                case SUBCMD_INTERPRETERS -> {
                    listInterpreters(console);
                    return;
                }
                case SUBCMD_KEYWORD_SPOTTERS -> {
                    listKeywordSpotters(console);
                    return;
                }
                case SUBCMD_STT_SERVICES -> {
                    listSTTs(console);
                    return;
                }
                case SUBCMD_TTS_SERVICES -> {
                    listTTSs(console);
                    return;
                }
                case SUBCMD_LLM_TOOLS -> {
                    listLLMTools(console);
                    return;
                }
                case SUBCMD_ITEMS -> {
                    listItems(Arrays.copyOfRange(args, 1, args.length), console);
                    return;
                }
                case SUBCMD_CONVERSATION -> {
                    printConversationMessages(Arrays.copyOfRange(args, 1, args.length), console);
                    return;
                }
                case SUBCMD_CONVERSATION_REMOVE -> {
                    removeConversationMessages(Arrays.copyOfRange(args, 1, args.length), console);
                    return;
                }
                default -> {
                }
            }
        }
        printUsage(console);
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
        HashMap<String, String> parameters;
        try {
            parameters = parseNamedParameters(args, true);
        } catch (IllegalStateException e) {
            console.println(Objects.requireNonNullElse(e.getMessage(), "An error parsing positional parameters"));
            return;
        }
        String[] arguments = Arrays.copyOfRange(args, parameters.size() * 2, args.length);
        @Nullable
        String hliIdList = parameters.remove("hli");
        @Nullable
        String conversationId = parameters.remove("conversation");
        @Nullable
        String llmToolIdList = parameters.remove("llm-tools");
        @Nullable
        String locationItem = parameters.remove("location");

        if (arguments.length == 0) {
            console.println("No command provided.");
            return;
        }
        StringBuilder sb = new StringBuilder(arguments[0]);
        for (int i = 1; i < arguments.length; i++) {
            sb.append(" ");
            sb.append(arguments[i]);
        }
        String msg = sb.toString();
        var interpretationArgs = new InterpretationArguments( //
                Objects.requireNonNullElse(hliIdList, ""), //
                Objects.requireNonNullElse(conversationId, ""), //
                Objects.requireNonNullElse(llmToolIdList, ""), //
                Objects.requireNonNullElse(locationItem, ""), null //
        );
        try {
            String result = voiceManager.interpret(msg, interpretationArgs);
            console.println(result);
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
                    msg.append(item.getState());
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

    private void transcribe(String[] args, Console console) {
        HashMap<String, String> parameters;
        try {
            parameters = parseNamedParameters(args, false);
        } catch (IllegalStateException e) {
            console.println(Objects.requireNonNullElse(e.getMessage(), "An error parsing positional parameters"));
            return;
        }
        @Nullable
        Locale locale;
        try {
            locale = parameters.containsKey("locale")
                    ? Locale.forLanguageTag(Objects.requireNonNull(parameters.get("locale")))
                    : null;
        } catch (MissingResourceException e) {
            console.println("Error: Locale '" + parameters.get("locale") + "' is not correct.");
            return;
        }
        String text;
        if (parameters.containsKey("file")) {
            FileAudioStream fileAudioStream;
            try {
                var file = Path.of(OpenHAB.getConfigFolder(), AudioManager.SOUND_DIR, parameters.get("file")).toFile();
                if (!file.exists()) {
                    throw new FileNotFoundException();
                }
                fileAudioStream = new FileAudioStream(file);
            } catch (AudioException e) {
                console.println("Error: Unable to open '" + parameters.get("file") + "' file audio stream.");
                return;
            } catch (FileNotFoundException e) {
                console.println("Error: File '" + parameters.get("file") + "' not found in sound folder.");
                return;
            }
            text = voiceManager.transcribe(fileAudioStream, parameters.get("stt"), locale);
        } else {
            text = voiceManager.transcribe(parameters.get("source"), parameters.get("stt"), null);
        }
        if (!text.isBlank()) {
            console.println("Transcription: " + text);
        } else {
            console.println("No transcription generated");
        }
    }

    private void listDialogRegistrations(Console console) {
        Collection<DialogRegistration> registrations = voiceManager.getDialogRegistrations();
        if (!registrations.isEmpty()) {
            registrations.stream().sorted(comparing(dr -> dr.sourceId)).forEach(dr -> {
                String locationText = dr.locationItem != null ? String.format(" Location: %s", dr.locationItem) : "";
                console.println(String.format(
                        " Source: %s - Sink: %s (STT: %s, TTS: %s, HLIs: %s, KS: %s, Keyword: %s, Dialog Group: %s)%s",
                        dr.sourceId, dr.sinkId, getOrDefault(dr.sttId), getOrDefault(dr.ttsId),
                        dr.hliIds.isEmpty() ? getOrDefault(null) : String.join("->", dr.hliIds), getOrDefault(dr.ksId),
                        getOrDefault(dr.keyword), getOrDefault(dr.dialogGroup), locationText));
            });
        } else {
            console.println("No dialog registrations.");
        }
    }

    private String getOrDefault(@Nullable String value) {
        return value != null && !value.isBlank() ? value : "**Default**";
    }

    private void listDialogs(Console console) {
        Collection<DialogContext> dialogContexts = voiceManager.getDialogsContexts();
        if (!dialogContexts.isEmpty()) {
            dialogContexts.stream().sorted(comparing(s -> s.source().getId())).forEach(c -> {
                var ks = c.dt();
                String ksText = ks != null ? String.format(", KS: %s, Keyword: %s", ks.getId(), c.keyword()) : "";
                String locationText = c.locationItem() != null ? String.format(" Location: %s", c.locationItem()) : "";
                console.println(String.format(
                        " Source: %s - Sink: %s (STT: %s, TTS: %s, HLIs: %s%s, Dialog Group: %s)%s", c.source().getId(),
                        c.sink().getId(), c.stt().getId(), c.tts().getId(),
                        c.hlis().stream().map(HumanLanguageInterpreter::getId).collect(Collectors.joining("->")),
                        ksText, c.dialogGroup(), locationText));
            });
        } else {
            console.println("No running dialogs.");
        }
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

    private void listLLMTools(Console console) {
        Collection<LLMTool> tools = llmToolRegistry.getAll();
        if (!tools.isEmpty()) {
            Locale locale = localeProvider.getLocale();
            tools.stream().sorted(comparing(s -> s.getLabel(locale))).forEach(tool -> {
                console.println(String.format("  %s (%s)", tool.getLabel(locale), tool.getUID()));
            });
        } else {
            console.println("No LLM tools found.");
        }
    }

    private void listItems(String[] args, Console console) {
        boolean all = Arrays.asList(args).contains("--all");

        Collection<Item> items = all ? itemRegistry.getAll()
                : itemRegistry.getAll().stream() //
                        .filter(itemPermissionResolver::isAccessible) //
                        .toList();
        List<Item> sortedItems = items.stream().sorted(comparing(Item::getName)).toList();

        if (sortedItems.isEmpty()) {
            console.println("No accessible Items found (try --all to list all Items).");
            return;
        }

        record Row(String permission, String itemName, String source) {
        }

        List<Row> rows = sortedItems.stream().map(item -> {
            ItemPermissionResolver.ItemPermissionDetails access = itemPermissionResolver.getItemPermissionDetails(item);
            String permission = switch (access.permission()) {
                case NO_ACCESS -> "No Access";
                case READ_ONLY -> "Read-Only";
                case READ_WRITE -> "Read & Write";
            };
            return new Row(permission, item.getName(), Objects.requireNonNullElse(access.source(), ""));
        }).toList();

        int itemNameWidth = Math.max(rows.stream().mapToInt(r -> r.itemName.length()).max().orElse(0), 4);
        int permissionWidth = Math.max(rows.stream().mapToInt(r -> r.permission.length()).max().orElse(0), 10);
        int sourceWidth = Math.max(rows.stream().mapToInt(r -> r.source.length()).max().orElse(0), 6);

        String format = " %-" + itemNameWidth + "s | %-" + permissionWidth + "s | %-" + sourceWidth + "s";
        console.println(String.format(format, "Item", "Permission", "Source"));
        console.println(String.format("-%s-+-%s-+-%s", "-".repeat(itemNameWidth), "-".repeat(permissionWidth),
                "-".repeat(sourceWidth)));
        for (Row row : rows) {
            console.println(String.format(format, row.itemName, row.permission, row.source));
        }
    }

    private void printConversationMessages(String[] args, Console console) {
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        boolean uid = argList.remove("--uid");
        if (argList.isEmpty()) {
            console.println("Missing conversation ID.");
            return;
        }
        String conversationId = argList.removeFirst();
        if (!argList.isEmpty()) {
            console.println("Incorrect number of arguments");
            return;
        }

        Conversation conversation = conversationManager.getConversation(conversationId);
        if (conversation.getMessages().isEmpty()) {
            console.println("Empty conversation");
            return;
        }
        for (var message : conversation.getMessages()) {
            if (uid) {
                console.printf("%s - %s|> %s\n", message.id(), message.role(), message.content());
            } else {
                console.printf("%s|> %s\n", message.role(), message.content());
            }
        }
    }

    private void removeConversationMessages(String[] args, Console console) {
        HashMap<String, String> parameters;
        try {
            parameters = parseNamedParameters(args, true);
        } catch (IllegalStateException e) {
            console.println(Objects.requireNonNullElse(e.getMessage(), "An error parsing positional parameters"));
            return;
        }
        String[] arguments = Arrays.copyOfRange(args, parameters.size() * 2, args.length);
        if (arguments.length != 1) {
            console.println("Incorrect number of arguments");
            return;
        }
        @Nullable
        String rawMessageID = parameters.remove("message-id");
        if (!parameters.isEmpty()) {
            console.println("Argument " + parameters.keySet().stream().findAny().orElse("") + " is not supported");
            return;
        }
        Conversation conversation = conversationManager.getConversation(arguments[0]);
        if (conversation.getMessages().isEmpty()) {
            console.println("Empty conversation");
            return;
        }
        if (rawMessageID != null) {
            Integer messageID;
            try {
                messageID = Integer.parseInt(rawMessageID);
            } catch (NumberFormatException e) {
                console.print("Invalid message ID, must be a number: " + rawMessageID);
                return;
            }
            if (!conversation.removeSinceMessage(messageID)) {
                console.println("No messages were removed");
                return;
            }
            console.println("Messages since " + rawMessageID + " were removed");
        } else {
            conversation.removeMessages();
        }
    }

    private @Nullable Voice getVoice(@Nullable String id) {
        return id == null ? null
                : voiceManager.getAllVoices().stream().filter(voice -> voice.getUID().equals(id)).findAny()
                        .orElse(null);
    }

    private HashMap<String, String> parseNamedParameters(String[] args, boolean allowText) {
        var parameters = new HashMap<String, String>();
        for (int i = 0; i < args.length; i++) {
            var arg = args[i].trim();
            if (arg.startsWith("--")) {
                i++;
                if (i < args.length) {
                    parameters.put(arg.replace("--", ""), args[i].trim());
                } else {
                    throw new IllegalStateException("Missing value for argument " + arg);
                }
            } else {
                if (allowText) {
                    break;
                }
                throw new IllegalStateException("Argument name should start by -- " + arg);
            }
        }
        return parameters;
    }

    private DialogContext.Builder parseDialogContext(String[] args) {
        var dialogContextBuilder = voiceManager.getDialogContextBuilder();
        if (args.length < 1) {
            return dialogContextBuilder;
        }
        var parameters = parseNamedParameters(args, false);
        String sourceId = parameters.remove("source");
        if (sourceId != null) {
            var source = audioManager.getSource(sourceId);
            if (source == null) {
                throw new IllegalStateException("Audio source not found");
            }
            dialogContextBuilder.withSource(source);
        }
        String sinkId = parameters.remove("sink");
        if (sinkId != null) {
            var sink = audioManager.getSink(sinkId);
            if (sink == null) {
                throw new IllegalStateException("Audio sink not found");
            }
            dialogContextBuilder.withSink(sink);
        }
        Conversation conversation = conversationManager
                .getConversation(Objects.requireNonNullElse(parameters.remove("conversation"), ""));
        dialogContextBuilder //
                .withSTT(voiceManager.getSTT(parameters.remove("stt"))) //
                .withTTS(voiceManager.getTTS(parameters.remove("tts"))) //
                .withVoice(getVoice(parameters.remove("voice"))) //
                .withHLIs(voiceManager.getHLIsByIds(parameters.remove("hli"))) //
                .withKS(voiceManager.getKS(parameters.remove("ks"))) //
                .withListeningItem(parameters.remove("listening-item")) //
                .withLocationItem(parameters.remove("location-item")) //
                .withDialogGroup(parameters.remove("dialog-group")) //
                .withConversation(conversation) //
                .withLLMTools(llmToolRegistry.getByIds(parameters.remove("llm-tools"))) //
                .withKeyword(parameters.remove("keyword"));
        if (!parameters.isEmpty()) {
            throw new IllegalStateException(
                    "Argument" + parameters.keySet().stream().findAny().orElse("") + "is not supported");
        }
        return dialogContextBuilder;
    }

    private DialogRegistration parseDialogRegistration(String[] args) {
        var parameters = parseNamedParameters(args, false);
        @Nullable
        String sourceId = parameters.remove("source");
        if (sourceId == null) {
            sourceId = audioManager.getSourceId();
        }
        if (sourceId == null) {
            throw new IllegalStateException("A source is required if the default is not configured");
        }
        @Nullable
        String sinkId = parameters.remove("sink");
        if (sinkId == null) {
            sinkId = audioManager.getSinkId();
        }
        if (sinkId == null) {
            throw new IllegalStateException("A sink is required if the default is not configured");
        }
        var dr = new DialogRegistration(sourceId, sinkId);
        dr.ksId = parameters.remove("ks");
        dr.keyword = parameters.remove("keyword");
        dr.sttId = parameters.remove("stt");
        dr.ttsId = parameters.remove("tts");
        dr.voiceId = parameters.remove("voice");
        dr.listeningItem = parameters.remove("listening-item");
        dr.locationItem = parameters.remove("location-item");
        dr.dialogGroup = parameters.remove("dialog-group");
        dr.conversationId = parameters.remove("conversation");

        String hliIds = parameters.remove("hli");
        if (hliIds != null) {
            dr.hliIds = Arrays.stream(hliIds.split(",")).map(String::trim).toList();
        }
        String llmToolIds = parameters.remove("llm-tools");
        if (llmToolIds != null) {
            dr.llmToolIds = Arrays.stream(llmToolIds.split(",")).map(String::trim).toList();
        }
        if (!parameters.isEmpty()) {
            throw new IllegalStateException(
                    "Argument " + parameters.keySet().stream().findAny().orElse("") + " is not supported");
        }
        return dr;
    }
}
