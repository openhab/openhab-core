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
package org.openhab.core.voice.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A human language command interpretation service.
 *
 * @author Tilman Kamp - Initial contribution
 * @author Kai Kreuzer - Improved error handling
 */
public abstract class AbstractRuleBasedInterpreter implements HumanLanguageInterpreter {

    private static final String JSGF = "JSGF";
    private static final Set<String> SUPPORTED_GRAMMERS = Collections.unmodifiableSet(Collections.singleton(JSGF));

    private static final String OK = "ok";
    private static final String SORRY = "sorry";
    private static final String ERROR = "error";

    private static final String STATE_CURRENT = "state_current";
    private static final String STATE_ALREADY_SINGULAR = "state_already_singular";
    private static final String MULTIPLE_OBJECTS = "multiple_objects";
    private static final String NO_OBJECTS = "no_objects";
    private static final String COMMAND_NOT_ACCEPTED = "command_not_accepted";

    private static final String CMD = "cmd";
    private static final String NAME = "name";

    private static final String LANGUAGE_SUPPORT = "LanguageSupport";

    private Logger logger = LoggerFactory.getLogger(AbstractRuleBasedInterpreter.class);

    private Map<Locale, List<Rule>> languageRules;
    private Map<Locale, Set<String>> allItemTokens = null;
    private Map<Locale, Map<Item, List<Set<String>>>> itemTokens = null;

    private ItemRegistry itemRegistry;
    private EventPublisher eventPublisher;

    private RegistryChangeListener<Item> registryChangeListener = new RegistryChangeListener<Item>() {
        @Override
        public void added(Item element) {
            invalidate();
        }

        @Override
        public void removed(Item element) {
            invalidate();
        }

        @Override
        public void updated(Item oldElement, Item element) {
            invalidate();
        }
    };

    /**
     * Called whenever the rules are to be (re)generated and added by {@link addRules}
     */
    protected abstract void createRules();

    @Override
    public String interpret(Locale locale, String text) throws InterpretationException {
        ResourceBundle language = ResourceBundle.getBundle(LANGUAGE_SUPPORT, locale);
        Rule[] rules = getRules(locale);
        if (language == null || rules.length == 0) {
            throw new InterpretationException(
                    locale.getDisplayLanguage(Locale.ENGLISH) + " is not supported at the moment.");
        }
        TokenList tokens = new TokenList(tokenize(locale, text));
        if (tokens.eof()) {
            throw new InterpretationException(language.getString(SORRY));
        }
        InterpretationResult result;

        InterpretationResult lastResult = null;
        for (Rule rule : rules) {
            if ((result = rule.execute(language, tokens)).isSuccess()) {
                return result.getResponse();
            } else {
                if (result != InterpretationResult.SYNTAX_ERROR) {
                    lastResult = result;
                }
            }
        }
        if (lastResult == null) {
            throw new InterpretationException(language.getString(SORRY));
        } else {
            throw lastResult.getException();
        }
    }

    private void invalidate() {
        allItemTokens = null;
        itemTokens = null;
        languageRules = null;
    }

    /**
     * All the tokens (name parts) of the names of all the items in the {@link ItemRegistry}.
     *
     * @param locale The locale that is to be used for preparing the tokens.
     * @return the identifier tokens
     */
    Set<String> getAllItemTokens(Locale locale) {
        if (allItemTokens == null) {
            allItemTokens = new HashMap<>();
        }
        Set<String> localeTokens = allItemTokens.get(locale);
        if (localeTokens == null) {
            allItemTokens.put(locale, localeTokens = new HashSet<>());
            for (Item item : itemRegistry.getAll()) {
                localeTokens.addAll(tokenize(locale, item.getLabel()));
            }
        }
        return localeTokens;
    }

    /**
     * Retrieves the list of identifier token sets per item currently contained in the {@link ItemRegistry}.
     * Each item entry in the resulting hash map will feature a list of different token sets. Each token set
     * represents one possible way "through" a chain of parent groups, where each groups tokenized name is
     * part of the set.
     *
     * @param locale The locale that is to be used for preparing the tokens.
     * @return the list of identifier token sets per item
     */
    Map<Item, List<Set<String>>> getItemTokens(Locale locale) {
        if (itemTokens == null) {
            itemTokens = new HashMap<>();
        }
        Map<Item, List<Set<String>>> localeTokens = itemTokens.get(locale);
        if (localeTokens == null) {
            itemTokens.put(locale, localeTokens = new HashMap<>());
            for (Item item : itemRegistry.getItems()) {
                if (item.getGroupNames().isEmpty()) {
                    addItem(locale, localeTokens, new HashSet<>(), item);
                }
            }
        }
        return localeTokens;
    }

