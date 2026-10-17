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
package org.openhab.core.voice.internal.text.interpreter;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.InterpreterContext;
import org.openhab.core.voice.text.UnrecoverableInterpretationException;
import org.openhab.core.voice.text.interpreter.llm.UnrecoverableLLMToolException;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A composite {@link HumanLanguageInterpreter} that sequentially chains up to three interpreters,
 * falling back on recoverable {@link InterpretationException} and failing fast on
 * {@link UnrecoverableInterpretationException}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@Component(service = { HumanLanguageInterpreter.class, ConfigOptionProvider.class }, immediate = true, //
        configurationPid = ChainingHumanLanguageInterpreter.CONFIGURATION_PID, //
        property = Constants.SERVICE_PID + "=" + ChainingHumanLanguageInterpreter.CONFIGURATION_PID)
@ConfigurableService(category = "system", label = ChainingHumanLanguageInterpreter.DESCRIPTION, description_uri = ChainingHumanLanguageInterpreter.CONFIG_URI)
public class ChainingHumanLanguageInterpreter implements HumanLanguageInterpreter, ConfigOptionProvider {

    public static final String ID = "chained";
    public static final String DESCRIPTION = "Chaining Human Language Interpreter";
    public static final String CONFIGURATION_PID = "org.openhab.voice.chaininghli";
    public static final String CONFIG_URI = "system:chaininghli";

    public static final String CONFIG_PRIMARY_INTERPRETER = "primaryInterpreter";
    public static final String CONFIG_SECONDARY_INTERPRETER = "secondaryInterpreter";
    public static final String CONFIG_TERTIARY_INTERPRETER = "tertiaryInterpreter";

    private final Logger logger = LoggerFactory.getLogger(ChainingHumanLanguageInterpreter.class);
    private final Map<String, HumanLanguageInterpreter> interpreters = new ConcurrentHashMap<>();

    private volatile ChainingHumanLanguageInterpreterConfiguration configuration = new ChainingHumanLanguageInterpreterConfiguration();

    @Activate
    public ChainingHumanLanguageInterpreter(Map<String, @Nullable Object> config) {
        modified(config);
    }

