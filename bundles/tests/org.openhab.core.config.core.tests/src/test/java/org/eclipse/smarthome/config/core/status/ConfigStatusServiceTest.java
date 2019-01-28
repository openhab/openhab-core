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
package org.eclipse.smarthome.config.core.status;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.eclipse.smarthome.config.core.status.ConfigStatusMessage.Type;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.util.BundleResolver;
import org.eclipse.smarthome.test.java.JavaTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing the {@link ConfigStatusService} OSGi service.
 *
 * @author Thomas Höfer - Initial contribution
 */
public class ConfigStatusServiceTest extends JavaTest {

    private ConfigStatusService configStatusService;

    private static final String ENTITY_ID1 = "entity1";
    private static final String ENTITY_ID2 = "entity2";

    private static final Locale LOCALE_DE = new Locale("de");
    private static final Locale LOCALE_EN = new Locale("en");

    private static final String PARAM1 = "param1";
    private static final String PARAM2 = "param2";
    private static final String PARAM3 = "param3";

    private static final String MSG_KEY1 = "msg1";
    private static final String MSG_KEY2 = "msg2";
    private static final String MSG_KEY3 = "msg3";

    private static final String MSG1_DE = "German 1";
    private static final String MSG2_DE = "German 2 - {0}";
    private static final String MSG3_DE = "German 3 - {0}";
    private static final String MSG1_EN = "English 1";
    private static final String MSG2_EN = "English 2 - {0}";
    private static final String MSG3_EN = "English 3 - {0}";

    private static final String ARGS = "args";

    private final ConfigStatusMessage PARAM1_MSG1 = ConfigStatusMessage.Builder.information(PARAM1)
            .withMessageKeySuffix(MSG_KEY1).build();
    private final ConfigStatusMessage PARAM2_MSG2 = ConfigStatusMessage.Builder.warning(PARAM2)
            .withMessageKeySuffix(MSG_KEY2).withStatusCode(1).withArguments(ARGS).build();
    private final ConfigStatusMessage PARAM3_MSG3 = ConfigStatusMessage.Builder.error(PARAM3)
            .withMessageKeySuffix(MSG_KEY3).withStatusCode(2).withArguments(ARGS).build();

    private final Collection<ConfigStatusMessage> messagesEntity1 = new ArrayList<>();
    private final Collection<ConfigStatusMessage> messagesEntity2 = new ArrayList<>();

    private final Collection<ConfigStatusMessage> messagesEntity1De = new ArrayList<>();
    private final Collection<ConfigStatusMessage> messagesEntity2De = new ArrayList<>();

    private final Collection<ConfigStatusMessage> messagesEntity1En = new ArrayList<>();
    private final Collection<ConfigStatusMessage> messagesEntity2En = new ArrayList<>();

    @Before
    public void setUp() {
        messagesEntity1.add(PARAM1_MSG1);
        messagesEntity1.add(PARAM2_MSG2);
        messagesEntity1De.add(
                buildConfigStatusMessage(PARAM1_MSG1.parameterName, PARAM1_MSG1.type, MSG1_DE, PARAM1_MSG1.statusCode));
        messagesEntity1De.add(buildConfigStatusMessage(PARAM2_MSG2.parameterName, PARAM2_MSG2.type,
                MessageFormat.format(MSG2_DE, ARGS), PARAM2_MSG2.statusCode));
        messagesEntity1En.add(
                buildConfigStatusMessage(PARAM1_MSG1.parameterName, PARAM1_MSG1.type, MSG1_EN, PARAM1_MSG1.statusCode));
        messagesEntity1En.add(buildConfigStatusMessage(PARAM2_MSG2.parameterName, PARAM2_MSG2.type,
                MessageFormat.format(MSG2_EN, ARGS), PARAM2_MSG2.statusCode));

        messagesEntity2.add(PARAM2_MSG2);
        messagesEntity2.add(PARAM3_MSG3);
        messagesEntity2De.add(buildConfigStatusMessage(PARAM2_MSG2.parameterName, PARAM2_MSG2.type,
                MessageFormat.format(MSG2_DE, ARGS), PARAM2_MSG2.statusCode));
        messagesEntity2De.add(buildConfigStatusMessage(PARAM3_MSG3.parameterName, PARAM3_MSG3.type,
                MessageFormat.format(MSG3_DE, ARGS), PARAM3_MSG3.statusCode));
        messagesEntity2En.add(buildConfigStatusMessage(PARAM2_MSG2.parameterName, PARAM2_MSG2.type,
                MessageFormat.format(MSG2_EN, ARGS), PARAM2_MSG2.statusCode));
        messagesEntity2En.add(buildConfigStatusMessage(PARAM3_MSG3.parameterName, PARAM3_MSG3.type,
                MessageFormat.format(MSG3_EN, ARGS), PARAM3_MSG3.statusCode));

        configStatusService = new ConfigStatusService();

        LocaleProvider localeProvider = mock(LocaleProvider.class);
        when(localeProvider.getLocale()).thenReturn(new Locale("en", "US"));
        configStatusService.setLocaleProvider(localeProvider);

        configStatusService.setEventPublisher(mock(EventPublisher.class));
        configStatusService.setBundleResolver(mock(BundleResolver.class));
        configStatusService.setTranslationProvider(getTranslationProvider());
        configStatusService.addConfigStatusProvider(getConfigStatusProviderMock(ENTITY_ID1));
        configStatusService.addConfigStatusProvider(getConfigStatusProviderMock(ENTITY_ID2));
    }

