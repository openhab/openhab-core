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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.Template;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.CompositeActionType;
import org.openhab.core.automation.type.CompositeConditionType;
import org.openhab.core.automation.type.CompositeTriggerType;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.Output;
import org.openhab.core.automation.type.TriggerType;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;

/**
 * This class provides the functionality responsible for printing the automation objects as a result of commands.
 *
 * @author Ana Dimova - Initial contribution
 * @author Yordan Mihaylov - updates related to api changes
 */
public class Printer {

    private static final int TABLE_WIDTH = 100;
    private static final int COLUMN_ID = 7;
    private static final int COLUMN_UID = 93;
    private static final int COLUMN_RULE_UID = 36;
    private static final int COLUMN_RULE_NAME = 36;
    private static final int COLUMN_RULE_STATUS = 15;
    private static final int COLUMN_PROPERTY = 28;
    private static final int COLUMN_PROPERTY_VALUE = 72;
    private static final int COLUMN_CONFIG_PARAMETER = 20;
    private static final int COLUMN_CONFIG_PARAMETER_VALUE = 52;
    private static final int COLUMN_CONFIG_PARAMETER_PROP = 16;
    private static final int COLUMN_CONFIG_PARAMETER_PROP_VALUE = 36;
    private static final String ID = "ID";
    private static final String UID = "UID";
    private static final String NAME = "NAME";
    private static final String STATUS = "STATUS";
    private static final String TAGS = "TAGS";
    private static final String LABEL = "LABEL";
    private static final String VISIBILITY = "VISIBILITY";
    private static final String DESCRIPTION = "DESCRIPTION";
    private static final String CONFIGURATION_DESCRIPTIONS = "CONFIGURATION DESCRIPTIONS ";
    private static final String ACTIONS = "ACTIONS";
    private static final String TRIGGERS = "TRIGGERS";
    private static final String CONDITIONS = "CONDITIONS";
    private static final String INPUTS = "INPUTS";
    private static final String OUTPUTS = "OUTPUTS";
    private static final String CHILDREN = "CHILDREN";
    private static final String TYPE = "TYPE";
    private static final String CONFIGURATION = "CONFIGURATION";
    private static final String MIN = "MIN";
    private static final String MAX = "MAX";
    private static final String DEFAULT = "DEFAULT";
    private static final String CONTEXT = "CONTEXT";
    private static final String PATTERN = "PATTERN";
    private static final String OPTIONS = "OPTIONS";
    private static final String STEP_SIZE = "STEP_SIZE";
    private static final String FILTER_CRITERIA = "FILTER CRITERIA ";
    private static final String REQUIRED = "REQUIRED";
    private static final String NOT_REQUIRED = "NOT REQUIRED";

    /**
     * This method is responsible for printing the list with indexes, UIDs, names and statuses of the {@link Rule}s.
     *
     * @param autoCommands
     * @param ruleUIDs
     * @return
     */
    static String printRules(AutomationCommandsPluggable autoCommands, Map<String, String> ruleUIDs) {
        int[] columnWidths = new int[] { COLUMN_ID, COLUMN_RULE_UID, COLUMN_RULE_NAME, COLUMN_RULE_STATUS };
        List<String> columnValues = new ArrayList<>();
        columnValues.add(ID);
        columnValues.add(UID);
        columnValues.add(NAME);
        columnValues.add(STATUS);
        String titleRow = Utils.getRow(columnWidths, columnValues);

        List<String> rulesRows = new ArrayList<>();
        for (int i = 1; i <= ruleUIDs.size(); i++) {
            String id = new Integer(i).toString();
            String uid = ruleUIDs.get(id);
            columnValues.set(0, id);
            columnValues.set(1, uid);
            Rule rule = autoCommands.getRule(uid);
            columnValues.set(2, rule.getName());
            columnValues.set(3, autoCommands.getRuleStatus(uid).toString());
            rulesRows.add(Utils.getRow(columnWidths, columnValues));
        }
        return Utils.getTableContent(TABLE_WIDTH, columnWidths, rulesRows, titleRow);
    }

