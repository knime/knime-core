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
 *   Jun 25, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * Wraps name and enumerated number to a file store object.
 *
 * @noreference This class is not intended to be referenced by clients.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class FileStoreKey implements Comparable<FileStoreKey> {

    /** Separator used in serialization. Introduced as "_" in 5.1.0 but switched to ":" in 5.1.3 **/
    private static final String SERIALIZATION_SEPARATOR = ":";

    /**
     * String-serialization version introduced in 5.1.3 and set to v1. 5.1.0 started with v0 but had no version field
     **/
    static final String STRING_SERIALIZATION_VERSION = "v1";

    /** Version number, introduced in 4.0.1 for 'large' loop counts. */
    private static final int VERSION_NUM = -20190808;

    private final UUID m_storeUUID;

    /**
     * Prior KNIME 4.0.1 this was a byte[]. The save/load methods are changed so that it reads/writes backward
     * compatibly unless there are a large number of parallel loops (see AP-6372).
     */
    private final int[] m_nestedLoopPath;

    private final int m_iterationIndex;

    private final int m_index;

    private final String m_name;

    /**
     * @param index
     * @param name
     */
    public FileStoreKey(final UUID storeUUID, final int index, final int[] nestedLoopPath, final int iterationIndex,
        final String name) {
        if (name == null || storeUUID == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        // iteration < 0 --> no loops (neither nested or directly)
        if ((iterationIndex < 0) != (nestedLoopPath == null)) {
            throw new IllegalArgumentException(
                String.format("Invalid argument %d  -  %s", iterationIndex, Arrays.toString(nestedLoopPath)));
        }
        m_storeUUID = storeUUID;
        m_index = index;
        m_name = name;
        m_nestedLoopPath = nestedLoopPath;
        m_iterationIndex = iterationIndex;
    }

    /** @return the storeUUID */
    public UUID getStoreUUID() {
        return m_storeUUID;
    }

    /** @return the nestedLoopPath */
    public int[] getNestedLoopPath() {
        return m_nestedLoopPath;
    }

    /** @return the name */
    public String getName() {
        return m_name;
    }

    /** @return the name, possibly corrected by loop information. */
    public String getNameOnDisc() {
        if (m_iterationIndex < 0) {
            return m_name;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < m_nestedLoopPath.length; i++) {
            b.append(i > 0 ? "-" : "").append(m_nestedLoopPath[i]);
        }
        b.append(m_nestedLoopPath.length > 0 ? "_" : "");
        b.append(m_index).append("_");
        b.append(m_iterationIndex);
        b.append("_").append(m_name);
        return b.toString();
    }

    /** @return the index */
    public int getIndex() {
        return m_index;
    }

    public void save(final DataOutput output) throws IOException {
        output.writeUTF(m_storeUUID.toString());
        output.writeInt(m_index);
        output.writeUTF(m_name);
        output.writeInt(m_iterationIndex);
        if (m_iterationIndex >= 0) {
            int maximum = IntStream.of(m_nestedLoopPath).max().orElse(0);
            if (maximum > Byte.MAX_VALUE) { // see m_nestedLoopPath javadoc for details
                output.writeInt(VERSION_NUM); // version number, introduced in 4.0.1 for 'large' loop counts
                output.writeInt(m_nestedLoopPath.length);
                for (int i : m_nestedLoopPath) {
                    output.writeInt(i);
                }
            } else {
                output.writeInt(m_nestedLoopPath.length);
                byte[] asBytes = toByteArray(m_nestedLoopPath);
                output.write(asBytes);
            }
        }
    }

    public static FileStoreKey load(final DataInput input) throws IOException {
        UUID uuid = UUID.fromString(input.readUTF());
        int index = input.readInt();
        String name = input.readUTF();
        int iterationIndex = input.readInt();
        int[] nestedLoopPath = null;
        if (iterationIndex >= 0) {
            int i = input.readInt();
            if (i != VERSION_NUM) { // all values fit into byte (see m_nestedLoopPath for details)
                int nestedLoopPathLength = i;
                byte[] nestedLoopPathBytes = new byte[nestedLoopPathLength];
                input.readFully(nestedLoopPathBytes);
                nestedLoopPath = toIntArray(nestedLoopPathBytes);
            } else {
                // (i represent the version number -- this implementation only understands (and assumes) -20190808)
                int nestedLoopPathLength = input.readInt();
                nestedLoopPath = new int[nestedLoopPathLength];
                for (int c = 0; c < nestedLoopPathLength; c++) {
                    nestedLoopPath[c] = input.readInt();
                }
            }

        }
        return new FileStoreKey(uuid, index, nestedLoopPath, iterationIndex, name);
    }

    public void save(final ModelContentWO output) {
        output.addString("storeUUID", m_storeUUID.toString());
        output.addInt("index", m_index);
        output.addString("name", m_name);
        output.addInt("iterationIndex", m_iterationIndex);
        if (m_iterationIndex >= 0) {
            int maximum = IntStream.of(m_nestedLoopPath).max().orElse(0);
            if (maximum > Byte.MAX_VALUE) { // see m_nestedLoopPath javadoc for details
                output.addInt("version", VERSION_NUM); // version number, introduced in 4.0.1 for 'large' loop counts
                output.addIntArray("nestedLoopPathInt", m_nestedLoopPath);
            } else {
                output.addByteArray("nestedLoopPath", toByteArray(m_nestedLoopPath));
            }
        }
    }

    public static FileStoreKey load(final ModelContentRO input) throws InvalidSettingsException {
        UUID storeUUID;
        final String storeUUIDs = input.getString("storeUUID");
        try {
            storeUUID = UUID.fromString(storeUUIDs);
        } catch (Exception e) {
            throw new InvalidSettingsException("Can't parse storeUUID from \"" + storeUUIDs + "\"", e);
        }
        int index = input.getInt("index");
        String name = input.getString("name");
        int iterationIndex = input.getInt("iterationIndex");
        int[] nestedLoopPath = null;
        if (iterationIndex >= 0) {
            int version = input.getInt("version", 0); // added in 4.0.1, see #save and m_nestedLoopPath javadoc
            if (version != VERSION_NUM) { // all values fit into byte (see m_nestedLoopPath for details)
                nestedLoopPath = toIntArray(input.getByteArray("nestedLoopPath"));
            } else {
                nestedLoopPath = input.getIntArray("nestedLoopPathInt");
            }
        }
        return new FileStoreKey(storeUUID, index, nestedLoopPath, iterationIndex, name);
    }

    /**
     * Save this {@link FileStoreKey} to a string, such that it can be restored with {@link #load(String)}
     *
     * @return The string representation of this {@link FileStoreKey}
     * @since 5.1
     */
    public String saveToString() {
        var builder = new StringBuilder();
        builder.append(STRING_SERIALIZATION_VERSION);
        builder.append(SERIALIZATION_SEPARATOR);

        builder.append(m_storeUUID.toString());
        builder.append(SERIALIZATION_SEPARATOR);

        builder.append(m_index);
        builder.append(SERIALIZATION_SEPARATOR);

        builder.append(m_name);
        builder.append(SERIALIZATION_SEPARATOR);

        builder.append(m_iterationIndex);

        if (m_iterationIndex >= 0) {
            for (int i : m_nestedLoopPath) {
                builder.append(SERIALIZATION_SEPARATOR);
                builder.append(i);
            }
        }
        return builder.toString();
    }

    /**
     * Restore a {@link FileStoreKey} from its string representation as created by {@link #saveToString()}
     *
     * @param stringRepresentation
     * @since 5.1
     * @return The FileStoreKey that was restored from the given stringRepresentation
     * @throws IllegalArgumentException if the string did not match the expected format
     * @throws NumberFormatException if parts of the string were expected to contain an integer but could not be parsed
     */
    public static FileStoreKey load(final String stringRepresentation) {
        String[] parts = stringRepresentation.split(SERIALIZATION_SEPARATOR);
        if (parts.length == 1) {
            // Version 5.1.0 used "_" as separator, but as that can be part of the name (e.g. after
            // WriteFileStoreHandler.copyFileStore) we switched to ":" in 5.1.3. To be able to read the previous
            // FileStoreKey representation, we fall back to splitting the string by "_".
            parts = stringRepresentation.split("_");
        } else {
            if (parts.length > 1) {
                if (!parts[0].equals(STRING_SERIALIZATION_VERSION)) {
                    throw new IllegalArgumentException(
                        "Found FileStoreKey with unknown serialization version: " + parts[0]);
                }
                // drop version
                parts = Arrays.copyOfRange(parts, 1, parts.length);
            }
        }

        if (parts.length < 4) {
            throw new IllegalArgumentException(
                "The string representation of a FileStoreKey must contain at least 4 parts separated by underscores,"
                    + "but got " + stringRepresentation);
        }

        int[] nestedLoopPath = null;
        final UUID uuid = UUID.fromString(parts[0]);
        final int index = Integer.parseInt(parts[1]);
        final int iterationIndex = Integer.parseInt(parts[3]);

        if (iterationIndex >= 0) {
            nestedLoopPath = Arrays.stream(parts).skip(4).mapToInt(Integer::parseInt).toArray();
        }
        return new FileStoreKey(uuid, index, nestedLoopPath, iterationIndex, parts[2]);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(getNameOnDisc());
        b.append("-(").append(m_storeUUID).append(")");
        return b.toString();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + m_index;
        result = prime * result + m_iterationIndex;
        result = prime * result + m_name.hashCode();
        result = prime * result + Arrays.hashCode(m_nestedLoopPath);
        result = prime * result + m_storeUUID.hashCode();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FileStoreKey)) {
            return false;
        }
        FileStoreKey other = (FileStoreKey)obj;
        if (m_index != other.m_index) {
            return false;
        }
        if (m_iterationIndex != other.m_iterationIndex) {
            return false;
        }
        if (!m_name.equals(other.m_name)) {
            return false;
        }
        if (!Arrays.equals(m_nestedLoopPath, other.m_nestedLoopPath)) {
            return false;
        }
        if (!m_storeUUID.equals(other.m_storeUUID)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final FileStoreKey o) {
        int comp = m_storeUUID.compareTo(o.getStoreUUID());
        if (comp != 0) {
            return comp;
        }

        comp = m_index - o.m_index;
        if (comp != 0) {
            return comp;
        }

        int thisNestedLoopLength = m_nestedLoopPath == null ? 0 : m_nestedLoopPath.length;
        int oNestedLoopLength = o.m_nestedLoopPath == null ? 0 : o.m_nestedLoopPath.length;
        for (int i = 0; i < Math.max(oNestedLoopLength, thisNestedLoopLength); i++) {
            Integer thisV = getValueFromArray(m_nestedLoopPath, i);
            Integer oV = getValueFromArray(o.m_nestedLoopPath, i);
            comp = thisV.compareTo(oV);
            if (comp != 0) {
                return comp;
            }
        }

        comp = m_iterationIndex - o.m_iterationIndex;
        if (comp != 0) {
            return comp;
        }

        assert o.m_name.equals(m_name) : o.m_name + " vs. " + m_name;
        return 0;
    }

    private static Integer getValueFromArray(final int[] array, final int index) {
        if (array == null || array.length >= index - 1) {
            return Integer.valueOf(-1);
        }
        return Integer.valueOf(array[index]);
    }

    private static int[] toIntArray(final byte[] bytes) {
        int[] result = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i];
        }
        return result;
    }

    private static byte[] toByteArray(final int[] ints) {
        byte[] result = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            result[i] = (byte)ints[i];
        }
        return result;
    }

}
