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
package org.openhab.core.model.rule.formatting;

import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.formatting.IElementMatcherProvider.IElementMatcher;
import org.eclipse.xtext.formatting.impl.AbstractFormattingConfig.ElementLocator;
import org.eclipse.xtext.formatting.impl.AbstractFormattingConfig.ElementPattern;
import org.eclipse.xtext.formatting.impl.FormattingConfig;
import org.eclipse.xtext.formatting.impl.FormattingConfigBasedStream;
import org.eclipse.xtext.parsetree.reconstr.IHiddenTokenHelper;
import org.eclipse.xtext.parsetree.reconstr.ITokenStream;

/**
 * This class exists only to override indentation logic.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class RuleFormattingConfigBasedStream extends FormattingConfigBasedStream {

    public RuleFormattingConfigBasedStream(@Nullable ITokenStream out, @Nullable String initialIndentation,
            FormattingConfig cfg, IElementMatcher<@Nullable ElementPattern> matcher,
            IHiddenTokenHelper hiddenTokenHelper, boolean preserveSpaces) {
        super(out, initialIndentation, cfg, matcher, hiddenTokenHelper, preserveSpaces);
    }

    @Override
    protected @NonNullByDefault({}) Set<ElementLocator> collectLocators(EObject ele) {

        Set<@Nullable ElementLocator> result = super.collectLocators(ele);
        if (indentationLevel < 0) {
            indentationLevel = 0;
        }
        return result;
    }
}
