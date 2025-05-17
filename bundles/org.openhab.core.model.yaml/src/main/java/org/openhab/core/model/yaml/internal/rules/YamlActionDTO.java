package org.openhab.core.model.yaml.internal.rules;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.automation.Action;

public class YamlActionDTO extends YamlModuleDTO { // TODO: (Nad) Header + JavaDocs

    public Map<@NonNull String, @NonNull String> inputs;

    public YamlActionDTO() {
    }

    public YamlActionDTO(@NonNull Action action) {
        super(action);
        this.inputs = action.getInputs();
    }

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
            builder.append("type=").append(type).append(", ");
        }
        if (label != null) {
            builder.append("label=").append(label).append(", ");
        }
        if (description != null) {
            builder.append("description=").append(description).append(", ");
        }
        if (config != null) {
            builder.append("config=").append(config);
        }
        builder.append("]");
        return builder.toString();
    }
}
