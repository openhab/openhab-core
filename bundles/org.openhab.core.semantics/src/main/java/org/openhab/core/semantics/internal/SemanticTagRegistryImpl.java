/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.semantics.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.semantics.ManagedSemanticTagProvider;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagProvider;
import org.openhab.core.semantics.SemanticTagRegistry;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.Tag;
import org.openhab.core.semantics.TagInfo;
import org.openhab.core.semantics.model.DefaultSemanticTagProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main implementing class of the {@link SemanticTagRegistry} interface. It
 * keeps track of all declared semantic tags of all semantic tags providers and keeps
 * their current state in memory.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class SemanticTagRegistryImpl extends AbstractRegistry<SemanticTag, String, SemanticTagProvider>
        implements SemanticTagRegistry {

    private final Logger logger = LoggerFactory.getLogger(SemanticTagRegistryImpl.class);

    private final DefaultSemanticTagProvider defaultSemanticTagProvider;
    private final ManagedSemanticTagProvider managedProvider;

    @Activate
    public SemanticTagRegistryImpl(@Reference DefaultSemanticTagProvider defaultSemanticTagProvider,
            @Reference ManagedSemanticTagProvider managedProvider) {
        super(SemanticTagProvider.class);
        this.defaultSemanticTagProvider = defaultSemanticTagProvider;
        this.managedProvider = managedProvider;
        // Add the default semantic tags provider first, before all others
        super.addProvider(defaultSemanticTagProvider);
        super.addProvider(managedProvider);
        setManagedProvider(managedProvider);
    }

    @Override
    protected void addProvider(Provider<SemanticTag> provider) {
        // Ignore the default semantic tags provider and the managed provider (they are added in the constructor)
        if (!provider.equals(defaultSemanticTagProvider) && !provider.equals(managedProvider)) {
            logger.trace("addProvider {} => calling super.addProvider", provider.getClass().getSimpleName());
            super.addProvider(provider);
        } else {
            logger.trace("addProvider {} => ignoring it", provider.getClass().getSimpleName());
        }
    }

    @Override
    public List<SemanticTag> getSubTree(SemanticTag tag) {
        List<String> ids = getAll().stream().map(t -> t.getUID()).filter(uid -> uid.startsWith(tag.getUID() + "_"))
                .collect(Collectors.toList());
        List<SemanticTag> tags = new ArrayList<>();
        tags.add(tag);
        ids.forEach(id -> {
            SemanticTag t = get(id);
            if (t != null) {
                tags.add(t);
            }
        });
        return tags;
    }

    @Override
    protected void onAddElement(SemanticTag tag) throws IllegalArgumentException {
        logger.trace("onAddElement {}", tag.getUID());
        super.onAddElement(tag);
        String uid = tag.getUID();
        Class<? extends Tag> tagClass = SemanticTags.getById(uid);
        if (tagClass != null) {
            // Class already exists
            return;
        }
        String name;
        Class<? extends Tag> parentTagClass;
        int lastSeparator = uid.lastIndexOf("_");
        if (lastSeparator < 0) {
            name = uid;
            parentTagClass = null;
        } else {
            name = uid.substring(lastSeparator + 1);
            parentTagClass = SemanticTags.getById(uid.substring(0, lastSeparator));
            if (parentTagClass == null) {
                throw new IllegalArgumentException("No existing parent tag with id " + uid.substring(0, lastSeparator));
            }
        }
        if (!name.matches("[A-Z][a-zA-Z0-9]+")) {
            throw new IllegalArgumentException("Invalid tag name " + name);
        }
        tagClass = SemanticTags.getById(name);
        if (tagClass != null) {
            throw new IllegalArgumentException("Tag " + tagClass.getAnnotation(TagInfo.class).id() + " already exist");
        }
        if (SemanticTags.add(name, parentTagClass) == null) {
            throw new IllegalArgumentException("Failed to create semantic tag " + uid);
        }
    }

    @Override
    protected void onRemoveElement(SemanticTag tag) {
        logger.trace("onRemoveElement {}", tag.getUID());
        super.onRemoveElement(tag);
        Class<? extends Tag> tagClass = SemanticTags.getById(tag.getUID());
        if (tagClass != null) {
            SemanticTags.remove(tagClass);
        }
    }
}
