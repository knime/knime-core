/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 23, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.predict;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.knime.base.node.mine.regression.linear.LinearRegressionParams;
import org.knime.base.node.mine.regression.linear.view.LinRegDataProvider;
import org.knime.base.node.util.DataArray;
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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Node model for the linear regression predictor.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegPredictorNodeModel extends NodeModel implements
        LinRegDataProvider {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(LinRegPredictorNodeModel.class);

    private LinearRegressionParams m_parameters;

    /** Initialization with 1 data input, 1 model input and 1 data output. */
    public LinRegPredictorNodeModel() {
        super(1, 1, 1, 0);
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
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec spec = inData[0].getDataTableSpec();
        ColumnRearranger c = createRearranger(spec);
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], c,
                exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * @see NodeModel#loadModelContent(int, ModelContentRO)
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        if (index != 0) {
            throw new IndexOutOfBoundsException(
                    "Invalid model input: " + index);
        }
        LinearRegressionParams param;
        if (predParams == null) {
            param = null;
        } else {
            param = LinearRegressionParams.loadParams(predParams);
        }
        m_parameters = param;
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * @see NodeModel#configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        Map<String, Double> map = m_parameters == null ? null : m_parameters
                .getMap();
        if (map == null || map.isEmpty()) {
            throw new InvalidSettingsException("No parameters set.");
        }
        if (map.size() == 1) {
            throw new InvalidSettingsException("Not enough parameters.");
        }
        boolean isFirst = true;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            // first one is the offset value
            if (isFirst) {
                isFirst = false;
                if (Double.isNaN(entry.getValue())) {
                    throw new InvalidSettingsException(
                            "Invalid offset value: NaN");
                }
                continue;
            }
            String name = entry.getKey();
            DataColumnSpec colSpec = inSpecs[0].getColumnSpec(name);
            if (colSpec == null) {
                throw new InvalidSettingsException("No such column: " + name);
            }
            if (!colSpec.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Not a double type: "
                        + colSpec.getType().toString());
            }
            double val = entry.getValue();
            if (Double.isNaN(val)) {
                throw new InvalidSettingsException(
                        "Invalid value for parameter " + name + ": " + val);
            }
        }
        // settings seem to be ok, create out spec
        ColumnRearranger c = createRearranger(inSpecs[0]);
        DataTableSpec outSpec = c.createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * Creates the name of the new appended column, possibly the original name
     * of the response (or target) column used in the learner. If that column
     * exists in the data, it is concatenated with "(prediction)"
     * 
     * @param spec the input spec
     * @return the output spec with one(!) column. The two tables will be
     *         joined.
     */
    private DataColumnSpec createAppendSpec(final DataTableSpec spec) {
        String name = null;
        Map<String, Double> map = m_parameters == null ? null : m_parameters
                .getMap();
        if (map != null && !map.isEmpty()) {
            name = map.keySet().iterator().next();
        }
        if (name == null) {
            // node is not executable if map is null or empty, though
            name = "prediction";
        } else if (spec.containsName(name)) {
            name = name.toString() + " (prediction)";
        }
        while (spec.containsName(name)) {
            name = name.toString() + "_";
        }
        DataColumnSpecCreator creator = new DataColumnSpecCreator(name,
                DoubleCell.TYPE);
        return creator.createSpec();
    }

    private ColumnRearranger createRearranger(final DataTableSpec inSpec) {
        DataColumnSpec appendSpec = createAppendSpec(inSpec);
        Map<String, Double> par = m_parameters.getMap();
        final double[] parameters = new double[par.size()];
        // -1 because the offset value is not contained in the data
        final int[] columns = new int[par.size() - 1];
        // find the indizes of the columns to include
        int count = 0;
        for (Iterator<Map.Entry<String, Double>> it = par.entrySet().iterator();
            it.hasNext(); count++) {
            Map.Entry<String, Double> entry = it.next();
            String colName = entry.getKey();
            parameters[count] = entry.getValue();
            if (count == 0) {
                continue;
            }
            int index = inSpec.findColumnIndex(colName);
            columns[count - 1] = index;
        }
        SingleCellFactory fac = new SingleCellFactory(appendSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                // sum of the product
                double sum = parameters[0]; // offset value
                boolean containsMissing = false;
                for (int i = 0; i < columns.length && !containsMissing; i++) {
                    DataCell val = row.getCell(columns[i]);
                    if (val.isMissing()) {
                        containsMissing = true; // will be skipped
                    } else {
                        double v = ((DoubleValue)val).getDoubleValue();
                        sum += v * parameters[i + 1];
                    }
                }
                DataCell appendCell;
                if (containsMissing) {
                    LOGGER.debug("Row \"" + row.getKey().getId()
                            + "\" contains missing values, skipping.");
                    appendCell = DataType.getMissingCell();
                } else {
                    appendCell = new DoubleCell(sum);
                }
                return appendCell;
            }
        };
        ColumnRearranger c = new ColumnRearranger(inSpec);
        c.append(fac);
        return c;
    }

    /**
     * @see LinRegDataProvider#getParams()
     */
    public LinearRegressionParams getParams() {
        return null;
    }

    /**
     * @see LinRegDataProvider#getRowContainer()
     */
    public DataArray getRowContainer() {
        return null;
    }

    /**
     * @see NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * @see NodeModel#saveInternals(File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
}
