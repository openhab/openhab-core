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
package org.openhab.core.auth.oauth2client.test.internal.cipher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.auth.oauth2client.internal.cipher.SymmetricKeyCipher;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Verify that the SymmetricKeyCipher is working properly.
 *
 * @author Gary Tse - Initial contribution
 */
@NonNullByDefault
public class CipherTest {

    private static final String PLAIN_TEXT = "hello world";

    private @NonNullByDefault({}) SymmetricKeyCipher symmetricKeyCipher;

    @BeforeEach
    public void setUp() throws IOException, InvalidSyntaxException, NoSuchAlgorithmException {
        symmetricKeyCipher = new SymmetricKeyCipher(mockConfigurationAdmin());
    }

    @Test
    public void testEncDec() throws GeneralSecurityException {
        String cipherText = symmetricKeyCipher.encrypt(PLAIN_TEXT);
        assertNotNull(cipherText, "Cipher text should not be null");
        assertNotEquals(PLAIN_TEXT, cipherText, "Cipher text should not be the same as plaintext");

        String decryptedText = symmetricKeyCipher.decrypt(cipherText);
        assertNotNull(decryptedText, "Decrypted text should not be null");
        assertEquals(PLAIN_TEXT, decryptedText, "Decrypted text should be same as before");
    }

    private ConfigurationAdmin mockConfigurationAdmin() throws IOException {
        ConfigurationAdmin configurationAdmin = mock(ConfigurationAdmin.class);
        Configuration configuration = mockConfiguration();
        when(configurationAdmin.getConfiguration(anyString())).thenReturn(configuration);
        return configurationAdmin;
    }

    private Configuration mockConfiguration() {
        Dictionary<String, Object> properties = new Hashtable<>();
        Configuration configurationMock = mock(Configuration.class);
        when(configurationMock.getPid()).thenReturn("PID");
        when(configurationMock.getProperties()).thenReturn(properties);
        return configurationMock;
    }
}
