/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

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
 * @author Laurent Garnier - Added method generateSyntaxFromModel
 * @author Laurent Garnier - Added method createTemporaryModel
 */
@Component(immediate = true)
@NonNullByDefault
public class ModelRepositoryImpl implements ModelRepository {

    private static final String PREFIX_TMP_MODEL = "tmp_";

    private final Logger logger = LoggerFactory.getLogger(ModelRepositoryImpl.class);
    private final ResourceSet resourceSet;
    private final Map<String, String> resourceOptions = Map.of(XtextResource.OPTION_ENCODING,
            StandardCharsets.UTF_8.name());

    private final List<ModelRepositoryChangeListener> listeners = new CopyOnWriteArrayList<>();

    private final SafeEMF safeEmf;

    private int counter;

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
                    return resource.getContents().getFirst();
                } else {
                    logger.warn("DSL model '{}' is either empty or cannot be parsed correctly!", name);
                    resourceSet.getResources().remove(resource);
                    return null;
                }
            } else {
                logger.trace("DSL model '{}' can not be found", name);
                return null;
            }
        }
    }

    @Override
    public boolean addOrRefreshModel(String name, final InputStream originalInputStream) {
        return addOrRefreshModel(name, originalInputStream, null, null);
    }

    public boolean addOrRefreshModel(String name, final InputStream originalInputStream, @Nullable List<String> errors,
            @Nullable List<String> warnings) {
        logger.info("Loading DSL model '{}'", name);
        Resource resource = null;
        byte[] bytes;
        try (InputStream inputStream = originalInputStream) {
            bytes = inputStream.readAllBytes();
            List<String> newErrors = new ArrayList<>();
            List<String> newWarnings = new ArrayList<>();
            boolean valid = validateModel(name, new ByteArrayInputStream(bytes), newErrors, newWarnings);
            if (errors != null) {
                errors.addAll(newErrors);
            }
            if (warnings != null) {
                warnings.addAll(newWarnings);
            }
            if (!valid) {
                logger.warn("DSL model '{}' has errors, therefore ignoring it: {}", name, String.join("\n", newErrors));
                removeModel(name);
                return false;
            }
            if (!newWarnings.isEmpty()) {
                logger.info("Validation issues found in DSL model '{}', using it anyway:\n{}", name,
                        String.join("\n", newWarnings));
            }
        } catch (IOException e) {
            if (errors != null) {
                errors.add("Model cannot be parsed correctly: %s".formatted(e.getMessage()));
            }
            logger.warn("DSL model '{}' cannot be parsed correctly!", name, e);
            return false;
        }
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
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
                    resource.load(inputStream, resourceOptions);
                    notifyListeners(name, EventType.MODIFIED);
                    return true;
                }
            }
        } catch (IOException e) {
            if (errors != null) {
                errors.add("Model cannot be parsed correctly: %s".formatted(e.getMessage()));
            }
            logger.warn("DSL model '{}' cannot be parsed correctly!", name, e);
            if (resource != null) {
                resourceSet.getResources().remove(resource);
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

            return resourceListCopy.stream()
                    .filter(input -> input.getURI().lastSegment().contains(".") && input.isLoaded()
                            && modelType.equalsIgnoreCase(input.getURI().fileExtension())
                            && !isTemporaryModel(input.getURI().lastSegment()))
                    .map(from -> from.getURI().path()).toList();
        }
    }

    @Override
    public void reloadAllModelsOfType(final String modelType) {
        synchronized (resourceSet) {
            // Make a copy to avoid ConcurrentModificationException
            List<Resource> resourceListCopy = new ArrayList<>(resourceSet.getResources());
            for (Resource resource : resourceListCopy) {
                if (resource.getURI().lastSegment().contains(".") && resource.isLoaded()
                        && modelType.equalsIgnoreCase(resource.getURI().fileExtension())
                        && !isTemporaryModel(resource.getURI().lastSegment())) {
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

    @Override
    public Set<String> removeAllModelsOfType(final String modelType) {
        Set<String> ret = new HashSet<>();
        synchronized (resourceSet) {
            // Make a copy to avoid ConcurrentModificationException
            List<Resource> resourceListCopy = new ArrayList<>(resourceSet.getResources());
            for (Resource resource : resourceListCopy) {
                if (resource.getURI().lastSegment().contains(".") && resource.isLoaded()
                        && modelType.equalsIgnoreCase(resource.getURI().fileExtension())
                        && !isTemporaryModel(resource.getURI().lastSegment())) {
                    logger.debug("Removing resource '{}'", resource.getURI().lastSegment());
                    ret.add(resource.getURI().lastSegment());
                    resourceSet.getResources().remove(resource);
                    notifyListeners(resource.getURI().lastSegment(), EventType.REMOVED);
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

    @Override
    public @Nullable String createTemporaryModel(String modelType, InputStream inputStream, List<String> errors,
            List<String> warnings) {
        String name = "%smodel_%d.%s".formatted(PREFIX_TMP_MODEL, ++counter, modelType);
        return addOrRefreshModel(name, inputStream, errors, warnings) ? name : null;
    }

    private boolean isTemporaryModel(String modelName) {
        return modelName.startsWith(PREFIX_TMP_MODEL);
    }

    @Override
    public void generateSyntaxFromModel(OutputStream out, String modelType, EObject modelContent) {
        synchronized (resourceSet) {
            String name = "%sgenerated_syntax_%d.%s".formatted(PREFIX_TMP_MODEL, ++counter, modelType);
            Resource resource = resourceSet.createResource(URI.createURI(name));
            try {
                resource.getContents().add(modelContent);
                resource.save(out, Map.of(XtextResource.OPTION_ENCODING, StandardCharsets.UTF_8.name()));
            } catch (IOException e) {
                logger.warn("Exception when saving DSL model {}", resource.getURI().lastSegment());
            } finally {
                resourceSet.getResources().remove(resource);
            }
        }
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
     * @param name the model name
     * @param inputStream an input stream with the model's content
     * @param errors the list to be used to fill the errors
     * @param warnings the list to be used to fill the warnings
     * @return false if any syntactical error were found, false otherwise
     * @throws IOException if there was an error with the given {@link InputStream}, loading the resource from there
     */
    private boolean validateModel(String name, InputStream inputStream, List<String> errors, List<String> warnings)
            throws IOException {
        // use another resource for validation in order to keep the original one for emergency-removal in case of errors
        Resource resource = resourceSet.createResource(URI.createURI(PREFIX_TMP_MODEL + name));
        try {
            resource.load(inputStream, resourceOptions);

            if (!resource.getContents().isEmpty()) {
                // Check for syntactical errors
                for (Diagnostic diagnostic : resource.getErrors()) {
                    errors.add(MessageFormat.format("[{0},{1}]: {2}", Integer.toString(diagnostic.getLine()),
                            Integer.toString(diagnostic.getColumn()), diagnostic.getMessage()));
                }
                if (!resource.getErrors().isEmpty()) {
                    return false;
                }

                // Check for validation errors, but log them only
                try {
                    final org.eclipse.emf.common.util.Diagnostic diagnostic = safeEmf
                            .call(() -> Diagnostician.INSTANCE.validate(resource.getContents().getFirst()));
                    for (org.eclipse.emf.common.util.Diagnostic d : diagnostic.getChildren()) {
                        warnings.add(d.getMessage());
                    }
                } catch (NullPointerException e) {
                    // see https://github.com/eclipse/smarthome/issues/3335
                    logger.debug("Validation of '{}' skipped due to internal errors.", name);
                }
            }
        } finally {
            resourceSet.getResources().remove(resource);
        }
        return true;
    }

    private void notifyListeners(String name, EventType type) {
        if (!isTemporaryModel(name)) {
            for (ModelRepositoryChangeListener listener : listeners) {
                listener.modelChanged(name, type);
            }
        }
    }
}
