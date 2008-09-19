/*
 * --------------------------------------------------------------------- *
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
 */
package org.knime.base.node.preproc.discretization.caim2.modelapply;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.discretization.caim2.DiscretizationModel;
import org.knime.base.node.preproc.discretization.caim2.modelcreator.CAIMDiscretizationNodeModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * This node takes a discretization model and applies it to the given input data
 * table.
 *
 * @author Christoph Sieb, University of Konstanz
 *
 * @see DiscretizationApplyNodeFactory
 */
public class DiscretizationApplyNodeModel extends NodeModel {

    /** index of the port receiving data. */
    static final int DATA_INPORT = 1;

    /** index of the port providing the model. */
    static final int MODEL_INPORT = 0;

    /** index of the port providing output data. */
    static final int DATA_OUTPORT = 0;

    /**
     * Inits a new discretization applier model with one data in-, one model in-
     * and one data output port.
     */
    public DiscretizationApplyNodeModel() {
        super(new PortType[]{DiscretizationModel.TYPE, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {

        DiscretizationModel discrModel =
                (DiscretizationModel)inData[MODEL_INPORT];

        // if an empty model was received, just return the input data
        if (discrModel.getSchemes().length == 0) {
            return new PortObject[]{inData[DATA_INPORT]};
        }

        // create an output table that replaces the included columns by
        // interval values from the model
        BufferedDataTable resultTable =
                CAIMDiscretizationNodeModel.createResultTable(exec,
                        (BufferedDataTable)inData[DATA_INPORT], discrModel);

        return new BufferedDataTable[]{resultTable};
    }

    /**
     * Resets all internal data.
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {

        // if no columns are defined to discretize, return the input spec
        DataTableSpec modelSpec = (DataTableSpec)inSpecs[MODEL_INPORT];
        if (modelSpec == null || modelSpec.getNumColumns() == 0) {
            return new PortObjectSpec[]{inSpecs[DATA_INPORT]};
        } else {
            // else replace for each included column the attribute type to
            // string
            DataTableSpec dataSpec = (DataTableSpec)inSpecs[DATA_INPORT];
            if (dataSpec == null) {
                return new DataTableSpec[]{null};
            }
            DataColumnSpec[] newColumnSpecs =
                    new DataColumnSpec[dataSpec.getNumColumns()];

            int counter = 0;
            for (DataColumnSpec origColSpec : dataSpec) {

                // if the column is included for discretizing, change the spec
                int modelColIdx =
                        modelSpec.findColumnIndex(origColSpec.getName());
                if (modelColIdx >= 0) {
                    // types of columns must be compatible
                    if (!modelSpec.getColumnSpec(modelColIdx).getType()
                            .isASuperTypeOf(origColSpec.getType())) {
                        throw new InvalidSettingsException("The type of the"
                                + " column used to create the model is not"
                                + " compatible to the input column type ("
                                + " column name = " + origColSpec.getName()
                                + ")");
                    }
                    // create a nominal string column spec
                    newColumnSpecs[counter] =
                            new DataColumnSpecCreator(origColSpec.getName(),
                                    StringCell.TYPE).createSpec();
                } else {
                    // add it as is
                    newColumnSpecs[counter] = origColSpec;
                }

                counter++;
            }

            DataTableSpec[] newSpecs = new DataTableSpec[1];
            newSpecs[0] = new DataTableSpec(newColumnSpecs);
            return newSpecs;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // do nothing here
    }
}
