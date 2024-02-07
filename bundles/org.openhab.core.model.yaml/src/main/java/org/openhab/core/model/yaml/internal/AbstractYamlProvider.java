/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.model.yaml.YamlModelListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractYamlProvider} is a base class for the implementation of element providers from YAML files
 *
 * @author Jan N. Klug - Initial contribution
 *
 * @param <E> Type of the provided elements, MUST implement {@link Identifiable}
 * @param <K> Type of the key of the provided elements
 * @param <D> Type of the YAML representation, MUST extend {@link YamlElement}
 *
 */
@NonNullByDefault
public abstract class AbstractYamlProvider<@NonNull E extends Identifiable<K>, @NonNull K, D extends YamlElement>
        extends AbstractProvider<E> implements YamlModelListener<D> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<K, ModelElement<E>> internalMap = new ConcurrentHashMap<>();

    private final Class<D> dtoClass;
    private final String elementName;

    @Activate
    public AbstractYamlProvider(Class<D> dtoClass) {
        this.dtoClass = dtoClass;
        YamlElementName annotation = dtoClass.getAnnotation(YamlElementName.class);
        if (annotation == null) {
            logger.warn("Class '{}' is missing the mandatory YamlElementName annotation. This is a bug.", dtoClass);
            elementName = "";
        } else {
            elementName = annotation.value();
        }
    }

    @Deactivate
    public void deactivate() {
        internalMap.clear();
    }

    @Override
    public Collection<E> getAll() {
        return internalMap.values().stream().map(ModelElement::element).toList();
    }

    @Override
    public Class<D> getElementClass() {
        return dtoClass;
    }

    @Override
    public void addedModel(String modelName, Collection<D> elements) {
        mapCollection(elements).forEach(addedElement -> {
            ModelElement<E> oldElement = internalMap.put(addedElement.getUID(),
                    new ModelElement<>(modelName, addedElement));
            if (oldElement == null) {
                logger.debug("'{}' element '{}' from model '{}' added.", elementName, addedElement.getUID(), modelName);
                notifyListenersAboutAddedElement(addedElement);
            } else if (oldElement.modelName().equals(modelName)) {
                logger.debug("'{}' element '{}' from model '{}' already present, considering it as updated.",
                        elementName, addedElement.getUID(), modelName);
                notifyListenersAboutUpdatedElement(oldElement.element(), addedElement);
            } else {
                logger.warn(
                        "'{}' element '{}' from model '{}' already present from other model '{}', considering it as updated. Check your configuration.",
                        elementName, addedElement.getUID(), modelName, oldElement.modelName());
                notifyListenersAboutUpdatedElement(oldElement.element(), addedElement);
            }
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<D> elements) {
        mapCollection(elements).forEach(updatedElement -> {
            ModelElement<E> oldElement = internalMap.put(updatedElement.getUID(),
                    new ModelElement<>(modelName, updatedElement));
            if (oldElement == null) {
                logger.debug("'{}' element '{}' from model '{}' not found, considering it as newly added.", elementName,
                        updatedElement.getUID(), modelName);
                notifyListenersAboutAddedElement(updatedElement);
            } else if (oldElement.modelName().equals(modelName)) {
                logger.debug("{}' element '{}' from model '{}' updated", elementName, updatedElement.getUID(),
                        modelName);
                notifyListenersAboutUpdatedElement(oldElement.element(), updatedElement);
            } else {
                logger.warn(
                        "'{}' element '{}' from model '{}' updated, but was previously added from other model '{}'. Check your configuration.",
                        elementName, updatedElement.getUID(), modelName, oldElement.modelName());
            }
        });
    }

    @Override
    public void removedModel(String modelName, Collection<D> elements) {
        mapCollection(elements).forEach(removedElement -> {
            ModelElement<E> oldElement = internalMap.remove(removedElement.getUID());
            if (oldElement == null) {
                logger.debug("'{}' element '{}' from model '{}' not found, considering it as missing.", elementName,
                        removedElement.getUID(), modelName);
            } else if (!oldElement.modelName().equals(modelName)) {
                logger.warn(
                        "'{}' element '{}' from model '{}' removed, but was originally added from other model '{}'. Check your configuration.",
                        elementName, removedElement.getUID(), modelName, oldElement.modelName());
                notifyListenersAboutRemovedElement(removedElement);
            } else {
                logger.debug("'{}' element '{}' from model '{}' removed.", elementName, removedElement.getUID(),
                        modelName);
                notifyListenersAboutRemovedElement(removedElement);
            }
        });
    }

    /**
     * Map the YAML representation to the provided element
     *
     * @param yamlElement The YAML element that should be mapped
     * @return The mapped element (or {@code null} when conversion fails)
     *
     */
    protected abstract @Nullable E map(D yamlElement);

    private Stream<E> mapCollection(Collection<D> yamlDTOs) {
        return yamlDTOs.stream().map(this::map).filter(Objects::nonNull).map(Objects::requireNonNull);
    }

    private record ModelElement<E> (String modelName, E element) {
    }
}
