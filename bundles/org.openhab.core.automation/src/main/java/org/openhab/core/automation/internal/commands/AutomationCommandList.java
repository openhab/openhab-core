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
package org.openhab.core.automation.internal.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.Template;
import org.openhab.core.automation.type.ModuleType;

/**
 * This class provides common functionality of commands:
 * <ul>
 * <li>{@link AutomationCommands#LIST_MODULE_TYPES}
 * <li>{@link AutomationCommands#LIST_TEMPLATES}
 * <li>{@link AutomationCommands#LIST_RULES}
 * </ul>
 *
 * @author Ana Dimova - Initial contribution
 */
public class AutomationCommandList extends AutomationCommand {

    /**
     * This field serves to keep the UID of a {@link Rule}, {@link Template} or {@link ModuleType}, or part of it, or
     * sequence number of a {@link Rule}, {@link Template}, or {@link ModuleType} in the list.
     */
    private String id;

    /**
     * This field is used to search for templates or types of modules, which have been translated to the language from
     * the locale. If the parameter <b>locale</b> is not passed to the command line then the default locale will be
     * used.
     */
    private Locale locale;

    /**
     * @see AutomationCommand#AutomationCommand(String, String[], int, AutomationCommandsPluggable)
     */
    public AutomationCommandList(String command, String[] params, int adminType,
            AutomationCommandsPluggable autoCommands) {
        super(command, params, adminType, autoCommands);
        if (locale == null) {
            locale = Locale.getDefault();
        }
    }

    /**
     * This method is responsible for execution of commands:
     * <ul>
     * <li>{@link AutomationCommands#LIST_MODULE_TYPES}
     * <li>{@link AutomationCommands#LIST_TEMPLATES}
     * <li>{@link AutomationCommands#LIST_RULES}
     * </ul>
     */
    @Override
    public String execute() {
        if (parsingResult != SUCCESS) {
            return parsingResult;
        }
        if (providerType == AutomationCommands.MODULE_TYPE_PROVIDER) {
            return listModuleTypes();
        }
        if (providerType == AutomationCommands.TEMPLATE_PROVIDER) {
            return listTemplates();
        }
        if (providerType == AutomationCommands.RULE_PROVIDER) {
            return listRules();
        }
        return FAIL;
    }

    /**
     * This method is invoked from the constructor to parse all parameters and options of the command <b>LIST</b>.
     * If there are redundant parameters or options the result will be the failure of the command. This command has:
     * <ul>
     * <b>Options:</b>
     * <ul>
     * <li><b>PrintStackTrace</b> is common for all commands and its presence triggers printing of stack trace in case
     * of exception.
     * </ul>
     * </ul>
     * <ul>
     * <b>Parameters:</b>
     * <ul>
     * <li><b>id</b> is optional and its presence triggers printing of details on specified automation object.
     * <li><b>locale</b> is optional and it triggers printing of localized details on specified automation object. Its
     * value is interpreted as <b>language</b> or <b>language tag</b>. If missing - the default locale will be used.
     * </ul>
     * </ul>
     */
    @Override
    protected String parseOptionsAndParameters(String[] parameterValues) {
        boolean getId = true;
        boolean getLocale = true;
        for (int i = 0; i < parameterValues.length; i++) {
            if (null == parameterValues[i]) {
                continue;
            }
            if (parameterValues[i].charAt(0) == '-') {
                if (OPTION_ST.equals(parameterValues[i])) {
                    st = true;
                    continue;
                }
                return String.format("Unsupported option: %s", parameterValues[i]);
            }
            if (getId) {
                id = parameterValues[i];
                getId = false;
                continue;
            }
            if (getLocale) {
                String l = parameterValues[i];
                locale = new Locale(l);
                getLocale = false;
            }
            if (getId && getLocale) {
                return String.format("Unsupported parameter: %s", parameterValues[i]);
            }
        }
        return SUCCESS;
    }

