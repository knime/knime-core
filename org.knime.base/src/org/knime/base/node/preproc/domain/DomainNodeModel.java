/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Aug 12, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.domain;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class DomainNodeModel extends NodeModel {
    
    /** Config identifier for columns for which possible values 
     * must be determined. */
    static final String CFG_POSSVAL_COLS = "possible_values_columns";
    /** Config identifier for columns for which min and max values 
     * must be determined. */
    static final String CFG_MIN_MAX_COLS = "min_max_columns";
    
    private String[] m_possValCols;
    private String[] m_minMaxCols;

    /** Constructor, inits one input, one output. */
    public DomainNodeModel() {
        super(1, 1);
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final DataTableSpec oldSpec = inData[0].getDataTableSpec();
        final int colCount = oldSpec.getNumColumns();
        HashSet<String> possValColsHash = 
            new HashSet<String>(Arrays.asList(m_possValCols));
        HashSet<String> minMaxColsHash = 
            new HashSet<String>(Arrays.asList(m_minMaxCols));
        @SuppressWarnings("unchecked")
        LinkedHashSet<DataCell>[] possVals = new LinkedHashSet[colCount];
        DataCell[] mins = new DataCell[colCount];
        DataCell[] maxs = new DataCell[colCount];
        @SuppressWarnings("unchecked")
        DataValueComparator[] comparators = new DataValueComparator[colCount]; 
        for (int i = 0; i < colCount; i++) {
            DataColumnSpec col = oldSpec.getColumnSpec(i);
            if (possValColsHash.contains(col.getName())) {
                possVals[i] = new LinkedHashSet<DataCell>();
            } else {
                possVals[i] = null;
            }
            if (minMaxColsHash.contains(col.getName())) {
                mins[i] = DataType.getMissingCell();
                maxs[i] = DataType.getMissingCell();
                comparators[i] = col.getType().getComparator();
            } else {
                mins[i] = null;
                maxs[i] = null;
                comparators[i] = null;
            }
        }
        
        int row = 0;
        final double rowCount = inData[0].getRowCount();
        for (RowIterator it = inData[0].iterator(); it.hasNext(); row++) {
            DataRow r = it.next();
            for (int i = 0; i < colCount; i++) {
                DataCell c = r.getCell(i);
                if (!c.isMissing() && possVals[i] != null) {
                    possVals[i].add(c);
                }
                if (!c.isMissing() && mins[i] != null) {
                    if (mins[i].isMissing()) {
                        mins[i] = c;
                        maxs[i] = c;
                        continue; // it was the first row with a valid value
                    }
                    if (comparators[i].compare(c, mins[i]) < 0) {
                        mins[i] = c;
                    }
                    if (comparators[i].compare(c, maxs[i]) > 0) {
                        maxs[i] = c;
                    }
                }
            }
            exec.checkCanceled();
            exec.setProgress(row / rowCount, "Processed row #" 
                    + (row + 1) + " (\"" + r.getKey() + "\")");
        }
        DataColumnSpec[] colSpec = new DataColumnSpec[colCount];
        for (int i = 0; i < colSpec.length; i++) {
            DataColumnSpec original = oldSpec.getColumnSpec(i);
            DataCell[] possVal = possVals[i] != null 
                ? possVals[i].toArray(new DataCell[0]) : null;
            DataCell min = mins[i] != null && !mins[i].isMissing()
                ? mins[i] : null;
            DataCell max = maxs[i] != null && !maxs[i].isMissing()
                ? maxs[i] : null;
            DataColumnDomainCreator domainCreator = 
                new DataColumnDomainCreator(possVal, min, max);
            DataColumnSpecCreator specCreator = 
                new DataColumnSpecCreator(original);
            specCreator.setDomain(domainCreator.createDomain());
            colSpec[i] = specCreator.createSpec();
        }
        DataTableSpec newSpec = new DataTableSpec(oldSpec.getName(), colSpec);
        BufferedDataTable o = exec.createSpecReplacerTable(inData[0], newSpec);
        return new BufferedDataTable[]{o};
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
        DataTableSpec oldSpec = inSpecs[0];
        int colCount = oldSpec.getNumColumns();
        DataColumnSpec[] colSpec = new DataColumnSpec[colCount];
        for (int i = 0; i < colSpec.length; i++) {
            DataColumnSpec original = oldSpec.getColumnSpec(i);
            DataColumnSpecCreator specCreator = 
                new DataColumnSpecCreator(original);
            specCreator.setDomain(null);
            colSpec[i] = specCreator.createSpec();
        }
        DataTableSpec newSpec = new DataTableSpec(oldSpec.getName(), colSpec);
        return new DataTableSpec[]{newSpec};
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_possValCols != null) {
            settings.addStringArray(CFG_POSSVAL_COLS, m_possValCols);
            settings.addStringArray(CFG_MIN_MAX_COLS, m_minMaxCols);
        }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getStringArray(CFG_POSSVAL_COLS);
        settings.getStringArray(CFG_MIN_MAX_COLS);
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_possValCols = settings.getStringArray(CFG_POSSVAL_COLS);
        m_minMaxCols = settings.getStringArray(CFG_MIN_MAX_COLS);
    }

    /**
     * @see NodeModel#loadInternals(java.io.File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
    
    /**
     * @see NodeModel#saveInternals(java.io.File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}

