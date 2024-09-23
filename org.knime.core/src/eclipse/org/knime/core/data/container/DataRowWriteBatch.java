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
 *   19 Sept 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.container;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.v2.RowBatch;
import org.knime.core.data.v2.RowKeyType;
import org.knime.core.data.v2.RowWrite;
import org.knime.core.data.v2.WriteBatch;
import org.knime.core.data.v2.schema.ValueSchemaUtils;

/**
 * A fully in-memory row batch that materializes the row write contents in-memory.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 */
public final class DataRowWriteBatch implements WriteBatch {

    private final DataTableSpec m_spec;

    private final List<DataRow> m_buffer = new ArrayList<>();

    private final BufferedRowWrite m_row;

    private boolean m_needsCommit;

    private boolean m_closed;

    public DataRowWriteBatch(final DataTableSpec spec) {
        final var schema =
            ValueSchemaUtils.create(spec, RowKeyType.CUSTOM, new NotInWorkflowWriteFileStoreHandler(UUID.randomUUID()));
        m_row = new BufferedRowWrite(schema);
        m_spec = spec;
    }

    @Override
    public RowWrite forward() {
        if (m_closed) {
            throw new IllegalStateException("Write batch is already closed");
        }
        commitIfNecessary();
        m_needsCommit = true;
        return m_row;
    }

    private void commitIfNecessary() {
        if (m_needsCommit) {
            m_buffer.add(m_row.materializeDataRow());
            m_needsCommit = false;
        }
    }

    @Override
    public boolean canForward() {
        return true;
    }

    @Override
    public void close() {
        m_closed = true;
    }

    @Override
    public RowBatch finish() {
        commitIfNecessary();
        return new InMemoryRowBatch(m_spec, m_buffer);
    }

    @Override
    public long size() {
        return m_buffer.size();
    }

}