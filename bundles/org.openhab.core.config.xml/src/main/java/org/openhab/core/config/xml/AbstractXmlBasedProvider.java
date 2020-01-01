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
package org.openhab.core.config.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.i18n.LocalizedKey;
import org.osgi.framework.Bundle;

/**
 * Common base class for XML based providers.
 *
 * @author Simon Kaufmann - Initial contribution, factored out of subclasses
 *
 * @param <T_ID> the key type, e.g. ThingTypeUID, ChannelUID, URI,...
 * @param <T_OBJECT> the object type, e.g. ThingType, ChannelType, ConfigDescription,...
 */
public abstract class AbstractXmlBasedProvider<T_ID, T_OBJECT extends Identifiable<T_ID>> {

    private final Map<Bundle, List<T_OBJECT>> bundleObjectMap = new ConcurrentHashMap<>();
    private final Map<LocalizedKey, T_OBJECT> localizedObjectCache = new ConcurrentHashMap<>();

    /**
     * Create a translated/localized copy of the given object.
     *
     * @param bundle the module to be used for the look-up of the translations
     * @param object the object to translate
     * @param locale the target locale
     * @return a translated copy of the given object or <code>null</code> if translation was not possible.
     */
    protected abstract @Nullable T_OBJECT localize(Bundle bundle, T_OBJECT object, @Nullable Locale locale);

    /**
     * Adds an object to the internal list associated with the specified module.
     * <p>
     * This method returns silently, if any of the parameters is {@code null}.
     *
     * @param bundle the module to which the object is to be added
     * @param object the object to be added
     */
    public final synchronized void add(Bundle bundle, T_OBJECT object) {
        addAll(bundle, Collections.singletonList(object));
    }

    /**
     * Adds a {@link Collection} of objects to the internal list associated with the specified module.
     * <p>
     * This method returns silently, if any of the parameters is {@code null}.
     *
     * @param bundle the module to which the object is to be added
     * @param objectList the objects to be added
     */
    public final synchronized void addAll(Bundle bundle, Collection<T_OBJECT> objectList) {
        if (objectList == null || objectList.isEmpty()) {
            return;
        }
        List<T_OBJECT> objects = acquireObjects(bundle);
        if (objects == null) {
            return;
        }
        objects.addAll(objectList);
        for (T_OBJECT object : objectList) {
            // just make sure no old entry remains in the cache
            removeCachedEntries(object);
        }
    }

    private List<T_OBJECT> acquireObjects(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        List<T_OBJECT> objects = bundleObjectMap.get(bundle);
        if (objects == null) {
            objects = new CopyOnWriteArrayList<>();
            bundleObjectMap.put(bundle, objects);
        }
        return objects;
    }

    /**
     * Gets the object with the given key.
     *
     * @param key the key which identifies the object
     * @param locale the locale
     * @return the object if found, <code>null</code> otherwise
     */
    protected final @Nullable T_OBJECT get(T_ID key, @Nullable Locale locale) {
        for (Entry<Bundle, List<T_OBJECT>> objects : bundleObjectMap.entrySet()) {
            for (T_OBJECT object : objects.getValue()) {
                if (key.equals(object.getUID())) {
                    return acquireLocalizedObject(objects.getKey(), object, locale);
                }
            }
        }
        return null;
    }

    /**
     * Gets all available objects.
     *
     * @param locale the locale
     * @return a collection containing all available objects. Never <code>null</code>
     */
    protected final synchronized Collection<T_OBJECT> getAll(@Nullable Locale locale) {
        List<T_OBJECT> ret = new LinkedList<>();
        Collection<Entry<Bundle, List<T_OBJECT>>> objectList = bundleObjectMap.entrySet();
        for (Entry<Bundle, List<T_OBJECT>> objects : objectList) {
            for (T_OBJECT object : objects.getValue()) {
                ret.add(acquireLocalizedObject(objects.getKey(), object, locale));
            }
        }
        return ret;
    }

    /**
     * Removes all objects from the internal list associated with the specified module.
     * <p>
     * This method returns silently if the module is {@code null}.
     *
     * @param bundle the module for which all associated Thing types to be removed
     */
    public final synchronized void removeAll(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        List<T_OBJECT> objects = bundleObjectMap.remove(bundle);
        if (objects != null) {
            removeCachedEntries(objects);
        }
    }

    private void removeCachedEntries(List<T_OBJECT> objects) {
        for (T_OBJECT object : objects) {
            removeCachedEntries(object);
        }
    }

    private void removeCachedEntries(T_OBJECT object) {
        for (Iterator<Entry<LocalizedKey, T_OBJECT>> it = localizedObjectCache.entrySet().iterator(); it.hasNext();) {
            Entry<LocalizedKey, T_OBJECT> entry = it.next();
            if (entry.getKey().getKey().equals(object.getUID())) {
                it.remove();
            }
        }
    }

    private T_OBJECT acquireLocalizedObject(Bundle bundle, T_OBJECT object, @Nullable Locale locale) {
        final LocalizedKey localizedKey = getLocalizedKey(object, locale);

        final T_OBJECT cacheEntry = localizedObjectCache.get(localizedKey);
        if (cacheEntry != null) {
            return cacheEntry;
        }

        final T_OBJECT localizedObject = localize(bundle, object, locale);
        if (localizedObject != null) {
            localizedObjectCache.put(localizedKey, localizedObject);
            return localizedObject;
        } else {
            return object;
        }
    }

    private LocalizedKey getLocalizedKey(T_OBJECT object, @Nullable Locale locale) {
        return new LocalizedKey(object.getUID(), locale != null ? locale.toLanguageTag() : null);
    }

}
