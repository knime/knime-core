/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.bayes.naivebayes.predictor3;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.node.mine.bayes.naivebayes.datamodel2.NaiveBayesModel;
import org.knime.base.node.mine.bayes.naivebayes.datamodel2.PMMLNaiveBayesModelTranslator;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
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
 * This is the <code>NodeModel</code> implementation of the
 * "Naive Bayes Predictor" node.
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class NaiveBayesPredictorNodeModel2 extends NodeModel {

    // our logger instance
    private static final NodeLogger LOGGER = NodeLogger.getLogger(NaiveBayesPredictorNodeModel2.class);

    private static final int DATA_IN_PORT = 1;

    private static final int MODEL_IN_PORT = 0;

    private final SettingsModelBoolean m_inclProbVals = createProbabilityColumnModel();

    /**
     * @return include probability column model
     */
    static SettingsModelBoolean createProbabilityColumnModel() {
        return new SettingsModelBoolean("inclProbVals", false);
    }

    /**
     * @return the normalize model
     */
    static SettingsModelBoolean createNormalizeModel() {
        return new SettingsModelBoolean("normalize", true);
    }

    private final SettingsModelString m_predictionColumnName = PredictorHelper.getInstance().createPredictionColumn();
    private final SettingsModelBoolean m_overridePredicted = PredictorHelper.getInstance().createChangePrediction();
    private final SettingsModelString m_probabilitySuffix = PredictorHelper.getInstance().createSuffix();

    /**Constructor for class NaiveBayesPredictorNodeModel.
     */
    public NaiveBayesPredictorNodeModel2() {
//      we have one data in and out port and one model in port
        super(new PortType[] {PMMLPortObject.TYPE, BufferedDataTable.TYPE},
                new PortType[] {BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        LOGGER.debug("Entering execute(inData, exec) of class NaiveBayesPredictorNodeModel.");
//      check input data
        assert (inData != null && inData.length == 2 && inData[DATA_IN_PORT] != null
                && inData[MODEL_IN_PORT] != null);
        final PortObject dataObject = inData[DATA_IN_PORT];
        if (!(dataObject instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Invalid input data");
        }
        final BufferedDataTable data = (BufferedDataTable)dataObject;
        final PortObject modelObject = inData[MODEL_IN_PORT];
        if (!(modelObject instanceof PMMLPortObject)) {
            throw new IllegalArgumentException("Invalid input data");
        }
        final PMMLPortObject modelPort = (PMMLPortObject)modelObject;
        final Collection<Node> models = modelPort.getPMMLValue().getModels(PMMLModelType.NaiveBayesModel);
        if (models == null || models.isEmpty()) {
            throw new Exception("Node not properly configured. No Naive Bayes Model available.");
        }
        if (models.size() > 1) {
            throw new Exception("Node supports only one Naive Bayes Model at a time.");
        }
        exec.setMessage("Classifying rows...");
        ColumnRearranger rearranger = createColumnRearranger(modelPort, data.getDataTableSpec());
        final BufferedDataTable returnVal = exec.createColumnRearrangeTable(data, rearranger, exec);
        LOGGER.debug("Exiting execute(inData, exec) of class NaiveBayesPredictorNodeModel.");
        return new PortObject[] {returnVal};
    }

    /* Helper to create the column rearranger that does the actual work */
    private ColumnRearranger createColumnRearranger(final PMMLPortObject pmmlPortObj, final DataTableSpec inSpec){
        final PMMLNaiveBayesModelTranslator translator = new PMMLNaiveBayesModelTranslator();
        pmmlPortObj.initializeModelTranslator(translator);
        final NaiveBayesModel model = translator.getModel();
        PredictorHelper predictorHelper = PredictorHelper.getInstance();
        final String classColumnName = model.getClassColumnName();
        final String predictionColName = m_overridePredicted.getBooleanValue()
                ? m_predictionColumnName.getStringValue() : predictorHelper.computePredictionDefault(classColumnName);
        final NaiveBayesCellFactory appender = new NaiveBayesCellFactory(model, predictionColName,
            inSpec, m_inclProbVals.getBooleanValue(), m_probabilitySuffix.getStringValue());
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        rearranger.append(appender);
        return rearranger;
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
                PMMLPortObject pmmlPortObj = (PMMLPortObject)((PortObjectInput) inputs[MODEL_IN_PORT]).getPortObject();

                DataTableSpec inSpec = (DataTableSpec) inSpecs[DATA_IN_PORT];
                StreamableFunction fct = createColumnRearranger(pmmlPortObj, inSpec).createStreamableFunction(DATA_IN_PORT, 0);
                fct.runFinal(inputs, outputs, exec);
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        //check the input data
        assert (inSpecs != null && inSpecs.length == 2 && inSpecs[DATA_IN_PORT] != null
                && inSpecs[MODEL_IN_PORT] != null);
        final PortObjectSpec modelObject = inSpecs[MODEL_IN_PORT];
        if (!(modelObject instanceof PMMLPortObjectSpec)) {
            throw new IllegalArgumentException("Invalid input data");
        }
        final PMMLPortObjectSpec pmmlSpec = (PMMLPortObjectSpec) modelObject;
        final DataTableSpec trainingSpec = pmmlSpec.getDataTableSpec();
        if (trainingSpec == null) {
            throw new InvalidSettingsException("No model spec available");
        }
        final List<DataColumnSpec> targetCols = pmmlSpec.getTargetCols();
        if (targetCols.size() != 1) {
            throw new InvalidSettingsException("No valid class column found");
        }
        final DataColumnSpec classColumn = targetCols.get(0);
        final PortObjectSpec inSpec = inSpecs[DATA_IN_PORT];
        if (!(inSpec instanceof DataTableSpec)) {
            throw new IllegalArgumentException("TableSpec must not be null");
        }
        final DataTableSpec spec = (DataTableSpec)inSpec;
        //check the input data for columns with the wrong name or wrong type
        final List<String> unknownCols = check4UnknownCols(trainingSpec, spec);
        warningMessage("The following input columns are unknown and will be skipped: ", unknownCols);
        //check if the learned model contains columns which are not in the
        //input data
        final List<String> missingInputCols = check4MissingCols(trainingSpec, classColumn.getName(), spec);
        warningMessage("The following attributes are missing in the input data: ", missingInputCols);
        final PredictorHelper predictorHelper = PredictorHelper.getInstance();
        final DataColumnSpec resultColSpecs =
            NaiveBayesCellFactory.createResultColSpecs(predictorHelper.checkedComputePredictionColumnName(
                m_predictionColumnName.getStringValue(), m_overridePredicted.getBooleanValue(), classColumn.getName()),
                classColumn.getType(), spec, m_inclProbVals.getBooleanValue());
        if (resultColSpecs != null) {
            return new PortObjectSpec[] {AppendedColumnTable.getTableSpec(spec, resultColSpecs)};
        }
        return null;
    }

    private void warningMessage(final String message, final List<String> colNames) {
        if (!colNames.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            buf.append(message);
            for (int i = 0, length = colNames.size(); i < length; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                if (i == 4) {
                    setWarningMessage(buf.toString() + "... (see log file for details)");
                }
                buf.append(colNames.get(i));

            }
            if (colNames.size() < 4) {
                setWarningMessage(buf.toString());
            } else {
                LOGGER.info(buf.toString());
            }
        }
    }

    private List<String> check4MissingCols(final DataTableSpec trainingSpec,
            final String classCol, final DataTableSpec spec) {
        final List<String> missingInputCols = new ArrayList<>();
        for (final DataColumnSpec trainColSpec : trainingSpec) {
            if (!trainColSpec.getName().equals(classCol)) {
                //check only for none class value columns
                if (spec.getColumnSpec(trainColSpec.getName()) == null) {
                    missingInputCols.add(trainColSpec.getName());
                }
            }
        }
        return missingInputCols;
    }

    private List<String> check4UnknownCols(final DataTableSpec trainingSpec,
            final DataTableSpec spec) {
        if (spec == null) {
            throw new NullPointerException("TableSpec must not be null");
        }
        final List<String> unknownCols = new ArrayList<>();
        for (final DataColumnSpec colSpec : spec) {
            final DataColumnSpec trainColSpec = trainingSpec.getColumnSpec(colSpec.getName());
            if (trainColSpec == null || !colSpec.getType().equals(trainColSpec.getType())) {
                unknownCols.add(colSpec.getName());
            }
        }
        return unknownCols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inclProbVals.saveSettingsTo(settings);
        m_predictionColumnName.saveSettingsTo(settings);
        m_overridePredicted.saveSettingsTo(settings);
        m_probabilitySuffix.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_inclProbVals.loadSettingsFrom(settings);
        m_predictionColumnName.loadSettingsFrom(settings);
        m_overridePredicted.loadSettingsFrom(settings);
        m_probabilitySuffix.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_inclProbVals.validateSettings(settings);
        m_predictionColumnName.validateSettings(settings);
        m_overridePredicted.validateSettings(settings);
        m_probabilitySuffix.validateSettings(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //nothing to do
    }
}
