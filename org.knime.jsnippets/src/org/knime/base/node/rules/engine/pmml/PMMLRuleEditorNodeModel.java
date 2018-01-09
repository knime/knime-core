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
 * Created on 2013.08.11. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.pmml;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.RuleSetDocument.RuleSet;
import org.dmg.pmml.RuleSetModelDocument.RuleSetModel;
import org.dmg.pmml.SimpleRuleDocument.SimpleRule;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.rules.engine.BaseRuleParser.ParseState;
import org.knime.base.node.rules.engine.Expression;
import org.knime.base.node.rules.engine.RuleEngineNodeModel;
import org.knime.base.node.rules.engine.RuleEngineSettings;
import org.knime.base.node.rules.engine.RuleFactory;
import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.base.node.rules.engine.RuleSupport;
import org.knime.base.node.rules.engine.Util;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectOutput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.Pair;

/**
 * This is the model implementation of PMML41RuleEditor. Edits PMML RuleSets.
 *
 * @author Gabor Bakos
 */
public class PMMLRuleEditorNodeModel extends NodeModel {

    private RuleEngineSettings m_settings = new RuleEngineSettings();

    /**
     * Constructor for the node model.
     */
    protected PMMLRuleEditorNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable table = (BufferedDataTable)inData[0];
        RearrangerAndPMMLModel m = createRearrangerAndPMMLModel(table.getDataTableSpec());
        return new PortObject[]{exec.createColumnRearrangeTable(table, m.getRearranger(), exec), m.getPMMLPortObject()};
    }

    private RearrangerAndPMMLModel createRearrangerAndPMMLModel(final DataTableSpec spec)
            throws ParseException, InvalidSettingsException {
        final PMMLDocument doc = PMMLDocument.Factory.newInstance();
        final PMML pmml = doc.addNewPMML();
        RuleSetModel ruleSetModel = pmml.addNewRuleSetModel();
        RuleSet ruleSet = ruleSetModel.addNewRuleSet();
        PMMLRuleParser parser = new PMMLRuleParser(spec, getAvailableInputFlowVariables());
        ColumnRearranger rearranger = createRearranger(spec, ruleSet, parser);
        PMMLPortObject ret =
                new PMMLPortObject(createPMMLPortObjectSpec(rearranger.createSpec(), parser.getUsedColumns()));
//        if (inData[1] != null) {
//            PMMLPortObject po = (PMMLPortObject)inData[1];
//            TransformationDictionary dict = TransformationDictionary.Factory.newInstance();
//            dict.setDerivedFieldArray(po.getDerivedFields());
//            ret.addGlobalTransformations(dict);
//        }
        PMMLRuleTranslator modelTranslator = new PMMLRuleTranslator();
        ruleSetModel.setFunctionName(MININGFUNCTION.CLASSIFICATION);
        ruleSet.setDefaultConfidence(defaultConfidenceValue());
        PMMLMiningSchemaTranslator.writeMiningSchema(ret.getSpec(), ruleSetModel);
        PMMLDataDictionaryTranslator ddTranslator = new PMMLDataDictionaryTranslator();
        ddTranslator.exportTo(doc, ret.getSpec());
        modelTranslator.initializeFrom(doc);
        ret.addModelTranslater(modelTranslator);
        ret.validate();
        return new RearrangerAndPMMLModel(rearranger, ret);
    }

    /**
     * Creates the {@link ColumnRearranger} that can compute the new column.
     *
     * @param tableSpec The spec of the input table.
     * @param ruleSet The {@link RuleSet} xml object where the rules should be added.
     * @param parser The parser for the rules.
     * @return The {@link ColumnRearranger}.
     * @throws ParseException Problem during parsing.
     * @throws InvalidSettingsException if settings are invalid
     */
    private ColumnRearranger createRearranger(final DataTableSpec tableSpec, final RuleSet ruleSet,
        final PMMLRuleParser parser) throws ParseException, InvalidSettingsException {
        if (m_settings.isAppendColumn() && m_settings.getNewColName().isEmpty()) {
            throw new InvalidSettingsException("No name for prediction column provided");
        }


        Set<String> outcomes = new LinkedHashSet<String>();
        List<DataType> outcomeTypes = new ArrayList<DataType>();
        int line = 0;
        final List<Pair<PMMLPredicate, Expression>> rules = new ArrayList<Pair<PMMLPredicate, Expression>>();
        for (String ruleText : m_settings.rules()) {
            ++line;
            if (RuleSupport.isComment(ruleText)) {
                continue;
            }
            try {
                ParseState state = new ParseState(ruleText);
                PMMLPredicate expression = parser.parseBooleanExpression(state);
                SimpleRule simpleRule = ruleSet.addNewSimpleRule();
                setCondition(simpleRule, expression);
                state.skipWS();
                state.consumeText("=>");
                state.skipWS();
                Expression outcome = parser.parseOutcomeOperand(state, null);
                // Only constants are allowed in the outcomes.
                assert outcome.isConstant() : outcome;
                rules.add(new Pair<PMMLPredicate, Expression>(expression, outcome));
                outcomeTypes.add(outcome.getOutputType());
                simpleRule.setScore(outcome.toString());
//                simpleRule.setConfidence(confidenceForRule(simpleRule, line, ruleText));
                simpleRule.setWeight(weightForRule(simpleRule, line, ruleText));
                outcomes.add(simpleRule.getScore());
            } catch (ParseException e) {
                throw Util.addContext(e, ruleText, line);
            }
        }
        DataType outcomeType = RuleEngineNodeModel.computeOutputType(outcomeTypes, true);
        ColumnRearranger rearranger = new ColumnRearranger(tableSpec);
        DataColumnSpecCreator specProto =
            new DataColumnSpecCreator(m_settings.isAppendColumn() ? DataTableSpec.getUniqueColumnName(tableSpec, m_settings.getNewColName())
                : m_settings.getReplaceColumn(), outcomeType);
        specProto.setDomain(new DataColumnDomainCreator(toCells(outcomes, outcomeType)).createDomain());
        SingleCellFactory cellFactory = new SingleCellFactory(true, specProto.createSpec()) {
            @Override
            public DataCell getCell(final DataRow row) {
                for (Pair<PMMLPredicate, Expression> pair : rules) {
                    if (pair.getFirst().evaluate(row, tableSpec) == Boolean.TRUE) {
                        return pair.getSecond().evaluate(row, null).getValue();
                    }
                }
                return DataType.getMissingCell();
            }

        };
        if (m_settings.isAppendColumn()) {
            rearranger.append(cellFactory);
        } else {
            rearranger.replace(cellFactory, m_settings.getReplaceColumn());
        }
        return rearranger;
    }

    /**
     * @return The default value for confidence.
     */
    protected double defaultConfidenceValue() {
        return 0.0;
    }

    /**
     * Computes the weight for the current rule.
     *
     * @param simpleRule The current xml {@link SimpleRule}.
     * @param line The line index, starting from {@code 1}.
     * @param ruleText The text the rule was parsed of.
     * @return The weight for the rule.
     */
    protected double weightForRule(final SimpleRule simpleRule, final int line, final String ruleText) {
        return 1.0;
    }

    /**
     * Computes the confidence value for the rule.
     *
     * @param simpleRule The xml {@link SimpleRule} representation.
     * @param line The line index, starting from {@code 1}.
     * @param ruleText The text the rule was parsed of.
     * @return The confidence value for the rule.
     */
    protected double confidenceForRule(final SimpleRule simpleRule, final int line, final String ruleText) {
        return 1.0;
    }

    /**
     * Initializes the xml {@link SimpleRule} based on the {@code expression}. After this call, the predicate will be
     * set.
     *
     * @param simpleRule An xml {@link SimpleRule}.
     * @param expression A condition to set.
     */
    private void setCondition(final SimpleRule simpleRule, final PMMLPredicate expression) {
        PMMLRuleTranslator translator = new PMMLRuleTranslator();
        translator.setPredicate(simpleRule, expression);
    }

    /**
     * Initializes the {@link PMMLPortObjectSpec} based on the model, input and the used column.
     *
     * @param modelSpec The preprocessing model, can be {@code null}.
     * @param spec The input table spec.
     * @param usedColumns The columns used by the rules.
     * @return The {@link PMMLPortObjectSpec} filled with proper data for configuration.
     */
    private PMMLPortObjectSpec createPMMLPortObjectSpec(final DataTableSpec spec, final List<String> usedColumns) {
        // this assumes that the new column is always the last column in the spec; which is the case if
        // #createRearranger uses ColumnRearranger.append.
        String targetCol =
            m_settings.isAppendColumn() ? spec.getColumnSpec(spec.getNumColumns() - 1).getName() : m_settings
                .getReplaceColumn();
        Set<String> set = new LinkedHashSet<>(usedColumns);
        List<String> learnCols = new LinkedList<>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec columnSpec = spec.getColumnSpec(i);
            String col = columnSpec.getName();
            if (!col.equals(targetCol)
                && set.contains(col)
                && (columnSpec.getType().isCompatible(DoubleValue.class) || columnSpec.getType().isCompatible(
                    NominalValue.class)
                    && (/*!m_skipColumns.getBooleanValue() ||*/columnSpec.getDomain().hasValues()))) {
                learnCols.add(spec.getColumnSpec(i).getName());
            }
        }
        PMMLPortObjectSpecCreator pmmlSpecCreator = new PMMLPortObjectSpecCreator(spec);
        pmmlSpecCreator.setLearningColsNames(learnCols);
        pmmlSpecCreator.setTargetColName(targetCol);
        return pmmlSpecCreator.createSpec();
    }

    /**
     * @param values Some values as {@link String}s.
     * @param outcomeType The preferred outcome type.
     * @return The {@code values} as {@link DataCell}s.
     */
    private static Set<DataCell> toCells(final Iterable<String> values, final DataType outcomeType) {
        Set<DataCell> cellValues = new LinkedHashSet<DataCell>();
        for (String val : values) {
            try {
                if (outcomeType.isCompatible(StringValue.class)) {
                    cellValues.add(new StringCell(val));
                } else if (outcomeType.isCompatible(BooleanValue.class)) {
                    cellValues.add(BooleanCellFactory.create(Boolean.parseBoolean(val)));
                } else if (outcomeType.isCompatible(IntValue.class)) {
                    cellValues.add(new IntCell(Integer.parseInt(val)));
                } else if (outcomeType.isCompatible(DoubleValue.class)) {
                    cellValues.add(new DoubleCell(Double.parseDouble(val)));
                }
            } catch (RuntimeException e) {
                cellValues.add(DataType.getMissingCell());
            }
        }
        return cellValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state to reset
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        RuleSet ruleSet = RuleSet.Factory.newInstance();
        PMMLRuleParser parser = new PMMLRuleParser((DataTableSpec)inSpecs[0], getAvailableInputFlowVariables());
        try {
            ColumnRearranger rearranger = createRearranger((DataTableSpec)inSpecs[0], ruleSet, parser);
            PMMLPortObjectSpec portObjectSpec =
                createPMMLPortObjectSpec(rearranger.createSpec(), Collections.<String> emptyList());
            return new PortObjectSpec[]{rearranger.createSpec(), portObjectSpec};
        } catch (ParseException e) {
            throw new InvalidSettingsException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        RuleEngineSettings res = new RuleEngineSettings();
        res.loadSettings(settings);
        RuleFactory ruleFactory = RuleFactory.getInstance(RuleNodeSettings.PMMLRule).cloned();
        ruleFactory.disableColumnChecks();
        ruleFactory.disableFlowVariableChecks();
        Map<String, FlowVariable> flowVars = getAvailableInputFlowVariables();
        for (String rule : res.rules()) {
            try {
                ruleFactory.parse(rule, null, flowVars);
            } catch (ParseException e) {
                throw new InvalidSettingsException(e.getMessage(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        final InputPortRole[] inputPortRoles = super.getInputPortRoles();
        inputPortRoles[0] = InputPortRole.DISTRIBUTED_STREAMABLE;
        return inputPortRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        final OutputPortRole[] outputPortRoles = super.getOutputPortRoles();
        outputPortRoles[0] = OutputPortRole.DISTRIBUTED;
        return outputPortRoles;
    }

    /**
     * This should be private, but cannot be unfortunately because it's deserialized by the framework.
     * @noinstantiate This class is not intended to be instantiated by clients.
     * @noreference This class is not intended to be referenced by clients.
     * @since 3.2
     */
    public static final class StreamInternalForPMMLPortObject extends StreamableOperatorInternals {
        private PMMLPortObject m_object;

        /**
         */
        public StreamInternalForPMMLPortObject() {
        }
        /**
         * @return the object
         */
        PMMLPortObject getObject() {
            return m_object;
        }

        /**
         * @param object the object to set
         * @return The update internal object.
         */
        StreamInternalForPMMLPortObject setObject(final PMMLPortObject object) {
            m_object = object;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void load(final DataInputStream input) throws IOException {
            try {
                m_object = (PMMLPortObject)PortUtil.readObjectFromStream(input, new ExecutionMonitor());
            } catch (CanceledExecutionException e) {
                throw new IOException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void save(final DataOutputStream output) throws IOException {
            try {
                PortUtil.writeObjectToStream(m_object, output, new ExecutionMonitor());
            } catch (CanceledExecutionException e) {
                throw new IOException(e);
            }
        }
    }

    /** {@inheritDoc}
     * @since 3.2 */
    @Override
    public StreamInternalForPMMLPortObject createInitialStreamableOperatorInternals() {
        return new StreamInternalForPMMLPortObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MergeOperator createMergeOperator() {
        //This could probably be a functional interface.
        return new MergeOperator() {
            @Override
            public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
                return operators[0];
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec[] computeFinalOutputSpecs(final StreamableOperatorInternals internals, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        final PortObjectSpec[] computeFinalOutputSpecs = super.computeFinalOutputSpecs(internals, inSpecs);
        // TODO should this be done some place else (finish)?
        StreamInternalForPMMLPortObject poInternals = (StreamInternalForPMMLPortObject)internals;
        try {
            DataTableSpec tableSpec = (DataTableSpec)inSpecs[0];
            RearrangerAndPMMLModel m = createRearrangerAndPMMLModel(tableSpec);
            poInternals.setObject(m.getPMMLPortObject());
        } catch (ParseException e) {
            throw new InvalidSettingsException(e);
        }

        return computeFinalOutputSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        final DataTableSpec tableSpec = (DataTableSpec)inSpecs[0];
        return new StreamableOperator() {
            private ColumnRearranger m_rearrangerx;
            private PMMLPortObject m_portObject;
            {
                try {
                    final PMMLDocument doc = PMMLDocument.Factory.newInstance();
                    final PMML pmml = doc.addNewPMML();
                    RuleSetModel ruleSetModel = pmml.addNewRuleSetModel();
                    RuleSet ruleSet = ruleSetModel.addNewRuleSet();
                    PMMLRuleParser parser = new PMMLRuleParser(tableSpec, getAvailableInputFlowVariables());
                    m_rearrangerx= createRearranger(tableSpec, ruleSet, parser);
                } catch (ParseException e) {
                    throw new InvalidSettingsException(e);
                }
            }
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
                m_rearrangerx.createStreamableFunction(0, 0).runFinal(inputs, outputs, exec);
            }

            /** {@inheritDoc} */
            @Override
            public void loadInternals(final StreamableOperatorInternals internals) {
                super.loadInternals(internals);
                m_portObject = ((StreamInternalForPMMLPortObject)internals).getObject();
            }

            /** {@inheritDoc} */
            @Override
            public StreamableOperatorInternals saveInternals() {
                return createInitialStreamableOperatorInternals().setObject(m_portObject);
            }

        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        final StreamInternalForPMMLPortObject poInternals = (StreamInternalForPMMLPortObject)internals;
        PMMLPortObject ret = poInternals.getObject();
        ret.validate();
        ((PortObjectOutput)output[1]).setPortObject(ret);
    }

    /**
     * Computes the specs after applying the derived fields.
     *
     * @param specs The input table and the possible preprocessing port.
     * @return The computed (original+preproc) input table's specification.
     */
    @Deprecated
    static DataTableSpec computeSpecs(final PortObjectSpec[] specs) {
        final DataTableSpec tableSpec = (DataTableSpec)specs[0];
        if (specs[1] == null) {
            return tableSpec;
        }
        PMMLPortObjectSpec portObjectSpec = (PMMLPortObjectSpec)specs[1];
        List<DataColumnSpec> preprocessingCols = portObjectSpec.getPreprocessingCols();
        DataTableSpecCreator creator = new DataTableSpecCreator(tableSpec);
        for (DataColumnSpec spec : preprocessingCols) {
            creator.addColumns(spec);
        }
        return creator.createSpec();
    }

    /** Pair combining a rearranger and PO. */
    private static final class RearrangerAndPMMLModel {
        private final ColumnRearranger m_rearranger;
        private final PMMLPortObject m_pmmlPortObject;
        /**
         * @param rearranger
         * @param pmmlPortObject
         */
        RearrangerAndPMMLModel(final ColumnRearranger rearranger, final PMMLPortObject pmmlPortObject) {
            m_rearranger = CheckUtils.checkArgumentNotNull(rearranger);
            m_pmmlPortObject = CheckUtils.checkArgumentNotNull(pmmlPortObject);
        }

        /** @return the pmmlPortObject */
        PMMLPortObject getPMMLPortObject() {
            return m_pmmlPortObject;
        }

        /** @return the rearranger */
        ColumnRearranger getRearranger() {
            return m_rearranger;
        }

    }
}
