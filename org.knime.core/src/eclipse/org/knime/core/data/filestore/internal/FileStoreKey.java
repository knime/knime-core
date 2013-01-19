/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 25, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/** Wraps name and enumerated number to a file store object.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class FileStoreKey implements Comparable<FileStoreKey> {

    private final UUID m_storeUUID;
    private final byte[] m_nestedLoopPath;
    private final int m_iterationIndex;
    private final int m_index;
    private final String m_name;
    /**
     * @param index
     * @param name */
    FileStoreKey(final UUID storeUUID, final int index,
            final byte[] nestedLoopPath,
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
    public byte[] getNestedLoopPath() {
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
            output.writeInt(m_nestedLoopPath.length);
            output.write(m_nestedLoopPath);
        }
    }

    public static FileStoreKey load(final DataInput input) throws IOException {
        UUID uuid = UUID.fromString(input.readUTF());
        int index = input.readInt();
        String name = input.readUTF();
        int iterationIndex = input.readInt();
        byte[] nestedLoopPath = null;
        if (iterationIndex >= 0) {
            int nestedLoopPathLength = input.readInt();
            nestedLoopPath = new byte[nestedLoopPathLength];
            input.readFully(nestedLoopPath);
        }
        return new FileStoreKey(uuid, index, nestedLoopPath, iterationIndex, name);
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
            Byte thisV = getValueFromArray(m_nestedLoopPath, i);
            Byte oV = getValueFromArray(o.m_nestedLoopPath, i);
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

    Byte getValueFromArray(final byte[] array, final int index) {
        if (array == null || array.length >= index - 1) {
            return Byte.valueOf((byte)-1);
        }
        return Byte.valueOf(array[index]);
    }

}
