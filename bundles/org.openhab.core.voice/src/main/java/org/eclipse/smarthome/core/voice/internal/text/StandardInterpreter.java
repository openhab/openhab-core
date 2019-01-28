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
package org.eclipse.smarthome.core.voice.internal.text;

import java.util.Locale;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.NextPreviousType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.voice.text.AbstractRuleBasedInterpreter;
import org.eclipse.smarthome.core.voice.text.Expression;
import org.eclipse.smarthome.core.voice.text.HumanLanguageInterpreter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * A human language command interpretation service.
 *
 * @author Tilman Kamp - Initial contribution and API
 * @author Kai Kreuzer - Added further German interpretation rules
 * @author Laurent Garnier - Added French interpretation rules
 *
 */
@Component(service = HumanLanguageInterpreter.class)
public class StandardInterpreter extends AbstractRuleBasedInterpreter {

    @Override
    public void createRules() {
        /****************************** ENGLISH ******************************/

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

                itemRule(seq(cmd(alt("dim", "decrease", "lower", "soften"), IncreaseDecreaseType.DECREASE), the) /*
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
                                alt(cmd("next", NextPreviousType.NEXT), cmd("previous", NextPreviousType.PREVIOUS)))),

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

        /****************************** GERMAN ******************************/

        Expression einAnAus = alt(cmd("ein", OnOffType.ON), cmd("an", OnOffType.ON), cmd("aus", OnOffType.OFF));
        Expression denDieDas = opt(alt("den", "die", "das"));
        Expression schalte = alt("schalt", "schalte", "mach");
        Expression pause = alt("pause", "stoppe");
        Expression mache = alt("mach", "mache", "fahre");
        Expression spiele = alt("spiele", "spiel", "starte");
        Expression zu = alt("zu", "zum", "zur");
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

                itemRule(seq(schalte, denDieDas), /* item */ cmd(alt("heller", "mehr"), IncreaseDecreaseType.INCREASE)),

                /* ColorType */

                itemRule(seq(schalte, denDieDas), /* item */ seq(opt("auf"), farbe)),

                /* UpDownType */

                itemRule(seq(mache, denDieDas), /* item */ cmd("hoch", UpDownType.UP)),

                itemRule(seq(mache, denDieDas), /* item */ cmd("runter", UpDownType.DOWN)),

                /* NextPreviousType */

                itemRule("wechsle",
                        /* item */ seq(opt(zu),
                                alt(cmd(naechste, NextPreviousType.NEXT), cmd(vorherige, NextPreviousType.PREVIOUS)))),

                /* PlayPauseType */

                itemRule(seq(cmd(spiele, PlayPauseType.PLAY), the) /* item */),

                itemRule(seq(cmd(pause, PlayPauseType.PAUSE), the) /* item */)

        );

        /****************************** FRENCH ******************************/

        Expression allumer = alt("allumer", "démarrer", "activer");
        Expression eteindre = alt("éteindre", "stopper", "désactiver", "couper");
        Expression lela = opt(alt("le", "la", "les", "l"));
        Expression poursurdude = opt(alt("pour", "sur", "du", "de"));
        Expression couleur = alt(cmd("blanc", HSBType.WHITE), cmd("rose", HSBType.fromRGB(255, 96, 208)),
                cmd("jaune", HSBType.fromRGB(255, 224, 32)), cmd("orange", HSBType.fromRGB(255, 160, 16)),
                cmd("violet", HSBType.fromRGB(128, 0, 128)), cmd("rouge", HSBType.RED), cmd("vert", HSBType.GREEN),
                cmd("bleu", HSBType.BLUE));

        addRules(Locale.FRENCH,

                /* OnOffType */

                itemRule(seq(cmd(allumer, OnOffType.ON), lela) /* item */),
                itemRule(seq(cmd(eteindre, OnOffType.OFF), lela) /* item */),

                /* IncreaseDecreaseType */

                itemRule(seq(cmd("augmenter", IncreaseDecreaseType.INCREASE), lela) /* item */),
                itemRule(seq(cmd("diminuer", IncreaseDecreaseType.DECREASE), lela) /* item */),

                itemRule(seq(cmd("plus", IncreaseDecreaseType.INCREASE), "de") /* item */),
                itemRule(seq(cmd("moins", IncreaseDecreaseType.DECREASE), "de") /* item */),

                /* ColorType */

                itemRule(seq("couleur", couleur, opt("pour"), lela) /* item */),

                /* PlayPauseType */

                itemRule(seq(cmd("reprise", PlayPauseType.PLAY), "lecture", poursurdude, lela) /* item */),
                itemRule(seq(cmd("pause", PlayPauseType.PAUSE), "lecture", poursurdude, lela) /* item */),

                /* NextPreviousType */

                itemRule(
                        seq(alt("plage", "piste"),
                                alt(cmd("suivante", NextPreviousType.NEXT),
                                        cmd("précédente", NextPreviousType.PREVIOUS)),
                                poursurdude, lela) /* item */),

                /* UpDownType */

                itemRule(seq(cmd("monter", UpDownType.UP), lela) /* item */),
                itemRule(seq(cmd("descendre", UpDownType.DOWN), lela) /* item */),

                /* StopMoveType */

                itemRule(seq(cmd("arrêter", StopMoveType.STOP), lela) /* item */),
                itemRule(seq(cmd(alt("bouger", "déplacer"), StopMoveType.MOVE), lela) /* item */),

                /* RefreshType */

                itemRule(seq(cmd("rafraîchir", RefreshType.REFRESH), lela) /* item */)

        );
    }

    @Override
    public String getId() {
        return "system";
    }

    @Override
    public String getLabel(Locale locale) {
        return "Built-in Interpreter";
    }

    @Override
    @Reference
    public void setItemRegistry(ItemRegistry ItemRegistry) {
        super.setItemRegistry(ItemRegistry);
    }

    @Override
    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        super.unsetItemRegistry(itemRegistry);
    }

    @Override
    @Reference
    public void setEventPublisher(EventPublisher EventPublisher) {
        super.setEventPublisher(EventPublisher);
    }

    @Override
    public void unsetEventPublisher(EventPublisher eventPublisher) {
        super.unsetEventPublisher(eventPublisher);
    }

}
