/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   21.12.2006 (gabriel): created
 */
package org.knime.base.node.mds;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class MDSPivotNodeModel extends NodeModel {
    
    /**
     * Lower dimensionality settings model.
     */
    protected final SettingsModelIntegerBounded m_lowerDim =
        createLowerDimension();
    
    /**
     * Filter settings model panel.
     */
    protected final SettingsModelFilterString m_numericFilter =
        createColumnFilter();
    
    /**
     * Number of pivot elements.
     */
    protected final SettingsModelIntegerBounded m_nrPivots =
        createPivotElements();
    
    /**
     * Checkbox to use pivot elements.
     */
    protected final SettingsModelBoolean m_usePivots = createPivotCheckbox();

    /**
     * Create new MDS pivot model with the given number of in- and outputs.
     * @param nrDataIns number of data ins
     * @param nrDataOuts number of data outs
     */
    public MDSPivotNodeModel(final int nrDataIns, final int nrDataOuts) {
        super(nrDataIns, nrDataOuts);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        List<String> numericColumns = m_numericFilter.getIncludeList();
        if (numericColumns.size() <= m_lowerDim.getIntValue()) {
            throw new InvalidSettingsException("Number of columns is less or "
                    + "equal the dimension to map onto.");
        }
        for (String column : numericColumns) {
            if (inSpecs[0].containsName(column)) {
                if (!inSpecs[0].getColumnSpec(column).getType().isCompatible(
                        DoubleValue.class)) {
                    throw new InvalidSettingsException("Column \"" + column
                            + "\" is not compatible with type double.");
                }
            } else {
                throw new InvalidSettingsException("Column \"" + column 
                        + "\" not available in input data.");
            }
        }
        return new DataTableSpec[]{createSpec(m_lowerDim.getIntValue())};
    }
    
    /**
     * Create output spec with number of lower dimensions.
     * @param lowDim number of lower dimensions
     * @return new data table spec
     */
    protected final DataTableSpec createSpec(final int lowDim) {
        String[] names = new String[lowDim];
        DataType[] types = new DataType[lowDim];
        for (int i = 0; i < lowDim; i++) {
            names[i] = new String("X" + (i + 1));
            types[i] = DoubleCell.TYPE;
        }
        return new DataTableSpec(names, types);
    }
    
    /**
     * Create column re-arranger with the given input data table spec.
     * @param spec input spec
     * @return column re-arranger
     */
    protected final ColumnRearranger createColumnRearranger(
            final DataTableSpec spec) {
        List<String> numericColumns = m_numericFilter.getIncludeList();
        ColumnRearranger colRe = new ColumnRearranger(spec);
        colRe.keepOnly(numericColumns.toArray(new String[0]));
        return colRe;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable data = inData[0];
        ColumnRearranger colRe = createColumnRearranger(
                data.getDataTableSpec());
        colRe.keepOnly(m_numericFilter.getIncludeList().toArray(new String[0]));
        data = exec.createColumnRearrangeTable(
                data, colRe, exec.createSilentSubProgress(0.1));
        final int rowCnt = data.getRowCount();
        final int colCnt = data.getDataTableSpec().getNumColumns();
        double[][] dataArray = new double[colCnt][rowCnt];
        int rowIdx = 0;
        for (DataRow row : data) {
            exec.checkCanceled();
            for (int i = 0; i < colCnt; i++) {
                DataCell cell = row.getCell(i);
                if (cell.isMissing()) {
                    dataArray[i][rowIdx] = Double.NaN;
                } else {
                    dataArray[i][rowIdx] = 
                        ((DoubleValue) cell).getDoubleValue();
                }
            }
            rowIdx++;
        }
        int nrPivots = data.getRowCount();
        if (m_usePivots.getBooleanValue()) {
            nrPivots = Math.min(nrPivots, m_nrPivots.getIntValue());
        }
        final int lowDim = m_lowerDim.getIntValue();
        double[][] result = new double[lowDim][rowCnt];
        if (rowCnt > 0) {
            executeMDS(dataArray, nrPivots, result, exec);
        }
        DataTableSpec spec = createSpec(lowDim);
        BufferedDataContainer buf = exec.createDataContainer(spec);
        rowIdx = 0;
        for (DataRow row : data) {
            exec.checkCanceled();
            double[] doubles = new double[result.length];
            for (int j = 0; j < result.length; j++) {
                doubles[j] = result[j][rowIdx];
            }
            DataRow newRow = new DefaultRow(row.getKey(), doubles);
            buf.addRowToTable(newRow);
            rowIdx++;
        }
        buf.close();
        return new BufferedDataTable[]{buf.getTable()};

    }
    
    /**
     * Runs the MDS pivot algorithm on the input data.
     * @param dataArray data to perform MDS
     * @param nrPivots number of elements from input data used as pivots
     * @param result resulting data in lower dimensions
     * @param exec monitor to report progress and to cancel process
     * @throws CanceledExecutionException if canceled
     */
    protected synchronized void executeMDS(final double[][] dataArray,
            final int nrPivots, final double[][] result, 
            final ExecutionContext exec) 
            throws CanceledExecutionException {
        Thread t = new Thread() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run() {
                ClassicalMDS.randomize(result);
                ClassicalMDS.mds(dataArray, result, nrPivots);
            }
        };
        t.start();
        while (true) {
            if (t == null || !t.isAlive()) {
                return;
            }
            try { 
                exec.checkCanceled();
            } catch (CanceledExecutionException cee) {
                t.stop();
                throw cee;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                throw new CanceledExecutionException(
                        "Process has been canceled!");
            }
        }
            
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_lowerDim.loadSettingsFrom(settings);
        m_usePivots.loadSettingsFrom(settings);
        m_nrPivots.loadSettingsFrom(settings);
        m_numericFilter.loadSettingsFrom(settings);
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
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_lowerDim.saveSettingsTo(settings);
        m_usePivots.saveSettingsTo(settings);
        m_nrPivots.saveSettingsTo(settings);
        m_numericFilter.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_lowerDim.validateSettings(settings);
        m_usePivots.validateSettings(settings);
        m_nrPivots.validateSettings(settings);
        m_numericFilter.validateSettings(settings);
    }
    
    /**
     * @return settings model for dimensionality 
     */
    static final SettingsModelIntegerBounded createLowerDimension() {
        return new SettingsModelIntegerBounded(
                "lower_dimension", 2, 1, Integer.MAX_VALUE);
    }
    
    /**
     * @return settings model for number of pivot elements
     */
    static final SettingsModelIntegerBounded createPivotElements() {
        return new SettingsModelIntegerBounded(
                "nr_pivot_elements", 100, 1, Integer.MAX_VALUE);
    }
    
    /**
     * @return settings model for column filter panel
     */
    static final SettingsModelFilterString createColumnFilter() {
        return new SettingsModelFilterString("numeric_columms", 
                (List<String>) null, (List<String>) null);
    }

    /**
     * @return settings model for use of pivot elements
     */
    static final SettingsModelBoolean createPivotCheckbox() {
        return new SettingsModelBoolean("define_pivots", true);
    }
}

