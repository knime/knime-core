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
 * -------------------------------------------------------------------
 *
 * History
 *   Feb 15, 2007 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.WorkflowDataRepository;

/**
 * A table that is void and only a placeholder for a streamed output.
 * <p>
 * <b>This class is not intended to be used in any node implementation, it is public only because some KNIME framework
 * classes access it.</b>
 * <p>
 * This class is used to represent the {@link BufferedDataTable} that is returned by the
 * {@link org.knime.core.node.ExecutionContext}s
 * {@link org.knime.core.node.ExecutionContext#createJoinedTable(BufferedDataTable, BufferedDataTable, ExecutionMonitor)}
 * method.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @since 3.1
 */
public final class VoidTable implements KnowsRowCountTable {

    private final DataTableSpec m_spec;

    /**
     * Creates new object. No checks are done.
     *
     * @param spec The proper spec.
     */
    private VoidTable(final DataTableSpec spec) {
        m_spec = CheckUtils.checkArgumentNotNull(spec, "Spec must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseableRowIterator iterator() {
        return new CloseableRowIterator() {

            @Override
            public DataRow next() {
                throw new IllegalStateException("no next row on void table");
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public CloseableRowIterator iteratorWithFilter(final TableFilter filter, final ExecutionMonitor exec) {
        return iterator();
    }

    /** Does nothing. {@inheritDoc} */
    @Override
    public void clear() {
    }

    /** Internal use. {@inheritDoc} */
    @Override
    public void ensureOpen() {
        // no own data, only referencing other tables
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getReferenceTables() {
        return ContainerTable.EMPTY_ARRAY;
    }

    /** {@inheritDoc}
     * @return 0 */
    @Override
    @Deprecated
    public int getRowCount() {
        return 0;
    }

    /** {@inheritDoc}
     * @return 0 */
    @Override
    public long size() {
        return 0L;
    }

    /** {@inheritDoc} */
    @Override
    public void putIntoTableRepository(final WorkflowDataRepository dataRepository) {
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeFromTableRepository(final WorkflowDataRepository dataRepository) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void saveToFile(final File f, final NodeSettingsWO settings, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * Method being called when the workflow is restored and the table shall be recreated.
     *
     * @param spec The final non-null spec.
     * @return The restored table.
     */
    public static VoidTable load(final DataTableSpec spec) {
        return new VoidTable(spec);
    }

    /**
     * Creates new "void" table based on a table spec.
     *
     * @param spec The spec represented by this table.
     * @return A void table.
     * @throws IllegalArgumentException If argument is null.
     */
    public static VoidTable create(final DataTableSpec spec) {
        return new VoidTable(spec);
    }

}
