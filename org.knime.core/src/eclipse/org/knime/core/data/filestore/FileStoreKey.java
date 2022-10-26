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

/** Wraps name and enumerated number to a file store object.
 * @noreference This class is not intended to be referenced by clients.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class FileStoreKey implements Comparable<FileStoreKey> {

    /** Version number, introduced in 4.0.1 for 'large' loop counts. */
    private static final int VERSION_NUM = -20190808;

    private final UUID m_storeUUID;
    /** Prior KNIME 4.0.1 this was a byte[]. The save/load methods are changed so that it reads/writes backward
     * compatibly unless there are a large number of parallel loops (see AP-6372).
     */
    private final int[] m_nestedLoopPath;
    private final int m_iterationIndex;
    private final int m_index;
    private final String m_name;
    /**
     * @param index
     * @param name */
    public FileStoreKey(final UUID storeUUID, final int index,
            final int[] nestedLoopPath,
            final int iterationIndex,
            final String name) {
        if (name == null || storeUUID == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        // iteration < 0 --> no loops (neither nested or directly)
        if ((iterationIndex < 0) != (nestedLoopPath == null)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid argument %d  -  %s",
                    iterationIndex, Arrays.toString(nestedLoopPath)));
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
            if (maximum > Byte.MAX_VALUE) {          // see m_nestedLoopPath javadoc for details
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(getNameOnDisc());
        b.append("-(").append(m_storeUUID).append(")");
        return b.toString();
    }

    /**
     * Create a {@link FileStoreKey} from its string representation as returned by {@link FileStoreKey#toString()}.
     *
     * @param stringRepresentation
     * @return The FileStoreKey as described by the string
     * @throws IllegalArgumentException if the string did not have the expected format
     */
    public static FileStoreKey fromString(final String stringRepresentation) {
        String end = null;
        int index = 0;
        int[] nestedLoopPath = null;
        int iterationIndex = -1;

        String[] parts = stringRepresentation.split("_");

        if (parts.length == 0) {
            throw new IllegalArgumentException("String representation of FileStoreKey was not valid");
        }

        if (parts.length == 1) {
            // we only have a name
            end = parts[0];
        } else if (parts.length == 2) {
            throw new IllegalArgumentException("String representation of FileStoreKey was not valid");
        } else if (parts.length == 3) {
            // no nested loop path available, we start directly with the index
            index = Integer.valueOf(parts[0]);
            iterationIndex = Integer.valueOf(parts[1]);
            nestedLoopPath = new int[0];
            end = parts[2];
        } else {
            // parse nested loop path
            String[] nestedLoopIndices = parts[0].split("-");
            nestedLoopPath = Arrays.stream(nestedLoopIndices).mapToInt(s -> Integer.valueOf(s).intValue()).toArray();
            index = Integer.valueOf(parts[1]);
            iterationIndex = Integer.valueOf(parts[2]);
            end = parts[3];
        }

        String[] endParts = end.split("-\\(");
        if (endParts.length != 2) {
            throw new IllegalArgumentException("String representation of FileStoreKey was not valid");
        }
        String storeUUID = endParts[1].substring(0, endParts[1].length() - 1);

        return new FileStoreKey(UUID.fromString(storeUUID), index, nestedLoopPath, iterationIndex, endParts[0]);
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
