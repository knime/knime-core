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
 *   Jul 14, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.WorkflowDataRepository;


/**
 * Table that only replaces the data table spec of an underlying table. This
 * class is not intended for subclassing or to be used in a node model
 * implementation. Instead, use the methods provided through the execution
 * context.
 * @see org.knime.core.node.ExecutionContext#createSpecReplacerTable(
 *       BufferedDataTable, DataTableSpec)
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class TableSpecReplacerTable implements KnowsRowCountTable {

    private final BufferedDataTable m_reference;
    private final DataTableSpec m_newSpec;

    /** Creates new table. Not intended to be used directly for node
     * implementations.
     * @param table The reference table.
     * @param newSpec Its new spec.
     * @throws IllegalArgumentException If the spec doesn't match the data
     * (number of columns)
     */
    public TableSpecReplacerTable(
            final BufferedDataTable table, final DataTableSpec newSpec) {
        DataTableSpec oldSpec = table.getDataTableSpec();
        if (oldSpec.getNumColumns() != newSpec.getNumColumns()) {
            throw new IllegalArgumentException("Table specs have different "
                    + "lengths: " + oldSpec.getNumColumns() + " vs. "
                    + newSpec.getNumColumns());
            // I don't think we can make more assertions here.
        }
        m_reference = table;
        m_newSpec = newSpec;
    }

    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_ID = "table_reference_ID";
    private static final String ZIP_ENTRY_SPEC = "newspec.xml";

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToFile(final File f, final NodeSettingsWO s,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addInt(CFG_REFERENCE_ID, m_reference.getBufferedTableId());
    }

    /**
     * Does nothing.
     * {@inheritDoc}
     */
    @Override
    public void putIntoTableRepository(final WorkflowDataRepository dataRepository) {
    }

    /**
     * Does nothing.
     * {@inheritDoc}
     */
    @Override
    public boolean removeFromTableRepository(final WorkflowDataRepository dataRepository) {
        return false;
    }

    /**
     * Restores table from a file that has been written using KNIME 1.1.x
     * or before. Not intended to be used by node implementations.
     * @param f The file to read from.
     * @param s The settings to get meta information from.
     * @param tblRep The table repository
     * @param dataRepository The data repository (needed for blobs, file stores, and table ids).
     * @return The resulting table.
     * @throws IOException If reading the file fails.
     * @throws InvalidSettingsException If reading the settings fails.
     * @since 3.7
     */
    public static TableSpecReplacerTable load11x(final File f, final NodeSettingsRO s,
        final Map<Integer, BufferedDataTable> tblRep, final WorkflowDataRepository dataRepository)
        throws IOException, InvalidSettingsException {

        try (ZipFile zipFile = new ZipFile(f);
                InputStream in = new BufferedInputStream(zipFile.getInputStream(new ZipEntry(ZIP_ENTRY_SPEC)))) {
            NodeSettingsRO specSettings = NodeSettings.loadFromXML(in);
            DataTableSpec newSpec = DataTableSpec.load(specSettings);
            return load(s, newSpec, tblRep, dataRepository);
        }
    }

    /**
     * Restores table from a file that has been written using KNIME 1.2.0
     * or later. Not intended to be used by node implementations.
     * @param s The settings to get meta information from.
     * @param newSpec The new table spec.
     * @param tblRep The table repository
     * @param dataRepository The data repository (needed for blobs, file stores, and table ids).
     * @return The resulting table.
     * @throws InvalidSettingsException If reading the settings fails.
     * @since 3.7
     */
    public static TableSpecReplacerTable load(final NodeSettingsRO s, final DataTableSpec newSpec,
        final Map<Integer, BufferedDataTable> tblRep, final WorkflowDataRepository dataRepository)
        throws InvalidSettingsException {
        NodeSettingsRO subSettings = s.getNodeSettings(CFG_INTERNAL_META);
        int refID = subSettings.getInt(CFG_REFERENCE_ID);
        BufferedDataTable reference =
            BufferedDataTable.getDataTable(tblRep, refID, dataRepository);
        return new TableSpecReplacerTable(reference, newSpec);
    }

    /**
     * Do not call this method! It's used internally.
     * {@inheritDoc}
     */
    @Override
    public void clear() {
    }

    /** {@inheritDoc} */
    @Override
    public void ensureOpen() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_newSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseableRowIterator iterator() {
        return m_reference.iterator();
    }

    @Override
    public CloseableRowIterator iteratorWithFilter(final TableFilter filter, final ExecutionMonitor exec) {
        return m_reference.filter(filter, exec).iterator();
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
        return m_reference.size();
    }

    /**
     * Get handle to reference table in an array of length 1.
     * @return Reference to that table.
     */
    @Override
    public BufferedDataTable[] getReferenceTables() {
        return new BufferedDataTable[]{m_reference};
    }
}
