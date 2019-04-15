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
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 12, 2008 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.WorkflowDataRepository;

/**
 * Special table implementation that simply wraps a given
 * {@link BufferedDataTable}. This class is used by the framework and should not
 * be of public interest.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class WrappedTable implements KnowsRowCountTable {

    private final BufferedDataTable m_table;

    /** Creates new table wrapping the argument.
     * @param table Table to wrap
     * @throws NullPointerException If argument is null.
     */
    public WrappedTable(final BufferedDataTable table) {
        if (table == null) {
            throw new NullPointerException("Table must not be null.");
        }
        m_table = table;
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
    }

    /** {@inheritDoc} */
    @Override
    public void ensureOpen() {

    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getReferenceTables() {
        return new BufferedDataTable[]{m_table};
    }

    /**
     * {@inheritDoc}
     * @deprecated use {@link #size()} instead which supports more than {@link Integer#MAX_VALUE} rows
     */
    @Override
    @Deprecated
    public int getRowCount() {
        return KnowsRowCountTable.checkRowCount(size());
    }

    /**
     * {@inheritDoc}
     * @since 3.0
     */
    @Override
    public long size() {
        return m_table.size();
    }

    /** {@inheritDoc} */
    @Override
    public CloseableRowIterator iterator() {
        return m_table.iterator();
    }

    @Override
    public CloseableRowIterator iteratorWithFilter(final TableFilter filter, final ExecutionMonitor exec) {
        return m_table.filter(filter, exec).iterator();
    }

    /** {@inheritDoc} */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_table.getDataTableSpec();
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

    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_ID = "table_reference_ID";

    /** {@inheritDoc} */
    @Override
    public void saveToFile(final File f, final NodeSettingsWO s,
            final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addInt(CFG_REFERENCE_ID, m_table.getBufferedTableId());
    }

    /** Restore table, reverse operation to
     * {@link #saveToFile(File, NodeSettingsWO, ExecutionMonitor) save}.
     * @param s To load from
     * @param tblRep Global table loader map.
     * @param dataRepository The data repository (needed for blobs, file stores, and table ids).
     * @return A freshly created wrapped table.
     * @throws InvalidSettingsException If settings are invalid.
     * @since 3.7
     */
    public static WrappedTable load(final NodeSettingsRO s, final Map<Integer, BufferedDataTable> tblRep,
        final WorkflowDataRepository dataRepository)
        throws InvalidSettingsException {
        NodeSettingsRO subSettings = s.getNodeSettings(CFG_INTERNAL_META);
        int refID = subSettings.getInt(CFG_REFERENCE_ID);
        BufferedDataTable reference =
            BufferedDataTable.getDataTable(tblRep, refID, dataRepository);
        return new WrappedTable(reference);
    }

}
