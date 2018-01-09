/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
package org.knime.base.node.mine.neural.mlp2;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.data.neural.MultiLayerPerceptron;
import org.knime.base.node.mine.util.PredictorHelper;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.pmml.PMMLModelType;
import org.w3c.dom.Node;

/**
 * The Neural Net Predictor takes as input a
 * {@link org.knime.core.data.DataTable} with the data that has to be classified
 * and the trained Neural Network.
 * <p>Despite being public no official API.
 * @author Nicolas Cebron, University of Konstanz
 * @since 2.10 (since 2.9 really)
 */
public final class MLPPredictorNodeModel extends NodeModel {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(MLPPredictorNodeModel.class);
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

    /** Prediction column name. */
    private final SettingsModelString m_predictionColumn = PredictorHelper.getInstance().createPredictionColumn();

    /** Suffix for the probability columns. */
    private final SettingsModelString m_suffix = PredictorHelper.getInstance().createSuffix();

    private final SettingsModelBoolean m_overridePrediction = PredictorHelper.getInstance().createChangePrediction();

    private final SettingsModelBoolean m_appendProbs = createAppendProbs();

    private static final String CFGKEY_APPEND_PROBS = "append probabilities";
    private static final boolean DEFAULT_APPEND_PROBS = true;
    /**
     * @return The "append probabilities" node model.
     */
    static SettingsModelBoolean createAppendProbs() {
        return new SettingsModelBoolean(CFGKEY_APPEND_PROBS, DEFAULT_APPEND_PROBS);
    }

    /**
     * The MLPPredictorNodeModel takes as input a model and the test data. The
     * output is the classified test data.
     *
     */
    public MLPPredictorNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE,
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