    private void addItem(Locale locale, Map<Item, List<Set<String>>> target, Set<String> tokens, Item item) {
        Set<String> nt = new HashSet<>(tokens);
        nt.addAll(tokenize(locale, item.getLabel()));
        List<Set<String>> list = target.get(item);
        if (list == null) {
            target.put(item, list = new ArrayList<>());
        }
        list.add(nt);
        if (item instanceof GroupItem) {
            for (Item member : ((GroupItem) item).getMembers()) {
                addItem(locale, target, nt, member);
            }
        }
    }

    /**
     * Creates an item name placeholder expression. This expression is greedy: Only use it, if there are no other
     * expressions following this one.
     * It's safer to use {@link thingRule} instead.
     *
     * @return Expression that represents a name of an item.
     */
    protected Expression name() {
        return name(null);
    }

    /**
     * Creates an item name placeholder expression. This expression is greedy: Only use it, if you are able to pass in
     * all possible stop tokens as excludes.
     * It's safer to use {@link thingRule} instead.
     *
     * @param stopper Stop expression that, if matching, will stop this expression from consuming further tokens.
     * @return Expression that represents a name of an item.
     */
    protected Expression name(Expression stopper) {
        return tag(NAME, star(new ExpressionIdentifier(this, stopper)));
    }

    private Map<Locale, List<Rule>> getLanguageRules() {
        if (languageRules == null) {
            languageRules = new HashMap<>();
            createRules();
        }
        return languageRules;
    }

    /**
     * Retrieves all {@link Rule}s to a given {@link Locale}. It also retrieves all the same-language rules into greater
     * indexes of the array (lower match priority).
     *
     * @param locale Locale filter
     * @return Rules in descending match priority order.
     */
    public Rule[] getRules(Locale locale) {
        Map<Locale, List<Rule>> lr = getLanguageRules();
        List<Rule> rules = new ArrayList<>();
        Set<List<Rule>> ruleSets = new HashSet<>();
        List<Rule> ruleSet = lr.get(locale);
        if (ruleSet != null) {
            ruleSets.add(ruleSet);
            rules.addAll(ruleSet);
        }

        String l = locale.getLanguage();
        for (Locale rl : lr.keySet()) {
            if (rl.getLanguage().equals(l)) {
                ruleSet = lr.get(rl);
                if (!ruleSets.contains(ruleSet)) {
                    ruleSets.add(ruleSet);
                    rules.addAll(ruleSet);
                }
            }
        }
        return rules.toArray(new Rule[0]);
    }

    /**
     * Adds {@link Locale} specific rules to this interpreter. To be called from within {@link createRules}.
     *
     * @param locale Locale of the rules.
     * @param rules Rules to add.
     */
    protected void addRules(Locale locale, Rule... rules) {
        List<Rule> ruleSet = languageRules.get(locale);
        if (ruleSet == null) {
            languageRules.put(locale, ruleSet = new ArrayList<>());
        }
        for (Rule rule : rules) {
            ruleSet.add(rule);
        }
    }

    /**
     * Creates an item rule on base of an expression, where the tail of the new rule's expression will consist of an
     * item
     * name expression.
     *
     * @param headExpression The head expression that should contain at least one {@link cmd} generated expression. The
     *            corresponding {@link Command} will in case of a match be sent to the matching {@link Item}.
     * @return The created rule.
     */
    protected Rule itemRule(Object headExpression) {
        return itemRule(headExpression, null);
    }

