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
package org.eclipse.smarthome.config.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.core.common.registry.Identifiable;
import org.osgi.framework.Bundle;

/**
 * Common base class for XML based providers.
 *
 * @author Simon Kaufmann - initial contribution, factored out of subclasses
 *
 * @param <T_ID> the key type, e.g. ThingTypeUID, ChannelUID, URI,...
 * @param <T_OBJECT> the object type, e.g. ThingType, ChannelType, ConfigDescription,...
 */
public abstract class AbstractXmlBasedProvider<T_ID, T_OBJECT extends Identifiable<T_ID>> {

    private static class LocalizedKey {
        public final Object id;
        public final String locale;

        public LocalizedKey(Object id, String locale) {
            this.id = id;
            this.locale = locale;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((locale == null) ? 0 : locale.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            LocalizedKey other = (LocalizedKey) obj;
            if (!Objects.equals(id, other.id)) {
                return false;
            }
            if (!Objects.equals(locale, other.locale)) {
                return false;
            }
            return true;
        }

    }

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
    protected abstract T_OBJECT localize(Bundle bundle, T_OBJECT object, Locale locale);

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
            objects = new CopyOnWriteArrayList<T_OBJECT>();
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
    protected final T_OBJECT get(T_ID key, Locale locale) {
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
    protected final synchronized Collection<T_OBJECT> getAll(Locale locale) {
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
            if (entry.getKey().id.equals(object.getUID())) {
                it.remove();
            }
        }
    }

    private T_OBJECT acquireLocalizedObject(Bundle bundle, T_OBJECT object, Locale locale) {
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

    private LocalizedKey getLocalizedKey(T_OBJECT object, Locale locale) {
        return new LocalizedKey(object.getUID(), locale != null ? locale.toLanguageTag() : null);
    }

}