    /**
     * This method is responsible for printing the list with indexes and UIDs of the {@link Template}s.
     *
     * @param templateUIDs is a map with keys UIDs of the {@link Template}s and values the {@link Template}s.
     * @return a formated string, representing the sorted list with indexed UIDs of the available {@link Template}s.
     */
    static String printTemplates(Map<String, String> templateUIDs) {
        int[] columnWidths = new int[] { COLUMN_ID, COLUMN_UID };
        List<String> columnTitles = new ArrayList<>();
        columnTitles.add(ID);
        columnTitles.add(UID);
        String titleRow = Utils.getRow(columnWidths, columnTitles);

        List<String> templates = new ArrayList<>();
        collectListRecords(templateUIDs, templates, columnWidths);
        return Utils.getTableContent(TABLE_WIDTH, columnWidths, templates, titleRow);
    }

    /**
     * This method is responsible for printing the list with indexes and UIDs of the {@link ModuleType}s.
     *
     * @param moduleTypeUIDs is a map with keys UIDs of the {@link ModuleType}s and values the {@link ModuleType}s.
     * @return a formated string, representing the sorted list with indexed UIDs of the available {@link ModuleType}s.
     */
    static String printModuleTypes(Map<String, String> moduleTypeUIDs) {
        int[] columnWidths = new int[] { COLUMN_ID, COLUMN_UID };
        List<String> columnTitles = new ArrayList<>();
        columnTitles.add(ID);
        columnTitles.add(UID);
        String titleRow = Utils.getRow(columnWidths, columnTitles);

        List<String> moduleTypes = new ArrayList<>();
        collectListRecords(moduleTypeUIDs, moduleTypes, columnWidths);
        return Utils.getTableContent(TABLE_WIDTH, columnWidths, moduleTypes, titleRow);
    }

    /**
     * This method is responsible for printing the {@link Rule}.
     *
     * @param rule the {@link Rule} for printing.
     * @return a formated string, representing the {@link Rule} info.
     */
    static String printRule(Rule rule, RuleStatus status) {
        int[] columnWidths = new int[] { TABLE_WIDTH };
        List<String> ruleProperty = new ArrayList<>();
        ruleProperty.add(rule.getUID() + " [ " + status + " ]");
        String titleRow = Utils.getRow(columnWidths, ruleProperty);

        List<String> ruleContent = new ArrayList<>();
        columnWidths = new int[] { COLUMN_PROPERTY, COLUMN_PROPERTY_VALUE };
        ruleProperty.set(0, UID);
        ruleProperty.add(rule.getUID());
        ruleContent.add(Utils.getRow(columnWidths, ruleProperty));
        if (rule.getName() != null) {
            ruleProperty.set(0, NAME);
            ruleProperty.set(1, rule.getName());
            ruleContent.add(Utils.getRow(columnWidths, ruleProperty));
        }
        if (rule.getDescription() != null) {
            ruleProperty.set(0, DESCRIPTION);
            ruleProperty.set(1, rule.getDescription());
            ruleContent.add(Utils.getRow(columnWidths, ruleProperty));
        }
        ruleProperty.set(0, TAGS);
        ruleProperty.set(1, getTagsRecord(rule.getTags()));
        ruleContent.add(Utils.getRow(columnWidths, ruleProperty));

        ruleContent.addAll(
                collectRecords(columnWidths, CONFIGURATION, rule.getConfiguration().getProperties().entrySet()));
        ruleContent.addAll(collectRecords(columnWidths, CONFIGURATION_DESCRIPTIONS,
                getConfigurationDescriptionRecords(rule.getConfigurationDescriptions())));
        ruleContent.addAll(collectRecords(columnWidths, TRIGGERS, rule.getTriggers()));
        ruleContent.addAll(collectRecords(columnWidths, CONDITIONS, rule.getConditions()));
        ruleContent.addAll(collectRecords(columnWidths, ACTIONS, rule.getActions()));

        return Utils.getTableContent(TABLE_WIDTH, columnWidths, ruleContent, titleRow);
    }

