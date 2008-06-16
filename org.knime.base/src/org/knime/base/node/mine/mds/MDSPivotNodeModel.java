/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   21.12.2006 (gabriel): created
 */
package org.knime.base.node.mine.mds;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
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
     * Checkbox used to append remaining non-MDS columns.
     */
    protected final SettingsModelBoolean m_appendColumns =
        createAppendColumns();

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
        return new DataTableSpec[]{createSpec(
                m_lowerDim.getIntValue(), inSpecs[0])};
    }
    
    /**
     * Create output spec with number of lower dimensions.
     * @param lowDim number of lower dimensions
     * @param spec original data table spec from the input
     * @return new data table spec
     */
    protected final DataTableSpec createSpec(final int lowDim, 
            final DataTableSpec spec) {
        List<String> incl = m_numericFilter.getIncludeList();
        DataColumnSpec[] cspecs;
        if (m_appendColumns.getBooleanValue()) {
            cspecs = new DataColumnSpec[
                spec.getNumColumns() - incl.size() + lowDim];
            int idx = 0;
            for (int i = 0; i < spec.getNumColumns(); i++) {
                if (!incl.contains(spec.getColumnSpec(i).getName())) {
                    cspecs[(idx++) + lowDim] = spec.getColumnSpec(i);
                }
            }
        } else {
            cspecs = new DataColumnSpec[lowDim];
        }
        for (int i = 0; i < lowDim; i++) {
            String name = new String("X" + (i + 1));
            DataType type = DoubleCell.TYPE;
            cspecs[i] = new DataColumnSpecCreator(name, type).createSpec();
        }
        return new DataTableSpec(cspecs);
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
        final DataTableSpec ospec = inData[0].getDataTableSpec();
        ColumnRearranger colRe = createColumnRearranger(ospec);
        colRe.keepOnly(m_numericFilter.getIncludeList().toArray(new String[0]));
        data = exec.createColumnRearrangeTable(
                data, colRe, exec.createSilentSubProgress(0.0));
        final int nrRows = data.getRowCount();
        final int colCnt = data.getDataTableSpec().getNumColumns();
        double[][] dataArray = new double[colCnt][nrRows];
        int rowCnt = 0;
        for (DataRow row : data) {
            exec.checkCanceled();
            for (int i = 0; i < colCnt; i++) {
                DataCell cell = row.getCell(i);
                if (cell.isMissing()) {
                    break;
                } else {
                    dataArray[i][rowCnt] = 
                        ((DoubleValue) cell).getDoubleValue();
                }
            }
            rowCnt++;
        }
        int nrPivots = rowCnt;
        if (m_usePivots.getBooleanValue()) {
            nrPivots = Math.min(nrPivots, m_nrPivots.getIntValue());
        }
        final int lowDim = m_lowerDim.getIntValue();
        double[][] result = new double[lowDim][nrRows];
        if (rowCnt > 0) {
            // initialize with random nonzero stuff
            Random random = new Random(0L);
            for (int i = 0; i < result.length; i++) {
                for (int j = 0; j < result[0].length; j++) {
                    result[i][j] = random.nextDouble();
                }
            }
            executeMDS(dataArray, nrPivots, result, exec);
        }
        DataTableSpec spec = createSpec(lowDim, ospec);
        BufferedDataContainer buf = exec.createDataContainer(spec);
        rowCnt = 0;
        List<String> incl = m_numericFilter.getIncludeList();
        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            DataCell[] doubles;
            boolean isMissing = false;
            if (m_appendColumns.getBooleanValue()) {
                doubles = new DataCell[ospec.getNumColumns() 
                                       - incl.size() + lowDim];
                int idx = 0;
                for (int j = 0; j < ospec.getNumColumns(); j++) {
                    if (!incl.contains(ospec.getColumnSpec(j).getName())) {
                        DataCell cell = row.getCell(j);
                        doubles[(idx++) + lowDim] = cell;
                    } else {
                        isMissing |= row.getCell(j).isMissing();
                    }
                }
            } else {
                doubles = new DoubleCell[result.length];
            }
            for (int j = 0; j < result.length; j++) {
                if (isMissing) {
                    doubles[j] = DataType.getMissingCell();
                } else {
                    doubles[j] = new DoubleCell(result[j][rowCnt]);
                }
            }
            DataRow newRow = new DefaultRow(row.getKey(), doubles);
            buf.addRowToTable(newRow);
            rowCnt++;
        }
        buf.close();
        return new BufferedDataTable[]{buf.getTable()};
    }
    
    /**
     * Runs the MDS pivot algorithm on the dataArray using the specified 
     * number of elements as pivot elements. The result is contained in the
     * variable result after finishing the MDS process.
     * @param dataArray data to perform MDS on
     * @param nrPivots number of elements from input data used as pivots
     * @param result resulting data in lower dimensions
     * @param exec monitor to report progress and to cancel process
     * @throws CanceledExecutionException if canceled
     */
    public static synchronized void executeMDS(final double[][] dataArray,
            final int nrPivots, final double[][] result, 
            final ExecutionContext exec) 
            throws CanceledExecutionException {
        final Thread t = new Thread() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run() {
                double[][] dist = ClassicalMDS.distanceMatrix(dataArray);
                ClassicalMDS.squareEntries(dist);
                ClassicalMDS.doubleCenter(dist);
                ClassicalMDS.multiply(dist, -0.5);
                ClassicalMDS.fullmds(dist, result);
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
                CanceledExecutionException cee = 
                    new CanceledExecutionException(
                            "MDS process has been canceled!");
                cee.initCause(ie);
                throw cee; 
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
        try {
            m_appendColumns.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_appendColumns.setBooleanValue(
                    // ensures the same default value as specified
                    createAppendColumns().getBooleanValue());
        }
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
        m_appendColumns.saveSettingsTo(settings);
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
        try {
            m_appendColumns.validateSettings(settings);
        } catch (InvalidSettingsException ise) {
            // ignore
        }
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
    
    /**
     * @return settings model to append non-MDS columns
     */
    static final SettingsModelBoolean createAppendColumns() {
        return new SettingsModelBoolean("append_columns", false);
    }
}

