/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.internal.items;

import static org.openhab.core.internal.items.MetadataCommandDescriptionProvider.parseValueLabelPair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateDescriptionFragmentProvider;
import org.openhab.core.types.StateOption;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link StateDescriptionFragment} provider from items' metadata
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
@Component(service = StateDescriptionFragmentProvider.class)
public class MetadataStateDescriptionFragmentProvider implements StateDescriptionFragmentProvider {

    private final Logger logger = LoggerFactory.getLogger(MetadataStateDescriptionFragmentProvider.class);

    public static final String STATEDESCRIPTION_METADATA_NAMESPACE = "stateDescription";

    private final MetadataRegistry metadataRegistry;

    private final Integer rank;

    @Activate
    public MetadataStateDescriptionFragmentProvider(final @Reference MetadataRegistry metadataRegistry,
            Map<String, Object> properties) {
        this.metadataRegistry = metadataRegistry;

        Object serviceRanking = properties.get(Constants.SERVICE_RANKING);
        if (serviceRanking instanceof Integer) {
            rank = (Integer) serviceRanking;
        } else {
            rank = 1; // takes precedence over other providers usually ranked 0
        }
    }

    @Override
    public @Nullable StateDescriptionFragment getStateDescriptionFragment(String itemName, @Nullable Locale locale) {
        Metadata metadata = metadataRegistry.get(new MetadataKey(STATEDESCRIPTION_METADATA_NAMESPACE, itemName));

        if (metadata != null) {
            try {
                StateDescriptionFragmentBuilder builder = StateDescriptionFragmentBuilder.create();

                Object pattern = metadata.getConfiguration().get("pattern");
                if (pattern != null) {
                    builder.withPattern((String) pattern);
                }

                Object min = metadata.getConfiguration().get("min");
                if (min != null) {
                    builder.withMinimum(getBigDecimal(min));
                }

                Object max = metadata.getConfiguration().get("max");
                if (max != null) {
                    builder.withMaximum(getBigDecimal(max));
                }

                Object step = metadata.getConfiguration().get("step");
                if (step != null) {
                    builder.withStep(getBigDecimal(step));
                }

                Object readOnly = metadata.getConfiguration().get("readOnly");
                if (readOnly != null) {
                    builder.withReadOnly(getBoolean(readOnly));
                }

                if (metadata.getConfiguration().containsKey("options")) {
                    List<StateOption> stateOptions = Stream
                            .of(metadata.getConfiguration().get("options").toString().split(",")).map(o -> {
                                if (o.contains("=")) {
                                    var pair = parseValueLabelPair(o.trim());
                                    return new StateOption(pair[0], pair[1]);
                                } else {
                                    return new StateOption(o.trim(), null);
                                }
                            }).collect(Collectors.toList());
                    builder.withOptions(stateOptions);
                }

                return builder.build();
            } catch (Exception e) {
                logger.warn("Unable to parse the stateDescription from metadata for item {}, ignoring it", itemName);
            }
        }

        return null;
    }

    private BigDecimal getBigDecimal(Object value) {
        BigDecimal ret = null;
        if (value != null) {
            if (value instanceof BigDecimal decimal) {
                ret = decimal;
            } else if (value instanceof String string) {
                ret = new BigDecimal(string);
            } else if (value instanceof BigInteger integer) {
                ret = new BigDecimal(integer);
            } else if (value instanceof Number number) {
                ret = new BigDecimal(number.doubleValue());
            } else {
                throw new ClassCastException("Not possible to coerce [" + value + "] from class " + value.getClass()
                        + " into a BigDecimal.");
            }
        }
        return ret;
    }

    private Boolean getBoolean(Object value) {
        Boolean ret = null;
        if (value != null) {
            if (value instanceof Boolean boolean1) {
                ret = boolean1;
            } else if (value instanceof String string) {
                ret = Boolean.parseBoolean(string);
            } else {
                throw new ClassCastException(
                        "Not possible to coerce [" + value + "] from class " + value.getClass() + " into a Boolean.");
            }
        }
        return ret;
    }

    @Override
    public Integer getRank() {
        return rank;
    }
}
