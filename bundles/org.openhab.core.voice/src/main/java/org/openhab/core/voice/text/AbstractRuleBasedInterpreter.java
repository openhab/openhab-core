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
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigParser;
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
    private static final String VALUE = "value";

    public static final String IS_TEMPLATE_CONFIGURATION = "isTemplate";
    public static final String IS_SILENT_CONFIGURATION = "isSilent";
    public static final String IS_FORCED_CONFIGURATION = "isForced";

    /**
     * Reserved token to use in custom item rules.
     * Represents the name place in the phrase.
     */
    private static final String NAME_TOKEN = "$name$";
    /**
     * Reserved token to use in custom item rules.
     * Represents the command place in the phrase,
     * which possible values are defined by the command description
     * of the item registering these rule.
     */
    private static final String CMD_TOKEN = "$cmd$";
    /**
     * Reserved token to use in custom item rules.
     * Represents the command place in the phrase,
     * allows capturing multiple tokens.
     */
    private static final String DYN_CMD_TOKEN = "$*$";
    /**
     * Set of reserved tokens in custom item rules.
     */
    private static final Set<String> CUSTOM_RULE_TOKENS = Set.of(NAME_TOKEN, CMD_TOKEN, DYN_CMD_TOKEN);

    private static final String LANGUAGE_SUPPORT = "LanguageSupport";

    private static final String SYNONYMS_NAMESPACE = "synonyms";

    private static final String SEMANTICS_NAMESPACE = "semantics";

    private final MetadataRegistry metadataRegistry;

    private final Logger logger = LoggerFactory.getLogger(AbstractRuleBasedInterpreter.class);

    private final Map<Locale, List<Rule>> languageRules = new HashMap<>();
    private final Map<Locale, Set<String>> allItemTokens = new HashMap<>();
    private final Map<Locale, Map<Item, ItemInterpretationMetadata>> itemTokens = new HashMap<>();

    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;

    private final RegistryChangeListener<Item> registryChangeListener = new RegistryChangeListener<>() {
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
    private final RegistryChangeListener<Metadata> synonymsChangeListener = new RegistryChangeListener<>() {
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

    protected AbstractRuleBasedInterpreter(final EventPublisher eventPublisher, final ItemRegistry itemRegistry,
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
        if (lastResult != null && lastResult.getException() != null) {
            throw lastResult.getException();
        }
        throw new InterpretationException(language.getString(SORRY));
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
     * @return Expression that represents a name of an item.
     */
    private Expression value() {
        return value(null);
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
        List<Rule> ruleSet = languageRules.computeIfAbsent(locale, k -> new ArrayList<>());
        ruleSet.addAll(Arrays.asList(rules));
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
        return restrictedItemRule(ItemFilter.all(), headExpression, tailExpression, false, false);
    }

    /**
     * Creates an item rule on base of a head and a tail expression, where the middle part of the new rule's expression
     * will consist of an item
     * name expression. Either the head expression or the tail expression should contain at least one {@link #cmd}
     * generated expression.
     *
     * 
     * @param allowedItems Allowed item targets.
     * @param headExpression The head expression.
     * @param tailExpression The tail expression.
     * @return The created rule.
     */
    protected Rule restrictedItemRule(Set<Item> allowedItems, Object headExpression, @Nullable Object tailExpression) {
        return restrictedItemRule(ItemFilter.forItems(allowedItems), headExpression, tailExpression, false, false);
    }

    /**
     * Creates an item rule on base of a head and a tail expression, where the middle part of the new rule's ex ression
     * will consist of an item
     * name expression. Either the head expression or the tail expression should contain at least one {@link #cmd}
     * generated expression.
     * Rule will be restricted by the provided filter.
     *
     * @param itemFilter Filters allowed items.
     * @param headExpression The head expression.
     * @param tailExpression The tail expression.
     * @return The created rule.
     */
    protected Rule restrictedItemRule(ItemFilter itemFilter, Object headExpression, @Nullable Object tailExpression,
            boolean isForced, boolean isSilent) {
        Expression tail = exp(tailExpression);
        Expression expression = tail == null ? seq(headExpression, name()) : seq(headExpression, name(tail), tail);
        return new Rule(expression, itemFilter, isForced, isSilent) {
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
     * Creates an item rule which two dynamic capture values on base of a head a middle and an optional tail expression,
     * where one of the values is an item name expression and the other a free captured value.
     * Rule will be restricted by the provided filter.
     *
     * @param item Item registering the rule.
     * @param itemFilter Filters allowed items.
     * @param headExpression The head expression.
     * @param midExpression The middle expression.
     * @param tailExpression The optional tail expression.
     * @param isNameFirst Indicates whether the name goes between the head and the middle expressions.
     * @return The created rule.
     */
    protected Rule restrictedDynamicItemRule(Item item, ItemFilter itemFilter, Object headExpression,
            Object midExpression, @Nullable Object tailExpression, boolean isNameFirst, boolean isForced,
            boolean isSilent) {
        Expression head = Objects.requireNonNull(exp(headExpression));
        Expression mid = Objects.requireNonNull(exp(midExpression));
        @Nullable
        Expression tail = exp(tailExpression);
        Expression firstValue = isNameFirst ? name(mid) : value(mid);
        Expression secondValue = tail != null ? (isNameFirst ? value(tail) : name(tail))
                : (isNameFirst ? value() : name());
        Expression expression = tail == null ? //
                seq(head, firstValue, mid, secondValue) : //
                seq(head, firstValue, mid, secondValue, tail);
        Map<String, String> itemValuesByLabel = getItemValuesByLabel(item);
        return new Rule(expression, itemFilter, isForced, isSilent) {
            @Override
            public InterpretationResult interpretAST(ResourceBundle language, ASTNode node,
                    InterpretationContext context) {
                String[] name = node.findValueAsStringArray(NAME);
                String[] value = node.findValueAsStringArray(VALUE);
                if (name != null && value != null) {
                    try {
                        ItemCommandSupplier commandSupplier = new TextCommandSupplier(String.join(" ", value),
                                item.getAcceptedCommandTypes(), itemValuesByLabel);
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
     * Creates a custom rule on base of a head and a tail expression,
     * where the middle part of the new rule's expression will consist of a free command to be captured.
     * Rule will be restricted to the provided item name.
     *
     * @param item Item target
     * @param headExpression The head expression.
     * @param tailExpression The tail expression.
     * @return The created rule.
     */
    protected Rule customDynamicRule(Item item, ItemFilter itemFilter, Object headExpression,
            @Nullable Object tailExpression, boolean isForced, boolean isSilent) {
        Expression tail = exp(tailExpression);
        Expression expression = tail == null ? seq(headExpression, value(null))
                : seq(headExpression, value(tail), tail);
        HashMap<String, String> valuesByLabel = getItemValuesByLabel(item);
        return new Rule(expression, itemFilter, isForced, isSilent) {
            @Override
            public InterpretationResult interpretAST(ResourceBundle language, ASTNode node,
                    InterpretationContext context) {
                String[] commandParts = node.findValueAsStringArray(VALUE);
                if (commandParts != null && commandParts.length > 0) {
                    try {
                        ItemCommandSupplier commandSupplier = new TextCommandSupplier(
                                String.join(" ", commandParts).trim(), item.getAcceptedCommandTypes(), valuesByLabel);
                        return new InterpretationResult(true, executeCustom(language, commandSupplier, context));
                    } catch (InterpretationException ex) {
                        return new InterpretationResult(ex);
                    }
                }
                return InterpretationResult.SEMANTIC_ERROR;
            }
        };
    }

    private HashMap<String, String> getItemValuesByLabel(Item item) {
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
        return valuesByLabel;
    }

    /**
     * Creates a custom rule on base of a expression.
     * The expression should contain at least one {@link #cmd} generated expression.
     *
     * @param itemFilter Filters the allowed items.
     * @param cmdExpression The expression.
     * @return The created rule.
     */
    protected Rule customCommandRule(ItemFilter itemFilter, Object cmdExpression, boolean isForced, boolean isSilent) {
        return new Rule(Objects.requireNonNull(exp(cmdExpression)), itemFilter, isForced, isSilent) {
            @Override
            public InterpretationResult interpretAST(ResourceBundle language, ASTNode node,
                    InterpretationContext context) {
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
                try {
                    return new InterpretationResult(true, executeCustom(language, commandSupplier, context));
                } catch (InterpretationException ex) {
                    return new InterpretationResult(ex);
                }
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
            Item item = items.getFirst();
            Command command = commandSupplier.getItemCommand(item);
            if (command == null) {
                logger.warn("Failed resolving item command");
                return language.getString(ERROR);
            }
            return trySendCommand(language, item, command, context.isForced(), context.isSilent());
        }
    }

    /**
     * Executes a custom rule command.
     *
     * @param language resource bundle used for producing localized response texts
     * @param itemCommandSupplier the rule command supplier.
     * @param context to propagate the interpretation context.
     * @return response text
     * @throws InterpretationException in case that there is no or more than on item matching the fragments
     */
    protected String executeCustom(ResourceBundle language, ItemCommandSupplier itemCommandSupplier,
            Rule.InterpretationContext context) throws InterpretationException {
        Map<Item, ItemInterpretationMetadata> itemsMap = getItemTokens(language.getLocale());
        Set<Entry<Item, ItemInterpretationMetadata>> compatibleItemEntries = itemsMap.entrySet().stream() //
                .filter(itemEntry -> context.itemFilter().filterItem(itemEntry.getKey(), metadataRegistry)) //
                .collect(Collectors.toSet());
        if (compatibleItemEntries.isEmpty()) {
            throw new InterpretationException(language.getString(NO_OBJECTS));
        }
        if (compatibleItemEntries.size() > 1 && context.locationItem() != null) {
            var filteredCompatibleEntries = compatibleItemEntries.stream() //
                    .filter(itemEntry -> itemEntry.getValue().locationParentNames.contains(context.locationItem())) //
                    .collect(Collectors.toSet());
            if (filteredCompatibleEntries.size() == 1) {
                logger.debug("Collision resolved based on location context");
                compatibleItemEntries = filteredCompatibleEntries;
            }
        }
        if (compatibleItemEntries.size() > 1) {
            throw new InterpretationException(language.getString(MULTIPLE_OBJECTS));
        }
        Item item = compatibleItemEntries.stream().map(Entry::getKey).findFirst().get();
        Command command = itemCommandSupplier.getItemCommand(item);
        if (command == null) {
            logger.warn("Failed creating command");
            return language.getString(ERROR);
        }
        return trySendCommand(language, item, command, context.isForced(), context.isSilent());
    }

    private String trySendCommand(ResourceBundle language, Item item, Command command, boolean isForced,
            boolean isSilent) {
        if (command instanceof State newState) {
            try {
                State oldState = item.getStateAs(newState.getClass());
                if (!isForced && newState.equals(oldState)) {
                    String template = language.getString(STATE_ALREADY_SINGULAR);
                    String cmdName = "state_" + command.toString().toLowerCase();
                    String stateText;
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
        return !isSilent ? language.getString(OK) : "";
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
        Map<Item, ItemInterpretationMetadata> exactMatchOnTargetItemsData = new HashMap<>();
        Map<Item, ItemInterpretationMetadata> map = getItemTokens(language.getLocale());
        for (Entry<Item, ItemInterpretationMetadata> entry : map.entrySet()) {
            Item item = entry.getKey();
            ItemInterpretationMetadata interpretationMetadata = entry.getValue();
            if (!context.itemFilter().filterItem(item, metadataRegistry)) {
                logger.trace("Item {} discarded, not allowed for this rule", item.getName());
                continue;
            }
            for (List<List<String>> itemLabelFragmentsPath : interpretationMetadata.pathToItem) {
                boolean exactMatch = false;
                boolean exactMatchOnTarget = false;
                logger.trace("Checking tokens {} against the item tokens {}", labelFragments, itemLabelFragmentsPath);
                List<String> lowercaseLabelFragments = Arrays.stream(labelFragments)
                        .map(lf -> lf.toLowerCase(language.getLocale())).toList();
                List<String> unmatchedFragments = new ArrayList<>(lowercaseLabelFragments);
                if (itemLabelFragmentsPath.getLast().equals(lowercaseLabelFragments)) {
                    exactMatch = true;
                    exactMatchOnTarget = true;
                    unmatchedFragments.clear();
                } else {
                    for (List<String> itemLabelFragments : itemLabelFragmentsPath) {
                        if (itemLabelFragments.equals(lowercaseLabelFragments)) {
                            exactMatch = true;
                            unmatchedFragments.clear();
                            break;
                        }
                        unmatchedFragments.removeAll(itemLabelFragments);
                    }
                }
                boolean allMatched = unmatchedFragments.isEmpty();
                logger.trace("Matched: {}", allMatched);
                logger.trace("Exact match: {}", exactMatch);
                logger.trace("Exact match on target: {}", exactMatchOnTarget);
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
                        if (exactMatchOnTarget) {
                            insertDiscardingMembers(exactMatchOnTargetItemsData, item, interpretationMetadata);
                        }
                    }
                }
            }
        }
        if (logger.isTraceEnabled()) {
            List<Class<? extends Command>> commandTypes = commandSupplier != null
                    ? commandSupplier.getCommandClasses(null)
                    : List.of();
            String typeDetails = !commandTypes.isEmpty()
                    ? " that accept " + commandTypes.stream().map(Class::getSimpleName).distinct()
                            .collect(Collectors.joining(" or "))
                    : "";
            logger.trace("Partial matched items against {}{}: {}", labelFragments, typeDetails,
                    itemsData.keySet().stream().map(Item::getName).collect(Collectors.joining(", ")));
            logger.trace("Exact matched items against {}{}: {}", labelFragments, typeDetails,
                    exactMatchItemsData.keySet().stream().map(Item::getName).collect(Collectors.joining(", ")));
            logger.trace("Exact matched on target items against {}{}: {}", labelFragments, typeDetails,
                    exactMatchOnTargetItemsData.keySet().stream().map(Item::getName).collect(Collectors.joining(", ")));
        }
        @Nullable
        String locationContext = context.locationItem();
        if (locationContext != null && itemsData.size() > 1) {
            logger.trace("Filtering {} matched items based on location '{}'", itemsData.size(), locationContext);
            Item matchByLocation = filterMatchedItemsByLocation(itemsData, locationContext);
            if (matchByLocation != null) {
                return List.of(matchByLocation);
            }
        }
        if (locationContext != null && exactMatchItemsData.size() > 1) {
            logger.trace("Filtering {} exact matched items based on location '{}'", exactMatchItemsData.size(),
                    locationContext);
            Item matchByLocation = filterMatchedItemsByLocation(exactMatchItemsData, locationContext);
            if (matchByLocation != null) {
                return List.of(matchByLocation);
            }
        }
        if (locationContext != null && exactMatchOnTargetItemsData.size() > 1) {
            logger.trace("Filtering {} exact matched on target items based on location '{}'",
                    exactMatchOnTargetItemsData.size(), locationContext);
            Item matchByLocation = filterMatchedItemsByLocation(exactMatchOnTargetItemsData, locationContext);
            if (matchByLocation != null) {
                return List.of(matchByLocation);
            }
        }
        if (itemsData.size() == 1) {
            return new ArrayList<>(itemsData.keySet());
        }
        if (exactMatchItemsData.size() == 1) {
            return new ArrayList<>(exactMatchItemsData.keySet());
        }
        if (exactMatchOnTargetItemsData.size() == 1) {
            return new ArrayList<>(exactMatchOnTargetItemsData.keySet());
        }
        return new ArrayList<>(itemsData.keySet());
    }

    @Nullable
    private Item filterMatchedItemsByLocation(Map<Item, ItemInterpretationMetadata> itemsData, String locationContext) {
        var itemsFilteredByLocation = itemsData.entrySet().stream()
                .filter(entry -> entry.getValue().locationParentNames.contains(locationContext)).toList();
        if (itemsFilteredByLocation.size() != 1) {
            return null;
        }
        logger.trace("Unique match by location found in '{}', taking prevalence", locationContext);
        return itemsFilteredByLocation.getFirst().getKey();
    }

    private static void insertDiscardingMembers(Map<Item, ItemInterpretationMetadata> items, Item item,
            ItemInterpretationMetadata interpretationMetadata) {
        String name = item.getName();
        boolean insert = items.keySet().stream().noneMatch(i -> name.startsWith(i.getName()));
        if (insert) {
            items.keySet().removeIf(matchedItem -> matchedItem.getName().startsWith(name));
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
        return tokenize(locale, text, false);
    }

    /**
     * Tokenizes text. Filters out all unsupported punctuation. Tokens will be lowercase.
     *
     * @param locale the locale that should be used for lower casing
     * @param text the text that should be tokenized
     * @param customRuleCompat do not remove characters used on custom rules
     * @return resulting tokens
     */
    protected List<String> tokenize(Locale locale, @Nullable String text, boolean customRuleCompat) {
        if (text == null) {
            return List.of();
        }
        String specialCharactersRegex;
        String extraAllowedChars = customRuleCompat ? Pattern.quote("$|?") : "";
        if (Locale.FRENCH.getLanguage().equalsIgnoreCase(locale.getLanguage())) {
            specialCharactersRegex = "[^\\w\\sàâäçéèêëîïôùûü" + extraAllowedChars + "]";
        } else if ("es".equalsIgnoreCase(locale.getLanguage())) {
            specialCharactersRegex = "[^\\w\\sáéíóúïüñç" + extraAllowedChars + "]";
        } else {
            specialCharactersRegex = "[^\\w\\s" + extraAllowedChars + "]";
        }
        return Arrays.stream(text.toLowerCase(locale) //
                .replaceAll("[\\']", "") //
                .replaceAll(specialCharactersRegex, " ") //
                .split("\\s")) //
                .filter(i -> !i.isBlank()) //
                .map(String::trim) //
                .toList();
    }

    /**
     * Parses a rule as text into a {@link Rule} instance.
     * <p>
     * The rule text should be a list of space separated expressions,
     * one of them but not the first should be the character '*' (which indicates dynamic part to capture),
     * the other expressions can be conformed by a single word, alternative words separated by '|',
     * and can be marked as optional by adding '?' at the end.
     * There must be at least one non-optional expression at the beginning of the rule.
     * <p>
     * An example of a valid text will be 'watch * on|at? the tv'.
     *
     * @param item will be the target of the rule.
     * @param ruleText the text to parse into a {@link Rule}
     * @param metadata voiceSystem metadata.
     * @return The created rule.
     */
    protected List<Rule> parseItemCustomRules(Locale locale, Item item, String ruleText, Metadata metadata) {
        boolean isTemplate = ConfigParser.valueAsOrElse(metadata.getConfiguration().get(IS_TEMPLATE_CONFIGURATION),
                Boolean.class, false);
        boolean isSilent = ConfigParser.valueAsOrElse(metadata.getConfiguration().get(IS_SILENT_CONFIGURATION),
                Boolean.class, false);
        boolean isForced = ConfigParser.valueAsOrElse(metadata.getConfiguration().get(IS_FORCED_CONFIGURATION),
                Boolean.class, false);
        boolean isItemRule = false;
        boolean isCommandRule = false;
        boolean isDynamicRule = false;
        if (ruleText.startsWith(NAME_TOKEN) || ruleText.startsWith(DYN_CMD_TOKEN)) {
            logger.warn("Rule can not start with {} or {}: {}", NAME_TOKEN, DYN_CMD_TOKEN, ruleText);
            return List.of();
        }
        boolean containsMultiple = CUSTOM_RULE_TOKENS.stream().anyMatch(token -> {
            int firstIndex = token.indexOf(token);
            return firstIndex != -1 && token.indexOf(token, firstIndex + 1) != -1;
        });
        if (containsMultiple) {
            logger.warn("Rule can not contains {}, {} or {} multiple times: {}", NAME_TOKEN, CMD_TOKEN, DYN_CMD_TOKEN,
                    ruleText);
            return List.of();
        }
        if (ruleText.contains(NAME_TOKEN)) {
            isItemRule = true;
        }
        if (ruleText.contains(CMD_TOKEN)) {
            isCommandRule = true;
        }
        if (ruleText.contains(DYN_CMD_TOKEN)) {
            isDynamicRule = true;
        }
        if (isCommandRule && isDynamicRule) {
            logger.warn("Rule can not contain {} and {}: {}", CMD_TOKEN, DYN_CMD_TOKEN, ruleText);
            return List.of();
        }
        if (!isCommandRule && !isDynamicRule) {
            logger.warn("Rule should contain {} or {}: {}", CMD_TOKEN, DYN_CMD_TOKEN, ruleText);
            return List.of();
        }
        try {
            ItemFilter itemsFilter = isTemplate
                    ? ItemFilter.forSimilarItem(item,
                            metadataRegistry.get(new MetadataKey(SEMANTICS_NAMESPACE, item.getName())))
                    : ItemFilter.forItem(item);
            if (isItemRule && isCommandRule) {
                String[] ruleParts = ruleText.split(Pattern.quote(NAME_TOKEN));
                String headPart = ruleParts[0];
                @Nullable
                String tailPart = ruleParts.length > 1 ? ruleParts[1] : null;
                var itemCMDs = item.getCommandDescription();
                if (itemCMDs == null || itemCMDs.getCommandOptions().isEmpty()) {
                    throw new ParseException("Missing item " + item.getName() + " command description.", 0);
                }
                ArrayList<Rule> rules = new ArrayList<>();
                for (var cmd : itemCMDs.getCommandOptions()) {
                    String label = cmd.getLabel();
                    if (label == null) {
                        label = cmd.getCommand();
                    }
                    String value = cmd.getCommand();
                    String[] cmdInfo = new String[] { label, value };
                    var head = Objects.requireNonNull(parseCustomRuleSegment(locale, headPart, false, item, cmdInfo));
                    var tail = tailPart != null ? parseCustomRuleSegment(locale, tailPart, true, item, cmdInfo) : null;
                    rules.add(restrictedItemRule(itemsFilter, head, tail, isForced, isSilent));
                }
                return rules;
            } else if (isItemRule && isDynamicRule) {
                String[] ruleParts = Arrays.stream(ruleText.split(Pattern.quote(NAME_TOKEN))) //
                        .map(s -> s.split(Pattern.quote(DYN_CMD_TOKEN))) //
                        .flatMap(Arrays::stream).toArray(String[]::new);
                if (ruleParts.length > 3) {
                    throw new ParseException("Incorrectly rule segments: " + ruleText, 0);
                }
                Expression head = Objects
                        .requireNonNull(parseCustomRuleSegment(locale, ruleParts[0], false, item, null));
                Expression mid = Objects
                        .requireNonNull(parseCustomRuleSegment(locale, ruleParts[1], false, item, null));
                @Nullable
                Expression tail = ruleParts.length > 2 ? parseCustomRuleSegment(locale, ruleParts[2], true, item, null)
                        : null;
                boolean isNameFirst = ruleText.indexOf(NAME_TOKEN) < ruleText.indexOf(DYN_CMD_TOKEN);
                return List.of(
                        restrictedDynamicItemRule(item, itemsFilter, head, mid, tail, isNameFirst, isForced, isSilent));
            } else if (isDynamicRule) {
                String[] ruleParts = ruleText.split(Pattern.quote(DYN_CMD_TOKEN));
                if (ruleParts.length > 2) {
                    throw new ParseException("Incorrectly rule segments: " + ruleText, 0);
                }
                Expression head = Objects
                        .requireNonNull(parseCustomRuleSegment(locale, ruleParts[0], false, item, null));
                @Nullable
                Expression tail = ruleParts.length > 1 ? parseCustomRuleSegment(locale, ruleParts[1], true, item, null)
                        : null;
                return List.of(customDynamicRule(item, itemsFilter, head, tail, isForced, isSilent));
            } else if (isCommandRule) {
                var itemCMDs = item.getCommandDescription();
                if (itemCMDs == null || itemCMDs.getCommandOptions().isEmpty()) {
                    throw new ParseException("Missing item " + item.getName() + " command description.", 0);
                }
                ArrayList<Rule> rules = new ArrayList<>();
                for (var cmd : itemCMDs.getCommandOptions()) {
                    String label = cmd.getLabel();
                    if (label == null) {
                        label = cmd.getCommand();
                    }
                    String value = cmd.getCommand();
                    String[] cmdInfo = new String[] { label, value };
                    var expression = Objects
                            .requireNonNull(parseCustomRuleSegment(locale, ruleText, false, item, cmdInfo));
                    rules.add(customCommandRule(itemsFilter, expression, isForced, isSilent));
                }
                return rules;
            } else {
                throw new ParseException("Unable to parse rule: " + ruleText, 0);
            }
        } catch (ParseException e) {
            logger.warn("Unable to parse item {} rule '{}': {}", item.getName(), ruleText, e.getMessage());
            return List.of();
        }
    }

    private @Nullable Expression parseCustomRuleSegment(Locale locale, String text, boolean allowEmpty, Item item,
            String @Nullable [] cmdData) throws ParseException {
        List<Expression> subExpressions = new ArrayList<>();
        Expression sequenceExpression;
        boolean headHasNonOptional = true;
        for (String s : tokenize(locale, text, true)) {
            String trim = s.trim();
            Expression expression = parseItemRuleTokenText(locale, trim, item, cmdData);
            if (expression instanceof ExpressionCardinality expressionCardinality) {
                if (!expressionCardinality.isAtLeastOne()) {
                    headHasNonOptional = false;
                }
            } else {
                headHasNonOptional = false;
            }
            subExpressions.add(expression);
        }
        if (headHasNonOptional) {
            if (allowEmpty) {
                return null;
            }
            throw new ParseException("Rule segment contains only optional elements: " + text, 0);
        }
        sequenceExpression = seq(subExpressions.toArray());
        return sequenceExpression;
    }

    private Expression parseItemRuleTokenText(Locale locale, String tokenText, Item item, String @Nullable [] cmdData)
            throws ParseException {
        boolean optional = false;
        if (tokenText.equals(CMD_TOKEN) && cmdData != null) {
            Expression cmdExpression = seq(tokenize(locale, cmdData[0]).toArray());
            return cmd(cmdExpression, TypeParser.parseCommand(item.getAcceptedCommandTypes(), cmdData[1]));
        }
        if (tokenText.endsWith("?")) {
            tokenText = tokenText.substring(0, tokenText.length() - 1);
            optional = true;
        }
        if (tokenText.contains("?")) {
            throw new ParseException("The character '?' can only be used at the end of the expression", 0);
        }
        if ("|".equals(tokenText)) {
            throw new ParseException("The character '|' can not be used alone", 0);
        }
        Expression expression = seq(tokenText.contains("|") ? alt(Arrays.stream(tokenText.split("\\|"))//
                .filter(s -> !s.isBlank()).toArray()) : tokenText);
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

        protected String getGrammar() {
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

    private record TextCommandSupplier(String text, List<Class<? extends Command>> allowedCommands,
            Map<String, String> transformations) implements ItemCommandSupplier {
        @Override
        public @Nullable Command getItemCommand(Item item) {
            return TypeParser.parseCommand(item.getAcceptedCommandTypes(), transformations.getOrDefault(text, text));
        }

        @Override
        public String getCommandLabel() {
            return text;
        }

        @Override
        public List<Class<? extends Command>> getCommandClasses(@Nullable Item ignored) {
            return allowedCommands;
        }
    }

    public record ItemFilter(Set<String> itemNames, Set<String> excludedItemNames, Set<String> itemTags,
            Set<String> itemSemantics) {

        private static final ItemFilter ALL_INSTANCE = new ItemFilter(Set.of(), Set.of(), Set.of(), Set.of());
        public boolean filterItem(Item item, MetadataRegistry metadataRegistry) {
            if (!itemNames.isEmpty() && !itemNames.contains(item.getName())) {
                return false;
            }
            if (!excludedItemNames.isEmpty() && excludedItemNames.contains(item.getName())) {
                return false;
            }
            if (!itemTags.isEmpty()
                    && (item.getTags().size() != itemTags.size() || !item.getTags().containsAll(itemTags))) {
                return false;
            }
            Metadata semanticsMetadata = metadataRegistry.get(new MetadataKey(SEMANTICS_NAMESPACE, item.getName()));
            if (!itemSemantics.isEmpty()
                    && (semanticsMetadata == null || !itemSemantics.contains(semanticsMetadata.getValue()))) {
                return false;
            }
            return true;
        }

        public static ItemFilter all() {
            return ALL_INSTANCE;
        }

        public static ItemFilter forItem(Item item) {
            return new ItemFilter(Set.of(item.getName()), Set.of(), Set.of(), Set.of());
        }

        public static ItemFilter forItems(Set<Item> item) {
            return new ItemFilter(item.stream().map(Item::getName).collect(Collectors.toSet()), Set.of(), Set.of(),
                    Set.of());
        }

        public static ItemFilter forSimilarItem(Item item, @Nullable Metadata semantic) {
            return new ItemFilter(Set.of(), Set.of(item.getName()), item.getTags(),
                    semantic != null ? Set.of(semantic.getValue()) : Set.of());
        }
    }
}