    /**
     * Creates an item rule on base of a head and a tail expression, where the middle part of the new rule's expression
     * will consist of an item
     * name expression. Either the head expression or the tail expression should contain at least one {@link cmd}
     * generated expression.
     *
     * @param headExpression The head expression.
     * @param tailExpression The tail expression.
     * @return The created rule.
     */
    protected Rule itemRule(Object headExpression, Object tailExpression) {
        Expression tail = exp(tailExpression);
        Expression expression = tail == null ? seq(headExpression, name()) : seq(headExpression, name(tail), tail);
        return new Rule(expression) {
            @Override
            public InterpretationResult interpretAST(ResourceBundle language, ASTNode node) {
                String[] name = node.findValueAsStringArray(NAME);
                ASTNode cmdNode = node.findNode(CMD);
                Object tag = cmdNode.getTag();
                Object value = cmdNode.getValue();
                Command command;
                if (tag instanceof Command) {
                    command = (Command) tag;
                } else if (value instanceof Number) {
                    command = new DecimalType(((Number) value).longValue());
                } else {
                    command = new StringType(cmdNode.getValueAsString());
                }
                if (name != null && command != null) {
                    try {
                        return new InterpretationResult(true, executeSingle(language, name, command));
                    } catch (InterpretationException ex) {
                        return new InterpretationResult(ex);
                    }
                }
                return InterpretationResult.SEMANTIC_ERROR;
            }
        };
    }

    /**
     * Converts an object to an expression. Objects that are already instances of {@link Expression} are just returned.
     * All others are converted to {@link match} expressions.
     *
     * @param obj the object that's to be converted
     * @return resulting expression
     */
    protected Expression exp(Object obj) {
        if (obj instanceof Expression) {
            return (Expression) obj;
        } else {
            return obj == null ? null : new ExpressionMatch(obj.toString());
        }
    }

    /**
     * Converts all parameters to an expression array. Objects that are already instances of {@link Expression} are not
     * touched.
     * All others are converted to {@link match} expressions.
     *
     * @param obj the objects that are to be converted
     * @return resulting expression array
     */
    protected Expression[] exps(Object... objects) {
        List<Expression> result = new ArrayList<>();
        for (int i = 0; i < objects.length; i++) {
            Expression e = exp(objects[i]);
            if (e != null) {
                result.add(e);
            }
        }
        return result.toArray(new Expression[0]);
    }

    /**
     * Adds a name to the resulting AST tree, if the given expression matches.
     *
     * @param name name to add
     * @param expression the expression that has to match
     * @return resulting expression
     */
    protected Expression tag(String name, Object expression) {
        return tag(name, expression, null);
    }

    /**
     * Adds a value to the resulting AST tree, if the given expression matches.
     *
     * @param expression the expression that has to match
     * @param tag the tag that's to be set
     * @return resulting expression
     */
    protected Expression tag(Object expression, Object tag) {
        return tag(null, expression, tag);
    }

    /**
     * Adds a name and a tag to the resulting AST tree, if the given expression matches.
     *
     * @param name name to add
     * @param expression the expression that has to match
     * @param tag the tag that's to be set
     * @return resulting expression
     */
    protected Expression tag(String name, Object expression, Object tag) {
        return new ExpressionLet(name, exp(expression), null, tag);
    }

    /**
     * Adds a command to the resulting AST tree. If the expression evaluates to a
     * numeric value, it will get a {@link DecimalType}, otherwise a {@link StringType}.
     *
     * @param expression the expression that has to match
     * @return resulting expression
     */
    protected Expression cmd(Object expression) {
        return cmd(expression, null);
    }

    /**
     * Adds a command to the resulting AST tree, if the expression matches.
     *
     * @param expression the expression that has to match
     * @param command the command that should be added
     * @return resulting expression
     */
    protected Expression cmd(Object expression, Command command) {
        return tag(CMD, expression, command);
    }

    /**
     * Creates an alternatives expression. Matches, as soon as one of the given expressions matches. They are tested in
     * the provided order. The value of the matching expression will be used for the resulting nodes's value.
     *
     * @param expressions the expressions (alternatives) that are to be tested
     * @return resulting expression
     */
    protected ExpressionAlternatives alt(Object... expressions) {
        return new ExpressionAlternatives(exps(expressions));
    }

    /**
     * Creates a sequence expression. Matches, if all the given expressions match. They are tested in
     * the provided order. The resulting nodes's value will be an {link Object[]} that contains all values of the
     * matching expressions.
     *
     * @param expressions the expressions (alternatives) that have to match in sequence
     * @return resulting expression
     */
    protected ExpressionSequence seq(Object... expressions) {
        return new ExpressionSequence(exps(expressions));
    }

    /**
     * Creates an optional expression. Always succeeds. The resulting nodes's value will be the one of the
     * matching expression or null.
     *
     * @param expression the optionally matching expression
     * @return resulting expression
     */
    protected ExpressionCardinality opt(Object expression) {
        return new ExpressionCardinality(exp(expression), false, true);
    }