    /**
     * This method is responsible for printing the {@link Template}.
     *
     * @param template the {@link Template} for printing.
     * @return a formated string, representing the {@link Template} info.
     */
    static String printTemplate(Template template) {
        int[] columnWidths = new int[] { TABLE_WIDTH };
        List<String> templateProperty = new ArrayList<>();
        templateProperty.add(template.getUID());
        String titleRow = Utils.getRow(columnWidths, templateProperty);

        List<String> templateContent = new ArrayList<>();
        columnWidths = new int[] { COLUMN_PROPERTY, COLUMN_PROPERTY_VALUE };
        templateProperty.set(0, UID);
        templateProperty.add(template.getUID());
        templateContent.add(Utils.getRow(columnWidths, templateProperty));
        if (template.getLabel() != null) {
            templateProperty.set(0, LABEL);
            templateProperty.set(1, template.getLabel());
            templateContent.add(Utils.getRow(columnWidths, templateProperty));
        }
        if (template.getDescription() != null) {
            templateProperty.set(0, DESCRIPTION);
            templateProperty.set(1, template.getDescription());
            templateContent.add(Utils.getRow(columnWidths, templateProperty));
        }
        templateProperty.set(0, VISIBILITY);
        templateProperty.set(1, template.getVisibility().toString());
        templateContent.add(Utils.getRow(columnWidths, templateProperty));

        templateProperty.set(0, TAGS);
        templateProperty.set(1, getTagsRecord(template.getTags()));
        templateContent.add(Utils.getRow(columnWidths, templateProperty));
        if (template instanceof RuleTemplate) {
            templateContent.addAll(collectRecords(columnWidths, CONFIGURATION_DESCRIPTIONS,
                    getConfigurationDescriptionRecords(((RuleTemplate) template).getConfigurationDescriptions())));
            templateContent.addAll(collectRecords(columnWidths, TRIGGERS, ((RuleTemplate) template).getTriggers()));
            templateContent.addAll(collectRecords(columnWidths, CONDITIONS, ((RuleTemplate) template).getConditions()));
            templateContent.addAll(collectRecords(columnWidths, ACTIONS, ((RuleTemplate) template).getActions()));
        }
        return Utils.getTableContent(TABLE_WIDTH, columnWidths, templateContent, titleRow);
    }

    /**
     * This method is responsible for printing the {@link ModuleType}.
     *
     * @param moduleType the {@link ModuleType} for printing.
     * @return a formated string, representing the {@link ModuleType} info.
     */
    static String printModuleType(ModuleType moduleType) {
        int[] columnWidths = new int[] { TABLE_WIDTH };
        List<String> moduleTypeProperty = new ArrayList<>();
        moduleTypeProperty.add(moduleType.getUID());
        String titleRow = Utils.getRow(columnWidths, moduleTypeProperty);

        List<String> moduleTypeContent = new ArrayList<>();
        columnWidths = new int[] { COLUMN_PROPERTY, COLUMN_PROPERTY_VALUE };
        moduleTypeProperty.set(0, UID);
        moduleTypeProperty.add(moduleType.getUID());
        moduleTypeContent.add(Utils.getRow(columnWidths, moduleTypeProperty));
        if (moduleType.getLabel() != null) {
            moduleTypeProperty.set(0, LABEL);
            moduleTypeProperty.set(1, moduleType.getLabel());
            moduleTypeContent.add(Utils.getRow(columnWidths, moduleTypeProperty));
        }
        if (moduleType.getDescription() != null) {
            moduleTypeProperty.set(0, DESCRIPTION);
            moduleTypeProperty.set(1, moduleType.getDescription());
            moduleTypeContent.add(Utils.getRow(columnWidths, moduleTypeProperty));
        }
        moduleTypeProperty.set(0, VISIBILITY);
        moduleTypeProperty.set(1, moduleType.getVisibility().toString());
        moduleTypeContent.add(Utils.getRow(columnWidths, moduleTypeProperty));

        moduleTypeProperty.set(0, TAGS);
        moduleTypeProperty.set(1, getTagsRecord(moduleType.getTags()));
        moduleTypeContent.add(Utils.getRow(columnWidths, moduleTypeProperty));

        moduleTypeContent.addAll(collectRecords(columnWidths, CONFIGURATION_DESCRIPTIONS,
                getConfigurationDescriptionRecords(moduleType.getConfigurationDescriptions())));
        if (moduleType instanceof TriggerType) {
            moduleTypeContent.addAll(collectRecords(columnWidths, OUTPUTS, ((TriggerType) moduleType).getOutputs()));
        }
        if (moduleType instanceof ConditionType) {
            moduleTypeContent.addAll(collectRecords(columnWidths, INPUTS, ((ConditionType) moduleType).getInputs()));
        }
        if (moduleType instanceof ActionType) {
            moduleTypeContent.addAll(collectRecords(columnWidths, INPUTS, ((ActionType) moduleType).getInputs()));
            moduleTypeContent.addAll(collectRecords(columnWidths, OUTPUTS, ((ActionType) moduleType).getOutputs()));
        }
        if (moduleType instanceof CompositeTriggerType) {
            moduleTypeContent
                    .addAll(collectRecords(columnWidths, CHILDREN, ((CompositeTriggerType) moduleType).getChildren()));
        }
        if (moduleType instanceof CompositeConditionType) {
            moduleTypeContent.addAll(
                    collectRecords(columnWidths, CHILDREN, ((CompositeConditionType) moduleType).getChildren()));
        }
        if (moduleType instanceof CompositeActionType) {
            moduleTypeContent
                    .addAll(collectRecords(columnWidths, CHILDREN, ((CompositeActionType) moduleType).getChildren()));
        }
        return Utils.getTableContent(TABLE_WIDTH, columnWidths, moduleTypeContent, titleRow);
    }

