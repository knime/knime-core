/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 28, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2;

import java.util.Arrays;

import org.knime.core.data.RowKeyValue;
import org.knime.core.data.v2.schema.ValueSchema;
import org.knime.core.table.row.WriteAccessRow;

/**
 * Implements a {@link RowWrite} based on a {@link WriteAccessRow}.
 *
 * @since 5.4
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class WriteAccessRowWrite implements RowWrite {

    private final WriteAccessRow m_accesses;

    private final WriteValue<?>[] m_values;

    private final RowKeyWriteValue m_rowKeyValue;

    /**
     * Constructor.
     *
     * @param schema of the table
     * @param writeAccess to write to
     */
    public WriteAccessRowWrite(final ValueSchema schema, final WriteAccessRow writeAccess) {
        m_accesses = writeAccess;
        m_values = new WriteValue<?>[schema.numFactories()];
        Arrays.setAll(m_values, i -> schema.getValueFactory(i).createWriteValue(m_accesses.getWriteAccess(i)));
        m_rowKeyValue = (RowKeyWriteValue)m_values[0];
    }

    @Override
    public void setFrom(final RowRead values) {

        // TODO (TP) We could check instanceof ReadAccessRowRead here and directly
        //           set our WriteAccessRow from their ReadAccessRow
        //           Would that improve performance in any way?
        //           Is it always ok to do this? Or do we need to check for compatibility
        //           of ValueFactories somehow?

        assert values.getNumColumns() == getNumColumns();
        setRowKey(values.getRowKey());
        for (int i = 0; i < getNumColumns(); i++) {
            if (values.isMissing(i)) {
                setMissing(i);
            } else {
                m_values[i + 1].setValue(values.getValue(i));
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <W extends WriteValue<?>> W getWriteValue(final int index) {
        return (W)m_values[index + 1];
    }

    @Override
    public int getNumColumns() {
        return m_values.length - 1;
    }

    @Override
    public void setMissing(final int index) {
        m_accesses.getWriteAccess(index + 1).setMissing();
    }

    @Override
    public void setRowKey(final String rowKey) {
        m_rowKeyValue.setRowKey(rowKey);
    }

    @Override
    public void setRowKey(final RowKeyValue rowKey) {
        m_rowKeyValue.setRowKey(rowKey);
    }
}