    /**
     * Creates a repeating expression that will match the given expression as often as possible. Always succeeds. The
     * resulting node's value will be an {link Object[]} that contains all values of the
     * matches.
     *
     * @param expression the repeating expression
     * @return resulting expression
     */
    protected ExpressionCardinality star(Object expression) {
        return new ExpressionCardinality(exp(expression), false, false);
    }

    /**
     * Creates a repeating expression that will match the given expression as often as possible. Only succeeds, if there
     * is at least one match. The resulting node's value will be an {link Object[]} that contains all values of the
     * matches.
     *
     * @param expression the repeating expression
     * @return resulting expression
     */
    protected ExpressionCardinality plus(Object expression) {
        return new ExpressionCardinality(exp(expression), true, false);
    }

    /**
     * Executes a command on one item that's to be found in the item registry by given name fragments.
     * Fails, if there is more than on item.
     *
     * @param language resource bundle used for producing localized response texts
     * @param labelFragments label fragments that are used to match an item's label.
     *            For a positive match, the item's label has to contain every fragment - independently of their order.
     *            They are treated case insensitive.
     * @param command command that should be executed
     * @return response text
     * @throws InterpretationException in case that there is no or more than on item matching the fragments
     */
    protected String executeSingle(ResourceBundle language, String[] labelFragments, Command command)
            throws InterpretationException {
        List<Item> items = getMatchingItems(language, labelFragments, command.getClass());
        if (items.size() < 1) {
            if (getMatchingItems(language, labelFragments, null).size() >= 1) {
                throw new InterpretationException(
                        language.getString(COMMAND_NOT_ACCEPTED).replace("<cmd>", command.toString()));
            } else {
                throw new InterpretationException(language.getString(NO_OBJECTS));
            }
        } else if (items.size() > 1) {
            throw new InterpretationException(language.getString(MULTIPLE_OBJECTS));
        } else {
            Item item = items.get(0);
            if (command instanceof State) {
                try {
                    State newState = (State) command;
                    State oldState = item.getStateAs(newState.getClass());
                    if (oldState.equals(newState)) {
                        String template = language.getString(STATE_ALREADY_SINGULAR);
                        String cmdName = "state_" + command.toString().toLowerCase();
                        String stateText = null;
                        try {
                            stateText = language.getString(cmdName);
                        } catch (Exception e) {
                            stateText = language.getString(STATE_CURRENT);
                        }
                        return template.replace("<state>", stateText);
                    }
                } catch (Exception ex) {
                    logger.debug("Failed constructing response: {}", ex.getMessage());
                    return language.getString(ERROR);
                }
            }
            eventPublisher.post(ItemEventFactory.createCommandEvent(item.getName(), command));
            return language.getString(OK);
        }
    }

    /**
     * Filters the item registry by matching each item's name with the provided name fragments.
     * The item's label and its parent group's labels are tokenized {@link tokenize} and then altogether looked up
     * by each and every provided fragment.
     * For the item to get included into the result list, every provided fragment has to be found among the label
     * tokens.
     * If a command type is provided, the item also has to support it.
     * In case of channels and their owners being ambiguous due to sharing most of the label sequence, only the top
     * most item with support for the given command type is kept.
     *
     * @param language Language information that is used for matching
     * @param labelFragments label fragments that are used to match an item's label.
     *            For a positive match, the item's label has to contain every fragment - independently of their order.
     *            They are treated case insensitive.
     * @param commandType optional command type that all items have to support.
     *            Provide {null} if there is no need for a certain command to be supported.
     * @return All matching items from the item registry.
     */
    protected List<Item> getMatchingItems(ResourceBundle language, String[] labelFragments, Class<?> commandType) {
        List<Item> items = new ArrayList<>();
        Map<Item, List<Set<String>>> map = getItemTokens(language.getLocale());
        for (Item item : map.keySet()) {
            for (Set<String> parts : map.get(item)) {
                boolean allMatch = true;
                for (String fragment : labelFragments) {
                    if (!parts.contains(fragment.toLowerCase(language.getLocale()))) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    if (commandType == null || item.getAcceptedCommandTypes().contains(commandType)) {
                        String name = item.getName();
                        boolean insert = true;
                        for (Item si : items) {
                            if (name.startsWith(si.getName())) {
                                insert = false;
                            }
                        }
                        if (insert) {
                            for (int i = 0; i < items.size(); i++) {
                                Item si = items.get(i);
                                if (si.getName().startsWith(name)) {
                                    items.remove(i);
                                    i--;
                                }
                            }
                            items.add(item);
                        }
                    }
                }
            }
        }
        return items;
    }

