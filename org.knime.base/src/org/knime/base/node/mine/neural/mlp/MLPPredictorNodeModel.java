/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   28.10.2005 (cebron): created
 */
package org.knime.base.node.mine.neural.mlp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.data.neural.MultiLayerPerceptron;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DoubleBarRenderer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * The Neural Net Predictor takes as input a
 * {@link org.knime.core.data.DataTable} with the data that has to be classified
 * and the trained Neural Network.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class MLPPredictorNodeModel extends NodeModel {
    /*
     * The trained neural network to use for prediction.
     */
    private MultiLayerPerceptron m_mlp;

    /*
     * The number of possible values in the class column.
     */
    private int m_nrPossValues;

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
        super(new PortType[]{PMMLNeuralNetworkPortObject.TYPE,
              BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * The additional columns are created based on the model which is loaded in
     * the execute-method. Therefore, new DataTableSpecs are not available until
     * execute has been called.
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PMMLPortObjectSpec modelspec = (PMMLPortObjectSpec)inSpecs[0];
        DataTableSpec testSpec = (DataTableSpec)inSpecs[1];
        /*
         * Check consistency between model and inputs, find columns to work on.
         */
        for (String incol : modelspec.getLearningFields()) {
            if (!testSpec.containsName(incol)) {
                throw new InvalidSettingsException("Could not find " + incol
                        + " in inputspec");
            }
        }
        m_columns = getLearningColumnIndices(testSpec, modelspec);
        MLPClassificationFactory mymlp;
        DataColumnSpec targetCol = modelspec.getTargetCols().iterator().next();
        // Regression
        if (targetCol.getType().isCompatible(DoubleValue.class)) {
            mymlp = new MLPClassificationFactory(true, m_columns, targetCol);
        } else {
            // Classification
            mymlp = new MLPClassificationFactory(false, m_columns, targetCol);
        }
        ColumnRearranger colre = new ColumnRearranger(testSpec);
        colre.append(mymlp);
        return new DataTableSpec[]{colre.createSpec()};
    }

    private int[] getLearningColumnIndices(final DataTableSpec testspec,
            final PMMLPortObjectSpec portspec) throws InvalidSettingsException {
        List<String> learnfields = portspec.getLearningFields();
        int[] indices = new int[learnfields.size()];
        int counter = 0;
        for (String s : learnfields) {
            int pos = testspec.findColumnIndex(s);
            if (pos < 0) {
                throw new InvalidSettingsException("Could not find column " + s
                        + " in input data.");
            }
            indices[counter] = pos;
            counter++;
        }
        Arrays.sort(indices);
        return indices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable testdata = (BufferedDataTable)inData[1];
        PMMLNeuralNetworkPortObject pmmlMLP =
                (PMMLNeuralNetworkPortObject)inData[0];
        m_columns =
                getLearningColumnIndices(testdata.getDataTableSpec(), pmmlMLP
                        .getSpec());
        DataColumnSpec targetCol =
                pmmlMLP.getSpec().getTargetCols().iterator().next();
        m_mlp = pmmlMLP.getMLP();
        MLPClassificationFactory mymlp;
        /*
         * Regression
         */
        if (m_mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {

            mymlp = new MLPClassificationFactory(true, m_columns, targetCol);
        } else if (m_mlp.getMode()
                == MultiLayerPerceptron.CLASSIFICATION_MODE) {
            /*
             * Classification
             */
            mymlp = new MLPClassificationFactory(false, m_columns, targetCol);
        } else {
            throw new Exception("Unsupported Mode: " + m_mlp.getMode());
        }

        ColumnRearranger colre =
                new ColumnRearranger(testdata.getDataTableSpec());
        colre.append(mymlp);
        BufferedDataTable bdt =
                exec.createColumnRearrangeTable(testdata, colre, exec);
        return new BufferedDataTable[]{bdt};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // does nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_mlp = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // does nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // does nothing.
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

        private DataColumnSpec m_classcolspec;

        /**
         * A new AppendedColumnFactory that uses a MultiLayerPerceptron to
         * classify new instances.
         *
         * @param regression indicates whether a regression should take place.
         * @param columns to work on.
         * @param classcolspec DataColumnSpec with target column.
         */
        MLPClassificationFactory(final boolean regression, final int[] columns,
                final DataColumnSpec classcolspec) {
            m_regression = regression;
            m_faccolumns = columns;
            m_classcolspec = classcolspec;
        }

        /**
         * {@inheritDoc}
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
         * {@inheritDoc}
         */
        public DataColumnSpec[] getColumnSpecs() {
            String name = "PredClass";
            DataType type;
            if (m_regression) {
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
            if (m_regression) {
                allappSpec = new DataColumnSpec[1];
                allappSpec[0] = appendSpec;

            } else {
                /*
                 * Classification
                 */
                Set<DataCell> classvalues =
                        m_classcolspec.getDomain().getValues();
                if (classvalues == null) {
                    // no possible values information available.
                    classvalues = new HashSet<DataCell>();
                }
                m_nrPossValues = classvalues.size();
                allappSpec = new DataColumnSpec[classvalues.size() + 1];
                allappSpec[0] = appendSpec;
                int index = 1;
                DataColumnDomainCreator domaincreator =
                        new DataColumnDomainCreator();
                domaincreator.setLowerBound(new DoubleCell(0));
                domaincreator.setUpperBound(new DoubleCell(1));
                DataColumnDomain domain = domaincreator.createDomain();
                int counter = 0;
                for (DataCell nomValue : classvalues) {
                    name =
                            ((StringValue)nomValue).getStringValue()
                                    + " (Neuron " + counter + ")";
                    type = DoubleCell.TYPE;
                    DataColumnSpecCreator colspeccreator =
                            new DataColumnSpecCreator(name, type);
                    colspeccreator
                            .setProperties(new DataColumnProperties(
                                    Collections.singletonMap(
                              DataValueRenderer.PROPERTY_PREFERRED_RENDERER,
                              DoubleBarRenderer.DESCRIPTION)));
                    colspeccreator.setDomain(domain);
                    allappSpec[index] = colspeccreator.createSpec();
                    index++;
                    counter++;
                }
            }
            return allappSpec;
        }

        /**
         * {@inheritDoc}
         */
        public void setProgress(final int curRowNr, final int rowCount,
                final RowKey lastKey, final ExecutionMonitor exec) {
            exec.setProgress((double)curRowNr / (double)rowCount, "Prediction");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing.
    }
}