    /**
     * This method is responsible for printing the {@link RuleStatus}.
     *
     * @param ruleUID specifies the rule, which status is requested.
     * @param status corresponds to the status of specified rule.
     * @return a string representing the response of the command {@link AutomationCommands#ENABLE_RULE}.
     */
    static String printRuleStatus(String ruleUID, RuleStatus status) {
        List<String> title = new ArrayList<>();
        title.add(ruleUID + " [ " + status + " ]");
        String titleRow = Utils.getRow(new int[] { TABLE_WIDTH }, title);
        List<String> res = Utils.getTableTitle(titleRow, TABLE_WIDTH);
        StringBuilder sb = new StringBuilder();
        for (String line : res) {
            sb.append(line + Utils.ROW_END);
        }
        return sb.toString();
    }

    /**
     * This method is responsible for printing the strings, representing the auxiliary automation objects.
     *
     * @param columnWidths represents the column widths of the table.
     * @param width represents the table width.
     * @param prop is a property name of the property with value the collection of the auxiliary automation objects for
     *            printing.
     * @param list with the auxiliary automation objects for printing.
     * @return list of strings, representing the auxiliary automation objects.
     */
    @SuppressWarnings("unchecked")
    private static List<String> collectRecords(int[] columnWidths, String prop, Collection<?> list) {
        List<String> res = new ArrayList<>();
        boolean isFirst = true;
        boolean isList = false;
        List<String> values = new ArrayList<>();
        values.add(prop);
        values.add("");
        if (list != null && !list.isEmpty()) {
            for (Object element : list) {
                if (element instanceof String) {
                    res.add(Utils.getColumn(columnWidths[0], values.get(0)) + (String) element);
                    if (isFirst) {
                        isFirst = false;
                        values.set(0, "");
                    }
                } else if (element instanceof Module) {
                    List<String> moduleRecords = getModuleRecords((Module) element);
                    for (String elementRecord : moduleRecords) {
                        res.add(Utils.getColumn(columnWidths[0], values.get(0)) + elementRecord);
                        if (isFirst) {
                            isFirst = false;
                            values.set(0, "");
                        }
                    }
                } else {
                    isList = true;
                    if (isFirst) {
                        values.set(1, "[");
                        res.add(Utils.getRow(columnWidths, values));
                        isFirst = false;
                    }
                    values.set(0, "");
                    if (element instanceof FilterCriteria) {
                        values.set(1, getFilterCriteriaRecord((FilterCriteria) element));
                    } else if (element instanceof ParameterOption) {
                        values.set(1, getParameterOptionRecord((ParameterOption) element));
                    } else if (element instanceof Input) {
                        values.set(1, getInputRecord((Input) element));
                    } else if (element instanceof Output) {
                        values.set(1, getOutputRecord((Output) element));
                    } else if (element instanceof Entry) {
                        values.set(1, "  " + ((Entry<String, ?>) element).getKey() + " = \""
                                + ((Entry<String, ?>) element).getValue().toString() + "\"");
                    }
                    res.add(Utils.getRow(columnWidths, values));
                }
            }
            if (isList) {
                values.set(0, "");
                values.set(1, "]");
                res.add(Utils.getRow(columnWidths, values));
            }
        }
        return res;
    }

