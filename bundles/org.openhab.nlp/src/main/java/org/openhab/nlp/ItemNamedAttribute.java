/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.nlp;

/**
 * This class represents a named attribute, which be used for matching entities
 * extracted from the natural language query to items. They originate from
 * semantic tags and additional monikers defined in the item's "nlp" namespace.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class ItemNamedAttribute {
    public enum AttributeType {
        OBJECT,
        LOCATION
    }

    public enum AttributeSource {
        LABEL,
        CATEGORY,
        TAG,
        METADATA
    }

    public ItemNamedAttribute(AttributeType type, String value, boolean inherited, AttributeSource source) {
        super();
        this.type = type;
        this.value = value;
        this.inherited = inherited;
        this.source = source;
    }

    public ItemNamedAttribute(String type, String value, boolean inherited, AttributeSource source) {
        super();
        this.type = (type == "location") ? AttributeType.LOCATION : AttributeType.OBJECT;
        this.value = value;
        this.inherited = inherited;
        this.source = source;
    }

    public ItemNamedAttribute(String type, String value, AttributeSource source) {
        super();
        this.type = (type == "location") ? AttributeType.LOCATION : AttributeType.OBJECT;
        this.value = value;
        this.inherited = false;
        this.source = source;
    }

    AttributeType type;
    String value;
    boolean inherited;
    AttributeSource source;

    /**
     * Retrieves the type of the named attribute - "object" or "location"
     *
     * @return the type - "object" or "location"
     */
    public AttributeType getType() {
        return type;
    }

    /**
     * Sets the type of the named attribute
     *
     * @param type the type
     */
    public void setType(AttributeType type) {
        this.type = type;
    }

    /**
     * Retrieves the value of the named attribute
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the named attribute
     *
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns whether this named attribute is inherited or not
     *
     * @return whether the named attribute is inherited
     */
    public boolean isInherited() {
        return inherited;
    }

    /**
     * Specifies whether this named attribute is inherited
     *
     * @param inherited whether the named attribute is inherited
     */
    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    /**
     * Returns whether the named attribute comes from a tag or metadata
     *
     * @return the source of the named attribute
     */
    public AttributeSource getSource() {
        return source;
    }

    /**
     * Specifies whether the named attribute comes from a tag or metadata
     *
     * @param source the source the named attribute
     */
    public void setSource(AttributeSource source) {
        this.source = source;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (inherited ? 1231 : 1237);
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        ItemNamedAttribute other = (ItemNamedAttribute) obj;
        // doesn't matter
        // if (inherited != other.inherited) {
        // return false;
        // }
        // if (source != other.source) {
        // return false;
        // }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

}
