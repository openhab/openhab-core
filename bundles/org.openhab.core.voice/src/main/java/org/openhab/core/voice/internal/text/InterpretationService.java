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
package org.openhab.core.voice.internal.text;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.InterpreterContext;
import org.openhab.core.voice.text.UnrecoverableInterpretationException;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationException;
import org.openhab.core.voice.text.conversation.ConversationRole;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for executing human language interpretation across candidate interpreters.
 * Encapsulates conversation history recording, fallback loops, and error recording.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = InterpretationService.class)
@NonNullByDefault
public class InterpretationService {

    private final Logger logger = LoggerFactory.getLogger(InterpretationService.class);

    private final LocaleProvider localeProvider;
    private final SystemPromptEnricher systemPromptEnricher;

    @Activate
    public InterpretationService(final @Reference LocaleProvider localeProvider,
            final @Reference SystemPromptEnricher systemPromptEnricher) {
        this.localeProvider = localeProvider;
        this.systemPromptEnricher = systemPromptEnricher;
    }

    /**
     * Interprets user text using the given interpreters and context.
     *
     * @param text text to interpret
     * @param interpreters candidate interpreters to try in sequence
     * @param conversation conversation for recording dialogue
     * @param tools tools available for LLM interpreters
     * @param locale target locale
     * @param locationItem optional location item
     * @param baseSystemPrompt optional base LLM system prompt
     * @return the interpretation result
     * @throws InterpretationException if interpretation fails
     */
    public String interpret(String text, List<HumanLanguageInterpreter> interpreters, Conversation conversation,
            Collection<LLMTool> tools, @Nullable Locale locale, @Nullable String locationItem,
            @Nullable String baseSystemPrompt) throws InterpretationException {
        try {
            conversation.addMessage(ConversationRole.USER, text);
        } catch (ConversationException e) {
            String errMsg = e.getMessage();
            throw new InterpretationException(
                    errMsg != null ? errMsg : "Unknown exception adding user message to conversation");
        }

        Locale nullSafeLocale = locale != null ? locale : localeProvider.getLocale();
        String enrichedPrompt = systemPromptEnricher.enrichSystemPrompt(baseSystemPrompt, nullSafeLocale);

        InterpreterContext context = new InterpreterContext(conversation, tools, locationItem, enrichedPrompt);
        InterpretationException exception = null;

        for (var interpreter : interpreters) {
            try {
                String answer = interpreter.interpret(nullSafeLocale, context);
                logger.debug("Interpretation result from interpreter '{}': {}", interpreter.getId(), answer);
                return answer;
            } catch (UnrecoverableInterpretationException e) {
                logger.debug("Unrecoverable interpretation exception from interpreter '{}': {}", interpreter.getId(),
                        e.getMessage());
                addErrorToConversation(conversation, e.getMessage());
                throw e;
            } catch (InterpretationException e) {
                logger.debug("Interpretation exception from interpreter '{}': {}", interpreter.getId(), e.getMessage());
                exception = e;
            }
        }

        if (exception != null) {
            addErrorToConversation(conversation, exception.getMessage());
            throw exception;
        }

        throw new InterpretationException("No human language interpreter available!");
    }

    private void addErrorToConversation(Conversation conversation, @Nullable String errorMessage) {
        try {
            conversation.addMessage(ConversationRole.OPENHAB,
                    errorMessage != null ? errorMessage : "Unknown interpreter error");
        } catch (ConversationException e) {
            logger.error("Error adding error message to conversation: {}", e.getMessage());
        }
    }
}
