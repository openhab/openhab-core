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
package org.openhab.core.voice.text;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.openhab.core.voice.DialogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A human language command interpretation service.
 *
 * @author Tilman Kamp - Initial contribution
 * @author Kai Kreuzer - Improved error handling
 * @author Miguel Álvarez - Reduce collisions on exact match and use item synonyms
 * @author Miguel Álvarez - Reduce collisions using dialog location
 */
@NonNullByDefault
public abstract class AbstractRuleBasedInterpreter implements HumanLanguageInterpreter {

    private static final String JSGF = "JSGF";
    private static final Set<String> SUPPORTED_GRAMMERS = Set.of(JSGF);

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
    private static final String VALUE = "name";

    private static final String LANGUAGE_SUPPORT = "LanguageSupport";

    private static final String SYNONYMS_NAMESPACE = "synonyms";

    private final MetadataRegistry metadataRegistry;

    private Logger logger = LoggerFactory.getLogger(AbstractRuleBasedInterpreter.class);

    private final Map<Locale, List<Rule>> languageRules = new HashMap<>();
    private final Map<Locale, Set<String>> allItemTokens = new HashMap<>();
    private final Map<Locale, Map<Item, ItemInterpretationMetadata>> itemTokens = new HashMap<>();

    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;

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
    private RegistryChangeListener<Metadata> synonymsChangeListener = new RegistryChangeListener<Metadata>() {
        @Override
        public void added(Metadata element) {
            invalidateIfSynonymsMetadata(element);
        }

        @Override
        public void removed(Metadata element) {
            invalidateIfSynonymsMetadata(element);
        }

        @Override
        public void updated(Metadata oldElement, Metadata element) {
            invalidateIfSynonymsMetadata(element);
        }

        private void invalidateIfSynonymsMetadata(Metadata metadata) {
            if (metadata.getUID().getNamespace().equals(SYNONYMS_NAMESPACE)) {
                invalidate();
            }
        }
    };

    public AbstractRuleBasedInterpreter(final EventPublisher eventPublisher, final ItemRegistry itemRegistry,
            final MetadataRegistry metadataRegistry) {
        this.eventPublisher = eventPublisher;
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
        itemRegistry.addRegistryChangeListener(registryChangeListener);
        metadataRegistry.addRegistryChangeListener(synonymsChangeListener);
    }

    protected void deactivate() {
        itemRegistry.removeRegistryChangeListener(registryChangeListener);
    }

    /**
     * Called whenever the rules are to be (re)generated and added by {@link #addRules}
     */
    protected abstract void createRules(@Nullable Locale locale);

    @Override
    public String interpret(Locale locale, String text) throws InterpretationException {
        return interpret(locale, text, null);
    }

