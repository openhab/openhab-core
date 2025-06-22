package org.openhab.core.automation.module.script.providersupport.internal;

public interface ProviderRegistryDelegate {

    /**
     * Removes all elements that are provided by the script the {@link ProviderRegistryDelegate} instance is bound to.
     * To be called when the script is unloaded or reloaded.
     */
    void removeAllAddedByScript();
}
