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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.join;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.data.join.JoinedTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteManager;

/**
 * Joins two tables such that the first table appears on the left side of the
 * new table an the second one on the right side.
 *
 * @see JoinedTable
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JoinerNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(JoinerNodeModel.class);

    /** NodeSettings: Method for duplicate treatment, JoinedTable.METHOD_... */
    static final String CFG_DUPLICATE_METHOD = "treatment_duplicate";

    /** NodeSettings: duplicate treatment: suffix text. */
    static final String CFG_SUFFIX = "suffix";

    /** Key for joining the table in memory. */
    static final String CFG_IGNORE_MISSING_ROWS = "missingRows";

    /** method how to go along with duplicates, see METHOD_*. */
    private String m_method;

    /** suffix to append (if any). */
    private String m_suffix;

    private boolean m_ignoreMissingRows;

    private final HiLiteManager m_hiliteManager;

    /** Creates new model, with 2 inports and 1 outport. */
    public JoinerNodeModel() {
        super(2, 1);
        m_method = JoinedTable.METHOD_FAIL;
        m_suffix = "_duplicate";
        m_hiliteManager = new HiLiteManager();
    }

    /**
     * Sets the In<code>HiLiteHandler</code> and registers them at the
     * <code>HiLiteManager</code>.
     *
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        super.setInHiLiteHandler(inIndex, hiLiteHdl);
        m_hiliteManager.addToHiLiteHandler(hiLiteHdl);
    }

    private CounterRowIterator m_leftIt, m_rightIt;

    private int m_leftRows, m_outputRows;

    private ExecutionContext m_exec;

    /** Upper bound for total row count, used for progress only. */
    private double m_max;

    /**
     * Hard reference to the first element in the current chunk, used to
     * disallow garbage collection.
     */
    private Helper m_firstMapHelper;

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataContainer dc = exec.createDataContainer(JoinedTable
                .createSpec(inData[0].getDataTableSpec(), inData[1]
                        .getDataTableSpec(), m_method, m_suffix));

        DataTable leftTable = inData[0];
        DataTable rightTable = inData[1];
        // determine the list of columns in the right table that must be
        // in the output
        if (JoinedTable.METHOD_FILTER.equals(m_method)) {
            DataTableSpec leftTableSpec = leftTable.getDataTableSpec();
            DataTableSpec rightTableSpec = rightTable.getDataTableSpec();
            LinkedHashSet<String> leftHash = new LinkedHashSet<String>();
            for (DataColumnSpec c : leftTableSpec) {
                leftHash.add(c.getName());
            }
            LinkedHashSet<String> rightHash = new LinkedHashSet<String>();
            for (DataColumnSpec c : rightTableSpec) {
                rightHash.add(c.getName());
            }
            rightHash.removeAll(leftHash);
            String[] survivors =
                rightHash.toArray(new String[rightHash.size()]);
            if (survivors.length < rightTableSpec.getNumColumns()) {
                rightTable = new FilterColumnTable(rightTable, survivors);
            }
        }

        final BitSet rightRows = new BitSet(inData[1].getRowCount());
        final LinkedHashMap<RowKey, SoftReference<Helper>> map =
            new LinkedHashMap<RowKey, SoftReference<Helper>>(1024);
        m_leftRows = 0;
        m_outputRows = 0;
        m_leftIt = null;
        m_rightIt = null;
        m_firstMapHelper = null;
        m_exec = exec;

        if (m_ignoreMissingRows) {
            m_max = Math.min(inData[0].getRowCount(), inData[1].getRowCount());
        } else {
            m_max = Math.max(inData[0].getRowCount(), inData[1].getRowCount());
        }

        while (true) {
            if (!readLeftChunk(leftTable, map)) {
                if (!m_ignoreMissingRows) {
                    processRemainingRightRows(dc, leftTable, rightTable,
                            rightRows);
                }
                break;
            }

            if ((m_rightIt == null) || (!m_rightIt.hasNext())
                    || (rightRows.nextClearBit(0) <= m_rightIt.getIndex())) {
                m_rightIt = new CounterRowIterator(rightTable.iterator());
            }
            while (m_rightIt.hasNext() && (map.size() > 0)) {
                m_exec.checkCanceled();
                DataRow rightRow = m_rightIt.next();
                SoftReference<Helper> sr = map.get(rightRow.getKey());
                if (sr != null) {
                    Helper h = sr.get();
                    if (h == null) {
                        map.remove(rightRow.getKey());
                    } else {
                        h.m_rightRow = rightRow;
                        h.m_rightIndex = m_rightIt.getIndex();
                        if (h.m_leftIndex == m_leftRows) {
                            // m_firstMapHelper = h;
                            assert h.m_predecessor == null
                                || !map.containsKey(
                                        h.m_predecessor.m_leftRow.getKey());
                            h.m_predecessor = null;
                            DataRow joinedRow = new JoinedRow(h.m_leftRow,
                                    h.m_rightRow);
                            dc.addRowToTable(joinedRow);

                            map.remove(rightRow.getKey());
                            rightRows.set(m_rightIt.getIndex());
                            m_leftRows++;
                            m_outputRows++;
                            printProgress(rightRow.getKey());
                        }
                    }
                }

            }

            processRemainingLeftRowsInMap(dc, rightTable, map, rightRows);
            if (!m_ignoreMissingRows) {
                if (rightRows.cardinality() == inData[1].getRowCount()) {
                    processRemainingLeftRowsInTable(dc, leftTable, rightTable);
                }
            } else {
                m_leftRows += map.size();
                map.clear();
                if (rightRows.cardinality() == inData[1].getRowCount()) {
                    break;
                }
            }
        }

        m_leftIt = null;
        m_rightIt = null;
        m_exec = null;
        m_firstMapHelper = null;
        dc.close();
        return new BufferedDataTable[]{dc.getTable()};
    }

    private void processRemainingRightRows(final DataContainer dc,
            final DataTable leftTable, final DataTable rightTable,
            final BitSet rightRows) throws CanceledExecutionException {
        if (m_rightIt.getIndex() >= rightRows.nextClearBit(0)) {
            m_rightIt = new CounterRowIterator(rightTable.iterator());
        }

        while (m_rightIt.hasNext()) {
            final DataRow rightRow = m_rightIt.next();
            m_exec.checkCanceled();
            if (!rightRows.get(m_rightIt.getIndex())) {
                dc.addRowToTable(new JoinedRow(new DefaultRow(
                        rightRow.getKey(),
                        JoinedTable.createMissingCells(
                                leftTable.getDataTableSpec())), rightRow));
                rightRows.set(m_rightIt.getIndex());
                m_outputRows++;
                printProgress(rightRow.getKey());
            }
        }
    }

    private void processRemainingLeftRowsInTable(final DataContainer dc,
            final DataTable leftTable, final DataTable rightTable)
            throws CanceledExecutionException {

        if (m_leftIt.getIndex() > m_leftRows) {
            m_leftIt = new CounterRowIterator(leftTable.iterator());
            while (m_leftIt.getIndex() < m_leftRows) {
                m_leftIt.next();
                m_exec.checkCanceled();
            }
        }
        assert m_leftIt.getIndex() == (m_leftRows - 1);

        while (m_leftIt.hasNext()) {
            DataRow leftRow = m_leftIt.next();
            m_exec.checkCanceled();
            dc.addRowToTable(new JoinedRow(leftRow, new DefaultRow(leftRow
                    .getKey(), JoinedTable.createMissingCells(rightTable
                    .getDataTableSpec()))));
            m_leftRows++;
            m_outputRows++;
            printProgress(leftRow.getKey());
        }
    }

    private void processRemainingLeftRowsInMap(final DataContainer dc,
            final DataTable rightTable,
            final Map<RowKey, SoftReference<Helper>> map,
            final BitSet rightRows) {
        // all rows from the right table have been written

        // first read the remaining entries in the map
        for (SoftReference<Helper> sr : map.values()) {
            Helper h = sr.get();
            if (h == null) {
                break;
            }

            if (h.m_rightRow != null) {
                dc.addRowToTable(new JoinedRow(h.m_leftRow, h.m_rightRow));
                rightRows.set(h.m_rightIndex);
            } else if (!m_ignoreMissingRows) {
                dc.addRowToTable(new JoinedRow(h.m_leftRow, new DefaultRow(
                        h.m_leftRow.getKey(), JoinedTable
                                .createMissingCells(rightTable
                                        .getDataTableSpec()))));
            }
            m_outputRows++;
            printProgress(h.m_leftRow.getKey());
            m_leftRows++;
        }
        map.clear();
    }

    private void printProgress(final RowKey key) {
        m_exec.setProgress(m_outputRows / m_max, "Joined row " + m_outputRows
                + " (\"" + key + "\")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hiliteManager.getFromHiLiteHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec s1 = inSpecs[0];
        DataTableSpec s2 = inSpecs[1];
        DataTableSpec out = null;
        try {
            out = JoinedTable.createSpec(s1, s2, m_method, m_suffix);
        } catch (IllegalArgumentException iae) {
            // must be an error with duplicate column names, or unknown method.
            throw new InvalidSettingsException(iae.getMessage());
        }
        return new DataTableSpec[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_DUPLICATE_METHOD, m_method);
        settings.addString(CFG_SUFFIX, m_suffix);
        settings.addBoolean(CFG_IGNORE_MISSING_ROWS, m_ignoreMissingRows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        load(settings, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        load(settings, true);
    }

    /* Helper to load/validate settings. */
    private void load(final NodeSettingsRO settings, final boolean load)
            throws InvalidSettingsException {
        String method = settings.getString(CFG_DUPLICATE_METHOD);
        String suffix;
        boolean ignoreMissingRows = settings
                .getBoolean(CFG_IGNORE_MISSING_ROWS);
        if (JoinedTable.METHOD_FAIL.equals(method)) {
            // the parameter is not necessary here, allow it to fail
            suffix = settings.getString(CFG_SUFFIX, m_suffix);
        } else if (JoinedTable.METHOD_FILTER.equals(method)) {
            // the parameter is not necessary here, allow it to fail
            suffix = settings.getString(CFG_SUFFIX, m_suffix);
        } else if (JoinedTable.METHOD_APPEND_SUFFIX.equals(method)) {
            // must be in there.
            suffix = settings.getString(CFG_SUFFIX);
            if (suffix == null || suffix.equals("")) {
                throw new InvalidSettingsException("Suffix \"" + suffix
                        + "\" is not allowed.");
            }
        } else {
            throw new InvalidSettingsException("Unknown method: " + method);
        }
        if (load) {
            m_method = method;
            m_suffix = suffix;
            m_ignoreMissingRows = ignoreMissingRows;
        }
    }

    /**
     * Removes all registered <code>HiLiteHandlers</code> and adds only the
     * ones at the connected inports.
     *
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_leftIt = null;
        m_rightIt = null;
        m_exec = null;
        m_firstMapHelper = null;

        m_hiliteManager.removeAllToHiliteHandlers();
        for (int i = 0; i < getNrInPorts(); i++) {
            HiLiteHandler hdl = getInHiLiteHandler(i);
            m_hiliteManager.addToHiLiteHandler(hdl);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    private boolean readLeftChunk(final DataTable leftTable,
            final Map<RowKey, SoftReference<Helper>> map)
            throws CanceledExecutionException {
        m_firstMapHelper = null;

        if ((m_leftIt == null) || (m_leftIt.getIndex() > m_leftRows)) {
            m_leftIt = new CounterRowIterator(leftTable.iterator());

            for (int i = 0; i < m_leftRows; i++) {
                if (i % 25 == 0) {
                    m_exec.setMessage("Reading row " + i + " from left table");
                }
                m_leftIt.next();
                m_exec.checkCanceled();
            }
        }
        if (!m_leftIt.hasNext()) {
            return false;
        }

        System.gc();
        System.gc();
        int rowCount = m_leftRows;
        SoftReference<Helper> lastHelper = null;
        final Runtime r = Runtime.getRuntime();
        double used = 0;
        do {
            DataRow row = m_leftIt.next();
            m_exec.checkCanceled();
            if (rowCount++ % 25 == 0) {
                m_exec.setMessage("Reading row " + rowCount
                        + " from left table");
            }
            Helper h = new Helper(m_leftIt.getIndex(),
                    (lastHelper == null) ? null : lastHelper.get(), row);
            if (m_firstMapHelper == null) {
                m_firstMapHelper = h;
            }
            lastHelper = new SoftReference<Helper>(h);
            map.put(row.getKey(), lastHelper);
            used = (r.totalMemory() - r.freeMemory()) / (double)r.maxMemory();
        } while (m_leftIt.hasNext() && (used < 0.6));
        LOGGER.debug("Used = " + used + ", maxMem = " + r.maxMemory()
                + ", totalMem = " + r.totalMemory() + ", freeMem = "
                + r.freeMemory() + ", read " + map.size() + " rows");
        return true;
    }

    private class CounterRowIterator extends RowIterator {
        private int m_index = -1;

        private final RowIterator m_it;

        /**
         * Create new counting row iterator.
         * @param it Iterator to count rows.
         */
        CounterRowIterator(final RowIterator it) {
            m_it = it;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_it.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            DataRow r = m_it.next();
            m_index++;
            return r;
        }

        /**
         * @return Current position of the iterator.
         */
        int getIndex() {
            return m_index;
        }
    }

    private static final class Helper {

        private final int m_leftIndex;

        private Helper m_predecessor;

        private final DataRow m_leftRow;

        private DataRow m_rightRow;

        private int m_rightIndex;

        private Helper(final int leftIndex, final Helper predecessor,
                final DataRow leftRow) {
            this.m_leftIndex = leftIndex;
            this.m_predecessor = predecessor;
            this.m_leftRow = leftRow;
        }

//        @Override
//        protected void finalize() throws Throwable {
//             System.out.println("Finalizing helper for left index " +
//             leftIndex);
//        }
    }
}
