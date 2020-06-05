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
package org.openhab.core.model.core.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.resource.SynchronizedXtextResourceSet;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.openhab.core.model.core.EventType;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.model.core.SafeEMF;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kai Kreuzer - Initial contribution
 * @author Oliver Libutzki - Added reloadAllModelsOfType method
 * @author Simon Kaufmann - added validation of models before loading them
 */
@Component(immediate = true)
@NonNullByDefault
public class ModelRepositoryImpl implements ModelRepository {

    private final Logger logger = LoggerFactory.getLogger(ModelRepositoryImpl.class);
    private final ResourceSet resourceSet;
    private final Map<String, String> resourceOptions = Collections.singletonMap(XtextResource.OPTION_ENCODING,
            StandardCharsets.UTF_8.name());

    private final List<ModelRepositoryChangeListener> listeners = new CopyOnWriteArrayList<>();

    private final SafeEMF safeEmf;

    @Activate
    public ModelRepositoryImpl(final @Reference SafeEMF safeEmf) {
        this.safeEmf = safeEmf;

        XtextResourceSet xtextResourceSet = new SynchronizedXtextResourceSet();
        xtextResourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
        this.resourceSet = xtextResourceSet;
        // don't use XMI as a default
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove("*");
    }

    @Override
    public @Nullable EObject getModel(String name) {
        synchronized (resourceSet) {
            Resource resource = getResource(name);
            if (resource != null) {
                if (!resource.getContents().isEmpty()) {
                    return resource.getContents().get(0);
                } else {
                    logger.warn("Configuration model '{}' is either empty or cannot be parsed correctly!", name);
                    resourceSet.getResources().remove(resource);
                    return null;
                }
            } else {
                logger.trace("Configuration model '{}' can not be found", name);
                return null;
            }
        }
    }

