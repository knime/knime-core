/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedRowsTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.DuplicateKeyException;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 *
 */
public final class ConcatenateTable implements KnowsRowCountTable {
    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_IDS = "table_reference_IDS";
    private static final String CFG_ROW_COUNT = "table_rowcount";
    private static final String CFG_ROW_COUNT_L = "table_rowcount_long";

    private AppendedRowsTable m_tablesWrapper;
    private long m_rowCount;
    private BufferedDataTable[] m_tables;
    private DataTableSpec m_spec;

    private ConcatenateTable(final BufferedDataTable[] tables, final long rowCount) {
        m_rowCount = rowCount;

        // check whether all specs are the same and pass that spec using createSpec(specs);
        DataTableSpec firstSpec = tables[0].getDataTableSpec();
        for (int i = 1; i < tables.length; i++) {
            if (!firstSpec.equalStructure(tables[i].getDataTableSpec())) {
                //table specs don't match -> we need to use the AppendedRowsTable
                //create a new wrapper table without duplicate checking (was done already on creation)
                m_tablesWrapper = new AppendedRowsTable(AppendedRowsTable.DuplicatePolicy.Fail, null, tables);
                m_spec = m_tablesWrapper.getDataTableSpec();
            }
        }
        if(m_tablesWrapper == null) {
            //all table specs are equal
            DataTableSpec[] specs = new DataTableSpec[tables.length];
            for (int i = 0; i < specs.length; i++) {
                specs[i] = tables[i].getDataTableSpec();
            }
            m_spec = createSpec(specs);
        }
        m_tables = tables;
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
     *
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
        // return MyIterator if all specs are the same indicated by m_tablesWrapper == null
        if(m_tablesWrapper == null) {
            return new MyIterator();
        } else {
            return m_tablesWrapper.iterator(null, -1);
        }
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
        if (m_rowCount <= Integer.MAX_VALUE) {
            subSettings.addInt(CFG_ROW_COUNT, (int) m_rowCount);
        } else {
            subSettings.addLong(CFG_ROW_COUNT_L, m_rowCount);
        }
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
        return new ConcatenateTable(tables, rowCount);
    }

    /**
     * Creates a new table from argument tables. This methods checks for row key duplicates over all given tables.
     *
     * @param mon for progress info/cancellation
     * @param checkForDuplicates if for duplicates should be checked. If <code>false</code> the row keys of the input
     *            tables MUST be unique over all tables!
     * @param tables Tables to put together.
     * @return The new table.
     * @throws CanceledExecutionException If cancelled.
     * @since 3.1
     */
    public static ConcatenateTable create(final ExecutionMonitor mon, final boolean checkForDuplicates,
        final BufferedDataTable... tables) throws CanceledExecutionException {
        if(checkForDuplicates) {
            return ConcatenateTable.create(mon, tables);
        } else {
            long rowCount = 0;
            for (int i = 0; i < tables.length; i++) {
                rowCount += tables[i].size();
            }
            return new ConcatenateTable(tables, rowCount);
        }
    }

    /**
     * Creates a new table from argument tables. This methods checks for row key duplicates over all given tables.
     *
     * @param mon for progress info/cancellation
     * @param tables Tables to put together.
     * @return The new table.
     * @throws CanceledExecutionException If cancelled.
     * @throws IOException
     * @throws DuplicateKeyException
     */
    public static ConcatenateTable create(final ExecutionMonitor mon, final BufferedDataTable... tables)
        throws CanceledExecutionException {
        long rowCount = 0;
        for (int i = 0; i < tables.length; i++) {
            rowCount += tables[i].size();
        }
        DuplicateChecker check = new DuplicateChecker();
        int r = 0;
        for (int i = 0; i < tables.length; i++) {
            for (DataRow row : tables[i]) {
                RowKey key = row.getKey();
                try {
                    check.addKey(key.toString());
                } catch (DuplicateKeyException | IOException ex) {
                    throw new IllegalArgumentException("Duplicate row key \""
                            + key + "\" in table with index " + i);
                }
                r++;
                mon.setProgress(r / (double)rowCount,
                    "Checking tables, row " + r + "/" + rowCount + " (\"" + row.getKey() + "\")");
            }
            mon.checkCanceled();
        }
        try {
            check.checkForDuplicates();
        } catch (DuplicateKeyException | IOException ex) {
            throw new IllegalArgumentException("Duplicate row keys");
        }
        return new ConcatenateTable(tables, rowCount);
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
