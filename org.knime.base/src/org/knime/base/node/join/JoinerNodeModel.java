/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.join;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.base.data.join.JoinedRow;
import org.knime.base.data.join.JoinedTable;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.DefaultHiLiteManager;
import org.knime.core.node.property.hilite.HiLiteHandler;


/**
 * Joins two tables such that the first table appears on the left side
 * of the new table an the second one on the right side.
 * @see JoinedTable
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JoinerNodeModel extends NodeModel {

    /** NodeSettings: Method for duplicate treatment, JoinedTable.METHOD_... */
    static final String CFG_DUPLICATE_METHOD = "treatment_duplicate";

    /** NodeSettings: duplicate treatment: suffix text. */ 
    static final String CFG_SUFFIX = "suffix";
    
    /** Key for joining the table in memory. */
    static final String CFG_IGNORE_MISSING_ROWS = "missingRows";

    
    /** method how to go along with duplicates, see METHOD_*. */
    private String m_method;
    
    /** suffix to append (if any).*/
    private String m_suffix;
    
    private boolean m_ignoreMissingRows;
    
    private final DefaultHiLiteManager m_hiliteHandler;

    /** Creates new model, with 2 inports and 1 outport. */
    public JoinerNodeModel() {
        super(2, 1);
        m_method = JoinedTable.METHOD_FAIL;
        m_suffix = "_duplicate";
        m_hiliteHandler = new DefaultHiLiteManager();
    }
    
    /**
     * Sets the In<code>HiLiteHandler</code> and registers them at the 
     * <code>HiLiteManager</code>.
     * 
     * @see org.knime.core.node.NodeModel#setInHiLiteHandler(int, 
     * org.knime.core.node.property.hilite.HiLiteHandler)
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex, 
            final HiLiteHandler hiLiteHdl) {
        super.setInHiLiteHandler(inIndex, hiLiteHdl);
        m_hiliteHandler.addHiLiteHandler(hiLiteHdl);
    }

    
    private CounterRowIterator m_leftIt, m_rightIt;
    private SoftReference<Helper> m_lastHelper;
    private int m_leftRows, m_outputRows;
    private ExecutionContext m_exec;
    private double m_max;
    /** Hard reference to the first element in the current chunk, 
     * used todisallow garbage collection.
     */
    private Helper m_firstMapHelper;
    
    /**
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(
            final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        BufferedDataContainer dc = exec.createDataContainer(
                JoinedTable.createSpec(inData[0].getDataTableSpec(), 
                        inData[1].getDataTableSpec(), m_method, m_suffix));

        final BitSet rightRows = new BitSet(inData[1].getRowCount());        
        final LinkedHashMap<RowKey, SoftReference<Helper>> map =
            new LinkedHashMap<RowKey, SoftReference<Helper>>(1024);
        m_leftRows = 0;
        m_outputRows = 0;
        m_lastHelper = null;
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
            if (!readLeftChunk(inData[0], map)) {
                if  (!m_ignoreMissingRows) {
                    processRemainingRightRows(dc, inData[0], inData[1],
                            rightRows);
                }
                break;
            }

            m_rightIt = new CounterRowIterator(inData[1].iterator());
            while (m_rightIt.hasNext()) {
                m_exec.checkCanceled();
                DataRow rightRow = m_rightIt.next();
                SoftReference<Helper> sr = map.get(rightRow.getKey());
                if (sr != null) {
                    Helper h = sr.get();
                    if (h == null) {
                        map.remove(rightRow.getKey());
                    } else {
                        h.rightRow = rightRow;
                        h.rightIndex = m_rightIt.getIndex();
                        if (h.leftIndex == m_leftRows) {
                            DataRow joinedRow =
                                new JoinedRow(h.leftRow, h.rightRow);
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
            
            processRemainingLeftRowsInMap(dc, inData[1], map, rightRows);
            if (!m_ignoreMissingRows) {
               if (rightRows.cardinality() == inData[1].getRowCount()) {
                   processRemainingLeftRowsInTable(dc, inData[0], inData[1]);
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
        m_lastHelper = null;
        m_exec = null;
        m_firstMapHelper = null;
        dc.close();
        return new BufferedDataTable[] {dc.getTable()};
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
                dc.addRowToTable(new JoinedRow(new DefaultRow(rightRow.getKey(),
                        JoinedTable.createMissingCells(
                                leftTable.getDataTableSpec())),
                                rightRow));
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
            dc.addRowToTable(new JoinedRow(leftRow,
                    new DefaultRow(leftRow.getKey(),
                    JoinedTable.createMissingCells(
                            rightTable.getDataTableSpec()))));
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
            
            if (h.rightRow != null) {
                dc.addRowToTable(new JoinedRow(h.leftRow, h.rightRow));
                rightRows.set(h.rightIndex);                
            } else if (!m_ignoreMissingRows) {
                dc.addRowToTable(new JoinedRow(h.leftRow,
                        new DefaultRow(h.leftRow.getKey(),
                        JoinedTable.createMissingCells(
                                rightTable.getDataTableSpec()))));
            }
            m_outputRows++;
            printProgress(h.leftRow.getKey());
            m_leftRows++;
        }
        map.clear();
    }
    
    private void printProgress(final RowKey key) {
        m_exec.setProgress(m_outputRows / m_max,
                "Joined row " + m_outputRows + " (\"" + key + "\")");
    }

    
    /**
     * @see org.knime.core.node.NodeModel#getOutHiLiteHandler(int)
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        assert outIndex == 0;
        return m_hiliteHandler;
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(
            final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec s1 = inSpecs[0];
        DataTableSpec s2 = inSpecs[1];
        DataTableSpec out = null;
        try {
            out = JoinedTable.createSpec(s1, s2, m_method, m_suffix);
        } catch (IllegalArgumentException iae) {
            // must be an error with duplicate column names, or unknown method.
            throw new InvalidSettingsException(iae.getMessage());
        }
        return new DataTableSpec[] {out};
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_DUPLICATE_METHOD, m_method);
        settings.addString(CFG_SUFFIX, m_suffix);
        settings.addBoolean(CFG_IGNORE_MISSING_ROWS, m_ignoreMissingRows);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        load(settings, false);
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
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
        boolean ignoreMissingRows =
            settings.getBoolean(CFG_IGNORE_MISSING_ROWS);
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
     * Removes all registered <code>HiLiteHandlers</code> and adds only the ones
     * at the connected inports.
     * 
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_hiliteHandler.removeAllHiLiteHandlers();
        for (int i = 0; i < getNrDataIns(); i++) {
            HiLiteHandler hdl = getInHiLiteHandler(i);
            m_hiliteHandler.addHiLiteHandler(hdl);
        }
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) throws IOException, 
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
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
                m_leftIt.next();
                m_exec.checkCanceled();
            }
        }
        if (!m_leftIt.hasNext()) { return false; }
        
        final Runtime r = Runtime.getRuntime();
        long diff = 0;
        do {
            DataRow row = m_leftIt.next();
            m_exec.checkCanceled();
            Helper h = new Helper(m_leftIt.getIndex(),
                    (m_lastHelper == null) ? null : m_lastHelper.get(), row); 
            if (m_firstMapHelper == null) {
                m_firstMapHelper = h;
            }
            m_lastHelper = new SoftReference<Helper>(h);
            map.put(row.getKey(), m_lastHelper);
            diff = Math.max(r.maxMemory() - r.totalMemory(), r.freeMemory()); 
        } while (m_leftIt.hasNext() && (diff > 4 * 1024 * 1024));
        return true;
    }

    
    
    private class CounterRowIterator extends RowIterator {
        private int m_index = -1;
        private final RowIterator m_it;
        
        CounterRowIterator(final RowIterator it) {
            m_it = it;
        }
        
        /**
         * @see org.knime.core.data.RowIterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return m_it.hasNext();
        }

        /**
         * @see org.knime.core.data.RowIterator#next()
         */
        @Override
        public DataRow next() {
            DataRow r = m_it.next();
            m_index++;
            return r;
        }
        
        int getIndex() {
            return m_index;
        }
    }

    
    private static class Helper {
        final int leftIndex;
        final Helper predecessor;
        final DataRow leftRow;
        DataRow rightRow;
        int rightIndex;
        
        public Helper(final int leftIndex, final Helper predecessor,
                final DataRow leftRow) {
            this.leftIndex = leftIndex;
            this.predecessor = predecessor;
            this.leftRow = leftRow;
        }
    }
}
