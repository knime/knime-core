/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Aug 12, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.domain;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.NominalValue;
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
    /** Config identifier for columns for which min and max values 
     * must be determined. */
    static final String CFG_MAX_POSS_VALUES = "max_poss_values";
    
    private String[] m_possValCols;
    private String[] m_minMaxCols;
    private int m_maxPossValues;

    /** Constructor, inits one input, one output. */
    public DomainNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        if (m_possValCols.length == 0 && m_minMaxCols.length == 0) {
            return inData;
        }
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
                    if (m_maxPossValues >= 0 
                            && possVals[i].size() > m_maxPossValues) {
                        possVals[i] = null;
                    }
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
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
        DataTableSpec oldSpec = inSpecs[0];
        if (m_minMaxCols == null) {
            setWarningMessage(
                    "No configuration available, using auto-configuration.");
            m_minMaxCols = getAllCols(BoundedValue.class, oldSpec);
            m_possValCols = getAllCols(NominalValue.class, oldSpec);
            m_maxPossValues = 60;
        }
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
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_possValCols != null) {
            settings.addStringArray(CFG_POSSVAL_COLS, m_possValCols);
            settings.addStringArray(CFG_MIN_MAX_COLS, m_minMaxCols);
            settings.addInt(CFG_MAX_POSS_VALUES, m_maxPossValues);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getStringArray(CFG_POSSVAL_COLS);
        settings.getStringArray(CFG_MIN_MAX_COLS);
        settings.getInt(CFG_MAX_POSS_VALUES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_possValCols = settings.getStringArray(CFG_POSSVAL_COLS);
        m_minMaxCols = settings.getStringArray(CFG_MIN_MAX_COLS);
        m_maxPossValues = settings.getInt(CFG_MAX_POSS_VALUES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
    
    /** Finds all columns in a spec whose type is compatible to cl.
     * @param cl The value to be compatible to.
     * @param spec The spec to query.
     * @return The identified columns.
     */
    static String[] getAllCols(
            final Class<? extends DataValue> cl, final DataTableSpec spec) {
        ArrayList<String> result = new ArrayList<String>();
        for (DataColumnSpec c : spec) {
            if (c.getType().isCompatible(cl)) {
                result.add(c.getName());
            }
        }
        return result.toArray(new String[result.size()]);
    }

}

