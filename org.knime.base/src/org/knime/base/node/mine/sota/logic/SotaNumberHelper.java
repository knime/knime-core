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