    /**
     * Tokenizes text. Filters out all unsupported punctuation. Tokens will be lower case.
     *
     * @param locale the locale that should be used for lower casing
     * @param text the text that should be tokenized
     * @return resulting tokens
     */
    protected List<String> tokenize(Locale locale, String text) {
        final Locale localeSafe = locale != null ? locale : Locale.getDefault();
        List<String> parts = new ArrayList<>();
        if (text == null) {
            return parts;
        }
        String[] split;
        if (Locale.FRENCH.getLanguage().equalsIgnoreCase(localeSafe.getLanguage())) {
            split = text.toLowerCase(localeSafe).replaceAll("[\\']", " ").replaceAll("[^\\w\\sàâäçéèêëîïôùûü]", " ")
                    .split("\\s");
        } else {
            split = text.toLowerCase(localeSafe).replaceAll("[\\']", "").replaceAll("[^\\w\\s]", " ").split("\\s");
        }
        for (int i = 0; i < split.length; i++) {
            String part = split[i].trim();
            if (part.length() > 0) {
                parts.add(part);
            }
        }
        return parts;
    }

    @Override
    public Set<Locale> getSupportedLocales() {
        return Collections.unmodifiableSet(getLanguageRules().keySet());
    }

    @Override
    public Set<String> getSupportedGrammarFormats() {
        return SUPPORTED_GRAMMERS;
    }

    /**
     * Helper class to generate a JSGF grammar from the rules of the interpreter.
     *
     * @author Tilman Kamp - Initial contribution
     *
     */
    private class JSGFGenerator {

        private ResourceBundle language;

        private Map<Expression, Integer> ids = new HashMap<>();
        private Set<Expression> exported = new HashSet<>();
        private Set<Expression> shared = new HashSet<>();
        private int counter = 0;

        private Set<String> identifierExcludes = new HashSet<>();
        private Set<String> identifiers = new HashSet<>();

        private StringBuilder builder = new StringBuilder();

        JSGFGenerator(ResourceBundle language) {
            this.language = language;
        }

        private void addChildren(Expression exp) {
            for (Expression se : exp.getChildExpressions()) {
                addExpression(se);
            }
        }

        private int addExpression(Expression exp) {
            if (ids.containsKey(exp)) {
                shared.add(exp);
                return ids.get(exp);
            } else {
                int id = counter++;
                ids.put(exp, id);
                addChildren(exp);
                return id;
            }
        }

        private int addExportedExpression(Expression exp) {
            shared.add(exp);
            exported.add(exp);
            int id = addExpression(exp);
            return id;
        }

        private Expression unwrapLet(Expression expression) {
            Expression exp = expression;
            while (exp instanceof ExpressionLet) {
                exp = ((ExpressionLet) expression).getSubExpression();
            }
            return exp;
        }

        private void emit(Object obj) {
            builder.append(obj);
        }

        private void emitName(Expression expression) {
            emit("r");
            emit(ids.get(unwrapLet(expression)));
        }

        private void emitReference(Expression expression) {
            emit("<");
            emitName(expression);
            emit(">");
        }

        private void emitDefinition(Expression expression) {
            if (exported.contains(expression)) {
                emit("public ");
            }
            emit("<");
            emitName(expression);
            emit("> = ");
            emitExpression(expression);
            emit(";\n\n");
        }

        private void emitUse(Expression expression) {
            if (shared.contains(expression)) {
                emitReference(expression);
            } else {
                emitExpression(expression);
            }
        }

        private void emitExpression(Expression expression) {
            Expression unwrappedExpression = unwrapLet(expression);
            if (unwrappedExpression instanceof ExpressionMatch) {
                emitMatchExpression((ExpressionMatch) unwrappedExpression);
            } else if (unwrappedExpression instanceof ExpressionSequence) {
                emitSequenceExpression((ExpressionSequence) unwrappedExpression);
            } else if (unwrappedExpression instanceof ExpressionAlternatives) {
                emitAlternativesExpression((ExpressionAlternatives) unwrappedExpression);
            } else if (unwrappedExpression instanceof ExpressionCardinality) {
                emitCardinalExpression((ExpressionCardinality) unwrappedExpression);
            } else if (unwrappedExpression instanceof ExpressionIdentifier) {
                emitItemIdentifierExpression((ExpressionIdentifier) unwrappedExpression);
            }
        }