    @Override
    public String interpret(Locale locale, String text, @Nullable DialogContext dialogContext)
            throws InterpretationException {
        ResourceBundle language = ResourceBundle.getBundle(LANGUAGE_SUPPORT, locale);
        Rule[] rules = getRules(locale);
        if (rules.length == 0) {
            throw new InterpretationException(
                    locale.getDisplayLanguage(Locale.ENGLISH) + " is not supported at the moment.");
        }
        TokenList tokens = new TokenList(tokenize(locale, text));
        if (tokens.eof()) {
            throw new InterpretationException(language.getString(SORRY));
        }
        InterpretationResult result;

        InterpretationResult lastResult = null;
        String locationItem = dialogContext != null ? dialogContext.locationItem() : null;
        for (Rule rule : rules) {
            if ((result = rule.execute(language, tokens, locationItem)).isSuccess()) {
                return result.getResponse();
            } else {
                if (!InterpretationResult.SYNTAX_ERROR.equals(result)) {
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
        allItemTokens.clear();
        itemTokens.clear();
        languageRules.clear();
    }

    /**
     * All the tokens (name parts) of the names of all the items in the {@link ItemRegistry}.
     *
     * @param locale The locale that is to be used for preparing the tokens.
     * @return the identifier tokens
     */
    Set<String> getAllItemTokens(Locale locale) {
        Set<String> localeTokens = allItemTokens.get(locale);
        if (localeTokens == null) {
            allItemTokens.put(locale, localeTokens = new HashSet<>());
            for (Item item : itemRegistry.getAll()) {
                localeTokens.addAll(tokenize(locale, item.getLabel()));
                for (String synonym : getItemSynonyms(item)) {
                    localeTokens.addAll(tokenize(locale, synonym));
                }
            }
        }
        return localeTokens;
    }

    /**
     * Retrieves the list of identifier token sets per item currently contained in the {@link ItemRegistry}.
     * Each item entry in the resulting hash map will feature a list of different token lists. Each list
     * represents one possible way "through" a chain of parent groups, where each groups name is tokenized
     * into a list of strings and included as a member of the chain. Item synonym metadata options are
     * used as alternative labels creating new alternative chains for the item.
     *
     * @param locale The locale that is to be used for preparing the tokens.
     * @return the list of identifier token sets per item
     */
    Map<Item, ItemInterpretationMetadata> getItemTokens(Locale locale) {
        Map<Item, ItemInterpretationMetadata> localeTokens = itemTokens.get(locale);
        if (localeTokens == null) {
            itemTokens.put(locale, localeTokens = new HashMap<>());
            for (Item item : itemRegistry.getItems()) {
                if (item.getGroupNames().isEmpty()) {
                    addItem(locale, localeTokens, new ArrayList<>(), item, new ArrayList<>());
                }
            }
        }
        return localeTokens;
    }

    private String[] getItemSynonyms(Item item) {
        MetadataKey key = new MetadataKey("synonyms", item.getName());
        Metadata synonymsMetadata = metadataRegistry.get(key);
        return (synonymsMetadata != null) ? synonymsMetadata.getValue().split(",") : new String[] {};
    }

    private void addItem(Locale locale, Map<Item, ItemInterpretationMetadata> target, List<List<String>> tokens,
            Item item, ArrayList<String> locationParentNames) {
        addItem(locale, target, tokens, item, item.getLabel(), locationParentNames);
        for (String synonym : getItemSynonyms(item)) {
            addItem(locale, target, tokens, item, synonym, locationParentNames);
        }
    }

    private void addItem(Locale locale, Map<Item, ItemInterpretationMetadata> target, List<List<String>> tokens,
            Item item, @Nullable String itemLabel, ArrayList<String> locationParentNames) {
        List<List<String>> nt = new ArrayList<>(tokens);
        nt.add(tokenize(locale, itemLabel));
        ItemInterpretationMetadata metadata = target.computeIfAbsent(item, k -> new ItemInterpretationMetadata());
        metadata.pathToItem.add(nt);
        metadata.locationParentNames.addAll(locationParentNames);
        if (item instanceof GroupItem groupItem) {
            if (item.hasTag(CoreItemFactory.LOCATION)) {
                locationParentNames.add(item.getName());
            }
            for (Item member : groupItem.getMembers()) {
                addItem(locale, target, nt, member, locationParentNames);
            }
        }
    }

    /**
     * Creates an item name placeholder expression. This expression is greedy: Only use it, if there are no other
     * expressions following this one.
     * It's safer to use {@link #itemRule} instead.
     *
     * @return Expression that represents a name of an item.
     */
    protected Expression name() {
        return name(null);
    }

    /**
     * Creates an item name placeholder expression. This expression is greedy: Only use it, if you are able to pass in
     * all possible stop tokens as excludes.
     * It's safer to use {@link #itemRule} instead.
     *
     * @param stopper Stop expression that, if matching, will stop this expression from consuming further tokens.
     * @return Expression that represents a name of an item.
     */
    protected Expression name(@Nullable Expression stopper) {
        return tag(NAME, star(new ExpressionIdentifier(this, stopper)));
    }

    /**
     * Creates an item value placeholder expression. This expression is greedy: Only use it, if you are able to pass in
     * all possible stop tokens as excludes.
     * It's safer to use {@link #itemRule} instead.
     *
     * @param stopper Stop expression that, if matching, will stop this expression from consuming further tokens.
     * @return Expression that represents a name of an item.
     */
    private Expression value(@Nullable Expression stopper) {
        return tag(VALUE, star(new ExpressionIdentifier(this, stopper)));
    }

    private @Nullable List<@NonNull Rule> getLanguageRules(@Nullable Locale locale) {
        if (!languageRules.containsKey(locale)) {
            createRules(locale);
        }
        return languageRules.get(locale);
    }

    /**
     * Retrieves all {@link Rule}s to a given {@link Locale}. It also retrieves all the same-language rules into greater
     * indexes of the array (lower match priority).
     *
     * @param locale Locale filter
     * @return Rules in descending match priority order.
     */
    public Rule[] getRules(Locale locale) {
        List<Rule> rules = new ArrayList<>();
        Set<List<Rule>> ruleSets = new HashSet<>();
        List<Rule> ruleSet = getLanguageRules(locale);
        if (ruleSet != null) {
            ruleSets.add(ruleSet);
            rules.addAll(ruleSet);
        }
        String language = locale.getLanguage();
        for (Entry<Locale, List<Rule>> entry : languageRules.entrySet()) {
            Locale ruleLocale = entry.getKey();
            if (ruleLocale.getLanguage().equals(language)) {
                ruleSet = entry.getValue();
                if (!ruleSets.contains(ruleSet)) {
                    ruleSets.add(ruleSet);
                    rules.addAll(ruleSet);
                }
            }
        }
        return rules.toArray(new Rule[0]);
    }

    /**
     * Adds {@link Locale} specific rules to this interpreter. To be called from within {@link #createRules}.
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
     * @param headExpression The head expression that should contain at least one {@link #cmd} generated expression. The
     *            corresponding {@link Command} will in case of a match be sent to the matching {@link Item}.
     * @return The created rule.
     */
    protected Rule itemRule(Object headExpression) {
        return itemRule(headExpression, null);
    }

    /**
     * Creates an item rule on base of a head and a tail expression, where the middle part of the new rule's expression
     * will consist of an item
     * name expression. Either the head expression or the tail expression should contain at least one {@link #cmd}
     * generated expression.
     *
     * @param headExpression The head expression.
     * @param tailExpression The tail expression.
     * @return The created rule.
     */
    protected Rule itemRule(Object headExpression, @Nullable Object tailExpression) {
        return restrictedItemRule(List.of(), headExpression, tailExpression);
    }

    /**
     * Creates an item rule on base of a head and a tail expression, where the middle part of the new rule's ex ression
     * will consist of an item
     * name expression. Either the head expression or the tail expression should contain at least one {@link #cmd}
     * generated expression.
     * Rule will be restricted to the provided item names if any.
     *
     * @param allowedItemNames List of allowed item names, empty for disabled.
     * @param headExpression The head expression.
     * @param tailExpression The tail expression.
     * @return The created rule.
     */
    protected Rule restrictedItemRule(List<String> allowedItemNames, Object headExpression,
            @Nullable Object tailExpression) {
        Expression tail = exp(tailExpression);
        Expression expression = tail == null ? seq(headExpression, name()) : seq(headExpression, name(tail), tail);
        return new Rule(expression, allowedItemNames) {
            @Override
            public InterpretationResult interpretAST(ResourceBundle language, ASTNode node,
                    InterpretationContext context) {
                String[] name = node.findValueAsStringArray(NAME);
                ASTNode cmdNode = node.findNode(CMD);
                Object tag = cmdNode.getTag();
                Object value = cmdNode.getValue();
                ItemCommandSupplier commandSupplier;
                if (tag instanceof ItemCommandSupplier supplier) {
                    commandSupplier = supplier;
                } else if (value instanceof Number number) {
                    commandSupplier = new SingleCommandSupplier(new DecimalType(number.longValue()));
                } else {
                    commandSupplier = new SingleCommandSupplier(new StringType(cmdNode.getValueAsString()));
                }
                if (name != null) {
                    try {
                        return new InterpretationResult(true, executeSingle(language, name, commandSupplier, context));
                    } catch (InterpretationException ex) {
                        return new InterpretationResult(ex);
                    }
                }
                return InterpretationResult.SEMANTIC_ERROR;
            }
        };
    }

    /**
     * Creates a custom rule on base of a head and a tail expression, where the middle part of the new rule's
     * expression
     * will consist of a free command to be captured. Either the head expression or the tail expression should contain
     * at least one {@link #cmd}
     * generated expression.
     * Rule will be restricted to the provided item name.
     *
     * @param item Item target
     * @param headExpression The head expression.
     * @param tailExpression The tail expression.
     * @return The created rule.
     */
    protected Rule customItemRule(Item item, Object headExpression, @Nullable Object tailExpression) {
        Expression tail = exp(tailExpression);
        Expression expression = tail == null ? seq(headExpression, value(null))
                : seq(headExpression, value(tail), tail);

        HashMap<String, String> valuesByLabel = new HashMap<>();
        var stateDescription = item.getStateDescription();
        if (stateDescription != null) {
            stateDescription.getOptions().forEach(op -> {
                String label = op.getLabel();
                if (label != null) {
                    valuesByLabel.put(label, op.getValue());
                }
            });
        }
        var commandDesc = item.getCommandDescription();
        if (commandDesc != null) {
            commandDesc.getCommandOptions().forEach(op -> {
                String label = op.getLabel();
                if (label != null) {
                    valuesByLabel.put(label, op.getCommand());
                }
            });
        }
        return new Rule(expression, List.of(item.getName())) {
            @Override
            public InterpretationResult interpretAST(ResourceBundle language, ASTNode node,
                    InterpretationContext context) {
                String[] commandParts = node.findValueAsStringArray(VALUE);
                if (commandParts != null && commandParts.length > 0) {
                    try {
                        return new InterpretationResult(true,
                                executeCustom(language, item, String.join(" ", commandParts).trim(), valuesByLabel));
                    } catch (InterpretationException ex) {
                        return new InterpretationResult(ex);
                    }
                }
                return InterpretationResult.SEMANTIC_ERROR;
            }
        };
    }

    /**
     * Converts an object to an expression.
     * Objects that are already instances of {@link Expression} are just returned.
     * All others are converted to {@link Expression}.
     *
     * @param obj the object that's to be converted
     * @return resulting expression
     */
    protected @Nullable Expression exp(@Nullable Object obj) {
        if (obj instanceof Expression expression) {
            return expression;
        } else {
            return obj == null ? null : new ExpressionMatch(obj.toString());
        }
    }

    /**
     * Converts all parameters to an expression array.
     * Objects that are already instances of {@link Expression} are not touched.
     * All others are converted to {@link Expression}.
     *
     * @param objects the objects that are to be converted
     * @return resulting expression array
     */
    protected Expression[] exps(Object... objects) {
        List<Expression> result = new ArrayList<>();
        for (Object object : objects) {
            Expression e = exp(object);
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
    protected Expression tag(@Nullable String name, Object expression, @Nullable Object tag) {
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
        return cmd(expression, (Command) null);
    }

    /**
     * Adds a command to the resulting AST tree, if the expression matches.
     *
     * @param expression the expression that has to match
     * @param command the command that should be added
     * @return resulting expression
     */
    protected Expression cmd(Object expression, @Nullable Command command) {
        return tag(CMD, expression, command != null ? new SingleCommandSupplier(command) : null);
    }

    /**
     * Adds command resolver to the resulting AST tree, if the expression matches.
     *
     * @param expression the expression that has to match
     * @param command the command that should be added
     * @return resulting expression
     */
    protected Expression cmd(Object expression, AbstractRuleBasedInterpreter.@Nullable ItemCommandSupplier command) {
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
     * the provided order. The resulting nodes's value will be an {@code Object[]} that contains all values of the
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
     * resulting node's value will be an {@code Object[]} that contains all values of the
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
     * is at least one match. The resulting node's value will be an {@code Object[]} that contains all values of the
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
     * @param commandSupplier supplies the command to be executed.
     * @return response text
     * @throws InterpretationException in case that there is no or more than on item matching the fragments
     */
    protected String executeSingle(ResourceBundle language, String[] labelFragments,
            ItemCommandSupplier commandSupplier, Rule.InterpretationContext context) throws InterpretationException {
        List<Item> items = getMatchingItems(language, labelFragments, commandSupplier, context);
        if (items.isEmpty()) {
            if (!getMatchingItems(language, labelFragments, null, context).isEmpty()) {
                throw new InterpretationException(
                        language.getString(COMMAND_NOT_ACCEPTED).replace("<cmd>", commandSupplier.getCommandLabel()));
            } else {
                throw new InterpretationException(language.getString(NO_OBJECTS));
            }
        } else if (items.size() > 1) {
            throw new InterpretationException(language.getString(MULTIPLE_OBJECTS));
        } else {
            Item item = items.get(0);
            Command command = commandSupplier.getItemCommand(item);
            if (command == null) {
                logger.warn("Failed resolving item command");
                return language.getString(ERROR);
            }
            return trySendCommand(language, item, command);
        }
    }

    /**
     * Executes a custom rule command.
     *
     * @param item the rule target.
     * @param options replacement values from item description.
     * @param language resource bundle used for producing localized response texts
     * @param commandText label fragments that are used to match an item's label.
     *            For a positive match, the item's label has to contain every fragment - independently of their order.
     *            They are treated case-insensitive.
     * @return response text
     * @throws InterpretationException in case that there is no or more than on item matching the fragments
     */
    protected String executeCustom(ResourceBundle language, Item item, String commandText,
            HashMap<String, String> options) throws InterpretationException {
        @Nullable
        String commandReplacement = options.get(commandText);
        Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(),
                commandReplacement != null ? commandReplacement : commandText);
        if (command == null) {
            logger.warn("Failed creating command for {} from {}", item, commandText);
            return language.getString(ERROR);
        }
        return trySendCommand(language, item, command);
    }

    private String trySendCommand(ResourceBundle language, Item item, Command command) {
        if (command instanceof State newState) {
            try {
                State oldState = item.getStateAs(newState.getClass());
                if (newState.equals(oldState)) {
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

    /**
     * Filters the item registry by matching each item's name with the provided name fragments.
     * The item's label and its parent group's labels are {@link #tokenize(Locale, String) tokenized} and then
     * altogether looked up
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
     *            They are treated case-insensitive.
     * @param commandSupplier optional command supplier to access the command types an item have to support.
     *            Provide {null} if there is no need for a certain command type to be supported.
     * @return All matching items from the item registry.
     */
    protected List<Item> getMatchingItems(ResourceBundle language, String[] labelFragments,
            @Nullable ItemCommandSupplier commandSupplier, Rule.InterpretationContext context) {
        Map<Item, ItemInterpretationMetadata> itemsData = new HashMap<>();
        Map<Item, ItemInterpretationMetadata> exactMatchItemsData = new HashMap<>();
        Map<Item, ItemInterpretationMetadata> map = getItemTokens(language.getLocale());
        for (Entry<Item, ItemInterpretationMetadata> entry : map.entrySet()) {
            Item item = entry.getKey();
            ItemInterpretationMetadata interpretationMetadata = entry.getValue();
            if (!context.allowedItems().isEmpty() && !context.allowedItems().contains(item.getName())) {
                logger.trace("Item {} discarded, not allowed for this rule", item.getName());
                continue;
            }
            for (List<List<String>> itemLabelFragmentsPath : interpretationMetadata.pathToItem) {
                boolean exactMatch = false;
                logger.trace("Checking tokens {} against the item tokens {}", labelFragments, itemLabelFragmentsPath);
                List<String> lowercaseLabelFragments = Arrays.stream(labelFragments)
                        .map(lf -> lf.toLowerCase(language.getLocale())).toList();
                List<String> unmatchedFragments = new ArrayList<>(lowercaseLabelFragments);
                for (List<String> itemLabelFragments : itemLabelFragmentsPath) {
                    if (itemLabelFragments.equals(lowercaseLabelFragments)) {
                        exactMatch = true;
                        unmatchedFragments.clear();
                        break;
                    }
                    unmatchedFragments.removeAll(itemLabelFragments);
                }
                boolean allMatched = unmatchedFragments.isEmpty();
                logger.trace("Matched: {}", allMatched);
                logger.trace("Exact match: {}", exactMatch);
                if (allMatched) {
                    List<Class<? extends Command>> commandTypes = commandSupplier != null
                            ? commandSupplier.getCommandClasses(null)
                            : List.of();
                    if (commandSupplier == null
                            || commandTypes.stream().anyMatch(item.getAcceptedCommandTypes()::contains)) {
                        insertDiscardingMembers(itemsData, item, interpretationMetadata);
                        if (exactMatch) {
                            insertDiscardingMembers(exactMatchItemsData, item, interpretationMetadata);
                        }
                    }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            List<Class<? extends Command>> commandTypes = commandSupplier != null
                    ? commandSupplier.getCommandClasses(null)
                    : List.of();
            String typeDetails = !commandTypes.isEmpty()
                    ? " that accept " + commandTypes.stream().map(Class::getSimpleName).distinct()
                            .collect(Collectors.joining(" or "))
                    : "";
            logger.debug("Partial matched items against {}{}: {}", labelFragments, typeDetails,
                    itemsData.keySet().stream().map(Item::getName).collect(Collectors.joining(", ")));
            logger.debug("Exact matched items against {}{}: {}", labelFragments, typeDetails,
                    exactMatchItemsData.keySet().stream().map(Item::getName).collect(Collectors.joining(", ")));
        }
        @Nullable
        String locationContext = context.locationItem();
        if (locationContext != null && itemsData.size() > 1) {
            logger.debug("Filtering {} matched items based on location '{}'", itemsData.size(), locationContext);
            Item matchByLocation = filterMatchedItemsByLocation(itemsData, locationContext);
            if (matchByLocation != null) {
                return List.of(matchByLocation);
            }
        }
        if (locationContext != null && exactMatchItemsData.size() > 1) {
            logger.debug("Filtering {} exact matched items based on location '{}'", exactMatchItemsData.size(),
                    locationContext);
            Item matchByLocation = filterMatchedItemsByLocation(exactMatchItemsData, locationContext);
            if (matchByLocation != null) {
                return List.of(matchByLocation);
            }
        }
        return new ArrayList<>(itemsData.size() != 1 && exactMatchItemsData.size() == 1 ? exactMatchItemsData.keySet()
                : itemsData.keySet());
    }

    @Nullable
    private Item filterMatchedItemsByLocation(Map<Item, ItemInterpretationMetadata> itemsData, String locationContext) {
        var itemsFilteredByLocation = itemsData.entrySet().stream()
                .filter((entry) -> entry.getValue().locationParentNames.contains(locationContext)).toList();
        if (itemsFilteredByLocation.size() != 1) {
            return null;
        }
        logger.debug("Unique match by location found in '{}', taking prevalence", locationContext);
        return itemsFilteredByLocation.get(0).getKey();
    }

    private static void insertDiscardingMembers(Map<Item, ItemInterpretationMetadata> items, Item item,
            ItemInterpretationMetadata interpretationMetadata) {
        String name = item.getName();
        boolean insert = items.keySet().stream().noneMatch(i -> name.startsWith(i.getName()));
        if (insert) {
            items.keySet().removeIf((matchedItem) -> matchedItem.getName().startsWith(name));
            items.put(item, interpretationMetadata);
        }
    }

    /**
     * Tokenizes text. Filters out all unsupported punctuation. Tokens will be lowercase.
     *
     * @param locale the locale that should be used for lower casing
     * @param text the text that should be tokenized
     * @return resulting tokens
     */
    protected List<String> tokenize(Locale locale, @Nullable String text) {
        List<String> parts = new ArrayList<>();
        if (text == null) {
            return parts;
        }
        String[] split;
        if (Locale.FRENCH.getLanguage().equalsIgnoreCase(locale.getLanguage())) {
            split = text.toLowerCase(locale).replaceAll("[\\']", " ").replaceAll("[^\\w\\sàâäçéèêëîïôùûü]", " ")
                    .split("\\s");
        } else if ("es".equalsIgnoreCase(locale.getLanguage())) {
            split = text.toLowerCase(locale).replaceAll("[\\']", " ").replaceAll("[^\\w\\sáéíóúïüñç]", " ")
                    .split("\\s");
        } else {
            split = text.toLowerCase(locale).replaceAll("[\\']", "").replaceAll("[^\\w\\s]", " ").split("\\s");
        }
        for (String s : split) {
            String part = s.trim();
            if (part.length() > 0) {
                parts.add(part);
            }
        }
        return parts;
    }

    /**
     * Parses a rule as text into a {@link Rule} instance.
     *
     * The rule text should be a list of space separated expressions,
     * one of them but not the first should be the character '*' (which indicates dynamic part to capture),
     * the other expressions can be conformed by a single word, alternative words separated by '|',
     * and can be marked as optional by adding '?' at the end.
     * There must be at least one non-optional expression at the beginning of the rule.
     *
     * An example of a valid text will be 'watch * on|at? the tv'.
     *
     * @param item will be the target of the rule.
     * @param ruleText the text to parse into a {@link Rule}
     *
     * @return The created rule.
     */
    protected @Nullable Rule parseItemCustomRule(Item item, String ruleText) {
        String[] ruleParts = ruleText.split("\\*");
        Expression headExpression;
        @Nullable
        Expression tailExpression = null;
        try {
            if (ruleText.startsWith("*") || !ruleText.contains(" *") || ruleParts.length > 2) {
                throw new ParseException("Incorrect usage of character '*'", 0);
            }
            List<Expression> headExpressions = new ArrayList<>();
            boolean headHasNonOptional = true;
            for (String s : ruleParts[0].split("\\s")) {
                if (!s.isBlank()) {
                    String trim = s.trim();
                    Expression expression = parseItemRuleTokenText(trim);
                    if (expression instanceof ExpressionCardinality expressionCardinality) {
                        if (!expressionCardinality.isAtLeastOne()) {
                            headHasNonOptional = false;
                        }
                    } else {
                        headHasNonOptional = false;
                    }
                    headExpressions.add(expression);
                }
            }
            if (headHasNonOptional) {
                throw new ParseException("Rule head only contains optional expressions", 0);
            }
            headExpression = seq(headExpressions.toArray());
            if (ruleParts.length == 2) {
                List<Expression> tailExpressions = new ArrayList<>();
                for (String s : ruleParts[1].split("\\s")) {
                    if (!s.isBlank()) {
                        String trim = s.trim();
                        Expression expression = parseItemRuleTokenText(trim);
                        tailExpressions.add(expression);
                    }
                }
                if (!tailExpressions.isEmpty()) {
                    tailExpression = seq(tailExpressions.toArray());
                }
            }
        } catch (ParseException e) {
            logger.warn("Unable to parse item {} rule '{}': {}", item.getName(), ruleText, e.getMessage());
            return null;
        }
        return customItemRule(item, headExpression, tailExpression);
    }

    private Expression parseItemRuleTokenText(String tokenText) throws ParseException {
        boolean optional = false;
        if (tokenText.endsWith("?")) {
            tokenText = tokenText.substring(0, tokenText.length() - 1);
            optional = true;
        }
        if (tokenText.contains("?")) {
            throw new ParseException("The character '?' can only be used at the end of the expression", 0);
        }
        if (tokenText.equals("|")) {
            throw new ParseException("The character '|' can not be used alone", 0);
        }
        Expression expression = seq(tokenText.contains("|") ? alt(Arrays.stream(tokenText.split("\\|"))//
                .filter((s) -> !s.isBlank()).toArray()) : tokenText);
        if (optional) {
            return opt(expression);
        }
        return expression;
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
            return addExpression(exp);
        }

        private Expression unwrapLet(Expression expression) {
            Expression exp = expression;
            while (exp instanceof ExpressionLet) {
                exp = ((ExpressionLet) expression).getSubExpression();
            }
            return exp;
        }

        private void emit(@Nullable Object obj) {
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
            if (unwrappedExpression instanceof ExpressionMatch match) {
                emitMatchExpression(match);
            } else if (unwrappedExpression instanceof ExpressionSequence sequence) {
                emitSequenceExpression(sequence);
            } else if (unwrappedExpression instanceof ExpressionAlternatives alternatives) {
                emitAlternativesExpression(alternatives);
            } else if (unwrappedExpression instanceof ExpressionCardinality cardinality) {
                emitCardinalExpression(cardinality);
            } else if (unwrappedExpression instanceof ExpressionIdentifier identifier) {
                emitItemIdentifierExpression(identifier);
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
                if (e instanceof ExpressionIdentifier identifier) {
                    Expression stopper = identifier.getStopper();
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
            return builder.toString();
        }
    }

    @Override
    public @Nullable String getGrammar(Locale locale, String format) {
        if (!JSGF.equals(format)) {
            return null;
        }
        JSGFGenerator generator = new JSGFGenerator(ResourceBundle.getBundle(LANGUAGE_SUPPORT, locale));
        return generator.getGrammar();
    }

    private static class ItemInterpretationMetadata {
        final List<List<List<String>>> pathToItem = new ArrayList<>();
        final List<String> locationParentNames = new ArrayList<>();

        ItemInterpretationMetadata() {
        }
    }

    protected interface ItemCommandSupplier {
        @Nullable
        Command getItemCommand(Item item);

        String getCommandLabel();

        List<Class<? extends Command>> getCommandClasses(@Nullable Item item);
    }

    private record SingleCommandSupplier(Command command) implements ItemCommandSupplier {
        @Override
        public @Nullable Command getItemCommand(Item ignored) {
            return command;
        }

        @Override
        public String getCommandLabel() {
            return command.toFullString();
        }

        @Override
        public List<Class<? extends Command>> getCommandClasses(@Nullable Item ignored) {
            return List.of(command.getClass());
        }
    }
}
