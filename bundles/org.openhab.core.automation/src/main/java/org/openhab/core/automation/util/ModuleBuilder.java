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
package org.openhab.core.automation.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Trigger;
import org.openhab.core.config.core.Configuration;

/**
 * This class allows the easy construction of a {@link Module} instance using the builder pattern.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Split implementation for different module types in sub classes
 */
@NonNullByDefault
public abstract class ModuleBuilder<B extends ModuleBuilder<B, T>, T extends Module> {

    public static ActionBuilder createAction() {
        return ActionBuilder.create();
    }

    public static ActionBuilder createAction(final Action action) {
        return ActionBuilder.create(action);
    }

    public static ConditionBuilder createCondition() {
        return ConditionBuilder.create();
    }

    public static ConditionBuilder createCondition(final Condition condition) {
        return ConditionBuilder.create(condition);
    }

    public static TriggerBuilder createTrigger() {
        return TriggerBuilder.create();
    }

    public static TriggerBuilder createTrigger(final Trigger trigger) {
        return TriggerBuilder.create(trigger);
    }

    @SuppressWarnings("unchecked")
    public static <B extends ModuleBuilder<B, T>, T extends Module> ModuleBuilder<B, T> create(Module module) {
        if (module instanceof Action) {
            return (ModuleBuilder<B, T>) createAction((Action) module);
        } else if (module instanceof Condition) {
            return (ModuleBuilder<B, T>) createCondition((Condition) module);
        } else if (module instanceof Trigger) {
            return (ModuleBuilder<B, T>) createTrigger((Trigger) module);
        } else {
            throw new IllegalArgumentException("Parameter must be an instance of Action, Condition or Trigger.");
        }
    }

    private @Nullable String id;
    private @Nullable String typeUID;
    protected @Nullable Configuration configuration;
    protected @Nullable String label;
    protected @Nullable String description;

    protected ModuleBuilder() {
    }

    protected ModuleBuilder(T module) {
        this.id = module.getId();
        this.typeUID = module.getTypeUID();
        this.configuration = new Configuration(module.getConfiguration());
        this.label = module.getLabel();
        this.description = module.getDescription();
    }

    public B withId(String id) {
        this.id = id;
        return myself();
    }

    public B withTypeUID(String typeUID) {
        this.typeUID = typeUID;
        return myself();
    }

    public B withConfiguration(Configuration configuration) {
        this.configuration = new Configuration(configuration);
        return myself();
    }

    public B withLabel(@Nullable String label) {
        this.label = label;
        return myself();
    }

    public B withDescription(@Nullable String description) {
        this.description = description;
        return myself();
    }

    @SuppressWarnings("unchecked")
    private B myself() {
        return (B) this;
    }

    protected String getId() {
        final String id = this.id;
        if (id == null) {
            throw new IllegalStateException("The ID must not be null.");
        }
        return id;
    }

    protected String getTypeUID() {
        final String typeUID = this.typeUID;
        if (typeUID == null) {
            throw new IllegalStateException("The type UID must not be null.");
        }
        return typeUID;
    }

    public abstract T build();
}