        List<DataColumnSpec> targetCols = modelspec.getTargetCols();
        if (targetCols.isEmpty()) {
            throw new InvalidSettingsException("The PMML model"
                    + " does not specify a target column for the prediction.");
        }
        DataColumnSpec targetCol = targetCols.iterator().next();

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
        final String prediction =
            PredictorHelper.getInstance().checkedComputePredictionColumnName(m_predictionColumn.getStringValue(),
                m_overridePrediction.getBooleanValue(), targetCol.getName());
        MLPClassificationFactory mymlp;
        // Regression
        if (targetCol.getType().isCompatible(DoubleValue.class)) {
            mymlp = new MLPClassificationFactory(true, m_columns, targetCol, prediction, m_appendProbs.getBooleanValue(), m_suffix.getStringValue());
        } else {
            // Classification
            mymlp = new MLPClassificationFactory(false, m_columns, targetCol, prediction, m_appendProbs.getBooleanValue(), m_suffix.getStringValue());
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
        return indices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataTable testdata = (BufferedDataTable)inData[1];
        PMMLPortObject pmmlPort = (PMMLPortObject)inData[0];
        ColumnRearranger colre = createColumnRearranger(pmmlPort, testdata.getDataTableSpec());
        BufferedDataTable bdt = exec.createColumnRearrangeTable(testdata, colre, exec);
        return new BufferedDataTable[]{bdt};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
                PMMLPortObject pmmlPortObject = (PMMLPortObject) ((PortObjectInput) inputs[0]).getPortObject();
                ColumnRearranger colre = createColumnRearranger(pmmlPortObject, (DataTableSpec) inSpecs[1]);
                StreamableFunction func = colre.createStreamableFunction(1, 0);
                func.runFinal(inputs, outputs, exec);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    private ColumnRearranger createColumnRearranger(final PMMLPortObject pmmlPortObject, final DataTableSpec inSpec) throws Exception {
        List<Node> models = pmmlPortObject.getPMMLValue().getModels(PMMLModelType.NeuralNetwork);
        if (models.isEmpty()) {
            String msg = "Neural network evaluation failed: " + "No neural network model found.";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        PMMLNeuralNetworkTranslator trans = new PMMLNeuralNetworkTranslator();
        pmmlPortObject.initializeModelTranslator(trans);
        m_mlp = trans.getMLP();

        m_columns = getLearningColumnIndices(inSpec, pmmlPortObject.getSpec());
        DataColumnSpec targetCol = pmmlPortObject.getSpec().getTargetCols().iterator().next();
        final String predictionColumnName = PredictorHelper.getInstance().computePredictionColumnName(
            m_predictionColumn.getStringValue(), m_overridePrediction.getBooleanValue(), targetCol.getName());
        MLPClassificationFactory mymlp;
        /*
         * Regression
         */
        if (m_mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {

            mymlp = new MLPClassificationFactory(true, m_columns, targetCol, predictionColumnName,
                m_appendProbs.getBooleanValue(), m_suffix.getStringValue());
        } else if (m_mlp.getMode() == MultiLayerPerceptron.CLASSIFICATION_MODE) {
            /*
             * Classification
             */
            mymlp = new MLPClassificationFactory(false, m_columns, targetCol, predictionColumnName,
                m_appendProbs.getBooleanValue(), m_suffix.getStringValue());
        } else {
            throw new Exception("Unsupported Mode: " + m_mlp.getMode());
        }

        ColumnRearranger colre = new ColumnRearranger(inSpec);
        colre.append(mymlp);
        return colre;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_predictionColumn.loadSettingsFrom(settings);
        m_overridePrediction.loadSettingsFrom(settings);
        m_appendProbs.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
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
        m_predictionColumn.saveSettingsTo(settings);
        m_overridePrediction.saveSettingsTo(settings);
        m_appendProbs.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_predictionColumn.validateSettings(settings);
        m_overridePrediction.validateSettings(settings);
        m_appendProbs.validateSettings(settings);
        m_suffix.validateSettings(settings);
    }

    /**
     * This class generates the appended column with the classification from the
     * WekaNodeModel.
     *
     * @author Nicolas Cebron, University of Konstanz
     */
    private class MLPClassificationFactory implements CellFactory {
        private final String m_predictionColumnName;
        private final String m_probabilitySuffices;

        /*
         * Flag whether regression is done or not.
         */
        private final boolean m_regression;

        /*
         * The columns to work on.
         */
        private final int[] m_faccolumns;

        private final DataColumnSpec m_classcolspec;
        private final boolean m_addProbs;

        /**
         * A new AppendedColumnFactory that uses a MultiLayerPerceptron to
         * classify new instances.
         *
         * @param regression indicates whether a regression should take place.
         * @param columns to work on.
         * @param classcolspec DataColumnSpec with target column.
         */
        MLPClassificationFactory(final boolean regression, final int[] columns,
                final DataColumnSpec classcolspec, final String predictionColumn, final boolean addProbs, final String suffix) {
            m_regression = regression;
            m_faccolumns = columns;
            m_classcolspec = classcolspec;
            m_predictionColumnName = predictionColumn;
            m_addProbs = addProbs;
            m_probabilitySuffices = suffix;
        }

        /**
         * {@inheritDoc}
         */
        @Override
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
                append = new DataCell[1 + (m_addProbs ? m_nrPossValues : 0)];
                String output = m_mlp.getClassOutput(inputs);
                if (m_addProbs) {
                    double[] outputs = m_mlp.output(inputs);
                    for (int i = 0; i < append.length - 1; i++) {
                        append[i] = new DoubleCell(outputs[i]);
                    }
                }
                append[append.length - 1] = new StringCell(output);
            }
            return append;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataColumnSpec[] getColumnSpecs() {
            DataType type;
            if (m_regression) {
                type = DoubleCell.TYPE;
            } else {
                type = StringCell.TYPE;
            }
            DataColumnSpec appendSpec =
                    new DataColumnSpecCreator(m_predictionColumnName, type).createSpec();
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
                allappSpec = new DataColumnSpec[(m_addProbs?classvalues.size() : 0) + 1];
                if (m_addProbs) {
                    int index = 0;
                    DataColumnDomainCreator domaincreator =
                            new DataColumnDomainCreator();
                    domaincreator.setLowerBound(new DoubleCell(0));
                    domaincreator.setUpperBound(new DoubleCell(1));
                    DataColumnDomain domain = domaincreator.createDomain();
                    final PredictorHelper ph = PredictorHelper.getInstance();
                    for (DataCell nomValue : classvalues) {
                        final String name = ph.probabilityColumnName(m_classcolspec.getName(),
                            ((StringValue)nomValue).getStringValue(), m_probabilitySuffices);
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
                    }
                }
                allappSpec[allappSpec.length - 1] = appendSpec;
            }
            return allappSpec;
        }

        /**
         * {@inheritDoc}
         */
        @Override
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
