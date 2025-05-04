package org.openhab.core.model.yaml.internal.rules;

import java.util.Map;

public class YamlConditionDTO extends YamlModuleDTO {

    public Map<String, String> inputs;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append(" [");
        if (inputs != null) {
            builder.append("inputs=").append(inputs).append(", ");
        }
        if (id != null) {
            builder.append("id=").append(id).append(", ");
        }
        if (type != null) {
            builder.append("typeUID=").append(type).append(", ");
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