    /**
     * This method is responsible for printing the {@link Module}.
     *
     * @param module the {@link Module} for printing.
     * @return a formated string, representing the {@link Module}.
     */
    private static List<String> getModuleRecords(Module module) {
        int[] columnWidths = new int[] { COLUMN_PROPERTY_VALUE };
        List<String> columnValues = new ArrayList<>();
        columnValues.add(module.getId());
        List<String> moduleContent = new ArrayList<>();
        moduleContent.addAll(Utils.getTableTitle(Utils.getRow(columnWidths, columnValues), COLUMN_PROPERTY_VALUE));

        columnWidths = new int[] { COLUMN_CONFIG_PARAMETER, COLUMN_CONFIG_PARAMETER_VALUE };
        columnValues.set(0, ID);
        columnValues.add(module.getId());
        moduleContent.add(Utils.getRow(columnWidths, columnValues));

        if (module.getLabel() != null) {
            columnValues.set(0, LABEL);
            columnValues.set(1, module.getLabel());
            moduleContent.add(Utils.getRow(columnWidths, columnValues));
        }
        if (module.getDescription() != null) {
            columnValues.set(0, DESCRIPTION);
            columnValues.set(1, module.getDescription());
            moduleContent.add(Utils.getRow(columnWidths, columnValues));
        }

        columnValues.set(0, TYPE);
        columnValues.set(1, module.getTypeUID());
        moduleContent.add(Utils.getRow(columnWidths, columnValues));

        moduleContent.addAll(
                collectRecords(columnWidths, CONFIGURATION, module.getConfiguration().getProperties().entrySet()));
        Map<String, String> inputs = null;
        if (module instanceof Condition) {
            inputs = ((Condition) module).getInputs();
        }
        if (module instanceof Action) {
            inputs = ((Action) module).getInputs();
        }
        if (inputs != null && !inputs.isEmpty()) {
            moduleContent.addAll(collectRecords(columnWidths, INPUTS, new ArrayList<>(inputs.entrySet())));
        }
        return moduleContent;
    }

    private static String getParameterOptionRecord(ParameterOption option) {
        return "  value=\"" + option.getValue() + "\", label=\"" + option.getLabel() + "\"";
    }

    private static String getFilterCriteriaRecord(FilterCriteria criteria) {
        return "  name=\"" + criteria.getName() + "\", value=\"" + criteria.getValue() + "\"";
    }

    private static String getInputRecord(Input input) {
        return "  name=\"" + input.getName() + "\", label=\"" + input.getLabel() + "\", decription=\""
                + input.getDescription() + "\", type=\"" + input.getType() + "\", "
                + (input.isRequired() ? REQUIRED : NOT_REQUIRED)
                + (input.getDefaultValue() != null ? "\", default=\"" + input.getDefaultValue() : "");
    }

    private static String getOutputRecord(Output output) {
        return "  name=\"" + output.getName() + "\", label=\"" + output.getLabel() + "\", decription=\""
                + output.getDescription() + "\", type=\"" + output.getType() + "\"";
    }

