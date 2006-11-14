/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 3, 2006 (sieb): created
 */
package org.knime.core.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.knime.core.node.NodeLogger;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * This class handles the encryption and decryption with the static stored key.
 * To this class one static key supplier can be registered that is invoked if no
 * key is available.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public final class KnimeEncryption {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(KnimeEncryption.class);

    private static final String ENCRYPTION_METHOD = "DES/ECB/PKCS5Padding";

    private static Cipher cipher;

    private static EncryptionKeySupplier keySupplier;

    private static SecretKey secretKey;

    static {

        try {
            cipher = Cipher.getInstance(ENCRYPTION_METHOD);
        } catch (Exception e) {
            LOGGER.error("Cipher <" + ENCRYPTION_METHOD
                    + "> could not be created.", e);
            cipher = null;
        }
    }

    private KnimeEncryption() {
        // empty private default constructor as this is a static utility class
    }

    /**
     * Enrypts password.
     * 
     * @param password Char array.
     * @return The password encrypt.
     * @throws Exception If something goes wrong.
     */
    public static String encrypt(final char[] password) throws Exception {
        if (secretKey == null && keySupplier != null) {
            secretKey = createSecretKey(keySupplier.getEncryptionKey());
        }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] ciphertext = cipher.doFinal(new String(password).getBytes());
        return new BASE64Encoder().encode(ciphertext);
    }

    /**
     * Decrypts password.
     * 
     * @param password The password to decrypt.
     * @return The decrypted password.
     * @throws Exception If something goes wrong.
     */
    public static String decrypt(final String password) throws Exception {
        if (secretKey == null && keySupplier != null) {
            secretKey = createSecretKey(keySupplier.getEncryptionKey());
        }
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        // perform the decryption
        byte[] pw = new BASE64Decoder().decodeBuffer(password);
        byte[] decryptedText = cipher.doFinal(pw);
        return new String(decryptedText);
    }

    /**
     * Sets the static encryption key supplier for this global static knime
     * encryptor.
     * 
     * @param supplier the {@link EncryptionKeySupplier} that is asked for a key
     *            if has not been set so far
     */
    public static void setEncryptionKeySupplier(
            final EncryptionKeySupplier supplier) {
        keySupplier = supplier;
    }

    /**
     * Directly sets an encryption key.
     * 
     * @param key the encryption key to set
     */
    public static void setEncryptionKey(final SecretKey key) {
        secretKey = key;
    }

    private static SecretKey createSecretKey(final String keyAsString) {

        SecretKey secretKey1 = null;
        try {
            if (keyAsString.length() < 8) {
                throw new IllegalArgumentException(
                        "The encryption key must be at least 8 "
                                + "characters long.");
            }
            byte[] key = keyAsString.getBytes();

            secretKey1 =
                    SecretKeyFactory.getInstance("DES").generateSecret(
                            new DESKeySpec(key));

        } catch (Exception e) {
            secretKey1 = null;
        }

        return secretKey1;
    }
}
