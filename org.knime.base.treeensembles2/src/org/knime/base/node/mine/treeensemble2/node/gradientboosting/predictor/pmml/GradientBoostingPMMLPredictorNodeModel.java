/*
 * ------------------------------------------------------------------------
 *
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
 *   14.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.predictor.pmml;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostingModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.MultiClassGradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.model.pmml.AbstractGBTModelPMMLTranslator;
import org.knime.base.node.mine.treeensemble2.model.pmml.AbstractTreeModelPMMLTranslator;
import org.knime.base.node.mine.treeensemble2.model.pmml.ClassificationGBTModelPMMLTranslator;
import org.knime.base.node.mine.treeensemble2.model.pmml.RegressionGBTModelPMMLTranslator;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.predictor.GBTRegressionPredictor;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.predictor.LKGradientBoostedTreesPredictor;
import org.knime.base.node.mine.treeensemble2.node.predictor.PredictionRearrangerCreator;
import org.knime.base.node.mine.treeensemble2.node.predictor.TreeEnsemblePredictionUtility;
import org.knime.base.node.mine.treeensemble2.node.predictor.TreeEnsemblePredictorConfiguration;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.util.CheckUtils;

/**
 * Predictor for GBT models that imports its model from PMML prior to prediction.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <M> the model type (usually {@link GradientBoostedTreesModel} or {@link MultiClassGradientBoostedTreesModel})
 */
public class GradientBoostingPMMLPredictorNodeModel <M extends AbstractGradientBoostingModel> extends NodeModel {

    private TreeEnsemblePredictorConfiguration m_configuration;
    private final boolean m_isRegression;
    private final boolean m_pre_3_6;

    /**
     * This constructor ensures backwards compatibility.
     * @param isRegression indicates whether the node expects a regression model
     */
    public GradientBoostingPMMLPredictorNodeModel(final boolean isRegression) {
        this(isRegression, true);
    }

    /**
     * Default constructor
     * @param isRegression boolean indicating if the node model expects a regression model
     * @param pre_3_6 whether the model was build prior to version 3.6.0
     */
    public GradientBoostingPMMLPredictorNodeModel(final boolean isRegression, final boolean pre_3_6) {
        super(new PortType[]{PMMLPortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE});
        m_isRegression = isRegression;
        m_pre_3_6 = pre_3_6;
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        PMMLPortObjectSpec pmmlSpec = (PMMLPortObjectSpec)inSpecs[0];
        DataType targetType = extractTargetType(pmmlSpec);
        if (m_isRegression && !targetType.isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException("This node expects a regression model.");
        } else if (!m_isRegression && !targetType.isCompatible(StringValue.class)) {
            throw new InvalidSettingsException("This node expectes a classification model.");
        }
        try {
            AbstractTreeModelPMMLTranslator.checkPMMLSpec(pmmlSpec);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(e.getMessage());
        }
        TreeEnsembleModelPortObjectSpec modelSpec = translateSpec(pmmlSpec);
        String targetColName = modelSpec.getTargetColumn().getName();
        if (m_configuration == null) {
            m_configuration = TreeEnsemblePredictorConfiguration.createDefault(m_isRegression, targetColName);
        } else if (!m_configuration.isChangePredictionColumnName()) {
            m_configuration
                .setPredictionColumnName(TreeEnsemblePredictorConfiguration.getPredictColumnName(targetColName));
        }
        modelSpec.assertTargetTypeMatches(m_isRegression);
        DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
        PredictionRearrangerCreator prc = createRearrangerCreator(dataSpec, modelSpec, null);
        Optional<ColumnRearranger> rearranger = prc.createConfigurationRearranger();
        return rearranger.isPresent() ? new PortObjectSpec[]{rearranger.get().createSpec()} : null;
    }

    private PredictionRearrangerCreator createRearrangerCreator(final DataTableSpec predictSpec,
        final TreeEnsembleModelPortObjectSpec modelSpec, final M model) throws InvalidSettingsException {
        PredictionRearrangerCreator prc;
        if (m_isRegression) {
            prc = new PredictionRearrangerCreator(predictSpec,
                new GBTRegressionPredictor((GradientBoostedTreesModel)model,
                TreeEnsemblePredictionUtility.createRowConverter(modelSpec, model, predictSpec)));
            prc.addRegressionPrediction(m_configuration.getPredictionColumnName());
        } else {
            MultiClassGradientBoostedTreesModel gbt = (MultiClassGradientBoostedTreesModel)model;
            prc = new PredictionRearrangerCreator(predictSpec,
                new LKGradientBoostedTreesPredictor(gbt,
                    m_configuration.isAppendClassConfidences() || m_configuration.isAppendPredictionConfidence(),
                TreeEnsemblePredictionUtility.createRowConverter(modelSpec, model, predictSpec)));
            TreeEnsemblePredictionUtility.setupRearrangerCreatorGBT(m_pre_3_6, prc, modelSpec, gbt, m_configuration);
        }
        return prc;
    }