    /**
     * This method is responsible for printing the set of {@link ConfigDescriptionParameter}s.
     *
     * @param configDescriptions set of {@link ConfigDescriptionParameter}s for printing.
     * @return a formated string, representing the set of {@link ConfigDescriptionParameter}s.
     */
    private static List<String> getConfigurationDescriptionRecords(
            List<ConfigDescriptionParameter> configDescriptions) {
        List<String> configParamContent = new ArrayList<>();
        if (configDescriptions != null && !configDescriptions.isEmpty()) {
            for (ConfigDescriptionParameter parameter : configDescriptions) {
                int[] columnWidths = new int[] { COLUMN_CONFIG_PARAMETER, COLUMN_CONFIG_PARAMETER_PROP,
                        COLUMN_CONFIG_PARAMETER_PROP_VALUE };
                configParamContent.add(Utils.getColumn(COLUMN_PROPERTY_VALUE, parameter.getName() + " : "));
                List<String> configParamProperty = new ArrayList<>();
                configParamProperty.add("");
                configParamProperty.add(TYPE);
                configParamProperty.add(parameter.getType().toString());
                configParamContent.add(Utils.getRow(columnWidths, configParamProperty));
                if (parameter.getLabel() != null) {
                    configParamProperty.set(1, LABEL);
                    configParamProperty.set(2, parameter.getLabel());
                    configParamContent.add(Utils.getRow(columnWidths, configParamProperty));
                }
                if (parameter.getDescription() != null) {
                    configParamProperty.set(1, DESCRIPTION);
                    configParamProperty.set(2, parameter.getDescription());
                    configParamContent.add(Utils.getRow(columnWidths, configParamProperty));
                }
                if (parameter.getDefault() != null) {
                    configParamProperty.set(1, DEFAULT);
                    configParamProperty.set(2, parameter.getDefault());
                    configParamContent.add(Utils.getRow(columnWidths, configParamProperty));
                }
                if (parameter.getContext() != null) {
                    configParamProperty.set(1, CONTEXT);
                    configParamProperty.set(2, parameter.getContext());
                    configParamContent.add(Utils.getRow(columnWidths, configParamProperty));
                }
                if (parameter.getPattern() != null) {
                    configParamProperty.set(1, PATTERN);
                    configParamProperty.set(2, parameter.getPattern());
                    configParamContent.add(Utils.getRow(columnWidths, configParamProperty));
                }
                if (parameter.getStepSize() != null) {
                    configParamProperty.set(1, STEP_SIZE);
                    configParamProperty.set(2, parameter.getStepSize().toString());
                    configParamContent.add(Utils.getRow(columnWidths, configParamProperty));
                }
                if (parameter.getMinimum() != null) {
                    configParamProperty.set(1, MIN);
                    configParamProperty.set(2, parameter.getMinimum().toString());
                    configParamContent.add(Utils.getRow(columnWidths, configParamProperty));
                }
                if (parameter.getMaximum() != null) {
                    configParamProperty.set(1, MAX);
                    configParamProperty.set(2, parameter.getMaximum().toString());
                    configParamContent.add(Utils.getRow(columnWidths, configParamProperty));
                }
                columnWidths = new int[] { COLUMN_CONFIG_PARAMETER_PROP, COLUMN_CONFIG_PARAMETER_PROP_VALUE };
                List<String> options = collectRecords(columnWidths, OPTIONS, parameter.getOptions());
                for (String option : options) {
                    configParamContent.add(Utils.getColumn(COLUMN_CONFIG_PARAMETER, "") + option);
                }
                List<String> filters = collectRecords(columnWidths, FILTER_CRITERIA, parameter.getFilterCriteria());
                for (String filter : filters) {
                    configParamContent.add(Utils.getColumn(COLUMN_CONFIG_PARAMETER, "") + filter);
                }
                configParamContent
                        .add(Utils.getColumn(COLUMN_PROPERTY_VALUE, Utils.printChars('-', COLUMN_PROPERTY_VALUE)));
            }
        }
        return configParamContent;
    }

    /**
     * This method is responsible for printing the set of {@link Input}s or {@link Output}s or {@link Inputs}s.
     *
     * @param set is the set of {@link Input}s or {@link Output}s or {@link Inputs}s for printing.
     * @return a formated string, representing the set of {@link Input}s or {@link Output}s or {@link Input}s.
     */
    private static String getTagsRecord(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[ ]";
        }
        StringBuilder res = new StringBuilder().append("[ ");
        int i = 1;
        for (String tag : tags) {
            if (i < tags.size()) {
                res.append(tag + ", ");
            } else {
                res.append(tag);
            }
            i++;
        }
        return res.append(" ]").toString();
    }

    /**
     * This method is responsible for constructing the rows of a table with 2 columns - first column is for the
     * numbering, second column is for the numbered records.
     *
     * @param list is the list with row values for printing.
     * @param rows is used for accumulation of result
     * @param columnWidths represents the column widths of the table.
     */
    private static void collectListRecords(Map<String, String> list, List<String> rows, int[] columnWidths) {
        for (int i = 1; i <= list.size(); i++) {
            String id = new Integer(i).toString();
            String uid = list.get(id);
            List<String> columnValues = new ArrayList<>();
            columnValues.add(id);
            columnValues.add(uid);
            rows.add(Utils.getRow(columnWidths, columnValues));
        }
    }
}