        private void emitMatchExpression(ExpressionMatch expression) {
            emit(expression.getPattern());
        }

        private void emitSequenceExpression(ExpressionSequence expression) {
            emitGroup(" ", expression.getChildExpressions());
        }

        private void emitAlternativesExpression(ExpressionAlternatives expression) {
            emitGroup(" | ", expression.getChildExpressions());
        }

        private void emitCardinalExpression(ExpressionCardinality expression) {
            if (!expression.isAtLeastOne() && !expression.isAtMostOne()) {
                emitUse(expression.getSubExpression());
                emit("*");
            } else if (expression.isAtLeastOne()) {
                emitUse(expression.getSubExpression());
                emit("+");
            } else if (expression.isAtMostOne()) {
                emit("[");
                emitUse(expression.getSubExpression());
                emit("]");
            } else {
                emitUse(expression.getSubExpression());
            }
        }

        private void emitItemIdentifierExpression(ExpressionIdentifier expression) {
            Set<String> remainder = new HashSet<>(identifierExcludes);
            Expression stopper = expression.getStopper();
            Set<String> excludes = stopper == null ? new HashSet<>() : stopper.getFirsts(language);
            if (!excludes.isEmpty()) {
                remainder.removeAll(excludes);
                if (!remainder.isEmpty()) {
                    emit("(");
                }
                emit("<idbase>");
                for (String token : remainder) {
                    emit(" | ");
                    emit(token);
                }
                if (!remainder.isEmpty()) {
                    emit(")");
                }
            } else {
                emit("<idpart>");
            }
        }

        private void emitGroup(String separator, List<Expression> expressions) {
            int l = expressions.size();
            if (l > 0) {
                emit("(");
            }
            for (int i = 0; i < l; i++) {
                if (i > 0) {
                    emit(separator);
                }
                emitUse(expressions.get(i));
            }
            if (l > 0) {
                emit(")");
            }
        }

        private void emitSet(Set<String> set, String separator) {
            boolean sep = false;
            for (String p : set) {
                if (sep) {
                    emit(separator);
                } else {
                    sep = true;
                }
                emit(p);
            }
        }

        String getGrammar() {
            Rule[] rules = getRules(language.getLocale());
            identifiers.addAll(getAllItemTokens(language.getLocale()));
            for (Rule rule : rules) {
                Expression e = rule.getExpression();
                addExportedExpression(e);
            }
            for (Expression e : ids.keySet()) {
                if (e instanceof ExpressionIdentifier) {
                    Expression stopper = ((ExpressionIdentifier) e).getStopper();
                    if (stopper != null) {
                        identifierExcludes.addAll(stopper.getFirsts(language));
                    }
                }
            }

            emit("#JSGF V1.0;\n\n");

            if (!identifierExcludes.isEmpty()) {
                Set<String> identifierBase = new HashSet<>(identifiers);
                identifierBase.removeAll(identifierExcludes);
                emit("<idbase> = ");
                emitSet(identifierBase, " | ");
                emit(";\n\n<idpart> = <idbase> | ");
                emitSet(identifierExcludes, " | ");
                emit(";\n\n");
            } else {
                emit("<idpart> = ");
                emitSet(identifiers, " | ");
                emit(";\n\n");
            }

            for (Expression e : shared) {
                emitDefinition(e);
            }
            String grammar = builder.toString();
            return grammar;
        }

    }

    @Override
    public String getGrammar(Locale locale, String format) {
        if (!JSGF.equals(format)) {
            return null;
        }
        JSGFGenerator generator = new JSGFGenerator(ResourceBundle.getBundle(LANGUAGE_SUPPORT, locale));
        return generator.getGrammar();
    }

    public void setItemRegistry(ItemRegistry itemRegistry) {
        if (this.itemRegistry == null) {
            this.itemRegistry = itemRegistry;
            this.itemRegistry.addRegistryChangeListener(registryChangeListener);
        }
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        if (itemRegistry == this.itemRegistry) {
            this.itemRegistry.removeRegistryChangeListener(registryChangeListener);
            this.itemRegistry = null;
        }
    }

    public void setEventPublisher(EventPublisher eventPublisher) {
        if (this.eventPublisher == null) {
            this.eventPublisher = eventPublisher;
        }
    }

    public void unsetEventPublisher(EventPublisher eventPublisher) {
        if (eventPublisher == this.eventPublisher) {
            this.eventPublisher = null;
        }
    }

}