    @Modified
    public void modified(Map<String, @Nullable Object> config) {
        var parsed = ConfigParser.configurationAs(config, ChainingHumanLanguageInterpreterConfiguration.class);
        if (parsed != null) {
            this.configuration = parsed;
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addHumanLanguageInterpreter(HumanLanguageInterpreter hli) {
        if (!ID.equals(hli.getId())) {
            interpreters.put(hli.getId(), hli);
        }
    }

    public void removeHumanLanguageInterpreter(HumanLanguageInterpreter hli) {
        interpreters.remove(hli.getId());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return DESCRIPTION;
    }

    /**
     * Resolves the configured and available interpreters in order of evaluation.
     *
     * @return an ordered list of interpreters to evaluate
     */
    private List<HumanLanguageInterpreter> getChain() {
        List<HumanLanguageInterpreter> chain = new ArrayList<>();
        addInterpreterIfAvailable(chain, configuration.primaryInterpreter);
        addInterpreterIfAvailable(chain, configuration.secondaryInterpreter);
        addInterpreterIfAvailable(chain, configuration.tertiaryInterpreter);
        return chain;
    }

    private void addInterpreterIfAvailable(List<HumanLanguageInterpreter> chain, @Nullable String id) {
        if (id != null && !id.isBlank() && !ID.equals(id)) {
            HumanLanguageInterpreter hli = interpreters.get(id);
            if (hli != null && !chain.contains(hli)) {
                chain.add(hli);
            }
        }
    }

    private boolean isLocaleSupported(HumanLanguageInterpreter interpreter, Locale locale) {
        Set<Locale> supported = interpreter.getSupportedLocales();
        if (supported.isEmpty()) {
            return true;
        }
        return supported.contains(locale)
                || supported.stream().anyMatch(l -> l.getLanguage().equalsIgnoreCase(locale.getLanguage()));
    }

    @Override
    public String interpret(Locale locale, String text) throws InterpretationException {
        List<HumanLanguageInterpreter> chain = getChain();
        if (chain.isEmpty()) {
            throw new InterpretationException("No interpreter available in chain.");
        }
        InterpretationException lastException = null;
        for (HumanLanguageInterpreter interpreter : chain) {
            if (!isLocaleSupported(interpreter, locale)) {
                logger.debug("Skipping interpreter '{}': locale '{}' not supported", interpreter.getId(), locale);
                continue;
            }
            try {
                return interpreter.interpret(locale, text);
            } catch (UnrecoverableInterpretationException e) {
                logger.warn("Unrecoverable error in interpreter '{}': {}. Aborting chain.", interpreter.getId(),
                        e.getMessage());
                throw e;
            } catch (InterpretationException e) {
                if (e.getCause() instanceof UnrecoverableLLMToolException
                        || e.getCause() instanceof SecurityException) {
                    logger.warn("Unrecoverable tool error in interpreter '{}': {}. Aborting chain.",
                            interpreter.getId(), e.getMessage());
                    throw e;
                }
                logger.debug("Interpretation failed in interpreter '{}': {}. Trying next...", interpreter.getId(),
                        e.getMessage());
                lastException = e;
            }
        }
        throw lastException != null ? lastException
                : new InterpretationException("No interpreter in chain could process the request.");
    }

    @Override
    public String interpret(Locale locale, InterpreterContext interpreterContext) throws InterpretationException {
        List<HumanLanguageInterpreter> chain = getChain();
        if (chain.isEmpty()) {
            throw new InterpretationException("No interpreter available in chained interpreter");
        }
        InterpretationException lastException = null;
        for (HumanLanguageInterpreter interpreter : chain) {
            if (!isLocaleSupported(interpreter, locale)) {
                logger.debug("Skipping interpreter '{}': locale '{}' not supported", interpreter.getId(), locale);
                continue;
            }
            try {
                return interpreter.interpret(locale, interpreterContext);
            } catch (UnrecoverableInterpretationException e) {
                logger.warn("Unrecoverable error in interpreter '{}': {}. Aborting chain.", interpreter.getId(),
                        e.getMessage());
                throw e;
            } catch (InterpretationException e) {
                if (e.getCause() instanceof UnrecoverableLLMToolException
                        || e.getCause() instanceof SecurityException) {
                    logger.warn("Unrecoverable tool error in interpreter '{}': {}. Aborting chain.",
                            interpreter.getId(), e.getMessage());
                    throw e;
                }
                logger.debug("Interpretation failed in interpreter '{}': {}. Trying next...", interpreter.getId(),
                        e.getMessage());
                lastException = e;
            }
        }
        throw lastException != null ? lastException
                : new InterpretationException("No interpreter in chain could process the request.");
    }

    @Override
    public @Nullable String getGrammar(Locale locale, String format) {
        for (HumanLanguageInterpreter interpreter : getChain()) {
            String grammar = interpreter.getGrammar(locale, format);
            if (grammar != null) {
                return grammar;
            }
        }
        return null;
    }

    @Override
    public Set<Locale> getSupportedLocales() {
        List<HumanLanguageInterpreter> chain = getChain();
        if (chain.isEmpty()) {
            return Set.of();
        }
        Set<Locale> locales = new HashSet<>();
        for (HumanLanguageInterpreter interpreter : chain) {
            Set<Locale> supported = interpreter.getSupportedLocales();
            if (supported.isEmpty()) {
                return Set.of();
            }
            locales.addAll(supported);
        }
        return Set.copyOf(locales);
    }

    @Override
    public Set<String> getSupportedGrammarFormats() {
        return getChain().stream().flatMap(hli -> hli.getSupportedGrammarFormats().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (CONFIG_URI.equals(uri.toString()) && (CONFIG_PRIMARY_INTERPRETER.equals(param)
                || CONFIG_SECONDARY_INTERPRETER.equals(param) || CONFIG_TERTIARY_INTERPRETER.equals(param))) {
            return interpreters.values().stream().map(hli -> new ParameterOption(hli.getId(), hli.getLabel(locale)))
                    .toList();
        }
        return null;
    }
}
