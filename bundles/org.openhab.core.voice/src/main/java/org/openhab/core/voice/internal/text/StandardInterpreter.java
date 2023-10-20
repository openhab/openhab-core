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
package org.openhab.core.voice.internal.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.TypeParser;
import org.openhab.core.voice.text.AbstractRuleBasedInterpreter;
import org.openhab.core.voice.text.Expression;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.Rule;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A human language command interpretation service.
 *
 * @author Tilman Kamp - Initial contribution
 * @author Kai Kreuzer - Added further German interpretation rules
 * @author Laurent Garnier - Added French interpretation rules
 * @author Miguel Álvarez - Added Spanish interpretation rules
 * @author Miguel Álvarez - Added item's dynamic rules
 */
@NonNullByDefault
@Component(service = HumanLanguageInterpreter.class)
public class StandardInterpreter extends AbstractRuleBasedInterpreter {
    private Logger logger = LoggerFactory.getLogger(StandardInterpreter.class);
    private final ItemRegistry itemRegistry;
    private final String metadataNamespace = "voice-system";
    private final MetadataRegistry metadataRegistry;

    @Activate
    public StandardInterpreter(final @Reference EventPublisher eventPublisher,
            final @Reference ItemRegistry itemRegistry, @Reference MetadataRegistry metadataRegistry) {
        super(eventPublisher, itemRegistry, metadataRegistry);
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public Set<Locale> getSupportedLocales() {
        return Set.of(Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH, new Locale("es"));
    }

    @Override
    public void createRules(@Nullable Locale locale) {

        /****************************** ENGLISH ******************************/

        if (locale == null || Objects.equals(locale.getLanguage(), Locale.ENGLISH.getLanguage())) {
            Expression onOff = alt(cmd("on", OnOffType.ON), cmd("off", OnOffType.OFF));
            Expression turn = alt("turn", "switch");
            Expression put = alt("put", "bring");
            Expression of = opt("of");
            Expression the = opt("the");
            Expression to = opt("to");
            Expression color = alt(cmd("white", HSBType.WHITE), cmd("pink", HSBType.fromRGB(255, 96, 208)),
                    cmd("yellow", HSBType.fromRGB(255, 224, 32)), cmd("orange", HSBType.fromRGB(255, 160, 16)),
                    cmd("purple", HSBType.fromRGB(128, 0, 128)), cmd("red", HSBType.RED), cmd("green", HSBType.GREEN),
                    cmd("blue", HSBType.BLUE));
            addRules(Locale.ENGLISH,

                    /* OnOffType */

                    itemRule(seq(turn, the), /* item */ onOff),

                    itemRule(seq(turn, onOff) /* item */),

                    /* IncreaseDecreaseType */

                    itemRule(seq(cmd(alt("dim", "decrease", "lower", "soften"), IncreaseDecreaseType.DECREASE),
                            the) /*
                                  * item
                                  */),

                    itemRule(seq(cmd(alt("brighten", "increase", "harden", "enhance"), IncreaseDecreaseType.INCREASE),
                            the) /* item */),

                    /* ColorType */

                    itemRule(seq(opt("set"), the, opt("color"), of, the), /* item */ seq(to, color)),

                    /* UpDownType */

                    itemRule(seq(put, the), /* item */ cmd("up", UpDownType.UP)),

                    itemRule(seq(put, the), /* item */ cmd("down", UpDownType.DOWN)),

                    /* NextPreviousType */

                    itemRule("move",
                            /* item */ seq(opt("to"),
                                    alt(cmd("next", NextPreviousType.NEXT),
                                            cmd("previous", NextPreviousType.PREVIOUS)))),

                    /* PlayPauseType */

                    itemRule(seq(cmd("play", PlayPauseType.PLAY), the) /* item */),

                    itemRule(seq(cmd("pause", PlayPauseType.PAUSE), the) /* item */),

                    /* RewindFastForwardType */

                    itemRule(seq(cmd("rewind", RewindFastforwardType.REWIND), the) /* item */),

                    itemRule(seq(cmd(seq(opt("fast"), "forward"), RewindFastforwardType.FASTFORWARD), the) /* item */),

                    /* StopMoveType */

                    itemRule(seq(cmd("stop", StopMoveType.STOP), the) /* item */),

                    itemRule(seq(cmd(alt("start", "move", "continue"), StopMoveType.MOVE), the) /* item */),

                    /* RefreshType */

                    itemRule(seq(cmd("refresh", RefreshType.REFRESH), the) /* item */)

            );
            /* Item description commands */
            addRules(Locale.ENGLISH, createItemDescriptionRules( //
                    (allowedItemNames, labeledCmd) -> restrictedItemRule(allowedItemNames, //
                            seq(alt("set", "change"), opt(the)), /* item */ seq(to, labeledCmd)//
                    ), //
                    Locale.ENGLISH).toArray(Rule[]::new));
        }

        /****************************** GERMAN ******************************/

        if (locale == null || Objects.equals(locale.getLanguage(), Locale.GERMAN.getLanguage())) {

            Expression einAnAus = alt(cmd("ein", OnOffType.ON), cmd("an", OnOffType.ON), cmd("aus", OnOffType.OFF));
            Expression denDieDas = opt(alt("den", "die", "das"));
            Expression schalte = alt("schalt", "schalte", "mach");
            Expression pause = alt("pause", "stoppe");
            Expression mache = alt("mach", "mache", "fahre");
            Expression spiele = alt("spiele", "spiel", "starte");
            Expression zu = alt("zu", "zum", "zur");
            Expression the = opt("the");
            Expression naechste = alt("nächste", "nächstes", "nächster");
            Expression vorherige = alt("vorherige", "vorheriges", "vorheriger");
            Expression farbe = alt(cmd("weiß", HSBType.WHITE), cmd("pink", HSBType.fromRGB(255, 96, 208)),
                    cmd("gelb", HSBType.fromRGB(255, 224, 32)), cmd("orange", HSBType.fromRGB(255, 160, 16)),
                    cmd("lila", HSBType.fromRGB(128, 0, 128)), cmd("rot", HSBType.RED), cmd("grün", HSBType.GREEN),
                    cmd("blau", HSBType.BLUE));

            addRules(Locale.GERMAN,

                    /* OnOffType */

                    itemRule(seq(schalte, denDieDas), /* item */ einAnAus),

                    /* IncreaseDecreaseType */

                    itemRule(seq(cmd(alt("dimme"), IncreaseDecreaseType.DECREASE), denDieDas) /* item */),

                    itemRule(seq(schalte, denDieDas),
                            /* item */ cmd(alt("dunkler", "weniger"), IncreaseDecreaseType.DECREASE)),

                    itemRule(seq(schalte, denDieDas),
                            /* item */ cmd(alt("heller", "mehr"), IncreaseDecreaseType.INCREASE)),

                    /* ColorType */

                    itemRule(seq(schalte, denDieDas), /* item */ seq(opt("auf"), farbe)),

                    /* UpDownType */

                    itemRule(seq(mache, denDieDas), /* item */ cmd("hoch", UpDownType.UP)),

                    itemRule(seq(mache, denDieDas), /* item */ cmd("runter", UpDownType.DOWN)),

                    /* NextPreviousType */

                    itemRule("wechsle",
                            /* item */ seq(opt(zu),
                                    alt(cmd(naechste, NextPreviousType.NEXT),
                                            cmd(vorherige, NextPreviousType.PREVIOUS)))),

                    /* PlayPauseType */

                    itemRule(seq(cmd(spiele, PlayPauseType.PLAY), the) /* item */),

                    itemRule(seq(cmd(pause, PlayPauseType.PAUSE), the) /* item */)

            );

            /* Item description commands */

            addRules(Locale.GERMAN, createItemDescriptionRules( //
                    (allowedItemNames, labeledCmd) -> restrictedItemRule(allowedItemNames, //
                            seq(schalte, denDieDas), /* item */ seq(opt("auf"), labeledCmd)//
                    ), //
                    Locale.GERMAN).toArray(Rule[]::new));

        }

        /****************************** FRENCH ******************************/

        if (locale == null || Objects.equals(locale.getLanguage(), Locale.FRENCH.getLanguage())) {
            Expression allume = alt("allume", "démarre", "active");
            Expression eteins = alt("éteins", "stoppe", "désactive", "coupe");
            Expression lela = opt(alt("le", "la", "les", "l"));
            Expression poursurdude = opt(alt("pour", "sur", "du", "de"));
            Expression couleur = alt(cmd("blanc", HSBType.WHITE), cmd("rose", HSBType.fromRGB(255, 96, 208)),
                    cmd("jaune", HSBType.fromRGB(255, 224, 32)), cmd("orange", HSBType.fromRGB(255, 160, 16)),
                    cmd("violet", HSBType.fromRGB(128, 0, 128)), cmd("rouge", HSBType.RED), cmd("vert", HSBType.GREEN),
                    cmd("bleu", HSBType.BLUE));

            addRules(Locale.FRENCH,

                    /* OnOffType */

                    itemRule(seq(cmd(allume, OnOffType.ON), lela) /* item */),
                    itemRule(seq(cmd(eteins, OnOffType.OFF), lela) /* item */),

                    /* IncreaseDecreaseType */

                    itemRule(seq(cmd("augmente", IncreaseDecreaseType.INCREASE), lela) /* item */),
                    itemRule(seq(cmd("diminue", IncreaseDecreaseType.DECREASE), lela) /* item */),

                    itemRule(seq(cmd("plus", IncreaseDecreaseType.INCREASE), "de") /* item */),
                    itemRule(seq(cmd("moins", IncreaseDecreaseType.DECREASE), "de") /* item */),

                    /* ColorType */

                    itemRule(seq("couleur", couleur, opt("pour"), lela) /* item */),

                    /* PlayPauseType */

                    itemRule(seq(cmd("reprise", PlayPauseType.PLAY), "lecture", poursurdude, lela) /* item */),
                    itemRule(seq(cmd("pause", PlayPauseType.PAUSE), "lecture", poursurdude, lela) /* item */),

                    /* NextPreviousType */

                    itemRule(seq(alt("plage", "piste"),
                            alt(cmd("suivante", NextPreviousType.NEXT), cmd("précédente", NextPreviousType.PREVIOUS)),
                            poursurdude, lela) /* item */),

                    /* UpDownType */

                    itemRule(seq(cmd("monte", UpDownType.UP), lela) /* item */),
                    itemRule(seq(cmd("descends", UpDownType.DOWN), lela) /* item */),

                    /* StopMoveType */

                    itemRule(seq(cmd("arrête", StopMoveType.STOP), lela) /* item */),
                    itemRule(seq(cmd(alt("bouge", "déplace"), StopMoveType.MOVE), lela) /* item */),

                    /* RefreshType */

                    itemRule(seq(cmd("rafraîchis", RefreshType.REFRESH), lela) /* item */)

            );

            /* Item description commands */

            addRules(Locale.FRENCH, createItemDescriptionRules( //
                    (allowedItemNames, labeledCmd) -> restrictedItemRule(allowedItemNames, //
                            seq("mets", lela), /* item */ seq(poursurdude, lela, labeledCmd)//
                    ), //
                    Locale.FRENCH).toArray(Rule[]::new));
        }

        /****************************** SPANISH ******************************/

        Locale localeES = new Locale("es");
        if (locale == null || Objects.equals(locale.getLanguage(), localeES.getLanguage())) {
            Expression encenderApagar = alt(cmd(alt("enciende", "encender"), OnOffType.ON),
                    cmd(alt("apaga", "apagar"), OnOffType.OFF));
            Expression cambiar = alt("cambia", "cambiar");
            Expression poner = alt("pon", "poner");
            Expression preposicion = opt(alt("a", "de", "en"));
            Expression articulo = opt(alt("el", "la", "los", "las"));
            Expression nombreColor = alt(cmd("blanco", HSBType.WHITE), cmd("rosa", HSBType.fromRGB(255, 96, 208)),
                    cmd("amarillo", HSBType.fromRGB(255, 224, 32)), cmd("naranja", HSBType.fromRGB(255, 160, 16)),
                    cmd("púrpura", HSBType.fromRGB(128, 0, 128)), cmd("rojo", HSBType.RED), cmd("verde", HSBType.GREEN),
                    cmd("azul", HSBType.BLUE));

            addRules(localeES,

                    /* OnOffType */

                    itemRule(seq(encenderApagar, articulo)/* item */),

                    /* IncreaseDecreaseType */

                    itemRule(seq(cmd(alt("baja", "suaviza", "bajar", "suavizar"), IncreaseDecreaseType.DECREASE),
                            articulo) /*
                                       * item
                                       */),

                    itemRule(seq(cmd(alt("sube", "aumenta", "subir", "aumentar"), IncreaseDecreaseType.INCREASE),
                            articulo) /* item */),

                    /* ColorType */

                    itemRule(seq(cambiar, articulo, opt("color"), preposicion, articulo),
                            /* item */ seq(opt("a"), nombreColor)),

                    /* UpDownType */

                    itemRule(seq(poner, articulo), /* item */ cmd("arriba", UpDownType.UP)),

                    itemRule(seq(poner, articulo), /* item */ cmd("abajo", UpDownType.DOWN)),

                    /* NextPreviousType */

                    itemRule(cambiar,
                            /* item */ seq(opt("a"),
                                    alt(cmd("siguiente", NextPreviousType.NEXT),
                                            cmd("anterior", NextPreviousType.PREVIOUS)))),

                    /* PlayPauseType */

                    itemRule(seq(cmd(alt("continuar", "continúa", "reanudar", "reanuda", "play"), PlayPauseType.PLAY),
                            alt(articulo, "en")) /*
                                                  * item
                                                  */),

                    itemRule(seq(cmd(alt("pausa", "pausar", "detén", "detener"), PlayPauseType.PAUSE),
                            alt(articulo, "en")) /*
                                                  * item
                                                  */),

                    /* RewindFastForwardType */

                    itemRule(seq(cmd(alt("rebobina", "rebobinar"), RewindFastforwardType.REWIND),
                            alt(articulo, "en")) /* item */),

                    itemRule(seq(cmd(alt("avanza", "avanzar"), RewindFastforwardType.FASTFORWARD),
                            alt(articulo, "en")) /* item */),

                    /* StopMoveType */

                    itemRule(seq(cmd(alt("para", "parar", "stop"), StopMoveType.STOP), articulo) /* item */),

                    itemRule(seq(cmd(alt("mueve", "mover"), StopMoveType.MOVE), articulo) /* item */),

                    /* RefreshType */

                    itemRule(seq(cmd(alt("recarga", "refresca", "recargar", "refrescar"), RefreshType.REFRESH),
                            articulo) /* item */)

            );

            /* Item description commands */

            addRules(localeES, createItemDescriptionRules( //
                    (allowedItemNames, labeledCmd) -> restrictedItemRule(allowedItemNames, //
                            seq(alt(cambiar, poner), opt(articulo)), /* item */ seq(preposicion, labeledCmd)//
                    ), //
                    localeES).toArray(Rule[]::new));
        }
    }

    @Override
    public String getId() {
        return "system";
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "Built-in Interpreter";
    }

    private List<Rule> createItemDescriptionRules(CreateItemDescriptionRule creator, @Nullable Locale locale) {
        // Map different item state/command labels with theirs values by item
        HashMap<String, HashMap<Item, String>> options = new HashMap<>();
        List<Rule> customRules = new ArrayList<>();
        for (var item : itemRegistry.getItems()) {
            customRules.addAll(createItemCustomRules(item));
            var stateDesc = item.getStateDescription(locale);
            if (stateDesc != null) {
                stateDesc.getOptions().forEach(op -> {
                    var label = op.getLabel();
                    if (label == null || label.isBlank()) {
                        label = op.getValue();
                    }
                    var optionValueByItem = options.getOrDefault(label, new HashMap<>());
                    optionValueByItem.put(item, op.getValue());
                    options.put(label, optionValueByItem);
                });
            }
            var commandDesc = item.getCommandDescription(locale);
            if (commandDesc != null) {
                commandDesc.getCommandOptions().forEach(op -> {
                    var label = op.getLabel();
                    if (label == null || label.isBlank()) {
                        label = op.getCommand();
                    }
                    var optionValueByItem = options.getOrDefault(label, new HashMap<>());
                    optionValueByItem.put(item, op.getCommand());
                    options.put(label, optionValueByItem);
                });
            }
        }
        // create rules
        return Stream.concat(customRules.stream(), options.entrySet().stream() //
                .map(entry -> {
                    String label = entry.getKey();
                    Map<Item, String> commandByItem = entry.getValue();
                    List<String> itemNames = commandByItem.keySet().stream().map(Item::getName).toList();
                    String[] labelParts = Arrays.stream(label.split("\\s")).filter(p -> !p.isBlank())
                            .toArray(String[]::new);
                    Expression labeledCmd = cmd(seq((Object[]) labelParts),
                            new ItemStateCommandSupplier(label, commandByItem));
                    return creator.itemDescriptionRule(itemNames, labeledCmd);
                })) //
                .collect(Collectors.toList());
    }

    private List<Rule> createItemCustomRules(Item item) {
        var interpreterMetadata = metadataRegistry.get(new MetadataKey(metadataNamespace, item.getName()));
        if (interpreterMetadata == null) {
            return List.of();
        }
        List<Rule> list = new ArrayList<>();
        for (String s : interpreterMetadata.getValue().split("\n")) {
            String line = s.trim();
            Rule rule = this.parseItemCustomRule(item, line);
            if (rule != null) {
                list.add(rule);
            }
        }
        return list;
    }

    private interface CreateItemDescriptionRule {
        Rule itemDescriptionRule(List<String> allowedItemNames, Expression labeledCmd);
    }

    private record ItemStateCommandSupplier(String label,
            Map<Item, String> commandByItem) implements ItemCommandSupplier {
        @Override
        public @Nullable Command getItemCommand(Item item) {
            String textCommand = commandByItem.get(item);
            if (textCommand == null) {
                return null;
            }
            return TypeParser.parseCommand(item.getAcceptedCommandTypes(), textCommand);
        }

        @Override
        public String getCommandLabel() {
            return label;
        }

        @Override
        public List<Class<? extends Command>> getCommandClasses(@Nullable Item item) {
            if (item == null) {
                return commandByItem.keySet().stream()//
                        .flatMap(i -> i.getAcceptedCommandTypes().stream())//
                        .distinct()//
                        .collect(Collectors.toList());
            } else if (commandByItem.containsKey(item)) {
                return item.getAcceptedCommandTypes();
            } else {
                return List.of();
            }
        }
    }
}
