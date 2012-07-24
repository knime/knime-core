/* @(#)$RCSfile$
 * $Revision$ $Date$ $Author$
 *
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
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
        LOGGER.debug("Replacing current encryption key supplier \""
                + keySupplier + "\" with this new one \"" + supplier + "\".");
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
            newKey = newKey.substring(0, (newKey.length() / 8) * 8);
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