    private ConfigStatusMessage buildConfigStatusMessage(String parameterName, Type type, String msg,
            Integer statusCode) {
        return new ConfigStatusMessage(parameterName, type, msg, statusCode);
    }

    @Test
    public void assertThatNullIsReturnedForEntityWithoutRegisteredValidator() {
        ConfigStatusInfo info = configStatusService.getConfigStatus("unknown", null);
        assertThat("Config status info is null.", info, is((ConfigStatusInfo) null));
    }

    @Test
    public void assertThatConfigStatusMessagesAreReturnedForEntity() {
        test(ENTITY_ID1, LOCALE_DE, messagesEntity1De);
        test(ENTITY_ID2, LOCALE_DE, messagesEntity2De);

        test(ENTITY_ID1, LOCALE_EN, messagesEntity1En);
        test(ENTITY_ID2, LOCALE_EN, messagesEntity2En);
    }

    private void test(String entityId, Locale locale, Collection<ConfigStatusMessage> expectedMessages) {
        ConfigStatusInfo info = configStatusService.getConfigStatus(entityId, locale);
        assertConfigStatusInfo(info, new ConfigStatusInfo(expectedMessages));
    }

    private void assertConfigStatusInfo(ConfigStatusInfo actual, ConfigStatusInfo expected) {
        assertThat(actual, equalTo(expected));
    }

    private ConfigStatusProvider getConfigStatusProviderMock(final String entityId) {
        ConfigStatusProvider configStatusProvider = mock(ConfigStatusProvider.class);
        when(configStatusProvider.supportsEntity(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0).equals(entityId));
        when(configStatusProvider.getConfigStatus())
                .thenAnswer(invocation -> entityId.equals(ENTITY_ID1) ? messagesEntity1 : messagesEntity2);

        return configStatusProvider;
    }

    private TranslationProvider getTranslationProvider() {
        TranslationProvider translationProvider = mock(TranslationProvider.class);
        when(translationProvider.getText(any(), anyString(), eq(null), any(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(1);
            Locale locale = invocation.getArgument(3);
            if (locale.equals(LOCALE_DE)) {
                if (key.endsWith(MSG_KEY1)) {
                    return MSG1_DE;
                } else if (key.endsWith(MSG_KEY2)) {
                    return MessageFormat.format(MSG2_DE, ARGS);
                } else if (key.endsWith(MSG_KEY3)) {
                    return MessageFormat.format(MSG3_DE, ARGS);
                }
            } else {
                if (key.endsWith(MSG_KEY1)) {
                    return MSG1_EN;
                } else if (key.endsWith(MSG_KEY2)) {
                    return MessageFormat.format(MSG2_EN, ARGS);
                } else if (key.endsWith(MSG_KEY3)) {
                    return MessageFormat.format(MSG3_EN, ARGS);
                }
            }

            return null;
        });

        return translationProvider;
    }
}
