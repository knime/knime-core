/*
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
import java.util.UUID;

/** Wraps name and enumerated number to a file store object.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class FileStoreKey {

    private final UUID m_storeUUID;
    private final int m_index;
    private final String m_name;
    /**
     * @param index
     * @param name */
    FileStoreKey(final UUID storeUUID, final int index, final String name) {
        if (name == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_storeUUID = storeUUID;
        m_index = index;
        m_name = name;
    }

    /** @return the storeUUID */
    public UUID getStoreUUID() {
        return m_storeUUID;
    }

    /** @return the name */
    public String getName() {
        return m_name;
    }

    /** @return the index */
    int getIndex() {
        return m_index;
    }

    public void save(final DataOutput output) throws IOException {
        output.writeUTF(m_storeUUID.toString());
        output.writeInt(m_index);
        output.writeUTF(m_name);
    }

    public static FileStoreKey load(final DataInput input) throws IOException {
        UUID uuid = UUID.fromString(input.readUTF());
        int index = input.readInt();
        String name = input.readUTF();
        return new FileStoreKey(uuid, index, name);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // index 391 and name "foobar" to "0391-foobar (uuid-string)"
        return String.format("%04d-%s (%s)", m_index, m_name, m_storeUUID);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_name.hashCode() ^ m_index ^ m_storeUUID.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof FileStoreKey) {
            FileStoreKey o = (FileStoreKey)obj;
            return o.m_index == m_index && o.m_name.equals(m_name)
                && o.m_storeUUID.equals(m_storeUUID);
        }
        return false;
    }

}