    /**
     * This method is responsible for execution of command {@link AutomationCommands#LIST_RULES}.
     *
     * @return a string representing understandable for the user message containing information on the outcome of the
     *         command {@link AutomationCommands#LIST_RULES}.
     */
    private String listRules() {
        Collection<Rule> collection = autoCommands.getRules();
        Map<String, Rule> rules = new Hashtable<>();
        Map<String, String> listRules = null;
        if (collection != null && !collection.isEmpty()) {
            addCollection(collection, rules);
            String[] uids = new String[rules.size()];
            Utils.quickSort(rules.keySet().toArray(uids), 0, rules.size());
            listRules = Utils.putInHastable(uids);
        }
        if (listRules != null && !listRules.isEmpty()) {
            if (id != null) {
                collection = getRuleByFilter(listRules);
                if (collection.size() == 1) {
                    Rule r = (Rule) collection.toArray()[0];
                    if (r != null) {
                        RuleStatus status = autoCommands.getRuleStatus(r.getUID());
                        return Printer.printRule(r, status);
                    } else {
                        return String.format("Nonexistent ID: %s", id);
                    }
                } else if (collection.isEmpty()) {
                    return String.format("Nonexistent ID: %s", id);
                } else {
                    if (!rules.isEmpty()) {
                        rules.clear();
                    }
                    addCollection(collection, rules);
                    listRules = Utils.filterList(rules, listRules);
                }
            }
            return Printer.printRules(autoCommands, listRules);
        }
        return "There are no Rules available!";
    }

    /**
     * This method is responsible for execution of command {@link AutomationCommands#LIST_TEMPLATES}.
     *
     * @return a string representing understandable for the user message containing information on the outcome of the
     *         command {@link AutomationCommands#LIST_TEMPLATES}.
     */
    private String listTemplates() {
        Collection<RuleTemplate> collection = autoCommands.getTemplates(locale);
        Map<String, Template> templates = new Hashtable<>();
        Map<String, String> listTemplates = null;
        if (collection != null && !collection.isEmpty()) {
            addCollection(collection, templates);
            String[] uids = new String[templates.size()];
            Utils.quickSort(templates.keySet().toArray(uids), 0, templates.size());
            listTemplates = Utils.putInHastable(uids);
        }
        if (listTemplates != null && !listTemplates.isEmpty()) {
            if (id != null) {
                collection = getTemplateByFilter(listTemplates);
                if (collection.size() == 1) {
                    Template t = (Template) collection.toArray()[0];
                    if (t != null) {
                        return Printer.printTemplate(t);
                    } else {
                        return String.format("Nonexistent ID: %s", id);
                    }
                } else if (collection.isEmpty()) {
                    return String.format("Nonexistent ID: %s", id);
                } else {
                    if (!templates.isEmpty()) {
                        templates.clear();
                    }
                    addCollection(collection, templates);
                    listTemplates = Utils.filterList(templates, listTemplates);
                }
            }
            if (listTemplates != null && !listTemplates.isEmpty()) {
                return Printer.printTemplates(listTemplates);
            }
        }
        return "There are no Templates available!";
    }

    /**
     * This method is responsible for execution of command {@link AutomationCommands#LIST_MODULE_TYPES}.
     *
     * @return a string representing understandable for the user message containing information on the outcome of the
     *         command {@link AutomationCommands#LIST_MODULE_TYPES}.
     */
    private String listModuleTypes() {
        Map<String, ModuleType> moduleTypes = new Hashtable<>();
        Collection<? extends ModuleType> collection = autoCommands.getTriggers(locale);
        addCollection(collection, moduleTypes);
        collection = autoCommands.getConditions(locale);
        addCollection(collection, moduleTypes);
        collection = autoCommands.getActions(locale);
        addCollection(collection, moduleTypes);
        Map<String, String> listModuleTypes = null;
        if (!moduleTypes.isEmpty()) {
            String[] uids = new String[moduleTypes.size()];
            Utils.quickSort(moduleTypes.keySet().toArray(uids), 0, moduleTypes.size());
            listModuleTypes = Utils.putInHastable(uids);
        }
        if (listModuleTypes != null && !listModuleTypes.isEmpty()) {
            if (id != null) {
                collection = getModuleTypeByFilter(listModuleTypes);
                if (collection.size() == 1) {
                    ModuleType mt = (ModuleType) collection.toArray()[0];
                    if (mt != null) {
                        return Printer.printModuleType(mt);
                    } else {
                        return String.format("Nonexistent ID: %s", id);
                    }
                } else if (collection.isEmpty()) {
                    return String.format("Nonexistent ID: %s", id);
                } else {
                    if (!moduleTypes.isEmpty()) {
                        moduleTypes.clear();
                    }
                    addCollection(collection, moduleTypes);
                    listModuleTypes = Utils.filterList(moduleTypes, listModuleTypes);
                }
            }
            return Printer.printModuleTypes(listModuleTypes);
        }
        return "There are no Module Types available!";
    }

