package org.openhab.core.model.yaml.internal.rules;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

public class YamlModuleDTO { //TODO: (Nad) Header + JavaDocs

    public String id;
    public String type;
    public String label;
    public String description;
    public Map<@NonNull String, @NonNull Object> configuration;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append(" [");
        if (id != null) {
            builder.append("id=").append(id).append(", ");
        }
        if (type != null) {
            builder.append("type=").append(type).append(", ");
        }
        if (label != null) {
            builder.append("label=").append(label).append(", ");
        }
        if (description != null) {
            builder.append("description=").append(description).append(", ");
        }
        if (configuration != null) {
            builder.append("configuration=").append(configuration);
        }
        builder.append("]");
        return builder.toString();
    }
}
