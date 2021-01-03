package org.openhab.core.storage.json;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The MigrationHandler can be used to convert old legacy classes to new classes.
 *
 * @author Simon Lamon - Initial contribution
 */
@NonNullByDefault
public abstract class StorageMigration {
    private @Nullable String oldEntityClassName;
    private Class<?> oldEntityClass;
    private @Nullable String newEntityClassName;
    private Class<?> newEntityClass;

    public StorageMigration(Class<?> oldEntityClass, Class<?> newEntityClass) {
        super();
        this.oldEntityClass = oldEntityClass;
        this.newEntityClass = newEntityClass;
    }

    public StorageMigration(String oldEntityClassName, Class<?> oldEntityClass, String newEntityClassName,
            Class<?> newEntityClass) {
        super();
        this.oldEntityClassName = oldEntityClassName;
        this.oldEntityClass = oldEntityClass;
        this.newEntityClassName = newEntityClassName;
        this.newEntityClass = newEntityClass;
    }

    public @Nullable String getOldEntityClassName() {
        return oldEntityClassName;
    }

    public @Nullable String getNewEntityClassName() {
        return newEntityClassName;
    }

    public Class<?> getOldEntityClass() {
        return oldEntityClass;
    }

    public Class<?> getNewEntityClass() {
        return newEntityClass;
    }

    public abstract Object migrate(Object in);
}
