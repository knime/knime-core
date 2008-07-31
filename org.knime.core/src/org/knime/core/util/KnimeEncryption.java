/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 3, 2006 (sieb): created
 */
package org.knime.core.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
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
     * @param password as char array
     * @return The password encrypt.
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws UnsupportedEncodingException {@link UnsupportedEncodingException}
     */
    public static String encrypt(final char[] password) 
            throws BadPaddingException, IllegalBlockSizeException,
            InvalidKeyException, UnsupportedEncodingException {
        SecretKey secretKey = null;
        if (keySupplier != null) {
            secretKey = createSecretKey(keySupplier.getEncryptionKey());
        }
        return encrypt(secretKey, password);
    }
    
    /**
     * Enrypts password with the given <code>SecrectKey</code>.
     * 
     * @param secretKey <code>SecretKey</code> used to encrypt the password
     * @param password as char array
     * @return The password encrypt.
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws UnsupportedEncodingException {@link UnsupportedEncodingException}
     */
    public static String encrypt(final SecretKey secretKey, 
            final char[] password) throws BadPaddingException,
            IllegalBlockSizeException, InvalidKeyException, 
            UnsupportedEncodingException {
        if (secretKey == null) {
            return new String(password);
        }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] ciphertext = cipher.doFinal(
                new String(password).getBytes("UTF-8"));
        return new BASE64Encoder().encode(ciphertext);
    }

    /**
     * Decrypts password.
     * 
     * @param password The password to decrypt.
     * @return The decrypted password.
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws IOException {@link IOException}
     * @throws UnsupportedEncodingException {@link UnsupportedEncodingException}
     */
    public static String decrypt(final String password)
            throws BadPaddingException, IllegalBlockSizeException,
            InvalidKeyException, IOException, UnsupportedEncodingException {
        SecretKey secretKey = null;
        if (keySupplier != null) {
            secretKey = createSecretKey(keySupplier.getEncryptionKey());
        }
        return decrypt(secretKey, password);
    }

    /**
     * Decrypts password with the given <code>SecrectKey</code>.
     * 
     * @param secretKey <code>SecretKey</code> used to decrypt the password
     * @param password The password to decrypt.
     * @return The decrypted password.
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws IOException {@link IOException}
     * @throws UnsupportedEncodingException {@link UnsupportedEncodingException}
     */
    public static String decrypt(final SecretKey secretKey, 
            final String password) throws BadPaddingException, 
            IllegalBlockSizeException, InvalidKeyException, IOException, 
            UnsupportedEncodingException {
        if (secretKey == null) {
            return password;
        }
        // perform the decryption
        byte[] pw = new BASE64Decoder().decodeBuffer(password);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedText = cipher.doFinal(pw);
        return new String(decryptedText, "UTF-8");
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
     * Generates a <code>SecretKey</code> based on the given key phrase.
     * @param phrase key phrase used to generate secret key
     * @return a new secret key
     */
    public static SecretKey createSecretKey(final String phrase) {
        if (phrase == null || phrase.length() == 0) {
            return null;
        }
        String newKey = phrase;
        if (phrase.length() % 8 != 0) {
            // key is not a multiple of 8
            do {
                // extend key
                newKey += phrase;
            } while (newKey.length() < 8);
            // trim key to multiple of 8
            newKey = newKey.substring(0, ((int) newKey.length() / 8) * 8); 
        }
        try {
            byte[] key = newKey.getBytes();
            return SecretKeyFactory.getInstance("DES").generateSecret(
                            new DESKeySpec(key));
        } catch (Exception e) {
            LOGGER.error(e);
            return null;
        }
    }

}
