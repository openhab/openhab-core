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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.parser.ParsingException;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.Template;
import org.openhab.core.automation.template.TemplateRegistry;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.type.TriggerType;
import org.osgi.framework.BundleContext;

/**
 * This class provides mechanism to separate the Automation Commands implementation from the Automation Core
 * implementation.
 *
 * @author Ana Dimova - Initial contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 */
@NonNullByDefault
public abstract class AutomationCommands {

    /**
     * This static field is used to switch between providers in different commands.
     */
    protected static final int RULE_PROVIDER = 1;

    /**
     * This static field is used to switch between providers in different commands.
     */
    protected static final int TEMPLATE_PROVIDER = 2;

    /**
     * This static field is used to switch between providers in different commands.
     */
    protected static final int MODULE_TYPE_PROVIDER = 3;

    /**
     * This static field is an identifier of the command {@link AutomationCommandImport} for {@link ModuleType}s.
     */
    protected static final String IMPORT_MODULE_TYPES = "importModuleTypes";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandImport} for {@link ModuleType}s.
     */
    protected static final String IMPORT_MODULE_TYPES_SHORT = "imt";

    /**
     * This static field is an identifier of the command {@link AutomationCommandImport} for {@link RuleTemplate}s.
     */
    protected static final String IMPORT_TEMPLATES = "importTemplates";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandImport} for {@link RuleTemplate}s.
     */
    protected static final String IMPORT_TEMPLATES_SHORT = "it";

    /**
     * This static field is an identifier of the command {@link AutomationCommandImport} for {@link Rule}s.
     */
    protected static final String IMPORT_RULES = "importRules";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandImport} for {@link Rule}s.
     */
    protected static final String IMPORT_RULES_SHORT = "ir";

    /**
     * This static field is an identifier of the command {@link AutomationCommandExport} for {@link ModuleType}s.
     */
    protected static final String EXPORT_MODULE_TYPES = "exportModuleTypes";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandExport} for {@link ModuleType}s.
     */
    protected static final String EXPORT_MODULE_TYPES_SHORT = "emt";

    /**
     * This static field is an identifier of the command {@link AutomationCommandExport} for {@link RuleTemplate}s.
     */
    protected static final String EXPORT_TEMPLATES = "exportTemplates";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandExport} for {@link RuleTemplate}s.
     */
    protected static final String EXPORT_TEMPLATES_SHORT = "et";

    /**
     * This static field is an identifier of the command {@link AutomationCommandExport} for {@link Rule}s.
     */
    protected static final String EXPORT_RULES = "exportRules";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandExport} for {@link Rule}s.
     */
    protected static final String EXPORT_RULES_SHORT = "er";

    /**
     * This static field is an identifier of the command {@link AutomationCommandRemove} for {@link Rule}.
     */
    protected static final String REMOVE_RULE = "removeRule";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandRemove} for {@link Rule}.
     */
    protected static final String REMOVE_RULE_SHORT = "rmr";

    /**
     * This static field is an identifier of the command {@link AutomationCommandRemove} for {@link Rule}s.
     */
    protected static final String REMOVE_RULES = "removeRules";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandRemove} for {@link Rule}s.
     */
    protected static final String REMOVE_RULES_SHORT = "rmrs";

    /**
     * This static field is an identifier of the command {@link AutomationCommandRemove} for {@link RuleTemplate}s.
     */
    protected static final String REMOVE_TEMPLATES = "removeTemplates";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandRemove} for {@link RuleTemplate}s.
     */
    protected static final String REMOVE_TEMPLATES_SHORT = "rmts";

    /**
     * This static field is an identifier of the command {@link AutomationCommandRemove} for {@link ModuleType}s.
     */
    protected static final String REMOVE_MODULE_TYPES = "removeModuleTypes";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandRemove} for {@link ModuleType}s.
     */
    protected static final String REMOVE_MODULE_TYPES_SHORT = "rmmts";

    /**
     * This static field is an identifier of the command {@link AutomationCommandList} for {@link ModuleType}s.
     */
    protected static final String LIST_MODULE_TYPES = "listModuleTypes";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandList} for {@link ModuleType}s.
     */
    protected static final String LIST_MODULE_TYPES_SHORT = "lsmt";

    /**
     * This static field is an identifier of the command {@link AutomationCommandList} for {@link RuleTemplate}s.
     */
    protected static final String LIST_TEMPLATES = "listTemplates";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandList} for {@link RuleTemplate}s.
     */
    protected static final String LIST_TEMPLATES_SHORT = "lst";

    /**
     * This static field is an identifier of the command {@link AutomationCommandList} for {@link Rule}s.
     */
    protected static final String LIST_RULES = "listRules";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandList} for {@link Rule}s.
     */
    protected static final String LIST_RULES_SHORT = "lsr";

    /**
     * This static field is an identifier of the command {@link AutomationCommandEnableRule}.
     */
    protected static final String ENABLE_RULE = "enableRule";

    /**
     * This static field is a short identifier of the command {@link AutomationCommandEnableRule}.
     */
    protected static final String ENABLE_RULE_SHORT = "enr";

    /**
     * This field holds a reference to the {@link CommandlineModuleTypeProvider} instance.
     */
    protected @NonNullByDefault({}) CommandlineModuleTypeProvider moduleTypeProvider;

    /**
     * This field holds a reference to the {@link CommandlineTemplateProvider} instance.
     */
    protected @NonNullByDefault({}) CommandlineTemplateProvider templateProvider;

    /**
     * This field holds a reference to the {@link CommandlineRuleImporter} instance.
     */
    protected @NonNullByDefault({}) CommandlineRuleImporter ruleImporter;

    /**
     * This method is used for getting the rule corresponding to the specified UID from the RuleManager.
     *
     * @param uid
     *            specifies the wanted {@link Rule} uniquely.
     * @return a {@link Rule}, corresponding to the specified UID.
     */
    public abstract @Nullable Rule getRule(String uid);

    /**
     * This method is used to get the all existing rules from the RuleManager.
     *
     * @return a collection of all existing rules in the RuleManager.
     */
    public abstract Collection<Rule> getRules();

    /**
     *
     * @param uid
     * @return
     */
    public abstract @Nullable RuleStatus getRuleStatus(String uid);

    /**
     *
     * @param uid
     * @param isEnabled
     */
    public abstract void setEnabled(String uid, boolean isEnabled);

    /**
     * This method is used for getting the {@link RuleTemplate} corresponding to the specified UID from the manager of
     * the {@link Template}s.
     *
     * @param templateUID
     *            specifies the wanted {@link RuleTemplate} uniquely.
     * @param locale
     *            a {@link Locale} that specifies the variant of the {@link RuleTemplate} that the user wants to see.
     *            Can be <code>null</code> and then the default locale will be used.
     * @return a {@link RuleTemplate}, corresponding to the specified UID and locale.
     */
    public abstract @Nullable Template getTemplate(String templateUID, @Nullable Locale locale);

    /**
     * This method is used for getting the collection of {@link RuleTemplate}s corresponding to the specified locale
     * from the manager of the {@link Template}s.
     *
     * @param locale
     *            a {@link Locale} that specifies the variant of the {@link RuleTemplate}s that the user wants to see.
     *            Can be <code>null</code> and then the default locale will be used.
     * @return a collection of {@link RuleTemplate}s, corresponding to the specified locale.
     */
    public abstract Collection<RuleTemplate> getTemplates(@Nullable Locale locale);

    /**
     * This method is used for getting the {@link ModuleType} corresponding to the specified UID from the manager of the
     * {@link ModuleType}s.
     *
     * @param typeUID
     *            specifies the wanted {@link ModuleType} uniquely.
     * @param locale
     *            a {@link Locale} that specifies the variant of the {@link ModuleType} that the user wants to see. Can
     *            be <code>null</code> and then the default locale will be used.
     * @return a {@link ModuleType}, corresponding to the specified UID and locale.
     */
    public abstract @Nullable ModuleType getModuleType(String typeUID, @Nullable Locale locale);

    /**
     * This method is used for getting the collection of {@link TriggerType}s corresponding to
     * specified locale from the ModuleTypeRegistry.
     *
     * @param locale
     *            a {@link Locale} that specifies the variant of the {@link ModuleType}s that the user wants to see. Can
     *            be <code>null</code> and then the default locale will be used.
     * @return a collection of {@link ModuleType}s from given class and locale.
     */
    public abstract <T extends ModuleType> Collection<T> getTriggers(@Nullable Locale locale);

    /**
     * This method is used for getting the collection of {@link ConditionType}s corresponding to
     * specified locale from the ModuleTypeRegistry.
     *
     * @param locale
     *            a {@link Locale} that specifies the variant of the {@link ModuleType}s that the user wants to see. Can
     *            be <code>null</code> and then the default locale will be used.
     * @return a collection of {@link ModuleType}s from given class and locale.
     */
    public abstract <T extends ModuleType> Collection<T> getConditions(@Nullable Locale locale);

    /**
     * This method is used for getting the collection of {@link ActionType}s corresponding to
     * specified locale from the ModuleTypeRegistry.
     *
     * @param locale
     *            a {@link Locale} that specifies the variant of the {@link ModuleType}s that the user wants to see. Can
     *            be <code>null</code> and then the default locale will be used.
     * @return a collection of {@link ModuleType}s from given class and locale.
     */
    public abstract <T extends ModuleType> Collection<T> getActions(@Nullable Locale locale);

    /**
     * This method is used for removing a rule corresponding to the specified UID from the RuleManager.
     *
     * @param uid
     *            specifies the wanted {@link Rule} uniquely.
     * @return a string representing the result of the command.
     */
    public abstract String removeRule(String uid);

    /**
     * This method is used for removing the rules from the RuleManager, corresponding to the specified filter.
     *
     * @param ruleFilter
     *            specifies the wanted {@link Rule}s.
     * @return a string representing the result of the command.
     */
    public abstract String removeRules(String ruleFilter);

    /**
     * This method is responsible for choosing a particular class of commands and creates an instance of this class on
     * the basis of the identifier of the command.
     *
     * @param command
     *            is the identifier of the command.
     * @param parameterValues
     *            is an array of strings which are basis for initializing the options and parameters of the command. The
     *            order for their description is a random.
     * @return an instance of the class corresponding to the identifier of the command.
     */
    protected abstract @Nullable AutomationCommand parseCommand(String command, String[] parameterValues);

    /**
     * Initializing method.
     *
     * @param bundleContext bundle's context
     * @param ruleRegistry
     * @param templateRegistry
     */
    public void initialize(BundleContext bundleContext, ModuleTypeRegistry moduleTypeRegistry,
            TemplateRegistry<RuleTemplate> templateRegistry, RuleRegistry ruleRegistry) {
        moduleTypeProvider = new CommandlineModuleTypeProvider(bundleContext, moduleTypeRegistry);
        templateProvider = new CommandlineTemplateProvider(bundleContext, templateRegistry);
        ruleImporter = new CommandlineRuleImporter(bundleContext, ruleRegistry);
    }

    /**
     * This method closes the providers and the importer.
     */
    public void dispose() {
        moduleTypeProvider.close();
        templateProvider.close();
        ruleImporter.close();
        moduleTypeProvider = null;
        templateProvider = null;
        ruleImporter = null;
    }

    /**
     * This method is responsible for exporting a set of {@link ModuleType}s in a specified file.
     *
     * @param parserType
     *            is relevant to the format that you need for conversion of the {@link ModuleType}s in text.
     * @param set
     *            a set of {@link ModuleType}s to export.
     * @param file
     *            a specified file for export.
     * @throws Exception
     *             when I/O operation has failed or has been interrupted or generating of the text fails for some
     *             reasons.
     * @return a string representing the result of the command.
     */
    public String exportModuleTypes(String parserType, Set<ModuleType> set, File file) throws Exception {
        return moduleTypeProvider.exportModuleTypes(parserType, set, file);
    }

    /**
     * This method is responsible for exporting a set of {@link Template}s in a specified file.
     *
     * @param parserType
     *            is relevant to the format that you need for conversion of the {@link Template}s in text.
     * @param set
     *            a set of {@link Template}s to export.
     * @param file
     *            a specified file for export.
     * @throws Exception
     *             when I/O operation has failed or has been interrupted or generating of the text fails for some
     *             reasons.
     * @return a string representing the result of the command.
     */
    public String exportTemplates(String parserType, Set<RuleTemplate> set, File file) throws Exception {
        return templateProvider.exportTemplates(parserType, set, file);
    }

    /**
     * This method is responsible for exporting a set of {@link Rule}s in a specified file.
     *
     * @param parserType
     *            is relevant to the format that you need for conversion of the {@link Rule}s in text.
     * @param set
     *            a set of {@link Rule}s to export.
     * @param file
     *            a specified file for export.
     * @throws Exception
     *             when I/O operation has failed or has been interrupted or generating of the text fails for some
     *             reasons.
     * @return a string representing the result of the command.
     */
    public String exportRules(String parserType, Set<Rule> set, File file) throws Exception {
        return ruleImporter.exportRules(parserType, set, file);
    }

    /**
     * This method is responsible for importing a set of {@link ModuleType}s from a specified file or URL resource.
     *
     * @param parserType
     *            is relevant to the format that you need for conversion of the {@link ModuleType}s from text.
     * @param url
     *            is a specified file or URL resource.
     * @throws ParsingException
     *             when parsing of the text fails for some reasons.
     * @throws IOException
     *             when I/O operation has failed or has been interrupted.
     * @return a set of module types, representing the result of the command.
     */
    public Set<ModuleType> importModuleTypes(String parserType, URL url) throws Exception {
        return moduleTypeProvider.importModuleTypes(parserType, url);
    }

    /**
     * This method is responsible for importing a set of {@link Template}s from a specified file or URL resource.
     *
     * @param parserType
     *            is relevant to the format that you need for conversion of the {@link Template}s from text.
     * @param url
     *            is a specified file or URL resource.
     * @throws ParsingException
     *             is thrown when parsing of the text fails for some reasons.
     * @throws IOException
     *             is thrown when I/O operation has failed or has been interrupted.
     * @return a set of templates, representing the result of the command.
     */
    public Set<RuleTemplate> importTemplates(String parserType, URL url) throws Exception {
        return templateProvider.importTemplates(parserType, url);
    }

    /**
     * This method is responsible for importing a set of {@link Rule}s from a specified file or URL resource.
     *
     * @param parserType
     *            is relevant to the format that you need for conversion of the {@link Rule}s from text.
     * @param url
     *            is a specified file or URL resource.
     * @throws ParsingException
     *             is thrown when parsing of the text fails for some reasons.
     * @throws IOException
     *             is thrown when I/O operation has failed or has been interrupted.
     * @return a set of rules, representing the result of the command.
     */
    public Set<Rule> importRules(String parserType, URL url) throws Exception {
        return ruleImporter.importRules(parserType, url);
    }

    /**
     * This method is responsible for removing a set of objects loaded from a specified file or URL resource.
     *
     * @param providerType
     *            specifies the provider responsible for removing the objects loaded from a specified file or URL
     *            resource.
     * @param url
     *            is a specified file or URL resource.
     * @return a string representing the result of the command.
     */
    public String remove(int providerType, URL url) {
        switch (providerType) {
            case AutomationCommands.MODULE_TYPE_PROVIDER:
                if (moduleTypeProvider != null) {
                    return moduleTypeProvider.remove(url);
                }
                break;
            case AutomationCommands.TEMPLATE_PROVIDER:
                if (templateProvider != null) {
                    return templateProvider.remove(url);
                }
                break;
        }
        return AutomationCommand.FAIL;
    }

    /**
     * This method is responsible for execution of every particular command and to return the result of the execution.
     *
     * @param command
     *            is an identifier of the command.
     * @param parameterValues
     *            is an array of strings which are basis for initializing the options and parameters of the command. The
     *            order for their description is a random.
     * @return understandable for the user message containing information on the outcome of the command.
     */
    public String executeCommand(String command, String[] parameterValues) {
        AutomationCommand commandInst = parseCommand(command, parameterValues);
        if (commandInst != null) {
            return commandInst.execute();
        }
        return String.format("Command \"%s\" is not supported!", command);
    }
}
