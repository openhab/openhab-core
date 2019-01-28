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
package org.eclipse.smarthome.core.auth.client.oauth2;

import java.security.GeneralSecurityException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This allows the encryption and decryption to be performed before saving to storage.
 *
 * @author Gary Tse - initial contribution
 *
 */
@NonNullByDefault
public interface StorageCipher {

    /**
     * A unique cipher identifier per each implementation of StorageCipher.
     * It allows the OAuthStoreHandler to choose which cipher implementation to use.
     * This is particularly important when old ciphers becomes out-dated and
     * need to be replaced by new implementations.
     *
     * @return unique identifier
     */
    String getUniqueCipherId();

    /**
     * Encrypt the plainText, then produce a base64 encoded cipher text
     *
     * @param plainText
     * @return base64-encoded( encrypted( text ) )
     * @throws GeneralSecurityException all security-related exception
     */
    @Nullable
    String encrypt(@Nullable String plainText) throws GeneralSecurityException;

    /**
     * Decrypt the base64 encoded cipher text.
     *
     * @param base64CipherText This should be the result from the {@link #encrypt(String)}
     * @return plain text
     * @throws GeneralSecurityException all security-related exception
     */
    @Nullable
    String decrypt(@Nullable String base64CipherText) throws GeneralSecurityException;

}
