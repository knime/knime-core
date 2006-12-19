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
 *   28.10.2005 (cebron): created
 */
package org.knime.base.node.mine.neural.mlp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.data.neural.MultiLayerPerceptron;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
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

/**
 * The Neural Net Predictor takes as input a
 * {@link org.knime.core.data.DataTable} with the data that has to be
 * classified and the trained Neural Network.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class MLPPredictorNodeModel extends NodeModel {
    /*
     * The trained neural network to use for prediction.
     */
    private MultiLayerPerceptron m_mlp;

    /*
     * Tehe number of possible values in the class column.
     */
    private int m_nrPossValues;

    private ModelContentRO m_predParams;

    /*
     * The columns to work on.
     */
    private int[] m_columns;

    /**
     * The MLPPredictorNodeModel takes as input a model and the test data. The
     * output is the classified test data.
     * 
     */
    public MLPPredictorNodeModel() {
        super(1, 1, 1, 0);
    }

    /**
     * The additional columns are created based on the model which is loaded in
     * the execute-method. Therefore, new DataTableSpecs are not available until
     * execute has been called.
     * 
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_predParams != null) {
            /*
             * Check consistency between model and inputs, find columns to work
             * on.
             */
            m_mlp = MultiLayerPerceptron.loadPredictorParams(m_predParams);
            HashMap<String, Integer> inputmap = m_mlp.getInputMapping();
            Set<String> inputcols = inputmap.keySet();
            m_columns = new int[inputcols.size()];
            for (String incol : inputcols) {
                if (!inSpecs[0].containsName(incol)) {
                    throw new InvalidSettingsException("Could not" + " find "
                            + incol.toString() + " in inputspec");
                } else {
                    m_columns[inputmap.get(incol)] =
                            inSpecs[0].findColumnIndex(incol);
                }
            }
            

            String name = "PredClass";
            DataType type;
            if (m_mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
                type = DoubleCell.TYPE;
            } else if (m_mlp.getMode() 
                    == MultiLayerPerceptron.CLASSIFICATION_MODE) {
                type = StringCell.TYPE;
            } else {
                throw new InvalidSettingsException("Unsupported mode in MLP: "
                        + m_mlp.getMode());
            }
            DataColumnSpec appendSpec =
                    new DataColumnSpecCreator(name, type).createSpec();
            DataColumnSpec[] allappSpec;

            /*
             * Regression
             */
            if (m_mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
                allappSpec = new DataColumnSpec[1];
                allappSpec[0] = appendSpec;
            } else {
                /*
                 * Classification
                 */
                m_nrPossValues = m_mlp.getArchitecture().getNrOutputNeurons();
                allappSpec = new DataColumnSpec[m_nrPossValues + 1];
                allappSpec[0] = appendSpec;
                for (int i = 1; i <= m_nrPossValues; i++) {
                    name = "Neuron" + (i - 1);
                    type = DoubleCell.TYPE;
                    allappSpec[i] =
                            new DataColumnSpecCreator(name, type).createSpec();
                }
            }
            DataTableSpec returnspec =
                    AppendedColumnTable.getTableSpec(inSpecs[0], allappSpec);
            return new DataTableSpec[]{returnspec};
        }
        throw new InvalidSettingsException("No model content "
                + "available for configuration");
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_mlp = MultiLayerPerceptron.loadPredictorParams(m_predParams);
        MLPClassificationFactory mymlp;
        /*
         * Regression
         */
        if (m_mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {

            mymlp = new MLPClassificationFactory(true, m_columns);
        } else if (m_mlp.getMode() 
                == MultiLayerPerceptron.CLASSIFICATION_MODE) {
            /*
             * Classification
             */
            mymlp = new MLPClassificationFactory(false, m_columns);
        } else {
            throw new Exception("Unsupported Mode: " + m_mlp.getMode());
        }

        ColumnRearranger colre =
                new ColumnRearranger(inData[0].getDataTableSpec());
        colre.append(mymlp);
        BufferedDataTable bdt =
                exec.createColumnRearrangeTable(inData[0], colre, exec);
        return new BufferedDataTable[]{bdt};
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_mlp = null;
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
     * Loads a MLP from a ModelContent object.
     * 
     * @see NodeModel#loadModelContent(int, ModelContentRO)
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        if (index == 0 && predParams != null) {
            m_predParams = predParams;
        }
    }

    /**
     * This class generates the appended column with the classification from the
     * WekaNodeModel.
     * 
     * @author Nicolas Cebron, University of Konstanz
     */
    private class MLPClassificationFactory implements CellFactory {

        /*
         * Flag whether regression is done or not.
         */
        private boolean m_regression;

        /*
         * The columns to work on.
         */
        private int[] m_faccolumns;

        /**
         * A new AppendedColumnFactory that uses a MultiLayerPerceptron to
         * classify new instaces.
         * 
         * @param regression indicates whether a regression should take place.
         * @param columns to work on.
         */
        MLPClassificationFactory(final boolean regression,
                final int[] columns) {
            m_regression = regression;
            m_faccolumns = columns;
        }

        /**
         * 
         * @see org.knime.base.data.append.column.AppendedCellFactory
         *      #getAppendedCell(org.knime.core.data.DataRow)
         */
        public DataCell[] getCells(final DataRow row) {
            double[] inputs = new double[m_faccolumns.length];
            for (int i = 0; i < m_faccolumns.length; i++) {
                if (!row.getCell(m_faccolumns[i]).isMissing()) {
                    DoubleValue dv = (DoubleValue)row.getCell(m_faccolumns[i]);
                    inputs[i] = dv.getDoubleValue();
                } else {
                    throw new IllegalArgumentException("Input DataTable"
                            + " should not contain missing values.");
                }
            }
            DataCell[] append;
            if (m_regression) {
                append = new DataCell[1];
                double[] outputs = m_mlp.output(inputs);
                append[0] = new DoubleCell(outputs[0]);
            } else {
                append = new DataCell[1 + m_nrPossValues];
                String output = m_mlp.getClassOutput(inputs);
                append[0] = new StringCell(output);
                double[] outputs = m_mlp.output(inputs);
                for (int i = 1; i < append.length; i++) {
                    append[i] = new DoubleCell(outputs[i - 1]);
                }
            }
            return append;
        }

        /**
         * 
         * @see org.knime.core.data.container.CellFactory#getColumnSpecs()
         */
        public DataColumnSpec[] getColumnSpecs() {
            String name = "PredClass";
            DataType type;
            if (m_mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
                type = DoubleCell.TYPE;
            } else {
                type = StringCell.TYPE;
            }
            DataColumnSpec appendSpec =
                    new DataColumnSpecCreator(name, type).createSpec();
            DataColumnSpec[] allappSpec;

            /*
             * Regression
             */
            if (m_mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
                allappSpec = new DataColumnSpec[1];
                allappSpec[0] = appendSpec;

            } else {
                /*
                 * Classification
                 */

                m_nrPossValues = m_mlp.getArchitecture().getNrOutputNeurons();
                allappSpec = new DataColumnSpec[m_nrPossValues + 1];
                allappSpec[0] = appendSpec;
                for (int i = 1; i <= m_nrPossValues; i++) {
                    name = "Neuron" + (i - 1);
                    type = DoubleCell.TYPE;
                    allappSpec[i] =
                            new DataColumnSpecCreator(name, type).createSpec();
                }
            }
            return allappSpec;
        }

        /**
         * @see org.knime.core.data.container.CellFactory# setProgress(int, int,
         *      org.knime.core.data.RowKey,
         *      org.knime.core.node.ExecutionMonitor)
         */
        public void setProgress(final int curRowNr, final int rowCount,
                final RowKey lastKey, final ExecutionMonitor exec) {
            exec.setProgress((double)curRowNr / (double)rowCount, "Prediction");
        }
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
}
