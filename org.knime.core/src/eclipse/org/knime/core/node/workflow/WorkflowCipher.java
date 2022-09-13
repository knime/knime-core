/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 11, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.WorkflowCipherPrompt.PromptCancelled;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.crypto.HexUtils;
import org.knime.shared.workflow.def.CipherDef;
import org.knime.shared.workflow.def.impl.CipherDefBuilder;

/** A cipher object associated with a metanode or workflow. Most workflows
 * have the {@link #NULL_CIPHER} assigned, i.e. no locking/encryption.
 *
 * <p>
 * If a metanode is locked, then the content of the metanode can only be
 * accessed in the UI after the users enters the correct password. It does not
 * cover the usage (executing a metanode without entering the password is still
 * possible).
 *
 * <p>
 * The encryption in this class refers to workflow saving and loading. The
 * encryption key is part of the node configuration (it's written to the
 * workflow.knime file). The very only reason for this is to make it harder
 * (but not impossible) for the user to inspect the workflow internals by going
 * into the saved workflow and inspect the settings.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class WorkflowCipher implements Cloneable {

    /**
     * Version of the locking mechanism, used to prevent older AP versions from
     * opening newly locked components (potentially circumventing fixes).
     * Versions are listed in reverse order so that {@code CipherVersion.values()[0]}
     * returns the current version.
     */
    private enum CipherVersion {
        /** Version 2 is used in KNIME AP 4.6.2--present, see AP-19479. */
        VERSION_2("version_2") {
            @Override
            final Optional<byte[]> extractKey(final byte[] prefix, final byte[] versionedKey) {
                final var extracted = stripPrefix(prefix, versionedKey);
                extracted.ifPresent(ArrayUtils::reverse);
                return extracted;
            }

            @Override
            byte[] packageKey(final byte[] prefix, final byte[] rawKey) {
                final byte[] reversed = rawKey.clone();
                ArrayUtils.reverse(reversed);
                return ArrayUtils.addAll(prefix, reversed);
            }
        },

        /** Version 1 is used in KNIME AP 4.4--4.6.1, see AP-16296. */
        VERSION_1("version_1") {
            @Override
            Optional<byte[]> extractKey(final byte[] prefix, final byte[] versionedKey) {
                return stripPrefix(prefix, versionedKey);
            }

            @Override
            byte[] packageKey(final byte[] prefix, final byte[] rawKey) {
                return ArrayUtils.addAll(prefix, rawKey);
            }
        },

        /** Version 0 is used in KNIME APs <4.4. */
        VERSION_0("") {
            @Override
            Optional<byte[]> extractKey(final byte[] prefix, final byte[] versionedKey) {
                return Optional.of(versionedKey);
            }

            @Override
            byte[] packageKey(final byte[] prefix, final byte[] rawKey) {
                return rawKey;
            }
        };

        /** Prefix for the key. */
        private final byte[] m_prefix;

        /**
         * Tries to strip a given prefix from the given byte array.
         *
         * @param prefix prefix to strip
         * @param array array to strip the prefix from
         * @return remainder of the array after the prefix if found, {@link Optional#empty()} otherwise
         */
        private static Optional<byte[]> stripPrefix(final byte[] prefix, final byte[] array) {
            return Arrays.mismatch(prefix, array) == prefix.length
                    ? Optional.of(Arrays.copyOfRange(array, prefix.length, array.length)) : Optional.empty();
        }

        /**
         * @param prefix version string to prepend to the key
         */
        CipherVersion(final String prefix) {
            m_prefix = prefix.getBytes(StandardCharsets.UTF_8);
        }

        /**
         * Tries to extract a raw key from the given string.
         *
         * @param encoded
         * @return raw key if the byte array was packaged with this version, {@link Optional#empty()} otherwise
         */
        final Optional<byte[]> extractKey(final String encoded) {
            return extractKey(m_prefix, HexUtils.hexToBytes(encoded));
        }

        abstract Optional<byte[]> extractKey(final byte[] prefix, final byte[] versionedKey);

        /**
         * Packages a raw key according to this version.
         *
         * @param rawKey raw key to package
         * @return packaged key
         */
        final String versionedKeyHex(final byte[] rawKey) {
            return HexUtils.bytesToHex(packageKey(m_prefix, rawKey));
        }

        abstract byte[] packageKey(final byte[] prefix, final byte[] rawKey);
    }

    /** Current version of the cipher. */
    private static final CipherVersion CURRENT_VERSION = CipherVersion.values()[0];

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowCipher.class);

    /** The null cipher, no encryption, always unlocked. */
    public static final WorkflowCipher NULL_CIPHER = new WorkflowCipher(null, null, null, true);

    private boolean m_isUnlocked;
    private final SecretKey m_secretKey;
    private final String m_passwordHint;
    private final byte[] m_passwordDigest;

    /**
     * @param passwordBytes
     * @param passwordDigest */
    private WorkflowCipher(final SecretKey secretKey, final byte[] passwordDigest, final String passwordHint,
            final boolean isUnlocked) {
        m_secretKey = secretKey;
        m_passwordDigest = passwordDigest;
        m_passwordHint = passwordHint;
        m_isUnlocked = isUnlocked;
    }

    /** @return whether unlocked, i.e. not encrypted or correct
     *          password entered.
     */
    boolean isUnlocked() {
        return m_isUnlocked;
    }

    /** Unlock metanode.
     * @param prompt The callback to prompt for the password.
     * @return If successfully unlocked (or already unlocked).
     */
    boolean unlock(final WorkflowCipherPrompt prompt) {
        if (m_isUnlocked) {
            return true;
        }
        String hint = getPasswordHint();
        boolean isPromptForComponent = prompt.isPromptForComponent();
        if (hint == null || hint.length() == 0) {
            hint = (isPromptForComponent ? "Component" : "Metanode") + " is protected by password: ";
        }
        boolean result;
        LOGGER.debug("Prompting user for " + (isPromptForComponent ? "component" : "metanode") + " password, hint \""
            + hint + "\"");
        try {
            String enteredPass = prompt.prompt(hint, null);
            byte[] digest = hashPassword(enteredPass);
            while (enteredPass == null || !Arrays.equals(m_passwordDigest, digest)) {
                enteredPass = prompt.prompt(hint, "The entered password is wrong.");
                digest = hashPassword(enteredPass);
            }
            LOGGER.debug(
                "Entered password is correct; " + (isPromptForComponent ? "component" : "metanode") + " unlocked");
            result = true;
        } catch (PromptCancelled cancel) {
            LOGGER.debug((isPromptForComponent ? "Component" : "Metanode") + " password prompt was cancelled");
            result = false;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Unable to check password", e);
            result = false;
        }
        m_isUnlocked = result;
        return result;
    }

    /**
     * @param opmode
     * @return
     * @throws IOException */
    private Cipher initJavaCryptoCipher(final int opmode) throws IOException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(m_secretKey.getAlgorithm());
            cipher.init(opmode, m_secretKey);
        } catch (Exception e) {
            throw new IOException("Unable to init cipher", e);
        }
        return cipher;
    }

    /** Used to open an input stream using this cipher.
     * @param input The input
     * @return deciphered input
     * @throws IOException If that fails. */
    InputStream decipherInput(final InputStream input) throws IOException {
        if (m_secretKey == null) {
            return input;
        }
        Cipher cipher = initJavaCryptoCipher(Cipher.DECRYPT_MODE);
        return new CipherInputStream(input, cipher);
    }

    private OutputStream cipherOutput(final OutputStream output)
        throws IOException {
        if (m_secretKey == null) {
            return output;
        }
        Cipher cipher = initJavaCryptoCipher(Cipher.ENCRYPT_MODE);
        return new CipherOutputStream(output, cipher);
    }

    /** Cipher output.
     * @param wfm The workflow to cipher (possibly the parent also ciphers).
     * @param out The underlying output, i.e. file output.
     * @return The The ciphered output, possibly wrapped.
     * @throws IOException if that fails. */
    @SuppressWarnings("resource")
    OutputStream cipherOutput(final WorkflowManager wfm,
            final OutputStream out) throws IOException {
        if (wfm == WorkflowManager.ROOT) {
            return out;
        }
        OutputStream myOut = cipherOutput(out);
        return wfm.getDirectNCParent().cipherOutput(myOut);
    }

    /** @return if non null cipher. */
    boolean isEncrypted() {
        return !isNullCipher();
    }

    /** Implementation of {@link WorkflowManager#getCipherFileName(String)}.
     * @param wfm ...
     * @param name ...
     * @return ...
     */
    static String getCipherFileName(final NodeContainerParent wfm, final String name) {
        boolean isEncrypted;
        NodeContainerParent curNCParent = wfm;
        do {
            isEncrypted = curNCParent.getWorkflowCipher().isEncrypted();
            curNCParent = curNCParent.getDirectNCParent();
        } while (!isEncrypted && curNCParent != null);
        if (isEncrypted) {
            return getCipherFileName(name);
        }
        return name;
    }

    /** The name appended with '.encrypted'.
     * @param name ...
     * @return name + ".encrypted" */
    static String getCipherFileName(final String name) {
        return name.concat(".encrypted");
    }

    /** @return the hint/copyright associated with this cipher (user message).*/
    String getPasswordHint() {
        return m_passwordHint;
    }

    /** @return whether this is {@link #NULL_CIPHER}. */
    boolean isNullCipher() {
        return NULL_CIPHER == this;
    }

    static boolean isNullCipherDef(final CipherDef def) {
        return def == null || def.getEncryptionKey() == null || def.getPasswordDigest() == null;
    }

    /** Save cipher settings.
     * @param cipherSettings to save to. */
    void save(final NodeSettingsWO cipherSettings) {
        final String passwordDigestHex = HexUtils.bytesToHex(m_passwordDigest);
        final String encryptionKeyHex = CURRENT_VERSION.versionedKeyHex(m_secretKey.getEncoded());
        cipherSettings.addString("passwordDigest", passwordDigestHex);
        cipherSettings.addString("encryptionKey", encryptionKeyHex);
        cipherSettings.addString("passwordHint", m_passwordHint);
    }

    /** Load cipher settings.
     * @param version Workflow version.
     * @param cipherSettings Settings to load from.
     * @return A new cipher settings object.
     * @throws InvalidSettingsException if that fails. */
    static WorkflowCipher load(final LoadVersion version, final NodeSettingsRO cipherSettings)
        throws InvalidSettingsException {
        String passwordDigestHex = cipherSettings.getString("passwordDigest");
        String encryptionKeyHex = cipherSettings.getString("encryptionKey");
        byte[] passwordDigest = HexUtils.hexToBytes(passwordDigestHex);
        byte[] encryptionKey = extractRawKey(encryptionKeyHex);
        String hint = cipherSettings.getString("passwordHint");
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");
        return new WorkflowCipher(keySpec, passwordDigest, hint, false);
    }

    /**
     * Strips any version prefixes and extracts the raw entryption key.
     *
     * @param encryptionKey versioned encryption key
     * @return raw encryption key
     * @see CipherVersion#extractKey(byte[])
     */
    private static byte[] extractRawKey(final String encryptionKey) {
        // Enum#values() enumerates all members in declaration order (JLS 17, Section 8.9.3)
        return Arrays.stream(CipherVersion.values()).flatMap(v -> v.extractKey(encryptionKey).stream()).findFirst()
            .orElseThrow(() -> new IllegalStateException("Cipher Version 0 should accept all keys."));
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowCipher clone() {
        if (isNullCipher()) {
            return this;
        }
        try {
            return (WorkflowCipher)super.clone();
        } catch (CloneNotSupportedException e) {
            InternalError error = new InternalError(e.getMessage());
            error.initCause(e);
            throw error;
        }
    }

    /** Create new cipher with a given password and hint.
     * @param password The password (if null use {@link #NULL_CIPHER})
     * @param hint The hint.
     * @return A new cipher
     * @throws NoSuchAlgorithmException if encryption fails. */
    static WorkflowCipher newCipher(final String password, final String hint)
        throws NoSuchAlgorithmException {
        if (password == null) {
            return NULL_CIPHER;
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey encryptionKey = keyGenerator.generateKey();
        byte[] passwordDigest = hashPassword(password);
        return new WorkflowCipher(encryptionKey, passwordDigest, hint, true);
    }

    private static byte[] hashPassword(final String password) throws NoSuchAlgorithmException  {
        if (password == null) {
            return null;
        }
        return MessageDigest.getInstance("SHA1").digest(password.getBytes());
    }

    static WorkflowCipher toWorkflowCipher(final CipherDef def) {
        if (isNullCipherDef(def)) {
            return NULL_CIPHER;
        }
        var passwordDigestHex = def.getPasswordDigest();
        var encryptionKeyHex = def.getEncryptionKey();
        byte[] passwordDigest = HexUtils.hexToBytes(passwordDigestHex);
        byte[] encryptionKey = extractRawKey(encryptionKeyHex);
        var hint = def.getPasswordHint();
        var keySpec = new SecretKeySpec(encryptionKey, "AES");
        return new WorkflowCipher(keySpec, passwordDigest, hint, false);
    }

    CipherDef toDef() {
        if (isNullCipher()) {
            return new CipherDefBuilder().build();
        }
        var passwordDigestHex = HexUtils.bytesToHex(m_passwordDigest);
        var encryptionKeyHex = CURRENT_VERSION.versionedKeyHex(m_secretKey.getEncoded());
        return new CipherDefBuilder() //
            .setEncryptionKey(encryptionKeyHex) //
            .setPasswordDigest(passwordDigestHex) //
            .setPasswordHint(m_passwordHint) //
            .build();
    }

}
