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
package org.knime.base.node.mine.regression.predict;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.mine.regression.PMMLRegressionPortObject;
import org.knime.base.node.mine.regression.PMMLRegressionPortObject.NumericPredictor;
import org.knime.base.node.mine.regression.PMMLRegressionPortObject.RegressionTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * Node model for the linear regression predictor.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class RegressionPredictorNodeModel extends GenericNodeModel {

    /** Initialization with 1 data input, 1 model input and 1 data output. */
    public RegressionPredictorNodeModel() {
        super(new PortType[]{PMMLRegressionPortObject.TYPE,
                BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
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
        PMMLRegressionPortObject regModel = (PMMLRegressionPortObject)inData[0];
        BufferedDataTable data = (BufferedDataTable)inData[1];
        DataTableSpec spec = data.getDataTableSpec();
        ColumnRearranger c =
                createRearranger(spec, regModel.getSpec(), regModel);
        BufferedDataTable out = exec.createColumnRearrangeTable(data, c, exec);
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
        PMMLPortObjectSpec regModelSpec = (PMMLPortObjectSpec)inSpecs[0];
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
            final PMMLPortObjectSpec regModelSpec,
            final PMMLRegressionPortObject regModel)
            throws InvalidSettingsException {
        if (regModelSpec == null) {
            throw new InvalidSettingsException("No input");
        }

        // exclude last (response column)
        String targetCol = "Response";
        for (String s : regModelSpec.getTargetFields()) {
            targetCol = s;
            break;
        }

        final List<String> learnFields;
        if (regModel != null) {
            RegressionTable regTable = regModel.getRegressionTable();
            learnFields = new ArrayList<String>();
            for (NumericPredictor p : regTable.getVariables()) {
                learnFields.add(p.getName());
            }
        } else {
            learnFields =
                    new ArrayList<String>(regModelSpec.getLearningFields());
        }

        final int[] colIndices = new int[learnFields.size()];
        int k = 0;
        for (String learnCol : learnFields) {
            int index = inSpec.findColumnIndex(learnCol);
            if (index < 0) {
                throw new InvalidSettingsException("Missing column for "
                        + "regressor variable : \"" + learnCol + "\"");
            }
            DataColumnSpec regressor = inSpec.getColumnSpec(index);
            String name = regressor.getName();
            DataColumnSpec col = inSpec.getColumnSpec(index);
            if (!col.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Incompatible type of "
                        + "column \"" + name + "\": " + col.getType());
            }

            colIndices[k++] = index;
        }
        // try to use some smart naming scheme for the append column
        String oldName = targetCol;
        if (inSpec.containsName(oldName)
                && !oldName.toLowerCase().endsWith("(prediction)")) {
            oldName = oldName + " (prediction)";
        }
        String newColName = DataTableSpec.getUniqueColumnName(inSpec, oldName);
        DataColumnSpec newCol =
                new DataColumnSpecCreator(newColName, DoubleCell.TYPE)
                        .createSpec();

        SingleCellFactory fac = new SingleCellFactory(newCol) {
            @Override
            public DataCell getCell(final DataRow row) {
                RegressionTable t = regModel.getRegressionTable();
                int j = 0;
                double result = t.getIntercept();
                for (NumericPredictor p : t.getVariables()) {
                    DataCell c = row.getCell(colIndices[j++]);
                    if (c.isMissing()) {
                        return DataType.getMissingCell();
                    }
                    double v = ((DoubleValue)c).getDoubleValue();
                    if (p.getExponent() != 1) {
                        v = Math.pow(v, p.getExponent());
                    }
                    result += p.getCoefficient() * v;
                }
                return new DoubleCell(result);
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
