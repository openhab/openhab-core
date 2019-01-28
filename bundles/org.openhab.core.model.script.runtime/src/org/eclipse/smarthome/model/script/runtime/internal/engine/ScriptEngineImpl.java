/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.script.runtime.internal.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.smarthome.model.core.ModelParser;
import org.eclipse.smarthome.model.script.ScriptServiceUtil;
import org.eclipse.smarthome.model.script.ScriptStandaloneSetup;
import org.eclipse.smarthome.model.script.engine.Script;
import org.eclipse.smarthome.model.script.engine.ScriptEngine;
import org.eclipse.smarthome.model.script.engine.ScriptExecutionException;
import org.eclipse.smarthome.model.script.engine.ScriptParsingException;
import org.eclipse.smarthome.model.script.runtime.ScriptRuntime;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.StringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.xbase.XExpression;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the implementation of a {@link ScriptEngine} which is made available as an OSGi service.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Oliver Libutzki - Reorganization of Guice injection
 *
 */
@Component(immediate = true)
public class ScriptEngineImpl implements ScriptEngine, ModelParser {

    protected XtextResourceSet resourceSet;

    private final Logger logger = LoggerFactory.getLogger(ScriptEngineImpl.class);

    private ScriptServiceUtil scriptServiceUtil;

    public ScriptEngineImpl() {
    }

    @Activate
    public void activate() {
        ScriptStandaloneSetup.doSetup(scriptServiceUtil, this);
        logger.debug("Registered 'script' configuration parser");
    }

    private XtextResourceSet getResourceSet() {
        if (resourceSet == null) {
            resourceSet = ScriptStandaloneSetup.getInjector().getInstance(XtextResourceSet.class);
            resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.FALSE);
        }
        return resourceSet;
    }

    @Deactivate
    public void deactivate() {
        this.resourceSet = null;
        ScriptStandaloneSetup.unregister();
    }

    /**
     * we need to make sure that the scriptRuntime service has been started before us
     */
    protected void setScriptRuntime(final ScriptRuntime scriptRuntime) {
    }

    protected void unsetScriptRuntime(final ScriptRuntime scriptRuntime) {
    }

    @Reference
    protected void setScriptServiceUtil(ScriptServiceUtil scriptServiceUtil) {
        this.scriptServiceUtil = scriptServiceUtil;
        scriptServiceUtil.setScriptEngine(this);
    }

    protected void unsetScriptServiceUtil(ScriptServiceUtil scriptServiceUtil) {
        scriptServiceUtil.unsetScriptEngine(this);
        this.scriptServiceUtil = null;
    }

    @Override
    public Script newScriptFromString(String scriptAsString) throws ScriptParsingException {
        return newScriptFromXExpression(parseScriptIntoXTextEObject(scriptAsString));
    }

    @Override
    public Script newScriptFromXExpression(XExpression expression) {
        ScriptImpl script = ScriptStandaloneSetup.getInjector().getInstance(ScriptImpl.class);
        script.setXExpression(expression);
        return script;
    }

    @Override
    public Object executeScript(String scriptAsString) throws ScriptParsingException, ScriptExecutionException {
        return newScriptFromString(scriptAsString).execute();
    }

    private XExpression parseScriptIntoXTextEObject(String scriptAsString) throws ScriptParsingException {
        XtextResourceSet resourceSet = getResourceSet();
        Resource resource = resourceSet.createResource(computeUnusedUri(resourceSet)); // IS-A XtextResource
        try {
            resource.load(new StringInputStream(scriptAsString, StandardCharsets.UTF_8.name()),
                    resourceSet.getLoadOptions());
        } catch (IOException e) {
            throw new ScriptParsingException(
                    "Unexpected IOException; from close() of a String-based ByteArrayInputStream, no real I/O; how is that possible???",
                    scriptAsString, e);
        }

        List<Diagnostic> errors = resource.getErrors();
        if (errors.size() != 0) {
            throw new ScriptParsingException("Failed to parse expression (due to managed SyntaxError/s)",
                    scriptAsString).addDiagnosticErrors(errors);
        }

        EList<EObject> contents = resource.getContents();

        if (!contents.isEmpty()) {
            Iterable<Issue> validationErrors = getValidationErrors(contents.get(0));
            if (!validationErrors.iterator().hasNext()) {
                return (XExpression) contents.get(0);
            } else {
                throw new ScriptParsingException("Failed to parse expression (due to managed ValidationError/s)",
                        scriptAsString).addValidationIssues(validationErrors);
            }
        } else {
            return null;
        }
    }

    protected URI computeUnusedUri(ResourceSet resourceSet) {
        String name = "__synthetic";
        final int MAX_TRIES = 1000;
        for (int i = 0; i < MAX_TRIES; i++) {
            // NOTE: The "filename extension" (".script") must match the file.extensions in the *.mwe2
            URI syntheticUri = URI.createURI(name + Math.random() + "." + Script.SCRIPT_FILEEXT);
            if (resourceSet.getResource(syntheticUri, false) == null) {
                return syntheticUri;
            }
        }
        throw new IllegalStateException();
    }

    protected List<Issue> validate(EObject model) {
        IResourceValidator validator = ((XtextResource) model.eResource()).getResourceServiceProvider()
                .getResourceValidator();
        return validator.validate(model.eResource(), CheckMode.ALL, CancelIndicator.NullImpl);
    }

    protected Iterable<Issue> getValidationErrors(final EObject model) {
        final List<Issue> validate = validate(model);
        return validate.stream().filter(input -> Severity.ERROR == input.getSeverity()).collect(Collectors.toList());
    }

    @Override
    public String getExtension() {
        return "script";
    }

}
