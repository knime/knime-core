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
 * ---------------------------------------------------------------------
 *
 * History
 *   01.10.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.predictor2;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.knime.base.node.mine.svm.PMMLSVMTranslator;
import org.knime.base.node.mine.svm.Svm;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
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
 * NodeModel of the SVM Predictor Node.
 * @author cebron, University of Konstanz
 */
public final class SVMPredictorNodeModel extends NodeModel {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SVMPredictorNodeModel.class);
    /** Configuration key for add probabilities property. */
    static final String CFGKEY_ADD_PROBABILITIES = "add probabilities";
    /** Default value for add probabilities. */
    static final boolean DEFAULT_ADD_PROBABILITIES = false;
    /*
     * The extracted Support Vector Machines.
     */
    private Svm[] m_svms;

    /*
     * Column indices to use.
     */
    private int[] m_colindices;

    /**
     * @return The {@link SettingsModelBoolean} for adding class probabilities or not.
     */
    static SettingsModelBoolean createAddProbabilities() {
        return new SettingsModelBoolean(CFGKEY_ADD_PROBABILITIES, DEFAULT_ADD_PROBABILITIES);
    }

    private final SettingsModelBoolean m_addProbabilities = createAddProbabilities();
    private final SettingsModelString m_predictionColumn = PredictorHelper.getInstance().createPredictionColumn();
    private final SettingsModelBoolean m_overridePrediction = PredictorHelper.getInstance().createChangePrediction();
    private final SettingsModelString m_suffix = PredictorHelper.getInstance().createSuffix();

    /**
     * Constructor, one model and data input, one (classified) data output.
     */
    public SVMPredictorNodeModel() {
        super(new PortType[] {PMMLPortObject.TYPE, BufferedDataTable.TYPE },
                new PortType[] {
                BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec testSpec = (DataTableSpec)inSpecs[1];
        PMMLPortObjectSpec trainingSpec = (PMMLPortObjectSpec)inSpecs[0];
        // try to find all columns (except the class column)
        Vector<Integer> colindices = new Vector<Integer>();
        for (DataColumnSpec colspec : trainingSpec.getLearningCols()) {
            if (colspec.getType().isCompatible(DoubleValue.class)) {
                int colindex = testSpec.findColumnIndex(colspec.getName());
                if (colindex < 0) {
                    throw new InvalidSettingsException("Column " + "\'" + colspec.getName() + "\' not found"
                        + " in test data");
                }
                colindices.add(colindex);
            }
        }
        final PredictorHelper predictorHelper = PredictorHelper.getInstance();
        return new DataTableSpec[]{predictorHelper.createOutTableSpec(testSpec, trainingSpec,
            m_addProbabilities.getBooleanValue(), m_predictionColumn.getStringValue(),
            m_overridePrediction.getBooleanValue(), m_suffix.getStringValue())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        PMMLPortObject port = (PMMLPortObject)inData[0];
        BufferedDataTable testData = (BufferedDataTable) inData[1];
        ColumnRearranger colre = createColumnRearranger(port, testData.getDataTableSpec());
        BufferedDataTable result =
                exec.createColumnRearrangeTable(testData, colre, exec);
        return new BufferedDataTable[]{result};
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
                PMMLPortObject pmmlModel = (PMMLPortObject) ((PortObjectInput) inputs[0]).getPortObject();
                ColumnRearranger colre = createColumnRearranger(pmmlModel, (DataTableSpec) inSpecs[1]);
                StreamableFunction func = colre.createStreamableFunction(1, 0);
                func.runFinal(inputs, outputs, exec);
            }
        };
    }

    private ColumnRearranger createColumnRearranger(final PMMLPortObject pmmlModel, final DataTableSpec inSpec)
        throws InvalidSettingsException {
        List<Node> models = pmmlModel.getPMMLValue().getModels(PMMLModelType.SupportVectorMachineModel);
        if (models.isEmpty()) {
            String msg = "SVM evaluation failed: " + "No support vector machine model found.";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        PMMLSVMTranslator trans = new PMMLSVMTranslator();
        pmmlModel.initializeModelTranslator(trans);

        List<Svm> svms = trans.getSVMs();
        m_svms = svms.toArray(new Svm[svms.size()]);
        if (m_addProbabilities.getBooleanValue() == pmmlModel.getSpec().getTargetCols().size() > 0) {
            adjustOrder(pmmlModel.getSpec().getTargetCols().get(0));
        }
        DataTableSpec testSpec = inSpec;
        PMMLPortObjectSpec pmmlSpec = pmmlModel.getSpec();
        DataTableSpec trainingSpec = pmmlSpec.getDataTableSpec();
        // try to find all columns (except the class column)
        Vector<Integer> colindices = new Vector<Integer>();
        for (DataColumnSpec colspec : trainingSpec) {
            if (colspec.getType().isCompatible(DoubleValue.class)) {
                int colindex = testSpec.findColumnIndex(colspec.getName());
                if (colindex < 0) {
                    throw new InvalidSettingsException(
                        "Column " + "\'" + colspec.getName() + "\' not found" + " in test data");
                }
                colindices.add(colindex);
            }
        }
        m_colindices = new int[colindices.size()];
        for (int i = 0; i < m_colindices.length; i++) {
            m_colindices[i] = colindices.get(i);
        }
        final PredictorHelper predictorHelper = PredictorHelper.getInstance();
        final String targetCol = pmmlSpec.getTargetFields().iterator().next();
        SVMPredictor svmpredict = new SVMPredictor(targetCol, m_svms, m_colindices,
            predictorHelper.computePredictionColumnName(m_predictionColumn.getStringValue(),
                m_overridePrediction.getBooleanValue(), targetCol),
            m_addProbabilities.getBooleanValue(), m_suffix.getStringValue());
        ColumnRearranger colre = new ColumnRearranger(testSpec);
        colre.append(svmpredict);
        return colre;
    }

    /**
     * @param targetSpec The target column from the model.
     */
    private void adjustOrder(final DataColumnSpec targetSpec) {
        if (targetSpec.getDomain() != null) {
            Map<String, Svm> map = new LinkedHashMap<>();
            for (Svm svm : m_svms) {
                map.put(svm.getPositive(), svm);
            }
            int i = 0;
            for (DataCell v : targetSpec.getDomain().getValues()) {
                String key = v.toString();
                Svm svm = map.get(key);
                if (svm != null) {
                    m_svms[i++] = svm;
                }
            }
        }
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
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_predictionColumn.loadSettingsFrom(settings);
        m_overridePrediction.loadSettingsFrom(settings);
        m_addProbabilities.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_predictionColumn.saveSettingsTo(settings);
        m_overridePrediction.saveSettingsTo(settings);
        m_addProbabilities.saveSettingsTo(settings);
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
        m_addProbabilities.validateSettings(settings);
        m_suffix.validateSettings(settings);
    }

}
