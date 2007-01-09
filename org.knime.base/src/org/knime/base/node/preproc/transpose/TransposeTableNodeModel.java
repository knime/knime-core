/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 */
package org.knime.base.node.preproc.transpose;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Model of the transpose node which swaps rows and columns. In addition, a new 
 * <code>HiLiteHandler</code> is provided at the output.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class TransposeTableNodeModel extends NodeModel {
    private HiLiteHandler m_outHiLite;

    /**
     * Creates a transpose model with one data in- and output.
     *
     */
    TransposeTableNodeModel() {
        super(1, 1);
        m_outHiLite = new DefaultHiLiteHandler();
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        // new number of columns = number of rows
        int cols = 0;
        // new column names
        final ArrayList<String> colNames = new ArrayList<String>();
        // new column types
        final ArrayList<DataType> colTypes = new ArrayList<DataType>();
        // over entire table
        for (RowIterator it = inData[0].iterator(); it.hasNext(); cols++) {
            DataRow row = it.next();
            exec.checkCanceled();
            exec.setProgress(0, "Computing new column type for row: "
                    + row.getKey().getId());
            // fallback type if no column is available
            DataType type = DataType.getType(DataCell.class);
            // and all cells
            for (int i = 0; i < row.getNumCells(); i++) {
                DataType newType = row.getCell(i).getType();
                if (type == null) {
                    type = newType;
                } else {
                    type = DataType.getCommonSuperType(type, newType);
                }
            }
            colNames.add(row.getKey().getId().toString());
            colTypes.add(type);
        }
        DataTableSpec spec = inData[0].getDataTableSpec();
        // new number of rows
        int rows = spec.getNumColumns();
        // create new specs
        final DataColumnSpec[] colSpecs = new DataColumnSpec[cols];
        for (int c = 0; c < cols; c++) {
            colSpecs[c] = new DataColumnSpecCreator(colNames.get(c), colTypes
                    .get(c)).createSpec();
            exec.checkCanceled();
        }
        BufferedDataContainer cont = exec
                .createDataContainer(new DataTableSpec(colSpecs));
        for (int r = 0; r < rows; r++) {
            String header = spec.getColumnSpec(r).getName();
            exec.setProgress((r + 1.0) / rows, "Column -> Row: " + header);
            DataCell[] data = new DataCell[cols];
            int c = 0;
            for (RowIterator it1 = inData[0].iterator(); it1.hasNext(); c++) {
                DataRow row = it1.next();
                data[c] = row.getCell(r);
                try {
                    exec.checkCanceled();
                } catch (CanceledExecutionException cee) {
                    cont.close();
                    throw cee;
                }
            }
            DataRow row = new DefaultRow(new StringCell(header), data);
            cont.addRowToTable(row);
        }
        exec.setProgress(1.0, "Finished, closing buffer...");
        cont.close();
        return new BufferedDataTable[]{cont.getTable()};

    }

    /**
     * @see NodeModel#getOutHiLiteHandler(int)
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        assert outIndex == 0;
        return m_outHiLite;
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_outHiLite.fireClearHiLiteEvent();
    }

    /**
     * @see NodeModel#configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[1];
    }
}
