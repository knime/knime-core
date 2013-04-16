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
 * @see Discretization2ApplyNodeFactory
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
