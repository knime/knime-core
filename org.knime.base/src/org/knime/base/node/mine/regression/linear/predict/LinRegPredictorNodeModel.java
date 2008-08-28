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
 *   Feb 23, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.predict;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.regression.RegressionPortObject;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * Node model for the linear regression predictor.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegPredictorNodeModel extends GenericNodeModel {

    /** Initialization with 1 data input, 1 model input and 1 data output. */
    public LinRegPredictorNodeModel() {
        super(new PortType[]{RegressionPortObject.TYPE, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        RegressionPortObject regModel = (RegressionPortObject)inData[0];
        BufferedDataTable data = (BufferedDataTable)inData[1]; 
        DataTableSpec spec = data.getDataTableSpec();
        ColumnRearranger c = createRearranger(
                spec, regModel.getSpec(), regModel);
        BufferedDataTable out = 
            exec.createColumnRearrangeTable(data, c, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

     /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec regModelSpec = (DataTableSpec)inSpecs[0];
        DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
        if (dataSpec == null || regModelSpec == null) {
            throw new InvalidSettingsException(
                    "No input specification available");
        }
        ColumnRearranger rearranger = 
            createRearranger(dataSpec, regModelSpec, null);
        DataTableSpec outSpec = rearranger.createSpec();
        return new DataTableSpec[]{outSpec};
    }

    private ColumnRearranger createRearranger(final DataTableSpec inSpec, 
            final DataTableSpec regModelSpec, 
            final RegressionPortObject regModel) 
        throws InvalidSettingsException {
        // exclude last (response column)
        final int[] varsIndices = new int[regModelSpec.getNumColumns() - 1];
        for (int i = 0; i < varsIndices.length; i++) {
            DataColumnSpec regressor = regModelSpec.getColumnSpec(i);
            String name = regressor.getName();
            int index = inSpec.findColumnIndex(name);
            if (index < 0) {
                throw new InvalidSettingsException("Missing column for " 
                        + "regressor variable : \"" + name + "\"");
            }
            DataColumnSpec col = inSpec.getColumnSpec(index);
            if (!col.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Incompatible type of " 
                        + "column \"" + name + "\": " + col.getType());
            }
            varsIndices[i] = index;
        }
        // try to use some smart naming scheme for the append column
        String oldName = 
            regModelSpec.getColumnSpec(varsIndices.length).getName();
        if (inSpec.containsName(oldName) 
                && !oldName.toLowerCase().endsWith("(prediction)")) {
            oldName = oldName + " (prediction)";
        }
        String newColName = DataTableSpec.getUniqueColumnName(inSpec, oldName); 
        DataColumnSpec newCol = 
            new DataColumnSpecCreator(newColName, DoubleCell.TYPE).createSpec();
        SingleCellFactory fac = new SingleCellFactory(newCol) {
            @Override
            public DataCell getCell(final DataRow row) {
                FilterColumnRow reduced = new FilterColumnRow(row, varsIndices);
                return regModel.predict(reduced);
            }
        };
        ColumnRearranger c = new ColumnRearranger(inSpec);
        c.append(fac);
        return c;
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
}
