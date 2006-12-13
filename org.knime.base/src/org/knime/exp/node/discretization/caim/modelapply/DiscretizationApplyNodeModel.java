/*
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package org.knime.exp.node.discretization.caim.modelapply;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.exp.node.discretization.caim.DiscretizationModel;
import org.knime.exp.node.discretization.caim.modelcreator.CAIMDiscretizationNodeModel;

/**
 * This node takes a discretization model and applies it to the given input data
 * table.
 * 
 * @author Christoph Sieb, University of Konstanz
 * 
 * @see DiscretizationApplyNodeFactory
 */
class DiscretizationApplyNodeModel extends NodeModel {

    /**
     * Key to store the included columns settings. (Columns to perform the
     * discretization on)
     */
    public static final String INCLUDED_COLUMNS_KEY = "includedColumns";

    /** Index of input data port. */
    public static final int DATA_INPORT = 0;

    /** Index of data out port. */
    public static final int DATA_OUTPORT = 0;

    /** Index of model out port. */
    public static final int MODEL_INPORT = 0;

    /**
     * The learned discretization model for the included columns.
     */
    private DiscretizationModel m_discretizationModel;

    /**
     * Inits a new discretization applier model with one data in-, one model in-
     * and one data output port.
     */
    public DiscretizationApplyNodeModel() {
        super(1, 1, 1, 0);
        reset();
    }

    /**
     * Applies the discretization model to the input data table.
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     * @throws CanceledExecutionException If canceled.
     * @throws Exception if something else goes wrong.
     */
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {

        // if an empty model was received, just return the input data
        if (m_discretizationModel.getSchemes().length == 0) {
            return data;
        }

        // create an output table that replaces the included columns by
        // interval values from the model
        BufferedDataTable resultTable =
                CAIMDiscretizationNodeModel.createResultTable(exec,
                        data[DATA_INPORT], m_discretizationModel);

        return new BufferedDataTable[]{resultTable};
    }

    /**
     * Checks if the given column spec is included in the included columns list.
     * 
     * @param columnSpec the column spec to check
     * @return true, if the given column spec was discretized
     */
    private static boolean isIncluded(final DataColumnSpec columnSpec,
            final String[] includedColumnNames) {
        for (String inludedColumn : includedColumnNames) {
            if (inludedColumn.equals(columnSpec.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Resets all internal data.
     */
    protected void reset() {

        // nothing to do yet
    }

    /**
     * The number of the class columns must be > 0 and < number of input
     * columns. Also create the output table spec replacing the columns to
     * discretize to nominal String values.
     * 
     * @see NodeModel#configure(DataTableSpec[])
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        // if no columns are defined to discretize, return the input spec
        if (m_discretizationModel == null) {
            return inSpecs;
        } else {
            // else replace for each included column the attribute type to
            // string
            DataColumnSpec[] newColumnSpecs =
                    new DataColumnSpec[inSpecs[DATA_INPORT].getNumColumns()];

            String[] includedColumnNames =
                    m_discretizationModel.getIncludedColumnNames();
            int counter = 0;
            for (DataColumnSpec originalColumnSpec : inSpecs[DATA_INPORT]) {

                // if the column is included for discretizing, change the spec
                if (isIncluded(originalColumnSpec, includedColumnNames)) {
                    // creat a nominal string column spec
                    newColumnSpecs[counter] =
                            new DataColumnSpecCreator(originalColumnSpec
                                    .getName(), StringCell.TYPE).createSpec();
                } else {
                    // add it as is
                    newColumnSpecs[counter] = originalColumnSpec;
                }

                counter++;
            }

            DataTableSpec[] newSpecs = new DataTableSpec[1];
            newSpecs[0] = new DataTableSpec(newColumnSpecs);
            return newSpecs;
        }
    }

    /**
     * Loads the class column and the classification value in the model.
     * 
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    }

    /**
     * Saves the class column and the classification value in the settings.
     * 
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {

    }

    /**
     * This method validates the settings. That is:
     * <ul>
     * <li>The number of the class column must be an integer > 0</li>
     * <li>The positive value <code>DataCell</code> must not be null</li>
     * </ul>
     * 
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    }

    /**
     * Loads the discretization model from the model input port.
     * 
     * @see org.knime.core.node.NodeModel#loadModelContent(int,
     *      org.knime.core.node.ModelContentRO)
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {

        if (predParams == null) {
            return;
        }

        m_discretizationModel = new DiscretizationModel(predParams);
    }

    /**
     * @see org.knime.core.node. NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

        // do nothing
    }

    /**
     * @see org.knime.core.node. NodeModel#saveInternals(File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

        // do nothing here
    }
}
