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

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class MDSPivotDataNodeModel extends MDSPivotNodeModel {
    
    /**
     * New MDS data node model with the given number of in- and outputs.
     * @param nrDataIns number of data ins
     * @param nrDataOuts number of data outs
     */
    public MDSPivotDataNodeModel(final int nrDataIns, final int nrDataOuts) {
        super(nrDataIns, nrDataOuts);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (!inSpecs[0].equalStructure(inSpecs[1])) {
            throw new InvalidSettingsException("Input specs don't have equal"
                    + " structure.");
        }
        DataTableSpec[] resultSpec = super.configure(
                new DataTableSpec[]{inSpecs[0]});
        return new DataTableSpec[]{resultSpec[0], resultSpec[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final int nrPivotRows = inData[0].getRowCount();
        final int nrDataRows = inData[1].getRowCount();
        ColumnRearranger colRe = createColumnRearranger(
                inData[0].getDataTableSpec());
        colRe.keepOnly(m_numericFilter.getIncludeList().toArray(new String[0]));
        BufferedDataTable data = exec.createColumnRearrangeTable(
                inData[0], colRe, exec.createSilentSubProgress(0.1));
        final int colCnt = data.getDataTableSpec().getNumColumns();
        double[][] dataArray = new double[colCnt][nrPivotRows + nrDataRows];
        int rowIdx = 0;
        for (DataRow row : data) {
            exec.checkCanceled();
            for (int i = 0; i < colCnt; i++) {
                dataArray[i][rowIdx] = 
                    ((DoubleValue) row.getCell(i)).getDoubleValue();
            }
            rowIdx++;
        }
        colRe = createColumnRearranger(inData[1].getDataTableSpec());
        colRe.keepOnly(m_numericFilter.getIncludeList().toArray(new String[0]));
        data = exec.createColumnRearrangeTable(inData[1], colRe, 
                exec.createSilentSubProgress(0.1));
        for (DataRow row : data) {
            exec.checkCanceled();
            for (int i = 0; i < colCnt; i++) {
                dataArray[i][rowIdx] = 
                    ((DoubleValue) row.getCell(i)).getDoubleValue();
            }
            rowIdx++;
        }
        final int lowDim = m_lowerDim.getIntValue();
        double[][] result = new double[lowDim][nrPivotRows + nrDataRows];
        super.executeMDS(dataArray, nrPivotRows, result, exec);
        DataTableSpec spec = createSpec(lowDim);
        BufferedDataContainer buf1 = exec.createDataContainer(spec);
        BufferedDataContainer buf2 = exec.createDataContainer(spec);
        rowIdx = 0;
        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            double[] doubles = new double[result.length];
            for (int j = 0; j < result.length; j++) {
                doubles[j] = result[j][rowIdx];
            }
            DataRow newRow = new DefaultRow(row.getKey(), doubles);
            buf1.addRowToTable(newRow);
            rowIdx++;
        }
        buf1.close();
        for (DataRow row : inData[1]) {
            exec.checkCanceled();
            double[] doubles = new double[result.length];
            for (int j = 0; j < result.length; j++) {
                doubles[j] = result[j][rowIdx];
            }
            DataRow newRow = new DefaultRow(row.getKey(), doubles);
            buf2.addRowToTable(newRow);
            rowIdx++;
        }
        buf2.close();
        return new BufferedDataTable[]{buf1.getTable(), buf2.getTable()};
    }

}

