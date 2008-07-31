/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 23, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import org.knime.base.node.util.DataArray;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaNumberHelper extends SotaHelper {
    /**
     * Creates an instance of SotaNumberHelper with given DataArray with the
     * trainingdata.
     * 
     * @param rowContainer the DataArray with the training data
     * @param exec the <code>ExecutionMonitor</code> to set.
     */
    public SotaNumberHelper(final DataArray rowContainer, 
            final ExecutionMonitor exec) {
        super(rowContainer, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int initializeDimension() {
        int dimension = 0;
        //
        // / Count all number cells in rows of row container
        //
        for (int i = 0; i < this.getRowContainer().getDataTableSpec()
                .getNumColumns(); i++) {

            DataType type = this.getRowContainer().getDataTableSpec()
                    .getColumnSpec(i).getType();

            if (SotaUtil.isNumberType(type)) {
                dimension++;
            }
        }
        this.setDimension(dimension);

        return this.getDimension();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SotaTreeCell initializeTree() throws CanceledExecutionException {
        //
        // / Calculate the mean values of each column of the row container
        //
        double[] means = new double[this.getDimension()];
        for (int i = 0; i < means.length; i++) {
            means[i] = 0;
        }

        int col = 0;
        for (int i = 0; i < this.getRowContainer().size(); i++) {

            col = 0;
            for (int j = 0; j < this.getRowContainer().getDataTableSpec()
                    .getNumColumns(); j++) {
                getExec().checkCanceled();
                
                DataType type = this.getRowContainer().getDataTableSpec()
                        .getColumnSpec(j).getType();

                if (SotaUtil.isNumberType(type)
                        && !SotaUtil.hasMissingValues(getRowContainer().getRow(
                                i))) {
                    means[col] += ((DoubleValue)this.getRowContainer()
                            .getRow(i).getCell(j)).getDoubleValue();
                    col++;
                }
            }
        }

        for (int i = 0; i < means.length; i++) {
            means[i] = means[i] / this.getRowContainer().size();
        }

        //
        // / initialize root and children node/cells
        //
        SotaTreeCell root = SotaTreeCellFactory.initCellFactory(
                this.getDimension()).createCell(means, 0);

        root.split();

        return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void adjustSotaCell(final SotaTreeCell cell, final DataRow row,
            final double learningrate, final String cellClass) {
        int col = 0;

        if (SotaUtil.hasMissingValues(row)) {
            return;
        }

        cell.addTreeCellClass(cellClass);
        
        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type)) {
                if (col < cell.getData().length) {
                    cell.getData()[col].adjustCell(
                            row.getCell(i), learningrate);
                    col++;
                }
            }
        }
    }
}