    @Override
    public boolean addOrRefreshModel(String name, final InputStream originalInputStream) {
        Resource resource = null;
        InputStream inputStream = null;
        try {
            if (originalInputStream != null) {
                byte[] bytes = originalInputStream.readAllBytes();
                String validationResult = validateModel(name, new ByteArrayInputStream(bytes));
                if (validationResult != null) {
                    logger.warn("Configuration model '{}' has errors, therefore ignoring it: {}", name,
                            validationResult);
                    removeModel(name);
                    return false;
                }
                inputStream = new ByteArrayInputStream(bytes);
            }
            resource = getResource(name);
            if (resource == null) {
                synchronized (resourceSet) {
                    // try again to retrieve the resource as it might have been created by now
                    resource = getResource(name);
                    if (resource == null) {
                        // seems to be a new file
                        // don't use XMI as a default
                        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove("*");
                        resource = resourceSet.createResource(URI.createURI(name));
                        if (resource != null) {
                            logger.info("Loading model '{}'", name);
                            if (inputStream == null) {
                                logger.warn(
                                        "Resource '{}' not found. You have to pass an inputStream to create the resource.",
                                        name);
                                return false;
                            }
                            resource.load(inputStream, resourceOptions);
                            notifyListeners(name, EventType.ADDED);
                            return true;
                        } else {
                            logger.warn("Ignoring file '{}' as we do not have a parser for it.", name);
                        }
                    }
                }
            } else {
                synchronized (resourceSet) {
                    resource.unload();
                    logger.info("Refreshing model '{}'", name);
                    if (inputStream == null) {
                        resource.load(resourceOptions);
                    } else {
                        resource.load(inputStream, resourceOptions);
                    }
                    notifyListeners(name, EventType.MODIFIED);
                    return true;
                }
            }
        } catch (IOException e) {
            logger.warn("Configuration model '{}' cannot be parsed correctly!", name, e);
            if (resource != null) {
                resourceSet.getResources().remove(resource);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeModel(String name) {
        Resource resource = getResource(name);
        if (resource != null) {
            synchronized (resourceSet) {
                // do not physically delete it, but remove it from the resource set
                notifyListeners(name, EventType.REMOVED);
                resourceSet.getResources().remove(resource);
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public Iterable<String> getAllModelNamesOfType(final String modelType) {
        synchronized (resourceSet) {
            // Make a copy to avoid ConcurrentModificationException
            List<Resource> resourceListCopy = new ArrayList<>(resourceSet.getResources());

            return resourceListCopy.stream().filter(input -> {
                return input != null && input.getURI().lastSegment().contains(".") && input.isLoaded()
                        && modelType.equalsIgnoreCase(input.getURI().fileExtension());
            }).map(from -> {
                return from.getURI().path();
            }).collect(Collectors.toList());
        }
    }

    @Override
    public void reloadAllModelsOfType(final String modelType) {
        synchronized (resourceSet) {
            // Make a copy to avoid ConcurrentModificationException
            List<Resource> resourceListCopy = new ArrayList<>(resourceSet.getResources());
            for (Resource resource : resourceListCopy) {
                if (resource != null && resource.getURI().lastSegment().contains(".") && resource.isLoaded()) {
                    if (modelType.equalsIgnoreCase(resource.getURI().fileExtension())) {
                        XtextResource xtextResource = (XtextResource) resource;
                        // It's not sufficient to discard the derived state.
                        // The quick & dirts solution is to reparse the whole resource.
                        // We trigger this by dummy updating the resource.
                        logger.debug("Refreshing resource '{}'", resource.getURI().lastSegment());
                        xtextResource.update(1, 0, "");
                        notifyListeners(resource.getURI().lastSegment(), EventType.MODIFIED);
                    }
                }
            }
        }
    }

    @Override
    public Set<String> removeAllModelsOfType(final String modelType) {
        Set<String> ret = new HashSet<>();
        synchronized (resourceSet) {
            // Make a copy to avoid ConcurrentModificationException
            List<Resource> resourceListCopy = new ArrayList<>(resourceSet.getResources());
            for (Resource resource : resourceListCopy) {
                if (resource != null && resource.getURI().lastSegment().contains(".") && resource.isLoaded()) {
                    if (modelType.equalsIgnoreCase(resource.getURI().fileExtension())) {
                        logger.debug("Removing resource '{}'", resource.getURI().lastSegment());
                        ret.add(resource.getURI().lastSegment());
                        resourceSet.getResources().remove(resource);
                        notifyListeners(resource.getURI().lastSegment(), EventType.REMOVED);
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public void addModelRepositoryChangeListener(ModelRepositoryChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeModelRepositoryChangeListener(ModelRepositoryChangeListener listener) {
        listeners.remove(listener);
    }

    private @Nullable Resource getResource(String name) {
        return resourceSet.getResource(URI.createURI(name), false);
    }

    /**
     * Validates the given model.
     *
     * There are two "layers" of validation
     * <ol>
     * <li>
     * errors when loading the resource. Usually these are syntax violations which irritate the parser. They will be
     * returned as a String.
     * <li>
     * all kinds of other errors (i.e. violations of validation checks) will only be logged, but not included in the
     * return value.
     * </ol>
     * <p>
     * Validation will be done on a separate resource, in order to keep the original one intact in case its content
     * needs to be removed because of syntactical errors.
     *
     * @param name
     * @param inputStream
     * @return error messages as a String if any syntactical error were found, <code>null</code> otherwise
     * @throws IOException if there was an error with the given {@link InputStream}, loading the resource from there
     */
    private @Nullable String validateModel(String name, InputStream inputStream) throws IOException {
        // use another resource for validation in order to keep the original one for emergency-removal in case of errors
        Resource resource = resourceSet.createResource(URI.createURI("tmp_" + name));
        try {
            resource.load(inputStream, resourceOptions);
            StringBuilder criticalErrors = new StringBuilder();
            List<String> warnings = new LinkedList<>();

            if (!resource.getContents().isEmpty()) {
                // Check for syntactical errors
                for (Diagnostic diagnostic : resource.getErrors()) {
                    criticalErrors
                            .append(MessageFormat.format("[{0},{1}]: {2}\n", Integer.toString(diagnostic.getLine()),
                                    Integer.toString(diagnostic.getColumn()), diagnostic.getMessage()));
                }
                if (criticalErrors.length() > 0) {
                    return criticalErrors.toString();
                }

                // Check for validation errors, but log them only
                try {
                    final org.eclipse.emf.common.util.Diagnostic diagnostic = safeEmf
                            .call(() -> Diagnostician.INSTANCE.validate(resource.getContents().get(0)));
                    for (org.eclipse.emf.common.util.Diagnostic d : diagnostic.getChildren()) {
                        warnings.add(d.getMessage());
                    }
                    if (!warnings.isEmpty()) {
                        logger.info("Validation issues found in configuration model '{}', using it anyway:\n{}", name,
                                warnings.stream().collect(Collectors.joining("\n")));
                    }
                } catch (NullPointerException e) {
                    // see https://github.com/eclipse/smarthome/issues/3335
                    logger.debug("Validation of '{}' skipped due to internal errors.", name);
                }
            }
        } finally {
            resourceSet.getResources().remove(resource);
        }
        return null;
    }

    private void notifyListeners(String name, EventType type) {
        for (ModelRepositoryChangeListener listener : listeners) {
            listener.modelChanged(name, type);
        }
    }
}
