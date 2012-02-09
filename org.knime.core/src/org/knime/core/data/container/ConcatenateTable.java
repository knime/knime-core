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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 * History
 *   Jan 5, 2007 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ConcatenateTable implements KnowsRowCountTable {

    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_IDS = "table_reference_IDS";
    private static final String CFG_ROW_COUNT = "table_rowcount";

    private final BufferedDataTable[] m_tables;
    private final DataTableSpec m_spec;
    private final int m_rowCount;

    private ConcatenateTable(final BufferedDataTable[] tables,
            final DataTableSpec spec, final int rowCount) {
        m_tables = tables;
        m_spec = spec;
        m_rowCount = rowCount;
    }

    /** Internal use.
     * {@inheritDoc} */
    @Override
    public void clear() {
        // left empty, it's up to the node to clear our underlying tables.
    }

    /** Internal use.
     * {@inheritDoc} */
    @Override
    public void ensureOpen() {
        // no own data, only referencing other tables
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getReferenceTables() {
        return m_tables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_rowCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /** {@inheritDoc} */
    @Override
    public CloseableRowIterator iterator() {
        return new MyIterator();
    }

    /** {@inheritDoc} */
    @Override
    public void putIntoTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
        // no new tables, ignore
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeFromTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
        // no new tables, ignore
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void saveToFile(final File f, final NodeSettingsWO s,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        int[] referenceIDs = new int[m_tables.length];
        for (int i = 0; i < m_tables.length; i++) {
            referenceIDs[i] = m_tables[i].getBufferedTableId();
        }
        subSettings.addIntArray(CFG_REFERENCE_IDS, referenceIDs);
        subSettings.addInt(CFG_ROW_COUNT, m_rowCount);
    }

    /** Restore table form node settings object.
     * @param s Containing information.
     * @param spec Associated spec.
     * @param tblRep For table lookup
     * @return The newly instantiated table.
     * @throws InvalidSettingsException If information is invalid.
     */
    public static ConcatenateTable load(final NodeSettingsRO s,
            final DataTableSpec spec,
            final Map<Integer, BufferedDataTable> tblRep)
        throws InvalidSettingsException {
        NodeSettingsRO subSettings = s.getNodeSettings(CFG_INTERNAL_META);
        int[] referenceIDs = subSettings.getIntArray(CFG_REFERENCE_IDS);
        int rowCount = subSettings.getInt(CFG_ROW_COUNT);
        BufferedDataTable[] tables = new BufferedDataTable[referenceIDs.length];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = BufferedDataTable.getDataTable(tblRep, referenceIDs[i]);
        }
        return new ConcatenateTable(tables, spec, rowCount);
    }

    /** Creates a new table from argument tables.
     * @param mon for progress info/cancellation
     * @param tables Tables to put together.
     * @return The new table.
     * @throws CanceledExecutionException If cancelled.
     */
    public static ConcatenateTable create(
            final ExecutionMonitor mon, final BufferedDataTable... tables)
            throws CanceledExecutionException {
        DataTableSpec[] specs = new DataTableSpec[tables.length];
        int rowCount = 0;
        for (int i = 0; i < tables.length; i++) {
            specs[i] = tables[i].getDataTableSpec();
            rowCount += tables[i].getRowCount();
        }
        DataTableSpec finalSpec = createSpec(specs);
        HashSet<RowKey> hash = new HashSet<RowKey>();
        int r = 0;
        for (int i = 0; i < tables.length; i++) {
            for (DataRow row : tables[i]) {
                RowKey key = row.getKey();
                if (!hash.add(key)) {
                    throw new IllegalArgumentException("Duplicate row key \""
                            + key + "\" in table with index " + i);
                }
                r++;
                mon.setProgress(r / (double)rowCount, "Checking tables, row "
                        + r + "/" + rowCount + " (\"" + row.getKey() + "\")");
            }
            mon.checkCanceled();
        }
        return new ConcatenateTable(tables, finalSpec, rowCount);
    }

    /** Creates merged table spec.
     * @param specs the argument tables.
     * @return the new spec
     * @see DataTableSpec#mergeDataTableSpecs(DataTableSpec...)
     */
    public static DataTableSpec createSpec(final DataTableSpec... specs) {
        return DataTableSpec.mergeDataTableSpecs(specs);
    }

    private class MyIterator extends CloseableRowIterator {
        private int m_tableIndex;
        private CloseableRowIterator m_curIterator;
        private DataRow m_next;

        /** Creates new iterator. */
        public MyIterator() {
            m_tableIndex = 0;
            m_curIterator = m_tables[m_tableIndex].iterator();
            m_next = internalNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_next != null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            DataRow next = m_next;
            m_next = internalNext();
            return next;
        }

        private DataRow internalNext() {
            if (m_curIterator.hasNext()) {
                return m_curIterator.next();
            }
            if (m_tableIndex < m_tables.length - 1) {
                m_tableIndex++;
                m_curIterator = m_tables[m_tableIndex].iterator();
                return internalNext();
            }
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
            m_curIterator.close();
            m_tableIndex = m_tables.length;
        }

    }

}
