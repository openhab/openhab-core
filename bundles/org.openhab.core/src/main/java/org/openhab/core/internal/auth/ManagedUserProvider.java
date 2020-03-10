package org.openhab.core.internal.auth;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.User;
import org.openhab.core.common.registry.DefaultAbstractManagedProvider;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@NonNullByDefault
@Component(service = ManagedUserProvider.class, immediate = true)
public class ManagedUserProvider extends DefaultAbstractManagedProvider<User, String> {

    @Activate
    public ManagedUserProvider(final @Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return "users";
    }

    @Override
    protected @NonNull String keyToString(@NonNull String key) {
        return key;
    }

}