    private ColumnRearranger createExecutionRearranger(final DataTableSpec predictSpec,
        final TreeEnsembleModelPortObjectSpec modelSpec, final M model) throws InvalidSettingsException {
        PredictionRearrangerCreator prc = createRearrangerCreator(predictSpec, modelSpec, model);
        return prc.createExecutionRearranger();
    }

    /** {@inheritDoc} */
    @Override
    public PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        PMMLPortObject pmmlPO = (PMMLPortObject)inObjects[0];
        GradientBoostingModelPortObject model = importModel(pmmlPO);
        BufferedDataTable data = (BufferedDataTable)inObjects[1];
        DataTableSpec dataSpec = data.getDataTableSpec();
        // only happens if configure was not called previously e.g. in the generic PMML predictor
        if (m_configuration == null) {
            m_configuration = TreeEnsemblePredictorConfiguration.createDefault(
                m_isRegression, translateSpec(pmmlPO.getSpec()).getTargetColumn().getName());
        }
        ColumnRearranger rearranger = createExecutionRearranger(dataSpec, model.getSpec(), (M)model.getEnsembleModel());
        BufferedDataTable outTable = exec.createColumnRearrangeTable(data, rearranger, exec);
        return new BufferedDataTable[]{outTable};
    }

    private TreeEnsembleModelPortObjectSpec translateSpec(final PMMLPortObjectSpec pmmlSpec) {
        DataTableSpec pmmlDataSpec = pmmlSpec.getDataTableSpec();
        ColumnRearranger cr = new ColumnRearranger(pmmlDataSpec);
        List<DataColumnSpec> targets = pmmlSpec.getTargetCols();
        CheckUtils.checkArgument(!targets.isEmpty(), "The provided PMML does not declare a target field.");
        CheckUtils.checkArgument(targets.size() == 1, "The provided PMML declares multiple target. "
            + "This behavior is currently not supported.");
        cr.move(targets.get(0).getName(), pmmlDataSpec.getNumColumns());
        return new TreeEnsembleModelPortObjectSpec(cr.createSpec());
    }

    private DataType extractTargetType(final PMMLPortObjectSpec pmmlSpec) {
        return pmmlSpec.getTargetCols().get(0).getType();
    }

    @SuppressWarnings("unchecked")
    private GradientBoostingModelPortObject importModel(final PMMLPortObject pmmlPO) {
        AbstractGBTModelPMMLTranslator<M> pmmlTranslator;
        DataType targetType = extractTargetType(pmmlPO.getSpec());
        if (targetType.isCompatible(DoubleValue.class)) {
            pmmlTranslator = (AbstractGBTModelPMMLTranslator<M>)new RegressionGBTModelPMMLTranslator();
        } else if (targetType.isCompatible(StringValue.class)) {
            pmmlTranslator = (AbstractGBTModelPMMLTranslator<M>)new ClassificationGBTModelPMMLTranslator();
        } else {
            throw new IllegalArgumentException("Currently only regression models are supported.");
        }
        pmmlPO.initializeModelTranslator(pmmlTranslator);
        if (pmmlTranslator.hasWarning()) {
            setWarningMessage(pmmlTranslator.getWarning());
        }
        return new GradientBoostingModelPortObject(new TreeEnsembleModelPortObjectSpec(pmmlTranslator.getLearnSpec()),
            pmmlTranslator.getGBTModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                PMMLPortObject model =
                    (PMMLPortObject)((PortObjectInput)inputs[0]).getPortObject();
                DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
                GradientBoostingModelPortObject gbt = importModel(model);
                ColumnRearranger rearranger = createExecutionRearranger(dataSpec, gbt.getSpec(),
                    (M)gbt.getEnsembleModel());
                StreamableFunction func = rearranger.createStreamableFunction(1, 0);
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

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.save(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        TreeEnsemblePredictorConfiguration config = new TreeEnsemblePredictorConfiguration(m_isRegression, "");
        config.loadInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        TreeEnsemblePredictorConfiguration config = new TreeEnsemblePredictorConfiguration(m_isRegression, "");
        config.loadInModel(settings);
        m_configuration = config;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }
}
