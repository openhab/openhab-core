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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.parser.ParsingException;
import org.openhab.core.automation.parser.ParsingNestedException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * This class is a {@link Rule}s importer. It extends functionality of {@link AbstractCommandProvider}.
 * <p>
 * It is responsible for execution of Automation Commands, corresponding to the {@link Rule}s:
 * <ul>
 * <li>imports the {@link Rule}s from local files or from URL resources
 * <li>provides functionality for persistence of the {@link Rule}s
 * <li>removes the {@link Rule}s and their persistence
 * <li>lists the {@link Rule}s and their details
 * </ul>
 *
 * @author Ana Dimova - Initial contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 */
@NonNullByDefault
public class CommandlineRuleImporter extends AbstractCommandProvider<Rule> {

    private final RuleRegistry ruleRegistry;

    /**
     * This constructor creates instances of this particular implementation of Rule Importer. It does not add any new
     * functionality to the constructors of the providers. Only provides consistency by invoking the parent's
     * constructor.
     *
     * @param context is the {@link BundleContext}, used for creating a tracker for {@link Parser} services.
     * @param ruleRegistry
     */
    public CommandlineRuleImporter(BundleContext context, RuleRegistry ruleRegistry) {
        super(context);
        this.ruleRegistry = ruleRegistry;
    }

    /**
     * This method differentiates what type of {@link Parser}s is tracked by the tracker.
     * For this concrete provider, this type is a {@link Rule} {@link Parser}.
     *
     * @see AbstractCommandProvider#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public @Nullable Object addingService(@SuppressWarnings("rawtypes") @Nullable ServiceReference reference) {
        if (reference != null && Parser.PARSER_RULE.equals(reference.getProperty(Parser.PARSER_TYPE))) {
            return super.addingService(reference);
        }
        return null;
    }

    /**
     * This method is responsible for exporting a set of Rules in a specified file.
     *
     * @param parserType is relevant to the format that you need for conversion of the Rules in text.
     * @param set a set of Rules to export.
     * @param file a specified file for export.
     * @throws Exception when I/O operation has failed or has been interrupted or generating of the text fails
     *             for some reasons.
     * @see AutomationCommandsPluggable#exportRules(String, Set, File)
     */
    public String exportRules(String parserType, Set<Rule> set, File file) throws Exception {
        return super.exportData(parserType, set, file);
    }

    /**
     * This method is responsible for importing a set of Rules from a specified file or URL resource.
     *
     * @param parserType is relevant to the format that you need for conversion of the Rules in text.
     * @param url a specified URL for import.
     * @throws IOException when I/O operation has failed or has been interrupted.
     * @throws ParsingException when parsing of the text fails for some reasons.
     * @see AutomationCommandsPluggable#importRules(String, URL)
     */
    public Set<Rule> importRules(String parserType, URL url) throws IOException, ParsingException {
        Parser<Rule> parser = parsers.get(parserType);
        if (parser != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(new BufferedInputStream(url.openStream()));
            try {
                return importData(url, parser, inputStreamReader);
            } finally {
                inputStreamReader.close();
            }
        } else {
            throw new ParsingException(new ParsingNestedException(ParsingNestedException.RULE, null,
                    new Exception("Parser " + parserType + " not available")));
        }
    }

    @Override
    protected Set<Rule> importData(URL url, Parser<Rule> parser, InputStreamReader inputStreamReader)
            throws ParsingException {
        Set<Rule> providedRules = parser.parse(inputStreamReader);
        if (providedRules != null && !providedRules.isEmpty()) {
            Iterator<Rule> i = providedRules.iterator();
            while (i.hasNext()) {
                Rule rule = i.next();
                if (rule != null) {
                    if (ruleRegistry.get(rule.getUID()) != null) {
                        ruleRegistry.update(rule);
                    } else {
                        ruleRegistry.add(rule);
                    }
                }
            }
        }
        return providedRules;
    }
}