    /**
     * This method reduces the list of {@link Rule}s so that their unique identifier or part of it to match the
     * {@link #id} or
     * the index in the <tt>list</tt> to match the {@link #id}.
     *
     * @param list is the list of {@link Rule}s for reducing.
     * @return a collection of {@link Rule}s that match the filter.
     */
    private Collection<Rule> getRuleByFilter(Map<String, String> list) {
        Collection<Rule> rules = new ArrayList<>();
        if (!list.isEmpty()) {
            Rule r = null;
            String uid = list.get(id);
            if (uid != null) {
                r = autoCommands.getRule(uid);
                if (r != null) {
                    rules.add(r);
                    return rules;
                }
            } else {
                r = autoCommands.getRule(id);
                if (r != null) {
                    rules.add(r);
                    return rules;
                } else {
                    for (String ruleUID : list.values()) {
                        if (ruleUID.indexOf(id) > -1) {
                            rules.add(autoCommands.getRule(ruleUID));
                        }
                    }
                }
            }
        }
        return rules;
    }

    /**
     * This method reduces the list of {@link Template}s so that their unique identifier or part of it to match the
     * {@link #id} or
     * the index in the <tt>list</tt> to match the {@link #id}.
     *
     * @param list is the list of {@link Template}s for reducing.
     * @return a collection of {@link Template}s that match the filter.
     */
    private Collection<RuleTemplate> getTemplateByFilter(Map<String, String> list) {
        Collection<RuleTemplate> templates = new ArrayList<>();
        RuleTemplate t = null;
        String uid = list.get(id);
        if (uid != null) {
            t = autoCommands.getTemplate(uid, locale);
            if (t != null) {
                templates.add(t);
                return templates;
            }
        } else {
            t = autoCommands.getTemplate(id, locale);
            if (t != null) {
                templates.add(t);
                return templates;
            } else {
                for (String templateUID : list.keySet()) {
                    if (templateUID.indexOf(id) != -1) {
                        templates.add(autoCommands.getTemplate(templateUID, locale));
                    }
                }
            }
        }
        return templates;
    }

    /**
     * This method reduces the list of {@link ModuleType}s so that their unique identifier or part of it to match the
     * {@link #id} or
     * the index in the <tt>list</tt> to match the {@link #id}.
     *
     * @param list is the list of {@link ModuleType}s for reducing.
     * @return a collection of {@link ModuleType}s that match the filter.
     */
    private Collection<ModuleType> getModuleTypeByFilter(Map<String, String> list) {
        Collection<ModuleType> moduleTypes = new ArrayList<>();
        if (!list.isEmpty()) {
            ModuleType mt = null;
            String uid = list.get(id);
            if (uid != null) {
                mt = autoCommands.getModuleType(uid, locale);
                if (mt != null) {
                    moduleTypes.add(mt);
                    return moduleTypes;
                }
            } else {
                mt = autoCommands.getModuleType(id, locale);
                if (mt != null) {
                    moduleTypes.add(mt);
                    return moduleTypes;
                } else {
                    for (String typeUID : list.values()) {
                        if (typeUID.indexOf(id) != -1) {
                            moduleTypes.add(autoCommands.getModuleType(typeUID, locale));
                        }
                    }
                }
            }
        }
        return moduleTypes;
    }

    /**
     * This method converts a {@link Collection} of {@link Rule}s, {@link Template}s or {@link ModuleType}s to a
     * {@link Hashtable} with keys - the UID of the object and values - the object.
     *
     * @param collection is the {@link Collection} of {@link Rule}s, {@link Template}s or {@link ModuleType}s which
     *            must be converted.
     * @param list is the map with keys - the UID of the object and values - the object, which must be
     *            filled with the objects from <tt>collection</tt>.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void addCollection(Collection collection, Map list) {
        if (collection != null && !collection.isEmpty()) {
            Iterator i = collection.iterator();
            while (i.hasNext()) {
                Object element = i.next();
                if (element instanceof ModuleType) {
                    list.put(((ModuleType) element).getUID(), element);
                }
                if (element instanceof RuleTemplate) {
                    list.put(((RuleTemplate) element).getUID(), element);
                }
                if (element instanceof Rule) {
                    list.put(((Rule) element).getUID(), element);
                }
            }
        }
    }

}
